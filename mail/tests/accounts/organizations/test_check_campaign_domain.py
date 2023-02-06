import pytest
from fan.accounts.organizations.domains import CheckDomainResult, check_campaign_domain


pytestmark = pytest.mark.django_db


def test_domain_ok(mock_tvm, mock_directory_domains, mock_gendarme, campaign_with_letter):
    res = check_campaign_domain(campaign_with_letter)
    assert res == CheckDomainResult.OK


def test_not_belongs(mock_tvm, mock_directory_domains, campaign_with_letter):
    mock_directory_domains.resp_owned = False
    res = check_campaign_domain(campaign_with_letter)
    assert res == CheckDomainResult.NOT_BELONGS


def test_no_mx(mock_tvm, mock_directory_domains, mock_gendarme, campaign_with_letter):
    mock_gendarme.resp_mx = False
    res = check_campaign_domain(campaign_with_letter)
    assert res == CheckDomainResult.NO_MX
