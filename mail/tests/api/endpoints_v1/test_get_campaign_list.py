from django.conf import settings
from rest_framework import status

import pytest
from fan.campaigns.create import create_campaign
from fan.models import Campaign
from fan.testutils.matchers import (
    assert_forbidden_error,
    assert_lists_are_matching,
    assert_not_authenticated_error,
    assert_status_code,
    assert_validation_error,
)

pytestmark = pytest.mark.django_db


@pytest.fixture
def campaign_list(project, account):
    return [
        create_campaign(account=account, project=project, state=Campaign.STATUS_EMBRYON),
        create_campaign(account=account, project=project, state=Campaign.STATUS_DRAFT),
        create_campaign(account=account, project=project, state=Campaign.STATUS_DONE),
    ]


@pytest.fixture
def force_campaigns_list_limit():
    limit = settings.CAMPAIGNS_LIST_LIMIT
    settings.CAMPAIGNS_LIST_LIMIT = 1
    yield
    settings.CAMPAIGNS_LIST_LIMIT = limit


def assert_campaigns_are_sorted_by_creation(response_data):
    original_list = [c["created_at"] for c in response_data]
    sorted_list = sorted(original_list, reverse=True)
    assert set(original_list) == set(sorted_list)


class TestCampaignList:
    def do_request(self, client, user_id, account, campaign_list):
        url = "/api/v1/campaign-list?user_id={user_id}&account_slug={account_slug}".format(
            user_id=user_id, account_slug=account.name
        )
        return client.get(url)

    def do_test_campaign_list(self, client, user_id, account, campaign_list):
        response = self.do_request(client, user_id, account, campaign_list)
        assert_status_code(response, status.HTTP_200_OK)
        response_data = response.json()

        expected_campaigns = [
            dict(
                from_email=c.from_email,
                from_name=c.from_name,
                slug=c.slug,
                state=c.state,
                subject=c.subject,
            )
            for c in campaign_list
        ]
        assert_lists_are_matching(response_data, expected_campaigns)
        assert_campaigns_are_sorted_by_creation(response_data)

    def test_campaign_list_session_auth(self, auth_api_client, user_id, account, campaign_list):
        self.do_test_campaign_list(auth_api_client, user_id, account, campaign_list)

    def test_campaign_list(self, tvm_api_client, user_id, account, campaign_list):
        self.do_test_campaign_list(tvm_api_client, user_id, account, campaign_list)

    def test_campaign_list_wo_auth_localhost(
        self, api_client, user_id, account, campaign_list, disable_auth_on_loopback
    ):
        self.do_test_campaign_list(api_client, user_id, account, campaign_list)

    def test_campaign_list_wo_auth(self, api_client, user_id, account, campaign_list):
        response = self.do_request(api_client, user_id, account, campaign_list)
        assert_not_authenticated_error(response, "Authentication credentials were not provided.")

    def test_campaign_list_with_damaged_tvm_credentials(
        self, tvm_api_client_with_damaged_ticket, user_id, account, campaign_list
    ):
        response = self.do_request(
            tvm_api_client_with_damaged_ticket, user_id, account, campaign_list
        )
        assert_not_authenticated_error(response, "Authentication credentials were not provided.")

    def test_campaign_list_with_non_registered_tvm_source(
        self,
        tvm_api_client,
        user_id,
        account,
        campaign_list,
        tvm_clear_api_v1_allowed_services_list,
    ):
        response = self.do_request(tvm_api_client, user_id, account, campaign_list)
        assert_forbidden_error(response, "forbidden_service")

    def test_campaign_list_missing_account_slug(self, tvm_api_client, user_id):
        url = "/api/v1/campaign-list?user_id={user_id}".format(
            user_id=user_id,
        )
        response = tvm_api_client.get(url)
        assert_validation_error(response, "account_slug", "not_found")

    def test_campaign_list_non_existent_account_slug(self, tvm_api_client, user_id):
        url = "/api/v1/campaign-list?user_id={user_id}&account_slug={account_slug}".format(
            user_id=user_id, account_slug="NON EXISTENT SLUG"
        )
        response = tvm_api_client.get(url)
        assert_status_code(response, status.HTTP_404_NOT_FOUND)

    def test_campaign_list_filter_by_state(self, tvm_api_client, user_id, account, campaign_list):
        filtered_states = (Campaign.STATUS_EMBRYON, Campaign.STATUS_DRAFT)
        url = "/api/v1/campaign-list?user_id={user_id}&account_slug={account_slug}&states={states}".format(
            user_id=user_id,
            account_slug=account.name,
            states=",".join(filtered_states),
        )
        response = tvm_api_client.get(url)
        assert_status_code(response, status.HTTP_200_OK)
        response_data = response.json()
        expected_campaigns = [
            dict(
                from_email=campaign.from_email,
                from_name=campaign.from_name,
                slug=campaign.slug,
                state=campaign.state,
                subject=campaign.subject,
            )
            for campaign in campaign_list
            if campaign.state in filtered_states
        ]
        assert_lists_are_matching(response_data, expected_campaigns)

    def test_campaign_list_invalid_state(self, tvm_api_client, user_id, account):
        url = "/api/v1/campaign-list?user_id={user_id}&account_slug={account_slug}&states={states}".format(
            user_id=user_id,
            account_slug=account.name,
            states="INVALID CAMPAIGN STATE",
        )
        response = tvm_api_client.get(url)
        assert_validation_error(response, "state", "not_supported")

    def test_forbidden_user(self, tvm_api_client, account, campaign_list):
        url = "/api/v1/campaign-list?user_id={user_id}&account_slug={account_slug}".format(
            user_id="forbidden_user_id", account_slug=account.name
        )
        response = tvm_api_client.get(url)
        assert_forbidden_error(response, "forbidden_user")

    def test_list_limit(
        self, auth_api_client, user_id, account, campaign_list, force_campaigns_list_limit
    ):
        url = "/api/v1/campaign-list?user_id={user_id}&account_slug={account_slug}".format(
            user_id=user_id,
            account_slug=account.name,
        )
        response = auth_api_client.get(url)
        response_data = response.json()
        assert_status_code(response, status.HTTP_200_OK)
        assert len(response_data) == 1
