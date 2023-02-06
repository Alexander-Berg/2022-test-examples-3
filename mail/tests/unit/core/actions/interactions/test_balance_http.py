import mock
import pytest

from mail.payments.payments.core.actions.order.get import GetInternalOrderPayoutInfoAction
from mail.payments.payments.core.entities.enums import TransactionStatus


class TestBalanceHttp:
    @pytest.fixture(autouse=True)
    def get_payouts_by_purchase_token_mock(
        self,
        balance_http_client_mocker,
        mocker,
        payouts_data,
        payouts_data_composite,
        transaction
    ):
        def get_payouts_by_purchase_token(purchase_token):
            if purchase_token == transaction.trust_purchase_token:
                return payouts_data_composite
            else:
                return payouts_data

        return mocker.patch(
            'mail.payments.payments.interactions.balance_http.BalanceHttpClient.get_payouts_by_purchase_token',
            side_effect=get_payouts_by_purchase_token
        )

    @pytest.fixture
    def transaction_data(self):
        return {"status": TransactionStatus.CLEARED}

    @pytest.fixture
    async def returned(self, order_with_service, transaction, items, service_client):
        return await GetInternalOrderPayoutInfoAction(
            service_tvm_id=service_client.tvm_id,
            service_merchant_id=order_with_service.service_merchant_id,
            order_id=order_with_service.order_id
        ).run()

    @pytest.mark.asyncio
    async def test_get_payouts_by_purchase_token(
        self,
        get_payouts_by_purchase_token_mock,
        returned,
        transaction,
        payouts_data_composite,
    ):
        assert get_payouts_by_purchase_token_mock.call_args_list == [
            mock.call(transaction.trust_purchase_token),
            mock.call(payouts_data_composite["payments"][0]["child_payments"][0]),
            mock.call(payouts_data_composite["payments"][0]["child_payments"][1]),
        ]
