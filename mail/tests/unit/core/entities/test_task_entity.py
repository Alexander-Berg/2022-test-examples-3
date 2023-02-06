import pytest

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.core.entities.enums import TaskState, TaskType
from mail.ipa.ipa.core.entities.task import Task


@pytest.mark.parametrize('nonterminal_children, state, expected_finished', (
    (0, TaskState.PENDING, False),
    (1, TaskState.PENDING, False),
    (0, TaskState.FINISHED, True),
    (1, TaskState.FINISHED, False),
    (0, TaskState.FAILED, True),
    (1, TaskState.FAILED, False),
))
class TestTaskEntityFinished:
    @pytest.fixture
    def task(self, nonterminal_children, state):
        return Task(
            task_type=TaskType.PARSE_CSV,
            nonterminal_children=nonterminal_children,
            state=state,
        )

    def test_task_entity_finished(self, task, expected_finished):
        assert_that(task.finished, equal_to(expected_finished))
