import pytest

from mail.beagle.beagle.core.actions.mail_list.get_list import GetListListMailAction
from mail.beagle.beagle.core.exceptions import OrganizationNotFoundError


@pytest.mark.asyncio
class TestGetMailList:
    @pytest.fixture
    def params(self, mail_list):
        return {'org_id': mail_list.org_id}

    @pytest.fixture
    def returned_func(self):
        async def _inner(params):
            return await GetListListMailAction(**params).run()

        return _inner

    async def test_get(self, mail_list, params, returned_func):
        returned = await returned_func(params)
        assert [mail_list] == returned

    async def test_unknown_org(self, returned_func, params, randn):
        params.update({'org_id': randn()})
        with pytest.raises(OrganizationNotFoundError):
            await returned_func(params)

    @pytest.mark.asyncio
    class TestQueryParams:
        @pytest.fixture
        def params(self, mail_list):
            return {
                'org_id': mail_list.org_id,
                'name_query': mail_list.username
            }

        async def test_get_by_name_query(self, params, mail_list, returned_func):
            returned = await returned_func(params)
            assert [mail_list] == returned

        async def test_random_query(self, returned_func, params, rands):
            params.update({'name_query': rands()})
            returned = await returned_func(params)
            assert returned == []
