import logging
from collections import namedtuple
from time import sleep

from mail.pypg.pypg.common import qexec, fetch_as_objects
from mail.pypg.pypg.query_conf import load_from_my_file

from mail.husky.husky.types import Status

QUERIES = load_from_my_file(__file__)
log = logging.getLogger(__name__)


TaskData = namedtuple(
    'TaskData',
    (
        'uid',
        'task',
        'status',
        'task_args',
        'transfer_id',
        'error_type',
        'tries',
        'try_notices',
        'last_update',
    )
)


def get_task_row(context, transfer_id):
    cur = qexec(
        context.huskydb_conn,
        QUERIES.get_task,
        transfer_id=transfer_id
    )
    tasks = [x for x in fetch_as_objects(cur, TaskData)]
    if not tasks:
        return None
    assert len(tasks) == 1, \
        'Expect one task but got {0}'.format(tasks)
    return tasks[0]


def get_task_row_expect_one(context, transfer_id):
    task = get_task_row(context, transfer_id)
    assert task is not None, \
        'Can\'t find task for %r transfer_id' % transfer_id
    return task


def get_task(context, transfer_id=None, expected_status=Status.Complete):
    if transfer_id is None:
        transfer_id = context.transfer_id
    for timeout in [2, 3, 5, 10, 30, 0]:
        task = get_task_row_expect_one(context, transfer_id)

        log.info('Got task %s', task)
        if expected_status is None or task.status in\
                (expected_status, Status.Error, Status.Complete):
            return task
        else:
            sleep(timeout)


def check_task(context, transfer_id=None,
               expected_status=Status.Complete):
    task = get_task(context, transfer_id,
                    expected_status=expected_status)
    assert task, 'Could not wait for task'
    actual_status = task.status
    error = task.try_notices
    assert actual_status == expected_status, \
        'Expected {expected_status} but was {actual_status}\nError is {error}'.format(**locals())
    context.task_info = task
