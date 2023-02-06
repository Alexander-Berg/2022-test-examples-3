from dataclasses import asdict

import pytest

from hamcrest import assert_that, contains, has_properties

from mail.ipa.ipa.core.actions.tasks import GetTasksAction
from mail.ipa.ipa.core.entities.enums import TaskState, TaskType
from mail.ipa.ipa.core.entities.task import TasksPage
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestTasksAction(ActionTestContract):
    @pytest.fixture
    def offset(self):
        return 1

    @pytest.fixture
    def limit(self):
        return 4

    @pytest.fixture
    def action_class(self):
        return GetTasksAction

    @pytest.fixture
    def params(self, offset, limit, org_id):
        return {'offset': offset, 'limit': limit, 'org_id': org_id}

    @pytest.fixture(autouse=True)
    async def tasks(self, create_task, org_id, user):
        tasks = {
            'omit_by_limit': await create_task(entity_id=org_id,
                                               task_type=TaskType.PARSE_CSV,
                                               state=TaskState.FINISHED,
                                               meta_info={'name': 'csv-name'},
                                               ),
            'pending': await create_task(entity_id=org_id,
                                         task_type=TaskType.PARSE_CSV,
                                         state=TaskState.PENDING,
                                         meta_info={'name': 'csv-name'},
                                         ),
            'finished': await create_task(entity_id=org_id,
                                          task_type=TaskType.INIT_IMPORT,
                                          state=TaskState.FINISHED,
                                          meta_info={'count': 5},
                                          ),
            'pending_with_subtasks': await create_task(entity_id=org_id,
                                                       task_type=TaskType.INIT_IMPORT,
                                                       state=TaskState.FINISHED,
                                                       meta_info={'count': 7},
                                                       ),
            'finished_with_failed_subtasks': await create_task(entity_id=org_id,
                                                               task_type=TaskType.STOP_IMPORT,
                                                               state=TaskState.FINISHED,
                                                               meta_info={},
                                                               ),
            'omit_by_offset': await create_task(entity_id=org_id,
                                                task_type=TaskType.PARSE_CSV,
                                                state=TaskState.FINISHED,
                                                meta_info={'name': 'csv-name'},
                                                ),
        }

        await create_task(
            entity_id=org_id,
            task_type=TaskType.INIT_USER_IMPORT,
            state=TaskState.FINISHED,
            meta_task_id=tasks['finished'].task_id,
        )
        await create_task(
            entity_id=user.user_id,
            task_type=TaskType.INIT_USER_IMPORT,
            state=TaskState.PENDING,
            meta_task_id=tasks['pending_with_subtasks'].task_id,
        )
        await create_task(
            entity_id=user.user_id,
            task_type=TaskType.INIT_USER_IMPORT,
            state=TaskState.FAILED,
            meta_task_id=tasks['finished_with_failed_subtasks'].task_id,
        )

        return tasks

    @pytest.fixture
    def expected(self, tasks, offset, limit):
        pending = tasks['pending']
        finished = tasks['finished']
        pending_with_subtasks = tasks['pending_with_subtasks']
        failed = tasks['finished_with_failed_subtasks']

        return TasksPage(
            offset=offset,
            limit=limit,
            total=6,
            tasks=contains(*[
                has_properties({
                    'task_id': failed.task_id,
                    'meta_info': failed.meta_info,
                    'task_type': TaskType.STOP_IMPORT,
                    'created': failed.created,
                    'finished': True,
                    'has_errors': True,
                }),
                has_properties({
                    'task_id': pending_with_subtasks.task_id,
                    'meta_info': pending_with_subtasks.meta_info,
                    'task_type': TaskType.INIT_IMPORT,
                    'created': pending_with_subtasks.created,
                    'finished': False,
                    'has_errors': False,
                }),
                has_properties({
                    'task_id': finished.task_id,
                    'meta_info': finished.meta_info,
                    'task_type': TaskType.INIT_IMPORT,
                    'created': finished.created,
                    'finished': True,
                    'has_errors': False,
                }),
                has_properties({
                    'task_id': pending.task_id,
                    'meta_info': pending.meta_info,
                    'task_type': TaskType.PARSE_CSV,
                    'created': pending.created,
                    'finished': False,
                    'has_errors': False,
                }),
            ])
        )

    def test_returned(self, returned, expected):
        assert_that(returned, has_properties(asdict(expected)))
