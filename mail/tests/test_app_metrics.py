#!/usr/bin/python -tt
import base64

import pytest
from logging.config import dictConfig

import logbroker_client_common.utils as utils
import tests.utils
from logbroker_client.logbroker.decompress import Zstd
from logbroker_processors.utils.unistat import Unistat
from test_input import app_metrics_tokens
from logbroker_client_common.handler import CommonHandler


@pytest.fixture
def handler():
    conf = tests.utils.get_conf("configs/app-metrics/development.json")
    conf['workers']['args']['handler']['args']['stream']['app_metrics_tokens']['args']['pusher']['allowed_apps'] = ['com.allowed.app']
    cls_conf = conf['workers']['args']['handler']['args']
    # Fire up an instance
    handler_obj = CommonHandler(**cls_conf)
    log_cls = utils.importobj('logbroker_client_common.logs.config_stdout')
    dictConfig(log_cls())

    return handler_obj


@pytest.fixture(autouse=True)
def mock_push(mocker):
    return mocker.patch('logbroker_processors.app_metrics.pusher.EventPushTokensPusher._push_one_event')


def test_tokens(handler, mock_push):
    with open('tests/zstd.base64', 'rb') as f:
        compressed = base64.b64decode(f.read())
        data = Zstd().decompress(compressed)
    handler.process(app_metrics_tokens.header, data)
    handler.flush(True)

    # assert mock_push.called TODO: find blob with push_token and uncomment

    # push_call_args = [x[0] for x in mock_push.call_args_list]
    # assert push_call_args == app_metrics_tokens.expected
    # assert Unistat._counter['processed_row_count_summ'] == 17
    # assert Unistat._counter['14836_EVENT_CRASH_summ'] == 1
    # assert Unistat._counter['ru.yandex.mail_android_EVENT_CRASH_summ'] == 1
    # assert Unistat._counter['10321_EVENT_ERROR_summ'] == 1
    # assert Unistat._counter['ru.yandex.searchplugin_android_EVENT_ERROR_summ'] == 1
    # assert Unistat._counter['10321_EventName_EVENTUS_bar_request_execution_error_summ'] == 1
    # assert Unistat._counter['ru.yandex.searchplugin_android_EventName_EVENTUS_bar_request_execution_error_summ'] == 1
