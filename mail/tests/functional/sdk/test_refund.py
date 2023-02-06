from datetime import timezone

import pytest

from hamcrest import assert_that, contains, contains_inanyorder, has_entries

from mail.payments.payments.core.entities.enums import PayStatus
from mail.payments.payments.tests.base import BaseSdkRefundTest


class TestRefundCreate(BaseSdkRefundTest):
    def test_response(self, refund, refund_response):
        assert_that(
            refund_response['data'],
            has_entries({
                'meta': refund.data.meta,
                'refund_status': refund.refund_status.value,
                'price': float(round(refund.price, 2)),
                'updated': refund.updated.astimezone(timezone.utc).isoformat(),
                'revision': refund.revision,
                'active': refund.active,
                'refund_id': refund.order_id,
                'order_id': refund.original_order_id,
                'description': refund.description,
                'currency': refund.currency,
                'created': refund.created.astimezone(timezone.utc).isoformat(),
                'caption': refund.caption,
                'items': contains_inanyorder(*[
                    {
                        'amount': float(round(item.amount, 2)),  # type: ignore
                        'currency': item.currency,
                        'name': item.name,
                        'nds': item.nds.value,
                        'price': float(round(item.price, 2)),  # type: ignore
                        'product_id': item.product_id,
                        'markup': item.markup,
                    }
                    for item in refund.items
                ])
            }))

    @pytest.mark.asyncio
    async def test_new_item(self, make_refund_response, refund_data):
        refund_data['items'][0]['name'] = 'some new order'
        refund_response = await make_refund_response()
        assert refund_response['data']['message'] == 'ORDER_ITEM_NOT_PRESENT_IN_ORIGINAL_ORDER'

    @pytest.mark.asyncio
    async def test_order_not_paid(self, storage, make_refund_response, order):
        order.pay_status = PayStatus.NEW
        await storage.order.save(order)
        refund_response = await make_refund_response()
        assert refund_response['data']['message'] == 'ORDER_ORIGINAL_ORDER_MUST_BE_PAID'

    @pytest.mark.asyncio
    async def test_request_already_refunded(self, make_refund_response):
        for _ in range(2):
            refund_response = await make_refund_response()
        assert refund_response['data']['message'] == 'ORDER_REQUESTED_ITEM_AMOUNT_EXCEEDS_PAID'


class TestRefundGet(BaseSdkRefundTest):
    @pytest.mark.asyncio
    async def test_response(self, sdk_client, order, refund, refund_response):
        r = await sdk_client.get(f'/v1/order/{order.order_id}/refund/{refund.order_id}')

        assert_that(
            (r.status, await r.json()),
            contains(200, has_entries({'data': has_entries({
                'refund_status': refund.refund_status.value,
                'price': float(round(refund.price, 2)),
                'updated': refund.updated.astimezone(timezone.utc).isoformat(),
                'revision': refund.revision,
                'active': refund.active,
                'refund_id': refund.order_id,
                'order_id': refund.original_order_id,
                'description': refund.description,
                'currency': refund.currency,
                'created': refund.created.astimezone(timezone.utc).isoformat(),
                'caption': refund.caption,
                'items': contains_inanyorder(*[
                    {
                        'amount': float(round(item.amount, 2)),  # type: ignore
                        'currency': item.currency,
                        'name': item.name,
                        'nds': item.nds.value,
                        'price': float(round(item.price, 2)),  # type: ignore
                        'product_id': item.product_id,
                        'markup': item.markup,
                    }
                    for item in refund.items
                ])
            })}))
        )

    @pytest.mark.parametrize('reset', ('order_id', 'refund_id'))
    @pytest.mark.asyncio
    async def test_not_found(self, sdk_client, order, refund, refund_response, reset):
        params = {'order_id': order.order_id, 'refund_id': refund.order_id}
        params[reset] = 0

        r = await sdk_client.get(f'/v1/order/{params["order_id"]}/refund/{params["refund_id"]}')

        assert_that(
            (r.status, await r.json()),
            contains(404, has_entries({
                'data': has_entries({'message': 'ORDER_NOT_FOUND'})
            }))
        )
