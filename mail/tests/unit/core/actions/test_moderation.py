from dataclasses import asdict
from random import randint

import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.payments.payments.core.actions.moderation import (
    ScheduleMerchantModerationAction, ScheduleOrderModerationAction, ScheduleSubscriptionModerationAction
)
from mail.payments.payments.core.entities.enums import (
    CallbackMessageType, FunctionalityType, PaymentsTestCase, TaskType
)
from mail.payments.payments.core.entities.moderation import Moderation, ModerationType
from mail.payments.payments.core.exceptions import (
    MerchantCannotScheduleModerationForChildError, MerchantCannotScheduleModerationForPreregisterError
)
from mail.payments.payments.tests.base import BaseTestRequiresNoModeration


@pytest.fixture
def functionality_type():
    return FunctionalityType.PAYMENTS


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestScheduleMerchantModeration(BaseTestRequiresNoModeration):
    @pytest.fixture
    def skip_moderation_task(self, request):
        return False

    @pytest.fixture
    def params(self, merchant, skip_moderation_task, functionality_type):
        return {
            'uid': merchant.uid,
            'skip_moderation_task': skip_moderation_task,
            'functionality_type': functionality_type,
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await ScheduleMerchantModerationAction(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, service_merchant, service_client, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    async def test_creates_tasks(self, storage, merchant, service_client,
                                 service_merchant, returned, functionality_type):
        tasks = await alist(storage.task.find())

        start_moderation_task = next(filter(lambda t: t.task_type == TaskType.START_MODERATION, tasks))
        assert start_moderation_task.params == dict(merchant_uid=merchant.uid,
                                                    functionality_type=functionality_type.value)

        api_callback_task = next(filter(lambda t: t.task_type == TaskType.API_CALLBACK, tasks))
        expected_params = dict(tvm_id=service_client.tvm_id,
                               callback_message_type=CallbackMessageType.MERCHANT_MODERATION_STARTED.value,
                               callback_url=service_client.api_callback_url,
                               message={'service_merchant_id': service_merchant.service_merchant_id})
        assert api_callback_task.params == expected_params

    @pytest.mark.parametrize('skip_moderation_task', (True,))
    @pytest.mark.asyncio
    async def test_not_creates_task(self, storage, returned):
        with pytest.raises(StopAsyncIteration):
            await storage.task.find().__anext__()

    @pytest.mark.asyncio
    async def test_creates_moderation(self, storage, merchant, returned, functionality_type):
        moderation = await storage.moderation.find().__anext__()
        assert all([
            moderation.uid == merchant.uid,
            moderation.revision == merchant.revision,
            moderation.moderation_type == ModerationType.MERCHANT,
            moderation.functionality_type == functionality_type,
        ])

    @pytest.mark.asyncio
    async def test_returns_created_moderation(self, storage, returned):
        assert returned == await storage.moderation.find().__anext__()

    @pytest.mark.parametrize('moderations_data', (
        [{'approved': False, 'revision': randint(100, 10 ** 6)}],
    ))
    @pytest.mark.asyncio
    async def test_does_not_reuse_other_revision_moderation(self, storage, moderations, returned):
        assert returned not in moderations

    @pytest.mark.asyncio
    async def test_does_not_reuse_other_functionality_type_moderation(self, storage, merchant, returned_func):
        moderation = await storage.moderation.create(
            Moderation(
                uid=merchant.uid,
                moderation_type=ModerationType.MERCHANT,
                functionality_type=FunctionalityType.YANDEX_PAY,
                revision=merchant.revision,
            )
        )

        returned = await returned_func()
        assert returned.moderation_id != moderation.moderation_id

    class TestReusesExistingModeration:
        @pytest.fixture
        def moderations_data(self, merchant):
            return [
                {'approved': False, 'revision': merchant.revision - 1},
                {'approved': None, 'revision': merchant.revision},
            ]

        @pytest.fixture(autouse=True)
        def setup(self, moderations):
            pass

        @pytest.mark.asyncio
        async def test_does_not_create_task(self, storage, returned):
            with pytest.raises(StopAsyncIteration):
                await storage.task.find().__anext__()

        @pytest.mark.asyncio
        async def test_does_not_create_moderation(self, storage, moderations, returned):
            assert_that(
                [m async for m in storage.moderation.find()],
                contains_inanyorder(*moderations),
            )

        def test_returns_same_revision_moderation(self, moderations, returned):
            assert returned == moderations[1]

    class TestPreregisterError:
        @pytest.mark.asyncio
        @pytest.mark.parametrize('acquirer', [None])
        async def test_preregister_error__raises_error(self, returned_func):
            with pytest.raises(MerchantCannotScheduleModerationForPreregisterError):
                await returned_func()

    class TestParentUID:
        @pytest.fixture
        def params(self, merchant_with_parent, params):
            params['uid'] = merchant_with_parent.uid
            return params

        @pytest.mark.asyncio
        async def test_parent_uid__raises_error(self, returned_func):
            with pytest.raises(MerchantCannotScheduleModerationForChildError):
                await returned_func()


class TestScheduleOrderModeration:
    @pytest.fixture
    def params(self, order):
        return {'order': order}

    @pytest.fixture
    def action(self, params):
        return ScheduleOrderModerationAction(**params)

    @pytest.fixture
    def returned_func(self, action):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    async def test_create_moderation(self, returned, order, storage, get_tasks):
        task = (await get_tasks())[0]

        assert_that(
            {
                'moderation': asdict(returned),
                'task': asdict(task),
            },
            {
                'moderation': has_entries({
                    'uid': order.uid,
                    'entity_id': order.order_id,
                    'moderation_type': ModerationType.ORDER,
                    'revision': order.revision,
                    'approved': False,
                }),
                'task': has_entries({
                    'task_type': TaskType.START_ORDER_MODERATION,
                    'params': {
                        'moderation_id': returned.moderation_id,
                    },
                }),
            }
        )

    class TestTestOrder:
        @pytest.fixture(params=list(PaymentsTestCase))
        def test_case(self, request):
            return request.param

        @pytest.fixture
        def order_data(self, test_case):
            return {'test': test_case}

        @pytest.mark.asyncio
        async def test_create_only_moderation(self, returned, order, storage, get_tasks):
            assert_that(
                asdict(returned),
                has_entries({
                    'uid': order.uid,
                    'entity_id': order.order_id,
                    'moderation_type': ModerationType.ORDER,
                    'revision': order.revision,
                    'approved': order.test != PaymentsTestCase.TEST_MODERATION_FAILED
                })
            )
            assert len(await get_tasks()) == 0


class TestScheduleSubscriptionModeration:
    @pytest.fixture
    def params(self, subscription):
        return {'subscription': subscription}

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await ScheduleSubscriptionModerationAction(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    async def test_create_moderation(self, returned, subscription, storage, get_tasks):
        task = (await get_tasks())[0]

        assert_that(
            {
                'moderation': asdict(returned),
                'task': asdict(task),
            },
            {
                'moderation': has_entries({
                    'uid': subscription.uid,
                    'entity_id': subscription.subscription_id,
                    'moderation_type': ModerationType.SUBSCRIPTION,
                    'revision': subscription.revision,
                    'approved': False,
                }),
                'task': has_entries({
                    'task_type': TaskType.START_SUBSCRIPTION_MODERATION,
                    'params': {
                        'moderation_id': returned.moderation_id,
                    },
                }),
            }
        )
