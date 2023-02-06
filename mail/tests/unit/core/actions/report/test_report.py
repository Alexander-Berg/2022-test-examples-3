import csv
import io
import os
from datetime import timedelta
from decimal import Decimal

import pytest
from library.python import resource

from hamcrest import assert_that, has_entries, has_properties

from mail.payments.payments.core.actions.report import CreateReportAction, CreateTaskReportAction
from mail.payments.payments.core.entities.enums import (
    NDS, PAY_METHODS, PAYMETHOD_ID_OFFLINE, OrderKind, PayStatus, RefundStatus, TaskType
)
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.product import Product
from mail.payments.payments.core.entities.report import Report
from mail.payments.payments.storage.mappers.order.order import SKIP_CONDITION, OrderMapper
from mail.payments.payments.tests.utils import dummy_async_generator
from mail.payments.payments.utils.datetime import utcnow
from mail.payments.payments.utils.helpers import create_csv_writer, temp_setattr


def strip_csv(file_):
    return sorted([row[1:3] + row[4:] for row in csv.reader(file_)])


@pytest.fixture
def original_order_id(order):
    return order.order_id


@pytest.fixture
def orders_data(original_order_id):
    return [
        {
            'kind': OrderKind.PAY,
            'pay_status': PayStatus.NEW,
        },
        {
            'kind': OrderKind.PAY,
            'pay_status': PayStatus.PAID,
        },
        {
            'kind': OrderKind.REFUND,
            'pay_status': None,
            'refund_status': RefundStatus.COMPLETED,
            'original_order_id': original_order_id,
        },
        {
            'kind': OrderKind.PAY,
            'pay_status': PayStatus.NEW,
        },
        {
            'kind': OrderKind.PAY,
            'pay_status': PayStatus.PAID,
            'paymethod_id': PAYMETHOD_ID_OFFLINE
        },
        {
            'kind': OrderKind.MULTI,
            'pay_status': None,
        },
        {
            'kind': OrderKind.PAY,
            'pay_status': PayStatus.PAID,
            'parent_order_id': original_order_id,
        }
    ]


@pytest.fixture
async def orders(storage, merchant, orders_data, shop):
    created = []
    for order_data in orders_data:
        order_data.setdefault('uid', merchant.uid)
        order_data.setdefault('shop_id', shop.shop_id)
        price = order_data.pop('price', Decimal())
        order = await storage.order.create(Order(**order_data))
        created.append(order)
        product, _ = await storage.product.get_or_create(Product(
            uid=merchant.uid,
            name='product',
            price=price,
            nds=NDS.NDS_0,
        ))
        await storage.item.create(Item(
            uid=merchant.uid,
            order_id=order.order_id,
            product_id=product.product_id,
            amount=Decimal(1)
        ))

    return created


@pytest.fixture
def pay_method(randitem):
    return randitem(PAY_METHODS)


@pytest.fixture
def lower_dt():
    return utcnow() - timedelta(days=1)


@pytest.fixture
def upper_dt():
    return utcnow() + timedelta(days=1)


@pytest.fixture
def mds_path(rands):
    return rands()


@pytest.fixture(autouse=True)
def upload_mock(mds_client_mocker, mds_path):
    with mds_client_mocker('upload', mds_path) as mock:
        yield mock


class TestCreateTaskReportAction:
    @pytest.fixture
    def params(self, merchant, lower_dt, pay_method, upper_dt):
        return {
            'uid': merchant.uid,
            'lower_dt': lower_dt,
            'upper_dt': upper_dt,
            'pay_method': pay_method
        }

    @pytest.fixture
    async def returned(self, params):
        return await CreateTaskReportAction(**params).run()

    @pytest.mark.asyncio
    async def test_report_params(self, returned, lower_dt, upper_dt, pay_method, storage):
        task_returned, report_returned = returned
        report = await storage.report.get(report_id=report_returned.report_id)
        assert_that(report, has_properties({
            'data': has_entries({
                'lower_dt': lower_dt,
                'upper_dt': upper_dt,
                'pay_method': pay_method
            }),
            'uid': report_returned.uid,
            'mds_path': report_returned.mds_path
        }))

    @pytest.mark.asyncio
    async def test_task_params(self, returned, storage, merchant):
        task_returned, report_returned = returned
        task = await storage.task.get(task_id=task_returned.task_id)
        assert_that(task, has_properties({
            'params': has_entries(
                action_kwargs={
                    'uid': merchant.uid,
                    'report_id': report_returned.report_id,
                }
            ),
            'action_name': CreateReportAction.action_name,
            'task_type': TaskType.RUN_ACTION,
        }))


class TestCreateReportAction:
    @pytest.fixture(autouse=True)
    def setup(self, storage, mocker):
        mocker.spy(OrderMapper, 'find')

    @pytest.fixture
    async def report_id(self, storage, merchant, lower_dt, pay_method, upper_dt):
        report = await storage.report.create(Report(
            uid=merchant.uid,
            data={'lower_dt': lower_dt, 'upper_dt': upper_dt, 'pay_method': pay_method},
        ))
        return report.report_id

    @pytest.fixture
    def params(self, merchant, report_id):
        return {
            'uid': merchant.uid,
            'report_id': report_id,
        }

    @pytest.fixture
    async def csv_returned(self, storage, params, lower_dt, upper_dt):
        output = io.StringIO()
        with temp_setattr(CreateReportAction.context, 'storage', storage):
            async for row in CreateReportAction(**params).create_csv(lower_dt=lower_dt, upper_dt=upper_dt):
                print(row.decode('utf-8'), file=output, end='')
        return output

    @pytest.fixture
    async def returned(self, storage, params):
        return await CreateReportAction(**params).run()

    @pytest.fixture
    def header(self):
        return ["Номер строки", "Ключ товара", "Название товара", "Время выставления счета продавцом",
                "Время завершения оплаты", "Уникальный ключ заказа", "Почта покупателя", "Цена товара",
                "Количество", "Заказ или возврат", "Статус всего заказа/возврата", "Название заказа", "Способ оплаты"]

    @pytest.fixture
    def expected_csvfile(self, header):
        writer, output = create_csv_writer()
        writer.writerow(header)
        return output

    @pytest.fixture
    def csv_mock(self, mocker, expected_csvfile):
        return mocker.patch.object(
            CreateReportAction,
            'create_csv',
            mocker.Mock(return_value=dummy_async_generator([])())
        )

    @pytest.fixture
    def expected_report_file(self, root):
        report_path = os.path.join('resfs', 'file', 'tests', 'unit', 'data', 'report.csv')
        return resource.find(report_path).decode('utf-8').strip('\n').split('\n')

    def test_mapper(self, storage, returned, merchant, lower_dt, upper_dt):
        find_params = storage.order.find.call_args[0][1]

        assert_that(
            find_params,
            has_properties({
                'uid': merchant.uid,
                'created_from': lower_dt,
                'created_to': upper_dt,
                'exclude_stats': False,
                'parent_order_id': SKIP_CONDITION
            })
        )

    @pytest.mark.asyncio
    async def test_report_returned(self, merchant_uid, pay_method, lower_dt, upper_dt, returned, mds_path):
        assert_that(
            returned,
            has_properties({
                'data': has_entries({
                    'lower_dt': lower_dt,
                    'upper_dt': upper_dt,
                    'pay_method': pay_method
                }),
                'uid': merchant_uid,
                'mds_path': mds_path,
            }),
        )

    @pytest.mark.asyncio
    async def test_create_csv(self, orders, csv_returned, expected_report_file):
        csv_returned.seek(0)
        assert strip_csv(expected_report_file) == strip_csv(csv_returned)

    def test_csv_call(self, csv_mock, returned, lower_dt, upper_dt, pay_method):
        csv_mock.assert_called_once_with(lower_dt=lower_dt, upper_dt=upper_dt, pay_method=pay_method)

    @pytest.mark.asyncio
    async def test_csv_returned(self, csv_returned, expected_csvfile):
        assert csv_returned.getvalue() == expected_csvfile.getvalue()

    def test_kinds_mapping(self):
        for name, member in OrderKind.__members__.items():
            assert member in CreateReportAction.KINDS_MAPPING, f"{name} key absent in CreateReportAction.KINDS_MAPPING"

    def test_refund_mapping(self):
        for name, member in RefundStatus.__members__.items():
            assert member in CreateReportAction.REFUND_MAPPING, \
                f"{name} key absent in CreateReportAction.REFUND_MAPPING"

    def test_pay_mapping(self):
        for name, member in PayStatus.__members__.items():
            assert member in CreateReportAction.PAY_MAPPING, f"{name} key absent in CreateReportAction.PAY_MAPPING"
