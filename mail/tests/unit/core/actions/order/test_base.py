from decimal import Decimal

import pytest

from hamcrest import assert_that, contains_inanyorder

from mail.payments.payments.core.actions.order.base import BaseOrderAction
from mail.payments.payments.core.entities.enums import NDS
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.core.entities.product import Product
from mail.payments.payments.core.exceptions import (
    CoreFieldError, ItemsInvalidMarkupSumError, OrderHasDuplicateItemEntriesError, OrderPriceExceeds100kRUBError
)


@pytest.fixture
def action(storage):
    BaseOrderAction.context.storage = storage
    yield BaseOrderAction()
    BaseOrderAction.context.storage = None


@pytest.fixture
def item(items):
    return items[0]


class TestFetchItems:
    @pytest.mark.asyncio
    async def test_no_items(self, order, action):
        assert await action._fetch_items(order) == []

    @pytest.mark.asyncio
    async def test_fetch_items(self, storage, order, items, action):
        assert_that(
            await action._fetch_items(order),
            contains_inanyorder(*items)
        )


class TestValidateOrderItems:
    @pytest.fixture(autouse=True)
    def key_out_mock(self, mocker):
        return mocker.patch.object(
            Product,
            'key_out',
            property(lambda self: self.name)
        )

    @pytest.fixture
    def items(self, merchant):
        return [
            Item(
                uid=merchant.uid,
                order_id=None,
                product_id=None,
                amount=Decimal(1),
                product=Product(
                    uid=merchant.uid,
                    name='product 01',
                    price=Decimal(5),
                    nds=NDS.NDS_0,
                )
            ),
            Item(
                uid=merchant.uid,
                order_id=None,
                product_id=None,
                amount=Decimal(4),
                product=Product(
                    uid=merchant.uid,
                    name='product 02',
                    price=Decimal(5),
                    nds=NDS.NDS_0,
                ),
                markup={
                    'virtual::new_promocode': '8',
                    'card': '12',
                }
            ),
        ]

    def test_duplicates(self, action, items, merchant):
        items.append(items[0])
        with pytest.raises(OrderHasDuplicateItemEntriesError):
            action._validate_order_items(items, merchant)

    def test_total_price_greater_than_max_allowed(self, action, items, payments_settings, merchant):
        items[0].product.price = payments_settings.ORDER_MAX_TOTAL_PRICE + 1
        with pytest.raises(OrderPriceExceeds100kRUBError):
            action._validate_order_items(items, merchant)

    def test_total_price_greater_than_max_merchant_limit(self, action, items, merchant):
        merchant.options.order_max_total_price = 100
        items[0].product.price = 101

        with pytest.raises(OrderPriceExceeds100kRUBError):
            action._validate_order_items(items, merchant)

    def test_total_price_not_greater_than_max_merchant_limit(self, action, items, merchant):
        merchant.options.order_max_total_price = 30
        action._validate_order_items(items, merchant)

    def test_markup_contains_negative_value(self, action, items, merchant):
        items[0].markup = {
            'card': '-1.00'
        }
        with pytest.raises(CoreFieldError):
            action._validate_order_items(items, merchant)

    def test_validated(self, action, items, merchant):
        action._validate_order_items(items, merchant)

    def test_markup_contains_only_card(self, action, items, merchant):
        items[0].markup = {
            'card': '5',
        }
        action._validate_order_items(items, merchant)

    @pytest.mark.parametrize('markup,expected_error', [
        # Missing 'card'
        (
            {
                'virtual::new_promocode': '8',
            },
            CoreFieldError,
        ),
        # Unexpected field
        (
            {
                'virtual::new_promocode': '8',
                'card': '12',
                'unexpected_field': '153',
            },
            CoreFieldError,
        ),
        # Invalid number
        (
            {
                'virtual::new_promocode': 'abc',
                'card': '12',
            },
            CoreFieldError,
        ),
        # field is None
        (
            {
                'virtual::new_promocode': None,
                'card': '12',
            },
            CoreFieldError,
        ),
        # invalid precision
        (
            {
                'virtual::new_promocode': '8.005',
                'card': '11.995',
            },
            CoreFieldError,
        ),
        # Invalid sum
        (
            {
                'virtual::new_promocode': '1',
                'card': '12',
            },
            ItemsInvalidMarkupSumError,
        ),
    ])
    def test_markup(self, action, items, markup, expected_error, merchant):
        items[0].markup = markup
        with pytest.raises(expected_error):
            action._validate_order_items(items, merchant)
