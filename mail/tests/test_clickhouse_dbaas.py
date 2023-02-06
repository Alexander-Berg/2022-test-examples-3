import json
import logging
import mock
import pytest

from logbroker_client_common.handler import CommonHandler
from test_input.dbaas import mongodb, postgres, pgbouncer, skip

logging.basicConfig(level=logging.DEBUG)


@pytest.fixture
def handler():
    cls_conf = {
        'flush_lines': 100,
        'format': 'TSKV',
        'slow_log': 100.0,
        'stream': {
            'clickhouse': {
                'filename': '.*',
                'processor': 'dbaas.DBaaSLogProcessor',
                'args': {
                    'hosts': ['localhost:8123'],
                    'database': 'test_db',
                    'delay_max': 1,
                    'delay_min': 10,
                    'delay_thresh': 0,
                    'log_data_errors': True
                }
            }
        }
    }
    return CommonHandler(**cls_conf)


@pytest.fixture(autouse=True)
def mock_request(mocker):
    post_method = mocker.patch('requests.post')
    post_method.return_value = mocker.MagicMock
    post_method.return_value.status_code = 200
    return post_method


@pytest.fixture(autouse=True)
def mock_stats_reset(mocker):
    reset = mocker.patch(
        'logbroker_processors.clickhouse.stats.ClickhouseStats.reset'
    )
    return reset


@pytest.mark.parametrize('_header, _data, _expected', [
    (postgres.header, postgres.data, postgres.expected),
    (pgbouncer.header, pgbouncer.data, pgbouncer.expected),
    (mongodb.header, mongodb.data, mongodb.expected),
], ids=('postgres', 'pgbouncer', 'mongodb'))
def test_process(_header, _data, _expected, handler, mock_request):
    handler.process(_header, _data)
    handler.flush(True)
    data_sent = mock_request.call_args[1]['data']
    assert data_sent.decode('utf-8') == _expected
    assert handler.processors['clickhouse']['inst'].stat.all == {
        'total': {
            'read': 1,
            'unknown': 1,
            'dropped': 1
        },
        'rt3.sas--dbaas--dbaas-int-log:1': {
            'read': 1,
            'unknown': 1,
            'dropped': 1
        }
    }


@pytest.mark.parametrize('_header, _data, _expected', [
    (skip.header, skip.data, skip.expected),
], ids=('skip',))
def test_skip_record(_header, _data, _expected, handler, mock_request):
    handler.process(_header, _data)
    handler.flush(True)
    data_sent = mock_request.call_args
    assert data_sent is None
    assert handler.processors['clickhouse']['inst'].stat.all == {
        'total': {
            'read': 2,
            'unknown': 1,
            'dropped': 0
        },
        'rt3.sas--dbaas--dbaas-int-log:1': {
            'read': 2,
            'unknown': 1,
        }
    }
