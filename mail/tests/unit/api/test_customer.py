from datetime import datetime, timezone

import pytest

from mail.ohio.ohio.core.entities.order_service import OrderService


@pytest.fixture(autouse=True)
def check_user_ticket_mock(mocker):
    mocker.patch(
        'mail.ohio.ohio.api.handlers.customer.BaseCustomerHandler._check_user_ticket',
        mocker.Mock(),
    )


class TestCustomerOrdersHandler:
    @pytest.mark.asyncio
    class TestGet:
        @pytest.fixture
        async def orders(self, create_order):
            return [await create_order() for _ in range(2)]

        @pytest.fixture
        def next_(self, randn):
            return {
                'created_keyset': datetime(2020, 6, 29, 13, 15, tzinfo=timezone.utc),
                'order_id_keyset': randn(),
            }

        @pytest.fixture(autouse=True)
        def action(self, mock_action, orders, next_):
            from mail.ohio.ohio.core.actions.order.get_for_customer import GetForCustomerOrderAction
            return mock_action(
                GetForCustomerOrderAction,
                (orders, next_),
            )

        @pytest.fixture
        def params(self):
            return {}

        @pytest.fixture
        async def response(self, app, customer, params):
            return await app.get(f'/v1/customer/{customer.customer_uid}/orders', params=params)

        @pytest.mark.parametrize('params,kwargs', (
            ({}, {'limit': 100}),
            (
                {
                    'limit': 1,
                    'service_ids': [2],
                    'subservice_ids': ['3'],
                    'created_keyset': datetime(2020, 6, 29, 13, 55, tzinfo=timezone.utc).isoformat(),
                    'order_id_keyset': 4,
                },
                {
                    'limit': 1,
                    'service_ids': [2],
                    'subservice_ids': ['3'],
                    'created_keyset': datetime(2020, 6, 29, 13, 55, tzinfo=timezone.utc),
                    'order_id_keyset': 4,
                },
            )
        ))
        async def test_get__params(self, customer, kwargs, action, response):
            action.assert_called_once_with(customer_uid=customer.customer_uid, **kwargs)

        @pytest.mark.parametrize('params', (
            pytest.param({'created_keyset': datetime.now().isoformat()}, id='missing order_id_keyset'),
            pytest.param({'order_id_keyset': 1}, id='missing created_keyset'),
            pytest.param({'limit': 101}, id='limit is too high'),
        ))
        async def test_get__bad_request(self, response):
            assert response.status == 400

        @pytest.mark.parametrize('orders,next_', (
            ([], {}),
        ))
        async def test_get__empty_response(self, response):
            assert {
                'status': 'success',
                'code': 200,
                'data': {
                    'orders': [],
                    'next': {},
                },
            } == await response.json()

        async def test_get__response(self, orders, next_, response):
            assert {
                'status': 'success',
                'code': 200,
                'data': {
                    'orders': [
                        {
                            'customer_uid': order.customer_uid,
                            'service_id': order.service_id,
                            'order_id': order.order_id,
                            'subservice_id': order.subservice_id,

                            'merchant_uid': order.merchant_uid,
                            'service_merchant_id': order.service_merchant_id,
                            'payments_order_id': order.payments_order_id,

                            'trust_payment_id': order.trust_payment_id,
                            'trust_purchase_token': order.trust_purchase_token,

                            'status': order.status.value,
                            'order_revision': order.order_revision,
                            'service_data': order.service_data,
                            'service_revision': order.service_revision,

                            'created': order.created.astimezone(timezone.utc).isoformat(),
                            'updated': order.updated.astimezone(timezone.utc).isoformat(),

                            'total': str(order.order_data.total),
                            'currency': order.order_data.currency,
                            'items': [
                                {
                                    'amount': str(item.amount),
                                    'price': str(item.price),
                                    'currency': item.currency,
                                    'nds': item.nds.value,
                                    'name': item.name,
                                    'image_path': item.image_path,
                                }
                                for item in order.order_data.items
                            ],
                            'description': order.order_data.description,
                            'refunds': [
                                {
                                    'trust_refund_id': refund.trust_refund_id,
                                    'refund_status': refund.refund_status.value,
                                    'total': str(refund.total),
                                    'currency': refund.currency,
                                    'items': [
                                        {
                                            'amount': str(item.amount),
                                            'price': str(item.price),
                                            'currency': item.currency,
                                            'nds': item.nds.value,
                                            'name': item.name,
                                        }
                                        for item in refund.items
                                    ]
                                }
                                for refund in order.order_data.refunds
                            ],
                        }
                        for order in orders
                    ],
                    'next': {
                        'created_keyset': next_['created_keyset'].astimezone(timezone.utc).isoformat(),
                        'order_id_keyset': next_['order_id_keyset'],
                    }
                }
            } == await response.json()


class TestCustomerOrderByIdHandler:
    class TestGet:
        @pytest.fixture(autouse=True)
        def action(self, mock_action, order):
            from mail.ohio.ohio.core.actions.order.get_by_id import GetByIdOrderAction
            return mock_action(GetByIdOrderAction, order)

        @pytest.fixture
        async def response(self, app, order):
            return await app.get(f'/v1/customer/{order.customer_uid}/orders/{order.order_id}')

        def test_get__params(self, order, action, response):
            action.assert_called_once_with(customer_uid=order.customer_uid, order_id=order.order_id)


class TestCustomerServicesHandler:
    class TestGet:
        @pytest.fixture
        def order_services(self, randn, rands):
            return [
                OrderService(service_id=randn(), subservice_id=rands()),
                OrderService(service_id=randn(), subservice_id=rands()),
                OrderService(service_id=randn(), subservice_id=None),
            ]

        @pytest.fixture(autouse=True)
        def action(self, mock_action, order_services):
            from mail.ohio.ohio.core.actions.order.get_services import GetServicesOrderAction
            return mock_action(GetServicesOrderAction, order_services)

        @pytest.fixture
        async def response(self, app, customer):
            return await app.get(f'/v1/customer/{customer.customer_uid}/services')

        def test_get__params(self, customer, action, response):
            action.assert_called_once_with(customer_uid=customer.customer_uid)

        @pytest.mark.asyncio
        async def test_get__response(self, order_services, response):
            assert {
                'status': 'success',
                'code': 200,
                'data': {
                    'services': [
                        {
                            'service_id': obj.service_id,
                            'subservice_id': obj.subservice_id,
                        }
                        for obj in order_services
                    ],
                },
            } == await response.json()
