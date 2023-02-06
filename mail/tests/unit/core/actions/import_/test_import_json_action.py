import pytest

from sendr_utils import alist

from hamcrest import assert_that, has_entries, has_properties

from mail.ipa.ipa.core.actions.import_.json import CreateImportFromJSONAction
from mail.ipa.ipa.core.actions.import_.org import InitOrgImportAction
from mail.ipa.ipa.core.entities.enums import TaskType
from mail.ipa.ipa.core.entities.password import Password
from mail.ipa.ipa.core.entities.user_info import UserInfo
from mail.ipa.ipa.core.exceptions import ImportEmptyError
from mail.ipa.ipa.tests.contracts import ActionTestContract, GeneratesStartEventContract


@pytest.fixture
def users():
    return [
        UserInfo(login='foo', src_login='foomail@example.test', password=Password.from_plain('foopasswd')),
        UserInfo(login='bar', src_login='barmail@example.test', password=Password.from_plain('barpasswd')),
        UserInfo(login='baz', src_login='bazmail@example.test', password=Password.from_plain('bazpasswd')),
    ]


class TestCreateImportFromJSONAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return CreateImportFromJSONAction

    @pytest.fixture
    def params(self, general_init_import_params, users):
        return {
            'params': general_init_import_params,
            'users': users,
        }

    @pytest.mark.asyncio
    async def test_task_created(self, returned, storage, general_init_import_params, users):
        tasks = await alist(storage.task.find())
        task = tasks[0]
        task.params = InitOrgImportAction.deserialize_kwargs(task.params)
        assert_that(
            task,
            has_properties({
                'task_type': TaskType.INIT_IMPORT,
                'params': has_entries({
                    'users': users,
                    'params': general_init_import_params,
                })
            })
        )

    class TestEmptyUsers:
        @pytest.fixture
        def params(self, params):
            params['users'] = []
            return params

        @pytest.fixture
        def expected_exc_type(self):
            return ImportEmptyError

        @pytest.mark.asyncio
        async def test_raises_import_empty_error(self, exc_info):
            pass

    class TestGeneratesEvent(GeneratesStartEventContract):
        pass
