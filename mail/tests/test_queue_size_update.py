import json

from uuid import uuid4 as rand_uuid
from mopsdb_cron import get_cron


def test_queue_size_update(context):
    cron = get_cron('queue_size_update', context.config)

    assert cron.run() == 0

    with context.reflect_db() as db:
        db.code.add_task(
            i_uid=1,
            i_task_id=rand_uuid(),
            i_task_info=json.dumps({'type': 'test', 'param': 'value'}),
            i_chunks=json.dumps([
                {'id': str(rand_uuid()), 'mids': [11, 12, 13]},
                {'id': str(rand_uuid()), 'mids': [21, 22]},
            ])
        )
        db.code.add_task(
            i_uid=2,
            i_task_id=rand_uuid(),
            i_task_info=json.dumps({'type': 'test', 'param': 'value'}),
            i_chunks=json.dumps([
                {'id': str(rand_uuid()), 'mids': [11, 12, 13]},
            ])
        )

    assert cron.run() == 3
