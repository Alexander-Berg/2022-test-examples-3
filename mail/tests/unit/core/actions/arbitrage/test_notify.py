import pytest

from mail.payments.payments.core.actions.arbitrage.notify import NotifyArbitrageAction
from mail.payments.payments.core.entities.enums import ArbitrageVerdict, RefundStatus


class TestNotifyArbitrageAction:
    @pytest.fixture(params=(ArbitrageVerdict.REFUND, ArbitrageVerdict.DECLINE, None))
    def verdict(self, request):
        return request.param

    @pytest.fixture
    def arbitrage_id(self, arbitrage):
        return arbitrage.arbitrage_id

    @pytest.fixture
    def arbitrage_data(self, verdict):
        return {'verdict': verdict}

    @pytest.fixture
    def refund_id(self, refund):
        return refund.order_id

    @pytest.fixture
    def refund_data(self):
        return {'refund_status': RefundStatus.COMPLETED}

    @pytest.fixture(autouse=True)
    async def setup(self, arbitrage, refund_id, storage):
        arbitrage.refund_id = refund_id
        await storage.arbitrage.save(arbitrage)

    @pytest.fixture(autouse=True)
    def execute_verdict_mock(self, arbiter_client_mocker):
        with arbiter_client_mocker('execute_verdict') as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def custom_action_mock(self, floyd_client_mocker):
        with floyd_client_mocker('custom_action') as mock:
            yield mock

    @pytest.fixture
    def returned_func(self, arbitrage_id):
        async def _inner():
            return await NotifyArbitrageAction(arbitrage_id).run()

        return _inner

    @pytest.mark.parametrize('arbitrage_id', [-1])
    def test_not_found(self, returned, execute_verdict_mock):
        execute_verdict_mock.assert_not_called()

    @pytest.mark.parametrize('arbitrage_data', [{'escalate_id': None}])
    def test_escalate_id_none(self, returned, execute_verdict_mock):
        execute_verdict_mock.assert_not_called()

    @pytest.mark.parametrize('refund_id', [None])
    def test_no_refund(self, returned, arbitrage, execute_verdict_mock):
        execute_verdict_mock.assert_not_called()

    @pytest.mark.parametrize('refund_data', [{}])
    def test_refund_not_completed(self, returned, arbitrage, execute_verdict_mock):
        execute_verdict_mock.assert_not_called()

    def test_returned(self, mocker, verdict, returned, arbitrage, execute_verdict_mock):
        calls = []
        if verdict == ArbitrageVerdict.REFUND:
            calls.append(mocker.call(conversation_id=arbitrage.escalate_id))
        execute_verdict_mock.assert_has_calls(calls)

    def test_custom_action_mock(self, verdict, merchant, arbitrage, mocker, returned, custom_action_mock):
        calls = []
        if verdict:
            calls.append(
                mocker.call(
                    merchant.dialogs_org_id,
                    arbitrage.consultation_id,
                    'finishArbitrage',
                    {
                        'org_id': merchant.dialogs_org_id,
                        'verdict': arbitrage.verdict.value
                    }
                )
            )
        custom_action_mock.assert_has_calls(calls)
