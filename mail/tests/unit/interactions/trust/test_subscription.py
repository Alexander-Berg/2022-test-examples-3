import pytest

from hamcrest import assert_that, contains, ends_with, has_entries

from mail.payments.payments.utils.datetime import utcnow
from mail.payments.payments.utils.helpers import without_none


@pytest.fixture(params=(None, 'some_uid'))
def customer_uid(request):
    return request.param


@pytest.fixture(params=(None, 'rand'))
def order_id_prefix(request, rands, payments_settings):
    prefix = '' if request.param is None else rands()
    payments_settings.TRUST_ORDER_ID_PREFIX = prefix
    return prefix


class TestCreate:
    @pytest.mark.asyncio
    async def test__subscription_create(self, trust_client, uid, acquirer):
        data = {'key': 'value'}
        headers = {'abc': 'def'}
        await trust_client._subscription_create(uid=uid, acquirer=acquirer, subscription_data=data, headers=headers)
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'POST',
                ends_with('/subscriptions'),
                has_entries({
                    'headers': headers,
                    'json': data,
                    'uid': uid,
                    'acquirer': acquirer,
                })
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('order_data_version', [1, 2])
    async def test_subscription_create(self, trust_client, order_with_customer_subscription, items, customer_uid,
                                       order_id_prefix, acquirer):
        order = order_with_customer_subscription
        item = items[0]
        order.customer_uid = customer_uid

        delimiter = {1: '.', 2: '-'}[order.data.version]
        expected_json = {
            "product_id": order.customer_subscription.subscription.product_id,
            "order_id": f'{order_id_prefix}'
                        f'{order.uid}'
                        f'{delimiter}{order.order_id}'
                        f'{delimiter}{item.product_id}'
                        f'{delimiter}{"_" if customer_uid is None else customer_uid}',
            "region_id": order.customer_subscription.region_id
        }
        expected_headers = without_none({'X-UID': customer_uid})
        await trust_client.subscription_create(order.uid, acquirer, order, item, customer_uid)

        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'POST',
                ends_with('/subscriptions'),
                has_entries({
                    'headers': expected_headers,
                    'json': expected_json,
                    'uid': order.uid,
                    'acquirer': acquirer,
                })
            )
        )


class TestGet:
    @pytest.mark.asyncio
    async def test__subscription_get(self, rands, trust_client, acquirer):
        uid = rands()
        order_id = rands()
        headers = {'abc': 'def'}
        await trust_client._subscription_get(uid=uid, acquirer=acquirer, order_id=order_id, headers=headers)
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'GET',
                ends_with(f'/subscriptions/{order_id}'),
                has_entries({
                    'headers': headers,
                    'uid': uid,
                    'acquirer': acquirer,
                }),
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('order_data_version', [1, 2])
    async def test_subscription_get(self, order_id_prefix, trust_client, order_with_customer_subscription, items,
                                    customer_uid, acquirer):
        order = order_with_customer_subscription
        item = items[0]
        order.customer_uid = customer_uid

        delimiter = {1: '.', 2: '-'}[order.data.version]
        order_id = f'{order_id_prefix}' \
                   f'{order.uid}' \
                   f'{delimiter}{order.order_id}' \
                   f'{delimiter}{item.product_id}' \
                   f'{delimiter}{"_" if customer_uid is None else customer_uid}'

        expected_headers = without_none({'X-UID': customer_uid})
        await trust_client.subscription_get(
            uid=order.uid,
            acquirer=acquirer,
            order=order,
            item=item,
            customer_uid=customer_uid,
        )
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'GET',
                ends_with(f'/subscriptions/{order_id}'),
                has_entries({
                    'headers': expected_headers,
                    'uid': order.uid,
                    'acquirer': acquirer,
                }),
            )
        )


class TestCancel:
    @pytest.mark.asyncio
    async def test__subscription_cancel(self, rands, trust_client, uid, acquirer):
        order_id = rands()
        finish_ts = utcnow()
        headers = {rands(): rands()}

        await trust_client._subscription_cancel(
            uid=uid,
            acquirer=acquirer,
            order_id=order_id,
            finish_ts=finish_ts,
            headers=headers,
        )

        expected_json = {'finish_ts': finish_ts.timestamp()}
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'PUT',
                ends_with(f'/subscriptions/{order_id}'),
                has_entries({
                    'headers': headers,
                    'json': expected_json,
                    'uid': uid,
                    'acquirer': acquirer,
                })
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('order_data_version', [1, 2])
    async def test_subscription_cancel(self, order_id_prefix, trust_client, order_with_customer_subscription,
                                       items, customer_uid, acquirer):
        finish_ts = utcnow()
        order = order_with_customer_subscription
        item = items[0]
        order.customer_uid = customer_uid

        delimiter = {1: '.', 2: '-'}[order.data.version]
        order_id = (
            f'{order_id_prefix}{order.uid}'
            + f'{delimiter}{order.order_id}'
            + f'{delimiter}{item.product_id}'
            + f'{delimiter}{"_" if customer_uid is None else customer_uid}'
        )

        await trust_client.subscription_cancel(
            uid=order.uid,
            acquirer=acquirer,
            order=order,
            item=item,
            finish_ts=finish_ts,
            customer_uid=customer_uid,
        )

        expected_json = {"finish_ts": finish_ts.timestamp()}
        expected_headers = without_none({'X-UID': customer_uid})
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'PUT',
                ends_with(f'/subscriptions/{order_id}'),
                has_entries({
                    'headers': expected_headers,
                    'json': expected_json,
                    'uid': order.uid,
                    'acquirer': acquirer,
                })
            )
        )
