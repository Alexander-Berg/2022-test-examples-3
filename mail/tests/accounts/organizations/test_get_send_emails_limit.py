import pytest
from django.conf import settings
from fan.accounts.organizations.limits import get_send_emails_limit


pytestmark = pytest.mark.django_db


def test_unknown_org(org_id):
    assert get_send_emails_limit(org_id) == settings.DEFAULT_SEND_EMAILS_LIMIT


def test_org_with_custom_settings(org_with_custom_settings, large_send_emails_limit):
    assert get_send_emails_limit(org_with_custom_settings) == large_send_emails_limit


def test_org_with_overridden_settings(org_with_overridden_settings):
    limit = get_send_emails_limit(org_with_overridden_settings["org_id"])
    assert limit == org_with_overridden_settings["SEND_EMAILS_LIMIT_FOR_ORG"]
