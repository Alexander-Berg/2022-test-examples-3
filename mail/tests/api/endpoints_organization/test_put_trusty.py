import urllib.request, urllib.parse, urllib.error
import pytest
from rest_framework import status
from fan.models import OrganizationSettings
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_status_code,
    assert_validation_error,
)


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def default_fixtures(mock_tvm, tvm_api_client):
    global api_client
    api_client = tvm_api_client
    yield
    api_client = None


@pytest.fixture
def trusty_org(org_id):
    OrganizationSettings.objects.update_or_create(org_id=org_id, defaults={"trusty": True})
    return org_id


def test_missing_org_id():
    response = do_request(None, True)
    assert_validation_error(response, "org_id", "not_found")


def test_missing_value(org_id):
    response = do_request(org_id, None)
    assert_validation_error(response, "value", "not_found")


def test_wrong_type_value(org_id):
    response = do_request(org_id, "not_a_bool")
    assert_validation_error(response, "value", "invalid_type")


def test_rejects_request_with_unregistered_tvm_source(
    tvm_clear_api_organization_allowed_services_list, org_id
):
    response = do_request(org_id, True)
    assert_forbidden_error(response, "forbidden_service")


def test_do_set_trusty(org_id):
    response = do_request(org_id, True)
    assert_status_code(response, status.HTTP_200_OK)
    assert _is_organization_trusty(org_id)


def test_do_set_untrusty(org_id):
    response = do_request(org_id, False)
    assert_status_code(response, status.HTTP_200_OK)
    assert not _is_organization_trusty(org_id)


def test_do_reset_trusty(trusty_org):
    assert _is_organization_trusty(trusty_org)
    response = do_request(trusty_org, False)
    assert_status_code(response, status.HTTP_200_OK)
    assert not _is_organization_trusty(trusty_org)


def do_request(org_id=None, value=None):
    if value is True:
        value = "true"
    elif value is False:
        value = "false"
    args = {
        "org_id": org_id,
        "value": value,
    }
    url = "/api/organization/trusty?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    global api_client
    return api_client.put(url)


def _is_organization_trusty(org_id):
    return OrganizationSettings.objects.get(org_id=org_id).trusty
