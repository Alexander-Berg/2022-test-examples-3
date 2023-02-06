import pytest

from hamcrest import assert_that, contains_inanyorder, equal_to, has_properties

from mail.payments.payments.core.actions.mixins.moderation import MerchantModerationMixin
from mail.payments.payments.core.entities.enums import FunctionalityType
from mail.payments.payments.core.entities.moderation import Moderation
from mail.payments.payments.tests.utils import dummy_coro
from mail.payments.payments.utils.helpers import temp_setattr


async def async_iterable(iter_):
    for i in iter_:
        yield i


@pytest.fixture
def action(storage):
    with temp_setattr(MerchantModerationMixin.context, 'storage', storage):
        yield MerchantModerationMixin()


@pytest.fixture(params=FunctionalityType)
def functionality_type(request):
    return request.param


@pytest.fixture
def effective():
    return None


@pytest.fixture
def has_ongoing():
    return False


@pytest.fixture
def get_effective_mock(mocker, action, effective):
    return mocker.patch.object(
        action,
        'get_effective_moderation',
        mocker.Mock(return_value=dummy_coro(effective)),
    )


@pytest.fixture
def has_ongoing_mock(mocker, action, has_ongoing):
    return mocker.patch.object(
        action,
        'has_ongoing_moderations',
        mocker.Mock(return_value=dummy_coro(has_ongoing)),
    )


class TestEffectiveUID:
    def test_without_parent(self, merchant):
        assert MerchantModerationMixin._effective_uid(merchant) == merchant.uid

    def test_with_parent(self, merchant_with_parent):
        assert MerchantModerationMixin._effective_uid(merchant_with_parent) == merchant_with_parent.parent_uid


class TestGetRevisionModeration:
    @pytest.fixture
    def moderations_data(self, randn, functionality_type):
        return [{'revision': randn(), 'functionality_type': functionality_type} for _ in range(5)]

    @pytest.mark.asyncio
    async def test_no_match(self, merchant, action, moderations, functionality_type):
        assert await action.get_revision_moderation(merchant.uid, -10, functionality_type) is None

    @pytest.mark.asyncio
    async def test_match(self, merchant, action, moderations, functionality_type):
        assert await action.get_revision_moderation(
            merchant.uid, moderations[3].revision, functionality_type
        ) == moderations[3]


class TestGetEffectiveModeration:
    @pytest.mark.parametrize('moderations_data', (
        pytest.param([], id='no_moderations'),
        pytest.param([{'approved': None}], id='not_finished'),
        pytest.param([{'approved': True, 'ignore': True}], id='ignored'),
        pytest.param([{'approved': True, 'functionality_type': FunctionalityType.YANDEX_PAY}], id='different-ftype'),
    ))
    @pytest.mark.asyncio
    async def test_no_effective_moderation(self, merchant, moderations, action):
        assert await action.get_effective_moderation(merchant, FunctionalityType.PAYMENTS) is None

    @pytest.mark.parametrize('moderations_data,correct_index', (
        pytest.param([{'approved': True}], 0, id='single_approved'),
        pytest.param([{'approved': False}], 0, id='single_disapproved'),
        pytest.param([{'approved': False}, {'approved': None}], 0, id='finished_and_ongoing'),
        pytest.param([{'approved': None}, {'approved': False}], 1, id='ongoing_and_finished'),
        pytest.param(
            [
                {'approved': False, 'functionality_type': FunctionalityType.PAYMENTS},
                {'approved': False, 'functionality_type': FunctionalityType.YANDEX_PAY},
            ],
            0,
            id='different-functionalities'
        ),
    ))
    @pytest.mark.asyncio
    async def test_returns_effective_moderation(self, merchant, moderations, action, correct_index):
        assert await action.get_effective_moderation(merchant, FunctionalityType.PAYMENTS) == moderations[correct_index]

    class TestParentUID:
        @pytest.fixture
        def moderations_data(self, merchant_with_parent):
            return [
                {'uid': merchant_with_parent.uid, 'approved': True},
                {'uid': merchant_with_parent.parent_uid, 'approved': True},
            ]

        @pytest.mark.asyncio
        async def test_returns_parent_moderation(self, merchant_with_parent, moderations, action):
            assert await action.get_effective_moderation(
                merchant_with_parent, FunctionalityType.PAYMENTS
            ) == moderations[1]


class TestGetOngoingModerations:
    @pytest.mark.parametrize('moderations_data,correct_indexes', (
        (
            [
                {'approved': True},
                {'approved': None},
                {'approved': False},
                {'approved': None, 'ignore': True},
                {'approved': None},
                {'approved': None, 'functionality_type': FunctionalityType.YANDEX_PAY},
            ],
            [1, 4],
        ),
    ))
    @pytest.mark.asyncio
    async def test_returns_ongoing_without_ignored(self, merchant, moderations, action, correct_indexes):
        assert_that(
            [m async for m in action.get_ongoing_moderations(merchant, FunctionalityType.PAYMENTS)],
            contains_inanyorder(*[moderations[i] for i in correct_indexes])
        )

    class TestParentUID:
        @pytest.fixture
        def moderations_data(self, merchant_with_parent):
            return [
                {'uid': merchant_with_parent.uid, 'approved': None, 'revision': 0},
                {'uid': merchant_with_parent.parent_uid, 'approved': None, 'revision': 0},
                {'uid': merchant_with_parent.parent_uid, 'approved': None, 'revision': 1},
                {'uid': merchant_with_parent.uid, 'approved': None, 'revision': 1},
            ]

        @pytest.mark.asyncio
        async def test_returns_parent_moderations(self, merchant_with_parent, moderations, action):
            assert_that(
                [m async for m in action.get_ongoing_moderations(merchant_with_parent, FunctionalityType.PAYMENTS)],
                contains_inanyorder(*[m for m in moderations if m.uid == merchant_with_parent.parent_uid])
            )


class TestHasOngoingModerations:
    @pytest.fixture
    def ongoing_moderations(self):
        return []

    @pytest.fixture(autouse=True)
    def get_ongoing_mock(self, mocker, action, ongoing_moderations):
        return mocker.patch.object(action, 'get_ongoing_moderations',
                                   mocker.Mock(return_value=async_iterable(ongoing_moderations)))

    @pytest.mark.parametrize('ongoing_moderations', (
        pytest.param([], id='none_ongoing'),
        pytest.param([1, 2], id='some_ongoing'),
    ))
    @pytest.mark.asyncio
    async def test_returns_correct(self, merchant, action, ongoing_moderations):
        assert await action.has_ongoing_moderations(merchant, FunctionalityType.PAYMENTS) == bool(ongoing_moderations)

    @pytest.mark.asyncio
    async def test_get_ongoing_call(self, merchant, action, get_ongoing_mock):
        await action.has_ongoing_moderations(merchant, FunctionalityType.PAYMENTS)
        get_ongoing_mock.assert_called_once_with(merchant, functionality_type=FunctionalityType.PAYMENTS)


class TestHasOngoingModerationsWhenMerchantPreloaded:
    @pytest.fixture(autouse=True)
    def get_ongoing_mock(self, mocker, action):
        return mocker.patch.object(action, 'get_ongoing_moderations',
                                   mocker.Mock(return_value=async_iterable([])))

    @pytest.mark.asyncio
    async def test_returns_moderation_from_merchant(self, merchant, action, get_ongoing_mock, mocker):
        has_ongoing = mocker.Mock()
        merchant.moderations = {FunctionalityType.PAYMENTS: mocker.Mock(has_ongoing=has_ongoing)}

        assert_that(
            await action.has_ongoing_moderations(merchant, FunctionalityType.PAYMENTS),
            equal_to(has_ongoing),
        )

    @pytest.mark.asyncio
    async def test_doesnt_call_get_ongoing_moderations(self, merchant, action, get_ongoing_mock, mocker):
        merchant.moderations = {FunctionalityType.PAYMENTS: mocker.Mock()}

        await action.has_ongoing_moderations(merchant, FunctionalityType.PAYMENTS),

        get_ongoing_mock.assert_not_called()


class TestIgnoreOngoingModerations:
    @pytest.fixture
    def moderations_data(self):
        return [
            {'approved': None, 'ignore': False},
            {'approved': True, 'ignore': False},
            {'approved': False, 'ignore': False},
            {'approved': None, 'ignore': False},
        ]

    @pytest.fixture(autouse=True)
    def get_ongoing_mock(self, mocker, action, moderations):
        return mocker.patch.object(action, 'get_ongoing_moderations',
                                   mocker.Mock(return_value=async_iterable([moderations[0], moderations[-1]])))

    @pytest.mark.asyncio
    async def test_ignores(self, storage, merchant, moderations, action):
        await action.ignore_ongoing_moderations(merchant, FunctionalityType.PAYMENTS)
        assert all([
            m.approved is not None or m.ignore
            async for m in storage.moderation.find()
        ])

    @pytest.mark.asyncio
    async def test_get_ongoing_call(self, merchant, action, get_ongoing_mock):
        await action.ignore_ongoing_moderations(merchant, FunctionalityType.PAYMENTS)
        get_ongoing_mock.assert_called_once_with(
            merchant,
            functionality_type=FunctionalityType.PAYMENTS, for_update=True
        )


class TestApprovedEffectiveModeration:
    @pytest.fixture(autouse=True)
    def setup(self, get_effective_mock):
        pass

    @pytest.mark.parametrize('effective,correct', (
        pytest.param(None, False, id='no_effective'),
        pytest.param(Moderation(uid=1, moderation_type=2, approved=True), True, id='approved_effective'),
        pytest.param(Moderation(uid=1, moderation_type=2, approved=False), False, id='disapproved_effective'),
    ))
    @pytest.mark.asyncio
    async def test_correct(self, merchant, action, correct):
        assert await action.approved_effective_moderation(merchant, FunctionalityType.PAYMENTS) == correct

    @pytest.mark.asyncio
    async def test_get_effective_call(self, merchant, action, get_effective_mock):
        await action.approved_effective_moderation(merchant, FunctionalityType.PAYMENTS)
        get_effective_mock.assert_called_once_with(merchant, functionality_type=FunctionalityType.PAYMENTS)


class TestApprovedEffectiveModerationWhenMerchantPreloaded:
    @pytest.fixture(autouse=True)
    def get_effective_moderation_mock(self, mocker, action):
        return mocker.patch.object(
            action,
            'get_effective_moderation',
            mocker.AsyncMock(),
        )

    @pytest.mark.asyncio
    async def test_correct(self, merchant, action, mocker):
        approved = mocker.Mock()
        merchant.moderations = {FunctionalityType.PAYMENTS: mocker.Mock(approved=approved)}

        assert await action.approved_effective_moderation(merchant, FunctionalityType.PAYMENTS) == approved

    @pytest.mark.asyncio
    async def test_get_effective_call(self, merchant, action, get_effective_moderation_mock, mocker):
        merchant.moderations = {FunctionalityType.PAYMENTS: mocker.Mock()}

        await action.approved_effective_moderation(merchant, FunctionalityType.PAYMENTS)

        get_effective_moderation_mock.assert_not_called()


class TestGetModerationData:
    @pytest.fixture(autouse=True)
    def setup(self, get_effective_mock, has_ongoing_mock):
        pass

    @pytest.mark.parametrize('effective,has_ongoing,has_moderation', (
        (None, True, True),
        (None, False, False),
        (Moderation(uid=1, moderation_type=2, approved=True), False, True),
        (Moderation(uid=1, moderation_type=2, approved=True), True, True),
        (Moderation(uid=1, moderation_type=2, approved=False, reasons=[1, 2]), True, True),
    ))
    @pytest.mark.asyncio
    async def test_result(self, merchant, action, effective, has_ongoing, has_moderation):
        assert_that(
            await action.get_moderation_data(merchant, FunctionalityType.PAYMENTS),
            has_properties({
                'approved': getattr(effective, 'approved', False),
                'reasons': getattr(effective, 'reasons', []),
                'has_moderation': has_moderation,
                'has_ongoing': has_ongoing,
            })
        )
