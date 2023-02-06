from copy import copy

import pytest

from sendr_taskqueue.worker.storage import TaskState
from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, has_entries, has_item, has_properties, is_not

from mail.payments.payments.core.actions.update_refund import UpdateRefundAction
from mail.payments.payments.core.entities.enums import RefundStatus, TaskType
from mail.payments.payments.tests.base import BaseOrderAcquirerTest, BaseTestParent, parametrize_shop_type


class TestUpdateRefund(BaseTestParent, BaseOrderAcquirerTest):
    @pytest.fixture(autouse=True)
    def get_acquirer_mock(self, mock_action, acquirer):
        from mail.payments.payments.core.actions.merchant.get_acquirer import GetAcquirerMerchantAction
        return mock_action(GetAcquirerMerchantAction, acquirer)

    @pytest.fixture
    def trust_refund_status(self):
        return 'success'

    @pytest.fixture(autouse=True)
    def refund_get_mock(self, shop_type, trust_client_mocker, trust_refund_status):
        with trust_client_mocker(shop_type, 'refund_get', {'status': trust_refund_status}) as mock:
            yield mock

    @pytest.fixture
    def existing_refunds_data(self):
        return [{'refund_status': RefundStatus.REQUESTED}]

    @pytest.fixture
    def refund(self, existing_refunds):
        return existing_refunds[0]

    @pytest.fixture
    def refund_before(self, refund):
        return copy(refund)

    @pytest.fixture
    def params(self, refund):
        return {'refund': refund}

    @pytest.fixture
    async def returned(self, params):
        return await UpdateRefundAction(**params).run()

    @pytest.fixture
    async def updated_refund(self, storage, refund, returned):
        return await storage.order.get(refund.uid, refund.order_id)

    class TestRefundNotRequested:
        @pytest.fixture
        def existing_refunds_data(self):
            return [{'refund_status': RefundStatus.CREATED}]

        @parametrize_shop_type
        def test_refund_get_not_called(self, refund_get_mock, returned):
            refund_get_mock.assert_not_called()

        def test_returns_unchanged(self, refund_before, returned):
            assert returned == refund_before

        def test_does_not_update(self, refund_before, updated_refund):
            assert updated_refund == refund_before

        @pytest.mark.asyncio
        async def test_no_callback_scheduled(self, storage, returned):
            tasks = [t async for t in storage.task.find()]
            assert_that(
                tasks,
                is_not(has_item(has_properties({
                    'task_type': TaskType.API_CALLBACK,
                })))
            )

    class TestRefundNotReady:
        @pytest.fixture
        def trust_refund_status(self):
            return 'wait_for_notification'

        def test_refund_not_ready__returns_updated(self, refund_before, returned):
            assert returned.updated > refund_before.updated and returned.revision > refund_before.revision

        def test_refund_not_ready__returns(self, refund_before, returned):
            refund_before.updated = returned.updated
            refund_before.revision = returned.revision
            assert returned == refund_before

        def test_refund_not_ready__updates(self, returned, updated_refund):
            assert updated_refund == returned

        @parametrize_shop_type
        def test_refund_not_ready__refund_get_call(self, refund, refund_get_mock, returned, order_acquirer):
            refund_get_mock.assert_called_once_with(
                uid=refund.uid,
                acquirer=order_acquirer,
                refund_id=refund.trust_refund_id
            )

    class TestRefundReady:
        @pytest.fixture(params=('success', 'fail'))
        def trust_refund_status(self, request):
            return request.param

        @pytest.fixture
        def expected_status(self, trust_refund_status):
            return RefundStatus.from_trust(trust_refund_status)

        def test_refund_ready__returns_updated(self, refund_before, returned, expected_status):
            assert all([
                returned.updated > refund_before.updated,
                returned.revision > refund_before.revision,
                returned.refund_status == expected_status,
            ])

        def test_refund_ready__returns(self, refund_before, returned, expected_status):
            refund_before.updated = returned.updated
            refund_before.revision = returned.revision
            refund_before.closed = returned.closed
            refund_before.items = returned.items
            refund_before.refund_status = expected_status
            assert returned == refund_before

        def test_refund_ready__updates(self, returned, updated_refund):
            updated_refund.items = returned.items
            assert updated_refund == returned

        def test_push_call(self, merchant, pushers_order_calls, returned):
            assert_that(
                pushers_order_calls[0][0][0],
                has_properties({
                    'merchant_name': merchant.name,
                    'merchant_uid': merchant.uid,
                    'refund_id': returned.order_id,
                    'order_id': returned.original_order_id,
                    'status': returned.refund_status.value,
                    'price': returned.log_price
                })
            )

        @parametrize_shop_type
        def test_refund_ready__refund_get_call(self, refund, refund_get_mock, returned, order_acquirer):
            refund_get_mock.assert_called_once_with(
                uid=refund.uid,
                acquirer=order_acquirer,
                refund_id=refund.trust_refund_id
            )

        @pytest.mark.asyncio
        async def test_merchant_callback_scheduled(self, storage, merchant, updated_refund):
            tasks = [t async for t in storage.task.find()]
            assert_that(
                tasks,
                has_item(has_properties({
                    'task_type': TaskType.API_CALLBACK,
                    'params': has_entries(callback_url=merchant.api_callback_url)
                }))
            )

        @pytest.mark.asyncio
        async def test_service_callback_scheduled(self, storage, merchant, service_client, service_merchant, refund,
                                                  params):
            refund.service_client_id = service_client.service_client_id
            refund.service_merchant_id = service_merchant.service_merchant_id
            await storage.order.save(refund)
            await UpdateRefundAction(**params).run()

            tasks = [t async for t in storage.task.find()]
            assert_that(
                tasks,
                has_item(has_properties({
                    'task_type': TaskType.API_CALLBACK,
                    'params': has_entries(callback_url=service_client.api_callback_url)
                }))
            )

        @pytest.mark.asyncio
        async def test_send_to_history_task_created(self, storage, refund, returned):
            tasks = await alist(storage.task.find())
            task = next((t for t in tasks if t.action_name == 'send_to_history_order_action'), None)
            assert all((
                task is not None,
                task.params['action_kwargs'] == {'uid': refund.uid, 'order_id': refund.original_order_id},
            ))

        @pytest.mark.asyncio
        @pytest.mark.parametrize('trust_refund_status', ('success',))
        async def test_refund_send_to_tlog(self, expected_status, storage, refund, returned):
            tasks = await alist(storage.task.find())
            task = next((t for t in tasks if t.action_name == 'export_refund_to_tlog'), None)

            assert all((
                task is not None,
                task and task.params['action_kwargs'] == {'uid': refund.uid, 'order_id': refund.order_id}
            ))

        @pytest.mark.asyncio
        @pytest.mark.parametrize('trust_refund_status', ('fail',))
        async def test_refund_not_send_to_tlog(self, expected_status, storage, refund, returned):
            tasks = await alist(storage.task.find())
            task = next((t for t in tasks if t.action_name == 'export_refund_to_tlog'), None)
            assert task is None

        @pytest.mark.asyncio
        async def test_no_notify_arbitrage(self, storage, returned):
            assert len([t async for t in storage.task.find() if t.action_name == 'notify_arbitrage_action']) == 0

        class TestNotifyArbitrage:
            @pytest.fixture(autouse=True)
            def arbitrage_data(self, refund):
                return {'refund_id': refund.order_id}

            @pytest.mark.asyncio
            async def test_notify_arbitrage(self, storage, arbitrage, returned):
                assert_that(
                    [t async for t in storage.task.find() if t.action_name == 'notify_arbitrage_action'],
                    contains_inanyorder(
                        has_properties({
                            'action_name': 'notify_arbitrage_action',
                            'task_type': TaskType.RUN_ACTION,
                            'state': TaskState.PENDING,
                            'params': has_entries({
                                'action_kwargs': {'arbitrage_id': arbitrage.arbitrage_id}
                            })
                        })
                    )
                )
