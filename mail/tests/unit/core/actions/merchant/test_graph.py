from datetime import timedelta

import pytest

from mail.payments.payments.core.actions.merchant.graph import MerchantPaymentsGraphAction
from mail.payments.payments.core.entities.enums import GraphType, GroupType, OrderKind, PayStatus, RefundStatus
from mail.payments.payments.tests.utils import dummy_async_generator
from mail.payments.payments.utils.datetime import utcnow


class TestMerchantPaymentsGraph:
    @pytest.fixture
    def points(self, randn):
        x = utcnow().isoformat(),
        y = randn()
        return [{'x': x, 'y': y}]

    @pytest.fixture
    def graph_params(self, rands, randn, merchant):
        return {
            'lower_dt': (utcnow() - timedelta(days=1)).isoformat(),
            'upper_dt': (utcnow() + timedelta(days=1)).isoformat(),
            'group_by': GroupType.DAY,
            'uid': merchant.uid,
            'shop_id': randn(),
            'pay_method': rands()
        }

    @pytest.fixture
    def params(self, graph_params, graph_type):
        return {
            **graph_params,
            'graph_type': graph_type,
        }

    @pytest.fixture
    async def returned(self, params, points):
        return await MerchantPaymentsGraphAction(**params).run()

    class TestPaymentsCount:
        @pytest.fixture(autouse=True)
        async def payments_count_mock(self, mocker, points):
            return mocker.patch(
                'mail.payments.payments.storage.mappers.order.order.OrderMapper.payments_count',
                mocker.Mock(return_value=dummy_async_generator(points)()))

        @pytest.fixture(params=[
            GraphType.ALL_PAYMENTS_COUNT,
            GraphType.PAID_COUNT,
            GraphType.REFUND_COUNT
        ])
        def graph_type(self, request):
            return request.param

        def test_payments_count(self, payments_count_mock, graph_params, returned, graph_type):
            graph_params.pop('graph_type', None)
            if graph_type == GraphType.PAID_COUNT:
                graph_params['pay_status'] = PayStatus.PAID
            if graph_type == GraphType.REFUND_COUNT:
                graph_params['order_kind'] = OrderKind.REFUND
                graph_params['refund_status'] = RefundStatus.COMPLETED
            payments_count_mock.assert_called_once_with(**graph_params, iterator=True)

        def test_payments_count__returned(self, returned, points):
            assert returned == points

    class TestPaymentsSum:
        @pytest.fixture(autouse=True)
        def payments_sum_mock(self, mocker, points):
            return mocker.patch(
                'mail.payments.payments.storage.mappers.order.order.OrderMapper.payments_sum',
                mocker.Mock(return_value=dummy_async_generator(points)()))

        @pytest.fixture(params=[
            GraphType.SUM_PAID_BILLS,
            GraphType.REFUND_SUM
        ])
        def graph_type(self, request):
            return request.param

        def test_payments_sum(self, payments_sum_mock, graph_params, returned, graph_type):
            graph_params.pop('graph_type', None)
            if graph_type == GraphType.SUM_PAID_BILLS:
                graph_params['pay_status'] = PayStatus.PAID

            if graph_type == GraphType.REFUND_SUM:
                graph_params['order_kind'] = OrderKind.REFUND
                graph_params['refund_status'] = RefundStatus.COMPLETED
            payments_sum_mock.assert_called_once_with(**graph_params, iterator=True)

        def test_paymennts_sum__returned(self, returned, points):
            assert returned == points

    class TestAverageBill:
        @pytest.fixture(autouse=True)
        def average_bill_mock(self, mocker, points):
            return mocker.patch(
                'mail.payments.payments.storage.mappers.order.order.OrderMapper.average_bill',
                mocker.Mock(return_value=dummy_async_generator(points)()))

        @pytest.fixture
        def graph_type(self):
            return GraphType.AVERAGE_BILL

        def test_average_bill(self, average_bill_mock, graph_params, returned):
            graph_params.pop('graph_type', None)
            graph_params['pay_status'] = PayStatus.PAID
            average_bill_mock.assert_called_once_with(**graph_params, iterator=True)

        def test_average_bill_returned(self, returned, points):
            assert returned == points
