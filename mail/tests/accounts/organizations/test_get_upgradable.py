import pytest
from django.conf import settings
from fan.accounts.organizations.limits import get_upgradable


pytestmark = pytest.mark.django_db


def test_unknown_org(org_id):
    assert get_upgradable(org_id) == (not settings.DEFAULT_TRUSTY)


def test_org_with_custom_settingsg(org_with_custom_settings, trusty):
    assert get_upgradable(org_with_custom_settings) == (not trusty)


def test_org_with_overridden_settings(org_with_overridden_settings):
    upgradable = get_upgradable(org_with_overridden_settings["org_id"])
    assert upgradable == (not org_with_overridden_settings["TRUSTY_FOR_ORG"])
