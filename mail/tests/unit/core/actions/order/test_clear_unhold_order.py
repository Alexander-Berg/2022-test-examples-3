from decimal import Decimal

import pytest

from sendr_utils import alist

from hamcrest import assert_that, has_entries, has_item, has_properties

from mail.payments.payments.core.actions.order.clear import ClearByIdsOrderAction
from mail.payments.payments.core.actions.order.clear_unhold import (
    CoreClearUnholdOrderAction, CoreScheduleClearUnholdOrderAction, ScheduleClearUnholdOrderAction,
    UpdateTransactionAction
)
from mail.payments.payments.core.actions.order.get_trust_env import GetOrderTrustEnvAction
from mail.payments.payments.core.actions.order.resize import ResizeOrderAction
from mail.payments.payments.core.entities.enums import OrderKind, PayStatus, TaskType, TrustEnv
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.core.entities.transaction import Transaction, TransactionStatus
from mail.payments.payments.core.exceptions import (
    CoreActionDenyError, CoreDataError, CoreNotFoundError, ItemsInvalidAmountDataError, ItemsInvalidDataError,
    ItemsInvalidTotalPriceDataError, OrderCannotClearOrUnholdRefundError, OrderHasAutoclearOnError,
    OrderPayStatusMustBeHeldOrInModerationError
)
from mail.payments.payments.tests.base import BaseAcquirerTest, BaseTestRequiresModeration, parametrize_shop_type
from mail.payments.payments.tests.utils import dummy_coro
from mail.payments.payments.utils.helpers import without_none


@pytest.fixture(autouse=True)
def update_transaction_calls(mocker, transaction):
    def dummy_init(self, *args, **kwargs):
        self.tx = kwargs['transaction']

    async def dummy_run(self):
        return self.tx

    mocker.patch.object(UpdateTransactionAction, 'run', dummy_run)
    mocker.patch.object(UpdateTransactionAction, '__init__', dummy_init)
    return mocker.spy(UpdateTransactionAction, '__init__').call_args_list


@pytest.fixture(autouse=True)
def trust_clear_mock(shop_type, trust_client_mocker):
    with trust_client_mocker(shop_type, 'payment_clear') as mock:
        yield mock


@pytest.fixture(autouse=True)
def action_clear_by_ids_mock(mock_action):
    return mock_action(ClearByIdsOrderAction)


@pytest.fixture(autouse=True)
def action_resize_order_mock(mock_action):
    return mock_action(ResizeOrderAction)


@pytest.fixture(autouse=True)
def trust_unhold_mock(shop_type, trust_client_mocker):
    with trust_client_mocker(shop_type, 'payment_unhold') as mock:
        yield mock


@pytest.fixture
def core_clear_unhold_order_mock(mock_action):
    return mock_action(CoreClearUnholdOrderAction)


@pytest.fixture
async def validated_items(storage, items, order):
    return [
        {
            "product_id": item.product_id,
            "amount": item.amount,
            "price": item.price
        }
        async for item in storage.item.get_for_order(uid=order.uid, order_id=order.order_id)
    ]


class TestCoreClearUnholdOrderAction(BaseAcquirerTest, BaseTestRequiresModeration):
    @pytest.fixture
    def trust_env(self):
        return TrustEnv.PROD

    @pytest.fixture(autouse=True)
    def get_order_trust_env_action_mock(self, mock_action, trust_env):
        return mock_action(GetOrderTrustEnvAction, trust_env)

    @pytest.fixture(params=('clear', 'unhold'))
    def operation(self, request):
        return request.param

    @pytest.fixture
    def context_data(self):
        return {}

    @pytest.fixture
    def multi_max_amount(self):
        return None

    @pytest.fixture
    def order_data_data(self, multi_max_amount):
        return without_none({
            'trust_template': 'desktop',
            'multi_max_amount': multi_max_amount,
        })

    @pytest.fixture
    def params(self, order, operation, context_data):
        data = {
            'uid': order.uid,
            'order_id': order.order_id,
            'operation': operation,
        }
        data.update(context_data)
        return data

    @pytest.fixture
    def action(self, params):
        return CoreClearUnholdOrderAction(**params)

    @pytest.fixture
    def returned_func(self, action):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.mark.asyncio
    @pytest.mark.parametrize('acquirer', [None])
    async def test_deny_with_no_acquirer(self, returned_func):
        with pytest.raises(CoreActionDenyError):
            await returned_func()

    class TestActionDeny(BaseTestRequiresModeration.TestActionDeny):
        @pytest.fixture(autouse=True)
        async def setup(self, storage, order):
            order.pay_status = PayStatus.HELD
            order.autoclear = False
            await storage.order.save(order)

        @pytest.fixture(params=TrustEnv)
        def trust_env(self, request):
            return request.param

        @pytest.mark.asyncio
        async def test_action_deny__raises_error(self, trust_env, returned_func, noop_manager):
            manager = pytest.raises(CoreActionDenyError) if trust_env == TrustEnv.PROD else noop_manager()
            with manager:
                await returned_func()

    class TestNotFound:
        @pytest.mark.parametrize('context_data', [
            {'uid': -1},
            {'order_id': -1},
        ])
        @pytest.mark.asyncio
        async def test_invalid_id(self, returned_func):
            with pytest.raises(CoreNotFoundError):
                await returned_func()

        @pytest.mark.usefixtures('order_with_customer_subscription')
        @pytest.mark.asyncio
        async def test_customer_subscription(self, returned_func):
            with pytest.raises(CoreNotFoundError):
                await returned_func()

    class TestFetchEntities:
        @pytest.fixture(autouse=True)
        def setup(self, storage, merchant, action):
            action.context.storage = storage
            action.merchant = merchant
            yield
            action.context.storage = None

        @pytest.mark.asyncio
        async def test_gets_last_transaction(self, storage, merchant, order, transaction, action):
            transaction.status = TransactionStatus.FAILED
            await storage.transaction.save(transaction)
            transaction = await storage.transaction.create(Transaction(
                uid=order.uid,
                order_id=order.order_id,
            ))
            assert await action._fetch_entities(order.uid, order.order_id) == (order, transaction)

        @pytest.mark.asyncio
        async def test_update_transaction_call(self, merchant, order, transaction, update_transaction_calls, action):
            await action._fetch_entities(order.uid, order.order_id)
            assert update_transaction_calls[0][1] == {
                'merchant': merchant,
                'transaction': transaction,
            }

    class TestCheckOrder:
        @pytest.fixture(autouse=True)
        def setup(self, merchant, storage, action):
            action.merchant = merchant
            action.context.storage = storage
            yield
            action.context.storage = None

        @pytest.mark.asyncio
        async def test_refund(self, action, order):
            order.kind = OrderKind.REFUND
            with pytest.raises(OrderCannotClearOrUnholdRefundError):
                await action._check_order(order)

        @pytest.mark.asyncio
        async def test_autoclear(self, action, order):
            order.autoclear = True
            with pytest.raises(OrderHasAutoclearOnError):
                await action._check_order(order)

        @pytest.mark.asyncio
        async def test_not_held_or_in_moderation(self, action, order):
            order.pay_status = PayStatus.IN_PROGRESS
            order.autoclear = False
            with pytest.raises(OrderPayStatusMustBeHeldOrInModerationError) as exc_info:
                await action._check_order(order)
            assert exc_info.value.params['pay_status'] == order.pay_status.value

    class TestValidateItems:
        @pytest.fixture(autouse=True)
        def setup(self, storage, action):
            action.context.storage = storage
            yield
            action.context.storage = None

        @pytest.fixture
        def params(self, validated_items, storage, order, operation, context_data):
            data = {
                'uid': order.uid,
                'order_id': order.order_id,
                'operation': operation,
                'items': validated_items
            }
            data.update(context_data)
            return data

        class TestInvalidAmount:
            @pytest.fixture
            async def validated_items(self, storage, items, order):
                return [
                    {
                        "product_id": item.product_id,
                        "amount": item.amount + Decimal("1.0"),
                        "price": item.price
                    }
                    async for item in storage.item.get_for_order(uid=order.uid, order_id=order.order_id)
                ]

            @pytest.mark.asyncio
            async def test_invalid_amount(self, storage, validated_items, params, action, returned_func, order):
                order.autoclear = False
                with pytest.raises(ItemsInvalidAmountDataError):
                    await action._fetch_validated_items(order, validated_items)

        class TestInvalidTotalPrice:
            @pytest.fixture
            async def validated_items(self, storage, items, order):
                return [
                    {
                        "product_id": item.product_id,
                        "amount": item.amount,
                        "price": item.price + Decimal("1.0")
                    }
                    async for item in storage.item.get_for_order(uid=order.uid, order_id=order.order_id)
                ]

            @pytest.mark.asyncio
            async def test_invalid_total_price(self, storage, validated_items, params, action, returned_func, order):
                order.autoclear = False
                with pytest.raises(ItemsInvalidTotalPriceDataError):
                    await action._fetch_validated_items(order, validated_items)

        class TestInvalidData:
            @pytest.fixture
            async def validated_items(self, storage, randn):
                return [
                    {
                        "product_id": randn(),
                        "amount": Decimal("1.0"),
                        "price": Decimal("1.0")
                    }
                ]

            @pytest.mark.asyncio
            async def test_invalid_data(self, storage, validated_items, params, action, returned_func, order):
                order.autoclear = False
                with pytest.raises(ItemsInvalidDataError):
                    await action._fetch_validated_items(order, validated_items)

    class TestAction:
        @pytest.fixture(autouse=True)
        def fetch_entities_mock(self, mocker, order, transaction, action):
            coro = dummy_coro((order, transaction))
            yield mocker.patch.object(action, '_fetch_entities', mocker.Mock(return_value=coro))
            coro.close()

        @pytest.fixture
        def check_order_exc(self):
            return None

        @pytest.fixture(autouse=True)
        def check_order_mock(self, mocker, action, check_order_exc):
            return mocker.patch.object(
                action,
                '_check_order',
                mocker.Mock(return_value=dummy_coro(exc=check_order_exc))
            )

        @pytest.fixture(autouse=True)
        async def disable_poll(self, storage, transaction):
            transaction.poll = False
            await storage.transaction.save(transaction)

        @pytest.mark.parametrize('check_order_exc', (CoreDataError,))
        @pytest.mark.asyncio
        async def test_check_order_failed(self, returned_func):
            with pytest.raises(CoreDataError):
                await returned_func()

        @pytest.mark.asyncio
        async def test_enables_transaction_poll(self, storage, transaction, returned):
            assert (await storage.transaction.get(transaction.uid, transaction.tx_id)).poll

        @pytest.mark.asyncio
        async def test_send_to_history_task_created(self, storage, order, returned):
            task = await (storage.task.find()).__anext__()
            assert all((
                task.action_name == 'send_to_history_order_action',
                task.params['action_kwargs'] == {'uid': order.uid, 'order_id': order.order_id},
            ))

        class TestClear:
            @pytest.fixture
            def operation(self):
                return 'clear'

            class TestHeld:
                @pytest.fixture(autouse=True)
                async def setup(self, storage, order):
                    order.pay_status = PayStatus.HELD
                    await storage.order.save(order)

                @parametrize_shop_type
                def test_held__clear_call(self, merchant, transaction, order, action_clear_by_ids_mock, returned):
                    action_clear_by_ids_mock.assert_called_once_with(
                        uid=transaction.uid,
                        order_id=order.order_id,
                        tx_id=transaction.tx_id
                    )

                @parametrize_shop_type
                def test_held__unhold_call(self, trust_unhold_mock, returned):
                    trust_unhold_mock.assert_not_called()

                class TestOrdinary:
                    def test_ordinary__order_status(self, merchant, transaction, order, action_clear_by_ids_mock,
                                                    returned):
                        assert order.pay_status == PayStatus.IN_PROGRESS

                    def test_ordinary__resize_call(self, action_resize_order_mock, returned):
                        action_resize_order_mock.assert_not_called()

                class TestResize:
                    @pytest.fixture
                    def expected_items(self, action, storage, validated_items, order):
                        return [
                            Item(
                                uid=order.uid,
                                order_id=order.order_id,
                                amount=item['amount'],
                                product_id=item['product_id'],
                                new_price=item['price']
                            ) for item in validated_items
                        ]

                    @pytest.fixture
                    def params(self, validated_items, storage, order, operation, context_data):
                        data = {
                            'uid': order.uid,
                            'order_id': order.order_id,
                            'operation': operation,
                            'items': validated_items
                        }
                        data.update(context_data)
                        return data

                    def test_resize__resize_call(self, transaction, order, expected_items,
                                                 action_resize_order_mock, returned):
                        action_resize_order_mock.assert_called_once_with(
                            transaction=transaction,
                            order=order,
                            items=expected_items
                        )

                    def test_resize__order_status(self, merchant, transaction, order, action_clear_by_ids_mock,
                                                  returned):
                        assert order.pay_status == PayStatus.IN_PROGRESS

            class TestInModeration:
                @pytest.fixture(autouse=True)
                async def setup(self, storage, order):
                    order.pay_status = PayStatus.IN_MODERATION
                    order.autoclear = False
                    await storage.order.save(order)

                @parametrize_shop_type
                def test_in_moderation__clear_call(self, action_clear_by_ids_mock, returned):
                    action_clear_by_ids_mock.assert_not_called()

                @parametrize_shop_type
                def test_in_moderation__unhold_call(self, trust_unhold_mock, returned):
                    trust_unhold_mock.assert_not_called()

                @pytest.mark.asyncio
                async def test_in_moderation__pay_status(self, storage, order, returned):
                    assert (await storage.order.get(order.uid, order.order_id)).pay_status == PayStatus.IN_MODERATION

                @pytest.mark.asyncio
                async def test_autoclear_set_true(self, storage, order, returned_func):
                    before = order.autoclear
                    await returned_func()
                    after = (await storage.order.get(order.uid, order.order_id)).autoclear
                    assert not before and after

        class TestUnhold:
            @pytest.fixture
            def operation(self):
                return 'unhold'

            @parametrize_shop_type
            def test_unhold__clear_call(self, action_clear_by_ids_mock, returned):
                action_clear_by_ids_mock.assert_not_called()

            @parametrize_shop_type
            def test_unhold__unhold_call(self, merchant, transaction, trust_unhold_mock, returned):
                trust_unhold_mock.assert_called_once_with(
                    uid=transaction.uid,
                    acquirer=merchant.acquirer,
                    purchase_token=transaction.trust_purchase_token,
                )

            @pytest.mark.asyncio
            async def test_unhold__pay_status(self, storage, order, returned):
                assert (await storage.order.get(order.uid, order.order_id)).pay_status == PayStatus.IN_CANCEL


class TestCoreScheduleClearUnholdOrderAction:
    @pytest.fixture(autouse=True)
    async def setup(self, storage, order):
        order.pay_status = PayStatus.HELD
        order.autoclear = False
        await storage.order.save(order)

    @pytest.fixture
    def params(self, order, items, validated_items):
        return {
            'uid': order.uid,
            'order_id': order.order_id,
            'operation': 'clear',
            'items': validated_items
        }

    @pytest.fixture
    async def returned(self, params):
        return await CoreScheduleClearUnholdOrderAction(**params).run()

    @pytest.mark.usefixtures('positive_merchant_moderation')
    def test_core_schedule_clear_unhold_order_action__clear_call(self, core_clear_unhold_order_mock, params, returned):
        core_clear_unhold_order_mock.assert_called_once_with(**params)

    @pytest.mark.asyncio
    @pytest.mark.usefixtures('positive_merchant_moderation')
    async def test_task(self, storage, params, returned):
        tasks = await alist(storage.task.find())
        assert_that(
            tasks,
            has_item(has_properties({
                'params': has_entries(
                    action_kwargs=params
                ),
                'action_name': 'core_clear_unhold_order_action',
                'task_type': TaskType.RUN_ACTION,
            }))
        )


class TestScheduleClearUnholdOrderAction:
    @pytest.fixture(autouse=True)
    async def setup(self, storage, order):
        order.pay_status = PayStatus.HELD
        order.autoclear = False
        await storage.order.save(order)

    @pytest.fixture
    def params(self, order, items, validated_items):
        return {
            'uid': order.uid,
            'order_id': order.order_id,
            'operation': 'clear',
        }

    @pytest.fixture
    async def returned(self, params):
        return await ScheduleClearUnholdOrderAction(**params).run()

    @pytest.mark.usefixtures('positive_merchant_moderation')
    def test_clear_unhold_order_action__clear_call(self, core_clear_unhold_order_mock, params, returned):
        core_clear_unhold_order_mock.assert_called_once_with(**{**params, 'items': None})
