import pytest

from hamcrest import assert_that, has_entries

from mail.beagle.beagle.core.entities.mail_list import MailListDescription
from mail.beagle.beagle.storage.exceptions import MailListNotFound


class TestMailListListHandler:
    @pytest.fixture(autouse=True)
    def setup(self, mock_passport, mock_directory, mock_blackbox, rands, randn):
        mock_directory('/v11/domains/', [{'name': rands(), 'master': True}])
        mock_passport('/1/bundle/account/register/pdd/', {'status': 'ok', 'uid': randn()})
        mock_blackbox({'users': []})

    @pytest.mark.asyncio
    class TestGet:
        async def test_get(self, app, mail_list):
            r = await app.get(f'/api/v1/mail_list/{mail_list.org_id}')
            assert all((r.status == 200, len((await r.json())['data']) == 1))

        async def test_get_empty(self, app, org):
            r = await app.get(f'/api/v1/mail_list/{org.org_id}')
            assert all((r.status == 200, len((await r.json())['data']) == 0))

        @pytest.mark.asyncio
        class TestQueryParams:
            @pytest.fixture
            def params(self, rands):
                return {'name_query': rands()}

            async def test_random_name_query(self, app, mail_list, params):
                r = await app.get(f'/api/v1/mail_list/{mail_list.org_id}', params=params)
                assert all((r.status == 200, len((await r.json())['data']) == 0))

            async def test_name_query(self, app, mail_list, params):
                params.update({'name_query': mail_list.username})
                r = await app.get(f'/api/v1/mail_list/{mail_list.org_id}', params=params)
                assert all((r.status == 200, len((await r.json())['data']) == 1))

    @pytest.mark.asyncio
    class TestPost:
        @pytest.fixture
        def data(self, randn, rands):
            return {
                'username': rands(),
                'description': {
                    'ru': rands(),
                    'en': rands(),
                },
            }

        async def test_post(self, app, org, data):
            r = await app.post(f'/api/v1/mail_list/{org.org_id}', json=data)
            r_data = (await r.json())['data']
            assert_that(r_data, has_entries(data))


@pytest.mark.asyncio
class TestMailListHandler:
    async def test_get(self, app, mail_list):
        r = await app.get(f'/api/v1/mail_list/{mail_list.org_id}/{mail_list.mail_list_id}')
        assert r.status == 200

    async def test_get_not_found(self, app, rands):
        r = await app.get(f'/api/v1/mail_list/{rands()}/{rands()}')
        assert r.status == 404

    async def test_put(self, app, storage, mail_list, rands):
        description = {
            'ru': rands(),
            'en': rands(),
        }

        r = await app.put(f'/api/v1/mail_list/{mail_list.org_id}/{mail_list.mail_list_id}',
                          json={'description': description})
        mail_list = await storage.mail_list.get(mail_list.org_id, mail_list.mail_list_id)

        assert all((r.status == 200, mail_list.description == MailListDescription(**description)))

    async def test_delete(self, app, storage, mail_list):
        mail_list = await storage.mail_list.get(mail_list.org_id, mail_list.mail_list_id)
        r = await app.delete(f'/api/v1/mail_list/{mail_list.org_id}/{mail_list.mail_list_id}')

        with pytest.raises(MailListNotFound):
            assert r.status == 200
            await storage.mail_list.get(mail_list.org_id, mail_list.mail_list_id)
