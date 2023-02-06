import pytest

from hamcrest import assert_that, equal_to

from mail.ipa.ipa.core.actions.collectors.set_enabled import SetCollectorEnabledAction
from mail.ipa.ipa.core.exceptions import CollectorNotFoundError
from mail.ipa.ipa.interactions.yarm import YarmClient
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestSetCollectorEnabledAction(ActionTestContract):
    @pytest.fixture
    def param_org_id(self):
        return None

    @pytest.fixture
    async def collector(self, create_collector, existing_user, pop_id):
        return await create_collector(existing_user.user_id, enabled=False, pop_id=pop_id)

    @pytest.fixture
    def action_class(self):
        return SetCollectorEnabledAction

    @pytest.fixture
    def params(self, collector, param_org_id, action_class):
        return {
            'collector_id': collector.collector_id,
            'enabled': True,
            'org_id': param_org_id
        }

    @pytest.fixture(autouse=True)
    def mock_yarm(self, mocker, coromock):
        return mocker.patch.object(YarmClient, 'set_enabled', coromock())

    def test_yarm_called(self, mock_yarm, collector, existing_user, returned):
        mock_yarm.assert_called_once_with(existing_user.suid, collector.pop_id, True)

    @pytest.mark.asyncio
    async def test_enabled(self, returned):
        assert_that(returned.enabled, equal_to(True))

    @pytest.mark.asyncio
    async def test_returned(self, storage, collector, returned):
        assert_that(returned, equal_to(await storage.collector.get(collector.collector_id)))

    class TestOrgId:
        @pytest.fixture
        def param_org_id(self, org_id):
            return org_id

        @pytest.mark.asyncio
        async def test_returned(self, storage, collector, returned):
            assert_that(returned, equal_to(await storage.collector.get(collector.collector_id)))

        @pytest.mark.asyncio
        @pytest.mark.parametrize('param_org_id', [-1])
        async def test_org_id_invalid(self, action_coro):
            with pytest.raises(CollectorNotFoundError):
                await action_coro
