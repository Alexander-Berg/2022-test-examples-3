import os.path

import pytest

from edera.exceptions import WorkflowExecutionError
from edera.workflow.tasks import RunStaticWorkflow


def test_workflow_finishes_successfully(correct_fly_task):
    RunStaticWorkflow(task=correct_fly_task).execute()
    with open(os.path.join(correct_fly_task.root, "flight.log")) as stream:
        assert stream.read() == "KU"


def test_workflow_actually_runs_only_once(correct_fly_task):
    RunStaticWorkflow(task=correct_fly_task).execute()
    log = os.path.join(correct_fly_task.root, "flight.log")
    timestamp = os.stat(log).st_mtime
    RunStaticWorkflow(task=correct_fly_task).execute()
    assert os.stat(log).st_mtime == timestamp


def test_executer_notifies_about_fails(incorrect_fly_task):
    with pytest.raises(WorkflowExecutionError) as info:
        RunStaticWorkflow(task=incorrect_fly_task).execute()
    assert set(info.value.failed_tasks) == {incorrect_fly_task}
