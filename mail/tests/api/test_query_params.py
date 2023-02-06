import django.http
import rest_framework.request

import pytest

from fan_ui.api.endpoints_v1.common import *
from fan_ui.api.exceptions import ValidationError, ResourceDoesNotExist
from fan_ui.api.query_params import *

pytestmark = pytest.mark.django_db


def create_request_mock(query_params):
    request = django.http.HttpRequest()
    request.method = "GET"
    request.META["HTTP_ORIGIN"] = "http://example.com"
    request.GET.update(query_params)
    return rest_framework.request.Request(request)


def check_passes_param(method, param_name, param_value="SOME_FAKE_PARAM_VALUE"):
    request = create_request_mock({param_name: param_value})
    assert method(request) == param_value


def check_raises_on_invalid_param(method, param_name, param_value, exception=ValidationError):
    request = create_request_mock({param_name: param_value})
    with pytest.raises(exception):
        method(request)


PARAMS_META = [
    ("user_id", pass_user_id_param),
    ("account_slug", pass_account_slug_param),
    ("campaign_slug", pass_campaign_slug_param),
    ("org_id", pass_org_id_param),
]


@pytest.mark.parametrize("param_name, decorator", PARAMS_META)
class TestPassParam:
    def test_valid_param(self, param_name, decorator):
        @decorator
        def _decorated(request, **kwargs):
            return kwargs[param_name]

        check_passes_param(_decorated, param_name)

    def test_empty_param(self, param_name, decorator):
        @decorator
        def _decorated(request, **kwargs):
            return kwargs[param_name]

        check_raises_on_invalid_param(_decorated, param_name, "")

    def test_empty_param_allow_blank(self, param_name, decorator):
        @decorator(blank=True)
        def _decorated(request, **kwargs):
            return kwargs[param_name]

        check_passes_param(_decorated, param_name, "")

    def test_no_param(self, param_name, decorator):
        @decorator
        def _decorated(request, **kwargs):
            return kwargs[param_name]

        check_raises_on_invalid_param(_decorated, param_name, None)

    def test_no_param_nonrequired(self, param_name, decorator):
        @decorator(required=False)
        def _decorated(request, **kwargs):
            return kwargs[param_name]

        check_passes_param(_decorated, param_name, None)


class TestPassAccountObject:
    @method_decorator(pass_account_object)
    def _decorated(self, request, account):
        return account

    @method_decorator(pass_account_object(required=False))
    def _decorated_as_nonrequired(self, request, account):
        return account

    def test_pass_valid_account(self, account):
        request = create_request_mock({"account_slug": account.name})
        assert self._decorated(request).id == account.id

    def test_pass_empty_account_slug(self):
        request = create_request_mock({"account_slug": ""})
        with pytest.raises(ValidationError):
            self._decorated(request)

    def test_pass_no_account_slug(self):
        request = create_request_mock({"account_slug": None})
        with pytest.raises(ValidationError):
            self._decorated(request)

    def test_pass_nonrequired_account_slug(self, account):
        request = create_request_mock({"account_slug": account.name})
        assert self._decorated_as_nonrequired(request) == account

    def test_pass_nonrequired_nonexistent_account_slug_raises_exception(self):
        request = create_request_mock({"account_slug": "non-existent-account"})
        with pytest.raises(ResourceDoesNotExist):
            assert self._decorated_as_nonrequired(request)

    def test_not_passing_nonrequired_account_slug_not_raises_exception(self):
        request = create_request_mock({})
        assert self._decorated_as_nonrequired(request) == None


class TestPassCampaignObject:
    @method_decorator(pass_account_object)
    @method_decorator(pass_campaign_object)
    def _decorated(self, request, account, campaign):
        return (account, campaign)

    @method_decorator(pass_account_object)
    @method_decorator(pass_campaign_object(required=False))
    def _decorated_as_nonrequired(self, request, account, campaign):
        return (account, campaign)

    def _create_request_mock(self, account_slug, campaign_slug):
        return create_request_mock(
            {
                "account_slug": account_slug,
                "campaign_slug": campaign_slug,
            }
        )

    def test_pass_valid_campaign(self, account, campaign):
        request = self._create_request_mock(account.name, campaign.slug)
        (acc, camp) = self._decorated(request)
        assert acc.id == account.id
        assert camp.id == campaign.id

    def test_pass_empty_account_slug(self, campaign):
        request = self._create_request_mock("", campaign.slug)
        with pytest.raises(ValidationError):
            self._decorated(request)

    def test_pass_no_account_slug(self, campaign):
        request = self._create_request_mock(None, campaign.slug)
        with pytest.raises(ValidationError):
            self._decorated(request)

    def test_pass_empty_campaign_slug(self, account):
        request = self._create_request_mock(account.name, "")
        with pytest.raises(ValidationError):
            self._decorated(request)

    def test_pass_no_campaign_slug(self, account):
        request = self._create_request_mock(account.name, None)
        with pytest.raises(ValidationError):
            self._decorated(request)

    def test_missing_account_decorator(self, account, campaign):
        @pass_campaign_object
        def _decorated(request, campaign):
            return campaign

        request = self._create_request_mock(account.name, campaign.slug)
        with pytest.raises(TypeError):
            _decorated(request)

    def test_incorrect_decoration_order(self, account, campaign):
        @pass_campaign_object
        @pass_account_object
        def _decorated(request, account, campaign):
            return (account, campaign)

        request = self._create_request_mock(account.name, campaign.slug)
        with pytest.raises(TypeError):
            _decorated(request)

    def test_custom_url_param_and_handler_arg_names(self, account, campaign):
        @pass_account_object
        @pass_campaign_object(url_param="source_campaign_slug", handler_arg="source_campaign")
        def _decorated(request, account, source_campaign):
            return (account, source_campaign)

        request = create_request_mock(
            {
                "account_slug": account.name,
                "source_campaign_slug": campaign.slug,
            }
        )
        assert _decorated(request) == (account, campaign)

    def test_pass_nonrequired_campaign_slug(self, account, campaign):
        request = create_request_mock(
            {
                "account_slug": account.name,
                "campaign_slug": campaign.slug,
            }
        )
        assert self._decorated_as_nonrequired(request) == (account, campaign)

    def test_pass_nonrequired_nonexistent_campaign_slug_raises_exception(self, account):
        request = create_request_mock(
            {
                "account_slug": account.name,
                "campaign_slug": "non-existent-campaign",
            }
        )
        with pytest.raises(ResourceDoesNotExist):
            assert self._decorated_as_nonrequired(request)

    def test_not_passing_nonrequired_campaign_slug_not_raises_exception(self, account):
        request = create_request_mock({"account_slug": account.name})
        assert self._decorated_as_nonrequired(request) == (account, None)


class TestPassSourceCampaignObject:
    def test_pass_source_campaign_object(self, account, campaign):
        @pass_account_object
        @pass_source_campaign_object
        def _decorated(request, account, source_campaign):
            return (account, source_campaign)

        request = create_request_mock(
            {
                "account_slug": account.name,
                "source_campaign_slug": campaign.slug,
            }
        )
        assert _decorated(request) == (account, campaign)


class TestPassMaillistObject:
    @method_decorator(pass_account_object)
    @method_decorator(pass_maillist_object)
    def _decorated(self, request, account, maillist):
        return (account, maillist)

    @method_decorator(pass_account_object)
    @method_decorator(pass_maillist_object(required=False))
    def _decorated_as_nonrequired(self, request, account, maillist):
        return (account, maillist)

    def _create_request_mock(self, account_slug, maillist_slug):
        return create_request_mock(
            {
                "account_slug": account_slug,
                "maillist_slug": maillist_slug,
            }
        )

    def test_pass_valid_maillist(self, account, maillist):
        request = self._create_request_mock(account.name, maillist.slug)
        (res_account, res_maillist) = self._decorated(request)
        assert res_account.id == account.id
        assert res_maillist.id == maillist.id

    def test_pass_empty_account_slug(self, maillist):
        request = self._create_request_mock("", maillist.slug)
        with pytest.raises(ValidationError):
            self._decorated(request)

    def test_pass_no_account_slug(self, maillist):
        request = self._create_request_mock(None, maillist.slug)
        with pytest.raises(ValidationError):
            self._decorated(request)

    def test_pass_empty_maillist_slug(self, account):
        request = self._create_request_mock(account.name, "")
        with pytest.raises(ValidationError):
            self._decorated(request)

    def test_pass_no_maillist_slug(self, account):
        request = self._create_request_mock(account.name, None)
        with pytest.raises(ValidationError):
            self._decorated(request)

    def test_missing_account_decorator(self, account, maillist):
        @pass_maillist_object
        def _decorated(request, maillist):
            return maillist

        request = self._create_request_mock(account.name, maillist.slug)
        with pytest.raises(TypeError):
            _decorated(request)

    def test_incorrect_decoration_order(self, account, maillist):
        @pass_maillist_object
        @pass_account_object
        def _decorated(request, account, maillist):
            return (account, maillist)

        request = self._create_request_mock(account.name, maillist.slug)
        with pytest.raises(TypeError):
            _decorated(request)

    def test_custom_url_param_and_handler_arg_names(self, account, maillist):
        @pass_account_object
        @pass_maillist_object(url_param="source_maillist_slug", handler_arg="source_maillist")
        def _decorated(request, account, source_maillist):
            return (account, source_maillist)

        request = create_request_mock(
            {
                "account_slug": account.name,
                "source_maillist_slug": maillist.slug,
            }
        )
        assert _decorated(request) == (account, maillist)

    def test_pass_nonrequired_maillist_slug(self, account, maillist):
        request = create_request_mock(
            {
                "account_slug": account.name,
                "maillist_slug": maillist.slug,
            }
        )
        assert self._decorated_as_nonrequired(request) == (account, maillist)

    def test_pass_nonrequired_nonexistent_maillist_slug_raises_exception(self, account):
        request = create_request_mock(
            {
                "account_slug": account.name,
                "maillist_slug": "non-existent-maillist",
            }
        )
        with pytest.raises(ResourceDoesNotExist):
            assert self._decorated_as_nonrequired(request)

    def test_not_passing_nonrequired_maillist_slug_not_raises_exception(self, account):
        request = create_request_mock({"account_slug": account.name})
        assert self._decorated_as_nonrequired(request) == (account, None)


class TestPassUserIDAccountCampaign:
    @method_decorator(pass_user_id_param)
    @method_decorator(pass_account_object)
    @method_decorator(pass_campaign_object)
    def _decorated(self, request, user_id, account, campaign):
        return (user_id, account, campaign)

    def _create_request_mock(self, user_id, account_slug, campaign_slug):
        return create_request_mock(
            {
                "user_id": user_id,
                "account_slug": account_slug,
                "campaign_slug": campaign_slug,
            }
        )

    def test_pass_valid(self, user_id, account, campaign):
        request = self._create_request_mock(user_id, account.name, campaign.slug)
        (uid, acc, camp) = self._decorated(request)
        assert uid == user_id
        assert acc.id == account.id
        assert camp.id == campaign.id

    def test_reorder_args_valid(self, user_id, account, campaign):
        @pass_user_id_param
        @pass_account_object
        @pass_campaign_object
        def _decorated(request, campaign, account, user_id):
            return (user_id, account, campaign)

        request = self._create_request_mock(user_id, account.name, campaign.slug)
        (uid, acc, camp) = _decorated(request)
        assert uid == user_id
        assert acc.id == account.id
        assert camp.id == campaign.id


class TestPassTestSendTaskObject:
    @method_decorator(pass_test_send_task_object(required=False))
    def _decorated_as_nonrequired(self, request, task):
        return task

    def test_pass_nonrequired_task(self, test_send_tasks):
        request = create_request_mock({"task_id": str(test_send_tasks[0].id)})
        assert self._decorated_as_nonrequired(request) == test_send_tasks[0]

    def test_pass_nonrequired_nonexistent_task_raises_exception(self):
        request = create_request_mock({"task_id": "2147483647"})
        with pytest.raises(ResourceDoesNotExist):
            assert self._decorated_as_nonrequired(request)

    def test_not_passing_nonrequired_task_not_raises_exception(self):
        request = create_request_mock({})
        assert self._decorated_as_nonrequired(request) == None
