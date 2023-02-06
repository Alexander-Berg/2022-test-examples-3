import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.enums import PaymentsTestCase, PayStatus, TransactionStatus


@pytest.mark.usefixtures('moderation', 'balance_person_mock')
class TestSuccessOrder:
    @pytest.fixture(params=[PaymentsTestCase.TEST_OK_CLEAR, PaymentsTestCase.TEST_OK_HELD])
    def test_param(self, request):
        return request.param

    @pytest.fixture
    async def refund_result(self,
                            test_order,
                            client,
                            service_merchant,
                            order_data,
                            pay_response,
                            storage,
                            run_transaction):
        transaction = await storage.transaction.get_last_by_order(
            uid=test_order.uid,
            order_id=test_order.order_id,
            raise_=False,
        )
        await run_transaction(transaction)
        response = await client.post(
            f'/v1/internal/order/{service_merchant.service_merchant_id}/{test_order.order_id}/refund',
            json=order_data
        )
        return await response.json()

    def test_success_test_order_created(self, test_order, test_param):
        assert test_order.test == test_param

    @pytest.mark.asyncio
    async def test_pay(self, pay_response, payments_settings, test_param):
        assert pay_response['trust_url'] == payments_settings.TEST_PAYMENT_URL

    @pytest.mark.asyncio
    async def test_transaction_after_pay(self, pay_response, storage, test_order):
        transaction = await storage.transaction.get_last_by_order(
            uid=test_order.uid,
            order_id=test_order.order_id,
            raise_=False,
        )
        assert all((transaction.status == TransactionStatus.ACTIVE,
                    transaction.poll))

    @pytest.mark.parametrize('test_param', [PaymentsTestCase.TEST_OK_HELD])
    @pytest.mark.asyncio
    async def test_moderation_approved_if_update_transaction(self,
                                                             pay_response,
                                                             storage,
                                                             test_order,
                                                             run_transaction):
        transaction = await storage.transaction.get_last_by_order(
            uid=test_order.uid,
            order_id=test_order.order_id,
            raise_=False,
        )
        transaction = await run_transaction(transaction)
        moderation = await storage.moderation.get_for_order(uid=test_order.uid, order_id=test_order.order_id)
        order = await storage.order.get(test_order.uid, test_order.order_id)
        assert all((transaction.status == TransactionStatus.HELD,
                    transaction.poll,
                    moderation.approved,
                    order.pay_status == PayStatus.IN_MODERATION))

    @pytest.mark.parametrize('test_param', [PaymentsTestCase.TEST_OK_CLEAR])
    @pytest.mark.asyncio
    async def test_refund_success_test_ok_clear(self, refund_result, test_order, order_data):
        assert_that(refund_result, has_entries({
            'status': 'success',
            'data': has_entries({
                'original_order_id': test_order.order_id,
                'refund_status': 'completed',
            })
        }))

    @pytest.mark.parametrize('test_param', [
        PaymentsTestCase.TEST_PAYMENT_FAILED,
        PaymentsTestCase.TEST_OK_HELD,
        PaymentsTestCase.TEST_PAYMENT_FAILED,
    ])
    @pytest.mark.asyncio
    async def test_refund_fail_on_test_order(self, refund_result):
        assert all((
            refund_result['status'] == 'fail',
            refund_result['data']['message'] == 'TEST_ORDER_CANNOT_BE_REFUNDED'
        ))
