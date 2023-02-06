from contextlib import contextmanager
from itertools import chain
from typing import Tuple

import pytest

from sendr_utils import alist, without_none

from hamcrest import assert_that, has_entries, has_item, has_properties

from mail.payments.payments.core.entities.document import Document
from mail.payments.payments.core.entities.enums import (
    AcquirerType, DocumentType, FunctionalityType, MerchantOAuthMode, MerchantRole, ModerationType, PayStatus, ShopType,
    TaskType, TransactionStatus
)
from mail.payments.payments.core.entities.moderation import Moderation, ModerationData
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.core.entities.user import User
from mail.payments.payments.core.entities.user_role import UserRole
from mail.payments.payments.core.exceptions import (
    CoreActionDenyError, OAuthAbsentError, TinkoffInvalidSubmerchantIdError
)
from mail.payments.payments.tests.utils import dummy_coro_generator

parametrize_acquirer = pytest.mark.parametrize('acquirer', list(AcquirerType))
parametrize_merchant_oauth_mode = pytest.mark.parametrize('merchant_oauth_mode', list(MerchantOAuthMode))
# parametrize shop_type and ensure correct OAuth token is created for merchant
parametrize_shop_type = pytest.mark.parametrize(
    ('shop_type', 'merchant_oauth_mode'),
    [
        (ShopType.PROD, MerchantOAuthMode.PROD),
        (ShopType.TEST, MerchantOAuthMode.TEST),
    ],
)


class BaseTestParent:
    @pytest.fixture(params=(
        pytest.param(False, id='without_parent'),
        pytest.param(True, id='with_parent'),
    ))
    def with_parent(self, request):
        return request.param

    @pytest.fixture
    def parent_uid(self, with_parent, parent_merchant):
        return parent_merchant.uid if with_parent else None


class BaseAcquirerTest:
    @pytest.fixture(params=list(AcquirerType))
    def acquirer(self, request):
        return request.param


class BaseOrderAcquirerTest:
    @pytest.fixture
    def order_acquirer(self, acquirer):
        for result in AcquirerType:
            if acquirer != result:
                return result


class BaseSubscriptionAcquirerTest:
    @pytest.fixture
    def subscription_acquirer(self, acquirer):
        for result in AcquirerType:
            if acquirer != result:
                return result


class BaseTestRequiresModeration(BaseTestParent):
    """
    Inherit this to add require_moderation check.
    """

    @pytest.fixture
    def action_deny_exception(self):
        return CoreActionDenyError

    @pytest.fixture
    def moderations_data(self):
        return [{'approved': True}]

    @pytest.fixture
    def approved_merchant_moderation(self, moderations):
        for moderation in moderations:
            if moderation.approved and moderation.moderation_type == ModerationType.MERCHANT:
                return moderation

    @pytest.fixture
    def approved_merchant_moderation_data(self, approved_merchant_moderation):
        return ModerationData(
            approved=approved_merchant_moderation.approved,
            reasons=approved_merchant_moderation.reasons,
            has_moderation=True,
            has_ongoing=False,
            updated=approved_merchant_moderation.updated,
        ) if approved_merchant_moderation else None

    @pytest.fixture(autouse=True)
    def setup_moderations(self, moderations):
        pass

    @pytest.fixture
    def returned_func(self):
        raise NotImplementedError

    class BaseTestActionDeny:
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
            pytest.param(
                [{'approved': True, 'functionality_type': FunctionalityType.YANDEX_PAY}],
                id='approved_with_other_functionality',
            ),
        ))
        def moderation_items(self, request):
            return request.param

        @pytest.fixture
        def moderations_data(self, moderation_items):
            return moderation_items

        async def check_action_deny__raises_error(self, action_deny_exception, moderations, returned_func):
            with pytest.raises(action_deny_exception):
                await returned_func()

    class TestActionDeny(BaseTestActionDeny):
        @pytest.mark.asyncio
        async def test_action_deny__raises_error(self, action_deny_exception, moderations, returned_func):
            await self.check_action_deny__raises_error(action_deny_exception, moderations, returned_func)


class BaseTestRequiresNoModeration:
    """
    Inherit this to add require_no_moderation check.
    """

    @pytest.fixture
    def action_deny_exception(self):
        return CoreActionDenyError

    @pytest.fixture
    def moderation_data(self):
        return []

    @pytest.fixture(autouse=True)
    def setup_moderations(self, moderations):
        pass

    @pytest.fixture
    def returned_func(self):
        raise NotImplementedError

    class TestActionDenyWhenHasModeration:
        @pytest.fixture(params=(
            pytest.param(
                [{'approved': True}],
                id='approved',
            ),
            pytest.param(
                [{'approved': False}, {'approved': True}],
                id='approved_after_disapprove',
            ),
            pytest.param(
                [{'approved': True}, {'approved': False, 'ignore': True}],
                id='approved_with_ignored_disapprove',
            )
        ))
        def moderations_data(self, request):
            return request.param

        @pytest.mark.asyncio
        async def test_raises_error(self, action_deny_exception, returned_func):
            with pytest.raises(action_deny_exception):
                await returned_func()


class BaseTrustCredentialsErrorTest:
    @pytest.mark.parametrize('acquirer', (AcquirerType.TINKOFF,))
    @pytest.mark.asyncio
    async def test_error_check_trust_credentials_tinkoff(self, storage, merchant, returned_func):
        for item in (merchant, merchant.parent):
            if item:
                item.submerchant_id = None
                await storage.merchant.save(item)

        with pytest.raises(TinkoffInvalidSubmerchantIdError):
            await returned_func()

    @pytest.mark.parametrize('acquirer', (AcquirerType.KASSA,))
    @pytest.mark.asyncio
    async def test_error_check_trust_credentials_kassa(self, order, storage, order_acquirer, merchant, action_params,
                                                       returned_func, noop_manager):
        manager = noop_manager
        payment_acquirer = order_acquirer or action_params['order'].acquirer
        if payment_acquirer == AcquirerType.KASSA:
            manager = pytest.raises

        for oauth in merchant.oauth:
            await storage.merchant_oauth.delete(oauth)
        merchant.oauth = []

        with manager(OAuthAbsentError):
            await returned_func()


class BaseTestOrderAction:
    @pytest.fixture
    def some_hash(self):
        return 'abcde_some_hash'

    @pytest.fixture(autouse=True)
    def add_crypto_mock(self, mocker, some_hash):
        def dummy(self, crypto_prefix, crypto):
            self.order_hash = some_hash

        return mocker.patch.object(Order, 'add_crypto', autospec=True, side_effect=dummy)

    @pytest.fixture(autouse=True)
    def fetch_items_mock(self, mocker, items):
        mocker.patch(
            'mail.payments.payments.core.actions.order.base.BaseOrderAction._fetch_items',
            mocker.Mock(side_effect=dummy_coro_generator(items)),
        )
        yield

    @pytest.fixture
    def crypto(self, mocker):
        return 'fake crypto'


class BaseTestOrderWithCustomerSubscriptionAction(BaseTestOrderAction):
    @pytest.fixture
    def items_amount(self):
        return 1


class BaseTestOrderHashAction(BaseTestOrderAction):
    @pytest.fixture
    def transaction_data(self):
        return {'status': TransactionStatus.CLEARED}

    @pytest.fixture
    def decrypted(self, order):
        return {
            'uid': order.uid,
            'order_id': order.order_id,
        }

    @pytest.fixture
    def crypto(self, mocker, decrypted):
        @contextmanager
        def dummy_decrypt(*args):
            yield decrypted

        mock = mocker.Mock()
        mock.decrypt_order = mock.decrypt_payment = dummy_decrypt
        return mock

    @pytest.fixture
    def update_transaction_mock(self, mock_action, transaction):
        from mail.payments.payments.core.actions.update_transaction import UpdateTransactionAction
        yield mock_action(UpdateTransactionAction, transaction)


class BaseTestOrder:
    @pytest.fixture(autouse=True)
    def crypto_setup(self, crypto_mock):
        from mail.payments.payments.core.actions.base.action import BaseAction
        BaseAction.context.crypto = crypto_mock

    @pytest.fixture
    def service_merchant_path(self, service_merchant, order):
        return f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}'

    @pytest.fixture
    def service_merchant_context(self, service_merchant, order, crypto_mock, tvm_client_id):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': tvm_client_id,
            'order_id': order.order_id,
        }

    @pytest.fixture
    def uid_path(self, order):
        return f'/v1/order/{order.uid}/{order.order_id}'

    @pytest.fixture
    def uid_context(self, order, crypto_mock):
        return {
            'uid': order.uid,
            'order_id': order.order_id,
        }

    @pytest.fixture
    def service_merchant_order_test_data(self, service_merchant_path, service_merchant_context):
        return {
            'path': service_merchant_path,
            'context': service_merchant_context
        }

    @pytest.fixture
    def uid_order_test_data(self, uid_path, uid_context):
        return {
            'path': uid_path,
            'context': uid_context
        }

    @pytest.fixture(params=(
        pytest.param(False, id='external'),
        pytest.param(True, id='internal'),
    ))
    def internal_api(self, request):
        return request.param

    @pytest.fixture
    def test_data(self, internal_api, service_merchant_order_test_data, uid_order_test_data):
        if internal_api:
            return service_merchant_order_test_data
        else:
            return uid_order_test_data


class BaseTestOrderList(BaseTestOrder):
    @pytest.fixture(autouse=True)
    def crypto_setup(self, crypto_mock):
        from mail.payments.payments.core.actions.base.action import BaseAction
        BaseAction.context.crypto = crypto_mock

    @pytest.fixture
    def service_merchant_path(self, service_merchant):
        return f'/v1/internal/order/{service_merchant.service_merchant_id}/'

    @pytest.fixture
    def service_merchant_context(self, service_merchant, crypto_mock, tvm_client_id):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': tvm_client_id,
        }

    @pytest.fixture
    def uid_path(self, order):
        return f'/v1/order/{order.uid}'

    @pytest.fixture
    def uid_context(self, order, crypto_mock):
        return {
            'uid': order.uid,
        }


class BaseTestMerchantRoles:
    ALLOWED_ROLES: Tuple[MerchantRole, ...] = ()

    def __init_subclass__(cls):
        params = []
        for role in chain((None,), MerchantRole):
            allow = role in cls.ALLOWED_ROLES
            marks = (pytest.mark.check_role(role, allow), )
            if not allow:
                marks = marks + (pytest.mark.xfail(strict=True), )
            params.append(
                pytest.param(
                    role,
                    id=role and role.value,
                    marks=marks
                )
            )
        cls.role = pytest.fixture(params=params)(lambda self, request: request.param)

    @pytest.fixture(autouse=True)
    def without_roles(self, request, payments_settings):
        marker = request.node.get_closest_marker('check_role')
        if marker:
            role, _ = marker.args
            if request.config.getoption('--without-roles'):
                payments_settings.CHECK_MERCHANT_USER = False
                if role is not None:
                    pytest.skip(msg='Skipped due to --without-roles')

    @pytest.fixture
    def user_uid(self, randn):
        return randn()

    @pytest.fixture
    async def user(self, storage, user_uid, rands):
        return await storage.user.create(User(uid=user_uid, email=rands()))

    @pytest.fixture
    def merchant_id(self, merchant_uid):
        return str(merchant_uid)

    @pytest.fixture
    async def user_role(self, storage, user, merchant_id, role):
        if role is None:
            return None
        return await storage.user_role.create(UserRole(
            uid=user.uid,
            merchant_id=merchant_id,
            role=role,
        ))

    @pytest.fixture
    def tvm_uid(self, user, user_role):
        return user.uid

    @pytest.fixture(autouse=True)
    def spark_suggest_get_hint_mock(self, spark_suggest_client_mocker):
        with spark_suggest_client_mocker('get_hint', result=[]) as mock:
            yield mock


class BaseTestDocumentByPathDownload:
    @pytest.fixture
    def merchant_documents(self, randn):
        return [
            Document(
                document_type=DocumentType.PROXY,
                path='test-document-download-path',
                size=randn(),
                name='test-document-download-name',
            )
        ]

    @pytest.fixture
    def content_type(self):
        return 'image/png'

    @pytest.fixture
    def chunks(self):
        return [b'aaa', b'bbb', b'ccc']

    @pytest.fixture(autouse=True)
    def mds_download_mock(self, mds_client_mocker, content_type, chunks):
        async def download():
            nonlocal chunks
            for chunk in chunks:
                yield chunk

        with mds_client_mocker('download', (content_type, download())) as mock:
            yield mock

    @pytest.fixture
    def path(self, merchant_documents):
        return merchant_documents[0].path

    @pytest.fixture
    async def response_data(self, download_response):
        return await download_response.read()

    @pytest.fixture
    async def not_found_error(self, download_response):
        return (await download_response.json())['data']['message']


class BaseSdkOrderTest(BaseAcquirerTest):
    @pytest.fixture
    def order_data(self, shop_type):
        return without_none({
            'caption': 'Some test order',
            'description': 'Some description',
            'items': [
                {
                    'name': 'first',
                    'amount': 2,
                    'price': 100,
                    'nds': 'nds_10_110',
                    'currency': 'RUB',
                },
                {
                    'name': 'second',
                    'amount': 3.33,
                    'price': 100.77,
                    'nds': 'nds_10',
                    'currency': 'RUB',
                },
            ],
            'mode': {ShopType.PROD: 'prod', ShopType.TEST: 'test'}[shop_type],
        })

    @pytest.fixture
    async def order_response(self, moderation, sdk_client, order_data):
        r = await sdk_client.post('/v1/order', json=order_data)
        assert r.status == 200
        return await r.json()

    @pytest.fixture
    async def order(self, storage, merchant, order_response):
        data = order_response['data']
        order = await storage.order.get(uid=merchant.uid, order_id=data['order_id'])
        order.items = await alist(storage.item.get_for_order(uid=order.uid, order_id=order.order_id))
        return order


class BaseSdkRefundTest(BaseSdkOrderTest):
    @pytest.fixture
    def customer_uid(self, randn):
        return randn()

    @pytest.fixture
    def trust_purchase_token(self, rands):
        return rands()

    @pytest.fixture
    def trust_refund_id(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    async def setup(self, storage, moderation, order, trust_purchase_token):
        await storage.transaction.create(Transaction(
            uid=order.uid,
            order_id=order.order_id,
            status=TransactionStatus.CLEARED,
            trust_purchase_token=trust_purchase_token,
        ))
        order.pay_status = PayStatus.PAID
        await storage.order.save(order)

    @pytest.fixture(autouse=True)
    def payment_get_mock(self, shop_type, trust_client_mocker, customer_uid):
        with trust_client_mocker(shop_type, 'payment_get', {'uid': customer_uid}) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def refund_create_mock(self, shop_type, trust_client_mocker, trust_refund_id):
        with trust_client_mocker(shop_type, 'refund_create', trust_refund_id) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def refund_start_mock(self, shop_type, trust_client_mocker, trust_refund_id):
        with trust_client_mocker(shop_type, 'refund_start', trust_refund_id) as mock:
            yield mock

    @pytest.fixture
    def refund_data(self, order_data):
        return order_data

    @pytest.fixture
    def make_refund_response(self, moderation, order, sdk_client, refund_data):
        async def _inner():
            r = await sdk_client.post(f'/v1/order/{order.order_id}/refund', json=refund_data)
            return await r.json()

        return _inner

    @pytest.fixture
    async def refund_response(self, make_refund_response):
        return await make_refund_response()

    @pytest.fixture
    async def refund(self, storage, order, merchant, refund_response):
        data = refund_response['data']
        refund = await storage.order.get(uid=merchant.uid, order_id=data['refund_id'], original_order_id=order.order_id)
        refund.items = await alist(storage.item.get_for_order(uid=refund.uid, order_id=refund.order_id))
        return refund


class BaseTestClearUnholdOrder(BaseAcquirerTest):
    @pytest.fixture(autouse=True)
    def trust_get_mock(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'payment_get', {}) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def trust_clear_mock(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'payment_clear', {}) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def trust_unhold_mock(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'payment_unhold', {}) as mock:
            yield mock

    @pytest.fixture(params=(PayStatus.HELD, PayStatus.IN_MODERATION))
    def order_pay_status(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    async def setup(self, storage, moderation, order, order_pay_status, rands):
        order.pay_status = order_pay_status
        order.autoclear = False
        await storage.order.save(order)
        await storage.transaction.create(Transaction(
            uid=order.uid,
            order_id=order.order_id,
            status=TransactionStatus.HELD,
            poll=False,
            trust_purchase_token=rands(),
        ))
        if order_pay_status == PayStatus.IN_MODERATION:
            await storage.moderation.create(Moderation(
                uid=order.uid,
                moderation_type=ModerationType.ORDER,
                entity_id=order.order_id,
                revision=order.revision,
            ))

    @pytest.fixture(params=('clear', 'unhold'))
    def operation(self, request):
        return request.param

    @pytest.fixture
    async def response(self):
        raise NotImplementedError

    @pytest.fixture
    async def response_data(self, response):
        return (await response.json())['data']

    def test_returns_empty_data(self, response_data):
        assert response_data == {}

    def test_status_ok(self, response):
        assert response.status == 200

    class TestPayStatus:
        @pytest.fixture(params=(PayStatus.NEW, PayStatus.PAID, PayStatus.IN_PROGRESS))
        def order_pay_status(self, request):
            return request.param

        def test_returned_error_message(self, response_data):
            assert response_data['message'] == 'ORDER_PAY_STATUS_MUST_BE_HELD_OR_IN_MODERATION'


class BaseTaskTest:
    @pytest.fixture(params=('clear', 'unhold'))
    def operation(self, request):
        return request.param

    @pytest.fixture
    async def response(self):
        raise NotImplementedError

    def action_name(self):
        raise NotImplementedError

    def params(self):
        raise NotImplementedError

    @pytest.fixture
    def validated_items(self):
        return None

    @pytest.mark.asyncio
    async def test_task(self, params, operation, action_name, order, response, tasks):
        assert_that(
            tasks,
            has_item(has_properties({
                'params': has_entries(
                    action_kwargs=params
                ),
                'action_name': action_name,
                'task_type': TaskType.RUN_ACTION,
            }))
        )
