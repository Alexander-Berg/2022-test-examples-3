import pytest

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.core.actions.import_.base import BaseCreateImportTaskAction
from mail.ipa.ipa.tests.contracts import ActionTestContract, GeneratesStartEventContract


class TestBaseCreateImportTaskAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        class CreateImportTaskAction(BaseCreateImportTaskAction):
            async def _handle(self):
                assert False

        return CreateImportTaskAction

    @pytest.fixture
    def params(self, org_id):
        return {'org_id': org_id}

    @pytest.fixture
    def task(self, mocker):
        return mocker.Mock()

    @pytest.fixture(autouse=True)
    def mock_handle(self, action_class, mocker, coromock, task):
        return mocker.patch.object(action_class, '_handle', coromock(task))

    def test_result(self, returned, task):
        assert_that(returned, equal_to(task.task_id))

    class TestGeneratesEvent(GeneratesStartEventContract):
        pass
