import pytest

from hamcrest import assert_that, equal_to, has_entries

from mail.ipa.ipa.core.entities.enums import TaskState, TaskType
from mail.ipa.ipa.core.entities.task import Task
from mail.ipa.ipa.core.exceptions import NotConnectUIDError
from mail.ipa.ipa.taskq.app import UserWorker


@pytest.fixture
async def worker(test_logger, worker_app):
    worker = UserWorker(logger=test_logger)
    worker.app = worker_app
    await worker.register_worker(worker_app)
    return worker


@pytest.fixture
def task_params():
    return {}


@pytest.fixture
async def meta_task(storage, create_task, organization):
    return await create_task(entity_id=organization.org_id)


@pytest.fixture
def task_entity(task_params, general_init_import_params, user, meta_task):
    import datetime
    return Task(task_type=TaskType.INIT_USER_IMPORT,
                meta_task_id=meta_task.task_id,
                entity_id=user.user_id,
                run_at=datetime.datetime(2019, 10, 1, 0, 0, 0),
                params=task_params)


@pytest.fixture
async def task(storage, task_entity):
    return await storage.task.create(task_entity)


class TestCommitTask:
    @pytest.fixture
    async def task_commited(self, worker, task, storage):
        worker_entity = await storage.worker.get(worker.worker_id)
        worker_entity.task_id = task.task_id
        await storage.worker.save(worker_entity)
        task.state = TaskState.FINISHED
        await worker.commit_task(task, storage)

    @pytest.mark.parametrize('task_params', ({'user_info': {'personal': 'data'}},))
    @pytest.mark.asyncio
    async def test_commit_task_cleans_user_info(self, task_commited, task, storage):
        assert_that(
            (await storage.task.get(task.task_id)).params,
            has_entries({
                'user_info': None,
            }),
        )

    @pytest.mark.parametrize('task_params', ({'password': {'encrypted': 'ciphertext'}},))
    @pytest.mark.asyncio
    async def test_commit_task_cleans_password(self, task_commited, task, storage):
        assert_that(
            (await storage.task.get(task.task_id)).params,
            has_entries({
                'password': None,
            }),
        )

    @pytest.mark.parametrize('task_params', ({'users': [{'password': 'secret'}, {'password': 'secret-too'}]},))
    @pytest.mark.asyncio
    async def test_commit_task_cleans_users_array(self, task_commited, task, storage):
        params = (await storage.task.get(task.task_id)).params
        assert_that(
            [u['password'] for u in params['users']],
            equal_to([None, None])
        )


@pytest.mark.asyncio
async def test_fetch_task_for_work(worker, storage, task):
    assert_that(
        await worker.fetch_task_for_work(storage),
        equal_to(task),
    )


@pytest.mark.parametrize('exception, should_retry', (
    (NotConnectUIDError(123), False),
    (AttributeError(), True),
))
@pytest.mark.asyncio
async def test_should_retry_exception(worker, exception, should_retry):
    assert_that(
        worker.should_retry_exception(None, exception),
        equal_to(should_retry),
    )
