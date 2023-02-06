import pytest

from sendr_utils import anext

from hamcrest import assert_that, has_entries, has_properties

from mail.payments.payments.core.entities.enums import CommonDataType


class TestCommonDataHandler:
    @pytest.fixture
    def payload(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def data_type(self, randitem):
        return randitem(CommonDataType)

    @pytest.fixture
    async def response_json(self, client, payload, data_type):
        r = await client.post('/v1/data', json={
            'data_type': data_type.value,
            'payload': payload,
        })
        assert r.status == 200
        return await r.json()

    def test_response(self, response_json):
        assert_that(
            response_json,
            has_entries({
                'status': 'success',
                'code': 200,
                'data': {}
            })
        )

    @pytest.mark.asyncio
    async def test_result(self, data_type, payload, response_json, storage):
        assert_that(
            await anext(storage.common_data.find(order=('-common_data_id',), limit=1)),
            has_properties({
                'payload': payload,
                'data_type': data_type
            })
        )
