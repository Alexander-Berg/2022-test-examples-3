from copy import deepcopy
from datetime import timedelta

import pytest

from sendr_utils import utcnow

from mail.payments.payments.conf import settings
from mail.payments.payments.core.actions.merchant.data_update import MerchantDataUpdateAction
from mail.payments.payments.core.entities.merchant import BankData, Merchant
from mail.payments.payments.taskq.workers.merchant_data_updater import MerchantDataUpdater


@pytest.fixture
async def merchant_data_updater(app, test_logger):
    merchant_data_updater = MerchantDataUpdater(logger=test_logger)
    await merchant_data_updater.initialize_worker(app)
    yield merchant_data_updater
    merchant_data_updater.heartbeat_task.cancel()


class TestMerchantDataUpdater:
    @pytest.fixture
    def now(self, mocker):
        now = utcnow()
        mocker.patch('mail.payments.payments.storage.mappers.merchant.merchant.utcnow', mocker.Mock(return_value=now))
        return now

    @pytest.fixture
    async def ready_merchant(self, storage, merchant):
        merchant.data_updated_at = utcnow() - timedelta(seconds=settings.MERCHANT_DATA_UPDATE_DELAY_SECS + 1)
        return await storage.merchant.save(merchant)

    @pytest.fixture
    async def locked_merchant(self, storage, merchant):
        merchant.data_updated_at = utcnow() - timedelta(seconds=settings.MERCHANT_DATA_OUTDATE_SECS + 1)
        merchant.data_locked = True
        return await storage.merchant.save(merchant)

    @pytest.fixture
    async def action_mock_ready_merchant(self, mock_action, ready_merchant):
        return mock_action(MerchantDataUpdateAction, ready_merchant)

    @pytest.fixture
    async def action_mock_locked_merchant(self, mock_action, locked_merchant):
        return mock_action(MerchantDataUpdateAction, locked_merchant)

    @pytest.mark.asyncio
    async def test_no_merchant(self, merchant_data_updater):
        assert not await merchant_data_updater.process_task()

    @pytest.mark.asyncio
    async def test_no_old_data(self, merchant, storage, merchant_data_updater):
        merchant.data_updated_at = utcnow()
        await storage.merchant.save(merchant)
        assert not await merchant_data_updater.process_task()

    @pytest.mark.asyncio
    async def test_get_locked(self, locked_merchant, merchant_data_updater, action_mock_locked_merchant, now):
        await merchant_data_updater.process_task()
        locked_merchant.revision += 1  # sync revision
        locked_merchant.updated = now  # sync updated timestamp
        action_mock_locked_merchant.assert_called_once_with(merchant=locked_merchant)

    @pytest.mark.asyncio
    async def test_action_params(self, ready_merchant, merchant_data_updater, action_mock_ready_merchant, now):
        await merchant_data_updater.process_task()
        ready_merchant.data_locked = True
        ready_merchant.revision += 1  # sync revision
        ready_merchant.updated = now  # sync updated timestamp
        action_mock_ready_merchant.assert_called_once_with(merchant=ready_merchant)

    @pytest.mark.asyncio
    async def test_merchant_update(self, ready_merchant, merchant_data_updater, storage, mocker):
        updated_at = utcnow()
        action_merchant_storage = None  # use inside action mock

        def _patch_bank(merchant: Merchant) -> None:
            data = merchant.data
            bank_copy = deepcopy(data.bank)
            data.bank = BankData(
                account='666666',
                bik=bank_copy.bik,
                correspondent_account=bank_copy.correspondent_account,
                name=bank_copy.name,
            )

        def init(self, merchant: Merchant) -> None:
            nonlocal action_merchant_storage
            action_merchant_storage = merchant

        async def run(self) -> Merchant:
            nonlocal action_merchant_storage
            # nonlocal updated_at
            _patch_bank(action_merchant_storage)
            # action_merchant_storage.data_updated_at = updated_at
            return action_merchant_storage

        mocker.patch.object(MerchantDataUpdateAction, '__init__', init)
        mocker.patch.object(MerchantDataUpdateAction, 'run', run)

        now_mock = mocker.Mock(return_value=updated_at)
        mocker.patch('mail.payments.payments.taskq.workers.merchant_data_updater.utcnow', now_mock)

        await merchant_data_updater.process_task()
        stored_merchant = await storage.merchant.get(uid=ready_merchant.uid)

        # sync actual and expected values
        _patch_bank(ready_merchant)                            # sync bank data
        ready_merchant.revision += 2                           # bump revision twice: on data lock and on data save
        ready_merchant.updated = stored_merchant.updated       # new updated timestamp
        ready_merchant.data_updated_at = updated_at            # new data_updated_at timestamp

        assert ready_merchant == stored_merchant
