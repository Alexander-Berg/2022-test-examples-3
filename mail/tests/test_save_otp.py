import pytest
from psycopg2.errors import CheckViolation
from hamcrest import assert_that, contains, has_entries, has_length, greater_than
from .utils import exec_procedure, all_otps


@pytest.fixture(autouse=True)
def clean_otps(db_cursor):
    db_cursor.execute("DELETE FROM botdb.otps")


@pytest.fixture()
def otp(botpeer, mail_account):
    return {
        **botpeer,
        **mail_account,
        "otp_value": "0123456",
        "extra": {"context_id": "7MdigZ8LK4Y1"},
    }


@pytest.fixture()
def second_otp(botpeer, mail_account2):
    return {
        **botpeer,
        **mail_account2,
        "otp_value": "6543210",
        "extra": {"context_id": "5Md2VZ8LO8c1"},
    }


def save_otp(cur, otp):
    exec_procedure(cur, "code.save_otp", **otp)


def contains_otp(otp):
    return contains(has_entries(otp))


def test_save_otp(db_cursor, otp):
    save_otp(db_cursor, otp)
    assert_that(all_otps(db_cursor), contains_otp(otp))


def test_save_duplicate_updates_timestamp(db_cursor, otp):
    save_otp(db_cursor, otp)
    first_ts = all_otps(db_cursor)[0]["creation_ts"]
    save_otp(db_cursor, otp)  # second save of same otp
    otps = all_otps(db_cursor)
    assert_that(otps, has_length(1))
    second_ts = all_otps(db_cursor)[0]["creation_ts"]
    assert_that(second_ts, greater_than(first_ts))


def test_second_otp_rewrites_first(db_cursor, otp, second_otp):
    save_otp(db_cursor, otp)
    save_otp(db_cursor, second_otp)
    assert_that(all_otps(db_cursor), contains_otp(second_otp))


def test_cannot_save_otp_with_empty_uid(db_cursor, otp):
    otp["uid"] = ""
    with pytest.raises(CheckViolation):
        save_otp(db_cursor, otp)


def test_cannot_save_otp_with_empty_email(db_cursor, otp):
    otp["email"] = ""
    with pytest.raises(CheckViolation):
        save_otp(db_cursor, otp)
