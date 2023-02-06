import logging
from datetime import datetime

import pytest

from logbroker_client_common.handler import CommonHandler
from test_input.userjournal_stats import data, header, expected
import utils

logging.basicConfig(level=logging.DEBUG)

MOCK_DATETIME_NOW_VALUE = datetime(2016, 12, 6, 23, 59, 44)
MOCK_DATETIME_FROM_TIMESTAMP_VALUE = datetime(2016, 12, 5, 23, 59, 44)


@pytest.fixture
def conf():
    return utils.get_conf("configs/mail-stats/development.json")


@pytest.fixture
def cls_conf(conf):
    return conf['workers']['args']['handler']['args']


@pytest.fixture
def handler(cls_conf):
    return CommonHandler(**cls_conf)


@pytest.fixture(autouse=True)
def mock_request(mocker):
    post_method = mocker.patch('requests.post')
    post_method.return_value = mocker.MagicMock
    post_method.return_value.status_code = 200
    return post_method


@pytest.fixture(autouse=True)
def mock_processor(mocker):
    return mocker.patch('logbroker_processors.mail_stats.processor.ClickhouseProcessor._flush_pause')


@pytest.fixture(autouse=True)
def mock_datetime(mocker):
    mock_datetime = mocker.patch('logbroker_processors.mail_stats.parsers.records.datetime')
    mock_datetime.now.return_value = MOCK_DATETIME_NOW_VALUE
    mock_datetime.fromtimestamp.return_value = MOCK_DATETIME_FROM_TIMESTAMP_VALUE

    return mock_datetime


class TestMailStats:
    @pytest.fixture
    def returned(self, handler):
        handler.process(header, data)
        handler.flush(True)

    class TestParse:
        def test_parse(self, handler, mock_request, mock_datetime, returned):
            call_data = mock_request.call_args[1]['data'].splitlines()

            assert list(call_data) == list(expected)
            assert len(handler.processors['users_history_stats']['inst']._buffer) == 0

    class TestPushers:
        @pytest.fixture(params=(0, 1, 2))
        def servers_count(self, request):
            return request.param

        @pytest.fixture
        def cls_conf(self, conf, servers_count):
            result = conf['workers']['args']['handler']['args']

            server = result['stream']['users_history_stats']['args']['clickhouse'].pop()
            for _ in xrange(servers_count):
                result['stream']['users_history_stats']['args']['clickhouse'].append(server)

            return result

        def test_pushers(self, returned, mock_request, servers_count):
            assert mock_request.call_count == servers_count
