import pytest
from mail.doberman.unistat.lib.stat import (
    get_job_info,
    is_alive,
)


@pytest.mark.parametrize(('stat', 'expected'), [
    ({}, {'worker_id': 'unknown_worker', 'shard_id': 'unknown_shard'}),
    ({'job': ''}, {'worker_id': 'unknown_worker', 'shard_id': 'unknown_shard'}),
    ({'job': {}}, {'worker_id': 'unknown_worker', 'shard_id': 'unknown_shard'}),
    ({'job': {'worker_id': ''}}, {'worker_id': 'unknown_worker', 'shard_id': 'unknown_shard'}),
    ({'job': {'shard_id': ''}}, {'worker_id': 'unknown_worker', 'shard_id': 'unknown_shard'}),
    ({'job': {'worker_id': 'dobby'}}, {'worker_id': 'dobby', 'shard_id': 'unknown_shard'}),
    ({'job': {'shard_id': '123'}}, {'worker_id': 'unknown_worker', 'shard_id': '123'}),
    ({'job': {'worker_id': 'dobby', 'shard_id': '123'}}, {'worker_id': 'dobby', 'shard_id': '123'})
])
def test_get_job_info(stat, expected):
    job = get_job_info(stat)
    assert job == expected


@pytest.mark.parametrize(('job_info', 'expected'), [
    ({'worker_id': 'unknown_worker', 'shard_id': 'unknown_shard'}, False),
    ({'worker_id': 'worker', 'shard_id': 'unknown_shard'}, False),
    ({'worker_id': 'unknown_worker', 'shard_id': 'shard'}, False),
    ({'worker_id': 'worker', 'shard_id': 'shard'}, True),
])
def test_is_alive(job_info, expected):
    res = is_alive(job_info)
    assert res == expected
