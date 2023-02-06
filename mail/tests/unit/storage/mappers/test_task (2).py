from collections import Counter
from datetime import timedelta
from typing import List

import pytest

from sendr_taskqueue.worker.base.entites import BaseTaskParams
from sendr_utils import enum_value, json_value

from hamcrest import assert_that, has_properties

from mail.payments.payments.core.entities.enums import CallbackMessageType, PayStatus, TaskState, TaskType
from mail.payments.payments.core.entities.task import Task
from mail.payments.payments.storage.db.tables import metadata
from mail.payments.payments.utils.datetime import utcnow
from mail.payments.payments.utils.helpers import without_none


@pytest.fixture(params=(None, 1))
def task(request):
    return Task(
        task_type=TaskType.API_CALLBACK,
        retries=2,
        task_id=4,
        params=without_none({
            'callback_url': 'http',
            'callback_message_type': enum_value(CallbackMessageType.ORDER_STATUS_UPDATED),
            'message': {
                'merchant_uid': 123,
                'order_id': 456,
                'status': enum_value(PayStatus.PAID),
                'updated': utcnow().isoformat(),
            },
            'tvm_id': request.param,
        }),
        details={'xx': 'yy'},
    )


@pytest.fixture
def task_dict(task):
    task_dict = {
        attr: getattr(task, attr)
        for attr in [
            'task_type',
            'action_name',
            'task_id',
            'params',
            'state',
            'retries',
            'details',
            'run_at',
            'created',
            'updated',
        ]
    }
    task_dict['params'] = (
        task_dict['params'].asdict()
        if isinstance(task_dict['params'], BaseTaskParams)
        else json_value(task_dict['params'])
    )
    return task_dict


def test_map(storage, task, task_dict):
    assert storage.task.map(task_dict) == task


def test_unmap(storage, task, task_dict):
    task_dict.pop('task_id')
    task_dict.pop('created')
    task_dict.pop('updated')
    assert storage.task.unmap(task) == task_dict


@pytest.mark.asyncio
async def test_create_returns(storage, task):
    task_from_db = await storage.task.create(task)
    assert_that(
        task_from_db,
        has_properties({
            'task_type': task.task_type,
            'params': task.params,
            'state': task.state,
            'retries': task.retries,
            'details': task.details,
            'run_at': task.run_at,
        }),
    )


@pytest.mark.asyncio
async def test_create_writes(storage, task):
    await storage.task.create(task)
    result = await storage.conn.execute(f'SELECT count(*) FROM {metadata.schema}.tasks;')
    assert (await result.fetchone())[0] == 1


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(Task.DoesNotExist):
        await storage.task.get(task_id=123441)


@pytest.mark.asyncio
async def test_get(storage, task):
    task = await storage.task.create(task)
    assert await storage.task.get(task_id=task.task_id) == task


@pytest.mark.asyncio
async def test_get_for_work_not_found(storage):
    with pytest.raises(Task.DoesNotExist):
        assert await storage.task.get_for_work(
            task_types=[],
            task_states=[],
        ) is None


@pytest.mark.asyncio
async def test_get_for_work(storage, task):
    task.run_at = utcnow() - timedelta(seconds=30)
    task = await storage.task.create(task)
    assert await storage.task.get_for_work(
        task_types=[task.task_type],
        task_states=[task.state],
    ) == task


@pytest.mark.asyncio
async def test_save_not_found(storage, task):
    with pytest.raises(Task.DoesNotExist):
        await storage.task.save(task)


@pytest.mark.asyncio
async def test_save_returns(storage, task):
    task = await storage.task.create(task)
    task.details = {'some': 'text'}
    assert_that(
        await storage.task.save(task),
        has_properties({'details': task.details})
    )


@pytest.mark.asyncio
async def test_save_saves(storage, task):
    task = await storage.task.create(task)
    task.details = {'some': 'text'}
    saved = await storage.task.save(task)
    assert saved == await storage.task.get(task_id=task.task_id)


class TestCountTasks:
    @pytest.fixture
    def tasks_data(self, payments_settings):
        return [
            {
                'task_type': TaskType.START_MODERATION,
                'state': TaskState.PENDING,
                'retries': 1,
                'params': {'merchant_uid': 123}
            },

            {
                'task_type': TaskType.START_MODERATION,
                'state': TaskState.PENDING,
                'retries': payments_settings.DEFAULT_MAX_RETRIES,
                'params': {'merchant_uid': 123}
            },

            {
                'task_type': TaskType.START_ORDER_MODERATION,
                'state': TaskState.PENDING,
                'retries': payments_settings.DEFAULT_MAX_RETRIES + 1,
                'params': {'moderation_id': 123},
            },

            {
                'task_type': TaskType.START_ORDER_MODERATION,
                'state': TaskState.PROCESSING,
                'retries': 1,
                'params': {'moderation_id': 123},
            },

            {
                'task_type': TaskType.START_ORDER_MODERATION,
                'state': TaskState.FINISHED,
                'retries': 2,
                'params': {'moderation_id': 123},
            },
        ]

    @pytest.fixture
    async def tasks(self, storage, tasks_data):
        return [
            await storage.task.create(Task(**data))
            for data in tasks_data
        ]

    @pytest.mark.asyncio
    async def test_count_pending_by_type(self, storage, tasks):
        expected = dict(Counter([task.task_type for task in tasks if task.state == TaskState.PENDING]))
        assert {type_: count async for type_, count in storage.task.count_pending_by_type()} == expected

    @pytest.mark.asyncio
    async def test_count_pending_by_retries(self, storage, tasks):
        expected = dict(Counter([task.retries for task in tasks if task.state == TaskState.PENDING]))
        assert {num_retries: count async for num_retries, count in storage.task.count_pending_by_retries()} == expected


class TestCountFailedTasks:
    @pytest.fixture(autouse=True)
    async def setup(self, tasks, storage):
        for task in tasks:
            await storage.task.create(task)

    @pytest.fixture
    def tasks(self) -> List[Task]:
        return [
            Task(task_type=TaskType.START_REFUND,
                 state=TaskState.FAILED),
            Task(task_type=TaskType.START_REFUND,
                 state=TaskState.FINISHED),
            Task(task_type=TaskType.START_MODERATION,
                 state=TaskState.FAILED),
        ]

    @pytest.mark.asyncio
    async def test_count_failed_tasks(self, storage):
        result_count = await storage.task.count_failed_tasks(TaskType.START_REFUND)
        assert result_count == 1

    class TestNoMatches:
        @pytest.fixture
        def tasks(self) -> List[Task]:
            return []

        @pytest.mark.asyncio
        async def test_should_return_zero_if_no_matches(self, storage):
            result_count = await storage.task.count_failed_tasks(TaskType.START_REFUND)
            assert result_count == 0

    class TestFilterByActionName:
        @pytest.fixture
        def tasks(self, expected_action_name) -> List[Task]:
            return [
                Task(task_type=TaskType.RUN_ACTION,
                     action_name=expected_action_name,
                     state=TaskState.FAILED),
                Task(task_type=TaskType.RUN_ACTION,
                     action_name=expected_action_name,
                     state=TaskState.FINISHED),
                Task(task_type=TaskType.RUN_ACTION,
                     state=TaskState.FAILED),
            ]

        @pytest.fixture
        def expected_action_name(self) -> str:
            return "some_action_name"

        @pytest.mark.asyncio
        async def test_should_filter_by_action_name_if_passed(self, storage, expected_action_name):
            result_count = await storage.task.count_failed_tasks(TaskType.RUN_ACTION, expected_action_name)
            assert result_count == 1
