import pytest


class TestDocumentHandler:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.manager.document import DownloadDocumentManagerAction
        return mock_action(DownloadDocumentManagerAction, (1, 2))

    @pytest.fixture(params=('assessor', 'admin'))
    def acting_manager(self, request, managers):
        return managers[request.param]

    @pytest.fixture
    async def response(self, admin_client, tvm, merchant, path):
        return await admin_client.get(f'/admin/api/v1/document/{merchant.uid}/{path}/download')

    def test_params(self, merchant, path, response, action, acting_manager):
        action.assert_called_once_with(
            uid=merchant.uid,
            path=path,
            manager_uid=acting_manager.uid,
        )
