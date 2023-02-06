import pytest

from hamcrest import assert_that, has_entries, has_properties

from mail.payments.payments.api.schemas.merchant import merchant_draft_request_schema
from mail.payments.payments.core.actions.merchant.create_entity import CreateMerchantEntityAction
from mail.payments.payments.core.actions.merchant.draft import (
    CreateMerchantDraftAction, CreateServiceMerchantDraftAction
)
from mail.payments.payments.core.entities.enums import AcquirerType, MerchantRole, OrderSource
from mail.payments.payments.core.entities.merchant import MerchantOptions
from mail.payments.payments.core.entities.service import OfferSettings, Service, ServiceMerchant, ServiceOptions
from mail.payments.payments.core.entities.userinfo import UserInfo
from mail.payments.payments.core.exceptions import MerchantCreateDeny
from mail.payments.payments.interactions.spark_suggest import SparkSuggestItem
from mail.payments.payments.storage.exceptions import UserRoleNotFound
from mail.payments.payments.tests.utils import MERCHANT_DATA_TEST_CASES


class BaseTestCreateMerchantDraftAction:
    @pytest.fixture(autouse=True)
    def blackbox_userinfo_mock(self, blackbox_client_mocker, merchant_draft_uid, rands):
        user_info = UserInfo(
            uid=merchant_draft_uid,
            default_email=rands(),
        )
        with blackbox_client_mocker('userinfo', user_info) as mock:
            yield mock

    @pytest.fixture
    def suggest_list(self):
        return [
            SparkSuggestItem(
                spark_id=1,
                name='Name',
                full_name='Full Name',
                inn='1234567890',
                ogrn='0987654321',
                address='Address',
                leader_name='Leader',
                region_name='Region',
            )
        ]

    @pytest.fixture(autouse=True)
    def spark_suggest_get_hint_mock(self, spark_suggest_client_mocker, suggest_list):
        with spark_suggest_client_mocker('get_hint', result=suggest_list) as mock:
            yield mock

    @pytest.fixture(params=MERCHANT_DATA_TEST_CASES)
    def test_data_draft(self, request):
        data, _ = merchant_draft_request_schema.load(request.param)
        return data

    @pytest.fixture
    def entity_id(self, rands):
        return rands()

    @pytest.fixture
    def params(self, entity_id, merchant_draft_uid, rands, service_client, test_data_draft):
        return {
            'uid': merchant_draft_uid,
            'name': 'some-name',
            **test_data_draft
        }

    @pytest.fixture
    def returned_func(self, action_class, params):
        async def _inner():
            return await action_class(**params).run()

        return _inner

    @pytest.fixture
    async def created_user_role(self, storage, returned):
        try:
            return await storage.user_role.get(
                uid=returned.uid,
                merchant_id=returned.merchant_id,
            )
        except UserRoleNotFound:
            return None


class TestCreateMerchantDraftAction(BaseTestCreateMerchantDraftAction):
    @pytest.fixture
    def action_class(self):
        return CreateMerchantDraftAction

    @pytest.fixture
    async def created_merchant(self, storage, returned):
        merchant = await storage.merchant.get(returned.uid)
        merchant.oauth = []
        return merchant

    def test_returned_uid_and_name(self, merchant_draft_uid, params, returned):
        assert_that(
            returned,
            has_properties({
                'uid': merchant_draft_uid,
                'name': params['name'],
                'data': None
            })
        )

    def test_returned_data(self, test_data_draft, returned):
        assert_that(
            returned.draft_data,
            has_entries({
                'username': test_data_draft.get('username'),
            })
        )

    def test_created(self, returned, created_merchant):
        assert created_merchant == returned

    def test_created_owner(self, created_user_role):
        assert created_user_role and created_user_role.role == MerchantRole.OWNER

    @pytest.fixture
    def spy_create_entity(self, mocker):
        return mocker.spy(CreateMerchantEntityAction, 'run')

    def test_create_entity_called(self, spy_create_entity, created_merchant):
        spy_create_entity.assert_called_once()


@pytest.mark.usefixtures('merchant_draft')
class TestUpdateMerchantDraftAction(BaseTestCreateMerchantDraftAction):
    @pytest.fixture
    def action_class(self):
        return CreateMerchantDraftAction

    @pytest.fixture
    async def created_merchant(self, storage, returned):
        merchant = await storage.merchant.get(returned.uid)
        merchant.oauth = []
        return merchant

    def test_created(self, returned, created_merchant):
        assert created_merchant == returned

    @pytest.mark.asyncio
    async def test_already_registred(self, storage, rands, merchant_draft, returned_func):
        service = await storage.service.create(Service(name=rands()))
        await storage.service_merchant.create(
            ServiceMerchant(uid=merchant_draft.uid,
                            service_id=service.service_id,
                            entity_id=rands(),
                            description=rands())
        )

        with pytest.raises(MerchantCreateDeny):
            await returned_func()


class BaseTestCreateServiceMerchantDraftAction(BaseTestCreateMerchantDraftAction):
    @pytest.fixture
    def required_acquirer(self, payments_settings):
        default_acquirer = AcquirerType(payments_settings.DEFAULT_ACQUIRER)
        for acquirer in AcquirerType:
            if acquirer != default_acquirer:
                return acquirer

    @pytest.fixture
    def action_class(self):
        return CreateServiceMerchantDraftAction

    @pytest.fixture
    def service_options(self, required_acquirer, randitem, rands, randbool):
        return ServiceOptions(
            allowed_order_sources=[randitem(OrderSource)],
            allow_create_service_merchants=randbool(),
            offer_settings=OfferSettings(
                pdf_template=rands(),
                slug=rands(),
                data_override={rands(): rands()},
            ),
            required_acquirer=required_acquirer,
            hide_commission=True
        )

    @pytest.fixture
    async def created_merchant(self, storage, returned):
        merchant = await storage.merchant.get(returned[1].uid)
        merchant.oauth = []
        return merchant

    @pytest.fixture
    def params(self, entity_id, merchant_draft_uid, rands, service_client, test_data_draft):
        return {
            'uid': merchant_draft_uid,
            'name': 'some-name',
            'service_tvm_id': service_client.tvm_id,
            'entity_id': entity_id,
            'description': rands(),
            **test_data_draft
        }


class TestCreateServiceMerchantDraftAction(BaseTestCreateServiceMerchantDraftAction):
    def test_pass_merchant_settings(self, returned, service_options):
        assert returned[1].options == MerchantOptions(
            allowed_order_sources=service_options.allowed_order_sources,
            allow_create_service_merchants=service_options.allow_create_service_merchants,
            hide_commission=service_options.hide_commission,
            offer_settings=service_options.offer_settings,
        )

    @pytest.mark.asyncio
    async def test_required_acquirer(self, required_acquirer, created_merchant, entity_id):
        assert created_merchant.acquirer == required_acquirer


@pytest.mark.usefixtures('merchant_draft')
class TestUpdateServiceMerchantDraftAction(BaseTestCreateServiceMerchantDraftAction):
    @pytest.mark.asyncio
    async def test_returned_uid_and_name(self, returned_func):
        with pytest.raises(MerchantCreateDeny):
            await returned_func()

    @pytest.mark.asyncio
    async def test_returned_data(self, returned_func):
        with pytest.raises(MerchantCreateDeny):
            await returned_func()

    @pytest.mark.asyncio
    async def test_created_service_merchant(self, returned_func):
        with pytest.raises(MerchantCreateDeny):
            await returned_func()
