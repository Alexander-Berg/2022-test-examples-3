import pytest

from logbroker_client_common.handler import CommonHandler
import logging
import test_input.userjournal_log as log
import test_input.userjournal_tskv_log as tskv_log

logging.basicConfig(level=logging.DEBUG)


@pytest.fixture
def handler():
    cls_conf = {
        'flush_lines': 100,
        'format': 'TSKV',
        'slow_log': 100.0,
        'stream': {
            'userjournal': {
                'filename': '.*',
                'processor': 'userjournal.UserjournalClickhouse',
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
    (log.header, log.data, log.expected),
    (tskv_log.header, tskv_log.data, tskv_log.expected),
], ids=('userjournal', 'userjournal_tskv'))
def test_process(_header, _data, _expected, handler, mock_request):
    handler.process(_header, _data)
    handler.flush(True)
    data_sent = mock_request.call_args[1]['data']
    print(data_sent.decode('utf-8'))
    assert data_sent.decode('utf-8') == _expected
    assert handler.processors['userjournal']['inst'].stat.all == {
        'total': {
            'read': 2,
            'unknown': 0,
            'dropped': 1
        },
        'rt3.sas--userjournal--mail-user-journal-log:1': {
            'read': 2,
            'dropped': 1
        }
    }
