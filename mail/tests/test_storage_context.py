from asyncio import sleep
from datetime import timedelta

import psycopg2
import pytest
from db import Merchant


@pytest.mark.asyncio
async def test_timeout_ok(aiopg_engine, randn, rands, storage_context, timeout):
    async with storage_context as storage:
        merchant = await storage.merchant.create(Merchant(uid=randn(), name=rands()))
        await storage.merchant.get(merchant.uid)


@pytest.mark.asyncio
async def test_timeout_exceeded(aiopg_engine, db_conn, randn, rands, storage_context, timeout):
    storage = await storage_context.__aenter__()
    merchant = await storage.merchant.create(Merchant(uid=randn(), name=rands()))

    delay = timedelta(seconds=0.5)
    await sleep((timeout + delay).total_seconds())

    with pytest.raises(psycopg2.Error):
        await storage.merchant.get(merchant.uid)

    with pytest.raises(psycopg2.Error):
        await storage_context.__aexit__(None, None, None)


class TestNoTimeoutOK:
    @pytest.fixture
    def transaction_timeout(self):
        return None

    @pytest.mark.asyncio
    async def test_no_timeout_ok(aiopg_engine, randn, rands, storage_context, timeout):
        async with storage_context as storage:
            merchant = await storage.merchant.create(Merchant(uid=randn(), name=rands()))
            delay = timedelta(seconds=0.5)
            await sleep((timeout + delay).total_seconds())
            await storage.merchant.get(merchant.uid)
