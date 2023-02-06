import pytest
from django.conf import settings
from fan.accounts.organizations.limits import get_trusty


pytestmark = pytest.mark.django_db


def test_unknown_org(org_id):
    assert get_trusty(org_id) == settings.DEFAULT_TRUSTY


def test_org_with_custom_settingsg(org_with_custom_settings, trusty):
    assert get_trusty(org_with_custom_settings) == trusty


def test_org_with_overridden_settings(org_with_overridden_settings):
    trusty = get_trusty(org_with_overridden_settings["org_id"])
    assert trusty == org_with_overridden_settings["TRUSTY_FOR_ORG"]
