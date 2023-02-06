import pytest
from fan.lists.maillist import get_maillist, MaillistDoesNotExist
from fan.models import Maillist


pytestmark = pytest.mark.django_db


def test_on_account_wo_maillist(account):
    with pytest.raises(MaillistDoesNotExist):
        get_maillist(account, "non-existent-maillist")


def test_on_account_with_maillist(account, maillist):
    res_maillist = get_maillist(account, maillist.slug)
    assert isinstance(res_maillist, Maillist)
    assert res_maillist.id == maillist.id
