import pytest
import requests
from datetime import date, datetime
from fan.db import clickhouse


pytestmark = pytest.mark.django_db


@pytest.fixture
def mock_clickhouse(mocker):
    mock = mocker.patch("requests.post")
    mock.args, mock.kwargs = None, None

    def side_effect(*args, **kwargs):
        mock.args, mock.kwargs = args, kwargs
        response = requests.Response()
        response.status_code = 200
        return response

    mock.side_effect = side_effect
    return mock


def test_query():
    query = clickhouse.Query(b"SELECT 1")
    assert query.value == b"SELECT 1\n"


def test_query_accepts_str():
    query = clickhouse.Query("SELECT 1")
    assert query.value == b"SELECT 1\n"


def test_query_rows_and_reset():
    query = clickhouse.Query(b"INSERT INTO table FORMAT TabSeparated")
    query.appendRow(b"hello\tworld", b"'hello world'")
    assert (
        query.value == b"INSERT INTO table FORMAT TabSeparated\nhello\\tworld\t\\'hello world\\'\n"
    )
    query.reset()
    assert query.value == b"INSERT INTO table FORMAT TabSeparated\n"


def test_import_star():
    d = {}
    exec("from fan.db.clickhouse import *", d, d)
    assert "ClickHouseQuery" in d
    assert "ClickHouseClient" in d


def test_quote_value():
    tests = (
        (b"test", b"test"),
        (b"te'st", b"te\\'st"),
        ("Проверка", "Проверка".encode()),
        (date(2014, 4, 10), b"2014-04-10"),
        (datetime(2014, 4, 10, 15, 0o1, 27), b"2014-04-10 15:01:27"),
        (123, b"123"),
        (123, b"123"),
        (123.5, b"123.500000"),
    )
    for source, expected in tests:
        assert clickhouse.quote_value(source) == expected


def test_unquote_value():
    tests = (
        (b"test", b"test"),
        (b"te\\nst", b"te\nst"),
        (b"te\\'st", b"te'st"),
        ("Проверка".encode(), "Проверка".encode()),
        ("Проверка", "Проверка".encode()),
    )
    for source, expected in tests:
        assert clickhouse.unquote_value(source) == expected


def test_unquote_row():
    tests = (
        (b"hello\tworld", [b"hello", b"world"]),
        (b"hello\tworld\n", [b"hello", b"world"]),
        (b"he\\tllo\twor\\'ld\n", [b"he\tllo", b"wor'ld"]),
    )
    for source, expected in tests:
        assert clickhouse.unquote_row(source) == expected


def test_client_request_contains_auth_header(mock_clickhouse):
    clickhouse.Client(username="username", password="password").query("SELECT 1")
    assert "headers" in mock_clickhouse.kwargs
    assert mock_clickhouse.kwargs["headers"] == {"Authorization": "Basic dXNlcm5hbWU6cGFzc3dvcmQ="}
