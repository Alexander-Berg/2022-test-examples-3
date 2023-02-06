import pytest
from db import AnotherMerchant, Merchant


@pytest.mark.asyncio
async def test_merchant_create(storage, merchant):
    assert await storage.merchant.get(merchant.uid) == merchant


@pytest.mark.asyncio
class TestGet:
    async def test_get(self, storage, merchant):
        assert await storage.merchant.get(merchant.uid) == merchant

    async def test_not_found(self, storage, randn):
        with pytest.raises(Merchant.DoesNotExist):
            await storage.merchant.get(randn())

    async def test_not_exception(self, storage, randn):
        try:
            await storage.merchant.get(randn())
        except AnotherMerchant.DoesNotExist:
            raise Exception('Invalid exception raised')
        except Merchant.DoesNotExist:
            pass
