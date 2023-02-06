import pytest

from mail.beagle.beagle.core.actions.sync.sync_organization import QueueSyncCurrentOrganizationAction


class TestSyncHandler:
    @pytest.fixture
    def client(self, rands):
        return rands()

    @pytest.fixture
    def action(self, mock_action):
        return mock_action(QueueSyncCurrentOrganizationAction)

    @pytest.fixture(params=(True, False))
    def force(self, request):
        return request.param

    @pytest.fixture
    async def response(self, app, client, org, force):
        return await app.post(f'/api/v1/sync/{client}', json={'org_id': org.org_id, 'force': force})

    def test_params(self, action, org, force, response):
        action.assert_called_once_with(org_id=org.org_id, force=force)
