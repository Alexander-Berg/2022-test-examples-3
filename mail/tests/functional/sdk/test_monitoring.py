import pytest


class BaseTestMonitoring:
    @pytest.fixture
    def path(self):
        raise NotImplementedError

    @pytest.fixture
    def headers(self):
        return {}

    @pytest.fixture
    async def response(self, sdk_client, path, headers):
        return await sdk_client.get(path, headers=headers)

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_response_format(self, response):
        assert response.status == 200

    class TestInternal:
        @pytest.fixture
        def headers(self):
            return {'X-External-Request': 'true'}

        def test_response_format_internal(self, response):
            assert response.status == 404


class TestPing(BaseTestMonitoring):
    @pytest.fixture
    def path(self):
        return '/ping'

    class TestInternal:
        pass


class TestPingDB(BaseTestMonitoring):
    @pytest.fixture
    def path(self):
        return '/pingdb'


class TestUnistat(BaseTestMonitoring):
    @pytest.fixture
    def path(self):
        return '/unistat'
