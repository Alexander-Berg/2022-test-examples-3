import pytest
from django.conf import settings
from fan.accounts.organizations.domains import CheckDomainResult
from fan.exceptions import PermanentError
from fan.models.test_send_task import TestSendTask as Task
from fan.send.test_send import schedule, get_tasks, get_by_id, complete, ScheduleError


pytestmark = pytest.mark.django_db


class TestSchedule:
    def test_without_recipients(self, campaign_with_letter):
        with pytest.raises(PermanentError):
            schedule(campaign_with_letter, [])

    def test_with_recipients(
        self, mock_tvm, mock_directory_domains, mock_gendarme, campaign_with_letter, recipients
    ):
        schedule(campaign_with_letter, recipients)
        tasks = Task.objects.all()
        assert len(tasks) == 1
        assert tasks[0].campaign == campaign_with_letter
        assert tasks[0].recipients == recipients

    def test_with_correct_user_template_variables(
        self,
        mock_tvm,
        mock_directory_domains,
        mock_gendarme,
        campaign_with_letter,
        recipients,
        correct_user_template_variables,
    ):
        schedule(
            campaign_with_letter,
            recipients,
            user_template_variables=correct_user_template_variables,
        )
        tasks = Task.objects.all()
        assert len(tasks) == 1
        assert tasks[0].user_template_variables == correct_user_template_variables

    def test_with_correct_user_template_variables_with_capital_letters(
        self,
        mock_tvm,
        mock_directory_domains,
        mock_gendarme,
        campaign_with_letter,
        recipients,
        correct_user_template_variables_with_capital_letters,
        correct_user_template_variables,
    ):
        schedule(
            campaign_with_letter,
            recipients,
            user_template_variables=correct_user_template_variables_with_capital_letters,
        )
        tasks = Task.objects.all()
        assert len(tasks) == 1
        assert tasks[0].user_template_variables == correct_user_template_variables

    def test_with_duplicated_user_template_variables_in_different_cases(
        self,
        mock_tvm,
        mock_directory_domains,
        mock_gendarme,
        campaign_with_letter,
        recipients,
        duplicated_user_template_variables_in_different_cases,
    ):
        err, task = schedule(
            campaign_with_letter,
            recipients,
            user_template_variables=duplicated_user_template_variables_in_different_cases,
        )
        assert err is ScheduleError.DUPLICATED_USER_TEMPLATE_VARIABLES
        assert task is None

    def test_with_unknown_user_template_variable(
        self,
        mock_tvm,
        mock_directory_domains,
        mock_gendarme,
        campaign_with_letter,
        recipients,
        unknown_user_template_variable,
    ):
        err, task = schedule(
            campaign_with_letter, recipients, user_template_variables=unknown_user_template_variable
        )
        assert err is ScheduleError.UNKNOWN_USER_TEMPLATE_VARIABLES
        assert task is None

    def test_with_too_long_user_template_variable_value(
        self,
        mock_tvm,
        mock_directory_domains,
        mock_gendarme,
        campaign_with_letter,
        recipients,
        too_long_user_template_variable_value,
    ):
        err, task = schedule(
            campaign_with_letter,
            recipients,
            user_template_variables=too_long_user_template_variable_value,
        )
        assert err is ScheduleError.USER_TEMPLATE_VARIABLE_VALUE_TOO_LONG
        assert task is None

    def test_domain_not_belongs(
        self, mock_tvm, mock_directory_domains, campaign_with_letter, recipients
    ):
        mock_directory_domains.resp_owned = False
        err, task = schedule(campaign_with_letter, recipients)
        assert err is CheckDomainResult.NOT_BELONGS
        assert task is None

    def test_from_login_check_on(
        self,
        mock_tvm,
        mock_directory_domains,
        permit_setting_check_campaign_from_login,
        campaign_with_wrong_from_login,
        recipients,
    ):
        err, task = schedule(campaign_with_wrong_from_login, recipients)
        assert err is ScheduleError.LOGIN_NOT_BELONGS
        assert task is None

    def test_from_login_check_off(
        self,
        mock_tvm,
        mock_directory_domains,
        mock_gendarme,
        prohibit_setting_check_campaign_from_login,
        campaign_with_wrong_from_login,
        recipients,
    ):
        schedule(campaign_with_wrong_from_login, recipients)
        tasks = Task.objects.all()
        assert len(tasks) == 1
        assert tasks[0].campaign == campaign_with_wrong_from_login
        assert tasks[0].recipients == recipients

    def test_with_new_untrusty_account(
        self,
        mock_tvm,
        mock_directory_domains,
        mock_gendarme,
        ready_campaign_from_new_untrusty_account,
        recipients,
        enable_silently_drop_sents_for_new_untrusty_accounts,
    ):
        err, task = schedule(ready_campaign_from_new_untrusty_account, recipients)
        assert err is None
        assert task is not None
        assert len(Task.objects.all()) == 0

    def test_with_old_unknown_account(
        self,
        mock_tvm,
        mock_directory_domains,
        mock_gendarme,
        ready_campaign_from_old_unknown_account,
        recipients,
        enable_silently_drop_sents_for_new_untrusty_accounts,
    ):
        err, task = schedule(ready_campaign_from_old_unknown_account, recipients)
        assert err is None
        assert task is not None
        assert len(Task.objects.all()) == 1


class TestGetTasks:
    def test_count_is_zero(self):
        with pytest.raises(RuntimeError):
            get_tasks(0)

    def test_no_tasks(self):
        res = get_tasks(1)
        assert len(res) == 0

    def test_single_task(self, test_send_tasks):
        res = get_tasks(1)
        assert len(res) == 1
        assert res[0].id == test_send_tasks[0].id

    def test_multiple_tasks(self, test_send_tasks):
        res = get_tasks(2)
        assert len(res) == 2
        assert res[0].id == test_send_tasks[0].id
        assert res[1].id == test_send_tasks[1].id

    def test_count_greater_than_number_of_tasks(self, test_send_tasks):
        res = get_tasks(2 * len(test_send_tasks))
        assert len(res) == len(test_send_tasks)


class TestGetById:
    def test_unexisted(self):
        with pytest.raises(Task.DoesNotExist):
            get_by_id(-1)

    def test_success(self, test_send_tasks):
        task = get_by_id(test_send_tasks[0].id)
        assert task == test_send_tasks[0]


class TestComplete:
    def test_success(self, test_send_tasks):
        complete(test_send_tasks[0])
        assert len(Task.objects.all()) == len(test_send_tasks) - 1


@pytest.fixture
def recipients():
    return ["repicent1@yandex.ru", "repicent2@yandex.ru", "repicent3@yandex.ru"]


@pytest.fixture
def correct_user_template_variables():
    return {"name": "Someone"}


@pytest.fixture
def correct_user_template_variables_with_capital_letters():
    return {"nAmE": "Someone"}


@pytest.fixture
def duplicated_user_template_variables_in_different_cases():
    return {"nAmE": "Someone1", "NaMe": "Someone2"}


@pytest.fixture
def unknown_user_template_variable():
    return {"unsubscribe_link": "some_link"}


@pytest.fixture
def too_long_user_template_variable_value():
    too_long_value = "a" * (settings.USER_TEMPLATE_VARIABLE_VALUE_MAX_LENGTH + 1)
    return {"name": too_long_value}
