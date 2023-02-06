from itertools import chain

import pytest
import ujson

from hamcrest import assert_that, contains, ends_with, equal_to, has_entries

from mail.payments.payments.tests.base import BaseAcquirerTest
from mail.payments.payments.tests.utils import dummy_async_function, dummy_coro


class TestOrder(BaseAcquirerTest):
    @pytest.fixture
    def customer_uid(self):
        return 'some_uid'

    @pytest.fixture(params=(None, 'test-paymethod_id'))
    def paymethod_id(self, request):
        return request.param

    @pytest.fixture
    def dummy_trust_orders(self):
        # список словарей произвольной природы
        return 55 * [{"Frodo": "Hobbit", "Gandalf": "Wizard"}]

    @pytest.fixture
    def developer_payload(self, merchant, acquirer, paymethod_id):
        legal = merchant.get_address_by_type('legal')
        return {
            'merchant': {
                'schedule_text': merchant.organization.schedule_text,
                'ogrn': merchant.organization.ogrn,
                'name': merchant.name,
                'acquirer': acquirer.value,
                'legal_address': {
                    'city': legal.city,
                    'country': legal.country,
                    'home': legal.home,
                    'street': legal.street,
                    'zip': legal.zip,
                } if legal else None,
            },
            'selected_card_id': paymethod_id,
        }

    @pytest.mark.asyncio
    async def test__create(self, trust_client, uid, acquirer):
        data = {'key': 'value'}
        headers = {'abc': 'def'}
        await trust_client._order_create(uid=uid, acquirer=acquirer, order_data=data, headers=headers)
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'POST',
                ends_with('/orders'),
                has_entries({
                    'json': data,
                    'headers': headers,
                    'uid': uid,
                    'acquirer': acquirer,
                })
            )
        )

    @pytest.mark.asyncio
    async def test__get(self, trust_client, uid, acquirer, randn):
        order_id = randn()
        await trust_client._order_get(uid=uid, acquirer=acquirer, order_id=order_id)
        assert_that(
            (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
            contains(
                'GET',
                ends_with(f'/orders/{order_id}'),
                has_entries({
                    'uid': uid,
                    'acquirer': acquirer,
                }),
            )
        )

    @pytest.mark.parametrize('customer_uid,service_fee', ((None, None), ('some_uid', '1')))
    @pytest.mark.parametrize('order_data_version', [1, 2])
    def test_trust_orders_data(self, customer_uid, trust_client, service_fee, merchant, order, items, paymethod_id,
                               acquirer, developer_payload):
        """Правильно создаем объекты заказов в АПИ траста."""
        orders_data = list(trust_client._trust_orders_data(merchant=merchant, customer_uid=customer_uid,
                                                           order=order, items=items, acquirer=acquirer,
                                                           paymethod_id=paymethod_id, service_fee=service_fee))

        delimiter = {1: '.', 2: '-'}[order.data.version]
        correct_data = [{
            'order_id': (
                f'{merchant.uid}'
                + f'{delimiter}{order.order_id}'
                + f'{delimiter}{item.product_id}'
                + f'{delimiter}{customer_uid or "_"}'
            ),
            'product_id': (
                f'{merchant.uid}.{merchant.organization.inn}.{merchant.client_id}.{item.nds.value}'
                + (f'.{service_fee}' if service_fee else '')
            ),
            'developer_payload': ujson.dumps(developer_payload),
        } for item in items]
        assert_that(orders_data, equal_to(correct_data))

    @pytest.mark.parametrize('customer_uid', (None, 'some_uid'))
    @pytest.mark.parametrize('order_id_prefix', ('', 'some-prefix-'))
    @pytest.mark.parametrize('order_data_version', [1, 2])
    def test_trust_payment_orders_data(self, customer_uid, payments_settings, trust_client, order, items,
                                       order_id_prefix):
        """Правильно создаем объекты заказов для создания платежа в трасте."""
        delimiter = {1: '.', 2: '-'}[order.data.version]
        payments_settings.TRUST_ORDER_ID_PREFIX = order_id_prefix
        orders_data = list(trust_client._trust_payment_orders_data(uid=order.uid,
                                                                   customer_uid=customer_uid,
                                                                   order=order,
                                                                   items=items))
        correct_data = [{
            'order_id': (
                f'{order_id_prefix}{order.uid}'
                + f'{delimiter}{order.order_id}'
                + f'{delimiter}{item.product_id}'
                + f'{delimiter}{customer_uid or "_"}'
            ),
            'fiscal_title': item.name,
            'fiscal_nds': item.nds.value,
            'price': str(item.price),
            'qty': str(item.amount)
        } for item in items]

        assert_that(orders_data, equal_to(correct_data))

    @pytest.mark.parametrize('customer_uid,service_fee,commission', ((None, None, None), ('some_uid', '1', 210)))
    @pytest.mark.asyncio
    async def test_orders_create__data(self, mocker, trust_client, merchant, order, items, customer_uid, paymethod_id,
                                       acquirer, service_fee, commission):
        # должны вызвать _orders_batch_create и передать данные из _trust_orders_data
        batch_create = mocker.patch.object(trust_client, '_orders_batch_create',
                                           mocker.Mock(return_value=dummy_coro([])))
        await trust_client.orders_create(
            uid=merchant.uid,
            acquirer=acquirer,
            merchant=merchant,
            order=order,
            items=items,
            customer_uid=customer_uid,
            paymethod_id=paymethod_id,
            service_fee=service_fee,
            commission=commission,
        )
        expected_orders_data = list(trust_client._trust_orders_data(merchant=merchant, order=order, items=items,
                                                                    acquirer=acquirer,
                                                                    customer_uid=customer_uid,
                                                                    paymethod_id=paymethod_id,
                                                                    service_fee=service_fee,
                                                                    commission=commission))
        headers = {'X-UID': customer_uid} if customer_uid else {}
        batch_create.assert_called_once_with(
            uid=merchant.uid,
            acquirer=acquirer,
            trust_orders=expected_orders_data,
            headers=headers,
        )

    @pytest.mark.asyncio
    async def test_orders_batch_create_splits_payload_into_batches(self, mocker, trust_client, dummy_trust_orders, uid,
                                                                   acquirer):
        # список ордеров порезан на кусочки заданного размера - число кусочков верное и они содержат исходные данные
        client_post_calls = []
        mocker.patch.object(trust_client, 'post', dummy_async_function(calls=client_post_calls))
        batch_size = 13
        await trust_client._orders_batch_create(uid=uid, acquirer=acquirer, trust_orders=dummy_trust_orders, headers={},
                                                _batch_size=batch_size)
        orders_num = len(dummy_trust_orders)
        expected_calls_num = orders_num // batch_size + (1 if orders_num % batch_size else 0)
        assert len(client_post_calls) == expected_calls_num
        batch_data = list(chain(*(kwargs['json']['orders'] for args, kwargs in client_post_calls)))
        assert_that(dummy_trust_orders, equal_to(batch_data))
