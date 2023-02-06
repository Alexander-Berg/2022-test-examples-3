import pytest
import datetime

from mail.xiva.crm.src.util import cur_utc_ts
from mail.xiva.crm.src.executor import Executor
from mail.xiva.crm.src.xiva import FakeXiva
from mail.xiva.crm.src.scheduler import FakeScheduler
from mail.xiva.crm.src.user_events_db import FakeUserEventsDB
from mail.xiva.crm.src.history import FakeHistory


@pytest.fixture
def executor():
    xiva = FakeXiva()
    scheduler = FakeScheduler()
    user_events_db = FakeUserEventsDB()
    history = FakeHistory()
    config = {
        'campaigns': {
            'test_campaign': {
                'scenario': {
                    'initial': 'step1',
                    'step1': 'step2'
                },
                'notifications': {
                    'step1': {
                        'body': {
                            'ru_RU': 'body1'
                        },
                        'title': {
                            'ru_RU': 'title1'
                        },
                        'deeplink': 'deeplink1'
                    },
                    'step2': {
                        'body': {
                            'ru_RU': 'body2'
                        },
                        'title': {
                            'ru_RU': 'title2'
                        },
                        'deeplink': 'deeplink2'
                    }
                },
                'notification': {
                    'time': [],
                    'limits': 'once_a_day'
                }
            }
        },
        'step_check_time': 10,
        'xiva': {
            'retries': 1
        }
    }
    return Executor(config, scheduler, user_events_db, history, xiva)


@pytest.fixture
def tasks():
    return [{
        'id': '123',
        'execute_ts': cur_utc_ts(),
        'data': {
            'device_id': '666',
            'uid': '111',
            'locale': 'ru_RU',
            'timezone': 10800,
            'campaign': 'test_campaign',
            'step': 'step1'
        }
    }]


def test_send_notification(executor, tasks):
    executor.execute_tasks(tasks)
    assert len(executor.xiva.notifications) == 1
    assert executor.xiva.notifications[0]['deeplink'] == 'deeplink1'
    assert executor.xiva.notifications[0]['body'] == 'body1'
    assert executor.xiva.notifications[0]['title'] == 'title1'
    assert executor.xiva.notifications[0]['collapse_id'] == '666_test_campaign_step1'


def test_schedule_next_step(executor, tasks):
    executor.execute_tasks(tasks)
    assert len(executor.scheduler.tasks) == 1
    assert executor.scheduler.tasks['123']['data']['device_id'] == '666'
    assert executor.scheduler.tasks['123']['data']['uid'] == '111'
    assert executor.scheduler.tasks['123']['data']['locale'] == 'ru_RU'
    assert executor.scheduler.tasks['123']['data']['timezone'] == 10800
    assert executor.scheduler.tasks['123']['data']['campaign'] == 'test_campaign'
    assert executor.scheduler.tasks['123']['data']['step'] == 'step2'


def test_not_schedule_last_step(executor, tasks):
    tasks[0]['data']['step'] = 'step2'
    executor.execute_tasks(tasks)
    assert len(executor.scheduler.tasks) == 0


def test_schedule_retry_on_xiva_error(executor, tasks):
    executor.xiva.response = {
        'response_status': 500,
        'response_text': 'Internal server error',
        'codes': '500'
    }
    executor.execute_tasks(tasks)
    assert len(executor.scheduler.tasks) == 1
    assert executor.scheduler.tasks['123']['data']['device_id'] == '666'
    assert executor.scheduler.tasks['123']['data']['uid'] == '111'
    assert executor.scheduler.tasks['123']['data']['locale'] == 'ru_RU'
    assert executor.scheduler.tasks['123']['data']['timezone'] == 10800
    assert executor.scheduler.tasks['123']['data']['campaign'] == 'test_campaign'
    assert executor.scheduler.tasks['123']['data']['step'] == 'step1'
    assert executor.scheduler.tasks['123']['data']['attempt'] == 1


def test_not_retry_if_retry_limit_exceeded(executor, tasks):
    tasks[0]['data']['attempt'] = 1
    executor.xiva.response = {
        'response_status': 500,
        'response_text': 'Internal server error',
        'codes': '500'
    }
    executor.execute_tasks(tasks)
    assert len(executor.scheduler.tasks) == 1
    assert executor.scheduler.tasks['123']['data']['step'] == 'step2'
    assert executor.scheduler.tasks['123']['data']['attempt'] == 0


def test_skip_unknown_locale(executor, tasks):
    tasks[0]['data']['locale'] = 'kz_KZ'
    executor.execute_tasks(tasks)
    assert len(executor.xiva.notifications) == 0
    assert len(executor.scheduler.tasks) == 1
    assert executor.scheduler.tasks['123']['data']['step'] == 'step2'


def test_skip_irrelevant_tasks(executor, tasks):
    executor.user_events_db.irrelevant_devices = ['666']
    executor.execute_tasks(tasks)
    assert len(executor.xiva.notifications) == 0
    assert len(executor.scheduler.tasks) == 1
    assert executor.scheduler.tasks['123']['data']['step'] == 'step2'


@pytest.mark.parametrize(
    "limit,days,minutes", [
        ("no_limits", 0, 25),
        ("no_limits", 1, -25),
        ("once_a_day", 1, 25)
    ])
def test_schedule_next_step_on_right_time(executor, tasks, limit, days, minutes):
    next_step_begin_time = datetime.datetime.now() + datetime.timedelta(days=days, minutes=minutes)
    next_step_end_time = next_step_begin_time + datetime.timedelta(minutes=15)
    exec_time = next_step_begin_time - datetime.timedelta(minutes=executor.config['step_check_time'])
    executor.config['campaigns']['test_campaign']['notification']['time'] = [[next_step_begin_time.strftime("%H:%M"), next_step_end_time.strftime("%H:%M")]]
    executor.config['campaigns']['test_campaign']['notification']['limits'] = limit
    executor.execute_tasks(tasks)
    actual_exec_time = datetime.datetime.fromtimestamp(executor.scheduler.tasks['123']['execute_ts'] / (10**6))
    assert actual_exec_time.day == exec_time.day
    assert actual_exec_time.hour == exec_time.hour
    assert actual_exec_time.minute == exec_time.minute
    actual_start_time = datetime.datetime.fromtimestamp(executor.scheduler.tasks['123']['data']['begin_ts'] / (10**6))
    assert actual_start_time.day == next_step_begin_time.day
    assert actual_start_time.hour == next_step_begin_time.hour
    assert actual_start_time.minute == next_step_begin_time.minute
    actual_end_time = datetime.datetime.fromtimestamp(executor.scheduler.tasks['123']['data']['end_ts'] / (10**6))
    assert actual_end_time.day == next_step_end_time.day
    assert actual_end_time.hour == next_step_end_time.hour
    assert actual_end_time.minute == next_step_end_time.minute
