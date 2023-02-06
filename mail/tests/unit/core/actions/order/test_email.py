import pytest

from mail.payments.payments.core.actions.interactions.so import SenderLetterIsSpamAction
from mail.payments.payments.core.actions.order.email import OrderEmailAction
from mail.payments.payments.core.actions.order.get_trust_env import GetOrderTrustEnvAction
from mail.payments.payments.core.entities.enums import TrustEnv
from mail.payments.payments.core.exceptions import (
    CoreActionDenyError, MerchantNotFoundError, OrderAlreadyEmailedError, OrderNotFoundError, SpamError
)
from mail.payments.payments.tests.base import BaseTestRequiresModeration
from mail.payments.payments.utils.helpers import decimal_format


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestOrderEmailAction(BaseTestRequiresModeration):
    @pytest.fixture
    def trust_env(self):
        return TrustEnv.PROD

    @pytest.fixture(autouse=True)
    def get_order_trust_env_action_mock(self, mock_action, trust_env):
        return mock_action(GetOrderTrustEnvAction, trust_env)

    @pytest.fixture(params=(1, 5))
    def letters_count(self, request):
        return request.param

    @pytest.fixture
    def campaign(self, randn, rands, letters_count):
        return {
            'letters': [{"code": "A", "id": rands()} for _ in range(letters_count)],
            "id": f"{randn()}",
            "submitted_by": "noname",
            "title": "test",
            "slug": rands()
        }

    @pytest.fixture
    def spam_verdict(self):
        return False

    @pytest.fixture(params=(True, False))
    def spam_check(self, request):
        return request.param

    @pytest.fixture
    def order_id(self, order):
        return order.order_id

    @pytest.fixture
    def uid(self, merchant):
        return merchant.uid

    @pytest.fixture
    def message_id(self, rands):
        return rands()

    @pytest.fixture
    def crypto_value(self, rands):
        return rands()

    @pytest.fixture
    def crypto(self, mocker, crypto_value):
        mock = mocker.Mock()
        mock.encrypt_order.return_value = crypto_value
        mock.encrypt_payment.return_value = crypto_value
        OrderEmailAction.context.crypto = mock
        return mock

    @pytest.fixture
    def params(self, uid, order_id, crypto, randmail, spam_check, rands):
        return {
            'uid': uid,
            'order_id': order_id,
            'to_email': randmail(),
            'reply_email': randmail(),
            'user_ip': rands(),
            'spam_check': spam_check,
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await OrderEmailAction(**params).run()

        return _inner

    @pytest.fixture(autouse=True)
    def mock_sender_campaign_detail(self, sender_client_mocker, campaign, message_id):
        with sender_client_mocker('campaign_detail', multiple_calls=True, result=campaign) as mock:
            yield mock

    @pytest.fixture
    def body(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def mock_sender_render_transactional_letter(self, sender_client_mocker, body):
        with sender_client_mocker('render_transactional_letter', multiple_calls=True, result=body) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def mock_sender_send_transactional_letter(self, sender_client_mocker, message_id):
        with sender_client_mocker('send_transactional_letter', multiple_calls=True, result=message_id) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def mock_so(self, so_client_mocker, spam_verdict):
        with so_client_mocker('form_is_spam', multiple_calls=True, result=spam_verdict) as mock:
            yield mock

    @pytest.fixture
    def render_context(self, crypto_value, payments_settings, items, order, merchant):
        return {
            'company_name': merchant.organization.full_name,
            'company_short_name': merchant.organization.name,
            'uid': merchant.uid,
            'order_id': order.order_id,
            'items': [
                {
                    'number': number,
                    'name': item.product.name,
                    'price': decimal_format(item.price),
                    'amount': decimal_format(item.amount),
                    'total': decimal_format(item.total_price),
                } for number, item in
                enumerate(sorted(items, key=lambda x: x.product.product_id), 1)
            ],
            'total_price': decimal_format(OrderEmailAction._items_price(items)),
            'pay_href': f"{payments_settings.FRONT_PAYMENT_URL.strip('/')}/{crypto_value}"
        }

    @pytest.mark.asyncio
    async def test_send_singleton(self, mock_sender_send_transactional_letter, mock_so, rands, returned_func, storage,
                                  order):
        email_context = {rands(): rands()}
        order.email_context = email_context
        await storage.order.save(order)

        with pytest.raises(OrderAlreadyEmailedError):
            await returned_func()

        mock_sender_send_transactional_letter.assert_not_called()
        mock_so.assert_not_called()

    @pytest.mark.parametrize('spam_verdict,spam_check', ((True, True),))
    @pytest.mark.asyncio
    async def test_is_spam(self, returned_func):
        with pytest.raises(SpamError):
            await returned_func()

    @pytest.mark.asyncio
    async def test_send(self,
                        uid,
                        order_id,
                        render_context,
                        message_id,
                        payments_settings,
                        mock_so,
                        mock_sender_send_transactional_letter,
                        params,
                        storage,
                        returned_func,
                        spam_check,
                        body,
                        request_id,
                        ):
        email_context = {
            'mailing_id': payments_settings.SENDER_MAILING_ORDER_EMAIL,
            'to_email': params['to_email'],
            'render_context': render_context,
            'reply_email': params['reply_email'],
        }

        await returned_func()

        if spam_check:
            _, fields = SenderLetterIsSpamAction._flat_context(render_context, hash_keys=True)

            mock_so.assert_called_with(
                request_id=request_id,
                form_id=payments_settings.SENDER_MAILING_ORDER_EMAIL,
                user_ip=params['user_ip'],
                to_email=params['to_email'],
                from_email=params['reply_email'],
                from_uid=params['uid'],
                fields=fields,
                body_template=body,
                body=body,
            )

        mock_sender_send_transactional_letter.assert_called_with(**email_context)

        returned_order = await storage.order.get(uid=uid, order_id=order_id)
        assert all((
            returned_order.email_message_id == message_id,
            returned_order.email_context == email_context,
        ))

    @pytest.mark.asyncio
    async def test_send_to_history_task_created(self, storage, order, returned):
        task = await (storage.task.find()).__anext__()
        assert all((
            task.action_name == 'send_to_history_order_action',
            task.params['action_kwargs'] == {'uid': order.uid, 'order_id': order.order_id},
        ))

    class TestActionDeny(BaseTestRequiresModeration.TestActionDeny):
        @pytest.fixture(params=TrustEnv)
        def trust_env(self, request):
            return request.param

        @pytest.mark.asyncio
        async def test_action_deny__raises_error(self, trust_env, returned_func, noop_manager):
            manager = pytest.raises(CoreActionDenyError) if trust_env == TrustEnv.PROD else noop_manager()
            with manager:
                await returned_func()

    class TestNotFound:
        @pytest.mark.parametrize('uid', (-1,))
        @pytest.mark.asyncio
        async def test_merchant_not_found(self, returned_func):
            with pytest.raises(MerchantNotFoundError):
                await returned_func()

        @pytest.mark.parametrize('order_id', (-1,))
        @pytest.mark.asyncio
        async def test_order_not_found(self, returned_func):
            with pytest.raises(OrderNotFoundError):
                await returned_func()

        @pytest.mark.usefixtures('order_with_customer_subscription')
        @pytest.mark.asyncio
        async def test_customer_subscription(self, returned_func):
            with pytest.raises(OrderNotFoundError):
                await returned_func()

        class TestOrderWithCustomerSubscription:
            @pytest.fixture
            def params(self, params):
                return {
                    **params,
                    'select_customer_subscription': None,
                }

            @pytest.mark.asyncio
            async def test_no_error(self,
                                    returned_func,
                                    order_with_customer_subscription,
                                    spam_check):
                await returned_func()
