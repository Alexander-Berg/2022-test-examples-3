from datetime import timedelta
from decimal import Decimal
from urllib.parse import urlencode

import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.enums import OrderKind, PayStatus
from mail.payments.payments.utils.datetime import utcnow
from mail.payments.payments.utils.helpers import without_none


class TestOrdersStats:
    @pytest.fixture
    def order_data(self):
        return {
            'caption': 'Some test order',
            'description': 'Some description',
            'items': [
                {
                    'name': 'first',
                    'amount': 2,
                    'price': 100,
                    'nds': 'nds_10_110',
                    'currency': 'RUB',
                },
                {
                    'name': 'second',
                    'amount': 3.33,
                    'price': 100.77,
                    'nds': 'nds_10',
                    'currency': 'RUB',
                },
            ],
            'kind': OrderKind.PAY.value,
        }

    @pytest.fixture
    async def create_order(self, no_merchant_user_check, client, merchant, order_data, storage):
        with no_merchant_user_check():
            r = await client.post(f'v1/order/{merchant.uid}/', json=order_data)
        assert r.status == 200
        order_id = (await r.json())['data']['order_id']
        order = await storage.order.get(uid=merchant.uid, order_id=order_id)
        return order

    @pytest.fixture
    def stats_response_func(self, client, merchant, tvm):
        async def _inner(uid=merchant.uid, date_from=None, date_to=None):
            params = without_none({
                'date_from': None if date_from is None else date_from.isoformat(),
                'date_to': None if date_to is None else date_to.isoformat(),
            })
            url = f'v1/merchant/{uid}/orders_stats'
            if params:
                url = f'{url}?{urlencode(params)}'
            return await client.get(url)

        return _inner

    @pytest.fixture
    async def stats_response(self, stats_response_func):
        r = await stats_response_func()
        assert r.status == 200
        return await r.json()

    @pytest.fixture
    async def paid_order(self, create_order, storage):
        create_order.pay_status = PayStatus.PAID
        create_order.closed = utcnow()
        return await storage.order.save(create_order)

    @pytest.mark.asyncio
    async def test_not_found(self, stats_response_func, merchant):
        r = await stats_response_func(uid=merchant.uid + 1)
        assert r.status == 404

    @pytest.mark.asyncio
    async def test_no_orders(self, merchant, stats_response):
        assert_that(stats_response['data'], has_entries({
            'money_average': 0,
            'orders_created_count': 0,
            'orders_paid_count': 0,
            'orders_sum': 0,
        }))

    @pytest.mark.asyncio
    async def test_success_status(self, merchant, paid_order, stats_response_func):
        r = await stats_response_func()
        assert r.status == 200

    @pytest.mark.asyncio
    async def test_success_data(self, order_data, paid_order, stats_response_func):
        r = await stats_response_func()
        price = sum([Decimal(item['price']) * Decimal(item['amount']) for item in order_data['items']]) \
            .quantize(Decimal('1.00'))
        data = await r.json()
        assert_that(data['data'], has_entries({
            'money_average': float(price),
            'orders_created_count': 1,
            'orders_paid_count': 1,
            'orders_sum': float(price),
        }))

    @pytest.mark.asyncio
    async def test_success_filtered_by_date(self, order_data, paid_order, stats_response_func):
        r = await stats_response_func(date_from=utcnow() + timedelta(hours=1))
        assert_that((await r.json())['data'], has_entries({
            'money_average': 0,
            'orders_created_count': 0,
            'orders_paid_count': 0,
            'orders_sum': 0,
        }))
