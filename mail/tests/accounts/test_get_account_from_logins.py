import pytest
from django.conf import settings
from fan.accounts.get import get_account_from_logins


pytestmark = pytest.mark.django_db


@pytest.fixture
def account_without_from_logins(account):
    account.from_logins = []
    account.save()
    return account


def test_get_list(account):
    res = get_account_from_logins(account)
    assert sorted(res) == sorted(settings.ACCOUNT_FROM_LOGINS_DEFAULT)
    assert isinstance(res, list)


def test_get_empty_list(account_without_from_logins):
    assert len(get_account_from_logins(account_without_from_logins)) == 0
