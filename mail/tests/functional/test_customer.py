from datetime import timezone

import pytest

from sendr_tvm.common import TicketCheckResult

from hamcrest import assert_that, contains_inanyorder, has_entries


@pytest.fixture
def tvm_mock_func(mocker, ohio_settings):
    def _inner(tvm_uid):
        ohio_settings.TVM_CHECK_SERVICE_TICKET = False  # disabling headers check
        mocker.patch.object(TicketCheckResult, 'default_uid', tvm_uid)

    return _inner


def serialize_order(order):
    return {
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


@pytest.mark.asyncio
class TestGetCustomerOrders:
    async def _assert_response(self, orders, response):
        orders = sorted(orders, key=lambda order: (order.created, order.order_id), reverse=True)
        assert {
            'code': 200,
            'status': 'success',
            'data': {
                'next': {
                    'created_keyset': orders[-1].created.astimezone(timezone.utc).isoformat(),
                    'order_id_keyset': orders[-1].order_id,
                },
                'orders': [serialize_order(order) for order in orders]
            },
        } == await response.json()

    @pytest.fixture
    async def orders(self, create_order):
        return sorted(
            [await create_order() for _ in range(3)],
            key=lambda order: (order.created, order.order_id),
            reverse=True,
        )

    @pytest.fixture
    def response_func(self, app, customer, tvm_mock_func):
        async def _inner(**kwargs):
            tvm_mock_func(customer.customer_uid)
            return await app.get(
                f'/v1/customer/{customer.customer_uid}/orders',
                params=kwargs,
            )

        return _inner

    async def test_orders(self, orders, response):
        await self._assert_response(orders, response)

    async def test_limit(self, orders, response_func):
        limit = len(orders) - 1
        response = await response_func(limit=limit)
        await self._assert_response(orders[:-1], response)

    async def test_service_id_filter(self, create_service, create_order, response_func):
        services = [await create_service() for _ in range(2)]
        orders = [
            await create_order(service_id=service.service_id)
            for service in services
            for _ in range(2)
        ]
        service = services[0]
        service_orders = [order for order in orders if order.service_id == service.service_id]
        response = await response_func(service_ids=service.service_id)
        await self._assert_response(service_orders, response)

    # TODO fix this
    # async def test_subservice_id_filter(self, create_order, response_func):
    #
    #     subservice_ids = ['2', '3']
    #     orders = [
    #         await create_order(subservice_id=subservice_id)
    #         for subservice_id in subservice_ids
    #         for _ in range(2)
    #     ]
    #     subservice_id = subservice_ids[0]
    #     subservice_orders = [order for order in orders if order.subservice_id == subservice_id]
    #     response = await response_func(subservice_ids=[subservice_id])
    #     await self._assert_response(subservice_orders, response)

    async def test_pagination(self, orders, response_func):
        response = await response_func(limit=1)
        next_ = (await response.json())['data']['next']
        response = await response_func(**next_)
        await self._assert_response(orders[1:], response)


@pytest.mark.asyncio
class TestGetCustomerOrderById:
    @pytest.fixture
    def response_func(self, app, order, tvm_mock_func):
        async def _inner(**kwargs):
            customer_uid = kwargs.get('customer_uid', order.customer_uid)
            order_id = kwargs.get('order_id', order.order_id)
            tvm_mock_func(customer_uid)
            return await app.get(f'/v1/customer/{customer_uid}/orders/{order_id}')

        return _inner

    async def test_resposne(self, order, response):
        assert {
            'status': 'success',
            'code': 200,
            'data': {'order': serialize_order(order)},
        } == await response.json()

    async def test_response_not_found(self, randn, response_func):
        response = await response_func(customer_uid=randn(), order_id=randn())
        assert {
            'status': 'fail',
            'code': 404,
            'data': {'message': 'NOT_FOUND_ERROR'},
        } == await response.json()


class TestGetCustomerServices:
    @pytest.fixture
    async def services(self, create_service):
        return [await create_service() for _ in range(2)]

    @pytest.fixture
    async def orders(self, randn, services, create_order):
        return [
            await create_order(service_id=service.service_id, subservice_id=str(randn()))
            for service in services
            for _ in range(2)
        ]

    @pytest.fixture
    async def response(self, app, customer, tvm_mock_func):
        tvm_mock_func(customer.customer_uid)
        return await app.get(f'/v1/customer/{customer.customer_uid}/services')

    def test_response_status(self, response):
        assert response.status == 200

    @pytest.mark.asyncio
    async def test_response_data(self, orders, response):
        assert_that(
            await response.json(),
            has_entries({
                'status': 'success',
                'code': 200,
                'data': has_entries({
                    'services': contains_inanyorder(*[
                        {
                            'service_id': order.service_id,
                            'subservice_id': order.subservice_id,
                        }
                        for order in orders
                    ]),
                }),
            })
        )
