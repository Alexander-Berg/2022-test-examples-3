import base64
import json
from datetime import datetime, timezone
from random import randint

import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.common import SearchStats
from mail.payments.payments.core.entities.enums import AcquirerType, ModerationStatus
from mail.payments.payments.core.entities.keyset import KeysetEntry, ManagerMerchantListKeysetEntity
from mail.payments.payments.core.entities.merchant import MerchantsAdminData


def b64text(string: str) -> str:
    return base64.b64encode(string.encode('utf-8')).decode('ascii')


class TestGetMerchantListManagerHandler:
    @pytest.fixture
    def mock_keyset(self):
        return ManagerMerchantListKeysetEntity(sort_order=[])

    @pytest.fixture
    def run_action_result(self, merchant, mock_keyset):
        return MerchantsAdminData(merchants=[merchant], stats=SearchStats(total=1, found=1), keyset=mock_keyset)

    @pytest.fixture(autouse=True)
    def action(self, mock_action, run_action_result):
        from mail.payments.payments.core.actions.manager.merchant import GetMerchantListManagerAction
        return mock_action(GetMerchantListManagerAction, run_action_result)

    @pytest.fixture
    def acting_manager(self, manager_assessor):
        return manager_assessor

    @pytest.fixture
    def request_params(self):
        return {}

    @pytest.fixture
    def request_url(self):
        return '/admin/api/v1/merchant'

    @pytest.fixture
    async def response(self, action, admin_client, request_url, request_params, tvm):
        return await admin_client.get(request_url, params=request_params)

    @pytest.mark.parametrize('request_params', (
        {'limit': 100},
        {'merchant_uid': 'not-a-number'},
    ))
    def test_error(self, response):
        assert response.status == 400

    @pytest.mark.parametrize('request_params', (
        {},
        {
            'name': 'test-name',
            'username': 'test-login',
            'merchant_uid': randint(1, 10 ** 9),
            'client_id': 'test-client_id',
            'submerchant_id': 'test-submerchant_id',
        },
        {'limit': 5, 'offset': 10}
    ))
    def test_params(self, action, response, manager_assessor, request_params):
        request_params.setdefault('limit', 10)
        request_params.setdefault('offset', 0)
        action.assert_called_once_with(
            **request_params,
            manager_uid=manager_assessor.uid,
        )


class TestGetMerchantListManagerHandlerV2(TestGetMerchantListManagerHandler):
    @pytest.fixture
    def request_url(self):
        return '/admin/api/v2/merchant'

    @pytest.mark.parametrize('request_params, expected_params', (
        ({}, {'limit': 10}),
        (
            {
                'name': 'test-name',
                'username': 'test-login',
                'merchant_uid': 555,
                'client_id': 'test-client_id',
                'submerchant_id': 'test-submerchant_id',
                'acquirers[]': 'kassa',
                'moderation_status': 'ongoing',
            },
            {
                'name': 'test-name',
                'username': 'test-login',
                'merchant_uid': 555,
                'client_id': 'test-client_id',
                'submerchant_id': 'test-submerchant_id',
                'acquirers': [AcquirerType.KASSA],
                'moderation_status': ModerationStatus.ONGOING,
                'limit': 10,
            },
        ),
        (
            {
                'keyset': b64text(
                    json.dumps({
                        "sort_order": ["created", "uid"],
                        "created": {"barrier": "2020-01-01T00:00:00+00:00", "order": "asc"},
                        "uid": {"barrier": 456, "order": "asc"},
                    })
                )
            },
            {
                'keyset': ManagerMerchantListKeysetEntity(
                    sort_order=['created', 'uid'],
                    created=KeysetEntry(barrier=datetime(2020, 1, 1, 0, 0, 0, tzinfo=timezone.utc), order='asc'),
                    uid=KeysetEntry(barrier=456, order='asc'),
                ),
                'limit': 10,
            },
        ),
        ({'limit': 5}, {'limit': 5})
    ))
    def test_params(self, action, response, manager_assessor, request_params, expected_params):
        action.assert_called_once_with(
            **expected_params,
            manager_uid=manager_assessor.uid,
        )

    @pytest.mark.parametrize('mock_keyset, expected_keyset', (
        (
            ManagerMerchantListKeysetEntity(
                sort_order=['created', 'uid'],
                created=KeysetEntry(barrier=datetime(2020, 1, 1, 0, 0, 0, tzinfo=timezone.utc), order='asc'),
                uid=KeysetEntry(barrier=456, order='asc'),
            ),
            {
                'sort_order': ['created', 'uid'],
                'created': {
                    'barrier': '2020-01-01T00:00:00+00:00',
                    'order': 'asc',
                },
                'uid': {
                    'barrier': 456,
                    'order': 'asc',
                },
                'updated': None,
            },
        ),
    ))
    @pytest.mark.asyncio
    async def test_returned_keyset(self, response, expected_keyset):
        data = await response.json()
        assert json.loads(base64.b64decode(data['data']['next']['keyset']).decode('utf-8')) == expected_keyset

    @pytest.mark.asyncio
    async def test_response(self, response, run_action_result):
        resp_json = await response.json()
        assert_that(resp_json['data'], has_entries({
            'found': run_action_result.stats.found,
            'total': run_action_result.stats.total
        }))

    class TestNoneKeyset:
        @pytest.fixture
        def mock_keyset(self):
            return None

        @pytest.mark.asyncio
        async def test_none_keyset_returns_no_next(self, response):
            data = await response.json()
            assert 'next' not in data['data']

    @pytest.mark.parametrize('request_params', (
        {
            'keyset': 'malformed base64',
        },
        {
            'keyset': b64text('malformed json'),
        },
        pytest.param(
            {
                'keyset': b64text('["not-a-dict"]'),
            },
            marks=pytest.mark.skip()
        ),
        pytest.param(
            {
                'keyset': b64text(
                    json.dumps({
                        'sort_order': ['created', 'uid'],
                        'created': ['not-a-dict'],
                    })
                )
            },
            marks=pytest.mark.skip()
        ),
    ))
    def test_keyset_error(self, response):
        assert response.status == 400


class TestBlockMerchantHandler:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.manager.merchant import BlockMerchantManagerAction
        return mock_action(BlockMerchantManagerAction)

    @pytest.fixture
    def acting_manager(self, manager_admin):
        return manager_admin

    @pytest.fixture
    def run_action_result(self, merchant):
        merchant.blocked = True
        return merchant

    @pytest.fixture
    def post_params(self):
        return {
            'terminate_contract': True,
        }

    @pytest.fixture
    async def response(self, admin_client, post_params, merchant, tvm):
        return await admin_client.post(f'/admin/api/v1/merchant/{merchant.uid}/block', json=post_params)

    def test_params(self, response, action, manager_admin, post_params, merchant):
        action.assert_called_once_with(
            **post_params,
            uid=merchant.uid,
            manager_uid=manager_admin.uid
        )


class TestUpdateSupportCommentHandler:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.manager.merchant import UpdateSupportCommentManagerAction
        return mock_action(UpdateSupportCommentManagerAction)

    @pytest.fixture
    def acting_manager(self, manager_assessor):
        return manager_assessor

    @pytest.fixture
    def support_comment(self, rands):
        return rands()

    @pytest.fixture
    def post_params(self, support_comment):
        return {
            'support_comment': support_comment
        }

    @pytest.fixture
    async def response(self, admin_client, post_params, merchant, tvm):
        return await admin_client.post(f'/admin/api/v1/merchant/{merchant.uid}/support_comment', json=post_params)

    def test_params(self, response, action, merchant, manager_assessor, post_params):
        action.assert_called_once_with(
            **post_params,
            manager_uid=manager_assessor.uid,
            uid=merchant.uid,
        )

    @pytest.mark.parametrize('post_params', ({},))
    class TestInvalidPostParams:
        def test_invalid_post_params__response(self, response):
            assert response.status == 400


class TestRecreateMerchantModerationManagerHandler:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.manager.merchant import RecreateMerchantModerationManagerAction
        return mock_action(RecreateMerchantModerationManagerAction)

    @pytest.fixture
    def acting_manager(self, manager_assessor):
        return manager_assessor

    @pytest.fixture
    async def response(self, admin_client, merchant, tvm):
        return await admin_client.post(f'/admin/api/v1/merchant/{merchant.uid}/moderation')

    def test_params(self, response, action, acting_manager, merchant):
        action.assert_called_once_with(
            uid=merchant.uid,
            manager_uid=acting_manager.uid,
        )
