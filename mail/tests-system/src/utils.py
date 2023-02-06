from __future__ import print_function
from psycopg2.extras import Json
import psycopg2
import requests
import time
from hamcrest import assert_that, contains, has_property, has_entry, has_entries, anything


def wait_webservice_is_ready(url):
    postponed_exception = None
    for _ in range(5):
        time.sleep(0.1)
        try:
            requests.get(url, timeout=0.5)
            return
        except Exception as e:
            postponed_exception = e
    raise postponed_exception


def wait_db_is_ready(db_connstring):
    postponed_exception = None
    for _ in range(10):
        time.sleep(1)
        try:
            psycopg2.connect(db_connstring)
            return
        except Exception as e:
            postponed_exception = e
    raise postponed_exception


def link(botpeer, mail_account):
    return {**botpeer, **mail_account, "extra": {}}


def otp(botpeer, mail_account):
    return {
        **botpeer,
        **mail_account,
        "otp_value": "0123456",
        "extra": {},
    }


class DB:
    def __init__(self, db_cursor):
        self.db_cursor = db_cursor

    def create_link(self, link):
        self._exec_procedure("code.create_link", **link)

    def clean_links(self):
        self.db_cursor.execute("DELETE FROM botdb.links")

    @property
    def all_links(self):
        self.db_cursor.execute("SELECT * FROM botdb.links")
        return self._rows_to_dicts(self.db_cursor.fetchall())

    def create_otp(self, otp):
        self._exec_procedure("code.save_otp", **otp)

    def clean_otps(self):
        self.db_cursor.execute("DELETE FROM botdb.otps")

    @property
    def all_otps(self):
        self.db_cursor.execute("SELECT * FROM botdb.otps")
        return self._rows_to_dicts(self.db_cursor.fetchall())

    def _exec_procedure(self, name, **kwargs):
        sql_args = ", ".join(
            [f"i_{key} => %({key})s::{self._sql_type(val)}" for key, val in kwargs.items()]
        )
        sql = f"SELECT * FROM {name}({sql_args})"

        self.db_cursor.execute(sql, {key: self._to_sql_type(val) for key, val in kwargs.items()})
        return self._rows_to_dicts(self.db_cursor.fetchall())

    def _to_sql_type(self, val):
        if self._sql_type(val) == "jsonb":
            return Json(val)
        # no conversion required for simple types
        return val

    def _sql_type(self, val):
        if isinstance(val, str):
            return "text"
        if isinstance(val, int):
            return "bigint"
        if isinstance(val, dict):
            return "jsonb"

    def _rows_to_dicts(self, rows):
        return [{k: v for k, v in row.items()} for row in rows]


def contains_otp(botpeer, mail_account):
    return has_property(
        "all_otps",
        contains(has_entries(**botpeer, **mail_account, otp_value=anything(), extra=anything())),
    )


def contains_link(botpeer, mail_account):
    return has_property(
        "all_links", contains(has_entries(**botpeer, **mail_account, extra=anything()))
    )


def contains_request(request_text):
    return has_property("send_message_requests", contains(has_entry("text", request_text)))


def contains_requests(*args):
    return has_property(
        "send_message_requests", contains(*map(lambda text: has_entry("text", text), args))
    )


def assert_status(resp, code):
    assert_that(resp, has_property("status_code", code))
