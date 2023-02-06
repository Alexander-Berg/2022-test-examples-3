import pytest
from hamcrest import (
    assert_that,
    contains_inanyorder,
)

from mail.doberman.unistat.lib.unistat import (
    get_prefix,
    make_log_stat,
    make_pa_stat,
    make_alive_stat,
)


def test_get_prefix():
    res = get_prefix('worker', 'shard')
    assert res == 'ctype=shard;tier=worker'


def test_make_log_stat():
    stat = {
        'error': {
            'apq': 1,
            'macs': 2
        },
        'notice': {
            'ignore': 3
        }
    }

    res = make_log_stat('prefix', stat)
    assert_that(res, contains_inanyorder(
        ['prefix;apq_error_ammm', 1],
        ['prefix;macs_error_ammm', 2],
        ['prefix;ignore_notice_ammm', 3],
    ))


def test_make_pa_stat():
    counts = {
        'oper_1': 4,
        'oper_2': 3,
    }
    times = {
        'oper_1': {'max': 300, 'min': 100, 'avg': 200},
        'oper_2': {'max': 400, 'min': 100, 'avg': 300},
    }

    res = make_pa_stat('prefix', (counts, times))
    assert_that(res, contains_inanyorder(
        ['prefix;oper_1_count_ammm', 4],
        ['prefix;oper_2_count_ammm', 3],
        ['prefix;oper_1_time_min_annn', 100],
        ['prefix;oper_1_time_avg_avvv', 200],
        ['prefix;oper_1_time_max_axxx', 300],
        ['prefix;oper_2_time_min_annn', 100],
        ['prefix;oper_2_time_avg_avvv', 300],
        ['prefix;oper_2_time_max_axxx', 400],
    ))


@pytest.mark.parametrize(('job_info', 'expected'), [
    ({'worker_id': 'unknown_worker', 'shard_id': 'unknown_shard'}, 0),
    ({'worker_id': 'worker', 'shard_id': 'unknown_shard'}, 0),
    ({'worker_id': 'unknown_worker', 'shard_id': 'shard'}, 0),
    ({'worker_id': 'worker', 'shard_id': 'shard'}, 1),
])
def test_make_alive_stat(job_info, expected):
    res = make_alive_stat(job_info)
    assert_that(res, contains_inanyorder(
        ['is_alive_worker_ammm', expected],
    ))
