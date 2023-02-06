import pytest

from mail.payments.payments.core.entities.enums import RefundStatus
from mail.payments.payments.taskq.workers.refund_updater import RefundUpdater


@pytest.fixture(autouse=True)
def action_mock(mock_action):
    from mail.payments.payments.core.actions.update_refund import UpdateRefundAction
    return mock_action(UpdateRefundAction)


@pytest.fixture
async def refund_updater(app, test_logger):
    worker = RefundUpdater(logger=test_logger)
    await worker.initialize_worker(app)
    yield worker
    worker.heartbeat_task.cancel()


@pytest.mark.asyncio
async def test_no_pending_refund(refund_updater):
    assert not await refund_updater.process_task()


class TestRefundUpdater:
    @pytest.fixture(autouse=True)
    def setup(self, payments_settings, existing_refunds):
        payments_settings.REFUND_UPDATER_DELAY = 0

    @pytest.fixture
    def existing_refunds_data(self):
        return [{'refund_status': RefundStatus.REQUESTED}]

    @pytest.mark.asyncio
    async def test_requested_refund_already_updated_recently(self, payments_settings, refund_updater):
        payments_settings.REFUND_UPDATER_DELAY = 60 * 60
        assert not await refund_updater.process_task()

    @pytest.mark.asyncio
    async def test_requested_refund_processed(self, refund_updater):
        assert await refund_updater.process_task()

    @pytest.mark.asyncio
    async def test_params(self, storage, action_mock, refund_updater, existing_refunds):
        await refund_updater.process_task()
        action_mock.assert_called_once_with(refund=existing_refunds[0])
