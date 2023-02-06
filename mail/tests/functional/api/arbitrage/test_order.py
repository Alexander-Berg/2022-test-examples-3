from datetime import timezone

import pytest

from sendr_utils import alist, anext, enum_value

from hamcrest import assert_that, contains_inanyorder, has_entries, has_item, has_properties

from mail.payments.payments.core.entities.arbitrage import Arbitrage
from mail.payments.payments.core.entities.enums import (
    ArbitrageStatus, ArbitrageVerdict, PayStatus, TaskType, TransactionStatus
)
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.storage.mappers.order.order import FindOrderParams


@pytest.mark.usefixtures('moderation')
class TestGetOrder:
    @pytest.fixture
    async def response(self, client, order, tvm):
        r = await client.get(f'/v1/arbitrage/order/{order.uid}/{order.order_id}')
        assert r.status == 200
        return await r.json()

    @pytest.fixture(params=(None, '123'))
    async def merchant(self, storage, merchant, request):
        merchant.dialogs_org_id = request.param
        return await storage.merchant.save(merchant)

    @pytest.fixture(params=(ArbitrageStatus.CONSULTATION, ArbitrageStatus.COMPLETE))
    async def arbitrage(self, request, storage, order, rands):
        return await storage.arbitrage.create(Arbitrage(
            uid=order.uid,
            order_id=order.order_id,
            status=request.param,
            chat_id=rands(),
            arbiter_chat_id=rands(),
            escalate_id=rands(),
        ))

    @pytest.mark.asyncio
    async def test_response_data(self, merchant, arbitrage, order, response):
        assert_that(
            response['data'],
            has_entries({
                'pay_status': enum_value(order.pay_status),
                'price': float(round(order.price, 2)),
                'updated': order.updated.astimezone(timezone.utc).isoformat(),
                'revision': order.revision,
                'active': order.active,
                'mode': enum_value(order.shop.shop_type),
                'order_id': order.order_id,
                'description': order.description,
                'currency': order.currency,
                'created': order.created.astimezone(timezone.utc).isoformat(),
                'caption': order.caption,
                'create_arbitrage_available': merchant.dialogs_org_id is not None,
                'current_arbitrage': {
                    'arbitrage_id': arbitrage.arbitrage_id,
                    'chat_id': arbitrage.chat_id,
                    'refund_id': arbitrage.refund_id,
                    'status': enum_value(arbitrage.status),
                    'created': arbitrage.created.astimezone(timezone.utc).isoformat(),
                    'updated': arbitrage.updated.astimezone(timezone.utc).isoformat(),
                } if arbitrage.status in ArbitrageStatus.ACTIVE_STATUSES else None,
                'items': contains_inanyorder(*[
                    {
                        'amount': float(round(item.amount, 2)),  # type: ignore
                        'currency': item.currency,
                        'name': item.name,
                        'nds': item.nds.value,
                        'price': float(round(item.price, 2)),  # type: ignore
                        'product_id': item.product_id,
                        'image': {
                            'url': item.image.url,
                            'stored': {
                                'original': item.image.stored.orig
                            } if item.image.stored is not None else None
                        } if item.image is not None else None,
                        'markup': item.markup,
                    }
                    for item in order.items
                ])
            })
        )


@pytest.mark.usefixtures('moderation')
class TestArbitrageConsultation:
    @pytest.fixture
    def tvm_uid(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    async def setup(self, storage, merchant, randn, tvm_uid, order):
        order.customer_uid = tvm_uid
        order.pay_status = PayStatus.PAID
        await storage.order.save(order)

        merchant.dialogs_org_id = randn()
        await storage.merchant.save(merchant)

    @pytest.fixture
    async def response(self, client, order, tvm):
        r = await client.post(f'/v1/arbitrage/order/{order.uid}/{order.order_id}/consultation')
        assert r.status == 200
        return await r.json()

    @pytest.fixture
    def consultation_id(self, rands):
        return rands()

    @pytest.fixture
    def chat_id(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def create_consultation(self, floyd_client_mocker, chat_id, consultation_id):
        result = {'id': consultation_id, 'chats': {'clients': {'chat_id': chat_id}}}
        with floyd_client_mocker('create_consultation', result) as mock:
            yield mock

    @pytest.mark.asyncio
    async def test_response_data(self, chat_id, response):
        assert_that(
            response['data'],
            has_entries({
                'status': ArbitrageStatus.CONSULTATION.value,
                'chat_id': chat_id,
            })
        )


@pytest.mark.usefixtures('moderation')
class TestArbitrageEscalate:
    @pytest.fixture
    def tvm_uid(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    async def setup(self, storage, merchant, arbitrage, randn, tvm_uid, order):
        order.customer_uid = tvm_uid
        await storage.order.save(order)

        merchant.dialogs_org_id = randn()
        await storage.merchant.save(merchant)

    @pytest.fixture
    async def response(self, client, order, tvm):
        r = await client.post(f'/v1/arbitrage/order/{order.uid}/{order.order_id}/escalate')
        assert r.status == 200
        return await r.json()

    @pytest.fixture
    def consultation_id(self, rands):
        return rands()

    @pytest.mark.asyncio
    async def test_response_data(self, order, response):
        assert_that(
            response['data'],
            has_entries({
                'status': ArbitrageStatus.ESCALATE.value,
            })
        )

    @pytest.mark.asyncio
    async def test_task(self, order, response, tasks):
        assert_that(
            tasks,
            has_item(has_properties({
                'action_name': 'start_escalate',
                'params': has_entries({
                    'action_kwargs': dict(uid=order.uid, order_id=order.order_id)
                }),
                'task_type': TaskType.RUN_ACTION,
            }))
        )


@pytest.mark.usefixtures('moderation')
class TestArbitrageVerdict:
    @pytest.fixture
    def trust_refund_id(self, rands):
        return rands()

    @pytest.fixture
    def customer_uid(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    async def setup(self, storage, merchant, rands, arbitrage, randn, tvm_uid, order):
        await storage.transaction.create(Transaction(
            uid=order.uid,
            order_id=order.order_id,
            status=TransactionStatus.CLEARED,
            trust_purchase_token=rands(),
        ))
        order.pay_status = PayStatus.PAID
        await storage.order.save(order)

        arbitrage.status = ArbitrageStatus.ESCALATE
        await storage.arbitrage.save(arbitrage)

    @pytest.fixture(autouse=True)
    def payment_get_mock(self, shop_type, trust_client_mocker, customer_uid):
        with trust_client_mocker(shop_type, 'payment_get', {'uid': customer_uid}) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def refund_create_mock(self, shop_type, trust_client_mocker, trust_refund_id):
        with trust_client_mocker(shop_type, 'refund_create', trust_refund_id) as mock:
            yield mock

    @pytest.fixture
    def request_json(self, items):
        return {
            'type': ArbitrageVerdict.REFUND.value.upper(),
            'refund': {
                'items': [{
                    'idInService': f'{item.uid}.{item.order_id}.{item.product_id}',
                    'quantity': f'{item.amount}',
                } for item in items]
            }
        }

    @pytest.fixture
    async def response(self, client, request_json, order, arbitrage, tvm):
        r = await client.post(
            '/v1/arbitrage/conversation/verdict',
            params={'conversationId': arbitrage.escalate_id},
            json=request_json
        )
        assert r.status == 200
        return await r.json()

    @pytest.mark.asyncio
    async def test_response_data(self, order, response):
        assert_that(
            response,
            has_entries({'code': 200, 'status': 'success'})
        )

    @pytest.mark.asyncio
    async def test_refund(self, items, storage, order, response):
        refund = await anext(storage.order.find(
            FindOrderParams(uid=order.uid, original_order_id=order.order_id)
        ))
        for item in items:
            item.order_id = refund.order_id
            item.image_id = None
            item.image = None
            item.markup = None
        assert_that(
            items,
            contains_inanyorder(
                *(await alist(storage.item.get_for_order(refund.uid, refund.order_id)))
            ),
        )

    def test_mocks_was_called(self, response, payment_get_mock, refund_create_mock):
        for mock in [payment_get_mock, refund_create_mock]:
            mock.assert_called_once()
