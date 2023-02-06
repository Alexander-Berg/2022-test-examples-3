#!/usr/bin/python -tt
import json
import pytest
from logging.config import dictConfig

import logbroker_client_common.utils as utils
import utils as test_utils
from test_input import app_metrics_tokens
from logbroker_client_common.handler import CommonHandler


@pytest.fixture
def handler():
    conf = test_utils.get_conf("configs/app-metrics/development.json")
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
    handler.process(app_metrics_tokens.header, app_metrics_tokens.data)
    handler.flush(True)

    assert mock_push.called

    push_call_args = [x[0] for x in mock_push.call_args_list]
    assert push_call_args == app_metrics_tokens.expected
