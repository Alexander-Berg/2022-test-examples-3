import pytest
from mail.doberman.unistat.lib.sharpei import (
    get_shard_name,
)


@pytest.mark.parametrize(('shard_id', 'stat', 'expected'), [
    ('123', {'100': None}, '123'),
    ('123', {'123': None}, '123'),
    ('123', {'123': ''}, '123'),
    ('123', {'123': {}}, '123'),
    ('123', {'123': []}, '123'),
    ('123', {'123': [{'role': 'replica'}]}, '123'),
    ('123', {'123': {'databases': [{'role': 'master', 'address': {'host': 'xdb1002h'}}]}}, '123'),
    ('123', {'123': {'name': 'xdb1234'}}, 'xdb1234'),
])
def test_get_shard_host(shard_id, stat, expected):
    host = get_shard_name(shard_id, stat)
    assert host == expected
