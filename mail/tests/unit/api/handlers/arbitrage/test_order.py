from decimal import Decimal

import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.enums import ArbitrageVerdict


@pytest.fixture(autouse=True)
def tvm(base_tvm, merchant, randn, tvm_client_id):
    base_tvm.src = tvm_client_id
    base_tvm.default_uid = randn()
    return base_tvm


class TestOrderGet:
    @pytest.fixture
    def action(self, mock_action, order, items):
        from mail.payments.payments.core.actions.arbitrage.get_order import GetOrderWithCurrentArbitrage
        return mock_action(GetOrderWithCurrentArbitrage, order)

    @pytest.fixture
    async def response(self, action, payments_client, order):
        return await payments_client.get(f'/v1/arbitrage/order/{order.uid}/{order.order_id}')

    def test_context(self, order, response, merchant, action):
        action.assert_called_once_with(uid=merchant.uid, order_id=order.order_id)


class TestArbitrageConsultationHandler:
    @pytest.fixture
    def action(self, mock_action, arbitrage):
        from mail.payments.payments.core.actions.arbitrage.consultation import ConsultationArbitrageAction
        return mock_action(ConsultationArbitrageAction, arbitrage)

    @pytest.fixture
    async def response(self, action, payments_client, order):
        return await payments_client.post(f'/v1/arbitrage/order/{order.uid}/{order.order_id}/consultation')

    def test_context(self, order, response, tvm, action):
        action.assert_called_once_with(uid=order.uid, order_id=order.order_id, customer_uid=tvm.default_uid)


class TestArbitrageEscalateHandler:
    @pytest.fixture
    def action(self, mock_action, arbitrage):
        from mail.payments.payments.core.actions.arbitrage.escalate import EscalateArbitrageAction
        return mock_action(EscalateArbitrageAction, arbitrage)

    @pytest.fixture
    async def response(self, action, payments_client, order):
        return await payments_client.post(f'/v1/arbitrage/order/{order.uid}/{order.order_id}/escalate')

    def test_context(self, order, response, tvm, action):
        action.assert_called_once_with(uid=order.uid, order_id=order.order_id, customer_uid=tvm.default_uid)


class TestArbitrageVerdictHandler:
    @pytest.fixture
    def verdict(self):
        return ArbitrageVerdict.REFUND

    @pytest.fixture
    def refund(self, id_in_service, randn):
        return {
            'items': [{
                'idInService': id_in_service,
                'quantity': randn(),
                'amount': randn()
            }]
        }

    @pytest.fixture
    def conversation_id(self, rands):
        return rands()

    @pytest.fixture
    def id_in_service(self, randn):
        return f'{randn()}.{randn()}.{randn()}'

    @pytest.fixture
    def action(self, mock_action, arbitrage):
        from mail.payments.payments.core.actions.arbitrage.verdict import VerdictArbitrageAction
        return mock_action(VerdictArbitrageAction)

    @pytest.fixture
    def request_json(self, refund, verdict):
        return {
            'type': verdict.value.upper(),
            'refund': refund
        }

    @pytest.fixture
    async def response(self, action, request_json, conversation_id, payments_client, order):
        return await payments_client.post(
            '/v1/arbitrage/conversation/verdict',
            json=request_json,
            params={'conversationId': conversation_id}
        )

    def test_context(self, order, conversation_id, response, refund, request_json, tvm, action):
        action.assert_called_once_with(
            escalate_id=conversation_id,
            verdict=ArbitrageVerdict(request_json['type'].lower()),
            items=[{
                'amount': Decimal(item['quantity']),
                'uid': int(item['idInService'].split('.')[0]),
                'order_id': int(item['idInService'].split('.')[1]),
                'product_id': int(item['idInService'].split('.')[2]),
            } for item in request_json['refund']['items']] if refund else None
        )

    @pytest.mark.parametrize('refund,verdict', [(None, ArbitrageVerdict.DECLINE)])
    def test_context_decline(self, order, conversation_id, response, refund, request_json, tvm, action):
        action.assert_called_once_with(escalate_id=conversation_id, verdict=ArbitrageVerdict.DECLINE)

    @pytest.mark.asyncio
    @pytest.mark.parametrize('id_in_service', ['1.1'])
    async def test_id_in_service_invalid_format(self, response):
        assert_that(await response.json(), has_entries({
            'status': 'fail',
            'data': has_entries({
                'params': {
                    'refund': {
                        'items': {
                            'idInService': ['Invalid format.']
                        }
                    }
                }
            })
        }))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('refund', [None])
    async def test_refund_required(self, response):
        assert_that(await response.json(), has_entries({
            'status': 'fail',
            'data': has_entries({
                'params': {
                    'refund': ['Missing data for required field.']
                }
            })
        }))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('id_in_service', ['1.1.z'])
    async def test_id_in_service_invalid_value(self, response):
        assert_that(await response.json(), has_entries({
            'status': 'fail',
            'data': has_entries({
                'params': {
                    'refund': {
                        'items': {
                            'idInService': ['Invalid value.']
                        }
                    }
                }
            })
        }))
