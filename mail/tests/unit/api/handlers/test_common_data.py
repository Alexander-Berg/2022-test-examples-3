import pytest

from mail.payments.payments.core.actions.common_data import CreateCommonDataAction
from mail.payments.payments.core.entities.enums import CommonDataType


class TestCommonDataHandler:
    @pytest.fixture
    def payload(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def data_type(self, randitem):
        return randitem(CommonDataType)

    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        return mock_action(CreateCommonDataAction)

    @pytest.fixture
    async def response(self, data_type, payload, payments_client):
        return await payments_client.post('/v1/data', json={
            'data_type': data_type.value,
            'payload': payload
        })

    def test_response(self, response):
        assert response.status == 200

    def test_called(self, response, action, data_type, payload):
        action.assert_called_once_with(data_type=data_type, payload=payload)
