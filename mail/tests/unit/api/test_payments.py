from datetime import datetime, timezone
from decimal import Decimal
from itertools import product

import pytest

from mail.ohio.ohio.core.entities.order import NDS, Item, OrderData, OrderStatus, Refund, RefundStatus


class TestOrderPaymentsHandler:
    class TestPost:
        @pytest.fixture(autouse=True)
        def action(self, mock_action):
            from mail.ohio.ohio.core.actions.order.create_or_update import CreateOrUpdateFromPaymentsOrderAction
            return mock_action(CreateOrUpdateFromPaymentsOrderAction)

        @pytest.fixture
        def json(self, randn, rands):
            return {
                'payments_service_id': randn(),
                'trust_purchase_token': rands(),
                'merchant_uid': randn(),
                'service_merchant_id': randn(),
                'payments_order_id': randn(),
                'status': 'paid',
                'order_data': {
                    'total': '100.77',
                    'currency': rands(),
                    'refunds': [
                        {
                            'trust_refund_id': rands(),
                            'refund_status': status.value,
                            'total': '12.34',
                            'currency': rands(),
                            'items': [
                                {
                                    'name': rands(),
                                    'price': '100.77',
                                    'currency': rands(),
                                    'nds': 'nds_20',
                                    'amount': '1.0',
                                }
                            ],
                        }
                        for status in RefundStatus
                    ],
                    'items': [
                        {
                            'name': rands(),
                            'price': '100.77',
                            'currency': rands(),
                            'nds': nds.value,
                            'amount': '1.0',
                            'image_path': image['path'],
                            'image_url': image['url'],
                        }
                        for nds, image in product(
                            NDS,
                            (
                                {'path': None, 'url': None},
                                {'url': 'http://image.test', 'path': 'pa/th'},
                            ),
                        )
                    ],
                    'description': rands(),
                },
                'service_data': {rands(): rands()},
                'revision': randn(),
                'created': datetime(2020, 7, 7, 10, 45, tzinfo=timezone.utc).isoformat(),
                'subservice_id': rands(),
            }

        @pytest.fixture
        def response_func(self, app, customer, json):
            async def _inner():
                return await app.post(f'/v1/internal/customer/{customer.customer_uid}/orders', json=json)

            return _inner

        def _make_item(self, data):
            return Item(
                name=data['name'],
                nds=NDS(data['nds']),
                price=Decimal(data['price']),
                currency=data['currency'],
                amount=Decimal(data['amount']),
                image_path=data.get('image_path'),
                image_url=data.get('image_url'),
            )

        @pytest.mark.asyncio
        @pytest.mark.parametrize('modifier', (
            pytest.param(lambda d: d, id='default'),
            pytest.param(lambda d: d.pop('subservice_id'), id='no subservice_id'),
        ))
        async def test_post__params(self, customer, action, json, modifier, response_func):
            modifier(json)
            await response_func()
            json['status'] = OrderStatus(json['status'])
            order_data = json['order_data']
            json['order_data'] = OrderData(
                total=Decimal(order_data['total']),
                currency=order_data['currency'],
                description=order_data['description'],
                items=[self._make_item(item) for item in order_data['items']],
                refunds=[
                    Refund(
                        trust_refund_id=refund['trust_refund_id'],
                        refund_status=RefundStatus(refund['refund_status']),
                        total=Decimal(refund['total']),
                        currency=refund['currency'],
                        items=[self._make_item(item) for item in refund['items']]
                    )
                    for refund in order_data['refunds']
                ]
            )
            json['created'] = datetime.fromisoformat(json['created'])
            action.assert_called_once_with(customer_uid=customer.customer_uid, **json)

        @pytest.mark.asyncio
        @pytest.mark.parametrize('modifier', (
            *[
                pytest.param(lambda d: d.pop(field), id=f'no {field}')
                for field in (
                    'payments_service_id',
                    'trust_purchase_token',
                    'merchant_uid',
                    'service_merchant_id',
                    'payments_order_id',
                    'status',
                    'order_data',
                    'service_data',
                    'revision',
                    'created',
                )
            ],
        ))
        async def test_post__required_params(self, json, modifier, response_func):
            modifier(json)
            response = await response_func()
            assert response.status == 400
