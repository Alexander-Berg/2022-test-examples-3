from datetime import datetime

import pytest

from sendr_utils import json_value

from mail.payments.payments.core.entities.enums import PAY_METHOD_YANDEX


class TestReportGet:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.report import GetReportListAction
        return mock_action(GetReportListAction)

    @pytest.fixture
    async def response(self, payments_client, merchant):
        return await payments_client.get(f'/v1/report/{merchant.uid}')

    def test_params(self, merchant, response, action):
        action.assert_called_once_with(uid=merchant.uid)


class TestReportPost:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.report import CreateTaskReportAction
        return mock_action(CreateTaskReportAction, (1, 2))

    @pytest.fixture
    def params(self):
        return {
            'lower_dt': datetime(2019, 9, 20, 10, 20, 30),
            'upper_dt': datetime(2019, 9, 21, 11, 21, 31),
            'pay_method': PAY_METHOD_YANDEX
        }

    @pytest.fixture
    async def response(self, payments_client, merchant, params):
        return await payments_client.post(f'/v1/report/{merchant.uid}', params=json_value(params))

    def test_params(self, merchant, response, action, params):
        action.assert_called_once_with(uid=merchant.uid, **params)


class TestReportDownload:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.report import DownloadReportAction
        return mock_action(DownloadReportAction)

    @pytest.fixture
    def report_id(self, rands):
        return rands()

    @pytest.fixture
    async def response(self, payments_client, merchant, report_id):
        return await payments_client.get(f'/v1/report/{merchant.uid}/download/{report_id}')

    def test_params(self, merchant, report_id, response, action):
        action.assert_called_once_with(uid=merchant.uid, report_id=report_id)
