import pytest

from mail.payments.payments.core.entities.order import Order


@pytest.fixture
def order_entity(merchant, randn):
    return Order(
        uid=merchant.uid,
        shop_id=randn(),
    )
