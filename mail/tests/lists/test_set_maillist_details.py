import pytest
from django.conf import settings
from fan.lists.maillist import (
    set_maillist_details,
    MaillistTitleDuplicated,
    MaillistTitleLengthExceeded,
)
from fan.models import Maillist


pytestmark = pytest.mark.django_db


@pytest.fixture
def title():
    return "Новое название списка рассылки"


def test_returns_updated_maillist(maillist, title):
    maillist = set_maillist_details(maillist, title)
    assert maillist.title == title


def test_stores_updated_maillist(maillist, title):
    set_maillist_details(maillist, title)
    assert Maillist.objects.get(id=maillist.id).title == title


def test_fail_on_duplicated_title(maillist):
    with pytest.raises(MaillistTitleDuplicated):
        set_maillist_details(maillist, maillist.title)


def test_fail_on_too_long_title(maillist):
    with pytest.raises(MaillistTitleLengthExceeded):
        set_maillist_details(maillist, "T" * (settings.MAILLIST_TITLE_MAX_LENGTH + 1))
