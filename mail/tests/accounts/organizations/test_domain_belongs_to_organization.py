import pytest
from fan.accounts.organizations.domains import CheckDomainResult, domain_belongs_to_organization


pytestmark = pytest.mark.django_db


def test_passes_org_id(mock_tvm, mock_directory_domains, org_id, domain):
    domain_belongs_to_organization(domain, org_id)
    assert mock_directory_domains.req_org_id == org_id


def test_passes_tvm_ticket(mock_tvm, mock_directory_domains, org_id, domain):
    domain_belongs_to_organization(domain, org_id)
    assert mock_directory_domains.req_tvm_ticket == "TEST_TVM_TICKET"


def test_domain_belongs_to_organization(mock_tvm, mock_directory_domains, org_id, domain):
    res = domain_belongs_to_organization(domain, org_id)
    assert res == CheckDomainResult.OK


def test_domain_does_not_belong_to_organization(mock_tvm, mock_directory_domains, org_id):
    res = domain_belongs_to_organization("foreign.com", org_id)
    assert res == CheckDomainResult.NOT_BELONGS


def test_domain_not_owned(mock_tvm, mock_directory_domains, org_id, domain):
    mock_directory_domains.resp_owned = False
    res = domain_belongs_to_organization(domain, org_id)
    assert res == CheckDomainResult.NOT_BELONGS
