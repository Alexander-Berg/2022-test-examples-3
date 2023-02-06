import pytest

from sendr_taskqueue.worker.storage import TaskState

from hamcrest import assert_that, contains_inanyorder, has_entries, has_properties

from mail.payments.payments.core.actions.arbitrage.verdict import VerdictArbitrageAction
from mail.payments.payments.core.actions.order.refund import CoreCreateRefundAction
from mail.payments.payments.core.entities.enums import ArbitrageStatus, ArbitrageVerdict, TaskType
from mail.payments.payments.core.exceptions import (
    ArbitrageNotEscalateError, ArbitrageNotFoundError, ProductNotFoundError
)


class TestEscalateArbitrageAction:
    @pytest.fixture
    def status(self):
        return ArbitrageStatus.ESCALATE

    @pytest.fixture
    def escalate_id(self, arbitrage):
        return arbitrage.escalate_id

    @pytest.fixture
    def verdict(self):
        return ArbitrageVerdict.REFUND

    @pytest.fixture
    def product_id_noise(self):
        return ''

    @pytest.fixture
    def refund_items(self, order, items, product_id_noise, products):
        return [
            {
                'uid': item.uid,
                'order_id': item.order_id,
                'product_id': int(f'{product_id_noise}{item.product_id}'),
                'amount': item.amount
            } for item in items
        ]

    @pytest.fixture(autouse=True)
    async def mock_create_refund(self, mock_action, refund):
        return mock_action(CoreCreateRefundAction, refund)

    @pytest.fixture(autouse=True)
    async def setup(self, status, storage, arbitrage):
        arbitrage.status = status
        await storage.arbitrage.save(arbitrage)

    @pytest.fixture
    def returned_func(self, escalate_id, verdict, refund_items):
        async def _inner():
            return await VerdictArbitrageAction(escalate_id, verdict, refund_items).run()

        return _inner

    @pytest.mark.asyncio
    @pytest.mark.parametrize('escalate_id', ('-1',))
    async def test_not_found(self, returned_func):
        with pytest.raises(ArbitrageNotFoundError):
            await returned_func()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('status', (ArbitrageStatus.COMPLETE,))
    async def test_not_escalate(self, returned_func):
        with pytest.raises(ArbitrageNotEscalateError):
            await returned_func()

    @pytest.mark.parametrize('verdict', (ArbitrageVerdict.AGREE, ArbitrageVerdict.DECLINE))
    def test_refund_not_called(self, mock_create_refund, returned):
        mock_create_refund.assert_not_called()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('product_id_noise', '1')
    async def test_refund_product_not_found(self, returned_func):
        with pytest.raises(ProductNotFoundError):
            await returned_func()

    def test_call(self, mock_create_refund, items, order, returned):
        mock_create_refund.assert_called_once_with(
            order_id=order.order_id,
            uid=order.uid,
            caption=order.caption,
            description=order.description,
            items=[{
                'uid': item.uid,
                'order_id': item.order_id,
                'product_id': item.product_id,
                'amount': item.amount,
                'name': item.product.name,
                'currency': item.product.currency,
                'price': item.product.price,
                'nds': item.product.nds
            } for item in items],
        )

    @pytest.mark.asyncio
    async def test_returned(self, storage, returned, refund, verdict):
        arbitrage = await storage.arbitrage.get(returned.arbitrage_id)
        assert_that(arbitrage, has_properties({
            'status': ArbitrageStatus.COMPLETE,
            'verdict': verdict,
            'refund_id': refund.order_id
        }))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('verdict', list(ArbitrageVerdict))
    async def test_tasks(self, verdict, arbitrage, returned, get_tasks):
        tasks = []
        if verdict != ArbitrageVerdict.REFUND:
            tasks.append(
                has_properties({
                    'action_name': 'notify_arbitrage_action',
                    'task_type': TaskType.RUN_ACTION,
                    'state': TaskState.PENDING,
                    'params': has_entries({
                        'action_kwargs': {
                            'arbitrage_id': arbitrage.arbitrage_id,
                        }
                    })
                })
            )

        assert_that(await get_tasks(), contains_inanyorder(*tasks))
