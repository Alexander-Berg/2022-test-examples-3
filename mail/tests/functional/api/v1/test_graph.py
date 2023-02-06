from datetime import timedelta

import pytest

from sendr_utils import enum_value, without_none

from hamcrest import assert_that, has_items, is_

from mail.payments.payments.core.entities.enums import PAY_METHOD_OFFLINE, GraphType, GroupType, MerchantRole
from mail.payments.payments.tests.base import BaseTestMerchantRoles


@pytest.mark.usefixtures('moderation')
class TestGraph(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

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
    def pay_method(self):
        return None

    @pytest.fixture
    def graph_data(self, order, pay_method, graph_type):
        return without_none({
            'pay_method': pay_method,
            'graph_type': enum_value(graph_type),
            'lower_dt': (order.created - timedelta(days=15)).isoformat(),
            'upper_dt': (order.created + timedelta(days=15)).isoformat(),
            'group_by': enum_value(GroupType.DAY)
        })

    @pytest.fixture
    async def response_json(self, client, merchant, graph_data, tvm):
        r = await client.get(
            f'/v1/graph/{merchant.uid}/{graph_data["graph_type"]}',
            params=graph_data
        )
        assert r.status == 200
        return await r.json()

    @pytest.mark.parametrize('role', (None,))
    @pytest.mark.asyncio
    async def test_required_params(self, client, merchant, graph_data, tvm):
        r = await client.get(
            f'/v1/graph/{merchant.uid}/{graph_data["graph_type"]}'
        )
        assert r.status == 400

    @pytest.mark.asyncio
    async def test_response(self, order, response_json):
        assert_that(response_json, has_items('data'))

    @pytest.mark.parametrize('graph_type', (GraphType.ALL_PAYMENTS_COUNT,))
    @pytest.mark.asyncio
    async def test_y_is_float(self, order, response_json):
        assert_that(response_json['data'][0]['y'], is_(float))

    @pytest.mark.parametrize('pay_method', (PAY_METHOD_OFFLINE,))
    @pytest.mark.asyncio
    async def test_pay_method(self, order, response_json):
        assert len(response_json['data']) == 0
