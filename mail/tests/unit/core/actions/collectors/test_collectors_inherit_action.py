import pytest

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.core.actions.collectors.inherit import InheritUserCollectorsAction
from mail.ipa.ipa.interactions.yarm import YarmClient
from mail.ipa.ipa.interactions.yarm.entities import YarmCollector, YarmCollectorState
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestInheritCollectors(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return InheritUserCollectorsAction

    @pytest.fixture
    def params(self, existing_user):
        return {'user': existing_user}

    @pytest.fixture
    def existing_pop_id(self):
        return 'pop-existing'

    @pytest.fixture
    def new_collector(self, import_params):
        return YarmCollector(
            pop_id='pop-new',
            login='login',
            email='email',
            state=YarmCollectorState.TEMPORARY_ERROR,
            server='server.test',
            port=993,
            imap=True,
            ssl=True,
            delete_msgs=False,
        )

    @pytest.fixture
    async def existing_collector(self, create_collector, existing_user, existing_pop_id):
        return await create_collector(existing_user.user_id, pop_id=existing_pop_id)

    @pytest.fixture(autouse=True)
    async def mock_yarm(self, mocker, new_collector):
        async def yarm_list(*args, **kwargs):
            yield new_collector
        return mocker.patch.object(YarmClient, 'list', yarm_list)

    @pytest.mark.asyncio
    async def test_new_collector_inherited(self, storage, new_collector, existing_collector, existing_user, returned):
        db_collectors = [collector.pop_id async for collector in storage.collector.find(user_id=existing_user.user_id)]
        assert_that(
            sorted(db_collectors),
            equal_to(sorted([existing_collector.pop_id, new_collector.pop_id]))
        )
