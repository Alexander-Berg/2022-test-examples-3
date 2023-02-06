from datetime import timezone

import pytest

from mail.ipa.ipa.core.entities.enums import TaskState, TaskType


class TestGetTasks:
    @pytest.fixture(autouse=True)
    async def tasks(self, create_task, org_id, user):
        tasks = {
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
    def expected(self, tasks):
        pending = tasks['pending']
        finished = tasks['finished']
        pending_with_subtasks = tasks['pending_with_subtasks']
        failed = tasks['finished_with_failed_subtasks']

        return {
            'code': 200,
            'status': 'success',
            'data': {
                'total': 4,
                'tasks': [
                    {
                        'task_id': str(failed.task_id),
                        'params': failed.meta_info,
                        'type': 'stop',
                        'created_at': failed.created.astimezone(timezone.utc).isoformat(),
                        'finished': True,
                        'has_errors': True,
                    },
                    {
                        'task_id': str(pending_with_subtasks.task_id),
                        'params': pending_with_subtasks.meta_info,
                        'type': 'json',
                        'created_at': pending_with_subtasks.created.astimezone(timezone.utc).isoformat(),
                        'finished': False,
                        'has_errors': False,
                    },
                    {
                        'task_id': str(finished.task_id),
                        'params': finished.meta_info,
                        'type': 'json',
                        'created_at': finished.created.astimezone(timezone.utc).isoformat(),
                        'finished': True,
                        'has_errors': False,
                    },
                    {
                        'task_id': str(pending.task_id),
                        'params': pending.meta_info,
                        'type': 'csv',
                        'created_at': pending.created.astimezone(timezone.utc).isoformat(),
                        'finished': False,
                        'has_errors': False,
                    },
                ],
            }
        }

    @pytest.fixture
    async def response(self, app, org_id):
        return await app.get(f'/import/{org_id}/tasks/')

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_response_status(self, response):
        assert 200 == response.status

    def test_response(self, response_json, expected):
        assert expected == response_json
