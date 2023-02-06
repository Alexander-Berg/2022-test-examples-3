import pytest

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.core.actions.collectors.edit import EditCollectorAction
from mail.ipa.ipa.core.entities.collector import Collector
from mail.ipa.ipa.interactions.directory import DirectoryClient
from mail.ipa.ipa.interactions.yarm import YarmClient
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestEditCollectorAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return EditCollectorAction

    @pytest.fixture
    async def collector(self, create_collector, existing_user, pop_id):
        return await create_collector(existing_user.user_id, pop_id=pop_id, status='UNKNOWN_ERROR')

    @pytest.fixture
    def params(self, collector, password, action_class):
        return {
            'collector_id': collector.collector_id,
            'password': password,
        }

    @pytest.fixture
    def email(self):
        return 'testcollector@example.test'

    @pytest.fixture(autouse=True)
    def mock_yarm(self, mocker, coromock):
        return mocker.patch.object(YarmClient, 'edit', coromock())

    @pytest.fixture(autouse=True)
    def mock_directory(self, mocker, email, coromock):
        return mocker.patch.object(DirectoryClient, 'get_user_by_login', coromock(mocker.Mock(email=email)))

    def test_yarm_called(self, mock_yarm, collector, existing_user, email, password, returned):
        mock_yarm.assert_called_once_with(existing_user.suid, collector.pop_id, email, password.value())

    def test_directory_called(self, mock_directory, collector, returned, existing_user, org_id):
        mock_directory.assert_called_once_with(org_id, existing_user.login)

    @pytest.mark.asyncio
    async def test_collector_status(self, storage, collector, returned):
        updated_collector = await storage.collector.get(collector.collector_id)
        assert_that(updated_collector.status, equal_to(Collector.OK_STATUS))
