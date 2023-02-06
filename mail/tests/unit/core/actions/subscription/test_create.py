import pytest

from sendr_interactions import exceptions as interaction_errors

from mail.payments.payments.core.actions.subscription.create import (
    CoreCreateSubscriptionAction, CreateSubscriptionAction, CreateSubscriptionServiceMerchantAction
)
from mail.payments.payments.core.entities.enums import NDS, FunctionalityType, ModerationType, PeriodUnit, TaskType
from mail.payments.payments.core.entities.subscription import Subscription, SubscriptionData, SubscriptionPrice
from mail.payments.payments.core.exceptions import CoreFieldError
from mail.payments.payments.tests.base import BaseTestRequiresModeration, parametrize_shop_type


@pytest.fixture
def region_id(randn):
    return randn()


@pytest.fixture
def period_amount(payments_settings):
    return payments_settings.CUSTOMER_SUBSCRIPTION_MIN_PERIOD


@pytest.fixture
def period_unit():
    return PeriodUnit.SECOND


class TestCoreCreateSubscriptionAction(BaseTestRequiresModeration):
    @pytest.fixture(autouse=True)
    def product_subscription_create(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'product_subscription_create', {'status': 'success'}) as mock:
            yield mock

    @pytest.fixture
    def get_region_exception(self):
        return None

    @pytest.fixture
    def get_region_result(self, region_id):
        return {"id": region_id, "type": 3}

    @pytest.fixture(autouse=True)
    def get_region(self, geobase_client_mocker, get_region_result, get_region_exception):
        with geobase_client_mocker('get_region', result=get_region_result, exc=get_region_exception) as mock:
            yield mock

    @pytest.fixture
    def fast_moderation(self, randbool):
        return randbool()

    @pytest.fixture
    def params(self, rands, randn, merchant_oauth_mode, randitem, period_amount, period_unit, randdecimal,
               region_id, merchant, service_merchant, service_client, fast_moderation):
        return {
            'uid': merchant.uid,
            'title': rands(),
            'fiscal_title': rands(),
            'nds': randitem(NDS),
            'period_amount': period_amount,
            'period_units': period_unit,
            'prices': [{'price': randdecimal(), 'currency': 'RUB', 'region_id': region_id}],
            'trial_period_amount': randn(max=100),
            'trial_period_units': randitem(PeriodUnit),
            'merchant_oauth_mode': merchant_oauth_mode,
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_client_id': service_client.service_client_id,
            'fast_moderation': fast_moderation,
        }

    @pytest.fixture
    def action(self, params):
        return CoreCreateSubscriptionAction(**params)

    @pytest.fixture
    def returned_func(self, action):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @parametrize_shop_type
    def test_create(self, service_client, merchant_oauth_mode, params, service_merchant, merchant, fast_moderation,
                    returned):
        subscription = Subscription(
            uid=merchant.uid,
            product_uuid=returned.product_uuid,
            subscription_id=returned.subscription_id,
            service_merchant_id=service_merchant.service_merchant_id,
            service_client_id=service_client.service_client_id,
            title=params['title'],
            fiscal_title=params['fiscal_title'],
            nds=params['nds'],
            period_amount=params['period_amount'],
            period_units=params['period_units'],
            prices=[SubscriptionPrice(**price) for price in params['prices']],
            trial_period_amount=params['trial_period_amount'],
            trial_period_units=params['trial_period_units'],
            revision=returned.revision,
            created=returned.created,
            updated=returned.updated,
            moderation=returned.moderation,
            merchant_oauth_mode=merchant_oauth_mode,
            acquirer=merchant.acquirer,
            data=SubscriptionData(
                fast_moderation=fast_moderation,
            )
        )
        assert subscription == returned

    @parametrize_shop_type
    def test_product_subscription_create_call(self, approved_merchant_moderation_data, merchant,
                                              product_subscription_create, empty_moderation_data, returned):
        returned.moderation = None
        merchant.moderation = approved_merchant_moderation_data
        merchant.moderations = {
            FunctionalityType.PAYMENTS: approved_merchant_moderation_data,
            FunctionalityType.YANDEX_PAY: empty_moderation_data,
        }

        product_subscription_create.assert_called_once_with(
            uid=merchant.uid,
            acquirer=merchant.acquirer,
            merchant=merchant,
            subscription=returned,
        )

    @pytest.mark.asyncio
    async def test_moderation(self, returned, get_moderations, get_tasks):
        tasks = await get_tasks()
        moderations = await get_moderations(moderation_type=ModerationType.SUBSCRIPTION)

        assert (
            len(tasks) == 1
            and tasks[0].task_type == TaskType.START_SUBSCRIPTION_MODERATION
            and tasks[0].params == dict(moderation_id=moderations[0].moderation_id)
            and len(moderations) == 1
            and moderations[0].moderation_type == ModerationType.SUBSCRIPTION
            and moderations[0].uid == returned.uid
            and moderations[0].entity_id == returned.subscription_id
            and moderations[0].revision == returned.revision
        )

    @pytest.mark.asyncio
    async def test_moderation_disabled(self, payments_settings, get_tasks, get_moderations, returned_func):
        payments_settings.SUBSCRIPTION_MODERATION_DISABLED = True

        await returned_func()

        tasks = await get_tasks()
        moderations = await get_moderations(moderation_type=ModerationType.SUBSCRIPTION)

        assert len(tasks) == 0 and len(moderations) == 0

    @pytest.mark.asyncio
    async def test_min_period(self, period_amount, payments_settings, returned_func):
        payments_settings.CUSTOMER_SUBSCRIPTION_MIN_PERIOD = period_amount + 1

        with pytest.raises(CoreFieldError) as error:
            await returned_func()

        assert error.value.fields == {
            "period_amount": [
                f'period should be more then {payments_settings.CUSTOMER_SUBSCRIPTION_MIN_PERIOD} seconds'
            ]
        }

    class TestGeobaseException:
        @pytest.fixture
        def get_region_exception(self):
            return interaction_errors.InteractionResponseError(
                status_code=400,
                service='geobase',
                method='GET',
                response_status=None,
            )

        @pytest.mark.asyncio
        async def test_geobase_exception__region_not_found(self, region_id, returned_func):
            with pytest.raises(CoreFieldError) as error:
                await returned_func()

            assert error.value.fields == {"prices": {"0": {"region_id": [f"Unknown region_id: {region_id}"]}}}

    class TestRegionIdIsNotCountry:
        @pytest.fixture
        def get_region_result(self, region_id):
            return {"id": region_id, "type": 2}

        @pytest.mark.asyncio
        async def test_regiod_id_is_not_country__region_not_found(self, region_id, returned_func):
            with pytest.raises(CoreFieldError) as error:
                await returned_func()

            assert error.value.fields == {"prices": {"0": {"region_id": ["region_id must be id of country"]}}}


@pytest.fixture
def subscription():
    return Subscription(
        uid=1,
        title='title',
        fiscal_title='fiscal_title',
        nds=NDS.NDS_NONE,
        period_amount=2,
        period_units=PeriodUnit.MONTH,
        prices=[],
    )


class TestCreateSubscriptionAction:
    @pytest.fixture
    def params(self, rands, randn, randbool, merchant_oauth_mode, randitem, period_amount, period_unit, randdecimal,
               region_id, merchant):
        return {
            'uid': merchant.uid,
            'title': rands(),
            'fiscal_title': rands(),
            'nds': randitem(NDS),
            'period_amount': period_amount,
            'period_units': period_unit,
            'prices': [{'price': randdecimal(), 'currency': 'RUB', 'region_id': region_id}],
            'trial_period_amount': randn(max=100),
            'trial_period_units': randitem(PeriodUnit),
            'merchant_oauth_mode': merchant_oauth_mode,
            'fast_moderation': randbool(),
        }

    @pytest.fixture(autouse=True)
    def mock_core_action(self, mock_action, subscription):
        return mock_action(CoreCreateSubscriptionAction, subscription)

    @pytest.fixture
    async def returned(self, params):
        return await CreateSubscriptionAction(**params).run()

    def test_result(self, returned, subscription):
        assert returned == subscription

    def test_core_call_args(self, returned, mock_core_action, params, merchant, service_client):
        mock_core_action.assert_called_once_with(**params)


class TestCreateSubscriptionServiceMerchantAction:
    @pytest.fixture
    def params(self, rands, randn, randbool, merchant_oauth_mode, randitem, period_amount, period_unit, randdecimal,
               service_merchant, region_id, service_client):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': service_client.tvm_id,
            'title': rands(),
            'fiscal_title': rands(),
            'nds': randitem(NDS),
            'period_amount': period_amount,
            'period_units': period_unit,
            'prices': [{'price': randdecimal(), 'currency': 'RUB', 'region_id': region_id}],
            'trial_period_amount': randn(max=100),
            'trial_period_units': randitem(PeriodUnit),
            'merchant_oauth_mode': merchant_oauth_mode,
            'fast_moderation': randbool(),
        }

    @pytest.fixture(autouse=True)
    def mock_core_action(self, mock_action, subscription):
        return mock_action(CoreCreateSubscriptionAction, subscription)

    @pytest.fixture
    async def returned(self, params):
        return await CreateSubscriptionServiceMerchantAction(**params).run()

    def test_result(self, returned, subscription):
        assert returned == subscription

    def test_core_call_args(self, returned, mock_core_action, params, merchant, service_client):
        params.pop('service_tvm_id')
        params['uid'] = merchant.uid
        params['service_client_id'] = service_client.service_client_id

        mock_core_action.assert_called_once_with(**params)
