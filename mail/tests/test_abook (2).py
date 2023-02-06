from __future__ import unicode_literals
import pytest
import logging
import os

from logbroker_client_common.handler import CommonHandler

import utils
from test_input.userjournal_stats import data, header

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


@pytest.fixture(autouse=True)
def mock_env_key():
    os.environ['SENDER_OAUTH_TOKEN'] = 'token'
    os.environ['TVM_COLLIE_CONSUMER_SECRET'] = 'c2VjcmV0'


@pytest.fixture
def mock_corp_user(mocker):
    mocked = mocker.patch('logbroker_processors.mail_abook.processor.passport.user_is_corp', return_value=True)
    return mocked


def test_process(handler, mock_request):
    handler.process(header, data)
    handler.flush(True)

    mock_request.assert_called_with(
        'http://collie-test.mail.yandex.net/v1/users/1130000022236626/contacts/emails',
        timeout=200,
        params=None,
        headers=None,
        data=None,
        json={'cc': [u'test2@yandex.ru'],
              'to': [u'test@yandex.ru'],
              'bcc': [u'test3@yandex.ru']},)


def test_process_corp(handler, mock_request, mock_corp_user):
    handler.process(header, data)
    handler.flush(True)

    mock_request.assert_called_with(
        'http://collie-test-corp.mail.yandex.net/v1/users/1130000022236626/contacts/emails',
        timeout=200,
        params=None,
        headers=None,
        data=None,
        json={'cc': [u'test2@yandex.ru'],
              'to': [u'test@yandex.ru'],
              'bcc': [u'test3@yandex.ru']},)
