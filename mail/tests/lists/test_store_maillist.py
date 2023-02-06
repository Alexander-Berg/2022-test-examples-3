import pytest
from zlib import decompress
from django.conf import settings
from fan.campaigns.set import ForbiddenCurrentCampaignState
from fan.lists.maillist import (
    store_maillist,
    MaillistsCountExceeded,
    MaillistTitleDuplicated,
    MaillistTitleLengthExceeded,
)
from fan.models import Campaign


pytestmark = pytest.mark.django_db


def test_account_has_no_maillists_initially(account):
    assert len(account.maillists.all()) == 0


def test_store_csv(account, maillist_csv_content):
    store_maillist(account, maillist_csv_content, "maillist.csv")
    assert len(account.maillists.all()) == 1
    maillist = account.maillists.all()[0]
    assert maillist.slug
    assert "Список получателей" in maillist.title
    assert maillist.filename == "maillist.csv"
    assert decompress(maillist.data).decode() == maillist_csv_content
    assert maillist.preview == [
        {"email": "a@b.c", "name": "Иван", "value": "100"},
        {"email": "d@e.f", "name": "Марья", "value": "50"},
    ]
    assert maillist.size == 2


def test_return_maillist(account, maillist_csv_content):
    maillist = store_maillist(account, maillist_csv_content, "maillist.csv")
    assert len(account.maillists.all()) == 1
    assert maillist == account.maillists.all()[0]


def test_fail_storing_too_many_maillists(
    account_with_max_maillists_count, maillist_csv_content, overriden_maillists_limit
):
    with pytest.raises(MaillistsCountExceeded):
        store_maillist(account_with_max_maillists_count, maillist_csv_content, "maillist.csv")
    assert len(account_with_max_maillists_count.maillists.all()) == overriden_maillists_limit


def test_set_maillist_for_draft_campaign(account, campaign, maillist_csv_content):
    store_maillist(account, maillist_csv_content, "maillist.csv", campaign=campaign)
    campaign.refresh_from_db()
    assert campaign.maillist is not None
    assert decompress(campaign.maillist.data).decode() == maillist_csv_content


@pytest.mark.parametrize(
    "wrong_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_fail_on_setting_maillist_for_wrong_state_campaign(
    account, campaign, maillist_csv_content, wrong_state
):
    force_set_campaign_state(campaign, wrong_state)
    with pytest.raises(ForbiddenCurrentCampaignState):
        store_maillist(account, maillist_csv_content, "maillist.csv", campaign=campaign)


def test_fail_on_campaign_from_another_account(account_with_users, campaign, maillist_csv_content):
    with pytest.raises(RuntimeError, match="campaign does not belong to account"):
        store_maillist(account_with_users, maillist_csv_content, "maillist.csv", campaign=campaign)


def test_set_title(account, maillist_csv_content):
    maillist = store_maillist(
        account, maillist_csv_content, "maillist.csv", title="Название списка"
    )
    assert maillist.title == "Название списка"


def test_fail_on_duplicated_title(account, maillist_csv_content):
    store_maillist(account, maillist_csv_content, "maillist.csv", title="Название списка")
    with pytest.raises(MaillistTitleDuplicated):
        store_maillist(account, maillist_csv_content, "maillist.csv", title="Название списка")


def test_fail_on_too_long_title(account, maillist_csv_content):
    with pytest.raises(MaillistTitleLengthExceeded):
        store_maillist(
            account,
            maillist_csv_content,
            "maillist.csv",
            title="T" * (settings.MAILLIST_TITLE_MAX_LENGTH + 1),
        )


def force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()
