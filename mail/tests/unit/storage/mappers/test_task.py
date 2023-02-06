from collections import Counter
from copy import copy

import pytest
from sqlalchemy import func

from sendr_taskqueue.worker.storage import TaskState
from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder

from mail.beagle.beagle.core.entities.enums import TaskType
from mail.beagle.beagle.core.entities.task import Task
from mail.beagle.beagle.storage.exceptions import OrganizationNotFound


@pytest.mark.asyncio
class TestTaskMapper:
    @pytest.fixture
    def task_entity(self, org_id):
        return Task(
            org_id=org_id,
            task_type=TaskType.SYNC_ORGANIZATION,
            run_at=func.now(),
            created=func.now(),
            updated=func.now(),
        )

    @pytest.fixture
    def tasks_data(self, org):
        return [
            {
                'org_id': org.org_id,
                'task_type': TaskType.SYNC_ORGANIZATION,
                'state': TaskState.PENDING,
            },

            {
                'org_id': org.org_id,
                'task_type': TaskType.SYNC_ORGANIZATION,
                'state': TaskState.PENDING,
            },

            {
                'org_id': org.org_id,
                'task_type': TaskType.RUN_ACTION,
                'state': TaskState.FINISHED,
            },
            {
                'org_id': org.org_id,
                'task_type': TaskType.RUN_ACTION,
                'state': TaskState.PENDING,
            },
        ]

    @pytest.fixture
    async def tasks(self, storage, tasks_data):
        return [
            await storage.task.create(Task(**data))
            for data in tasks_data
        ]

    async def test_create(self, storage, org, task_entity):
        task = await storage.task.create(task_entity)
        returned = await storage.task.get(task.task_id)
        assert task == returned

    async def test_create_for_not_found_organization(self, task_entity, storage):
        with pytest.raises(OrganizationNotFound):
            await storage.task.create(task_entity)

    async def test_get_for_work_by_org_single(self, org, task_entity, storage):
        task = await storage.task.create(task_entity)
        returned = await storage.task.get_for_work_by_org((TaskType.SYNC_ORGANIZATION,))
        assert task == returned

    async def test_get_for_work_by_org_with_dup(self, org, task_entity, storage, randn):
        first_task = None
        another_tasks = []

        for _ in range(randn(min=2, max=16)):
            task = await storage.task.create(task_entity)

            if first_task is None:
                first_task = task
            else:
                another_tasks.append(task)

        returned = await storage.task.get_for_work_by_org((TaskType.SYNC_ORGANIZATION,))
        assert first_task == returned

    async def test_delete_duplicates_by_org(self, org, task_entity, storage, randn):
        first_task = None
        tasks = []

        for _ in range(randn(min=2, max=16)):
            task = await storage.task.create(task_entity)

            if first_task is None:
                first_task = task
            else:
                task.state = TaskState.DELETED
            tasks.append(task)

        await storage.task.delete_duplicates_by_org(first_task)

        returned = await alist(storage.task.find(filters={'task_type': TaskType.SYNC_ORGANIZATION}))
        assert_that(tasks, contains_inanyorder(*returned))

    async def test_get_for_work_by_org_wait_finished(self, org, storage, task_entity):
        task_processing = copy(task_entity)
        task_processing.state = TaskState.PROCESSING

        await storage.task.create(task_processing)
        await storage.task.create(task_entity)

        with pytest.raises(Task.DoesNotExist):
            await storage.task.get_for_work_by_org((TaskType.SYNC_ORGANIZATION,))

    async def test_get_size(self, org, storage, task_entity):
        await storage.task.create(task_entity)
        queue_size = await storage.task.get_size()
        assert queue_size == 1

    async def test_count_pending_by_type(self, storage, tasks):
        expected = dict(Counter([task.task_type for task in tasks if task.state == TaskState.PENDING]))
        assert {type_: count async for type_, count in storage.task.count_pending_by_type()} == expected
