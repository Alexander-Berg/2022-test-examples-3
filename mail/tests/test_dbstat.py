
import json
import asyncio

from uuid import uuid4 as random_uuid
from datetime import timedelta

from .context import reflect_mopsdb, reflect_queuedb

from mail.puli.lib.dbstat.dbstat import make_db_stat_director


def colliedb_append_service_sync_timestamps(context):
    context.colliedb.execute('''
        INSERT INTO collie.service_sync (service_type, sync_timestamp)
        VALUES ('ml'::collie.service_type, to_timestamp(EXTRACT(EPOCH FROM now())::bigint - 60));
        INSERT INTO collie.service_sync (service_type, sync_timestamp)
        VALUES ('staff'::collie.service_type, to_timestamp(EXTRACT(EPOCH FROM now())::bigint - 60));
    ''')


def colliedb_append_processing_users(context):
    context.colliedb.execute('''
        INSERT INTO collie.directory_events (user_id, user_type, pending_events_count) VALUES (1000, 'connect_organization'::collie.user_type, 0);
        INSERT INTO collie.directory_events (user_id, user_type, pending_events_count) VALUES (1001, 'connect_organization'::collie.user_type, 1);
        INSERT INTO collie.directory_events (user_id, user_type, pending_events_count) VALUES (1002, 'connect_organization'::collie.user_type, 2);
        INSERT INTO collie.directory_events (user_id, user_type, pending_events_count) VALUES (1003, 'connect_organization'::collie.user_type, 3);
    ''')


def mopsdb_append_operation_chunks(context):
    with reflect_mopsdb(context) as db:
        db.code.add_task(
            i_uid=1,
            i_task_id=random_uuid(),
            i_task_info=json.dumps({'type': 'test', 'param': 'value'}),
            i_chunks=json.dumps([
                {'id': str(random_uuid()), 'mids': [11, 12, 13]},
                {'id': str(random_uuid()), 'mids': [21, 22]},
            ])
        )
        db.code.add_task(
            i_uid=2,
            i_task_id=random_uuid(),
            i_task_info=json.dumps({'type': 'test', 'param': 'value'}),
            i_chunks=json.dumps([
                {'id': str(random_uuid()), 'mids': [11, 12, 13]},
            ])
        )


def mopsdb_append_locks(context):
    with reflect_mopsdb(context) as db:
        db.code.acquire_lock(1, timedelta(minutes=10), 'launch', 'host')
        db.code.acquire_lock(2, timedelta(minutes=10), 'launch', 'host')
        db.code.acquire_lock(3, timedelta(seconds=0), 'launch', 'host')


def queuedb_append_tasks(context):
    def add_task(db, task):
        db.code.add_task(
            i_uid=1,
            i_task=task,
            i_task_args=None,
            i_timeout=timedelta(minutes=15),
            i_request_id='test'
        )

    with reflect_queuedb(context, user='barbet') as db:
        add_task(db, 'backup_user')
        tasks = db.code.acquire_tasks(i_worker='test_worker', i_tasks_limit=1)
        db.code.complete_task(i_task_id=tasks[0].task_id, i_worker='test_worker')

        add_task(db, 'backup_user')
        tasks = db.code.acquire_tasks(i_worker='test_worker', i_tasks_limit=1)
        db.code.fail_task(
            i_task_id=tasks[0].task_id,
            i_worker='test_worker',
            i_reason='some error',
            i_max_retries=0,
            i_delay=timedelta(minutes=5),
        )

        add_task(db, 'restore_user')
        tasks = db.code.acquire_tasks(i_worker='test_worker', i_tasks_limit=1)
        db.code.complete_task(i_task_id=tasks[0].task_id, i_worker='test_worker')

        for _ in range(5):
            add_task(db, 'backup_user')
        db.code.acquire_tasks(i_worker='test_worker', i_tasks_limit=3)

        add_task(db, 'restore_user')


def poll_stats(dbstat):
    async def poll_all():
        for poller in dbstat.db_stat_pollers:
            await poller._job()

    asyncio.get_event_loop().run_until_complete(poll_all())

    return dbstat.stats


def extract_stats_value(signal_name, stats):
    for metric_name, metric_value in stats:
        if signal_name in metric_name:
            return metric_value

    return None


def test_dbstat(context):
    dbstat = make_db_stat_director(context.config)

    colliedb_append_service_sync_timestamps(context)

    stats_before = poll_stats(dbstat)
    assert 5 == len(stats_before)
    assert 1 == extract_stats_value('colliedb_minutes_since_ml_sync', stats_before)
    assert 1 == extract_stats_value('colliedb_minutes_since_staff_sync', stats_before)
    assert 0 == extract_stats_value('colliedb_total_processing_users', stats_before)
    assert 0 == extract_stats_value('mopsdb_total_operations_chunks', stats_before)

    colliedb_append_processing_users(context)
    mopsdb_append_operation_chunks(context)
    mopsdb_append_locks(context)
    queuedb_append_tasks(context)

    stats_after = poll_stats(dbstat)
    print(stats_after)
    assert 11 == len(stats_after)
    assert 3 == extract_stats_value('colliedb_total_processing_users', stats_after)
    assert 3 == extract_stats_value('mopsdb_total_operations_chunks', stats_after)
    assert 2 == extract_stats_value('mopsdb_alive_locks', stats_after)

    assert 2 == extract_stats_value('queuedb_barbet_backup_user_pending_axxv', stats_after)
    assert 3 == extract_stats_value('queuedb_barbet_backup_user_in_progress_axxv', stats_after)
    assert 1 == extract_stats_value('queuedb_barbet_backup_user_complete_axxv', stats_after)
    assert 1 == extract_stats_value('queuedb_barbet_backup_user_error_axxv', stats_after)

    assert 1 == extract_stats_value('queuedb_barbet_restore_user_pending_axxv', stats_after)
    assert 1 == extract_stats_value('queuedb_barbet_restore_user_complete_axxv', stats_after)
