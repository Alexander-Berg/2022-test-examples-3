import pytest
from fan.lists.maillist import get_maillists
from fan.models import Maillist


pytestmark = pytest.mark.django_db


def test_on_account_wo_maillist(account):
    maillists = get_maillists(account)
    assert len(maillists) == 0


def test_on_account_with_maillist(account, maillist):
    maillists = get_maillists(account)
    assert len(maillists) == 1
    assert isinstance(maillists[0], Maillist)


def test_on_account_with_maillists(account_with_almost_max_maillists_count):
    maillists = get_maillists(account_with_almost_max_maillists_count)
    assert len(maillists) == 4
