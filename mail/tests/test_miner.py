import pytest
from mail.xiva.crm.src.user_events_db import FakeUserEventsDB
from mail.xiva.crm.src.local_storage import FakeLocalStorage
from mail.xiva.crm.src.scheduler import FakeScheduler
from mail.xiva.crm.src.miner import Miner
from mail.xiva.crm.src.util import cur_utc_ts


@pytest.fixture
def miner():
    scheduler = FakeScheduler()
    storage = FakeLocalStorage()
    user_events_db = FakeUserEventsDB()
    config = {
        'campaigns': {
            'welcome_android': {
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
        'mine_new_devices_percent': {
            'android': 100,
            'ios': 100
        }
    }
    return Miner(user_events_db, scheduler, storage, config)


def test_schedule_new_device(miner):
    miner.user_events_db.fresh_installs = [{
        'uid': 'test_uid',
        'device_id': 'test_device_id',
        'platform': 'android',
        'locale': 'ru_RU',
        'timezone': 10800
    }]
    miner.mine_if_needed()
    assert len(miner.scheduler.tasks) == 1
    task = miner.scheduler.tasks['test_device_id']
    assert task['id'] == 'test_device_id'
    assert cur_utc_ts() - 2 * 10**6 <= task['execute_ts'] <= cur_utc_ts()  # Execute_ts between cur_utc_ts - 2s and cur_utc_ts
    assert task['data']['device_id'] == 'test_device_id'
    assert task['data']['uid'] == 'test_uid'
    assert task['data']['platform'] == 'android'
    assert task['data']['locale'] == 'ru_RU'
    assert task['data']['timezone'] == 10800
    assert task['data']['step'] == 'step1'
    assert task['data']['campaign'] == 'welcome_android'


def test_not_schedule_device_without_uid(miner):
    miner.user_events_db.fresh_installs = [{
        'uid': None,
        'device_id': 'test_device_id',
        'platform': 'android',
        'locale': 'ru_RU',
        'timezone': 10800
    }]
    miner.mine_if_needed()
    assert len(miner.scheduler.tasks) == 0


def test_find_uid_for_new_device(miner):
    miner.user_events_db.fresh_installs = [{
        'uid': None,
        'device_id': 'test_device_id',
        'platform': 'android',
        'locale': 'ru_RU',
        'timezone': 10800
    }]
    miner.user_events_db.uids_for_new_devices['test_device_id'] = 'test_uid'
    miner.mine_if_needed()
    assert len(miner.scheduler.tasks) == 1
    assert miner.scheduler.tasks['test_device_id']['data']['uid'] == 'test_uid'


def test_mine_percent(miner):
    miner.user_events_db.fresh_installs = \
        [
            {
                'uid': 'test_uid',
                'device_id': 'test_device_id',
                'platform': 'android',
                'locale': 'ru_RU',
                'timezone': 10800
            },
            {
                'uid': 'test_uid2',
                'device_id': 'test_device_id2',
                'platform': 'android',
                'locale': 'ru_RU',
                'timezone': 10800
            },
            {
                'uid': 'test_uid3',
                'device_id': 'test_device_id3',
                'platform': 'android',
                'locale': 'ru_RU',
                'timezone': 10800
            }
        ]
    miner.config['mine_new_devices_percent']['android'] = 67
    miner.mine_if_needed()
    assert len(miner.scheduler.tasks) == 2


def test_mine_uids_for_existing_devices(miner):
    miner.user_events_db.new_uids_for_existing_devices.append({
        'device_id': 'test_device_id',
        'uid': 'test_uid'
    })
    miner.mine_if_needed()
    assert len(miner.user_events_db.written_uids_for_existing_devices) == 1
    assert miner.user_events_db.written_uids_for_existing_devices[0]['device_id'] == 'test_device_id'
    assert miner.user_events_db.written_uids_for_existing_devices[0]['uid'] == 'test_uid'


def test_dont_mine_twice_a_day(miner):
    miner.user_events_db.fresh_installs = [{
        'uid': 'test_uid',
        'device_id': 'test_device_id',
        'platform': 'android',
        'locale': 'ru_RU',
        'timezone': 10800
    }]
    miner.mine_if_needed()
    assert len(miner.scheduler.tasks) == 1

    miner.user_events_db.fresh_installs = [{
        'uid': 'test_uid2',
        'device_id': 'test_device_id2',
        'platform': 'android',
        'locale': 'ru_RU',
        'timezone': 10800
    }]
    miner.mine_if_needed()
    assert len(miner.scheduler.tasks) == 1
