import pytest
from fan.accounts.organizations.limits import is_maillists_limit_reached


pytestmark = pytest.mark.django_db


def test_on_account_wo_maillists(account):
    assert is_maillists_limit_reached(account.org_id) == False


def test_on_unexisted_account():
    assert is_maillists_limit_reached("unexisted_org") == False


def test_on_account_with_almost_max_maillists_count(account_with_almost_max_maillists_count):
    assert is_maillists_limit_reached(account_with_almost_max_maillists_count.org_id) == False


def test_on_account_with_max_maillists_count(account_with_max_maillists_count):
    assert is_maillists_limit_reached(account_with_max_maillists_count.org_id) == True
