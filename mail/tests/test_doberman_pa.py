import pytest
from mail.doberman.unistat.lib.pa import (
    parse_record,
    get_stat_by_records,
)


@pytest.mark.parametrize(('row', 'expected'), [
    (
        '19:13:00 doberman Change_cache fetch 46 0.123',
        {'sender': 'doberman', 'operation': 'change_cache_fetch', 'time': 123}
    ),
    (
        '19:13:00 macs_pg Query GetFolders 46 asdf',
        {'sender': 'macs_pg', 'operation': 'query_getfolders', 'time': 0}
    )
])
def test_parse_record(row, expected):
    parsed = parse_record(row)
    assert parsed == expected


@pytest.mark.parametrize(('records', 'counts_exp', 'times_exp'), [
    (
        [{'sender': 'doberman', 'operation': 'oper', 'time': 500}],
        {'oper_1': 0, 'oper_2': 0},
        {'oper_1': {'min': 0, 'avg': 0, 'max': 0}, 'oper_2': {'min': 0, 'avg': 0, 'max': 0}}
    ),
    (
        [{'sender': 'doberman', 'operation': 'oper_1', 'time': 500}],
        {'oper_1': 1, 'oper_2': 0},
        {'oper_1': {'min': 500, 'avg': 500, 'max': 500}, 'oper_2': {'min': 0, 'avg': 0, 'max': 0}}
    ),
    (
        [{'sender': 'doberman', 'operation': 'oper_2', 'time': 500}],
        {'oper_1': 0, 'oper_2': 1},
        {'oper_1': {'min': 0, 'avg': 0, 'max': 0}, 'oper_2': {'min': 500, 'avg': 500, 'max': 500}}
    ),
    (
        [{'sender': 'doberman', 'operation': 'oper_1', 'time': 500},
         {'sender': 'doberman', 'operation': 'oper_2', 'time': 500},
         {'sender': 'doberman', 'operation': 'oper_1', 'time': 700},
         {'sender': 'doberman', 'operation': 'oper_1', 'time': 1500},
         {'sender': 'doberman', 'operation': 'oper_2', 'time': 1500},
         {'sender': 'doberman', 'operation': 'oper_2', 'time': 1600}],
        {'oper_1': 3, 'oper_2': 3},
        {'oper_1': {'min': 500, 'avg': 900, 'max': 1500}, 'oper_2': {'min': 500, 'avg': 1200, 'max': 1600}}
    )
])
def test_get_stat(records, counts_exp, times_exp):
    operations = ['oper_1', 'oper_2']
    counts, times = get_stat_by_records(records, operations)
    assert counts == counts_exp
    assert times == times_exp
