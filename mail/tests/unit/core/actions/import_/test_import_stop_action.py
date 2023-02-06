import pytest

from sendr_utils import alist

from hamcrest import assert_that, equal_to, has_properties

from mail.ipa.ipa.core.actions.import_.stop import CreateStopImportAction, StopOrgImportAction
from mail.ipa.ipa.core.entities.enums import TaskState, TaskType
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestCreateStopImportAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return CreateStopImportAction

    @pytest.fixture
    def params(self, org_id):
        return {'org_id': org_id}

    @pytest.fixture
    async def task(self, returned, storage):
        tasks = await alist(storage.task.find())
        return tasks[0]

    def test_returned(self, returned, task):
        assert_that(returned, equal_to(task.task_id))

    def test_task(self, task, org_id):
        assert_that(
            task,
            has_properties({
                'task_type': TaskType.STOP_IMPORT,
                'state': TaskState.PENDING,
                'entity_id': org_id,
                'params': {'org_id': org_id},
            })
        )


class TestStopOrgImportAction(ActionTestContract):
    @pytest.fixture(autouse=True)
    async def users(self, create_user, org_id):
        return [
            await create_user(org_id=org_id, login='ok', error=None),
            await create_user(org_id=org_id, login='bad', error='error'),
        ]

    @pytest.fixture
    def action_class(self):
        return StopOrgImportAction

    @pytest.fixture
    def params(self, organization, org_id):
        return {'org_id': org_id}

    def test_returned(self, returned):
        assert_that(returned, equal_to(None))

    @pytest.mark.asyncio
    async def test_users(self, returned, storage, org_id):
        assert_that(
            [user.login async for user in storage.user.find(org_id=org_id)],
            equal_to(['ok']),
        )
