import pytest

from sendr_interactions import exceptions as interaction_errors
from sendr_utils import anext

from mail.payments.payments.core.actions.customer_subscription.create import (
    CoreCreateCustomerSubscriptionAction, CreateCustomerSubscriptionAction,
    CreateCustomerSubscriptionServiceMerchantAction
)
from mail.payments.payments.core.entities.enums import AcquirerType, ModerationType, OrderSource, ShopType
from mail.payments.payments.core.exceptions import (
    SubscriptionHasNoPriceForRegionId, SubscriptionNotFoundError, UnknownRegionId
)
from mail.payments.payments.storage.mappers.order.order import FindOrderParams
from mail.payments.payments.tests.base import (
    BaseSubscriptionAcquirerTest, BaseTestOrderAction, BaseTestRequiresModeration, parametrize_merchant_oauth_mode
)


@pytest.fixture
def region_id(subscription):
    return subscription.prices[0].region_id


@pytest.fixture
def param_region_id(region_id):
    return region_id


@pytest.fixture
def param_user_ip(rands):
    return rands()


@pytest.fixture
def region_parents(region_id, randn):
    return [region_id, randn()]


@pytest.fixture
def get_region_id_by_ip_exception():
    return None


@pytest.fixture
def get_parents_exception():
    return None


@pytest.fixture(autouse=True)
def get_region_id_by_ip(geobase_client_mocker, region_id, get_region_id_by_ip_exception):
    with geobase_client_mocker('get_region_id_by_ip', result=region_id, exc=get_region_id_by_ip_exception) as mock:
        yield mock


@pytest.fixture(autouse=True)
def get_parents(geobase_client_mocker, region_parents, randn, get_parents_exception):
    with geobase_client_mocker('get_parents', result=region_parents, exc=get_parents_exception) as mock:
        yield mock


class TestCoreCreateCustomerSubscriptionAction(BaseTestRequiresModeration,
                                               BaseSubscriptionAcquirerTest,
                                               BaseTestOrderAction):
    @pytest.fixture(autouse=True)
    async def setup_order_acquirer(
        self,
        storage,
        subscription_acquirer,
        create_merchant_oauth,
        default_merchant_shops,
        merchant,
        merchant_oauth_mode,
    ):
        if subscription_acquirer == AcquirerType.KASSA:
            uid = merchant.uid
            prod_shop = default_merchant_shops[ShopType.from_oauth_mode(merchant_oauth_mode)]
            oauth = [await create_merchant_oauth(uid, shop_id=prod_shop.shop_id)]
            merchant.oauth = oauth
            await storage.merchant.save(merchant)

    @pytest.fixture
    def moderations_data(self, merchant, subscription):
        return [
            {'approved': True},
            {
                'approved': True,
                'uid': merchant.uid,
                'entity_id': subscription.subscription_id,
                'moderation_type': ModerationType.SUBSCRIPTION
            }
        ]

    @pytest.fixture
    def params(self, merchant, param_region_id, param_user_ip, subscription, randn, service_merchant, service_client):
        return {
            'uid': merchant.uid,
            'subscription_id': subscription.subscription_id,
            'quantity': 1,
            'source': OrderSource.UI,
            'customer_uid': randn(),
            'user_ip': param_user_ip,
            'region_id': param_region_id,
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_client_id': service_client.service_client_id,
        }

    @pytest.fixture
    def action(self, crypto, params):
        CoreCreateCustomerSubscriptionAction.context.crypto = crypto
        return CoreCreateCustomerSubscriptionAction(**params)

    @pytest.fixture
    def returned_func(self, action):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @parametrize_merchant_oauth_mode
    @pytest.mark.asyncio
    async def test_shop_type_of_order(self, returned, subscription, merchant_oauth_mode, shop_type):
        _, order = returned
        assert order.shop.shop_type == ShopType.from_oauth_mode(subscription.merchant_oauth_mode)

    @pytest.mark.asyncio
    @parametrize_merchant_oauth_mode
    async def test_created(self, returned, subscription, storage):
        returned_customer_subscription, returned_order = returned

        created_customer_subscription = await anext(storage.customer_subscription.find())
        created_order = await anext(storage.order.find(FindOrderParams(
            uid=returned_order.uid,
            order_id=returned_order.order_id,
            select_customer_subscription=True,
        )))

        created_order.order_hash = returned_order.order_hash
        created_order.items = returned_order.items
        assert all((
            created_customer_subscription == returned_customer_subscription,
            created_order == returned_order,
            created_order.acquirer == subscription.acquirer
        ))

    class TestSubscriptionHasNoPriceForRegionId:
        @pytest.fixture
        def region_parents(self, randn):
            return [randn(), randn()]

        @pytest.mark.asyncio
        async def test_not_available_region_id(self, returned_func):
            with pytest.raises(SubscriptionHasNoPriceForRegionId):
                await returned_func()

    class TestUnknownRegionIdByIp:
        @pytest.fixture
        def param_region_id(self):
            return None

        @pytest.fixture
        def get_region_id_by_ip_exception(self):
            return interaction_errors.InteractionResponseError(
                service='geobase',
                method='POST',
                status_code=400,
                response_status='error',
                params={'error': 'error'},
            )

        @pytest.mark.asyncio
        async def test_unknown_region_id_by_ip__unknown_region_id(self, returned_func):
            with pytest.raises(UnknownRegionId) as exc_info:
                await returned_func()
            assert exc_info.value.params['error'] == 'error'

    class TestUnknownRegionIdByRegionId:
        @pytest.fixture
        def get_parents_exception(self):
            return interaction_errors.InteractionResponseError(
                service='geobase',
                method='POST',
                status_code=400,
                response_status='error',
                params={'error': 'error'},
            )

        @pytest.mark.asyncio
        async def test_unknown_region_id_by_region_id__unknown_regiod_id(self, returned_func):
            with pytest.raises(UnknownRegionId) as exc_info:
                await returned_func()
            assert exc_info.value.params['error'] == 'error'

    class TestActionDenySubscription:
        @pytest.fixture(params=(
            pytest.param(
                [],
                id='no_moderations',
            ),
            pytest.param(
                [{'approved': False}],
                id='disapproved',
            ),
            pytest.param(
                [{'approved': True}, {'approved': False}],
                id='disapproved_after_approve',
            ),
            pytest.param(
                [{'approved': False}, {'approved': True, 'ignore': True}],
                id='disapproved_with_ignored_approve',
            ),
        ))
        def moderations_data(self, request, subscription):
            # Кейс, когда мерчант отмодерирован, а подписка нет
            return [{'approved': True}] + request.param

        @pytest.mark.asyncio
        async def test_action_deny_subscription__raises_error(self, action_deny_exception, moderations, returned_func):
            with pytest.raises(action_deny_exception):
                await returned_func()

    class TestTrustworthy:
        @pytest.fixture
        def moderations_data(self):
            return [{'approved': True}]

        @pytest.fixture(autouse=True)
        async def setup(self, storage, merchant):
            merchant.trustworthy = True
            await storage.merchant.save(merchant)

        def test_trustworthy(self, returned):
            pass

    class TestDeletedSubscription:
        @pytest.fixture
        async def subscription(self, storage, subscription):
            subscription.deleted = True
            return await storage.subscription.save(subscription)

        @pytest.mark.asyncio
        async def test_deleted_subscription(self, subscription, returned_func):
            with pytest.raises(SubscriptionNotFoundError):
                await returned_func()


class TestCreateCustomerSubscriptionAction:
    @pytest.fixture(autouse=True)
    def mock_core_action(self, mock_action, customer_subscription_entity, order_entity):
        return mock_action(CoreCreateCustomerSubscriptionAction, (customer_subscription_entity, order_entity))

    @pytest.fixture
    def params(self, merchant, param_region_id, param_user_ip, subscription, randn, rands):
        return {
            'uid': merchant.uid,
            'subscription_id': subscription.subscription_id,
            'quantity': 1,
            'customer_uid': randn(),
            'user_ip': param_user_ip,
            'region_id': param_region_id,
            'paymethod_id': rands(),
        }

    @pytest.fixture
    async def returned(self, params):
        return await CreateCustomerSubscriptionAction(**params).run()

    def test_core_create_customer_subscription_action_call(self, mock_core_action, params, returned):
        mock_core_action.assert_called_once_with(
            **params,
            source=OrderSource.UI,
        )

    def test_create_customer_subscription_action_returned(self,
                                                          returned,
                                                          customer_subscription_entity,
                                                          order_entity):
        returned_customer_subscription, returned_order = returned
        assert returned_customer_subscription == customer_subscription_entity
        assert returned_order == order_entity


class TestCreateCustomerSubscriptionServiceMerchantAction:
    @pytest.fixture(autouse=True)
    def mock_core_action(self, mock_action, customer_subscription_entity, order_entity):
        return mock_action(CoreCreateCustomerSubscriptionAction, (customer_subscription_entity, order_entity))

    @pytest.fixture
    def params(self, service_merchant, service_client, param_region_id, param_user_ip, subscription, randn, rands):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': service_client.tvm_id,
            'subscription_id': subscription.subscription_id,
            'quantity': 1,
            'customer_uid': randn(),
            'user_ip': param_user_ip,
            'region_id': param_region_id,
            'paymethod_id': rands(),
        }

    @pytest.fixture
    async def returned(self, params):
        return await CreateCustomerSubscriptionServiceMerchantAction(**params).run()

    def test_core_create_customer_subscription_action_call(self,
                                                           mock_core_action,
                                                           params,
                                                           returned,
                                                           service_client,
                                                           service_merchant,
                                                           service,
                                                           merchant):
        del params['service_tvm_id']
        mock_core_action.assert_called_once_with(
            **params,
            uid=merchant.uid,
            source=OrderSource.SERVICE,
            service_client_id=service_client.service_client_id,
            create_order_extra={
                'service_client_id': service_client.service_client_id,
                'service_merchant_id': service_merchant.service_merchant_id,
            },
            update_log_extra={
                'service_id': service.service_id,
                'service_name': service.name,
            },
        )

    def test_create_customer_subscription_service_merchant_action_returned(self,
                                                                           returned,
                                                                           customer_subscription_entity,
                                                                           order_entity):
        returned_customer_subscription, returned_order = returned
        assert returned_customer_subscription == customer_subscription_entity
        assert returned_order == order_entity
