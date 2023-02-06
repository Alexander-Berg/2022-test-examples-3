import asyncio
from datetime import timedelta

from .context import reflect_mopsdb

from mail.puli.lib.dbtask.base_task import create_task


def test_mopsdb_remove_old_locks(context):
    with reflect_mopsdb(context) as db:
        old_lock_uid = 1
        db.code.acquire_lock(old_lock_uid, timedelta(seconds=0), 'launch', 'host')
        new_lock_uid = 2
        db.code.acquire_lock(new_lock_uid, timedelta(minutes=10), 'launch', 'host')

        task = create_task('mopsdb_remove_old_locks', context.config)
        asyncio.get_event_loop().run_until_complete(task.run('mopsdb'))

        assert len(db.operations.user_locks.select(uid=old_lock_uid)) == 0, 'Lock should be released'
        assert len(db.operations.user_locks.select(uid=new_lock_uid)) == 1, 'Lock should remain'
