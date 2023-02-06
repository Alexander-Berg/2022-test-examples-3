import pytest

from mail.beagle.beagle.core.actions.ping_db import PingDBAction


class TestGetPingDB:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(PingDBAction)

    @pytest.fixture
    async def response(self, app):
        return await app.get('/pingdb')

    def test_params(self, action, response):
        action.assert_called_once()
