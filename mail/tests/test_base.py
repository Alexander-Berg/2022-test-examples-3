import pytest

import mail.husky.husky.tasks as tasks
from mail.husky.husky.types import Task
import mail.husky.husky.tasks.tests.helpers as helpers

parametrize = pytest.mark.parametrize


@parametrize(
    ('task',               'TaskHandler'), [
    (Task.CloneUser,        tasks.CloneUser),
    (Task.DeleteMailUser,   tasks.DeleteMailUser),
    (Task.DeleteShardsUser, tasks.DeleteShardsUser),
    (Task.Transfer,         tasks.Transfer),
    ]
)
def test_get_handler_returns_right_handler(task, TaskHandler):
    assert tasks.get_handler(task) == TaskHandler


@parametrize('unsupported_task', ['fix', 'call_sql'])
def test_get_handler_not_supported_on_unexisted_task(unsupported_task):
    with pytest.raises(tasks.errors.NotSupportedError):
        tasks.get_handler(unsupported_task)


@parametrize(
    ('TaskHandler'),
    [tasks.Transfer]
)
def test_task_missing_arguments_error_on_empty_args(TaskHandler):
    with pytest.raises(tasks.errors.MissingArgumentError):
        TaskHandler(
            *helpers.make_handler_args(task_args=helpers.EMPTY_TASK_ARGS)
        )
