import pytest

from mail.beagle.beagle.core.actions.support import SupportAction


class TestRecipientsHandler:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(SupportAction)

    @pytest.fixture
    def params(self, randn):
        return {'list_uid': randn()}

    @pytest.fixture
    async def response(self, app, params):
        return await app.get('/api/v1/support', params=params)

    def test_params(self, action, response, params):
        action.assert_called_with(**params)
