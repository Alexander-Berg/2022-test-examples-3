import pytest

from mail.ipa.ipa.core.actions.collectors.get_list import GetCollectorsListAction
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestGetCollectorsListAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return GetCollectorsListAction

    @pytest.fixture
    async def collector(self, create_collector, existing_user, pop_id):
        return await create_collector(existing_user.user_id, pop_id=pop_id, status='UNKNOWN_ERROR')

    @pytest.fixture
    def params(self, org_id):
        return {
            'org_id': org_id,
        }

    @pytest.mark.asyncio
    async def test_result(self, collector, returned):
        returned[0].user = collector.user
        assert returned == [collector]

    class TestOffset:
        @pytest.fixture
        def params(self, org_id, collector):
            return {
                'org_id': org_id,
                'offset': 1
            }

        @pytest.mark.asyncio
        async def test_result(self, returned):
            assert returned == []

    class TestLogin:
        @pytest.fixture
        def login(self, existing_user):
            return existing_user.login

        @pytest.fixture
        def params(self, org_id, login):
            return {
                'org_id': org_id,
                'login': login
            }

        @pytest.mark.asyncio
        async def test_result(self, collector, existing_user, returned):
            collector.user = existing_user
            assert returned == [collector]

        @pytest.mark.asyncio
        @pytest.mark.parametrize('login', ('-1',))
        async def test_not_found(self, collector, existing_user, returned):
            assert returned == []

    class TestStatus:
        @pytest.fixture
        def status(self, collector):
            return collector.status

        @pytest.fixture
        def params(self, org_id, status):
            return {
                'org_id': org_id,
                'status': status
            }

        @pytest.mark.asyncio
        async def test_result(self, collector, existing_user, returned):
            collector.user = existing_user
            assert returned == [collector]

        @pytest.mark.asyncio
        @pytest.mark.parametrize('status', ('-1',))
        async def test_not_found(self, collector, returned):
            assert returned == []
