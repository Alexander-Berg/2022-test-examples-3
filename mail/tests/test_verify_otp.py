import pytest
from hamcrest import assert_that, equal_to, empty, has_length
from .utils import exec_procedure, all_otps


@pytest.fixture()
def otp_value():
    return "012345"


@pytest.fixture()
def otp(botpeer, mail_account, otp_value):
    return {
        **botpeer,
        **mail_account,
        "otp_value": otp_value,
        "extra": {"context_id": "7MdweZ8LKmI1"},
    }


@pytest.fixture()
def empty_account():
    return {"uid": None, "email": None}


@pytest.fixture(autouse=True)
def prepare_otp(db_cursor, otp):
    db_cursor.execute("DELETE FROM botdb.otps")
    exec_procedure(db_cursor, "code.save_otp", **otp)


def verify_otp(cur, botpeer, otp_value, ttl_sec=120):
    res = exec_procedure(cur, "code.verify_otp", **botpeer, otp_value=otp_value, ttl_sec=ttl_sec)
    assert_that(res, has_length(1))  # function must return exactly 1 row
    return res[0]


def test_verify_otp(db_cursor, botpeer, mail_account, otp_value):
    res_account = verify_otp(db_cursor, botpeer, otp_value)
    assert_that(res_account, equal_to(mail_account))
    assert_that(all_otps(db_cursor), empty())


def test_not_verify_bad_otp(db_cursor, botpeer, empty_account, otp_value):
    reverse_otp = otp_value[::-1]
    res_account = verify_otp(db_cursor, botpeer, reverse_otp)
    assert_that(res_account, equal_to(empty_account))
    assert_that(all_otps(db_cursor), empty())


def test_not_verify_expired_otp(db_cursor, botpeer, empty_account, otp_value):
    res_account = verify_otp(db_cursor, botpeer, otp_value, ttl_sec=0)
    assert_that(res_account, equal_to(empty_account))
    assert_that(all_otps(db_cursor), empty())
