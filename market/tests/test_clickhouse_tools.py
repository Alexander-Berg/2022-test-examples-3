# -*- coding: utf-8 -*-

import mock
import pytest

import io
import requests

from market.idx.pylibrary.clickhouse.clickhouse_tools import (
    escape_and_quote,
    escape_and_quote_parameters,
    ClickHouseConfig,
    get_clickhouse_data,
    _add_prepared_statements
)


# Проверка для int, float
@pytest.mark.parametrize('param, expected_escaped_param_as_string', [
    (12, '12'),
    (23.3, '23.3')
])
def test_escape_quote_int_float(param, expected_escaped_param_as_string):
    escaped = escape_and_quote(param)
    assert isinstance(escaped, str)
    assert escaped == expected_escaped_param_as_string


# Проверка для строк, в том числе содержащих спецсимволы
@pytest.mark.parametrize('param, expected_escaped_param_no_bracing_quotes', [
    ('someString', 'someString'),
    ('someStringWithQuote\'afterQuote', 'someStringWithQuote\\\'afterQuote'),
    ('someStringWithBackSlash\\afterSlash', 'someStringWithBackSlash\\\\afterSlash'),
    ('abc\\\'', 'abc\\\\\\\'')  # abc\' -> abc\\\'
])
def test_escape_strings(param, expected_escaped_param_no_bracing_quotes):
    escaped = escape_and_quote(param)
    assert isinstance(escaped, str)
    # Строка должна экранироваться одинарными кавычками
    assert escaped.startswith('\'')
    assert escaped.endswith('\'')
    escaped_no_bracing_quotes = escaped[1:-1]
    assert escaped_no_bracing_quotes == expected_escaped_param_no_bracing_quotes


# Проверка экранирования нескольких параметров
def test_multiple_parameters():
    query = 'SELECT * from T where feed_id = {} and session_name = {}'\
        .format(*escape_and_quote_parameters([12, '20180101_0000']))
    assert query == 'SELECT * from T where feed_id = 12 and session_name = \'20180101_0000\''


@pytest.fixture(scope='module')
def ch_config():
    yield ClickHouseConfig(
        host='http://fake.ch.com',
        port=16,
        user='user',
        passwd='passwd'
    )


def create_clickhouse_response(
    status_code=200,
    raw=None
):
    response = requests.models.Response()
    response.headers = {u'content-type': u'text/plain'}
    response.status_code = status_code
    response.raw = io.BytesIO(raw)

    return response


@pytest.fixture(scope='module')
def set_clickhouse_response():
    with mock.patch('requests.post') as patch:
        def fn(
                status_code=200,
                raw=b'message',
                side_effect=None
        ):
            patch.return_value = create_clickhouse_response(status_code, raw)
            patch.side_effect = side_effect

        yield fn


def test_get_clickhouse_data_ok(ch_config, set_clickhouse_response):
    set_clickhouse_response(raw=b'a\tb\nc\td\n')
    expected = [[u'a', u'b'], [u'c', u'd']]
    actual = get_clickhouse_data('some_query', ch_config)
    assert expected == actual


def test_get_clickhouse_data_fail(ch_config, set_clickhouse_response):
    set_clickhouse_response(raw=b'a\tb\tc\n', status_code=404)
    with pytest.raises(RuntimeError, match='someting went wrong during process query: "some_query"'):
        get_clickhouse_data('some_query', ch_config)


def test_get_clickhouse_data_retries(ch_config, set_clickhouse_response):
    responses = (
        (500, None),
        (500, None),
        (200, b'yay')
    )
    set_clickhouse_response(
        side_effect=[create_clickhouse_response(status_code=code, raw=raw) for code, raw in responses]
    )
    actual = get_clickhouse_data('some_query', ch_config)
    assert actual == [[u'yay']]


URL = 'http://fake.ch.com:16/'


# Проверка для строк, в том числе содержащих спецсимволы
@pytest.mark.parametrize('prepared_statements, result_url', [
    ({}, URL),
    ({'a': 'str', 'b': 1}, URL + '?param_a=str&param_b=1'),
])
def test_prepared_statements(prepared_statements, result_url):
    assert _add_prepared_statements(URL, prepared_statements) == result_url
