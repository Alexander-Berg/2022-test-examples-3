from django.conf import settings
from fan.accounts.get import get_account_from_logins, need_to_silently_drop_sents
from fan.accounts.organizations.domains import CheckDomainResult, check_campaign_domain
from fan.exceptions import PermanentError
from fan.message.get import get_message_by_letter
from fan.utils.emails import get_local_part
from fan.models.campaign import Campaign
from fan.models.test_send_task import TestSendTask
from fan.utils.persistent_log import persistent_log


class ScheduleError:
    FORBIDDEN_CURRENT_CAMPAIGN_STATE = "forbidden_current_campaign_state"
    NO_LETTER = "no_letter"
    EMPTY_FROM_EMAIL = "empty_from_email"
    EMPTY_FROM_NAME = "empty_from_name"
    EMPTY_SUBJECT = "empty_subject"
    LOGIN_NOT_BELONGS = "login_not_belongs"
    DUPLICATED_USER_TEMPLATE_VARIABLES = "duplicated_user_template_variables"
    UNKNOWN_USER_TEMPLATE_VARIABLES = "unknown_user_template_variables"
    USER_TEMPLATE_VARIABLE_VALUE_TOO_LONG = "user_template_variable_value_too_long"


def schedule(campaign, recipients, user_template_variables={}):
    if len(recipients) == 0:
        raise PermanentError("test send task must have recipients")
    if len(recipients) > settings.TEST_SEND_MAX_RECIPIENTS:
        raise PermanentError("too many recipients for test send")
    user_template_variables, err = _transform_user_template_variables_to_lower(
        user_template_variables
    )
    if err:
        return err, None
    err = (
        _check_allowed_campaign_state_param(campaign)
        or _check_campaign_letter(campaign)
        or _check_user_template_variables(user_template_variables, campaign)
    )
    if err:
        return err, None
    if settings.CHECK_CAMPAIGN_FROM_LOGIN:
        err = _check_campaign_from_login(campaign)
        if err:
            return err, None
    res = check_campaign_domain(campaign)
    if res != CheckDomainResult.OK:
        return res, None
    task = TestSendTask(
        campaign=campaign,
        recipients=recipients,
        user_template_variables=user_template_variables,
    )
    if not need_to_silently_drop_sents(campaign.account):
        task.save()

    persistent_log(
        object_type="campaign",
        object_id=campaign.id,
        component="test_send_task",
        action="schedule",
        description="Запланирована тестовая отправка",
    )

    return None, task


def _check_allowed_campaign_state_param(campaign):
    if campaign.state != Campaign.STATUS_DRAFT:
        return ScheduleError.FORBIDDEN_CURRENT_CAMPAIGN_STATE
    return None


def _check_campaign_letter(campaign):
    if not campaign.letter_uploaded:
        return ScheduleError.NO_LETTER
    if not campaign.from_email:
        return ScheduleError.EMPTY_FROM_EMAIL
    if not campaign.from_name:
        return ScheduleError.EMPTY_FROM_NAME
    if not campaign.subject:
        return ScheduleError.EMPTY_SUBJECT
    return None


def _check_campaign_from_login(campaign):
    from_logins = get_account_from_logins(campaign.account)
    login = get_local_part(campaign.from_email)
    if login not in from_logins:
        return ScheduleError.LOGIN_NOT_BELONGS
    return None


def _transform_user_template_variables_to_lower(user_template_variables):
    res = {}
    for key, value in list(user_template_variables.items()):
        res[key.lower()] = value
    if len(list(res.keys())) != len(list(user_template_variables.keys())):
        return None, ScheduleError.DUPLICATED_USER_TEMPLATE_VARIABLES
    return res, None


def _check_user_template_variables(user_template_variables, campaign):
    message = get_message_by_letter(campaign.default_letter)
    message_user_template_variables = set(message.user_template_variables)
    for variable in list(user_template_variables.keys()):
        if not variable in message_user_template_variables:
            return ScheduleError.UNKNOWN_USER_TEMPLATE_VARIABLES
    for value in list(user_template_variables.values()):
        if len(value) > settings.USER_TEMPLATE_VARIABLE_VALUE_MAX_LENGTH:
            return ScheduleError.USER_TEMPLATE_VARIABLE_VALUE_TOO_LONG
    return None


def get_tasks(count):
    if count <= 0:
        raise RuntimeError("count must be greater than 0")
    return TestSendTask.objects.order_by("created_at")[:count]


def get_by_id(task_id):
    return TestSendTask.objects.get(id=task_id)


def complete(task):
    task.delete()
