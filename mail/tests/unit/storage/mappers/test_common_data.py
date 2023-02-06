from dataclasses import asdict

import pytest

from sendr_utils import alist, utcnow

from mail.payments.payments.core.entities.common import CommonData
from mail.payments.payments.core.entities.enums import CommonDataType
from mail.payments.payments.storage.mappers.common_data import CommonDataDataDumper, CommonDataDataMapper


@pytest.fixture
async def common_data(storage, rands, randitem):
    return await storage.common_data.create(
        CommonData(
            data_type=randitem(CommonDataType),
            payload={rands(): rands()}
        )
    )


class TestCommonDataDataMapper:
    def test_map(self, common_data):
        row = {
            type(common_data).__name__ + '__' + key: value
            for key, value in asdict(common_data).items()
        }
        mapped = CommonDataDataMapper()(row)
        assert mapped == common_data


class TestCommonDataDataDumper:
    def test_unmap(self, common_data):
        assert CommonDataDataDumper()(common_data) == asdict(common_data)


class TestCommonDataMapper:
    @pytest.fixture
    def now(self, mocker):
        now = utcnow()
        mocker.patch('mail.payments.payments.storage.mappers.common_data.func.now', mocker.Mock(return_value=now))
        return now

    @pytest.mark.asyncio
    async def test_get(self, common_data, storage):
        from_db = await storage.common_data.get(common_data.common_data_id)
        assert from_db == common_data

    @pytest.mark.parametrize('field', ('common_data_id', 'data_type'))
    @pytest.mark.asyncio
    async def test_find(self, field, common_data, storage):
        from_db = await alist(storage.common_data.find(filters={field: getattr(common_data, field)}))
        assert from_db == [common_data]

    @pytest.mark.asyncio
    async def test_save(self, common_data, storage, rands, randitem, now):
        common_data.data_type = randitem(CommonDataType)
        common_data.payload = {rands(): rands()}
        await storage.common_data.save(common_data)
        from_db = await storage.common_data.get(common_data.common_data_id)
        assert from_db == common_data

    @pytest.mark.asyncio
    async def test_ignore_created_during_save(self, common_data, storage):
        common_data.created = utcnow()
        await storage.common_data.save(common_data)
        from_db = await storage.common_data.get(common_data.common_data_id)
        assert from_db.created != common_data.created

    @pytest.mark.asyncio
    async def test_delete(self, storage, common_data):
        await storage.common_data.get(common_data.common_data_id)
        await storage.common_data.delete(common_data)

        with pytest.raises(CommonData.DoesNotExist):
            await storage.common_data.get(common_data.common_data_id)
