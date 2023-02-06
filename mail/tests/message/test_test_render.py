import pytest
from fan.message.render import test_render
from fan.models import OrganizationSettings
from email import message_from_string as parse_eml
from fan.testutils.letter import get_html_body
from fan.testutils.matchers import assert_contains_stat_pixel


pytestmark = pytest.mark.django_db


@pytest.fixture
def trusty_org_campaign(campaign_with_letter):
    OrganizationSettings.objects.update_or_create(
        org_id=campaign_with_letter.account.org_id, defaults={"trusty": True}
    )
    campaign_with_letter.account.save()
    return campaign_with_letter


def test_contains_stat_pixel(campaign_with_letter):
    message = test_render(campaign_with_letter.default_letter)
    parsed = parse_eml(message.as_message().as_string())
    assert_contains_stat_pixel(get_html_body(parsed))


def test_produces_eml_with_so_headers(campaign_with_letter):
    message = test_render(campaign_with_letter.default_letter)
    parsed = parse_eml(message.as_message().as_string())
    assert parsed["X-Sender-Account"] == "Free-Sender-test_org_id"


def test_produces_eml_with_so_headers_for_trusty_org(trusty_org_campaign):
    message = test_render(trusty_org_campaign.default_letter)
    parsed = parse_eml(message.as_message().as_string())
    assert parsed["X-Sender-Account"] == "B2B-Sender-test_org_id"
