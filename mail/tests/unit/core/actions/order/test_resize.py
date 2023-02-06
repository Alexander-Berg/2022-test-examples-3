from decimal import Decimal

import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, has_entries, has_properties

from mail.payments.payments.core.actions.order.resize import ResizeOrderAction
from mail.payments.payments.core.entities.enums import OperationKind
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.tests.base import BaseAcquirerTest, BaseOrderAcquirerTest


class TestResizeOrderAction(BaseAcquirerTest, BaseOrderAcquirerTest):
    @pytest.fixture(autouse=True)
    def get_acquirer_mock(self, mock_action, acquirer):
        from mail.payments.payments.core.actions.merchant.get_acquirer import GetAcquirerMerchantAction
        return mock_action(GetAcquirerMerchantAction, acquirer)

    @pytest.fixture(autouse=True)
    def trust_resize_mock(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'payment_resize', {'status': 'success'}) as mock:
            yield mock

    @pytest.fixture
    def validated_item(self, order, items):
        return Item(
            uid=order.uid,
            order_id=order.order_id,
            amount=items[0].amount - Decimal('1.0'),
            product_id=items[0].product_id,
            new_price=items[0].price - Decimal('1.0')
        )

    @pytest.fixture
    def validated_items(self, validated_item):
        return [validated_item]

    @pytest.fixture
    def params(self, transaction, order, validated_items):
        return {
            'transaction': transaction,
            'order': order,
            'items': validated_items
        }

    @pytest.fixture
    def action(self):
        return ResizeOrderAction

    @pytest.fixture
    def returned_func(self, action, params):
        async def _inner():
            return await action(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    def test_resize(self, trust_resize_mock, order, validated_item, transaction, returned, order_acquirer):
        trust_resize_mock.assert_called_once_with(
            uid=transaction.uid,
            acquirer=order_acquirer,
            purchase_token=transaction.trust_purchase_token,
            order=order,
            item=validated_item
        )

    @pytest.mark.asyncio
    async def test_changelog(self, order, validated_item, storage, returned):
        assert_that(
            await alist(storage.change_log.find(order.uid)),
            contains_inanyorder(
                has_properties({
                    'uid': order.uid,
                    'revision': order.revision,
                    'operation': OperationKind.UPDATE_ORDER,
                    'arguments': has_entries({
                        'items': contains_inanyorder(
                            has_entries({
                                'uid': order.uid,
                                'amount': float(validated_item.amount),
                                'order_id': validated_item.order_id,
                                'new_price': float(validated_item.new_price),
                                'product_id': validated_item.product_id
                            })
                        )
                    })
                })
            )
        )
