import csv
import io
import os

import pytest
from library.python import resource

from mail.payments.payments.core.actions.order.create_from_multi import (
    CreateOrderFromMultiOrderAction, CreateOrderFromMultiOrderServiceMerchantAction, DownloadMultiOrderEmailListAction
)
from mail.payments.payments.core.entities.enums import OrderKind, PayStatus, RefundStatus
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.exceptions import OrdersAmountExceed
from mail.payments.payments.tests.base import BaseTestOrderAction
from mail.payments.payments.utils.helpers import create_csv_writer, temp_setattr


class TestCreateOrderFromMultiOrderAction(BaseTestOrderAction):
    @pytest.fixture(params=('uid', 'service_merchant'))
    def params(self, merchant, multi_order, service_client, service_merchant, request):
        data = {
            'uid': {'uid': merchant.uid},
            'service_merchant': {
                'service_tvm_id': service_client.tvm_id,
                'service_merchant_id': service_merchant.service_merchant_id
            }
        }

        params = {
            'order_id': multi_order.order_id,
            **data[request.param]
        }

        return params

    @pytest.fixture
    def action(self, params):
        service_action = bool(params.get('service_merchant_id'))
        if service_action:
            return CreateOrderFromMultiOrderServiceMerchantAction(**params)
        else:
            return CreateOrderFromMultiOrderAction(**params)

    @pytest.fixture
    def returned_func(self, action):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.fixture
    async def created_order(self, storage, returned):
        return await storage.order.get(returned.uid, returned.order_id)

    @pytest.fixture
    async def created_items(self, storage, returned):
        return [
            item
            async for item in storage.item.get_for_order(returned.uid, returned.order_id)
        ]

    @pytest.fixture
    async def multi_order_items(self, storage, multi_order):
        return [
            item
            async for item in storage.item.get_for_order(multi_order.uid, multi_order.order_id)
        ]

    def test_created_order(self, returned, multi_order, created_order):
        assert all((
            returned.revision == created_order.revision,
            returned.parent_order_id == multi_order.order_id,
        ))

    def test_created_order_service_client_id(self, service_client, params, created_order):
        service_client_id = service_client.service_client_id if 'service_tvm_id' in params else None
        assert created_order.service_client_id == service_client_id

    def test_created_order_service_merchant_id(self, params, created_order):
        assert created_order.service_merchant_id == params.get('service_merchant_id')

    def test_created_items(self, returned, items_data, multi_order_items, created_items):
        for item in multi_order_items:
            item.order_id = returned.order_id
        assert created_items == multi_order_items

    class TestMaxAmount:
        @pytest.fixture
        def order_data_data(self):
            return {
                'multi_issued': 1,
                'multi_max_amount': 1
            }

        @pytest.mark.asyncio
        async def test_max_amount(self, returned_func):
            with pytest.raises(OrdersAmountExceed):
                await returned_func()


class TestDownloadMultiOrderEmailListAction:
    @pytest.fixture
    def orders_data(self):
        return [
            {
                'kind': OrderKind.MULTI,
                'pay_status': None,
                'user_email': 'das@yandex.ru',
            },
            {
                'kind': OrderKind.PAY,
                'pay_status': PayStatus.NEW,
                'parent_order_id': 1,
                'user_email': 'asd@yandex.ru',
            },
            {
                'kind': OrderKind.PAY,
                'pay_status': PayStatus.PAID,
                'parent_order_id': 1,
                'user_email': 'sad@yandex.ru',
            },
            {
                'kind': OrderKind.REFUND,
                'pay_status': None,
                'refund_status': RefundStatus.COMPLETED,
                'original_order_id': 3,
                'parent_order_id': 1,
                'user_email': 'ads@yandex.ru',
            },
            {
                'kind': OrderKind.PAY,
                'pay_status': PayStatus.NEW,
                'user_email': 'dsa@yandex.ru',
            },
            {
                'kind': OrderKind.PAY,
                'pay_status': PayStatus.PAID,
                'parent_order_id': 1,
                'user_email': 'sda@yandex.ru',
            },
        ]

    @pytest.fixture
    async def orders(self, storage, merchant, shop, orders_data):
        created = []
        for order_data in orders_data:
            order_data.setdefault('uid', merchant.uid)
            order_data.setdefault('shop_id', shop.shop_id)
            order = await storage.order.create(Order(**order_data))
            created.append(order)
        return created

    @pytest.fixture
    def params(self, merchant):
        return {
            'uid': merchant.uid,
            'order_id': 1,
        }

    @pytest.fixture
    async def csv_returned(self, storage, params):
        output = io.StringIO()
        with temp_setattr(DownloadMultiOrderEmailListAction.context, 'storage', storage):
            for row in await DownloadMultiOrderEmailListAction(**params)._create_csv():
                print(row.decode('utf-8'), file=output, end='')
        return output

    @pytest.fixture
    async def returned(self, storage, params):
        return await DownloadMultiOrderEmailListAction(**params).run()

    @pytest.fixture
    def header(self):
        return ["Номер строки", "Email", "Оформлен возврат"]

    @pytest.fixture
    def expected_csvfile(self, header):
        writer, output = create_csv_writer()
        writer.writerow(header)
        return output

    @pytest.fixture
    def expected_email_list_file(self, root):
        report_path = os.path.join('resfs', 'file', 'tests', 'unit', 'data', 'email_list.csv')
        return resource.find(report_path).decode('utf-8').split('\n')

    def strip_csv(self, file_):
        return sorted([row[1:] for row in csv.reader(file_)])

    @pytest.mark.asyncio
    async def test_create_csv(self, storage, orders, csv_returned, expected_email_list_file):
        csv_returned.seek(0)
        assert self.strip_csv(expected_email_list_file) == self.strip_csv(csv_returned)

    @pytest.mark.asyncio
    async def test_header(self, csv_returned, expected_csvfile):
        assert csv_returned.getvalue() == expected_csvfile.getvalue()
