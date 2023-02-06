import pytest

from mail.beagle.beagle.core.actions.recipients import RecipientsAction


class TestRecipientsHandler:
    @pytest.fixture
    def action_result(self):
        return None

    @pytest.fixture(autouse=True)
    def action(self, mock_action, action_result):
        return mock_action(RecipientsAction, action_result)

    @pytest.fixture
    def params(self, randmail):
        return {'email_from': randmail(), 'email_to': randmail()}

    @pytest.fixture
    async def response(self, app, params):
        return await app.get('/api/v1/recipients', params=params)

    def test_params(self, action, response, params):
        action.assert_called_with(**params)

    @pytest.mark.parametrize('params', ({'email_to': 'a@a.com'}, {'email_from': 'a@a.com'}, None))
    def test_bad_request(self, response):
        assert response.status == 400

    @pytest.mark.asyncio
    @pytest.mark.parametrize('action_result', ({'subscriptions': [{'uid': 0, 'email': 'UPPER@YA.RU'}]},))
    async def test_email_lower(self, response):
        data = await response.json()
        assert data['response']['subscriptions'][0]['email'] == 'upper@ya.ru'
