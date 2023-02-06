from copy import copy
from datetime import timedelta

import pytest

from sendr_utils import alist, utcnow

from hamcrest import assert_that, contains, contains_inanyorder, equal_to, has_length, has_properties

from mail.ipa.ipa.core.entities.enums import TaskState, TaskType
from mail.ipa.ipa.core.entities.task import Task


class TestGetForWorkByOrg:
    @pytest.fixture(autouse=True)
    async def organizations(self, org_id, other_org_id, storage):
        await storage.organization.ensure_exists(org_id)
        await storage.organization.ensure_exists(other_org_id)

    @pytest.fixture
    def task_entity(self, org_id, past_time):
        return Task(task_type=TaskType.PARSE_CSV,
                    entity_id=org_id,
                    run_at=past_time,
                    )

    @pytest.fixture
    async def task(self, storage, task_entity):
        return await storage.task.create(task_entity)

    @pytest.mark.asyncio
    async def test_returns_task(self, storage, org_id, task):
        task_for_work = await storage.task.get_org_task_for_work((TaskType.PARSE_CSV,))
        assert_that(task_for_work.task_id, equal_to(task.task_id))

    @pytest.mark.asyncio
    async def test_returns_oldest_pending_task(self, storage, org_id, task, task_entity, past_time):
        task.state = TaskState.FINISHED
        task = await storage.task.save(task)

        pending_task = await storage.task.create(task_entity)

        additional_tasks = 5
        for i in range(additional_tasks):
            new_task_entity = copy(task_entity)
            new_task_entity.run_at = past_time + timedelta(minutes=additional_tasks - i)
            await storage.task.create(new_task_entity)

        task_for_work = await storage.task.get_org_task_for_work((TaskType.PARSE_CSV,))
        assert_that(task_for_work.task_id, equal_to(pending_task.task_id))

    @pytest.mark.asyncio
    async def test_raises_when_org_task_is_processing(self, storage, org_id, task):
        task.state = TaskState.PROCESSING
        await storage.task.save(task)

        with pytest.raises(Task.DoesNotExist):
            await storage.task.get_org_task_for_work((TaskType.PARSE_CSV,))

    @pytest.mark.asyncio
    async def test_returns_task_if_any_other_org_tasks_are_processing(self,
                                                                      storage,
                                                                      org_id,
                                                                      other_org_id,
                                                                      task,
                                                                      create_task):
        await create_task(entity_id=other_org_id, state=TaskState.PROCESSING, task_type=task.task_type)

        task_for_work = await storage.task.get_org_task_for_work((TaskType.PARSE_CSV,))
        assert_that(task_for_work.task_id, equal_to(task.task_id))

    @pytest.mark.asyncio
    async def test_returns_task_if_any_other_org_tasks_are_meta_processing(self,
                                                                           storage,
                                                                           org_id,
                                                                           other_org_id,
                                                                           task,
                                                                           create_task):
        meta_task = await create_task(entity_id=other_org_id, state=TaskState.FINISHED, task_type=TaskType.PARSE_CSV)
        await create_task(entity_id=1,
                          meta_task_id=meta_task.task_id,
                          state=TaskState.PROCESSING,
                          task_type=TaskType.INIT_USER_IMPORT
                          )

        task_for_work = await storage.task.get_org_task_for_work((TaskType.PARSE_CSV,))
        assert_that(task_for_work.task_id, equal_to(task.task_id))

    @pytest.mark.asyncio
    async def test_raises_when_any_other_task_for_this_org_is_processing(self, storage, org_id, task, create_task):
        task.state = TaskState.PROCESSING
        await storage.task.save(task)
        await create_task(entity_id=org_id,
                          state=TaskState.PENDING,
                          task_type=TaskType.PARSE_CSV)

        with pytest.raises(Task.DoesNotExist):
            await storage.task.get_org_task_for_work((TaskType.PARSE_CSV,))

    @pytest.mark.asyncio
    async def test_raises_when_any_other_task_for_this_org_is_meta_processing(self,
                                                                              storage,
                                                                              org_id,
                                                                              task,
                                                                              past_time,
                                                                              create_task):
        task.state = TaskState.FINISHED
        await storage.task.save(task)
        await create_task(entity_id=org_id,
                          meta_task_id=task.task_id,
                          state=TaskState.PROCESSING,
                          task_type=TaskType.INIT_USER_IMPORT)
        await create_task(entity_id=org_id,
                          state=TaskState.PENDING,
                          task_type=TaskType.PARSE_CSV)

        with pytest.raises(Task.DoesNotExist):
            await storage.task.get_org_task_for_work((TaskType.PARSE_CSV,))

    @pytest.mark.asyncio
    async def test_respects_run_at(self, storage, org_id, other_org_id, create_task, past_time, make_now):
        future = make_now() + timedelta(hours=1)

        await create_task(entity_id=org_id,
                          run_at=future,
                          state=TaskState.PENDING,
                          task_type=TaskType.PARSE_CSV)
        await create_task(entity_id=org_id,
                          state=TaskState.PENDING,
                          task_type=TaskType.PARSE_CSV)
        free_task = await create_task(entity_id=other_org_id,
                                      state=TaskState.PENDING,
                                      task_type=TaskType.PARSE_CSV)

        task_for_work = await storage.task.get_org_task_for_work((TaskType.PARSE_CSV,))
        assert_that(task_for_work.task_id, equal_to(free_task.task_id))


class TestGetForWorkByUser:
    @pytest.fixture
    def make_processing_tasks(self, create_task):
        async def _make_processing_tasks(meta_task_id, n_tasks):
            for _ in range(n_tasks):
                await create_task(task_type=TaskType.INIT_USER_IMPORT,
                                  meta_task_id=meta_task_id,
                                  state=TaskState.PROCESSING
                                  )

        return _make_processing_tasks

    @pytest.fixture
    def create_meta_task(self, storage):
        async def _create_meta_task(org_id):
            return await storage.task.create(
                Task(
                    task_type=TaskType.PARSE_CSV,
                    entity_id=org_id,
                    state=TaskState.FINISHED,
                )
            )
        return _create_meta_task

    @pytest.fixture(autouse=True)
    async def organizations(self, org_id, storage):
        await storage.organization.ensure_exists(org_id)

    @pytest.fixture
    async def meta_task(self, org_id, create_meta_task):
        return await create_meta_task(org_id)

    @pytest.fixture
    def task_entity(self, user, past_time, meta_task):
        return Task(task_type=TaskType.INIT_USER_IMPORT,
                    meta_task_id=meta_task.task_id,
                    entity_id=user.user_id,
                    run_at=past_time,
                    )

    @pytest.fixture
    async def task(self, storage, task_entity):
        return await storage.task.create(task_entity)

    @pytest.mark.asyncio
    async def test_returns_task(self, storage, task):
        task_for_work = await storage.task.get_user_task_for_work((TaskType.INIT_USER_IMPORT,))
        assert_that(task_for_work.task_id, equal_to(task.task_id))

    @pytest.mark.asyncio
    async def test_does_return_task_if_limit_is_satisfied(self, storage, task, make_processing_tasks, meta_task):
        LIMIT = 4
        await make_processing_tasks(meta_task.task_id, LIMIT - 1)

        task_for_work = await storage.task.get_user_task_for_work((TaskType.INIT_USER_IMPORT,),
                                                                  same_metatask_processing_limit=LIMIT)
        assert_that(task_for_work.task_id, equal_to(task.task_id))

    @pytest.mark.asyncio
    async def test_does_not_return_task_if_limit_is_exceeded(self, storage, task, make_processing_tasks, meta_task):
        LIMIT = 4
        await make_processing_tasks(meta_task.task_id, LIMIT)

        with pytest.raises(Task.DoesNotExist):
            await storage.task.get_user_task_for_work((TaskType.INIT_USER_IMPORT,),
                                                      same_metatask_processing_limit=LIMIT)

    @pytest.mark.asyncio
    async def test_metatask_does_not_affect_other_metatasks(self,
                                                            storage,
                                                            org_id,
                                                            other_org_id,
                                                            meta_task,
                                                            task,
                                                            create_meta_task,
                                                            make_processing_tasks,
                                                            ):
        LIMIT = 4
        other_meta_task = await create_meta_task(other_org_id)
        await make_processing_tasks(other_meta_task.task_id, LIMIT)

        task_for_work = await storage.task.get_user_task_for_work((TaskType.INIT_USER_IMPORT,),
                                                                  same_metatask_processing_limit=LIMIT)
        assert_that(task_for_work.task_id, equal_to(task.task_id))


class TestTaskMapperGetOrgTasks:
    @pytest.mark.asyncio
    async def test_limit(self, create_task, org_id, storage):
        await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV)
        await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV)
        await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV)
        assert_that(
            await alist(storage.task.get_org_tasks(org_id=org_id,
                                                   task_types=(TaskType.PARSE_CSV,),
                                                   offset=0,
                                                   limit=2)),
            has_length(2),
        )

    @pytest.mark.asyncio
    async def test_offset(self, create_task, org_id, storage):
        for _ in range(5):
            await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV)

        assert_that(
            await alist(storage.task.get_org_tasks(org_id=org_id,
                                                   task_types=(TaskType.PARSE_CSV,),
                                                   offset=1,
                                                   limit=10)),
            has_length(4),
        )

    @pytest.mark.asyncio
    async def test_order(self, create_task, org_id, storage, make_now):
        first = await create_task(created=make_now(), entity_id=org_id, task_type=TaskType.PARSE_CSV)
        recent = await create_task(created=make_now(), entity_id=org_id, task_type=TaskType.PARSE_CSV)

        expected_order = [recent, first]

        assert_that(
            await alist(storage.task.get_org_tasks(org_id=org_id,
                                                   task_types=(TaskType.PARSE_CSV,),
                                                   offset=0,
                                                   limit=10)),
            contains(*[has_properties({'task_id': task.task_id}) for task in expected_order]),
        )

    @pytest.mark.asyncio
    async def test_types_filter(self, create_task, org_id, storage):
        csv = await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV)
        await create_task(entity_id=org_id, task_type=TaskType.INIT_IMPORT)
        assert_that(
            await alist(storage.task.get_org_tasks(org_id=org_id,
                                                   task_types=(TaskType.PARSE_CSV,),
                                                   offset=0,
                                                   limit=10)),
            contains(has_properties({'task_id': csv.task_id})),
        )

    @pytest.mark.asyncio
    async def test_nonterminal_children(self, create_task, org_id, storage):
        meta = await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV, state=TaskState.FINISHED)
        await create_task(entity_id=org_id,
                          task_type=TaskType.INIT_USER_IMPORT,
                          state=TaskState.PENDING,
                          meta_task_id=meta.task_id)
        await create_task(entity_id=org_id,
                          task_type=TaskType.INIT_USER_IMPORT,
                          state=TaskState.FINISHED,
                          meta_task_id=meta.task_id)
        tasks = await alist(storage.task.get_org_tasks(org_id=org_id,
                                                       task_types=(TaskType.PARSE_CSV,),
                                                       offset=0,
                                                       limit=1))
        assert_that(
            tasks[0],
            has_properties({'nonterminal_children': 1})
        )


@pytest.mark.asyncio
async def test_get_org_tasks_count(create_task, org_id, storage):
    await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV, state=TaskState.FINISHED)
    await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV, state=TaskState.PENDING)
    await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV, state=TaskState.FAILED)
    count = await storage.task.get_org_tasks_count(org_id=org_id,
                                                   task_types=(TaskType.PARSE_CSV,),
                                                   )
    assert_that(count, equal_to(3))


@pytest.mark.asyncio
async def test_count_pending_by_type(create_task, org_id, storage):
    await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV, state=TaskState.FINISHED)
    await create_task(entity_id=org_id, task_type=TaskType.PARSE_CSV, state=TaskState.PENDING)
    await create_task(entity_id=org_id, task_type=TaskType.INIT_IMPORT, state=TaskState.PENDING)
    await create_task(entity_id=org_id, task_type=TaskType.INIT_IMPORT, state=TaskState.PENDING)
    await create_task(entity_id=org_id, task_type=TaskType.INIT_USER_IMPORT, state=TaskState.FAILED)
    stats = await alist(storage.task.count_pending_by_type())
    assert_that(
        stats,
        contains_inanyorder(
            (TaskType.PARSE_CSV, 1),
            (TaskType.INIT_IMPORT, 2),
        )
    )


class TestDeleteOldTasks:
    @pytest.mark.asyncio
    async def test_general_case(self, create_task, org_id, storage):
        await create_task(entity_id=org_id,
                          task_type=TaskType.PARSE_CSV,
                          state=TaskState.FINISHED,
                          created=utcnow() - timedelta(days=181))
        old_pending = await create_task(entity_id=org_id,
                                        task_type=TaskType.PARSE_CSV,
                                        state=TaskState.PENDING,
                                        created=utcnow() - timedelta(days=181))
        not_so_old = await create_task(entity_id=org_id,
                                       task_type=TaskType.PARSE_CSV,
                                       state=TaskState.FINISHED,
                                       created=utcnow() - timedelta(days=179))

        await storage.task.clean_old_tasks(timedelta(days=180))
        assert_that(
            sorted([task.task_id async for task in storage.task.find()]),
            equal_to(sorted([not_so_old.task_id, old_pending.task_id])),
        )

    @pytest.mark.asyncio
    async def test_ok_when_old_metatask_referenced_by_new_subtask(self, create_task, org_id, storage):
        metatask = await create_task(entity_id=org_id,
                                     task_type=TaskType.PARSE_CSV,
                                     state=TaskState.FINISHED,
                                     created=utcnow() - timedelta(days=181))
        new_pending = await create_task(entity_id=org_id,
                                        task_type=TaskType.INIT_USER_IMPORT,
                                        meta_task_id=metatask.task_id,
                                        state=TaskState.PENDING,
                                        created=utcnow())

        await storage.task.clean_old_tasks(timedelta(days=180))
        assert_that(
            sorted([task.task_id async for task in storage.task.find()]),
            equal_to(sorted([metatask.task_id, new_pending.task_id])),
        )

    @pytest.mark.asyncio
    async def test_deletes_with_subtasks(self, create_task, org_id, storage):
        metatask = await create_task(entity_id=org_id,
                                     task_type=TaskType.PARSE_CSV,
                                     state=TaskState.FINISHED,
                                     created=utcnow() - timedelta(days=181))
        await create_task(entity_id=org_id,
                          task_type=TaskType.INIT_USER_IMPORT,
                          meta_task_id=metatask.task_id,
                          state=TaskState.FINISHED,
                          created=utcnow() - timedelta(days=181))

        await storage.task.clean_old_tasks(timedelta(days=180))
        assert_that(
            sorted([task.task_id async for task in storage.task.find()]),
            equal_to([]),
        )

    @pytest.mark.asyncio
    async def test_delete_limited(self, create_task, org_id, storage):
        for _ in range(2):
            await create_task(entity_id=org_id,
                              task_type=TaskType.PARSE_CSV,
                              state=TaskState.FINISHED,
                              created=utcnow() - timedelta(days=181))
            metatask = await create_task(entity_id=org_id,
                                         task_type=TaskType.PARSE_CSV,
                                         state=TaskState.FINISHED,
                                         created=utcnow())
            await create_task(entity_id=org_id,
                              task_type=TaskType.INIT_USER_IMPORT,
                              meta_task_id=metatask.task_id,
                              state=TaskState.FINISHED,
                              created=utcnow() - timedelta(days=181))

        await storage.task.clean_old_tasks(timedelta(days=180), limit=1)
        assert_that(
            await alist(storage.task.find()),
            has_length(4),
        )
