import pytest

CSV_ERRORS = (
    pytest.param('CSV_EMPTY', None, '', id='empty csv'),
    pytest.param(
        'CSV_HEADER_FIELD_REQUIRED', {'field_name': 'spwd'}, 'ylog,slog\nval1\nval2', id='skip header'
    ),
    pytest.param(
        'CSV_HEADER_FIELD_DUPLICATE',
        {'field_name': 'ylog'},
        'ylog,slog,spwd,ylog\nval1,val2,val3,val4',
        id='duplicate header'
    ),

    pytest.param(
        'CSV_TOO_BIG',
        None,
        'slog,ylog,spwd\n' + '\n'.join(['10,let,rs'] * 1024 * 50),
        id='big csv',
    ),

    # Чуть больше, чем 2 * _DEFAULT_LIMIT - ограничение на размер буфера в asyncio.StreamReader
    pytest.param(
        'CSV_LINE_TOO_BIG',
        {'max_size_bytes': 65536, 'lineno': 1},
        'a' * 1024 * 200,
        id='big line in csv'
    ),
    pytest.param(
        'CSV_MALFORMED', {'lineno': 3}, 'ylog,slog,spwd\nval1,val2,val3\nval1,val2\nval1,val2,val3',
        id='skipped column in line 3'
    ),
    pytest.param(
        'CSV_MALFORMED', {'lineno': 3}, 'ylog,slog,spwd\nval1,val2,val3\nval1,val2,val3,val4\nval1,val2,val3',
        id='extra column in line 3'
    ),
    pytest.param(
        'CSV_MALFORMED',
        {'lineno': 3, 'field_name': 'slog'},
        'ylog,slog,spwd\nval1,val2,val3\nval1,,val3\nval1,val2,val3',
        id='empty required field in line 3'
    ),
)
