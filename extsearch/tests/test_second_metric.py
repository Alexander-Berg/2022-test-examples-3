from extsearch.audio.deepdive.tools.metrics.lib import second_metric as sm
from extsearch.audio.deepdive.common import utils

import yatest.common as yc
from mapreduce.yt.python.yt_stuff import YtConfig
import pytest
from unittest.mock import patch
import uuid


CYPRESS_DIR = 'extsearch/audio/deepdive/tools/metrics/tests/cypress_dir'


def mock_find_metricless(*args, **kwargs):
    return ['f-2', 'f-3', 'f-4', 'f-5', 'f-6']


def make_uuid(init_value=0):
    uuid_count = init_value

    def inner():
        nonlocal uuid_count
        uuid_count += 1
        return uuid.UUID(int=uuid_count)

    return inner


@pytest.fixture(scope='module')
def yt_config(request):
    return YtConfig(
        local_cypress_dir=yc.source_path(CYPRESS_DIR)
    )


def test_find_metricless(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    control, operations = utils.read_control(yt_client, '//data/control_table', collect_operations=['finish', 'secondmetric'])

    metric_operations = operations['secondmetric']
    finish_operations = set(operations['finish'])

    metricless = sm.find_metricless(control, finish_operations, metric_operations)
    assert sorted(metricless) == mock_find_metricless()


@patch('uuid.uuid4', make_uuid(2))
@patch('extsearch.audio.deepdive.tools.metrics.lib.second_metric.find_metricless', mock_find_metricless)
def test_parse_control(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    return sm.parse_control_table(yt_client, '//data/control_table')


@patch('uuid.uuid4', make_uuid(2))
@patch('extsearch.audio.deepdive.tools.metrics.lib.second_metric.find_metricless', mock_find_metricless)
@patch('time.time', lambda: 1.0)
def test_second_metrics(yt_stuff):
    table_map = {
        'toloka': '//data/aggregated_ann',
        'clusterizefaces': '//data/selected_clusters_ann'
    }
    yt_client = yt_stuff.get_yt_client()
    control_table = '//data/control_table'
    result_table = '//data/second_stage_metrics'
    toloka_tables, cluster_tables = sm.collect_tables(yt_client, table_map)
    sm.second_metrics(yt_client, control_table, toloka_tables, cluster_tables, result_table)
    metrics = sorted(list(yt_client.read_table(result_table)), key=lambda x: x['uuid'])
    control = list(yt_client.read_table(control_table))
    return [metrics, control]


def test_join_reducer():
    def rows():
        yield {
            'film_id': 1,
            'cluster_id': 100,
            'toloka_result': 'fake results'
        }
        yield {
            'film_id': 1,
            'cluster_id': 100,
            'cluster_tracks_length': 1
        }
    return list(sm.join_reducer({}, rows()))
