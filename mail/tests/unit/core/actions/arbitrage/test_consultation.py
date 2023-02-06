import pytest

from hamcrest import assert_that, has_properties

from mail.payments.payments.core.actions.arbitrage.consultation import ConsultationArbitrageAction
from mail.payments.payments.core.actions.order.get import CoreGetOrderAction
from mail.payments.payments.core.entities.enums import PayStatus
from mail.payments.payments.core.exceptions import (
    ActiveArbitrageAlreadyExistError, MerchantNotConnectedToDialogsError, OrderAnonymousError, OrderInvalidKind,
    OrderMustBePaidError, OrderNotFoundError
)
from mail.payments.payments.tests.base import BaseTestOrderAction


class TestCreateConsultationAction(BaseTestOrderAction):
    @pytest.fixture
    def consultation_id(self, rands):
        return rands()

    @pytest.fixture
    def chat_id(self, rands):
        return rands()

    @pytest.fixture
    def customer_uid(self, randn):
        return randn()

    @pytest.fixture
    def pay_status(self, randn):
        return PayStatus.PAID

    @pytest.fixture
    def order_id(self, order):
        return order.order_id

    @pytest.fixture
    def order_data(self, pay_status, customer_uid):
        return {
            'pay_status': pay_status,
            'customer_uid': customer_uid
        }

    @pytest.fixture(autouse=True)
    def create_consultation_mock(self, floyd_client_mocker, chat_id, consultation_id):
        result = {'id': consultation_id, 'chats': {'clients': {'chat_id': chat_id}}}
        with floyd_client_mocker('create_consultation', result) as mock:
            yield mock

    @pytest.fixture
    def returned_func(self, order, crypto, order_id, customer_uid):
        async def _inner():
            CoreGetOrderAction.context.crypto = crypto

            return await ConsultationArbitrageAction(order.uid, order_id, customer_uid).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('order_id', (-1,))
    async def test_not_found(self, returned_func):
        with pytest.raises(OrderNotFoundError):
            await returned_func()

    @pytest.mark.asyncio
    async def test_already_arbitrage(self, arbitrage, returned_func):
        with pytest.raises(ActiveArbitrageAlreadyExistError):
            await returned_func()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('pay_status', (PayStatus.NEW,))
    async def test_must_paid(self, returned_func):
        with pytest.raises(OrderMustBePaidError):
            await returned_func()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('dialogs_org_id', (None,))
    async def test_merchant_not_connected(self, returned_func):
        with pytest.raises(MerchantNotConnectedToDialogsError):
            await returned_func()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('customer_uid', (None,))
    async def test_anonymous(self, returned_func):
        with pytest.raises(OrderAnonymousError):
            await returned_func()

    def test_returned(self, returned, order, chat_id, consultation_id):
        assert_that(returned, has_properties({
            'uid': order.uid,
            'order_id': order.order_id,
            'consultation_id': consultation_id,
            'chat_id': chat_id
        }))

    class TestInvalidKind:
        @pytest.fixture
        def order_id(self, multi_order):
            return multi_order.order_id

        @pytest.mark.asyncio
        async def test_invalid_kind(self, returned_func):
            with pytest.raises(OrderInvalidKind):
                await returned_func()

    class TestInvalidKindSubscription:
        @pytest.fixture
        def order_id(self, order_with_customer_subscription):
            return order_with_customer_subscription.order_id

        @pytest.mark.asyncio
        async def test_invalid_kind_subscription(self, returned_func):
            with pytest.raises(OrderInvalidKind):
                await returned_func()
