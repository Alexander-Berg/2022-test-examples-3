import pytest

from mail.payments.payments.core.actions.manager.service import GetServiceListManagerAction


class TestGetServiceListManagerAction:
    @pytest.fixture
    def params(self, manager):
        return {
            'manager_uid': manager.uid,
        }

    @pytest.fixture
    async def returned_list(self, params, service):
        return await GetServiceListManagerAction(**params).run()

    def test_returned(self, service, returned_list):
        assert returned_list == [service]
