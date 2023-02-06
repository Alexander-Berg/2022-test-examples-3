import pytest

from mail.payments.payments.core.entities.enums import PaymentsTestCase, PayStatus, TransactionStatus


@pytest.mark.usefixtures('moderation', 'balance_person_mock')
class TestPaymentFailedOrder:
    @pytest.fixture
    def test_param(self):
        return PaymentsTestCase.TEST_PAYMENT_FAILED

    def test_success_test_order_created(self, test_order, test_param):
        assert test_order.test == test_param

    @pytest.mark.asyncio
    async def test_pay(self, pay_response, payments_settings):
        assert pay_response['trust_url'] == payments_settings.TEST_PAYMENT_URL

    @pytest.mark.asyncio
    async def test_transaction_after_pay(self, pay_response, storage, test_order):
        transaction = await storage.transaction.get_last_by_order(
            uid=test_order.uid,
            order_id=test_order.order_id,
            raise_=False,
        )
        assert all((transaction.status == TransactionStatus.ACTIVE, transaction.poll))

    @pytest.mark.asyncio
    async def test_status_if_update_transaction(self, pay_response, storage, test_order, run_transaction, test_param):
        transaction = await storage.transaction.get_last_by_order(
            uid=test_order.uid,
            order_id=test_order.order_id,
            raise_=False,
        )
        transaction = await run_transaction(transaction)
        order = await storage.order.get(test_order.uid, test_order.order_id)
        assert all((transaction.status == TransactionStatus.FAILED,
                    order.pay_status == PayStatus.REJECTED))
