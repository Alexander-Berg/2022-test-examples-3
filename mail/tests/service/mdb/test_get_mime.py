import logging

import pytest
from psycopg2 import DatabaseError, Error

from calendar_attach_processor.service.mdb import MailPg

logging.basicConfig(level=logging.DEBUG)


@pytest.fixture
def pg_connect(mocker):
    pg_connect_mock = mocker.patch("psycopg2.connect")
    return pg_connect_mock


@pytest.fixture
def pg_extras(mocker):
    pg_extras_mock = mocker.patch("psycopg2.extras")
    return pg_extras_mock


def test_retries_in_case_of_db_errors(pg_connect, pg_extras):
    pg_conn = pg_connect.return_value
    pg_cursor = pg_conn.__enter__.return_value.cursor.return_value
    pg_cursor.__enter__.return_value.fetchone.side_effect = DatabaseError()
    with pytest.raises(Error):
        MailPg('dsn').get_mime("2345", "1234567890")
    assert pg_cursor.__enter__.return_value.fetchone.call_count == 3


@pytest.mark.parametrize("fetch_result, expected", [
    (None, [])
])
def test_get_mime(pg_connect, pg_extras, fetch_result, expected):
    pg_conn = pg_connect.return_value
    pg_cursor = pg_conn.__enter__.return_value.cursor.return_value
    pg_cursor.__enter__.return_value.fetchone.return_value = fetch_result
    mime_parts = MailPg('dsn').get_mime("2345", "1234567890")
    assert mime_parts == expected
