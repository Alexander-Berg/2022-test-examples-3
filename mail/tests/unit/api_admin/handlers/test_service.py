import pytest


class TestServiceHandler:
    @pytest.fixture(autouse=True)
    def action(self, mock_action, service):
        from mail.payments.payments.core.actions.manager.service import GetServiceListManagerAction
        return mock_action(GetServiceListManagerAction, [service])

    @pytest.fixture(params=('assessor', 'admin'))
    def acting_manager(self, request, managers):
        return managers[request.param]

    @pytest.fixture
    async def response(self, admin_client, tvm, merchant, path):
        return await admin_client.get('/admin/api/v1/service')

    def test_params(self, merchant, path, response, action, acting_manager):
        action.assert_called_once_with(
            manager_uid=acting_manager.uid,
        )
