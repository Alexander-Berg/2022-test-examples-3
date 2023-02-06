import json
import logging
import pytest

from logbroker_client_common.handler import CommonHandler
import tests.utils as utils
from test_input.sendr import header_click, header_delivery, data_click, data_delivery, expected_click, expected_delivery

logging.basicConfig(level=logging.DEBUG)


@pytest.fixture
def conf():
    return utils.get_conf("configs/sendr-stats/development.json")


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
    return mocker.patch('logbroker_processors.sendr_stats.processor.BaseProcessor._flush_pause')
    return mocker.patch('logbroker_processors.sendr_stats.processor.DeliveryProcessor._flush_pause')
    return mocker.patch('logbroker_processors.sendr_stats.processor.ClickProcessor._flush_pause')


class TestSendrStats:
    @pytest.fixture(params=('delivery', 'click'))
    def data_type(self, request):
        return request.param

    @pytest.fixture
    def header(self, data_type):
        return {'delivery': header_delivery, 'click': header_click}[data_type]

    @pytest.fixture
    def data(self, data_type):
        return {'delivery': data_delivery, 'click': data_click}[data_type]

    @pytest.fixture
    def expected(self, data_type):
        return {'delivery': expected_delivery, 'click': expected_click}[data_type]

    @pytest.fixture
    def returned(self, handler, header, data):
        handler.process(header, data)
        handler.flush(True)

    class TestParse:
        def test_parse(self, returned, expected, handler, mock_request):
            call_data = mock_request.call_args[1]['data'].splitlines()
            assert list(call_data) == list(expected)
            assert len(handler.processors['sendr_delivery']['inst']._buffer) == 0
            assert len(handler.processors['sendr_click']['inst']._buffer) == 0

    class TestPushers:
        @pytest.fixture(params=(0, 1, 2))
        def servers_count(self, request):
            return request.param

        @pytest.fixture
        def streams(self):
            return ['sendr_delivery', 'sendr_click']

        @pytest.fixture
        def cls_conf(self, conf, streams, servers_count):
            result = conf['workers']['args']['handler']['args']

            for stream in streams:
                server = result['stream'][stream]['args']['clickhouse'].pop()
                for _ in xrange(servers_count):
                    result['stream'][stream]['args']['clickhouse'].append(server)

            return result

        def test_pushers(self, returned, streams, mock_request, servers_count):
            assert mock_request.call_count == servers_count
