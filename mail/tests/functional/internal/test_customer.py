from datetime import datetime, timezone
from decimal import Decimal

import pytest

from hamcrest import assert_that, contains, has_properties, is_

from mail.ohio.ohio.core.entities.customer import Customer
from mail.ohio.ohio.core.entities.enums import NDS, OrderStatus, RefundStatus
from mail.ohio.ohio.storage.exceptions import OrderNotFoundStorageError


@pytest.mark.asyncio
class TestPostInternalCustomerOrders:
    def _has_properties_item(self, item):
        return has_properties({
            'name': item['name'],
            'nds': NDS(item['nds']),
            'price': Decimal(item['price']),
            'currency': item['currency'],
            'amount': Decimal(item['amount']),
        })

    def _check_order(self, customer_uid, service, request_data, order):
        assert_that(
            order,
            has_properties({
                'customer_uid': customer_uid,
                'service_id': service.service_id,
                'order_id': is_(int),
                'subservice_id': request_data['subservice_id'],

                'merchant_uid': request_data['merchant_uid'],
                'service_merchant_id': request_data['service_merchant_id'],
                'payments_order_id': request_data['payments_order_id'],

                'trust_payment_id': None,
                'trust_purchase_token': request_data['trust_purchase_token'],

                'status': OrderStatus(request_data['status']),
                'order_data': has_properties({
                    'total': Decimal(request_data['order_data']['total']),
                    'currency': request_data['order_data']['currency'],
                    'description': request_data['order_data']['description'],
                    'items': contains(*[
                        self._has_properties_item(item)
                        for item in request_data['order_data']['items']
                    ]),
                    'refunds': contains(*[
                        has_properties({
                            'trust_refund_id': refund['trust_refund_id'],
                            'refund_status': RefundStatus(refund['refund_status']),
                            'total': Decimal(refund['total']),
                            'currency': refund['currency'],
                            'items': contains(*[self._has_properties_item(item) for item in refund['items']]),
                        })
                        for refund in request_data['order_data']['refunds']
                    ]),
                }),
                'order_revision': request_data['revision'],
                'service_data': request_data['service_data'],
                'service_revision': request_data['revision'],

                'created': datetime.fromisoformat(request_data['created']),
                'updated': is_(datetime),
            }),
        )

    @pytest.fixture
    def customer_uid(self, randn):
        return randn()

    @pytest.fixture
    def payments_service_id(self, service):
        return service.payments_service_id

    @pytest.fixture
    def trust_purchase_token(self, rands):
        return rands()

    @pytest.fixture
    def get_order(self, storage, customer_uid, trust_purchase_token):
        async def _inner():
            return await storage.order.get_by_trust_purchase_token(
                customer_uid=customer_uid,
                trust_purchase_token=trust_purchase_token,
            )

        return _inner

    @pytest.fixture
    def create_item(self, rands):
        def _inner():
            return {
                'name': rands(),
                'nds': NDS.NDS_0.value,
                'price': '12.34',
                'currency': rands(),
                'amount': '1.0',
            }

        return _inner

    @pytest.fixture
    def get_request_data(self, randn, rands, payments_service_id, trust_purchase_token, create_item):
        def _inner():
            return {
                'payments_service_id': payments_service_id,
                'trust_purchase_token': trust_purchase_token,
                'subservice_id': rands(),
                'merchant_uid': randn(),
                'service_merchant_id': randn(),
                'payments_order_id': randn(),
                'status': OrderStatus.PAID.value,
                'order_data': {
                    'total': '12.34',
                    'currency': rands(),
                    'items': [create_item() for _ in range(2)],
                    'description': rands(),
                    'refunds': [
                        {
                            'trust_refund_id': rands(),
                            'refund_status': RefundStatus.CREATED.value,
                            'total': '22.33',
                            'currency': rands(),
                            'items': [create_item() for _ in range(2)],
                        }
                        for _ in range(2)
                    ],
                },
                'service_data': {rands(): rands()},
                'revision': randn(),
                'created': datetime(2019, 7, 6, 11, 21, tzinfo=timezone.utc).isoformat(),
            }

        return _inner

    @pytest.fixture
    def request_data(self, get_request_data):
        return get_request_data()

    @pytest.fixture
    def response_func(self, app, customer_uid, request_data):
        async def _inner(**kwargs):
            return await app.post(
                f'/v1/internal/customer/{customer_uid}/orders',
                json={**request_data, **kwargs},
            )

        return _inner

    async def test_nonexistent_service(self, randn, get_order, customer_uid, trust_purchase_token, response_func):
        response = await response_func(payments_service_id=randn())
        try:
            order = await get_order()
        except OrderNotFoundStorageError:
            order = None
        assert response.status == 200 and order is None

    async def test_order_created(self, get_order, service, customer_uid, request_data, response):
        self._check_order(customer_uid, service, request_data, await get_order())

    async def test_order_created_with_existing_customer(self, storage, get_order, service, customer_uid, request_data,
                                                        response_func):
        await storage.customer.create(Customer(customer_uid=customer_uid))
        await response_func()
        self._check_order(customer_uid, service, request_data, await get_order())

    async def test_order_updated(self, get_order, service, customer_uid, trust_purchase_token, get_request_data,
                                 request_data, response_func):
        await response_func()
        new_request_data = get_request_data()
        new_request_data['revision'] = request_data['revision'] + 1
        await response_func(**new_request_data)
        self._check_order(customer_uid, service, new_request_data, await get_order())

    async def test_order_not_updated_because_of_revision(self, get_order, service, customer_uid, trust_purchase_token,
                                                         get_request_data, request_data, response_func):
        await response_func()
        new_request_data = get_request_data()
        new_request_data['revision'] = request_data['revision']
        await response_func(**new_request_data)
        self._check_order(customer_uid, service, request_data, await get_order())
