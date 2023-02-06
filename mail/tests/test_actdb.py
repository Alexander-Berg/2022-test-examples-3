import logging
import pytest

import test_input.actdb as actdb

from logbroker_client_common.handler import CommonHandler
logging.basicConfig(level=logging.DEBUG)


class PgMock(object):
    """
    Mock for psycopg2 lib
    """

    def __init__(self, *args, **kwargs):
        self.received_commands = []
        self.rowcount = 0

    def __getattr__(self, name):
        def handler(*args, **kwargs):
            self.received_commands.append((name, args, kwargs))
            ret_values = {
                'fetchone': (0, ),
                'mogrify': (args, kwargs),
                'rowcoint': 0
            }
            return ret_values.get(name, self)

        return handler

    def __str__(self):
        return str(self.received_commands)


@pytest.fixture
def handler():
    cls_conf = {
        'flush_lines': 100,
        'format': 'TSKV',
        'slow_log': 100.0,
        'stream': {
            'actdb': {
                'filename': '.*',
                'processor': 'actdb_processor.ActivitydbProcessor',
                "args": {
                    "conn_string":
                    "port=6432 dbname=actdb user=pipeline connect_timeout=1",
                    "cond_group":
                    "mail_actdb",
                    "batch_size":
                    5,
                }
            }
        }
    }
    return CommonHandler(**cls_conf)


@pytest.fixture(autouse=True)
def mock_get(mocker):
    get_method = mocker.patch('requests.get')
    get_method.return_value.text = 'test'
    get_method.status_code = 200
    return get_method


@pytest.fixture(autouse=True)
def mock_psycopg2(mocker):
    connect = mocker.patch('psycopg2.connect')
    connect.return_value = PgMock()
    return connect


@pytest.mark.parametrize(
    '_header, _tskv_log, _expected_data_accumulator, _expected_sql', [
        (actdb.header, actdb.tskv_log,
         actdb.expected_data_accumulator, actdb.expected_sql),
    ],
    ids=('actdb', ))
def test_process(_header, _tskv_log, _expected_data_accumulator, _expected_sql,
                 handler):
    handler.process(_header, _tskv_log)

    assert handler.processors['actdb']['inst'].data_accumulator == _expected_data_accumulator

    handler.flush(True)
    assert str(handler.processors['actdb']['inst'].cur) == str(_expected_sql)
