from decimal import Decimal

import pytest

from mail.payments.payments.core.entities.enums import NDS, OrderSource
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.core.entities.order import Order, OrderData, OriginalOrderInfo
from mail.payments.payments.core.entities.product import Product


@pytest.fixture
def parent_order_id():
    return None


@pytest.fixture(params=(
    pytest.param(True, id='exclude'),
    pytest.param(False, id='include'),
))
def exclude_stats(request):
    return request.param


@pytest.fixture
def order_entity(merchant, randn, shop, parent_order_id, service_merchant, service_client, test_param):
    return Order(
        uid=merchant.uid,
        shop_id=shop.shop_id,
        parent_order_id=parent_order_id,
        service_client_id=service_client.service_client_id,
        service_merchant_id=service_merchant.service_merchant_id,
        test=test_param,
        data=OrderData(trust_form_name='1', trust_template='1', multi_max_amount=10, multi_issued=0, version=2),
        customer_uid=randn(),
        created_by_source=OrderSource.SDK_API,
        pay_by_source=OrderSource.UI,
        paymethod_id='some_pay_method',
    )


@pytest.fixture
async def order(storage, shop, service_merchant, order_entity):
    order = await storage.order.create(order_entity)
    order.shop = shop
    order.service_merchant = service_merchant
    return order


@pytest.fixture
def original_order_id(order):
    return order.order_id


@pytest.fixture
def expected_order_info(order) -> OriginalOrderInfo:
    return OriginalOrderInfo(pay_status=order.pay_status, paymethod_id=order.paymethod_id)


@pytest.fixture
async def orders(storage, merchant, shop, randdecimal, orders_data):
    created = []
    for order_data in orders_data:
        order_data.setdefault('uid', merchant.uid)
        order_data.setdefault('shop_id', shop.shop_id)
        price = order_data.pop('price', randdecimal(min=1, max=10))
        order = await storage.order.create(Order(**order_data))
        if 'original_order_id' in order_data and order_data['original_order_id'] is not None:
            original_order = await storage.order.get(uid=order.uid, order_id=order_data['original_order_id'])
            order.original_order_info = OriginalOrderInfo(pay_status=original_order.pay_status,
                                                          paymethod_id=original_order.paymethod_id)
        order.shop = shop
        created.append(order)
        product, _ = await storage.product.get_or_create(Product(
            uid=merchant.uid,
            name='product',
            price=price,
            nds=NDS.NDS_0,
        ))
        await storage.item.create(Item(
            uid=merchant.uid,
            order_id=order.order_id,
            product_id=product.product_id,
            amount=Decimal(1)
        ))

    return created
