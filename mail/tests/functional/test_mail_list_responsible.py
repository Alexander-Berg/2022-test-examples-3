import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.beagle.beagle.utils.helpers import date_to_str


@pytest.fixture
def url(org_id, org, mail_list_id):
    return f'/api/v1/mail_list/{org_id}/{mail_list_id}/responsible'


@pytest.mark.asyncio
class TestGetMailListResponsible:
    @pytest.fixture
    async def response_json(self, app, url, mail_list_responsibles):
        r = await app.get(url)
        assert r.status == 200
        return await r.json()

    async def test_returned(self, mail_list_responsibles, response_json):
        assert_that(
            response_json['data'],
            contains_inanyorder(*[
                has_entries({
                    'uid': responsible.uid,
                    'mail_list_id': responsible.mail_list_id,
                    'org_id': responsible.org_id,
                    'created': date_to_str(responsible.created),
                    'updated': date_to_str(responsible.updated),
                })
                for responsible in mail_list_responsibles
            ])
        )

    @pytest.mark.asyncio
    class TestNotFound:
        @pytest.fixture
        def mail_list_id(self, randn):
            return randn()

        async def test_mail_list_not_found(self, app, mail_list_id, url):
            r = await app.get(url)
            assert r.status == 404


@pytest.mark.asyncio
class TestPostMailListResponsible:
    @pytest.fixture
    def params(self, user):
        return {'uid': user.uid}

    async def test_success(self, app, params, url):
        r = await app.post(url, json=params)
        assert r.status == 200

    class TestNotFound:
        class TestMailNotFound:
            @pytest.fixture
            def mail_list_id(self, randn):
                return randn()

            @pytest.mark.asyncio
            async def test_mail_list_not_found(self, app, params, mail_list_id, url):
                r = await app.post(url, json=params)
                assert r.status == 404

        @pytest.mark.asyncio
        async def test_user_not_found(self, app, params, url, randn):
            rand_uid = randn()
            params.update({'uid': rand_uid})
            r = await app.post(url, json=params)
            assert r.status == 404

    @pytest.mark.asyncio
    class TestAlreadyExists:
        @pytest.fixture
        def params(self, mail_list_responsible):
            return {'uid': mail_list_responsible.uid}

        @pytest.fixture
        def mail_list_id(self, mail_list_responsible):
            return mail_list_responsible.mail_list_id

        async def test_already_exists(self, app, params, url):
            r = await app.post(url, json=params)
            assert r.status == 409


@pytest.mark.asyncio
class TestDeleteMailListResponsible:
    @pytest.fixture
    def params(self, mail_list_responsible):
        return {'uid': mail_list_responsible.uid}

    @pytest.fixture
    def mail_list_id(self, mail_list_responsible):
        return mail_list_responsible.mail_list_id

    async def test_success(self, app, params, mail_list_id, url):
        r = await app.delete(url, json=params)
        assert r.status == 200

    class TestNotFound:
        class TestMailNotFound:
            @pytest.fixture
            def mail_list_id(self, randn):
                return randn()

            @pytest.mark.asyncio
            async def test_mail_list_not_found(self, app, params, mail_list_id, url):
                r = await app.delete(url, json=params)
                assert r.status == 404

        @pytest.mark.asyncio
        async def test_responsible_not_found(self, app, storage, mail_list_responsible, mail_list_id, params, url):
            await storage.mail_list_responsible.delete(mail_list_responsible)
            r = await app.delete(url, json=params)
            assert r.status == 404
