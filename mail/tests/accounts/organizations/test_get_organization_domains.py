import pytest
from fan.accounts.organizations.domains import get_organization_domains


pytestmark = pytest.mark.django_db


def test_passes_org_id_in_directory_request(mock_tvm, mock_directory_domains, org_id):
    get_organization_domains(org_id)
    assert mock_directory_domains.req_org_id == org_id


def test_passes_tvm_ticket_in_directory_request(mock_tvm, mock_directory_domains, org_id):
    get_organization_domains(org_id)
    assert mock_directory_domains.req_tvm_ticket == "TEST_TVM_TICKET"


def test_passes_domain_in_gendarme_request(
    mock_tvm, mock_directory_domains, mock_gendarme, org_id, domain
):
    get_organization_domains(org_id)
    assert mock_gendarme.req_domain == domain


def test_passes_tvm_ticket_in_gendarme_request(
    mock_tvm, mock_directory_domains, mock_gendarme, org_id
):
    get_organization_domains(org_id)
    assert mock_gendarme.req_tvm_ticket == "TEST_TVM_TICKET"


def test_organization_without_domains(mock_tvm, mock_directory_domains, org_id):
    mock_directory_domains.resp_owned = False
    assert get_organization_domains(org_id) == []


def test_organization_with_not_master_domain(
    mock_tvm, mock_directory_domains, mock_gendarme, org_id, domain
):
    mock_directory_domains.resp_master = False
    assert get_organization_domains(org_id) == [
        {
            "domain": domain,
            "master": False,
            "mx": True,
            "dkim": True,
            "spf": True,
        }
    ]


def test_organization_with_configured_domain(
    mock_tvm, mock_directory_domains, mock_gendarme, org_id, domain
):
    assert get_organization_domains(org_id) == [
        {
            "domain": domain,
            "master": True,
            "mx": True,
            "dkim": True,
            "spf": True,
        }
    ]


def test_organization_with_no_mx_domain(
    mock_tvm, mock_directory_domains, mock_gendarme, org_id, domain
):
    mock_gendarme.resp_mx = False
    assert get_organization_domains(org_id) == [
        {
            "domain": domain,
            "master": True,
            "mx": False,
            "dkim": True,
            "spf": True,
        }
    ]


def test_organization_with_no_dkim_domain(
    mock_tvm, mock_directory_domains, mock_gendarme, org_id, domain
):
    mock_gendarme.resp_dkim = False
    assert get_organization_domains(org_id) == [
        {
            "domain": domain,
            "master": True,
            "mx": True,
            "dkim": False,
            "spf": True,
        }
    ]


def test_organization_with_no_spf_domain(
    mock_tvm, mock_directory_domains, mock_gendarme, org_id, domain
):
    mock_gendarme.resp_spf = False
    assert get_organization_domains(org_id) == [
        {
            "domain": domain,
            "master": True,
            "mx": True,
            "dkim": True,
            "spf": False,
        }
    ]


def test_gendarme_unavailable(mock_tvm, mock_directory_domains, mock_gendarme, org_id, domain):
    mock_gendarme.resp_code = 503
    assert get_organization_domains(org_id) == [
        {
            "domain": domain,
            "master": True,
            "mx": None,
            "dkim": None,
            "spf": None,
        }
    ]
