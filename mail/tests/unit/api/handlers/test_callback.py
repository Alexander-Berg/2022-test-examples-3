import pytest


class TestCallbackPost:
    @pytest.fixture
    def action(self, mock_action):
        from mail.payments.payments.core.actions.order.sync import SyncOrderAction
        return mock_action(SyncOrderAction)

    @pytest.fixture
    async def response(self, action, payments_client, order):
        return await payments_client.post(f'/callback/payment/{order.uid}/{order.order_id}')

    def test_context(self, order, response, action):
        action.assert_called_once_with(uid=order.uid, order_id=order.order_id)
