import pytest
from fan.accounts.organizations.domains import CheckDomainResult, check_domain_configured


pytestmark = pytest.mark.django_db


def test_passes_domain(mock_tvm, mock_gendarme, domain):
    check_domain_configured(domain)
    assert mock_gendarme.req_domain == domain


def test_passes_tvm_ticket(mock_tvm, mock_gendarme, domain):
    check_domain_configured(domain)
    assert mock_gendarme.req_tvm_ticket == "TEST_TVM_TICKET"


def test_domain_configured(mock_tvm, mock_gendarme, domain):
    res = check_domain_configured(domain)
    assert res == CheckDomainResult.OK


def test_no_mx(mock_tvm, mock_gendarme, domain):
    mock_gendarme.resp_mx = False
    res = check_domain_configured(domain)
    assert res == CheckDomainResult.NO_MX


def test_no_dkim(mock_tvm, mock_gendarme, domain):
    mock_gendarme.resp_dkim = False
    res = check_domain_configured(domain)
    assert res == CheckDomainResult.NO_DKIM


def test_no_spf(mock_tvm, mock_gendarme, domain):
    mock_gendarme.resp_spf = False
    res = check_domain_configured(domain)
    assert res == CheckDomainResult.NO_SPF
