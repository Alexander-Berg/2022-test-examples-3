import pytest

from sendr_utils import alist, anext

from hamcrest import all_of, assert_that, has_properties, is_

from mail.payments.payments.core.actions.init_products import InitProductsAction
from mail.payments.payments.core.actions.mixins.notify import NotifyMixin
from mail.payments.payments.core.actions.service_merchant.create import CreateServiceMerchantAction
from mail.payments.payments.core.actions.transact_email import TransactEmailAction
from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.core.entities.log import ServiceMerchantCreatedLog
from mail.payments.payments.core.entities.service import ServiceOptions
from mail.payments.payments.core.entities.user import User
from mail.payments.payments.core.entities.user_role import UserRole
from mail.payments.payments.core.exceptions import (
    CoreActionDenyError, CoreAlreadyExists, CoreMerchantUserNotAuthorizedError, CoreNotFoundError,
    ServiceMerchantAlreadyExists
)


@pytest.fixture(autouse=True)
def balance_get_person_mock(balance_client_mocker, person_entity):
    with balance_client_mocker('get_person', person_entity) as mock:
        yield mock


@pytest.fixture
def entity_id():
    return 'test entity_id'


@pytest.fixture
def description():
    return 'test description'


@pytest.fixture
def returned_func(params):
    async def _inner():
        return await CreateServiceMerchantAction(**params).run()

    return _inner


class BaseTestCreate:
    @pytest.fixture(params=('merchant', 'merchant_preregistered'))
    def merchant_type(self, request):
        return request.param

    @pytest.fixture
    def merchant(self, merchant_type, merchant, merchant_preregistered):
        return {
            'merchant': merchant,
            'merchant_preregistered': merchant_preregistered
        }[merchant_type]

    @pytest.fixture
    def autoenable(self):
        return False

    @pytest.fixture(autouse=True)
    def mock_init_products(self, mock_action):
        return mock_action(InitProductsAction)

    @pytest.fixture
    def service_fee(self):
        return None

    @pytest.fixture
    def service_options(self, service_fee):
        return ServiceOptions(service_fee=service_fee)

    @pytest.fixture(autouse=True)
    async def mock(self, mocker):
        mocker.spy(NotifyMixin, 'notify_merchant_on_sm_created_enabled')
        mocker.spy(TransactEmailAction, 'run_async')

    @pytest.fixture
    async def created_service_merchant(self, storage, returned):
        return await storage.service_merchant.get(returned[0].service_merchant_id, fetch_service=False)

    def test_returned(self, returned, merchant, service, entity_id, description, autoenable):
        assert_that(
            returned[0],
            has_properties({
                'uid': merchant.uid,
                'service_id': service.service_id,
                'entity_id': entity_id,
                'description': description,
                'enabled': autoenable,
                'service': service,
            })
        )

    def test_created(self, returned, created_service_merchant, merchant):
        assert created_service_merchant == returned[0]
        assert merchant.uid == returned[1].uid

    def test_merchant_notify_method_called(self, returned, created_service_merchant):
        NotifyMixin.notify_merchant_on_sm_created_enabled.assert_called_once()

    @pytest.mark.asyncio
    async def test_single_task_scheduled(self, storage, merchant, returned):
        tasks = await alist(storage.task.find())
        assert len(tasks) == (1 if merchant.contact else 0)

    @pytest.mark.asyncio
    @pytest.mark.parametrize('merchant_type', ['merchant'])
    async def test_transact_email_action_scheduled(self,
                                                   storage,
                                                   merchant,
                                                   service,
                                                   returned,
                                                   payments_settings,
                                                   created_service_merchant,
                                                   ):
        task = await anext(storage.task.find())
        assert task.params['action_kwargs'] == {
            'mailing_id': payments_settings.SENDER_MAILING_SERVICE_MERCHANT_CREATED,
            'render_context': {
                'service': {
                    'name': service.name,
                    'service_id': created_service_merchant.service_id,
                },
                'service_merchant': {
                    'service_merchant_id': created_service_merchant.service_merchant_id,
                    'description': created_service_merchant.description,
                    'enabled': created_service_merchant.enabled,
                    'entity_id': created_service_merchant.entity_id,
                }
            },
            'to_email': merchant.contact.email
        }

    def test_service_merchant_created_logged(self, merchant, service, created_service_merchant, returned, pushers_mock):
        assert_that(
            pushers_mock.log.push.call_args[0][0],
            all_of(
                is_(ServiceMerchantCreatedLog),
                has_properties(dict(
                    merchant_uid=merchant.uid,
                    service_id=service.service_id,
                    service_name=service.name,
                    service_merchant_id=created_service_merchant.service_merchant_id,
                    enabled=created_service_merchant.enabled,
                )),
            ),
        )

    def test_init_products_not_call(self, returned, mock_init_products):
        mock_init_products.assert_not_called()

    @pytest.mark.parametrize('service_fee', [1])
    def test_init_products_call(self, returned, service_fee, merchant, mock_init_products):
        mock_init_products.assert_called_once_with(merchant=returned[1], service_fee=service_fee)

    class TestNotAllowCreateServiceMerchants:
        @pytest.fixture(autouse=True)
        async def setup(self, merchant, storage):
            merchant.options.allow_create_service_merchants = False
            await storage.merchant.save(merchant)

        @pytest.mark.asyncio
        async def test_allow_create_service_merchants(self, returned_func):
            with pytest.raises(CoreActionDenyError):
                await returned_func()


class TestCreateByToken(BaseTestCreate):
    @pytest.fixture
    def params(self, merchant, service_client, entity_id, description):
        return {
            'token': merchant.token,
            'service_tvm_id': service_client.tvm_id,
            'entity_id': entity_id,
            'description': description
        }


class TestCreateByUIDAndToken(BaseTestCreate):
    @pytest.fixture(params=[MerchantRole.ADMIN, MerchantRole.OWNER])
    def role(self, request):
        return request.param

    @pytest.fixture
    def autoenable(self):
        return True

    @pytest.fixture
    async def user_role(self, merchant, randn, role, storage):
        uid = randn()
        user = await storage.user.create(User(uid=uid, email=f'{uid}@ya.ru'))
        user_role = await storage.user_role.create(UserRole(uid=user.uid, merchant_id=merchant.merchant_id, role=role))
        return user_role

    @pytest.fixture
    def params(self, merchant, user_role, service_client, entity_id, description, autoenable):
        return {
            'uid': user_role.uid,
            'token': merchant.token,
            'autoenable': autoenable,
            'service_tvm_id': service_client.tvm_id,
            'entity_id': entity_id,
            'description': description
        }


class BaseTestNotFound:
    @pytest.mark.asyncio
    async def test_raises_error(self, returned_func):
        with pytest.raises(CoreNotFoundError):
            await returned_func()


class TestInvalidTokenNotFound(BaseTestNotFound):
    @pytest.fixture
    def params(self, merchant, service_client, entity_id, description):
        return {
            'token': 'some invalid token',
            'service_tvm_id': service_client.tvm_id,
            'entity_id': entity_id,
            'description': description
        }

    @pytest.mark.asyncio
    async def test_raises_error(self, returned_func):
        with pytest.raises(CoreNotFoundError):
            await returned_func()


class TestServiceNotFound(BaseTestNotFound):
    @pytest.fixture
    def params(self, randn, unique_rand, merchant, service, entity_id, description):
        return {
            'token': merchant.token,
            'service_tvm_id': unique_rand(randn, basket='tvm_id'),
            'entity_id': entity_id,
            'description': description
        }

    @pytest.mark.asyncio
    async def test_raises_error(self, returned_func):
        with pytest.raises(CoreNotFoundError):
            await returned_func()


class TestServiceNotFoundWithoutServiceTvmId(BaseTestNotFound):
    @pytest.fixture
    def params(self, merchant, service, entity_id, description):
        return {
            'token': merchant.token,
            'entity_id': entity_id,
            'description': description,
            'service_tvm_id': None
        }


class TestNotAllowed:
    @pytest.fixture
    def params(self, merchant, user_role, service_client, entity_id, description, autoenable):
        return {
            'uid': user_role.uid,
            'token': merchant.token,
            'autoenable': autoenable,
            'service_tvm_id': service_client.tvm_id,
            'entity_id': entity_id,
            'description': description
        }

    @pytest.fixture(params=[MerchantRole.VIEWER, MerchantRole.OPERATOR])
    def role(self, request):
        return request.param

    @pytest.fixture
    def autoenable(self):
        return True

    @pytest.fixture
    async def user_role(self, merchant, randn, role, storage):
        uid = randn()
        user = await storage.user.create(User(uid=uid, email=f'{uid}@ya.ru'))
        user_role = await storage.user_role.create(UserRole(uid=user.uid, merchant_id=merchant.merchant_id,
                                                            role=role))
        return user_role

    @pytest.mark.asyncio
    async def test_raises_error(self, returned_func):
        with pytest.raises(CoreMerchantUserNotAuthorizedError):
            await returned_func()


class TestServiceMerchantAlreadyExists:
    @pytest.fixture(autouse=True)
    async def mock(self, mocker):
        mocker.spy(NotifyMixin, 'notify_merchant_on_sm_created_enabled')

    @pytest.fixture
    def params(self, service_client, merchant, service_merchant):
        return {
            'uid': service_merchant.uid,
            'token': merchant.token,
            'service_tvm_id': service_client.tvm_id,
            'entity_id': service_merchant.entity_id,
            'description': service_merchant.description
        }

    @pytest.mark.asyncio
    async def test_raises_error(self, returned_func):
        with pytest.raises(CoreAlreadyExists):
            await returned_func()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('enabled', (True, False))
    async def test_call_notify_count_by_enabled(self, enabled, storage, service_merchant, returned_func):
        service_merchant.enabled = enabled
        await storage.service_merchant.save(service_merchant)

        try:
            await returned_func()
        except ServiceMerchantAlreadyExists:
            pass

        assert NotifyMixin.notify_merchant_on_sm_created_enabled.call_count == (0 if enabled else 1)
