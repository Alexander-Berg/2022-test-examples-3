from datetime import datetime

import pytest

from sendr_utils import enum_value

from mail.payments.payments.core.actions.merchant.graph import MerchantPaymentsGraphAction
from mail.payments.payments.core.entities.enums import PAY_METHOD_YANDEX, GraphType, GroupType


class TestGraphGet:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(MerchantPaymentsGraphAction)

    @pytest.fixture(params=[
        GraphType.ALL_PAYMENTS_COUNT,
        GraphType.PAID_COUNT,
        GraphType.REFUND_COUNT,
        GraphType.AVERAGE_BILL,
        GraphType.SUM_PAID_BILLS,
        GraphType.REFUND_SUM
    ])
    def graph_type(self, request):
        return request.param

    @pytest.fixture
    def graph_data(self, randn, graph_type):
        return {
            'graph_type': graph_type.value,
            'lower_dt': '2019-05-01T00:00:00+03:00',
            'upper_dt': '2019-09-01T00:00:00+03:00',
            'group_by': enum_value(GroupType.DAY),
            'shop_id': randn(),
            'pay_method': PAY_METHOD_YANDEX
        }

    @pytest.fixture
    async def response(self, action, payments_client, merchant, graph_data):
        return await payments_client.get(f'/v1/graph/{merchant.uid}/{graph_data["graph_type"]}', params=graph_data)

    def test_params(self, action, merchant, response, graph_data):
        graph_data['uid'] = merchant.uid
        graph_data['lower_dt'] = datetime.fromisoformat(graph_data['lower_dt'])
        graph_data['upper_dt'] = datetime.fromisoformat(graph_data['upper_dt'])
        graph_data['graph_type'] = GraphType(graph_data['graph_type'])
        graph_data['group_by'] = GroupType(graph_data['group_by'])
        action.assert_called_once_with(**graph_data)
