import pytest


class BaseGeobaseTest:
    @pytest.fixture(autouse=True)
    def spy_get(self, geobase_client, mocker):
        mocker.spy(geobase_client, 'get')
        return geobase_client

    @pytest.fixture
    def region_id(self, randn):
        return randn()

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()


class TestGetRegionIdByIp(BaseGeobaseTest):
    @pytest.fixture
    def user_ip(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def response_json(self, region_id):
        return {'region_id': region_id}

    @pytest.fixture
    def returned_func(self, geobase_client, user_ip):
        async def _inner():
            return await geobase_client.get_region_id_by_ip(user_ip)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_get(self, geobase_client, user_ip):
        geobase_client.get.assert_called_with(
            interaction_method='get_region_id_by_ip',
            url=f'{geobase_client.BASE_URL}/v1/get_traits_by_ip',
            params={"ip": user_ip},
        )

    @pytest.mark.asyncio
    async def test_returned(self, region_id, returned, response_json):
        assert region_id == returned


class TestGetRegion(BaseGeobaseTest):
    @pytest.fixture(autouse=True)
    def response_json(self, region_id):
        return {
            "id": region_id,
            "type": 6,
            "parent_id": 1,
            "geo_parent_id": 0,
            "capital_id": 0,
            "name": "Москва",
            "native_name": "",
            "iso_name": "RU MOW",
            "is_main": True,
            "en_name": "Moscow",
            "short_en_name": "MSK",
            "phone_code": "495 499",
            "zip_code": "",
            "position": 0,
            "population": 12506468,
            "synonyms": "Moskau, Moskva",
            "latitude": 55.753215,
            "longitude": 37.622504,
            "latitude_size": 0.878654,
            "latitiude_size": 0.878654,
            "longitude_size": 1.164423,
            "zoom": 10,
            "tzname": "Europe/Moscow",
            "official_languages": "ru",
            "widespread_languages": "ru",
            "services": [
                "bs",
                "yaca",
                "weather",
                "afisha",
                "maps",
                "tv",
                "ad",
                "etrain",
                "subway",
                "delivery",
                "route"
            ]
        }

    @pytest.fixture
    def returned_func(self, geobase_client, region_id):
        async def _inner():
            return await geobase_client.get_region(region_id)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_get(self, geobase_client, region_id):
        geobase_client.get.assert_called_with(
            interaction_method='get_region',
            url=f'{geobase_client.BASE_URL}/v1/region_by_id',
            params={"id": region_id},
        )

    @pytest.mark.asyncio
    async def test_returned(self, region_id, returned, response_json):
        assert response_json == returned


class TestIsIn(BaseGeobaseTest):
    @pytest.fixture
    def in_region_id(self, randn):
        return randn()

    @pytest.fixture(params=(True, False))
    def result(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def response_json(self, result):
        return result

    @pytest.fixture
    def returned_func(self, geobase_client, region_id, in_region_id):
        async def _inner():
            return await geobase_client.is_in(region_id, in_region_id)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_get(self, geobase_client, region_id, in_region_id):
        geobase_client.get.assert_called_with(
            interaction_method='in',
            url=f'{geobase_client.BASE_URL}/v1/in',
            params={'id': region_id, 'pid': in_region_id},
        )

    @pytest.mark.asyncio
    async def test_returned(self, result, returned):
        assert result == returned


class TestParents(BaseGeobaseTest):
    @pytest.fixture
    def result(self, region_id, randn):
        result = set()
        while len(result) < 5:
            parent_region_id = randn()
            if parent_region_id not in result:
                result.add(parent_region_id)
        return list(result)

    @pytest.fixture(autouse=True)
    def response_json(self, result):
        return result

    @pytest.fixture
    def returned_func(self, geobase_client, region_id):
        async def _inner():
            return await geobase_client.get_parents(region_id)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_get(self, geobase_client, region_id):
        geobase_client.get.assert_called_with(
            interaction_method='parents',
            url=f'{geobase_client.BASE_URL}/v1/parents',
            params={'id': region_id},
        )

    @pytest.mark.asyncio
    async def test_returned(self, result, returned):
        assert result == returned
