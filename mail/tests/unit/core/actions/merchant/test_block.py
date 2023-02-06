import pytest

from mail.payments.payments.core.actions.contract import TerminateContractAction
from mail.payments.payments.core.actions.merchant.block import BlockMerchantAction


class TestBlockMerchant:
    @pytest.fixture
    def blocked(self):
        return True

    @pytest.fixture
    async def modified_merchant(self, storage, blocked, merchant):
        merchant.blocked = False if blocked else True
        return await storage.merchant.save(merchant)

    @pytest.fixture
    def terminate_contract_action(self, mock_action):
        return mock_action(TerminateContractAction)

    @pytest.fixture
    def terminate_contract(self):
        return True

    @pytest.fixture
    def params(self, modified_merchant, terminate_contract, blocked):
        return {
            'uid': modified_merchant.uid,
            'terminate_contract': terminate_contract,
            'block': blocked
        }

    @pytest.fixture
    async def returned(self, params, terminate_contract_action):
        return await BlockMerchantAction(**params).run()

    @pytest.mark.parametrize('blocked', [True, False])
    @pytest.mark.asyncio
    async def test_blocked_flag(self, returned, blocked):
        assert returned.blocked == blocked

    @pytest.mark.parametrize('blocked', [True, False])
    @pytest.mark.asyncio
    async def test_blocked_flag_saved(self, returned, storage, blocked):
        saved = await storage.merchant.get(uid=returned.uid)
        assert saved.blocked == blocked

    class TestTerminateContract:
        @pytest.mark.asyncio
        async def test_terminate_contract(self, returned, terminate_contract_action):
            terminate_contract_action.assert_called_once()

        @pytest.mark.parametrize('terminate_contract', [False])
        @pytest.mark.asyncio
        async def test_not_terminate_contract(self, returned, terminate_contract, terminate_contract_action):
            terminate_contract_action.assert_not_called()
