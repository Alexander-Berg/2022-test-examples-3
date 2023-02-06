import pytest
import datetime

from mail.xiva.crm.src.fake_locker import FakeLocker
from mail.xiva.crm.src.scheduler import Scheduler, FakeSchedulerDB
from mail.xiva.crm.src.history import FakeHistory
from mail.xiva.crm.src.executor import FakeExecutor
from mail.xiva.crm.src.miner import FakeMiner
from mail.xiva.crm.src.util import cur_utc_ts, date_time_to_ts


TASK_EXPIRE_INTERVAL = 30


@pytest.fixture
def executor():
    return FakeExecutor()


@pytest.fixture
def miner():
    return FakeMiner()


def make_scheduler(executor, schedule):
    db = FakeSchedulerDB()
    db.schedule = schedule
    history = FakeHistory()
    locker = FakeLocker()
    config = {'wait_interval': 1, 'task_expire_interval_days': TASK_EXPIRE_INTERVAL}
    scheduler = Scheduler(db, history, locker, config)
    scheduler.set_exec_handler(lambda tasks: executor.execute_tasks(tasks))
    return scheduler


@pytest.fixture
def scheduler(executor):
    return make_scheduler(executor, [])


def test_scheduler_run_ready_task(executor):
    schedule = [
        {
            'id': '123',
            'ts': cur_utc_ts(),
            'execute_ts': cur_utc_ts(),
            'started': 0,
            'data': {'key': 'value'}
        }
    ]
    scheduler = make_scheduler(executor, schedule)
    scheduler.run_once()
    assert len(executor.tasks) == 1
    assert executor.tasks[0]['data']['key'] == 'value'
    assert len(scheduler.scheduler_db.schedule) == 1
    assert scheduler.scheduler_db.schedule[0]['started'] == 1


def test_scheduler_call_mine_handler(executor, miner):
    scheduler = make_scheduler(executor, [])
    scheduler.set_mine_handler(lambda: miner.mine_if_needed())
    scheduler.run_once()
    assert miner.mine_called


def test_scheduler_not_run_not_ready_task(executor):
    schedule = [
        {
            'id': '123',
            'ts': cur_utc_ts(),
            'execute_ts': cur_utc_ts() + 60 * 10**6,  # 1 minute
            'started': 0,
            'data': {'key': 'value'}
        }
    ]
    scheduler = make_scheduler(executor, schedule)
    scheduler.run_once()
    assert len(executor.tasks) == 0
    assert len(scheduler.scheduler_db.schedule) == 1
    assert scheduler.scheduler_db.schedule[0]['started'] == 0


def test_schedule_new_task(scheduler):
    now = cur_utc_ts()
    scheduler.schedule_tasks([{
        'id': '123',
        'execute_ts': now,
        'data': {'key': 'value'}
    }])
    assert len(scheduler.scheduler_db.schedule) == 1
    assert scheduler.scheduler_db.schedule[0]['execute_ts'] == now
    assert scheduler.scheduler_db.schedule[0]['data']['key'] == 'value'


def test_have_task(executor):
    schedule = [
        {
            'id': '123',
            'ts': cur_utc_ts(),
            'execute_ts': cur_utc_ts() + 60 * 10**6,  # 1 minute
            'started': 0,
            'data': {'key': 'value'}
        }
    ]
    scheduler = make_scheduler(executor, schedule)
    assert scheduler.have_task('123')
    assert not scheduler.have_task('321')


def test_start_stop(executor):
    schedule = [
        {
            'id': '123',
            'ts': cur_utc_ts(),
            'execute_ts': cur_utc_ts(),
            'started': 0,
            'data': {'key': 'value'}
        }
    ]
    scheduler = make_scheduler(executor, schedule)
    assert not scheduler.stopped
    scheduler.stop()
    assert scheduler.stopped
    scheduler.run_once()
    assert len(executor.tasks) == 0
    assert len(scheduler.scheduler_db.schedule) == 1
    assert scheduler.scheduler_db.schedule[0]['started'] == 0

    scheduler.start()
    assert not scheduler.stopped
    scheduler.run_once()
    assert len(executor.tasks) == 1
    assert len(scheduler.scheduler_db.schedule) == 1
    assert scheduler.scheduler_db.schedule[0]['started'] == 1


def test_dont_run_task_when_lock_not_acquired(executor):
    schedule = [
        {
            'id': '123',
            'ts': cur_utc_ts(),
            'execute_ts': cur_utc_ts(),
            'started': 0,
            'data': {'key': 'value'}
        }
    ]
    scheduler = make_scheduler(executor, schedule)
    scheduler.locker.acquired_flag = False
    scheduler.run_once()
    assert len(executor.tasks) == 0
    assert len(scheduler.scheduler_db.schedule) == 1
    assert scheduler.scheduler_db.schedule[0]['started'] == 0


def test_remove_old_completed_tasks(executor):
    execute_date = datetime.datetime.now() - datetime.timedelta(days=TASK_EXPIRE_INTERVAL+1)
    execute_ts = date_time_to_ts(execute_date)
    schedule = [{
        'id': '123',
        'execute_ts': execute_ts,
        'started': 1,
        'data': {'key': 'value'}
    }]
    scheduler = make_scheduler(executor, schedule)
    scheduler.schedule_tasks([{
        'id': '124',
        'execute_ts': execute_ts,
        'started': 0,
        'data': {'key': 'value'}
    }])
    assert len(scheduler.scheduler_db.schedule) == 1
    assert scheduler.scheduler_db.schedule[0]['id'] == '124'
