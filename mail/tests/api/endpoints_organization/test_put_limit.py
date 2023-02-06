import urllib.request, urllib.parse, urllib.error
import pytest
from django.conf import settings
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


def test_missing_org_id(small_send_emails_limit):
    response = do_request(None, small_send_emails_limit)
    assert_validation_error(response, "org_id", "not_found")


def test_missing_value(org_id):
    response = do_request(org_id, None)
    assert_validation_error(response, "value", "not_found")


def test_wrong_type_value(org_id):
    response = do_request(org_id, "not_an_integer")
    assert_validation_error(response, "value", "invalid_type")


def test_negative_value(org_id):
    response = do_request(org_id, -1)
    assert_validation_error(response, "value", "invalid_value")


def test_rejects_request_with_unregistered_tvm_source(
    tvm_clear_api_organization_allowed_services_list, org_id, large_send_emails_limit
):
    response = do_request(org_id, large_send_emails_limit)
    assert_forbidden_error(response, "forbidden_service")


def test_do_set_send_emails_limit(org_id, large_send_emails_limit):
    response = do_request(org_id, large_send_emails_limit)
    assert_status_code(response, status.HTTP_200_OK)
    assert _organization_send_emails_limit(org_id) == large_send_emails_limit


def test_do_set_default_send_emails_limit_on_zero_value(org_id):
    response = do_request(org_id, 0)
    assert_status_code(response, status.HTTP_200_OK)
    assert _organization_send_emails_limit(org_id) == settings.DEFAULT_SEND_EMAILS_LIMIT


def test_do_changes_send_emails_limit(small_org, small_send_emails_limit, large_send_emails_limit):
    assert _organization_send_emails_limit(small_org) == small_send_emails_limit
    response = do_request(small_org, large_send_emails_limit)
    assert_status_code(response, status.HTTP_200_OK)
    assert _organization_send_emails_limit(small_org) == large_send_emails_limit


def do_request(org_id=None, value=None):
    args = {
        "org_id": org_id,
        "value": value,
    }
    url = "/api/organization/limit?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    global api_client
    return api_client.put(url)


def _organization_send_emails_limit(org_id):
    return OrganizationSettings.objects.get(org_id=org_id).send_emails_limit
