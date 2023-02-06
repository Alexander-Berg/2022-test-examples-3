import pytest
from fan.message.letter import load_letter
from fan.message.render import render_eml
from fan.models import OrganizationSettings
from email import message_from_string as parse_eml
from fan.testutils.letter import get_html_body, load_test_letter
from fan.testutils.matchers import assert_contains_stat_pixel


pytestmark = pytest.mark.django_db


@pytest.fixture
def trusty_org_campaign(campaign_with_letter):
    OrganizationSettings.objects.update_or_create(
        org_id=campaign_with_letter.account.org_id, defaults={"trusty": True}
    )
    campaign_with_letter.account.save()
    return campaign_with_letter


@pytest.fixture
def letter_with_variable_in_different_case_html():
    letter_file = load_test_letter("letter_with_variable_in_different_case.html")
    yield letter_file
    letter_file.close()


@pytest.fixture
def campaign_with_letter_with_variable_in_different_case(
    campaign_with_letter, letter_with_variable_in_different_case_html
):
    load_letter(campaign_with_letter.default_letter, letter_with_variable_in_different_case_html)
    yield campaign_with_letter


def test_produces_eml_with_so_headers(campaign_with_letter):
    eml = render_eml(campaign_with_letter)
    parsed = parse_eml(eml)
    assert parsed["X-Sender-Account"] == "Free-Sender-test_org_id"


def test_produces_eml_with_so_headers_for_trusty_org(trusty_org_campaign):
    eml = render_eml(trusty_org_campaign)
    parsed = parse_eml(eml)
    assert parsed["X-Sender-Account"] == "B2B-Sender-test_org_id"


def test_contains_stat_pixel(campaign_with_letter):
    eml = render_eml(campaign_with_letter)
    parsed = parse_eml(eml)
    assert_contains_stat_pixel(get_html_body(parsed))


def test_contains_user_template_variable_placeholders(campaign_with_letter):
    eml = render_eml(campaign_with_letter)
    parsed = parse_eml(eml)
    assert "%user_variable_title%" not in parsed["Subject"]  # doesn't render subject
    assert "%user_variable_name%" in get_html_body(parsed)


def test_contains_user_template_variable_placeholders_in_lower_case(
    campaign_with_letter_with_variable_in_different_case,
):
    eml = render_eml(campaign_with_letter_with_variable_in_different_case)
    parsed = parse_eml(eml)
    assert get_html_body(parsed).count("%user_variable_var%") == 5
