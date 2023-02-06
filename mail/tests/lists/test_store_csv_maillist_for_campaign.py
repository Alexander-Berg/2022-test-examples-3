import pytest
from fan.campaigns.set import ForbiddenCurrentCampaignState
from fan.lists.singleuse import store_csv_maillist_for_campaign
from fan.models import Campaign


pytestmark = pytest.mark.django_db


def test_campaign_maillist_uploaded_false_initially(campaign):
    assert campaign.maillist_uploaded == False


def test_campaign_maillist_uploaded_true_after_list_upload(campaign, maillist_csv_content):
    store_csv_maillist_for_campaign(campaign, maillist_csv_content, "singleusemaillist.csv")
    assert campaign.maillist_uploaded == True


def test_unsets_maillist(campaign_with_maillist, maillist_csv_content):
    store_csv_maillist_for_campaign(
        campaign_with_maillist, maillist_csv_content, "singleusemaillist.csv"
    )
    assert campaign_with_maillist.maillist == None
    assert campaign_with_maillist.maillist_uploaded == True
    assert campaign_with_maillist.maillist_description == "singleusemaillist.csv"


def test_does_not_delete_maillist(campaign_with_maillist, maillist_csv_content):
    store_csv_maillist_for_campaign(
        campaign_with_maillist, maillist_csv_content, "singleusemaillist.csv"
    )
    assert len(campaign_with_maillist.account.maillists.all()) == 1


def test_stores_preview(campaign, maillist_csv_content):
    store_csv_maillist_for_campaign(campaign, maillist_csv_content, "singleusemaillist.csv")
    assert campaign.single_use_maillist.data_preview == [
        {"name": "Иван", "value": "100", "email": "a@b.c"},
        {"name": "Марья", "value": "50", "email": "d@e.f"},
    ]


def test_stores_preview_with_user_template_variables(campaign, maillist_content_with_missed_values):
    store_csv_maillist_for_campaign(
        campaign, maillist_content_with_missed_values, "singleusemaillist.csv"
    )
    assert campaign.single_use_maillist.data_preview == [
        {"email": "a@ya.ru", "a": "a1", "b": "b1"},
        {"email": "b@ya.ru", "a": "a2", "b": ""},
    ]


def test_stores_preview_with_user_template_variables_with_underscore(
    campaign, maillist_content_with_underscored_header
):
    store_csv_maillist_for_campaign(
        campaign, maillist_content_with_underscored_header, "singleusemaillist.csv"
    )
    assert campaign.single_use_maillist.data_preview == [
        {"email": "a@ya.ru", "b": "b1"},
    ]


def test_stores_preview_with_headers_in_lower_case(
    campaign, maillist_content_with_capital_letters_in_headers
):
    store_csv_maillist_for_campaign(
        campaign, maillist_content_with_capital_letters_in_headers, "singleusemaillist.csv"
    )
    assert campaign.single_use_maillist.data_preview == [
        {"email": "a@ya.ru", "abc": "a1", "cde": "b1"},
    ]


def test_stores_preview_without_duplicated_headers_in_different_case(
    campaign, maillist_content_with_duplicated_headers_in_different_case
):
    store_csv_maillist_for_campaign(
        campaign,
        maillist_content_with_duplicated_headers_in_different_case,
        "singleusemaillist.csv",
    )
    assert campaign.single_use_maillist.data_preview == [
        {"email": "a@ya.ru", "abc": "a1"},
    ]


def test_stores_preview_without_duplicated_headers(
    campaign, maillist_content_with_duplicated_headers
):
    store_csv_maillist_for_campaign(
        campaign, maillist_content_with_duplicated_headers, "singleusemaillist.csv"
    )
    assert campaign.single_use_maillist.data_preview == [
        {"email": "a@ya.ru", "abc": "a1"},
    ]


def test_stores_preview_with_user_template_variables_tab_separated(
    campaign, maillist_content_with_user_template_variables_tab_separated
):
    store_csv_maillist_for_campaign(
        campaign,
        maillist_content_with_user_template_variables_tab_separated,
        "singleusemaillist.csv",
    )
    assert campaign.single_use_maillist.data_preview == [
        {"email": "a@ya.ru", "a": "a1", "b": "b1"},
        {"email": "b@ya.ru", "a": "a2", "b": ""},
    ]


def test_stores_preview_with_user_template_variables_with_empty_lines(
    campaign, maillist_content_with_user_template_variables_with_empty_lines
):
    store_csv_maillist_for_campaign(
        campaign,
        maillist_content_with_user_template_variables_with_empty_lines,
        "singleusemaillist.csv",
    )
    assert campaign.single_use_maillist.data_preview == [
        {"email": "a@ya.ru", "a": "a1", "b": "b1"},
        {"email": "b@ya.ru", "a": "a2", "b": ""},
    ]


def test_stores_preview_with_user_template_variables_without_header(
    campaign, maillist_content_with_user_template_variables_without_header
):
    store_csv_maillist_for_campaign(
        campaign,
        maillist_content_with_user_template_variables_without_header,
        "singleusemaillist.csv",
    )
    assert campaign.single_use_maillist.data_preview == [
        {"email": "a@ya.ru", "col2": "a1", "col3": "b1"},
        {"email": "b@ya.ru", "col2": "a2", "col3": ""},
    ]


@pytest.mark.parametrize(
    "wrong_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_singleuse_raises_exception_on_campaign_wrong_state(
    campaign, maillist_csv_content, wrong_state
):
    _force_set_campaign_state(campaign, wrong_state)
    with pytest.raises(ForbiddenCurrentCampaignState):
        store_csv_maillist_for_campaign(campaign, maillist_csv_content, "singleusemaillist.csv")


def _force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()
