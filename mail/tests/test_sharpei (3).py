from __future__ import unicode_literals
import pytest
import json
import os
import logging

from mock import call

from logbroker_client_common.handler import CommonHandler

import utils
from test_input.sharpei import data, header

logging.basicConfig(level=logging.DEBUG)


@pytest.fixture
def handler():
    conf = utils.get_conf("configs/abook/development.json")
    cls_conf = conf['workers']['args']['handler']['args']
    return CommonHandler(**cls_conf)


@pytest.fixture
def mock_request(mocker):
    post_method = mocker.patch('logbroker_processors.mail_abook.client.service_base_client.requests.Session.post')
    post_method.return_value = mocker.MagicMock()
    post_method.return_value.status_code = 200
    post_method.return_value.json = mocker.MagicMock()
    post_method.return_value.json.return_value = {'result': 'Ok'}
    return post_method


@pytest.fixture
def mock_corp_user(mocker):
    mocked = mocker.patch('logbroker_processors.mail_abook.processor.passport.user_is_corp', return_value=True)
    return mocked


@pytest.fixture(autouse=True)
def mock_env_key():
    os.environ['SENDER_OAUTH_TOKEN'] = 'token'


def test_process(handler, mock_request):
    handler.process(header, data)
    handler.flush(True)

    first_call = call('https://test.sender.yandex-team.ru/api/0/Yandex.Mail/transactional/GIBQFMK2-IP31/send',
                      timeout=60,
                      data=None,
                      headers=None,
                      params={'to_yandex_puid': 100000000000000000668},
                      json={u'headers': '{"Date": "Thu, 01 Jan 1970 00:00:00 GMT", "Message-ID": "<welcome-letter>"}',
                            u'async': True})
    second_call = call('https://test.sender.yandex-team.ru/api/0/Yandex.Mail/transactional/8IM3X9Z2-C2E1/send',
                       timeout=60,
                       data=None,
                       params={'to_yandex_puid': 100000000000000000682},
                       headers=None,
                       json={u'headers': '{"Date": "Thu, 01 Jan 1970 00:00:00 GMT", "Message-ID": "<welcome-letter>"}',
                             u'async': True})
    assert mock_request.call_args_list == [first_call, second_call]
