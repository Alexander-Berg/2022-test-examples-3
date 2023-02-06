import pytest
from fan.message.letter import load_letter
from fan.message.render import render_test_eml
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


def test_produces_eml_with_correct_from_to(campaign_with_letter):
    eml = render_test_eml(campaign_with_letter, "recipient@test.ru")
    parsed = parse_eml(eml)
    assert parsed["From"] == "=?utf-8?b?TWUgUm9ib3Q=?= <no-reply@test.ru>"
    assert parsed["To"] == "recipient@test.ru"


def test_produces_multipart_message(campaign_with_letter):
    eml = render_test_eml(campaign_with_letter, "recipient@test.ru")
    parsed = parse_eml(eml)
    assert parsed.is_multipart()


def test_contains_stat_pixel(campaign_with_letter):
    eml = render_test_eml(campaign_with_letter, "recipient@test.ru")
    parsed = parse_eml(eml)
    assert_contains_stat_pixel(get_html_body(parsed))


def test_substitutes_user_template_variables(campaign_with_letter):
    eml = render_test_eml(
        campaign_with_letter,
        "recipient@test.ru",
        user_template_variables={"name": "Any Name"},
    )
    parsed = parse_eml(eml)
    assert "{{ title }}" in parsed["Subject"]  # doesn't render subject
    assert "Any Name" in get_html_body(parsed)


def test_user_template_variables_are_case_insensitive(
    campaign_with_letter_with_variable_in_different_case,
):
    eml = render_test_eml(
        campaign_with_letter_with_variable_in_different_case,
        "recipient@test.ru",
        user_template_variables={"VaR": "user_value"},
    )
    parsed = parse_eml(eml)
    assert get_html_body(parsed).count("user_value") == 5


def test_produces_eml_with_so_headers(campaign_with_letter):
    eml = render_test_eml(campaign_with_letter, "recipient@test.ru")
    parsed = parse_eml(eml)
    assert parsed["X-Sender-Account"] == "Free-Sender-test_org_id"


def test_produces_eml_with_so_headers_for_trusty_org(trusty_org_campaign):
    eml = render_test_eml(trusty_org_campaign, "recipient@test.ru")
    parsed = parse_eml(eml)
    assert parsed["X-Sender-Account"] == "B2B-Sender-test_org_id"
