import uuid
from datetime import datetime, timezone
from typing import Dict, Optional
from unittest import mock

import pytest

from sendr_utils import alist

from hamcrest import all_of, assert_that, contains, equal_to, has_entries, has_item, has_properties, is_

from mail.payments.payments.core.actions.interactions.trust import (
    ClearPaymentInTrustAction, GetPaymentInfoInTrustAction, UnholdPaymentInTrustAction
)
from mail.payments.payments.core.actions.update_transaction import UpdateTransactionAction
from mail.payments.payments.core.entities.enums import (
    FunctionalityType, ModerationType, OrderSource, PayStatus, TaskType, TransactionStatus
)
from mail.payments.payments.core.entities.log import OrderStatusUpdatedLog
from mail.payments.payments.core.entities.merchant import Merchant
from mail.payments.payments.core.entities.moderation import Moderation
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.task import Task
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.core.exceptions import CoreInteractionError
from mail.payments.payments.interactions.trust.exceptions import TrustException
from mail.payments.payments.storage.mappers.order.order import OrderMapper
from mail.payments.payments.tests.base import BaseTestParent
from mail.payments.payments.tests.utils import callback_tasks, dummy_coro_ctx
from mail.payments.payments.utils.datetime import utcnow
from mail.payments.payments.utils.helpers import without_none


@pytest.fixture(autouse=True)
def get_acquirer_mock(mock_action, merchant):
    from mail.payments.payments.core.actions.merchant.get_acquirer import GetAcquirerMerchantAction
    return mock_action(GetAcquirerMerchantAction, merchant.acquirer)


@pytest.fixture
def payment_info__user_email():
    return 'bob@example.com'


@pytest.fixture
def clear_real():
    return utcnow()


@pytest.fixture
def payment_resp(payment_info__user_email, clear_real):
    return {
        'payment_resp_code': 'a',
        'payment_resp_desc': 'b',
        'user_email': payment_info__user_email,
        'clear_real_ts': clear_real.timestamp(),
    }


@pytest.fixture(autouse=True)
def trust_payments_info_calls(mock_action, payment_resp):
    return mock_action(GetPaymentInfoInTrustAction, {'payment_status': '', **payment_resp}).call_args_list


@pytest.fixture(autouse=True)
def trust_payment_clear_calls(mock_action, payment_resp):
    return mock_action(ClearPaymentInTrustAction, payment_resp).call_args_list


@pytest.fixture(autouse=True)
def trust_unhold_calls(mock_action, payment_resp):
    return mock_action(UnholdPaymentInTrustAction, payment_resp).call_args_list


async def _correct_merchant_callback_task_params(transaction: Transaction, storage) -> Dict:
    """Построение корректных параметров задачки нотификации о статусе заказа по связанной обновленной транзакции."""
    order = await storage.order.get(uid=transaction.uid,
                                    order_id=transaction.order_id,
                                    select_customer_subscription=None)
    merchant = await storage.merchant.get(uid=transaction.uid)
    return {
        'callback_url': merchant.api_callback_url,
        'message': {
            'uid': merchant.uid,
            'order_id': order.order_id,
            'new_status': order.pay_status.value,
            'meta': order.data.meta,
            'updated': order.updated.isoformat(),
        }
    }


async def _start_order_moderation_task(storage) -> Optional[Task]:
    tasks = [t async for t in storage.task.find()]
    for t in tasks:
        if t.task_type == TaskType.START_ORDER_MODERATION:
            return t


async def _order_moderation(storage) -> Optional[Moderation]:
    moderations = [m async for m in storage.moderation.find()]
    for m in moderations:
        if m.moderation_type == ModerationType.ORDER:
            return m


@pytest.mark.parametrize('order_data__user_email', ['alice@example.com'])
@pytest.mark.parametrize('payment_info__user_email', ['bob@example.com'])
def test_maybe_update_user_email_method(order, payment_resp, order_data__user_email, payment_info__user_email):
    assert order_data__user_email != payment_info__user_email
    assert order.user_email == order_data__user_email
    updated = UpdateTransactionAction._maybe_update_user_email(order, payment_resp)
    assert updated and order.user_email == payment_info__user_email


@pytest.mark.parametrize('order_data__user_email', ['alice@example.com'])
@pytest.mark.parametrize('payment_info__user_email', [None])
def test_maybe_update_user_email_method_none_value(
    order, payment_resp, order_data__user_email, payment_info__user_email
):
    assert order_data__user_email != payment_info__user_email
    assert order.user_email == order_data__user_email
    updated = UpdateTransactionAction._maybe_update_user_email(order, payment_resp)
    assert not updated and order.user_email == order_data__user_email


@pytest.mark.parametrize('order_data__user_email', ['alice@example.com'])
@pytest.mark.parametrize('payment_resp', [{}])
def test_maybe_update_user_email_method_missing_key(
    order, payment_resp, order_data__user_email, payment_info__user_email
):
    assert order_data__user_email != payment_info__user_email
    assert order.user_email == order_data__user_email
    updated = UpdateTransactionAction._maybe_update_user_email(order, payment_resp)
    assert not updated and order.user_email == order_data__user_email


class BaseTestUpdateTransaction(BaseTestParent):
    @pytest.fixture
    async def order_moderation(self, storage, order: Order, transaction: Transaction) -> Moderation:
        """Создать модерацию для заказа и установить соответствующие статусы заказа и транзакции."""

        order.pay_status = PayStatus.IN_MODERATION
        await storage.order.save(order)

        transaction.status = TransactionStatus.HELD
        await storage.transaction.save(transaction)

        moderation = Moderation(uid=order.uid,
                                revision=order.revision,
                                moderation_type=ModerationType.ORDER,
                                entity_id=order.order_id)
        moderation = await storage.moderation.create(moderation)
        return moderation

    @pytest.fixture
    async def positive_merchant_moderation(self, storage, merchant: Merchant):
        for uid in [merchant.uid, merchant.parent_uid]:
            if uid:
                await storage.moderation.create(
                    Moderation(
                        uid=uid,
                        revision=merchant.revision,
                        moderation_type=ModerationType.MERCHANT,
                        functionality_type=FunctionalityType.PAYMENTS,
                        approved=True
                    )
                )

    @pytest.fixture
    async def positive_order_moderation(self, order_moderation, storage):
        order_moderation.approved = True
        moderation = await storage.moderation.save(order_moderation)
        return moderation

    @pytest.fixture
    async def negative_order_moderation(self, order_moderation, storage):
        order_moderation.approved = False
        moderation = await storage.moderation.save(order_moderation)
        return moderation

    @pytest.fixture
    async def pending_order_moderation(self, order_moderation, storage):
        order_moderation.approved = None
        moderation = await storage.moderation.save(order_moderation)
        return moderation

    @pytest.fixture
    def action_cls(self):
        return UpdateTransactionAction

    @pytest.fixture(autouse=True)
    def create_order_status_task_mock(self, mocker, action):
        return mocker.spy(action, 'create_order_status_task')

    @pytest.fixture
    def transaction_data(self):
        return {'trust_purchase_token': str(uuid.uuid4())}

    @pytest.fixture(autouse=True)
    async def set_merchant_callback_url(self, merchant, storage):
        merchant.api_callback_url = 'http://some.com'
        await storage.merchant.save(merchant)

    @pytest.fixture
    async def no_autoclear_order(self, storage, order):
        order.autoclear = False
        return await storage.order.save(order)

    @pytest.fixture(autouse=True)
    async def allowed_next_status(self, mocker):
        mocker.patch.object(TransactionStatus, 'allowed_next_status', mocker.Mock(return_value=True))

    @pytest.fixture
    def status_mocker(self, mocker):
        def _inner(status):
            return mocker.patch.object(TransactionStatus, 'from_trust', mocker.Mock(return_value=status))

        return _inner

    @pytest.fixture
    def params(self, transaction):
        return {'transaction': transaction}

    @pytest.fixture
    def action(self, action_cls, params):
        return action_cls(**params)

    @pytest.fixture
    def run_update_transaction(self, action):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.mark.parametrize('payment_info__user_email', ['bob@example.com'])
    @pytest.mark.asyncio
    async def test_updates_order_user_email_from_trust(self, order, storage, run_update_transaction):
        order.user_email = 'alice@example.com'
        await storage.order.save(order)
        await run_update_transaction()
        order = await storage.order.get(uid=order.uid, order_id=order.order_id, select_customer_subscription=None)
        assert order.user_email == 'bob@example.com'

    class TestSkipFetch:
        @pytest.fixture
        def transaction(self, transaction, order):
            transaction.order = order
            return transaction

        @pytest.fixture
        def spy_on_order_get(self, mocker):
            return mocker.spy(OrderMapper, 'get')

        @pytest.mark.asyncio
        async def test_skip_fetch_order_if_present_in_transaction(self, spy_on_order_get, run_update_transaction):
            await run_update_transaction()
            spy_on_order_get.assert_not_called()

    class TestUpdateTransaction:
        @pytest.mark.asyncio
        async def test_updates_on_not_allowed_status(self, run_update_transaction, mocker, storage, transaction):
            mocker.patch.object(TransactionStatus, 'allowed_next_status', mocker.Mock(return_value=False))

            now = utcnow()
            mocker.patch('mail.payments.payments.storage.mappers.transaction.utcnow', mocker.Mock(return_value=now))

            await run_update_transaction()
            tx = await storage.transaction.get(transaction.uid, transaction.tx_id)
            assert tx.updated == now

        @pytest.mark.parametrize('tx_status', [
            TransactionStatus.HELD,
            TransactionStatus.CLEARED,
            TransactionStatus.CANCELLED,
            TransactionStatus.FAILED,
        ])
        @pytest.mark.asyncio
        async def test_transaction_status(self, storage, run_update_transaction, transaction, status_mocker, tx_status):
            """Транзакция переводится в корректный статус по соответствующему статусу платежа из АПИ платежной
            системы."""
            status_mocker(tx_status)
            await run_update_transaction()
            tx = await storage.transaction.get(transaction.uid, transaction.tx_id)
            assert tx.status == tx_status

        @pytest.mark.asyncio
        async def test_transaction_failed_result(self, run_update_transaction, storage, transaction, payment_resp,
                                                 status_mocker):
            """Реакция на неудавшийся платеж сохранит в транзакции детали ответа платежной системы."""
            status_mocker(TransactionStatus.FAILED)
            await run_update_transaction()

            tx = await storage.transaction.get(transaction.uid, transaction.tx_id)
            code = payment_resp['payment_resp_code']
            desc = payment_resp['payment_resp_desc']
            assert tx.trust_failed_result == f'{code} :: {desc}'

    class TestTrustworthyMerchant:
        @pytest.fixture(autouse=True)
        async def setup(self, storage, merchant):
            merchant.trustworthy = True
            await storage.merchant.save(merchant)

        @pytest.mark.asyncio
        async def test_autoclear_trust_clear_call(self, run_update_transaction, trust_payment_clear_calls,
                                                  status_mocker,
                                                  transaction, storage):
            status_mocker(TransactionStatus.HELD)
            await run_update_transaction()
            order = await storage.order.get(
                uid=transaction.uid,
                order_id=transaction.order_id,
                select_customer_subscription=None
            )
            assert_that(
                trust_payment_clear_calls[0][1],
                has_entries({
                    'purchase_token': transaction.trust_purchase_token,
                    'order': order
                })
            )

        @pytest.mark.asyncio
        async def test_no_autoclear_trust_clear_call(self, run_update_transaction, trust_payment_clear_calls,
                                                     status_mocker, no_autoclear_order):
            status_mocker(TransactionStatus.HELD)
            await run_update_transaction()
            assert not trust_payment_clear_calls

        @pytest.mark.asyncio
        async def test_no_autoclear_polling_disabled(self, run_update_transaction, status_mocker, no_autoclear_order):
            status_mocker(TransactionStatus.HELD)
            transaction = await run_update_transaction()
            assert not transaction.poll

    class TestOrderModerationNotRequired:
        @pytest.fixture(autouse=True)
        async def initial_state(self, mocker, status_mocker, order_with_service):
            status_mocker(TransactionStatus.HELD)
            mocker.patch.object(UpdateTransactionAction,
                                '_merchant_moderation_enabled',
                                mocker.Mock(return_value=True))

        @pytest.mark.asyncio
        async def test_moderation_no_required_order_pay_status(self, run_update_transaction, storage):
            """Заказ получил pay_status = IN_PROGRESS"""
            transaction = await run_update_transaction()
            order = await storage.order.get(uid=transaction.uid,
                                            order_id=transaction.order_id,
                                            select_customer_subscription=None)
            assert order.pay_status == PayStatus.IN_PROGRESS

    class TestStateChangeOnTransactionHeldWhenModerationRequired:
        """Тестируем переход заказа в состояние модерации с точки зрения изменения состояния.
        Исходное состояние:
            заказ без модерации
            транзакция [в статусе active]
            вызов в апи траста возвращает статус HELD
        """

        @pytest.fixture
        def target_order_pay_status(self):
            return PayStatus.IN_MODERATION

        @pytest.fixture(autouse=True)
        async def initial_state(self, mocker, status_mocker):
            status_mocker(TransactionStatus.HELD)
            mocker.patch.object(UpdateTransactionAction, '_merchant_moderation_enabled', mocker.Mock(return_value=True))

        @pytest.mark.asyncio
        async def test_state_change_transaction_status(self, run_update_transaction):
            """Транзакция переведена в статус HELD."""
            transaction = await run_update_transaction()
            assert transaction.status == TransactionStatus.HELD

        @pytest.mark.asyncio
        async def test_state_change_order_pay_status(self, run_update_transaction, target_order_pay_status, storage):
            """Заказ получил pay_status = IN_MODERATION"""
            transaction = await run_update_transaction()
            order = await storage.order.get(uid=transaction.uid,
                                            order_id=transaction.order_id,
                                            select_customer_subscription=None)
            assert order.pay_status == target_order_pay_status

        @pytest.mark.asyncio
        async def test_moderation_created(self, run_update_transaction, storage):
            """Создана запись модерации заказа."""
            transaction = await run_update_transaction()
            order = await storage.order.get(uid=transaction.uid,
                                            order_id=transaction.order_id,
                                            select_customer_subscription=None)
            moderation = await _order_moderation(storage)
            assert_that(moderation, has_properties({
                'uid': transaction.uid,
                'entity_id': order.order_id,
                'moderation_type': ModerationType.ORDER
            }))

        @pytest.mark.asyncio
        async def test_start_moderation_task_created(self, run_update_transaction, storage):
            """Создана задача на отправку модерации заданного заказа."""
            await run_update_transaction()
            moderation = await _order_moderation(storage)
            task = await _start_order_moderation_task(storage)
            assert_that(task, has_properties({
                'task_type': TaskType.START_ORDER_MODERATION,
                'params': dict(moderation_id=moderation.moderation_id),
            }))

        @pytest.mark.asyncio
        async def test_state_change_callback_task_created(self, run_update_transaction, storage):
            transaction = await run_update_transaction()
            tasks = await callback_tasks(storage)
            params = await _correct_merchant_callback_task_params(transaction=transaction, storage=storage)
            assert_that(tasks, has_item(has_properties({'params': has_entries(**params)})))

    @pytest.mark.usefixtures('positive_merchant_moderation')
    class TestStateChangeOnModerationResult:
        class BaseTestOnPositiveModerationResult:
            async def check_on_positive_moderation_transaction_status(self, run_update_transaction):
                """Транзакция осталась в статусе HELD."""
                transaction = await run_update_transaction()
                assert transaction.status == TransactionStatus.HELD

            async def check_on_positive_moderation_callback_task_created(self, run_update_transaction, storage):
                transaction = await run_update_transaction()
                tasks = await callback_tasks(storage)
                params = await _correct_merchant_callback_task_params(transaction=transaction, storage=storage)
                assert_that(tasks, has_item(has_properties({'params': has_entries(**params)})))

        @pytest.mark.usefixtures('positive_order_moderation')
        class TestOnPositiveModerationResultAutoclear(BaseTestOnPositiveModerationResult):
            """Тестируем реакцию на положительную модерацию заказа с точки зрения изменения состояния.

            Исходное состояние:
                заказ в статусе IN_MODERATION
                транзакция в статусе HELD
                положительная модерация для заказа в базе
            """

            @pytest.mark.asyncio
            async def test_autoclear__on_positive_moderation_transaction_status(self, run_update_transaction):
                await self.check_on_positive_moderation_transaction_status(run_update_transaction)

            @pytest.mark.asyncio
            async def test_autoclear__on_positive_moderation_order_pay_status(self, run_update_transaction, storage):
                transaction = await run_update_transaction()
                order = await storage.order.get(uid=transaction.uid,
                                                order_id=transaction.order_id,
                                                select_customer_subscription=None)
                assert order.pay_status == PayStatus.IN_PROGRESS

            @pytest.mark.asyncio
            async def test_autoclear__on_positive_moderation_trust_clear_call(self, run_update_transaction, transaction,
                                                                              trust_payment_clear_calls, storage):
                """Clear is requested in trust"""
                await run_update_transaction()
                order = await storage.order.get(
                    uid=transaction.uid,
                    order_id=transaction.order_id,
                    select_customer_subscription=None
                )
                assert_that(
                    trust_payment_clear_calls[0][1],
                    has_entries(purchase_token=transaction.trust_purchase_token, order=order)
                )

            @pytest.mark.asyncio
            async def test_autoclear__on_positive_moderation_callback_task_created(self, run_update_transaction,
                                                                                   storage):
                await self.check_on_positive_moderation_callback_task_created(run_update_transaction, storage)

        @pytest.mark.usefixtures('positive_order_moderation', 'no_autoclear_order')
        class TestOnPositiveModerationResultNoAutoclear(BaseTestOnPositiveModerationResult):
            @pytest.mark.asyncio
            async def test_no_autoclear__on_positive_moderation_transaction_status(self, run_update_transaction):
                await self.check_on_positive_moderation_transaction_status(run_update_transaction)

            @pytest.mark.asyncio
            async def test_no_autoclear__on_positive_moderation_callback_task_created(self, run_update_transaction,
                                                                                      storage):
                await self.check_on_positive_moderation_callback_task_created(run_update_transaction, storage)

            @pytest.mark.asyncio
            async def test_no_autoclear__on_positive_moderation_order_pay_status(self, run_update_transaction, storage):
                transaction = await run_update_transaction()
                order = await storage.order.get(uid=transaction.uid,
                                                order_id=transaction.order_id,
                                                select_customer_subscription=None)
                assert order.pay_status == PayStatus.HELD

            @pytest.mark.asyncio
            async def test_no_autoclear__on_positive_moderation_trust_clear_call(self, run_update_transaction,
                                                                                 trust_payment_clear_calls,
                                                                                 order, transaction):
                """Clear is not requested in trust"""
                await run_update_transaction()
                assert not trust_payment_clear_calls

            @pytest.mark.asyncio
            async def test_no_autoclear__transaction_polling_disabled(self, run_update_transaction):
                transaction = await run_update_transaction()
                assert not transaction.poll

        @pytest.mark.usefixtures('negative_order_moderation')
        class BaseTestOnNegativeModerationResultAutoclear:
            """Тестируем реакцию на отрицательную модерацию заказа с точки зрения изменения состояния.

            Исходное состояние:
                заказ в статусе IN_MODERATION
                транзакция в статусе HELD
                отрицательная модерация для заказа в базе
            """

            async def check_on_negative_moderation_transaction_status(self, run_update_transaction):
                transaction = await run_update_transaction()
                assert transaction.status == TransactionStatus.HELD

            async def check_order_pay_status(self, run_update_transaction, storage):
                transaction = await run_update_transaction()
                order = await storage.order.get(uid=transaction.uid,
                                                order_id=transaction.order_id,
                                                select_customer_subscription=None)
                assert order.pay_status == PayStatus.MODERATION_NEGATIVE

            async def check_trust_unhold_call(self, run_update_transaction, transaction, trust_unhold_calls, storage):
                await run_update_transaction()
                order = await storage.order.get(uid=transaction.uid,
                                                order_id=transaction.order_id,
                                                select_customer_subscription=None)
                assert_that(trust_unhold_calls[0][1],
                            has_entries({
                                'purchase_token': transaction.trust_purchase_token,
                                'order': order
                            }))

            async def check_on_negative_moderation_callback_task_created(self, run_update_transaction, storage):
                transaction = await run_update_transaction()
                tasks = await callback_tasks(storage)
                params = await _correct_merchant_callback_task_params(transaction=transaction, storage=storage)
                assert_that(tasks, has_item(has_properties({'params': has_entries(params)})))

        class TestOnNegativeModerationResultAutoclear(BaseTestOnNegativeModerationResultAutoclear):
            @pytest.mark.asyncio
            async def test_autoclear__on_negative_moderation_transaction_status(self, run_update_transaction):
                await self.check_on_negative_moderation_transaction_status(run_update_transaction)

            @pytest.mark.asyncio
            async def test_autoclear__order_pay_status(self, run_update_transaction, storage):
                await self.check_order_pay_status(run_update_transaction, storage)

            @pytest.mark.asyncio
            async def test_autoclear__trust_unhold_call(self, run_update_transaction, transaction, trust_unhold_calls,
                                                        storage):
                await self.check_trust_unhold_call(run_update_transaction, transaction, trust_unhold_calls, storage)

            @pytest.mark.asyncio
            async def test_autoclear__on_negative_moderation_callback_task_created(self, run_update_transaction,
                                                                                   storage):
                await self.check_on_negative_moderation_callback_task_created(run_update_transaction, storage)

        @pytest.mark.usefixtures('negative_order_moderation', 'no_autoclear_order')
        class TestOnNegativeModerationResultNoAutoclear(BaseTestOnNegativeModerationResultAutoclear):
            @pytest.mark.asyncio
            async def test_no_autoclear__on_negative_moderation_transaction_status(self, run_update_transaction):
                await self.check_on_negative_moderation_transaction_status(run_update_transaction)

            @pytest.mark.asyncio
            async def test_no_autoclear__order_pay_status(self, run_update_transaction, storage):
                await self.check_order_pay_status(run_update_transaction, storage)

            @pytest.mark.asyncio
            async def test_no_autoclear__trust_unhold_call(self,
                                                           run_update_transaction,
                                                           transaction,
                                                           trust_unhold_calls,
                                                           storage):
                await self.check_trust_unhold_call(run_update_transaction, transaction, trust_unhold_calls, storage)

            @pytest.mark.asyncio
            async def test_no_autoclear__on_negative_moderation_callback_task_created(self, run_update_transaction,
                                                                                      storage):
                await self.check_on_negative_moderation_callback_task_created(run_update_transaction, storage)

        @pytest.mark.usefixtures('pending_order_moderation')
        class TestOnPendingModerationResult:
            """Тестируем реакцию на ожидаемую модерацию заказа с точки зрения изменения состояния - оно не меняется.

            Исходное состояние:
                заказ в статусе IN_MODERATION
                транзакция в статусе HELD
                ожидаемая модерация для заказа в базе (approved is None)
            """

            @pytest.mark.asyncio
            async def test_on_pending_moderation_transaction_status(self, run_update_transaction):
                """Транзакция осталась в статусе HELD."""
                transaction = await run_update_transaction()
                assert transaction.status == TransactionStatus.HELD

            @pytest.mark.asyncio
            async def test_on_pending_moderation_order_pay_status(self, run_update_transaction, storage):
                """Заказ остался в статусе IN_MODERATION."""
                transaction = await run_update_transaction()
                order = await storage.order.get(uid=transaction.uid,
                                                order_id=transaction.order_id,
                                                select_customer_subscription=None)
                assert order.pay_status == PayStatus.IN_MODERATION

    class TestUpdateTransactionUpdatedCheck:
        @staticmethod
        def _dummy_check_update(check_at, check_tries):
            def _inner(transaction):
                transaction.check_at = check_at
                transaction.check_tries = check_tries

            return _inner

        @pytest.fixture
        def updated(self):
            return True

        @pytest.fixture(autouse=True)
        def setup_updated(self, action_cls, mocker, updated):
            with dummy_coro_ctx(updated) as coro:
                mock = mocker.Mock(return_value=coro)
                mocker.patch.object(action_cls, '_when_order_in_moderation', mock)
                mocker.patch.object(action_cls, '_sync_transaction_status', mock)
                yield

        @pytest.fixture(autouse=True)
        def setup_spy(self, mocker):
            mocker.spy(Transaction, 'increment_check_tries')
            mocker.spy(Transaction, 'reset_check_tries')

        @pytest.mark.parametrize('updated', (True, False))
        @pytest.mark.asyncio
        async def test_calls_check_methods(self, run_update_transaction, updated):
            await run_update_transaction()
            assert all((
                Transaction.reset_check_tries.call_count == int(updated),
                Transaction.increment_check_tries.call_count == int(not updated),
            ))

        @pytest.mark.parametrize('updated,method', (
            (False, 'increment_check_tries'),
            (True, 'reset_check_tries'),
        ))
        @pytest.mark.asyncio
        async def test_increments_when_not_updated(self, mocker, storage, run_update_transaction, transaction, method):
            check_at = datetime(2019, 2, 3, 4, 5, 6, tzinfo=timezone.utc)
            check_tries = 11
            mocker.patch.object(
                Transaction,
                method,
                self._dummy_check_update(check_at=check_at, check_tries=check_tries),
            )
            transaction = await run_update_transaction()
            assert all((
                transaction.check_at == check_at,
                transaction.check_tries == check_tries,
            ))

    class TestOrderStatus:
        @pytest.fixture(params=(
            (TransactionStatus.HELD, PayStatus.IN_MODERATION),
            (TransactionStatus.CLEARED, PayStatus.PAID),
            (TransactionStatus.FAILED, PayStatus.REJECTED),
        ))
        def tx_status_order_status(self, request):
            return request.param

        @pytest.mark.asyncio
        async def test_order_status(self,
                                    storage,
                                    transaction,
                                    run_update_transaction,
                                    status_mocker,
                                    tx_status_order_status,
                                    ):
            """Заказ переводится в корректный статус по соответствующему статусу платежа из платежной системы."""
            tx_status, order_status = tx_status_order_status
            status_mocker(tx_status)
            await run_update_transaction()
            order = await storage.order.get(uid=transaction.uid,
                                            order_id=transaction.order_id,
                                            select_customer_subscription=None)
            assert order.pay_status == order_status

        class TestOrderStatusTrustworthy:
            @pytest.fixture(autouse=True)
            async def setup(self, storage, merchant):
                merchant.trustworthy = True
                await storage.merchant.save(merchant)

            @pytest.fixture(params=(
                (TransactionStatus.HELD, PayStatus.IN_PROGRESS),
                (TransactionStatus.CLEARED, PayStatus.PAID),
                (TransactionStatus.FAILED, PayStatus.REJECTED),
            ))
            def tx_status_order_status(self, request):
                return request.param

    class TestOrderPaymethodId:
        @pytest.fixture
        def payment_resp(self, payment_info__user_email, clear_real):
            return {
                'payment_resp_code': 'a',
                'payment_resp_desc': 'b',
                'user_email': payment_info__user_email,
                'paymethod_id': 'trust_web_page',
                'clear_real_ts': clear_real.timestamp(),
            }

        @pytest.mark.asyncio
        async def test_order_paymethod_id(self,
                                          storage,
                                          transaction,
                                          run_update_transaction,
                                          status_mocker,
                                          ):
            tx_status = TransactionStatus.CLEARED
            status_mocker(tx_status)
            await run_update_transaction()
            order = await storage.order.get(uid=transaction.uid,
                                            order_id=transaction.order_id,
                                            select_customer_subscription=None)
            assert order.paymethod_id == 'trust_web_page'

    class TestCreateCallbackTaskCalled:
        @pytest.fixture
        def params(self, params, merchant):
            params['merchant'] = merchant
            return params

        @pytest.mark.parametrize('tx_status', [
            TransactionStatus.HELD,
            TransactionStatus.CLEARED,
            TransactionStatus.CANCELLED,
            TransactionStatus.FAILED,
        ])
        @pytest.mark.asyncio
        async def test_create_callback_task_called(self,
                                                   storage,
                                                   run_update_transaction,
                                                   order_with_service,
                                                   service_with_related,
                                                   status_mocker,
                                                   tx_status,
                                                   merchant,
                                                   create_order_status_task_mock,
                                                   ):
            status_mocker(tx_status)
            await run_update_transaction()

            order_with_service = await storage.order.get(uid=order_with_service.uid,
                                                         order_id=order_with_service.order_id,
                                                         select_customer_subscription=None)
            order_with_service.items = await alist(storage.item.get_for_order(uid=order_with_service.uid,
                                                                              order_id=order_with_service.order_id))

            create_order_status_task_mock.assert_has_calls((
                mock.call(order=order_with_service, service=service_with_related),
                mock.call(order=order_with_service, merchant=merchant),
            ))

    @pytest.mark.asyncio
    async def test_order_closed_timestamp(self, storage, transaction, run_update_transaction, status_mocker,
                                          clear_real):
        status_mocker(TransactionStatus.CLEARED)
        await run_update_transaction()

        order = await storage.order.get(uid=transaction.uid,
                                        order_id=transaction.order_id,
                                        select_customer_subscription=None)
        assert order.closed == clear_real

    @pytest.mark.asyncio
    async def test_order_send_to_tlog(self, storage, transaction, run_update_transaction, status_mocker, order):
        status_mocker(TransactionStatus.CLEARED)
        await run_update_transaction()

        tasks = await alist(storage.task.find())
        task = next((t for t in tasks if t.action_name == 'export_order_transaction_to_tlog'), None)
        assert all((
            task is not None,
            task.params['action_kwargs'] == {'uid': order.uid, 'tx_id': transaction.tx_id},
        ))

    @pytest.mark.asyncio
    async def test_order_held_at_timestamp(self, mocker, storage, transaction, run_update_transaction, status_mocker):
        now = utcnow()
        mocker.patch('mail.payments.payments.core.actions.update_transaction.utcnow', mocker.Mock(return_value=now))

        status_mocker(TransactionStatus.HELD)
        await run_update_transaction()

        order = await storage.order.get(uid=transaction.uid,
                                        order_id=transaction.order_id,
                                        select_customer_subscription=None)
        assert order.held_at == now

    @pytest.mark.parametrize('tx_status', [
        TransactionStatus.HELD,
        TransactionStatus.CLEARED,
        TransactionStatus.CANCELLED,
        TransactionStatus.FAILED,
    ])
    @pytest.mark.asyncio
    async def test_callback_task_created(self, storage, transaction, run_update_transaction, status_mocker, tx_status):
        """После обновления статуса заказа создается корректная задача на уведомление продавца об изменнии статуса."""
        status_mocker(tx_status)
        await run_update_transaction()

        params = await _correct_merchant_callback_task_params(transaction=transaction, storage=storage)
        tasks = await callback_tasks(storage)
        assert_that(tasks, has_item(has_properties({'params': has_entries(params)})))

    @pytest.mark.asyncio
    async def test_cancel_order_on_disabled_service_merchant(self,
                                                             storage,
                                                             status_mocker,
                                                             run_update_transaction,
                                                             service_merchant,
                                                             order_with_service):
        service_merchant.enabled = False
        await storage.service_merchant.save(service_merchant)

        status_mocker(TransactionStatus.HELD)
        await run_update_transaction()
        order_with_service = await storage.order.get(uid=order_with_service.uid,
                                                     order_id=order_with_service.order_id,
                                                     select_customer_subscription=None)
        assert order_with_service.pay_status == PayStatus.IN_CANCEL

    @pytest.mark.parametrize('new_status', [TransactionStatus.ACTIVE, TransactionStatus.HELD])
    @pytest.mark.asyncio
    async def test_returns_updated_transaction(self, transaction, run_update_transaction, status_mocker, new_status):
        status_mocker(new_status)
        updated_tx = await run_update_transaction()
        assert all([
            updated_tx.uid == transaction.uid,
            updated_tx.tx_id == transaction.tx_id,
            updated_tx.revision > transaction.revision,
        ])

    @pytest.mark.asyncio
    async def test_order_status_updated_logged(self, storage, merchant, order, transaction, status_mocker,
                                               run_update_transaction, pushers_mock):
        status_mocker(TransactionStatus.HELD)
        await run_update_transaction()
        updated_order = await storage.order.get(uid=order.uid, order_id=order.order_id,
                                                select_customer_subscription=None)
        updated_order.items = await alist(storage.item.get_for_order(uid=order.uid, order_id=order.order_id))
        assert_that(
            pushers_mock.log.push.call_args[0][0],
            all_of(
                is_(OrderStatusUpdatedLog),
                has_properties(dict(
                    merchant_name=merchant.name,
                    merchant_uid=merchant.uid,
                    merchant_acquirer=merchant.acquirer,
                    order_id=order.order_id,
                    purchase_token=transaction.trust_purchase_token,
                    status=updated_order.pay_status.value,
                    customer_uid=order.customer_uid,
                    service_id=None,
                    service_name=None,
                    sdk_api_created=order.created_by_source == OrderSource.SDK_API,
                    sdk_api_pay=order.pay_by_source == OrderSource.SDK_API,
                    created_by_source=order.created_by_source,
                    pay_by_source=order.pay_by_source,
                    price=updated_order.log_price
                )),
            ),
        )

    @pytest.mark.asyncio
    async def test_send_to_history_task_created(self, storage, order, run_update_transaction, status_mocker):
        status_mocker(TransactionStatus.HELD)
        await run_update_transaction()
        tasks = await alist(storage.task.find())
        task = next((t for t in tasks if t.action_name == 'send_to_history_order_action'), None)
        assert all((
            task is not None,
            task.params['action_kwargs'] == {'uid': order.uid, 'order_id': order.order_id},
        ))


class TestUpdateTransactionOfOrder(BaseTestUpdateTransaction):
    @pytest.mark.usefixtures('positive_merchant_moderation', 'positive_order_moderation')
    class TestMultiOrder:
        @pytest.fixture
        def multi_max_amount(self):
            return None

        @pytest.fixture
        def order_data_data(self, multi_max_amount):
            return without_none({
                'trust_template': 'desktop',
                'multi_max_amount': multi_max_amount
            })

        @pytest.mark.asyncio
        async def test_issued_inc(self, storage, multi_order, run_update_transaction, transaction, trust_unhold_calls,
                                  trust_payment_clear_calls):
            multi_order = await storage.order.get(multi_order.uid, multi_order.order_id)
            multi_issued_before = multi_order.data.multi_issued

            await run_update_transaction()

            order = await storage.order.get(uid=transaction.uid, order_id=transaction.order_id)
            multi_order = await storage.order.get(multi_order.uid, multi_order.order_id)
            multi_issued_after = multi_order.data.multi_issued

            assert_that(
                (trust_unhold_calls, trust_payment_clear_calls[0][1], multi_issued_before, multi_issued_after),
                contains(([]), has_entries({'purchase_token': transaction.trust_purchase_token, 'order': order}), 0, 1)
            )

        @pytest.mark.parametrize('multi_max_amount', (0,))
        @pytest.mark.asyncio
        async def test_unhold_on_clear(self, storage, multi_order, run_update_transaction, trust_payment_clear_calls,
                                       trust_unhold_calls, transaction):
            await run_update_transaction()
            order = await storage.order.get(uid=transaction.uid, order_id=transaction.order_id)

            assert_that(
                (trust_unhold_calls[0][1], trust_payment_clear_calls),
                contains(has_entries({'purchase_token': transaction.trust_purchase_token, 'order': order}), ([]))
            )

    @pytest.mark.parametrize('exception', (
        TrustException(method='whatever'),
        CoreInteractionError(),
    ))
    class TestOnInteractionException:
        @pytest.fixture(autouse=True)
        def mock_update_transaction(self, mocker, action_cls, exception):
            return mocker.patch.object(action_cls, '_update_transaction', mocker.Mock(side_effect=exception))

        @pytest.mark.asyncio
        async def test_increments_retries(self, storage, run_update_transaction, exception, transaction):
            check_tries = transaction.check_tries
            await run_update_transaction()
            updated_transaction = await storage.transaction.get(transaction.uid, transaction.tx_id)
            assert_that(
                updated_transaction.check_tries,
                equal_to(check_tries + 1)
            )


@pytest.mark.usefixtures('order_with_customer_subscription')
class TestUpdateTransactionOfOrderWithCustomerSubscription(BaseTestUpdateTransaction):
    class TestOrderStatus(BaseTestUpdateTransaction.TestOrderStatus):
        @pytest.fixture(params=(
            (TransactionStatus.HELD, PayStatus.IN_PROGRESS),
            (TransactionStatus.CLEARED, PayStatus.PAID),
            (TransactionStatus.FAILED, PayStatus.REJECTED),
        ))
        def tx_status_order_status(self, request):
            return request.param

    class TestStateChangeOnTransactionHeldWhenModerationRequired(
        BaseTestUpdateTransaction.TestStateChangeOnTransactionHeldWhenModerationRequired
    ):
        @pytest.fixture
        def target_order_pay_status(self):
            return PayStatus.IN_PROGRESS

        @pytest.mark.asyncio
        async def test_moderation_created(self, run_update_transaction, storage):
            """Не создана запись модерации заказа."""
            await run_update_transaction()
            moderation = await _order_moderation(storage)
            assert moderation is None

        @pytest.mark.asyncio
        async def test_start_moderation_task_created(self, run_update_transaction, storage):
            """Несоздана задача на отправку модерации заданного заказа."""
            await run_update_transaction()
            task = await _start_order_moderation_task(storage)
            assert task is None
