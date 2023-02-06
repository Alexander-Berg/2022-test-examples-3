# coding: utf-8
from hamcrest import assert_that, has_entries, has_items, equal_to, greater_than_or_equal_to, all_of, has_entry, has_key, not_
import pytest
import time

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.resources.yt_table_resource import YtDynTableResource

NOW = int(time.time())
ONE_HOUR = 3600


class ScannerProcessingStatusTable(YtDynTableResource):
    def __init__(self, yt_stuff, path, data):
        super(ScannerProcessingStatusTable, self).__init__(
            yt_stuff=yt_stuff,
            path=path,
            data=data,
            attributes=dict(
                dynamic=True,
                external=False,
                schema=[
                    dict(name="type", type="string", sort_order="ascending"),
                    dict(name="proxy", type="string", sort_order="ascending"),
                    dict(name="table_path", type="string", sort_order="ascending"),
                    dict(name="content_revision", type="uint64", sort_order="ascending"),
                    dict(name="shard", type="int32", sort_order="ascending"),
                    dict(name="attempt", type="int32", sort_order="ascending"),
                    dict(name="host", type="string"),
                    dict(name="table_creation_ts", type="int64"),
                    dict(name="table_modification_ts", type="int64"),
                    dict(name="table_id", type="string"),
                    dict(name="table_target_path", type="string"),
                    dict(name="rows_count", type="int64"),
                    dict(name="processing_start_ts", type="int64"),
                    dict(name="processing_end_ts", type="int64"),
                ],
            ),
        )


@pytest.fixture(scope='module')
def scanner_processing_status(yt_server):
    return ScannerProcessingStatusTable(yt_server, '//tmp/tests/scanner_processing_status', [{
        'type': 'ReadTable',
        'proxy': 'tesla',
        'table_path': '//path',
        'content_revision': 1234,
        'shard': 0,
        'attempt': 1,
        'table_creation_ts': NOW - ONE_HOUR,
        'processing_end_ts': NOW,
        'rows_count': 100
    }, {
        'type': 'UnreadTable',
        'proxy': 'tesla',
        'table_path': '//path',
        'content_revision': 1234,
        'shard': 0,
        'attempt': 1,
        'table_creation_ts': NOW - ONE_HOUR,
        'rows_count': 100
    }, {
        'type': 'IgnoreOlderTable',
        'proxy': 'tesla',
        'table_path': '//path',
        'content_revision': 1234,
        'shard': 0,
        'attempt': 1,
        'table_creation_ts': NOW - 2 * ONE_HOUR,
        'rows_count': 5000
    }, {
        'type': 'IgnoreOlderTable',
        'proxy': 'tesla',
        'table_path': '//path',
        'content_revision': 2345,
        'shard': 0,
        'attempt': 1,
        'table_creation_ts': NOW - ONE_HOUR,
        'processing_end_ts': NOW,
        'rows_count': 100
    }, {
        'type': 'ShardedTable',
        'proxy': 'tesla',
        'table_path': '//path',
        'content_revision': 1234,
        'shard': 0,
        'attempt': 1,
        'table_creation_ts': NOW - ONE_HOUR,
        'processing_end_ts': NOW,
        'rows_count': 100
    }, {
        'type': 'ShardedTable',
        'proxy': 'tesla',
        'table_path': '//path',
        'content_revision': 1234,
        'shard': 1,
        'attempt': 1,
        'table_creation_ts': NOW - ONE_HOUR,
        'rows_count': 100
    }])


@pytest.fixture(scope='module')
def config(yt_server):
    cfg = {
        'general': {
            'color': 'white',
        },
        'scanner_metrics': {
            'processing_status_proxy': yt_server.get_yt_client().config['proxy']['url'],
            'processing_status_table': '//tmp/tests/scanner_processing_status'
        }
    }
    return RoutinesConfigMock(
        yt_server=yt_server,
        config=cfg)


@pytest.yield_fixture(scope='module')
def routines_http(yt_server, config, scanner_processing_status):
    resources = {
        'config': config,
        'scanner_processing_status': scanner_processing_status
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as routines_http_env:
        yield routines_http_env


def test_scanner_metrics(routines_http):
    response = routines_http.get('/metrics?project=market.datacamp&service=scanner')
    assert_that(response, HasStatus(200))
    sensors = response.json['sensors']

    def assert_sensor(sensor, reader_type, shard, value):
        assert_that(sensors, has_items(
            has_entries({
                'kind': 'IGAUGE',
                'labels': all_of(
                    has_entries({
                        'sensor': sensor,
                        'reader_type': reader_type,
                    }),
                    has_entry('shard', shard) if shard is not None else not_(has_key('shard'))
                ),
                'value': value
            })))

    assert_sensor('table_reader_commit_lag', 'ReadTable', '0', equal_to(0))
    assert_sensor('table_rows_count', 'ReadTable', '0', equal_to(100))

    assert_sensor('table_reader_commit_lag', 'IgnoreOlderTable', '0', equal_to(0))
    assert_sensor('table_rows_count', 'IgnoreOlderTable', '0', equal_to(100))

    assert_sensor('table_reader_commit_lag', 'UnreadTable', '0', greater_than_or_equal_to(ONE_HOUR))
    assert_sensor('table_rows_count', 'UnreadTable', '0', equal_to(100))

    assert_sensor('table_reader_commit_lag', 'ShardedTable', '0', equal_to(0))
    assert_sensor('table_rows_count', 'ShardedTable', '0', equal_to(100))
    assert_sensor('table_age', 'ShardedTable', '0', greater_than_or_equal_to(ONE_HOUR))
    assert_sensor('table_reader_commit_lag', 'ShardedTable', '1', greater_than_or_equal_to(ONE_HOUR))
    assert_sensor('table_rows_count', 'ShardedTable', '1', equal_to(100))
    assert_sensor('table_age', 'ShardedTable', '1', greater_than_or_equal_to(ONE_HOUR))
    assert_sensor('table_reader_commit_lag', 'ShardedTable', None, greater_than_or_equal_to(ONE_HOUR))
    assert_sensor('table_rows_count', 'ShardedTable', None, equal_to(200))
    assert_sensor('table_age', 'ShardedTable', None, greater_than_or_equal_to(ONE_HOUR))
