import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.serial import Serial
from mail.payments.payments.storage.db.tables import metadata


async def get_serials(storage, uid):
    result = await storage.conn.execute(f'''
        SELECT
            next_revision,
            next_order_id,
            next_tx_id,
            next_product_id
        FROM {metadata.schema}.serials
        WHERE uid = {uid}
        ;
    ''')
    return await result.first()


@pytest.mark.parametrize('column', [
    'next_revision',
    'next_order_id',
    'next_tx_id',
    'next_product_id',
])
@pytest.mark.asyncio
async def test_acquire_serial_increments(storage, merchant, column):
    before = await get_serials(storage, merchant.uid)
    await storage.order._acquire_serial(merchant.uid, column)
    after = await get_serials(storage, merchant.uid)
    assert_that(
        after,
        has_entries({
            **before,
            column: before[column] + 1,
        }),
    )


@pytest.mark.parametrize('column', [
    'next_revision',
    'next_order_id',
    'next_tx_id',
    'next_product_id',
])
@pytest.mark.asyncio
async def test_acquire_serial_returns(storage, merchant, column):
    before = await get_serials(storage, merchant.uid)
    value = await storage.order._acquire_serial(merchant.uid, column)
    assert value == before[column]


@pytest.mark.asyncio
async def test_get_serial(storage, merchant):
    assert Serial(uid=merchant.uid) == await storage.serial.get(merchant.uid)
