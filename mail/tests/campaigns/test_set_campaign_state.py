import pytest
from fan.campaigns.exceptions import (
    ForbiddenTargetCampaignState,
    ForbiddenCurrentCampaignState,
    NoMaillist,
    NoLetter,
    EmptyFromEmail,
    EmptyFromName,
    EmptySubject,
    DomainNotBelongs,
    DomainNoMX,
    LoginNotBelongs,
)
from fan.campaigns.set import set_campaign_details, set_campaign_state
from fan.models import Campaign


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize(
    "wrong_dst_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_SENDING],
)
def test_wrong_dst_state(ready_campaign, wrong_dst_state):
    with pytest.raises(ForbiddenTargetCampaignState):
        set_campaign_state(ready_campaign, wrong_dst_state)


@pytest.mark.parametrize(
    "wrong_src_state",
    [state for state in Campaign.VALID_GLOBAL_STATES if state != Campaign.STATUS_DRAFT],
)
def test_wrong_src_state(ready_campaign, wrong_src_state, sending_state):
    _force_set_campaign_state(ready_campaign, wrong_src_state)
    with pytest.raises(ForbiddenCurrentCampaignState):
        set_campaign_state(ready_campaign, sending_state)


def test_no_maillist(
    campaign_with_letter, sending_state, mock_tvm, mock_directory_domains, mock_gendarme
):
    with pytest.raises(NoMaillist):
        set_campaign_state(campaign_with_letter, sending_state)


def test_no_letter(campaign_with_singleusemaillist, sending_state):
    with pytest.raises(NoLetter):
        set_campaign_state(campaign_with_singleusemaillist, sending_state)


def test_empty_from_email(ready_campaign, sending_state):
    _clear_campaign_detail(ready_campaign, "from_email")
    with pytest.raises(EmptyFromEmail):
        set_campaign_state(ready_campaign, sending_state)


def test_domain_not_belongs(
    ready_campaign, sending_state, from_login, mock_tvm, mock_directory_domains
):
    set_campaign_details(ready_campaign, {"from_email": "{}@foreign_domain.ru".format(from_login)})
    with pytest.raises(DomainNotBelongs):
        set_campaign_state(ready_campaign, sending_state)


def test_domain_no_mx(
    ready_campaign, sending_state, mock_tvm, mock_directory_domains, mock_gendarme
):
    mock_gendarme.resp_mx = False
    with pytest.raises(DomainNoMX):
        set_campaign_state(ready_campaign, sending_state)


def test_empty_from_name(ready_campaign, sending_state):
    _clear_campaign_detail(ready_campaign, "from_name")
    with pytest.raises(EmptyFromName):
        set_campaign_state(ready_campaign, sending_state)


def test_from_login_check_on(
    campaign_with_wrong_from_login,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    permit_setting_check_campaign_from_login,
):
    with pytest.raises(LoginNotBelongs):
        set_campaign_state(campaign_with_wrong_from_login, sending_state)


def test_from_login_check_off(
    campaign_with_wrong_from_login,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    prohibit_setting_check_campaign_from_login,
):
    set_campaign_state(campaign_with_wrong_from_login, sending_state)
    assert (
        Campaign.objects.get(
            slug=campaign_with_wrong_from_login.slug,
            account_id=campaign_with_wrong_from_login.account.id,
        ).state
        == sending_state
    )


def test_empty_subject(ready_campaign, sending_state):
    _clear_campaign_detail(ready_campaign, "subject")
    with pytest.raises(EmptySubject):
        set_campaign_state(ready_campaign, sending_state)


def test_set_sending_state(
    ready_campaign, sending_state, mock_tvm, mock_directory_domains, mock_gendarme
):
    set_campaign_state(ready_campaign, sending_state)
    assert (
        Campaign.objects.get(slug=ready_campaign.slug, account_id=ready_campaign.account.id).state
        == sending_state
    )


def test_set_sending_state_for_new_untrusty_account(
    ready_campaign_from_new_untrusty_account,
    sending_state,
    sent_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    enable_silently_drop_sents_for_new_untrusty_accounts,
):
    set_campaign_state(ready_campaign_from_new_untrusty_account, sending_state)
    assert (
        Campaign.objects.get(
            slug=ready_campaign_from_new_untrusty_account.slug,
            account_id=ready_campaign_from_new_untrusty_account.account.id,
        ).state
        == sent_state
    )


def test_set_sending_state_for_old_unknown_account(
    ready_campaign_from_old_unknown_account,
    sending_state,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    enable_silently_drop_sents_for_new_untrusty_accounts,
):
    set_campaign_state(ready_campaign_from_old_unknown_account, sending_state)
    assert (
        Campaign.objects.get(
            slug=ready_campaign_from_old_unknown_account.slug,
            account_id=ready_campaign_from_old_unknown_account.account.id,
        ).state
        == sending_state
    )


@pytest.fixture
def sending_state():
    return Campaign.STATUS_SENDING


def _clear_campaign_detail(campaign, detail_name):
    set_campaign_details(campaign, {detail_name: ""})


def _force_set_campaign_state(campaign, state):
    campaign.state = state
    campaign.save()
