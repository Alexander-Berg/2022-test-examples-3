import pytest

from mail.payments.payments.core.actions.common_data import CreateCommonDataAction
from mail.payments.payments.core.entities.enums import CommonDataType


class TestCreateCommonDataAction:
    @pytest.fixture
    def payload(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def data_type(self, randitem):
        return randitem(CommonDataType)

    @pytest.fixture
    def returned_func(self, payload, data_type):
        async def _inner():
            return await CreateCommonDataAction(data_type=data_type, payload=payload).run()

        return _inner

    @pytest.mark.asyncio
    async def test_returned(self, returned, storage):
        assert await storage.common_data.get(returned.common_data_id) == returned
