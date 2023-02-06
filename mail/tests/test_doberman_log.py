import pytest
from mail.doberman.unistat.lib.log import (
    parse_record,
    get_stat_name,
    get_stat_by_records,
)


@pytest.mark.parametrize(('row', 'expected'), [
    ('key=val', {'key': 'val'}),
    ('key=', {'key': ''}),
    ('key', {'key': None}),
    ('key1=val1\tkey2=val 2', {'key1': 'val1', 'key2': 'val 2'}),
    ('key1=val1\tkey2=val=2', {'key1': 'val1', 'key2': 'val=2'})
])
def test_parse_record(row, expected):
    parsed = parse_record(row)
    assert parsed == expected


@pytest.mark.parametrize(('record', 'expected'), [
    ({}, None),
    ({'message': None}, None),
    ({'message': ''}, None),
    ({'message': '#val 1#'}, 'val1'),
    ({'category': '#val 2#'}, 'val2')
])
def test_get_stat_name(record, expected):
    values = [
        {'name': 'val1', 'field': 'message', 'grep': 'val 1'},
        {'name': 'val2', 'field': 'category', 'grep': 'val 2'},
    ]
    idx = get_stat_name(record, values)
    assert idx == expected


@pytest.mark.parametrize(('records', 'expected'), [
    (
        ['level=warning\tcategory=some error'],
        {'error': {'some': 0, 'other': 0, 'unexpected': 0},
         'notice': {'some': 0}}
    ),
    (
        ['level=error\tcategory=some error'],
        {'error': {'some': 1, 'other': 0, 'unexpected': 0},
         'notice': {'some': 0}}
    ),
    (
        ['level=error\ttype=other'],
        {'error': {'some': 0, 'other': 1, 'unexpected': 0},
         'notice': {'some': 0}}
    ),
    (
        ['level=error\tcategory=other'],
        {'error': {'some': 0, 'other': 0, 'unexpected': 1},
         'notice': {'some': 0}}
    ),
    (
        ['level=notice\tmessage=some notice'],
        {'error': {'some': 0, 'other': 0, 'unexpected': 0},
         'notice': {'some': 1}}
    ),
    (
        ['level=notice\tmessage=any notice'],
        {'error': {'some': 0, 'other': 0, 'unexpected': 0},
         'notice': {'some': 0}}
    ),
    (
        ['level=error\tcategory=some error',
         'level=notice\tmessage=some notice',
         'level=error\tcategory=some error'],
        {'error': {'some': 2, 'other': 0, 'unexpected': 0},
         'notice': {'some': 1}}
    )
])
def test_get_stat(records, expected):
    monitors = {
        'error': [
            {'name': 'some', 'field': 'category', 'grep': 'some error'},
            {'name': 'other', 'field': 'type', 'grep': 'other'},
        ],
        'notice': [
            {'name': 'some', 'field': 'message', 'grep': 'some notice'},
        ]
    }
    stat = get_stat_by_records(records, monitors)
    assert stat == expected
