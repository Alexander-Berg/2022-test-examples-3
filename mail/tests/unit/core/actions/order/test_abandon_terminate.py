import pytest

from sendr_utils import alist

from hamcrest import all_of, assert_that, contains_inanyorder, has_entries, has_properties, is_

from mail.payments.payments.core.actions.order.abandon_terminate import AbandonTerminateOrderAction
from mail.payments.payments.core.entities.enums import OperationKind, PayStatus, TaskType
from mail.payments.payments.core.entities.log import OrderUpdatedLog


class TestAbandonTerminateOrderAction:
    @pytest.fixture
    def params(self, order, items):
        return {
            'uid': order.uid,
            'order': order,
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await AbandonTerminateOrderAction(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    async def test_returned(self, storage, returned):
        updated = await storage.order.get(returned.uid, returned.order_id)
        assert updated.pay_status == PayStatus.ABANDONED

    @pytest.mark.asyncio
    async def test_changelog(self, storage, returned):
        assert_that(
            await alist(storage.change_log.find(returned.uid)),
            contains_inanyorder(
                has_properties({
                    'uid': returned.uid,
                    'revision': returned.revision,
                    'operation': OperationKind.UPDATE_ORDER,
                    'arguments': {'pay_status': PayStatus.ABANDONED.value, 'order_id': returned.order_id}
                })
            )
        )

    def test_logged(self, returned, pushers_mock):
        assert_that(
            pushers_mock.log.push.call_args[0][0],
            all_of(
                is_(OrderUpdatedLog),
                has_properties(dict(
                    status=PayStatus.ABANDONED.value,
                ))
            )
        )

    @pytest.mark.asyncio
    async def test_create_callback_task(self, order_with_service, storage, returned):
        assert_that(
            await alist(storage.task.find(TaskType.API_CALLBACK)),
            contains_inanyorder(*[
                has_properties({
                    'params': has_entries({
                        'message': has_entries({
                            'order_id': returned.order_id,
                            'new_status': PayStatus.ABANDONED.value
                        })
                    }),
                }) for _ in range(2)
            ])
        )

    @pytest.mark.asyncio
    async def test_send_to_history_task_created(self, storage, order, returned):
        task = await (storage.task.find()).__anext__()
        assert all((
            task.action_name == 'send_to_history_order_action',
            task.params['action_kwargs'] == {'uid': order.uid, 'order_id': order.order_id},
        ))
