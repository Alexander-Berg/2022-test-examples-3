import pytest

from sendr_utils import alist


@pytest.fixture
async def order(moderation, no_merchant_user_check, client, storage, merchant, order_data):
    with no_merchant_user_check():
        r = await client.post(f'/v1/order/{merchant.uid}/', json=order_data)
    assert r.status == 200
    order_id = (await r.json())['data']['order_id']
    order = await storage.order.get(uid=merchant.uid, order_id=order_id)
    order.items = await alist(storage.item.get_for_order(merchant.uid, order_id))
    return order
