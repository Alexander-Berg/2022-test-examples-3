import pytest
from fan.accounts.organizations.limits import set_trusty
from fan.models import OrganizationSettings


pytestmark = pytest.mark.django_db


def test_set_trusty(org_id):
    set_trusty(org_id, True)
    assert OrganizationSettings.objects.get(org_id=org_id).trusty == True


def test_set_untrusty(org_id):
    set_trusty(org_id, False)
    assert OrganizationSettings.objects.get(org_id=org_id).trusty == False


def test_pass_wrong_argument_raises_exception(org_id):
    with pytest.raises(Exception):
        set_trusty(org_id, 1)
