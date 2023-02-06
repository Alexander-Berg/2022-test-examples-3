from copy import copy
from dataclasses import replace
from datetime import timedelta

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, contains_inanyorder

from mail.payments.payments.core.entities.enums import FunctionalityType
from mail.payments.payments.core.entities.moderation import Moderation, ModerationType
from mail.payments.payments.core.entities.serial import Serial
from mail.payments.payments.storage.exceptions import ModerationNotFound


class TestModerationMapper:
    @pytest.fixture
    def moderation_entity(self, merchant):
        return Moderation(
            uid=merchant.uid,
            revision=44,
            moderation_type=ModerationType.MERCHANT,
            functionality_type=FunctionalityType.PAYMENTS,
            approved=True,
            ignore=False,
            reason='some reason',
            reasons=[0, 1],
        )

    @pytest.fixture
    async def created_moderation(self, storage, moderation_entity):
        return await storage.moderation.create(moderation_entity)

    @pytest.fixture
    async def moderation(self, storage, created_moderation):
        return await storage.moderation.get(created_moderation.moderation_id)

    @pytest.fixture
    async def saved(self, storage, moderation):
        moderation.approved = False
        moderation.ignore = True
        moderation.reason = 'another reason'
        moderation.reasons = [1, 2, 3]
        return await storage.moderation.save(moderation)

    @pytest.fixture
    async def non_approved_moderation(self, storage, moderation):
        moderation.approved = None
        return await storage.moderation.save(moderation)

    @pytest.fixture
    async def updated_moderation(self, storage, saved):
        return await storage.moderation.get(saved.moderation_id)

    def test_create_sets_id(self, created_moderation):
        assert created_moderation.moderation_id is not None

    def test_create_returns(self, moderation_entity, created_moderation):
        moderation_entity.moderation_id = created_moderation.moderation_id
        moderation_entity.created = moderation_entity.updated = created_moderation.created
        assert created_moderation == moderation_entity

    def test_create_creates(self, created_moderation, moderation):
        assert created_moderation == moderation

    def test_save_returns(self, moderation, saved):
        moderation.updated = saved.updated
        assert moderation == saved

    def test_save_updates(self, saved, updated_moderation):
        assert updated_moderation == saved

    @pytest.mark.parametrize('ignore', (True, False))
    @pytest.mark.asyncio
    async def test_count_pending_by_type(self, ignore, storage, non_approved_moderation):
        async for type_, count in storage.moderation.count_pending_by_type(ignore=ignore):
            if type_ == non_approved_moderation.moderation_type:
                target_count = 0 if ignore else 1
                assert count == target_count


class TestFind:
    @pytest.fixture
    def moderations_data(self):
        return []

    @pytest.fixture(autouse=True)
    def setup(self, moderations):
        pass

    @pytest.fixture
    def find_func(self, storage):
        async def _inner(**kwargs):
            return [m async for m in storage.moderation.find(**kwargs)]

        return _inner

    class TestUID:
        @pytest.fixture
        async def merchant2(self, storage, merchant):
            async with storage.conn.begin():
                m = copy(merchant)
                m.uid += 1
                m.merchant_id = str(m.uid)
                m = await storage.merchant.create(m)
                await storage.serial.create(Serial(m.uid))
                return m

        @pytest.fixture
        def moderations_data(self, merchant, merchant2):
            return [{'uid': merchant.uid}, {'uid': merchant2.uid}]

        @pytest.mark.asyncio
        async def test_uid(self, merchant, moderations, find_func):
            assert_that(
                await find_func(uid=merchant.uid),
                contains_inanyorder(*[
                    m
                    for m in moderations
                    if m.uid == merchant.uid
                ])
            )

    class TestModerationType:
        @pytest.fixture
        def moderations_data(self):
            return [
                {'moderation_type': ModerationType.MERCHANT},
                {'moderation_type': ModerationType.ORDER},
                {'moderation_type': ModerationType.MERCHANT},
            ]

        @pytest.mark.asyncio
        async def test_moderation_type(self, moderations, find_func):
            assert_that(
                await find_func(moderation_type=ModerationType.MERCHANT),
                contains_inanyorder(*[
                    m
                    for m in moderations
                    if m.moderation_type == ModerationType.MERCHANT
                ])
            )

    class TestFunctionalityType:
        @pytest.fixture
        def moderations_data(self):
            return [
                {'moderation_type': ModerationType.MERCHANT, 'functionality_type': FunctionalityType.PAYMENTS},
                {'moderation_type': ModerationType.MERCHANT, 'functionality_type': FunctionalityType.YANDEX_PAY},
            ]

        @pytest.mark.asyncio
        async def test_moderation_type(self, moderations, find_func):
            assert_that(
                await find_func(moderation_type=ModerationType.MERCHANT, functionality_type=FunctionalityType.PAYMENTS),
                contains_inanyorder(*[
                    m
                    for m in moderations
                    if (
                        m.moderation_type == ModerationType.MERCHANT
                        and m.functionality_type == FunctionalityType.PAYMENTS
                    )
                ])
            )

    class TestRevision:
        @pytest.fixture
        def moderations_data(self):
            return [{'revision': 1}, {'revision': 2, 'ignore': True}, {'revision': 2}]

        @pytest.mark.asyncio
        async def test_revision(self, moderations, find_func):
            assert_that(
                await find_func(revision=2),
                contains_inanyorder(*[
                    m
                    for m in moderations
                    if m.revision == 2
                ])
            )

    class TestHasApproved:
        @pytest.fixture
        def moderations_data(self):
            return [{'approved': None}, {'approved': False}, {'approved': True}]

        @pytest.mark.asyncio
        async def test_has_approved_true(self, moderations, find_func):
            assert_that(
                await find_func(has_approved=True),
                contains_inanyorder(*[
                    m
                    for m in moderations
                    if m.approved is not None
                ])
            )

        @pytest.mark.asyncio
        async def test_has_approved_false(self, moderations, find_func):
            assert_that(
                await find_func(has_approved=False),
                contains_inanyorder(*[
                    m
                    for m in moderations
                    if m.approved is None
                ])
            )


class TestGetEffective:
    @pytest.fixture(params=[
        pytest.param([], id='no_moderations'),
        pytest.param([{'approved': True}], id='single_approved'),
        pytest.param([{'approved': True}, {'approved': False}, {}], id='approved_disapproved'),
        pytest.param([{'approved': True, 'ignore': True}], id='ignore_approved'),
        pytest.param([{'approved': False, 'ignore': True}], id='ignore_disapproved'),
        pytest.param([{'approved': True}, {'approved': False, 'ignore': True}], id='approved_ignore_disapproved'),
    ])
    def moderations_items(self, request):
        return request.param

    def moderations_data(self, moderations_items):
        return moderations_items

    @pytest.fixture
    def expected(self, moderations):
        return next(
            (
                moderation
                for moderation in moderations[::-1]
                if moderation.approved is not None and not moderation.ignore
            ),
            None
        )

    @pytest.mark.asyncio
    async def test_return(self, storage, merchant, expected):
        assert await storage.moderation.get_effective(merchant.uid) == expected

    class TestModerationType:
        @pytest.fixture(params=list(ModerationType))
        def moderation_type(self, request):
            return request.param

        @pytest.fixture
        def expected(self, moderations, moderation_type):
            return next(
                (
                    moderation
                    for moderation in moderations[::-1]
                    if (moderation.approved is not None
                        and not moderation.ignore
                        and moderation.moderation_type == moderation_type)
                ),
                None
            )

        @pytest.mark.asyncio
        async def test_moderation_type(self, storage, merchant, expected, moderation_type):
            assert await storage.moderation.get_effective(merchant.uid, moderation_type=moderation_type) == expected

    class TestGetEffectiveSubscription:
        @pytest.fixture
        def moderations_data(self, subscription, moderations_items):
            return [
                {**item, 'entity_id': subscription.subscription_id, 'moderation_type': ModerationType.SUBSCRIPTION}
                for item in moderations_items
            ]

        @pytest.mark.asyncio
        async def test_get_effective_subscription__return(self, storage, subscription, merchant, expected):
            returned = await storage.moderation.get_effective(merchant.uid,
                                                              entity_id=subscription.subscription_id,
                                                              moderation_type=ModerationType.SUBSCRIPTION)
            assert returned == expected


class TestGetForOrder:
    @pytest.fixture
    def moderation_entity(self, order, merchant):
        return Moderation(
            uid=merchant.uid,
            entity_id=order.order_id,
            revision=44,
            moderation_type=ModerationType.ORDER,
            approved=True,
            ignore=False,
            reason='some reason',
            reasons=[0, 1],
        )

    @pytest.fixture
    def returned_func(self, storage, order):
        async def _inner():
            return await storage.moderation.get_for_order(order.uid, order.order_id)

        return _inner

    @pytest.fixture
    async def moderation(self, storage, moderation_entity):
        return await storage.moderation.create(moderation_entity)

    @pytest.mark.asyncio
    async def test_get(self, moderation, returned_func):
        assert (await returned_func()) == moderation

    @pytest.mark.asyncio
    async def test_ignored(self, storage, moderation, returned_func):
        moderation.ignore = True
        await storage.moderation.save(moderation)

        with pytest.raises(ModerationNotFound):
            await returned_func()


class TestGetOldestPendingCreatedTime:
    @pytest.fixture(autouse=True)
    async def setup(self, storage, moderation_entity, another_entity):
        storage.moderation._create_ignore_fields = ('moderation_id', 'updated')
        await storage.moderation.create(moderation_entity)
        await storage.moderation.create(another_entity)

    @pytest.fixture
    def moderation_type(self) -> ModerationType:
        return ModerationType.MERCHANT

    @pytest.fixture
    def another_moderation_type(self) -> ModerationType:
        return ModerationType.SUBSCRIPTION

    @pytest.fixture
    async def returned(self, storage, moderation_type):
        return await storage.moderation.get_oldest_pending_moderation_created_time(moderation_type)

    @pytest.fixture
    def valid_moderation_entity(self, merchant, moderation_type) -> Moderation:
        return Moderation(
            uid=merchant.uid,
            revision=44,
            moderation_type=moderation_type,
            functionality_type=FunctionalityType.PAYMENTS if moderation_type == ModerationType.MERCHANT else None,
            approved=None,
            ignore=False,
            reason='some reason',
            reasons=[0, 1],
        )

    class TestReturnOldestMatchedModerationTime:
        @pytest.fixture
        def moderation_entity(self, merchant, valid_moderation_entity) -> Moderation:
            return replace(valid_moderation_entity, created=utcnow() - timedelta(hours=1))

        @pytest.fixture
        def another_entity(self, merchant, moderation_entity) -> Moderation:
            return replace(
                moderation_entity,
                created=moderation_entity.created - timedelta(hours=1),
                revision=moderation_entity.revision + 1,
            )

        @pytest.mark.asyncio
        async def test_should_return_oldest_created_time(self, returned, another_entity):
            assert returned == another_entity.created

    class TestFilterByModerationType:
        @pytest.fixture
        def moderation_entity(self, merchant, valid_moderation_entity) -> Moderation:
            return replace(valid_moderation_entity, created=utcnow() - timedelta(hours=1))

        @pytest.fixture
        def another_entity(self, merchant, moderation_entity, another_moderation_type) -> Moderation:
            if another_moderation_type == ModerationType.MERCHANT:
                functionality_type = FunctionalityType.PAYMENTS
            else:
                functionality_type = None
            return replace(
                moderation_entity,
                moderation_type=another_moderation_type,
                functionality_type=functionality_type,
                created=moderation_entity.created - timedelta(hours=1)
            )

        @pytest.mark.asyncio
        async def test_should_filter_by_moderation_type(self, returned, moderation_entity):
            assert returned == moderation_entity.created

    class TestReturnNone:
        @pytest.fixture
        def moderation_entity(self, merchant, valid_moderation_entity, another_moderation_type) -> Moderation:
            if another_moderation_type == ModerationType.MERCHANT:
                functionality_type = FunctionalityType.PAYMENTS
            else:
                functionality_type = None
            return replace(
                valid_moderation_entity, moderation_type=another_moderation_type,
                functionality_type=functionality_type,
            )

        @pytest.fixture
        def another_entity(self, merchant, moderation_entity) -> Moderation:
            return replace(moderation_entity)

        @pytest.mark.asyncio
        async def test_should_return_none_if_no_matches(self, returned):
            assert not returned

    class TestIgnoredModerationExcluding:
        @pytest.fixture
        def moderation_entity(self, merchant, valid_moderation_entity) -> Moderation:
            return replace(valid_moderation_entity,
                           ignore=True,
                           created=utcnow() - timedelta(hours=2))

        @pytest.fixture
        def another_entity(self, merchant, moderation_entity) -> Moderation:
            return replace(moderation_entity,
                           ignore=False,
                           created=utcnow() - timedelta(hours=1))

        @pytest.mark.asyncio
        async def test_should_exclude_ignored_moderation(self, returned, another_entity):
            assert returned == another_entity.created

    class TestNotNoneApprovedValues:
        @pytest.fixture
        def moderation_entity(self, merchant, valid_moderation_entity) -> Moderation:
            return replace(valid_moderation_entity, approved=True)

        @pytest.fixture
        def another_entity(self, merchant, moderation_entity) -> Moderation:
            return replace(moderation_entity, approved=False, revision=moderation_entity.revision - 1)

        @pytest.mark.asyncio
        async def test_should_not_match_not_none_approved_values(self, returned):
            assert not returned
