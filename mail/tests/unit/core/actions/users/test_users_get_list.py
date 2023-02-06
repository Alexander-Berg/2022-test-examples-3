import pytest

from mail.ipa.ipa.core.actions.users.get_list import GetUsersListAction
from mail.ipa.ipa.tests.contracts import ActionTestContract


class TestGetUsersListAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return GetUsersListAction

    @pytest.fixture
    def params(self, org_id):
        return {
            'org_id': org_id,
        }

    @pytest.mark.asyncio
    async def test_result(self, existing_user, returned):
        assert returned == [existing_user]

    class TestOffset:
        @pytest.fixture
        def params(self, org_id):
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
        async def test_result(self, existing_user, returned):
            assert returned == [existing_user]

        @pytest.mark.asyncio
        @pytest.mark.parametrize('login', ('-1',))
        async def test_not_found(self, existing_user, returned):
            assert returned == []
