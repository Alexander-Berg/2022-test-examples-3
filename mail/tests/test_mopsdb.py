import uuid
from mail.pg.mopsdb.devpack.components.mopsdb import MopsDb


def test_mopsdb(mops_coordinator):
    mopsdb = mops_coordinator.components[MopsDb]
    task_id = str(uuid.uuid4())
    mopsdb.execute("insert into operations.tasks values('%s', 123, '{}');" % task_id)
    result = mopsdb.query("select task_id, uid, task_info from operations.tasks;")
    assert result == [(task_id, 123, {})]
    mopsdb.execute("delete from operations.tasks")
