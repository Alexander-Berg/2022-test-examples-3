from django.conf import settings
from fan.accounts.organizations.domains import CheckDomainResult
from fan.accounts.organizations.limits import is_test_send_limit_reached
from fan.models import Campaign
from fan.utils.emails import is_valid
from fan.send.test_send import schedule, ScheduleError
from fan_ui.api.exceptions import (
    ValidationError,
    WrongDomainError,
    NotReadyError,
    WrongStateError,
    TestSendLimitReached,
    WrongLoginError,
    InvalidEmailsError,
)
from fan_ui.api.query_params import pass_user_id_param, pass_account_object, pass_campaign_object
from fan_ui.api.serializers.test_send_task import TestSendTaskSerializerV1
from .common import *


class TestSendTaskEndpoint(Endpoint):
    permission_classes = (UserPermission, IsAuthenticated, ApiV1TvmServicePermission)

    @method_decorator(pass_user_id_param)
    @method_decorator(pass_account_object)
    @method_decorator(pass_campaign_object)
    def post(self, request, user_id, account, campaign):
        if is_test_send_limit_reached(account.org_id):
            raise TestSendLimitReached()
        recipients = self._get_recipients_param(request)
        self._validate_recipients(recipients)
        user_template_variables = self._get_user_template_variables_param(request)
        self._validate_user_template_variables(user_template_variables)
        err, task = schedule(campaign, recipients, user_template_variables=user_template_variables)
        if err == ScheduleError.FORBIDDEN_CURRENT_CAMPAIGN_STATE:
            raise WrongStateError(campaign.state, Campaign.STATUS_DRAFT)
        if err == ScheduleError.NO_LETTER:
            raise NotReadyError("no_letter")
        if err == ScheduleError.EMPTY_FROM_EMAIL:
            raise NotReadyError("empty_from_email")
        if err == ScheduleError.EMPTY_FROM_NAME:
            raise NotReadyError("empty_from_name")
        if err == ScheduleError.EMPTY_SUBJECT:
            raise NotReadyError("empty_subject")
        if err == CheckDomainResult.NOT_BELONGS:
            raise WrongDomainError("not_belongs")
        if err == CheckDomainResult.NO_MX:
            raise WrongDomainError("no_mx")
        if err == CheckDomainResult.NO_DKIM:
            raise WrongDomainError("no_dkim")
        if err == CheckDomainResult.NO_SPF:
            raise WrongDomainError("no_spf")
        if err == ScheduleError.LOGIN_NOT_BELONGS:
            raise WrongLoginError("not_belongs")
        if err == ScheduleError.DUPLICATED_USER_TEMPLATE_VARIABLES:
            raise ValidationError({"user_template_variables": "duplicated_user_template_variables"})
        if err == ScheduleError.UNKNOWN_USER_TEMPLATE_VARIABLES:
            raise ValidationError({"user_template_variables": "unknown_user_template_variables"})
        if err == ScheduleError.USER_TEMPLATE_VARIABLE_VALUE_TOO_LONG:
            raise ValidationError({"user_template_variables": "too_long"})
        return make_ok_response(task, TestSendTaskSerializerV1)

    def _get_recipients_param(self, request):
        if "recipients" not in request.data:
            raise ValidationError({"recipients": "not_found"})
        recipients = request.data["recipients"]
        if not isinstance(recipients, list):
            raise ValidationError({"recipients": "invalid_type"})
        return recipients

    def _validate_recipients(self, recipients):
        if len(recipients) == 0:
            raise ValidationError({"recipients": "empty"})
        if len(recipients) > settings.TEST_SEND_MAX_RECIPIENTS:
            raise ValidationError({"recipients": "too_long"})
        emails_with_errors = [recipient for recipient in recipients if not is_valid(recipient)]
        if emails_with_errors:
            raise InvalidEmailsError(emails_with_errors)

    def _get_user_template_variables_param(self, request):
        if "user_template_variables" not in request.data:
            return {}
        return request.data["user_template_variables"]

    def _validate_user_template_variables(seld, user_template_variables):
        if not isinstance(user_template_variables, dict):
            raise ValidationError({"user_template_variables": "invalid_type"})
        for value in list(user_template_variables.values()):
            if not isinstance(value, str) and not isinstance(value, str):
                raise ValidationError({"user_template_variables": "invalid_type"})
