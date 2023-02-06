import urllib.request, urllib.parse, urllib.error
import pytest
from rest_framework import status
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_status_code,
    assert_validation_error,
)


pytestmark = pytest.mark.django_db


def test_missing_user_id(tvm_api_client, org_id):
    response = do_request(tvm_api_client, None, org_id)
    assert_validation_error(response, "user_id", "not_found")


def test_missing_org_id(tvm_api_client, user_id):
    response = do_request(tvm_api_client, user_id, None)
    assert_validation_error(response, "org_id", "not_found")


def test_user_is_not_org_admin_or_org_account_user(
    tvm_api_client, mock_tvm, user_id, org_id, mock_directory_user
):
    response = do_request(tvm_api_client, user_id, org_id)
    assert_forbidden_error(response, "forbidden_user")


def test_user_has_role_in_org_account(
    tvm_api_client, mock_tvm, mock_directory_domains, user_id, account
):
    response = do_request(tvm_api_client, user_id, account.org_id)
    assert_status_code(response, status.HTTP_200_OK)


def test_user_is_admin_in_org(
    tvm_api_client, mock_tvm, mock_directory_domains, user_id, org_id, user_admin_in_directory
):
    response = do_request(tvm_api_client, user_id, org_id)
    assert_status_code(response, status.HTTP_200_OK)


def test_organization_without_domains(
    tvm_api_client, mock_tvm, mock_directory_domains, user_id, org_id, user_admin_in_directory
):
    mock_directory_domains.resp_owned = False
    response = do_request(tvm_api_client, user_id, org_id)
    assert_status_code(response, status.HTTP_200_OK)
    assert response.json() == []


def test_organization_with_configured_domain(
    tvm_api_client,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    user_id,
    org_id,
    domain,
    user_admin_in_directory,
):
    response = do_request(tvm_api_client, user_id, org_id)
    assert_status_code(response, status.HTTP_200_OK)
    assert response.json() == [
        {
            "domain": domain,
            "master": True,
            "mx": True,
            "dkim": True,
            "spf": True,
        }
    ]


def test_organization_with_no_spf_domain(
    tvm_api_client,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    user_id,
    org_id,
    domain,
    user_admin_in_directory,
):
    mock_gendarme.resp_spf = False
    response = do_request(tvm_api_client, user_id, org_id)
    assert_status_code(response, status.HTTP_200_OK)
    assert response.json() == [
        {
            "domain": domain,
            "master": True,
            "mx": True,
            "dkim": True,
            "spf": False,
        }
    ]


def test_gendarme_unavailable(
    tvm_api_client,
    mock_tvm,
    mock_directory_domains,
    mock_gendarme,
    user_id,
    org_id,
    domain,
    user_admin_in_directory,
):
    mock_gendarme.resp_code = 503
    response = do_request(tvm_api_client, user_id, org_id)
    assert_status_code(response, status.HTTP_200_OK)
    assert response.json() == [
        {
            "domain": domain,
            "master": True,
            "mx": None,
            "dkim": None,
            "spf": None,
        }
    ]
    assert b"null" in response.content


def do_request(client, user_id=None, org_id=None):
    args = {"user_id": user_id, "org_id": org_id}
    url = "/api/v1/org-domain-list?" + urllib.parse.urlencode(
        {k: v for k, v in list(args.items()) if v is not None}
    )
    return client.get(url)
