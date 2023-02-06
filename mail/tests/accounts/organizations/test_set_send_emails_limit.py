import pytest
from django.conf import settings
from fan.accounts.organizations.limits import set_send_emails_limit, default_send_emails_limit
from fan.models import OrganizationSettings


pytestmark = pytest.mark.django_db


def test_set_limit(org_id):
    set_send_emails_limit(org_id, 50000)
    assert OrganizationSettings.objects.get(org_id=org_id).send_emails_limit == 50000


def test_set_default_limit(org_id):
    set_send_emails_limit(org_id, default_send_emails_limit())
    assert (
        OrganizationSettings.objects.get(org_id=org_id).send_emails_limit
        == settings.DEFAULT_SEND_EMAILS_LIMIT
    )


def test_set_negative_limit_fails(org_id):
    with pytest.raises(Exception):
        set_send_emails_limit(org_id, -13)
