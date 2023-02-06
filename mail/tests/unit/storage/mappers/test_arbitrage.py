from dataclasses import asdict

import pytest

from sendr_utils import alist, utcnow

from mail.payments.payments.core.entities.arbitrage import Arbitrage, ArbitrageStatus
from mail.payments.payments.storage.mappers.arbitrage import ArbitrageDataDumper, ArbitrageDataMapper


class TestArbitrageDataMapper:
    def test_map(self, arbitrage):
        row = {
            type(arbitrage).__name__ + '__' + key: value
            for key, value in asdict(arbitrage).items()
        }
        mapped = ArbitrageDataMapper()(row)
        assert mapped == arbitrage


class TestArbitrageDataDumper:
    def test_unmap(self, arbitrage):
        assert ArbitrageDataDumper()(arbitrage) == asdict(arbitrage)


class TestArbitrageMapper:
    @pytest.fixture
    def now(self, mocker):
        now = utcnow()
        mocker.patch('mail.payments.payments.storage.mappers.arbitrage.func.now', mocker.Mock(return_value=now))
        return now

    @pytest.mark.asyncio
    async def test_get(self, arbitrage, storage):
        from_db = await storage.arbitrage.get(arbitrage.arbitrage_id)
        assert from_db == arbitrage

    @pytest.mark.asyncio
    async def test_get_by_escalate_id(self, arbitrage, storage):
        from_db = await storage.arbitrage.get_by_escalate_id(arbitrage.escalate_id)
        assert from_db == arbitrage

    @pytest.mark.parametrize('field', ('chat_id', 'arbiter_chat_id', 'escalate_id', 'status'))
    @pytest.mark.asyncio
    async def test_find(self, field, arbitrage, storage):
        from_db = await alist(storage.arbitrage.find(filters={field: getattr(arbitrage, field)}))
        assert from_db == [arbitrage]

    @pytest.mark.parametrize('field', ('chat_id', 'arbiter_chat_id', 'escalate_id', 'status'))
    @pytest.mark.asyncio
    async def test_save(self, field, arbitrage, storage, rands, randitem, now):
        setattr(arbitrage, field, randitem(ArbitrageStatus) if field == 'status' else rands())
        await storage.arbitrage.save(arbitrage)
        from_db = await storage.arbitrage.get(arbitrage.arbitrage_id)
        assert from_db == arbitrage

    @pytest.mark.asyncio
    async def test_ignore_created_during_save(self, arbitrage, storage):
        arbitrage.created = utcnow()
        await storage.arbitrage.save(arbitrage)
        from_db = await storage.arbitrage.get(arbitrage.arbitrage_id)
        assert from_db.created != arbitrage.created

    @pytest.mark.asyncio
    async def test_get_current(self, storage, arbitrage):
        returned = await storage.arbitrage.get_current(arbitrage.uid, arbitrage.order_id)
        assert arbitrage == returned

    @pytest.mark.asyncio
    async def test_get_by_refund_id(self, refund, storage, arbitrage):
        returned = await storage.arbitrage.get_by_refund_id(arbitrage.uid, arbitrage.refund_id)
        assert arbitrage == returned

    @pytest.mark.asyncio
    async def test_no_get_current_via_status(self, storage, arbitrage):
        arbitrage.status = ArbitrageStatus.COMPLETE
        arbitrage = await storage.arbitrage.save(arbitrage)
        assert await storage.arbitrage.get_current(arbitrage.uid, arbitrage.order_id) is None

    @pytest.mark.asyncio
    async def test_no_get_current_via_absent(self, storage, order):
        assert await storage.arbitrage.get_current(order.uid, order.order_id) is None

    @pytest.mark.asyncio
    async def test_delete(self, storage, arbitrage):
        await storage.arbitrage.get(arbitrage.arbitrage_id)
        await storage.arbitrage.delete(arbitrage)

        with pytest.raises(Arbitrage.DoesNotExist):
            await storage.arbitrage.get(arbitrage.arbitrage_id)
