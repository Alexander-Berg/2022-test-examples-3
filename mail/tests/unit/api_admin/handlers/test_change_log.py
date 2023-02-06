from datetime import datetime, timedelta
from random import randint

import pytest

from mail.payments.payments.utils.datetime import utcnow


class TestChangeLogHandler:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.manager.change_log import GetChangeLogListManagerAction
        return mock_action(GetChangeLogListManagerAction)

    @pytest.fixture
    def acting_manager(self, manager_assessor):
        return manager_assessor

    @pytest.fixture
    def run_action_result(self, change_logs):
        return change_logs

    @pytest.fixture
    def request_params(self):
        return {}

    @pytest.fixture
    async def response(self, admin_client, tvm, request_params):
        return await admin_client.get('/admin/api/v1/change_log', params=request_params)

    @pytest.mark.parametrize('request_params', (
        {},
        {
            'merchant_uid': randint(1, 10 ** 9),
            'changed_at_from': utcnow().isoformat(),
            'changed_at_to': (utcnow() + timedelta(hours=1)).isoformat(),
            'limit': 10,
            'offset': 20,
        },
        {
            'manager_uid': randint(1, 10 ** 9),  # Won't be overriden
        },
    ))
    def test_params(self, acting_manager, request_params, response, action):
        for datetime_key in ('changed_at_from', 'changed_at_to'):
            if datetime_key in request_params:
                request_params[datetime_key] = datetime.fromisoformat(request_params[datetime_key])

        action.assert_called_once_with(**{
            'merchant_uid': None,
            'changed_at_from': None,
            'changed_at_to': None,
            'limit': 100,
            'offset': 0,
            **request_params,
            'manager_uid': acting_manager.uid,
        })
