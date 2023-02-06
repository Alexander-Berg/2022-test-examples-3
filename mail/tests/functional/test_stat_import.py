import pytest

from hamcrest import assert_that, has_entries, is_


class TestStatImport:
    @pytest.fixture(autouse=True)
    async def users(self, org_id, create_user):
        return [await create_user(org_id) for _ in range(3)]

    @pytest.fixture(autouse=True)
    async def collectors(self, users, create_collector):
        return [await create_collector(user_id=user.user_id) for user in users]

    @pytest.fixture
    async def response(self, app, org_id, users, collectors):
        return await app.get(f'/import/{org_id}/stat/')

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_response_status(self, response):
        assert response.status == 200

    def test_response_json(self, response_json):
        assert_that(
            response_json,
            has_entries({
                'data': has_entries({
                    'total': is_(int),
                    'errors': is_(int),
                    'finished': is_(int),
                }),
                'code': 200,
                'status': 'success',
            })
        )
