import pytest
from django.conf import settings
from fan.accounts.set import (
    set_account_from_logins,
    AccountFromLoginsEmpty,
    AccountFromLoginsTooLong,
    AccountFromLoginInvalid,
)


pytestmark = pytest.mark.django_db


@pytest.fixture
def prohibit_setting_empty_from_logins():
    saved_value = settings.ACCOUNT_FROM_LOGINS_COUNT_MIN
    settings.ACCOUNT_FROM_LOGINS_COUNT_MIN = 1
    yield
    settings.ACCOUNT_FROM_LOGINS_COUNT_MIN = saved_value


@pytest.fixture
def from_logins_empty():
    return []


@pytest.fixture
def from_logins_too_long():
    return [str(uid) for uid in range(0, settings.ACCOUNT_FROM_LOGINS_COUNT_MAX + 1)]


@pytest.fixture
def from_logins_with_invalid():
    return ["valid", ";invalid;"]


@pytest.fixture
def from_logins_long():
    return [str(uid) for uid in range(0, settings.ACCOUNT_FROM_LOGINS_COUNT_MAX)]


@pytest.fixture
def from_logins_with_duplicates():
    return ["uid", "uid"]


def test_set_empty_list_fails(account, from_logins_empty, prohibit_setting_empty_from_logins):
    with pytest.raises(AccountFromLoginsEmpty):
        set_account_from_logins(account, from_logins_empty)


def test_set_too_long_list_fails(account, from_logins_too_long):
    with pytest.raises(AccountFromLoginsTooLong):
        set_account_from_logins(account, from_logins_too_long)


def test_set_invalid_localpart_fails(account, from_logins_with_invalid):
    with pytest.raises(AccountFromLoginInvalid):
        set_account_from_logins(account, from_logins_with_invalid)


def test_set_long_list(account, from_logins_long):
    set_account_from_logins(account, from_logins_long)
    assert sorted(refresh_from_db(account).from_logins) == sorted(from_logins_long)


def test_set_removes_duplicates(account, from_logins_with_duplicates):
    set_account_from_logins(account, from_logins_with_duplicates)
    assert len(refresh_from_db(account).from_logins) == len(set(from_logins_with_duplicates))


def test_set_default(account):
    set_account_from_logins(account)
    assert sorted(refresh_from_db(account).from_logins) == sorted(
        set(settings.ACCOUNT_FROM_LOGINS_DEFAULT)
    )


def refresh_from_db(obj):
    obj.refresh_from_db()
    return obj
