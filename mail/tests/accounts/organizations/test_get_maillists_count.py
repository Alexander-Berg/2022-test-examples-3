import pytest
from fan.accounts.organizations.limits import get_maillists_count


pytestmark = pytest.mark.django_db


def test_on_account_wo_maillists(account):
    assert get_maillists_count(account.org_id) == 0


def test_on_unexisted_account():
    assert get_maillists_count("unexisted_org") == 0


def test_on_account_with_maillist(account, maillist):
    assert get_maillists_count(account.org_id) == 1


def test_on_account_with_maillists(account_with_almost_max_maillists_count):
    assert get_maillists_count(account_with_almost_max_maillists_count.org_id) == 4
