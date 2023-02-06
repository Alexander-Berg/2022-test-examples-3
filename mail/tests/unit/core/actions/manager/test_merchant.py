from datetime import datetime
from random import randint

import pytest

from hamcrest import assert_that, contains_inanyorder, equal_to, has_entries, has_properties, match_equality

from mail.payments.payments.core.actions.manager.merchant import (
    BlockMerchantManagerAction, GetMerchantAction, GetMerchantListManagerAction,
    RecreateMerchantModerationManagerAction, UpdateSupportCommentManagerAction
)
from mail.payments.payments.core.actions.merchant.block import BlockMerchantAction
from mail.payments.payments.core.actions.moderation import ScheduleMerchantModerationAction
from mail.payments.payments.core.entities.common import SearchStats
from mail.payments.payments.core.entities.enums import AcquirerType, MerchantStatus, ModerationStatus
from mail.payments.payments.core.entities.keyset import KeysetEntry, ManagerMerchantListKeysetEntity
from mail.payments.payments.core.entities.moderation import Moderation
from mail.payments.payments.core.exceptions import (
    KeysetInvalidError, ManagerActionNotAllowed, ModerationAlreadyExistsError, SortByInvalidError
)
from mail.payments.payments.interactions.exceptions import BlackBoxUserNotFoundError
from mail.payments.payments.tests.base import BaseTestParent
from mail.payments.payments.tests.utils import dummy_coro


class BaseGetMerchantListManagerActionTest:
    @pytest.fixture(autouse=True)
    def get_merchant_calls(self, mocker):
        def dummy_init(self, merchant):
            self.merchant = merchant

        async def dummy_run(self):
            return self.merchant

        mocker.patch.object(GetMerchantAction, '__init__', dummy_init)
        mocker.patch.object(GetMerchantAction, 'run', dummy_run)
        return mocker.spy(GetMerchantAction, '__init__').call_args_list

    @pytest.fixture
    def blackbox_uid(self, merchant_uid):
        return merchant_uid

    @pytest.fixture(autouse=True)
    def mock_blackbox_user_info(self, blackbox_client_mocker, blackbox_uid, mocker):
        with blackbox_client_mocker('userinfo', mocker.Mock(uid=blackbox_uid)) as mock:
            yield mock

    @pytest.fixture
    def find_spy(self, mocker, merchants):
        from mail.payments.payments.storage.mappers.merchant import MerchantMapper
        return mocker.spy(MerchantMapper, 'find')

    @pytest.fixture
    async def merchants(self, storage, create_merchant):
        for _ in range(3):
            await create_merchant()
        return [m async for m in storage.merchant.find()]

    @pytest.fixture
    def params(self):
        return {}

    @pytest.fixture
    def action(self, params, manager_assessor):
        return GetMerchantListManagerAction(
            manager_uid=manager_assessor.uid,
            **params,
        )

    @pytest.fixture
    def returned_func(self, params, action, manager_assessor):
        async def _inner():
            return await action.run()
        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()


class TestGetMerchantListManagerAction(BaseGetMerchantListManagerActionTest):
    @pytest.mark.parametrize('merchant_uid, params', (
        (
            112233,
            {
                'merchant_uid': 112233,
                'name': 'test-find-call-name',
                'username': 'test-find-call-username',
                'client_id': 'test-find-call-client_id',
                'submerchant_id': 'test-find-call-submerchant_id',
                'limit': randint(1, 10 ** 9),
                'offset': randint(1, 10 ** 9),
                'sort_by': 'updated',
                'desc': False,
                'acquirers': [AcquirerType.TINKOFF],
                'moderation_status': ModerationStatus.ONGOING,
                'created_from': datetime.now(),
                'created_to': datetime.now(),
                'updated_from': datetime.now(),
                'updated_to': datetime.now(),
            },
        ),
    ))
    @pytest.mark.asyncio
    async def test_find_call(self, mocker, merchants, params, returned_func):
        from mail.payments.payments.storage.mappers.merchant import MerchantMapper
        find = mocker.spy(MerchantMapper, 'find')
        await returned_func()
        find_kwargs = find.call_args[1]
        assert_that(
            find_kwargs,
            has_entries({
                'uid': params['merchant_uid'],
                'name': params['name'],
                'client_id': params['client_id'],
                'submerchant_id': params['submerchant_id'],
                'limit': params['limit'],
                'offset': params['offset'],
                'sort_by': 'updated',
                'descending': False,
                'acquirers': params['acquirers'],
                'moderation_status': params['moderation_status'],
                'created_from': params['created_from'],
                'updated_from': params['updated_from'],
                'created_to': params['created_to'],
                'updated_to': params['updated_to'],
                'statuses': contains_inanyorder(
                    MerchantStatus.ACTIVE, MerchantStatus.INACTIVE, MerchantStatus.NEW
                )
            })
        )

    def test_returned(self, merchants, returned):
        assert all((
            returned.merchants == merchants,
            returned.stats == SearchStats(total=len(merchants), found=len(merchants))
        ))

    def test_get_merchant_calls(self, merchants, get_merchant_calls, returned):
        got_merchant_uids = [call[1]['merchant'].uid for call in get_merchant_calls]
        assert_that(got_merchant_uids, contains_inanyorder(*[m.uid for m in merchants]))

    @pytest.mark.parametrize('params', (
        {
            'name': 'test-find-call-name',
            'username': 'test-find-call-username',
            'client_id': 'test-find-call-client_id',
            'submerchant_id': 'test-find-call-submerchant_id',
            'acquirers': [AcquirerType.TINKOFF],
            'moderation_status': ModerationStatus.ONGOING,
            'created_from': datetime.now(),
            'created_to': datetime.now(),
            'updated_from': datetime.now(),
            'updated_to': datetime.now(),
        },
    ))
    def test_returned_counts(self, mocker, merchants, params, returned):
        # for params without limit len(returned.merchants) == found
        assert returned.stats == SearchStats(total=len(merchants), found=len(returned.merchants))

    @pytest.mark.parametrize('params', (
        {
            'limit': 1,
        },
    ))
    def test_returned_counts_with_limit(self, mocker, merchants, params, returned):
        assert all((
            returned.stats == SearchStats(total=len(merchants), found=len(merchants)),
            len(returned.merchants) == 1
        ))

    @pytest.mark.asyncio
    async def test_merchant_draft(self, storage, mocker, merchants, params, returned_func):
        merchants[0].status = MerchantStatus.DRAFT
        merchants[0] = await storage.merchant.save(merchants[0])
        returned = await returned_func()
        assert returned.stats == SearchStats(total=len(merchants) - 1, found=len(merchants) - 1)

    @pytest.mark.parametrize('params', (
        pytest.param({}, id='empty-keyset'),
        pytest.param({'sort_by': 'created'}, id='empty-keyset-with-sort'),
        pytest.param(
            {
                'sort_by': 'created',
                'desc': True,
                'keyset': ManagerMerchantListKeysetEntity(
                    created=KeysetEntry(barrier='2020', order='desc'),
                    uid=KeysetEntry(barrier=123, order='desc'),
                    sort_order=['created', 'uid'],
                ),
            },
            id='good-keyset',
        ),
    ))
    def test_get_keyset_ok(self, action):
        action._get_keyset_filter()

    @pytest.mark.parametrize('params', (
        pytest.param(
            {
                'desc': False,
                'sort_by': 'updated',
                'keyset': ManagerMerchantListKeysetEntity(
                    created=KeysetEntry(barrier='2020', order='desc'),
                    sort_order=['created'],
                ),
            },
            id='invalid-column',
        ),
    ))
    def test_get_keyset_error(self, action):
        with pytest.raises(KeysetInvalidError):
            action._get_keyset_filter()

    @pytest.mark.parametrize('params', (
        {'sort_by': 'client_id'},
    ))
    @pytest.mark.asyncio
    async def test_invalid_sort_by(self, returned_func):
        with pytest.raises(SortByInvalidError):
            await returned_func()

    @pytest.mark.parametrize('params, merchants, expected_keyset', (
        ({'sort_by': 'created'}, [], None),
        (
            {'desc': False, 'sort_by': 'created'},
            [{'created': 'aaa', 'uid': 123}, {'created': 'bbb', 'uid': 456}],
            ManagerMerchantListKeysetEntity(
                created=KeysetEntry(barrier='bbb', order='asc'),
                uid=KeysetEntry(barrier=456, order='asc'),
                sort_order=['created', 'uid']
            ),
        ),
        (
            {'desc': False, 'sort_by': 'uid'},
            [{'created': 'aaa', 'uid': 123}, {'created': 'bbb', 'uid': 456}],
            ManagerMerchantListKeysetEntity(
                uid=KeysetEntry(barrier=456, order='asc'),
                sort_order=['uid']
            ),
        ),
        (
            {
                'desc': False,
                'sort_by': 'created',
                'keyset': ManagerMerchantListKeysetEntity(
                    created=KeysetEntry(barrier='aaa', order='asc'),
                    uid=KeysetEntry(barrier=123, order='asc'),
                    sort_order=['created', 'uid']
                ),
            },
            [{'created': 'bbb', 'uid': 456}],
            ManagerMerchantListKeysetEntity(
                created=KeysetEntry(barrier='bbb', order='asc'),
                uid=KeysetEntry(barrier=456, order='asc'),
                sort_order=['created', 'uid']
            ),
        ),
        (
            {
                'desc': False,
                'sort_by': 'created',
                'keyset': ManagerMerchantListKeysetEntity(
                    created=KeysetEntry(barrier='aaa', order='asc'),
                    uid=KeysetEntry(barrier=123, order='asc'),
                    sort_order=['created', 'uid']
                ),
            },
            [],
            None,
        ),
    ))
    def test_make_keyset(self, mocker, action, expected_keyset, merchants):
        merchants = [mocker.NonCallableMock(**m) for m in merchants]
        assert_that(action._make_next_page_keyset(merchants), equal_to(expected_keyset))

    class TestKeyset:
        @pytest.fixture
        def mock_make_keyset(self, mocker):
            return mocker.patch.object(GetMerchantListManagerAction, '_make_next_page_keyset')

        def test_returns_keyset(self, merchants, mock_make_keyset, returned):
            assert_that(returned.keyset, equal_to(mock_make_keyset()))

        def test_calls_make_keyset(self, merchants, mock_make_keyset, returned):
            mock_make_keyset.assert_called_once_with(
                match_equality(
                    contains_inanyorder(*merchants),
                ),
            )


@pytest.mark.parametrize('merchant_uid, params', (
    (
        112233,
        {
            'merchant_uid': 112233,
        },
    ),
))
class TestFindWithoutUsername(BaseGetMerchantListManagerActionTest):
    def test_blackbox_call(self, mock_blackbox_user_info, params, returned):
        mock_blackbox_user_info.assert_not_called()

    @pytest.mark.asyncio
    async def test_find_call(self, find_spy, merchants, params, returned):
        find_spy.assert_called_once()


@pytest.mark.parametrize('merchant_uid, params', (
    (
        112233,
        {
            'merchant_uid': None,
            'username': 'loginlogin',
        },
    ),
))
class TestFindWithUsername(BaseGetMerchantListManagerActionTest):
    def test_blackbox_call(self, mock_blackbox_user_info, params, returned):
        mock_blackbox_user_info.assert_called_once_with(login=params['username'])

    @pytest.mark.asyncio
    async def test_find_call(self, merchant_uid, find_spy, merchants, params, returned):
        find_spy.assert_called_once()


@pytest.mark.parametrize('merchant_uid, blackbox_uid, params', (
    (
        112233,
        112233,
        {
            'merchant_uid': 112233,
            'name': 'test-find-call-name',
            'username': 'loginlogin',
            'client_id': 'test-find-call-client_id',
            'submerchant_id': 'test-find-call-submerchant_id',
            'limit': randint(1, 10 ** 9),
            'offset': randint(1, 10 ** 9),
            'sort_by': 'updated',
            'desc': False,
        },
    ),
))
class TestFindWithUsernameAndUid(BaseGetMerchantListManagerActionTest):
    def test_blackbox_call(self, mock_blackbox_user_info, params, returned):
        mock_blackbox_user_info.assert_called_once_with(login=params['username'])

    @pytest.mark.asyncio
    async def test_find_call(self, find_spy, merchants, params, returned):
        find_spy.assert_called()


@pytest.mark.parametrize('merchant_uid, blackbox_uid, params', (
    (
        112233,
        445566,
        {
            'merchant_uid': 112233,
            'name': 'test-find-call-name',
            'username': 'loginlogin',
            'client_id': 'test-find-call-client_id',
            'submerchant_id': 'test-find-call-submerchant_id',
            'limit': randint(1, 10 ** 9),
            'offset': randint(1, 10 ** 9),
            'sort_by': 'updated',
            'desc': False,
        },
    ),
))
class TestFindWithUsernameAndUidWhenUidDiffers(BaseGetMerchantListManagerActionTest):
    def test_blackbox_call(self, mock_blackbox_user_info, params, returned):
        mock_blackbox_user_info.assert_called_once_with(login=params['username'])

    @pytest.mark.asyncio
    async def test_find_call(self, find_spy, merchants, params, returned_func):
        await returned_func()
        find_spy.assert_not_called()

    def test_returned(self, returned):
        assert all((
            returned.merchants == [],
            returned.stats == SearchStats(total=0, found=0),
            returned.keyset is None,
        ))


@pytest.mark.parametrize('merchant_uid, blackbox_uid, params', (
    (
        112233,
        112233,
        {
            'merchant_uid': 112233,
            'name': 'test-find-call-name',
            'username': 'loginlogin',
            'client_id': 'test-find-call-client_id',
            'submerchant_id': 'test-find-call-submerchant_id',
            'limit': randint(1, 10 ** 9),
            'offset': randint(1, 10 ** 9),
            'sort_by': 'updated',
            'desc': False,
        },
    ),
))
class TestFindWithUsernameAndUidWhenBlackboxNotFound(BaseGetMerchantListManagerActionTest):
    @pytest.fixture(autouse=True)
    def mock_blackbox_user_info(self, blackbox_client_mocker, blackbox_uid, mocker):
        with blackbox_client_mocker(
            'userinfo',
            exc=BlackBoxUserNotFoundError(service='blackbox', method='userinfo'),
        ) as mock:
            yield mock

    def test_blackbox_call(self, mock_blackbox_user_info, params, returned):
        mock_blackbox_user_info.assert_called_once_with(login=params['username'])

    def test_find_call(self, find_spy, merchants, params, returned):
        find_spy.assert_not_called()


class TestBlockMerchantManagerAction:
    @pytest.fixture
    def manager_uid(self, manager_admin):
        return manager_admin.uid

    @pytest.fixture
    def blocked(self):
        return True

    @pytest.fixture
    def params(self, manager_uid, merchant):
        return {
            'manager_uid': manager_uid,
            'uid': merchant.uid
        }

    @pytest.fixture(autouse=True)
    def block_merchant(self, mock_action, merchant, blocked):
        merchant.blocked = blocked
        return mock_action(BlockMerchantAction, merchant)

    @pytest.fixture
    async def returned(self, params):
        return await BlockMerchantManagerAction(**params).run()

    @pytest.mark.parametrize('blocked', [True, False])
    def test_returned(self, returned, blocked):
        assert returned.blocked == blocked

    class TestBlockMerchantManagerActionDeniedForAssessor:
        @pytest.fixture
        def manager_uid(self, manager_assessor):
            return manager_assessor.uid

        @pytest.mark.asyncio
        async def test_deny_exception(self, params):
            with pytest.raises(ManagerActionNotAllowed):
                await BlockMerchantManagerAction(**params).run()


class TestRecreateMerchantModerationManagerAction:
    @pytest.fixture
    def manager_uid(self, manager_assessor):
        return manager_assessor.uid

    @pytest.fixture
    def params(self, manager_uid, merchant):
        return {
            'manager_uid': manager_uid,
            'uid': merchant.uid
        }

    @pytest.fixture
    def get_merchant_mock(self, mock_action, merchant):
        return mock_action(GetMerchantAction, merchant)

    @pytest.fixture
    def approved(self):
        return False

    @pytest.fixture
    def approved_effective_mock(self, mocker, approved):
        return mocker.patch.object(
            RecreateMerchantModerationManagerAction,
            'approved_effective_moderation',
            mocker.Mock(return_value=dummy_coro(approved)),
        )

    @pytest.fixture
    def moderation(self):
        return Moderation(uid=1, moderation_type=2, approved=True)

    @pytest.fixture(autouse=True)
    def schedule_moderation(self, mock_action, moderation):
        return mock_action(ScheduleMerchantModerationAction, moderation)

    @pytest.fixture
    async def returned(self, get_merchant_mock, params):
        return await RecreateMerchantModerationManagerAction(**params).run()

    def test_returned(self, returned, moderation):
        assert_that(
            returned,
            has_properties({
                'uid': moderation.uid,
                'moderation_type': moderation.moderation_type,
                'approved': moderation.approved
            }),
        )

    class TestModerationAlreadyExists:
        @pytest.fixture
        def approved(self):
            return True

        @pytest.mark.asyncio
        async def test_moderation_exists(self, params, merchant, get_merchant_mock, approved_effective_mock):
            with pytest.raises(ModerationAlreadyExistsError):
                await RecreateMerchantModerationManagerAction(**params).run()


class TestUpdateSupportCommentManagerAction(BaseTestParent):
    @pytest.fixture
    def manager_uid(self, manager_assessor):
        return manager_assessor.uid

    @pytest.fixture
    def support_comment(self):
        return 'test_comment'

    @pytest.fixture
    def params(self, manager_uid, merchant, support_comment):
        return {
            'manager_uid': manager_uid,
            'uid': merchant.uid,
            'support_comment': support_comment
        }

    @pytest.fixture
    def get_merchant_mock(self, mock_action, merchant):
        return mock_action(GetMerchantAction, merchant)

    @pytest.fixture
    async def returned(self, get_merchant_mock, params):
        return await UpdateSupportCommentManagerAction(**params).run()

    @pytest.mark.asyncio
    async def test_returned(self, returned, storage, support_comment):
        merchant = await storage.merchant.get(uid=returned.uid)
        assert merchant.support_comment == support_comment
