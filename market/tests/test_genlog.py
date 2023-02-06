# encoding: utf-8

from datetime import datetime, timedelta
import json
import pytest
import six
import yatest.common

import market.idx.pylibrary.mindexer_core.genlog.genlog as genlog
import market.idx.marketindexer.miconfig as miconfig


class FakeConfig(miconfig.MiConfig):
    def __init__(self, hours, warn_threshold, error_threshold):
        self.mapreduce_genlogs_memory_monitoring_hours = hours
        self.mapreduce_genlogs_memory_monitoring_warn_threshold = warn_threshold
        self.mapreduce_genlogs_memory_monitoring_error_threshold = error_threshold
        self.is_mir = False


def make_ts(dt):
    return dt.strftime(genlog.DATETIME_FORMAT)


def mins(num_minutes):
    return timedelta(minutes=num_minutes)


NOW = datetime(2018, 12, 12, 12, 12, 12)
DATA = [
    {'ts': make_ts(NOW - mins(150)), 'max_memory': 950, 'memory_limit': 1000},
    {'ts': make_ts(NOW - mins(90)), 'max_memory': 850, 'memory_limit': 1000},
    {'ts': make_ts(NOW - mins(30)), 'max_memory': 500, 'memory_limit': 1000},
    {'ts': make_ts(NOW), 'max_memory': 500, 'memory_limit': 1000},
]


@pytest.fixture(scope='module')
def log_path():
    path = yatest.common.test_output_path('memory_log.json')
    with open(path, 'w') as log_file:
        for line in DATA:
            six.print_(json.dumps(line), file=log_file)
    return path


@pytest.mark.parametrize(
    'hours,warn_threshold,error_threshold,code',
    [
        (1, 0.8, 0.9, 0),
        (2, 0.8, 0.9, 1),
        (3, 0.8, 0.9, 2),
    ],
)
def test_check_logs(log_path, hours, warn_threshold, error_threshold, code):
    config = FakeConfig(hours, warn_threshold, error_threshold)
    actual_code, _ = genlog.check_memory_log(config, now=NOW, log_path=log_path)
    assert code == actual_code
