import base64
import json
import re
from datetime import datetime, timezone

import pytest

from hamcrest import assert_that, equal_to, has_entries, has_item, has_properties, match_equality, not_none

from mail.payments.payments.core.actions.manager.merchant import RecreateMerchantModerationManagerAction
from mail.payments.payments.core.entities.document import Document
from mail.payments.payments.core.entities.enums import (
    AcquirerType, DocumentType, FunctionalityType, MerchantStatus, ModerationRejectReason, ModerationStatus,
    ModerationType, TaskType
)
from mail.payments.payments.core.entities.merchant import AddressData
from mail.payments.payments.core.entities.moderation import Moderation
from mail.payments.payments.interactions.balance.exceptions import BalanceContractNotFound
from mail.payments.payments.storage.mappers.merchant import MerchantMapper
from mail.payments.payments.tests.utils import check_merchant_from_person, dummy_coro

from .base import BaseTestNotAuthorized


class BaseGetMerchantsAdminTest(BaseTestNotAuthorized):
    @staticmethod
    def check_merchant(merchant, merchant_dict):
        assert_that(
            merchant_dict,
            has_entries({
                'uid': merchant.uid,
                'name': merchant.name,
                'revision': merchant.revision,
                'status': merchant.status.value,
                'created': merchant.created.astimezone(timezone.utc).isoformat(),
                'updated': merchant.updated.astimezone(timezone.utc).isoformat(),
                'username': merchant.username,
                'support_comment': merchant.support_comment,
            })
        )

    @pytest.fixture(autouse=True)
    def mock_blackbox_user_info(self, blackbox_client_mocker, merchant, mocker):
        with blackbox_client_mocker('userinfo', mocker.Mock(uid=merchant.uid)) as mock:
            yield mock

    @pytest.fixture(autouse=True, params=('assessor', 'admin'))
    def acting_manager(self, request, managers):
        return managers[request.param]

    @pytest.fixture
    def tvm_uid(self, acting_manager):
        return acting_manager.uid

    @pytest.fixture(autouse=True)
    def setup_spy(self, mocker):
        mocker.spy(MerchantMapper, 'find')
        mocker.spy(MerchantMapper, 'get_found_count')

    @pytest.fixture
    def merchant_document(self, randn):
        return Document(
            document_type=DocumentType.PASSPORT,
            path='test-document-get-passport-path',
            size=randn(),
            name='test-document-get-passport-name',
        )

    @pytest.fixture
    def merchant_documents(self, merchant_document):
        return [merchant_document]

    @pytest.fixture
    def request_params(self):
        return {}

    @pytest.fixture
    async def response(self,
                       request_url,
                       admin_client,
                       tvm,
                       request_params,
                       balance_client_mocker,
                       person_entity):
        return await admin_client.get(request_url, params=request_params)

    @pytest.fixture
    async def response_post_address(self,
                                    request_url,
                                    admin_client,
                                    tvm,
                                    request_params,
                                    balance_client_mocker,
                                    stored_person_entity,
                                    merchant,
                                    storage):
        merchant.data.addresses.append(AddressData(
            type='post',
            city='person-address_city',
            country='RUS',
            home='person-address_home',
            street='person-address_street',
            zip='person-address_postcode'
        ))
        await storage.merchant.save(merchant)
        return await admin_client.get(request_url, params=request_params)

    @pytest.fixture
    async def response_data(self, response):
        return (await response.json())['data']

    @pytest.fixture
    async def response_data_post_address(self, response_post_address):
        return (await response_post_address.json())['data']

    @pytest.fixture
    def response_merchants(self, response_data):
        return response_data

    @pytest.fixture
    def response_merchants_post_address(self, response_data_post_address):
        return response_data_post_address

    class TestGetByUID:
        @pytest.fixture
        def request_params(self, merchant):
            return {'merchant_uid': merchant.uid}

        def test_single_merchant(self, response_merchants):
            assert len(response_merchants) == 1

        def test_merchant_data(self, merchant, stored_person_entity, response_merchants):
            check_merchant_from_person(merchant, stored_person_entity, response_merchants[0])

    class TestGetEmpty:
        @pytest.fixture
        def request_params(self, unique_rand, randn):
            return {
                'merchant_uid': unique_rand(randn, basket='uid'),
                'client_id': unique_rand(randn, basket='client_id')
            }

        def test_get_empty__empty_response(self, response_merchants):
            assert response_merchants == []

    class TestFilterByUsernameNotFound:
        @pytest.fixture(autouse=True)
        def mock_blackbox_user_info(self, aioresponses_mocker, merchant, payments_settings):
            aioresponses_mocker.get(
                re.compile(f'^{payments_settings.BLACKBOX_API_URL}.*'),
                payload={
                    'users': [
                        {'id': ''}
                    ]
                }
            )

        @pytest.fixture
        def request_params(self, unique_rand, randn, rands):
            return {
                'username': unique_rand(rands)
            }

        def test_filter_by_username_not_found__empty_response(self, response_merchants):
            assert response_merchants == []


@pytest.mark.usefixtures('balance_person_mock')
class TestGetMerchantsAdmin(BaseGetMerchantsAdminTest):
    @pytest.fixture(params=('created', 'updated', 'uid'))
    def sort_by(self, request):
        return request.param

    @pytest.fixture(params=(True, False))
    def desc(self, request):
        return request.param

    @pytest.fixture
    def request_params(self, sort_by, merchant, desc):
        return {
            'merchant_uid': merchant.uid,
            'sort_by': sort_by,
            'desc': str(desc).lower(),
        }

    @pytest.fixture
    def request_url(self):
        return '/admin/api/v1/merchant'

    def test_merchant(self, response_merchants, merchant):
        assert len(response_merchants) == 1
        self.check_merchant(merchant, response_merchants[0])

    class TestInvalidSortBy:
        @pytest.fixture
        def sort_by(self, rands):
            return rands()

        def test_bad_request(self, response):
            assert response.status == 400

    def test_sort_mapper_params(self, sort_by, desc, response):
        assert_that(
            MerchantMapper.find.call_args[1],
            has_entries({
                'sort_by': sort_by,
                'descending': desc,
            })
        )

    @pytest.mark.asyncio
    async def test_post_address(self, merchant, stored_person_entity, response_merchants_post_address):
        stored_person_entity.address_city = 'person-address_city'
        stored_person_entity.address_home = 'person-address_home'
        stored_person_entity.address_postcode = 'person-address_postcode'
        stored_person_entity.address_street = 'person-address_street'
        check_merchant_from_person(merchant, stored_person_entity, response_merchants_post_address[0])

    @pytest.mark.asyncio
    async def test_documents(self, response_merchants, merchant_document):
        assert_that(response_merchants[0]['documents'][merchant_document.document_type.value][0],
                    has_entries({
                        'path': merchant_document.path,
                        'size': merchant_document.size,
                        'created': merchant_document.created.isoformat(),
                        'name': merchant_document.name,
                    }))


@pytest.mark.usefixtures('balance_person_mock')
class TestGetMerchantsAdminV2(TestGetMerchantsAdmin):
    @pytest.fixture
    def request_url(self):
        return '/admin/api/v2/merchant'

    @pytest.fixture
    def response_merchants(self, response_data):
        return response_data['merchants']

    @pytest.fixture
    def response_merchants_post_address(self, response_data_post_address):
        return response_data_post_address['merchants']

    @pytest.fixture
    def acquirer(self):
        return AcquirerType.KASSA

    @pytest.fixture
    def moderation_status(self):
        return ModerationStatus.NONE

    @pytest.fixture
    def count_mapper_params(self, merchant, acquirer, moderation_status):
        return {
            'uid': merchant.uid,
            'acquirers': [acquirer],
            'moderation_status': moderation_status,
        }

    @pytest.fixture
    def request_params(self, sort_by, merchant, desc, acquirer, moderation_status):
        return {
            'merchant_uid': merchant.uid,
            'sort_by': sort_by,
            'desc': str(desc).lower(),
            'acquirers[]': acquirer.value,
            'moderation_status': moderation_status.value,
        }

    def test_sort_mapper_params(self, sort_by, desc, acquirer, moderation_status, response):
        assert_that(
            MerchantMapper.find.call_args[1],
            has_entries({
                'sort_by': sort_by,
                'descending': desc,
                'acquirers': [acquirer],
                'moderation_status': moderation_status,
            })
        )

    @pytest.mark.asyncio
    async def test_stats(self, storage, count_mapper_params, response_data):
        total = await storage.merchant.get_found_count(statuses=[
            MerchantStatus.NEW, MerchantStatus.ACTIVE, MerchantStatus.INACTIVE
        ])
        found = await storage.merchant.get_found_count(**count_mapper_params)
        assert all((
            response_data['found'] == found,
            response_data['total'] == total
        ))

    class TestModerationRejectReasons:
        @pytest.fixture
        def request_params(self, merchant):
            return {
                'merchant_uid': merchant.uid
            }

        @pytest.mark.parametrize('moderations_data,response_moderation', (
            pytest.param(
                [
                    {
                        'approved': False,
                        'reasons': [1, 2],
                    },
                    {'approved': None}
                ],
                match_equality(
                    has_entries({
                        'approved': False,
                        'reasons': [
                            ModerationRejectReason.DOCUMENT_PROBLEMS.value,
                            ModerationRejectReason.UNKNOWN.value
                        ],
                        'hasOngoing': True,
                        'hasModeration': True,
                        'status': ModerationStatus.ONGOING.value,
                        'updated': not_none(),
                    })
                ),
                id='disapproved-ongoing',
            ),
        ))
        class TestModeration:
            @pytest.fixture
            async def create_moderations(self, storage, merchant, moderations_data):
                for moderation_data in moderations_data:
                    merchant = await storage.merchant.save(merchant)
                    await storage.moderation.create(Moderation(
                        uid=merchant.uid,
                        moderation_type=ModerationType.MERCHANT,
                        functionality_type=FunctionalityType.PAYMENTS,
                        revision=merchant.revision,
                        **moderation_data,
                    ))

            @pytest.mark.asyncio
            async def test_moderation(self,
                                      create_moderations,
                                      response_merchants,
                                      response_moderation):
                assert response_merchants[0]['moderation'] == response_moderation


@pytest.mark.usefixtures('balance_person_mock')
class TestGetMerchantsAdminV2KeysetPagination(BaseGetMerchantsAdminTest):
    @pytest.fixture
    def response_merchants(self, response_data):
        return response_data['merchants']

    @pytest.fixture
    def request_url(self):
        return '/admin/api/v2/merchant'

    @pytest.fixture
    def merchant_name(self, rands):
        """ Фикстура нужна, чтобы в тесте участвовали только нужные нам продавцы. """
        name = 'functional::TestGetMerchantsAdminV2KeysetPagination::' + rands()
        return name.lower()

    @pytest.fixture(autouse=True)
    async def merchants(self, create_merchant, merchant_name):
        merchants = []
        for day in range(1, 4):
            merchant = await create_merchant(
                name=merchant_name,
                created=datetime(2020, 1, day, 0, 0, 0, tzinfo=timezone.utc),
            )
            merchants.append(merchant)
        merchants.sort(key=lambda item: item.created)
        return merchants

    class TestFirstPage:
        @pytest.fixture
        def request_params(self, merchant_name):
            return {'name': merchant_name, 'sort_by': 'created', 'limit': 1, 'desc': 'false'}

        def test_first_page_merchants(self, merchants, response_merchants):
            expected_uids = [merchants[0].uid]
            actual_uids = [m['uid'] for m in response_merchants]
            assert actual_uids == expected_uids

        def test_first_page_keyset(self, merchants, response_data, response_merchants):
            keyset = json.loads(base64.b64decode(response_data['next']['keyset']).decode('utf-8'))
            assert_that(
                keyset,
                equal_to({
                    'created': {'order': 'asc', 'barrier': '2020-01-01T00:00:00+00:00'},
                    'uid': {'order': 'asc', 'barrier': response_merchants[0]['uid']},
                    'updated': None,
                    'sort_order': ['created', 'uid']
                })
            )

    class TestNextPage:
        @pytest.fixture
        def request_params(self, merchants, merchant_name):
            return {
                'name': merchant_name,
                'sort_by': 'created',
                'limit': 1,
                'desc': 'false',
                'keyset': base64.b64encode(
                    json.dumps({
                        'created': {
                            'order': 'asc',
                            'barrier': '2020-01-01T00:00:00+00:00',
                        },
                        'uid': {
                            'order': 'asc',
                            'barrier': merchants[0].uid,
                        },
                        'sort_order': ['created', 'uid'],
                    }).encode('utf-8')
                ).decode('ascii'),
            }

        def test_next_page_merchants(self, merchants, response_merchants):
            expected_uids = [merchants[1].uid]
            actual_uids = [m['uid'] for m in response_merchants]
            assert actual_uids == expected_uids

        def test_next_page_keyset(self, merchants, response_data, response_merchants):
            keyset = json.loads(base64.b64decode(response_data['next']['keyset']).decode('utf-8'))
            assert_that(
                keyset,
                equal_to({
                    'created': {'order': 'asc', 'barrier': '2020-01-02T00:00:00+00:00'},
                    'uid': {'order': 'asc', 'barrier': response_merchants[0]['uid']},
                    'updated': None,
                    'sort_order': ['created', 'uid']
                })
            )


class TestBlockMerchant(BaseTestNotAuthorized):
    @pytest.fixture
    def tvm_uid(self, manager_admin):
        return manager_admin.uid

    @pytest.fixture
    async def response(self, admin_client, merchant, tvm):
        return await admin_client.post(f'/admin/api/v1/merchant/{merchant.uid}/block')

    @pytest.mark.asyncio
    async def test_merchant_is_not_blocked_if_balance_client_has_failed(self, response, storage, merchant):
        db_merchant = await storage.merchant.get(merchant.uid)
        assert not db_merchant.blocked

    class TestCorrectBlocking:
        @pytest.fixture(autouse=True)
        def create_collateral_mock(self, balance_client_mocker):
            with balance_client_mocker('create_collateral') as mock:
                yield mock

        @pytest.mark.asyncio
        async def test_correct_blocking__merchant_should_be_blocked(self, response, storage, merchant):
            db_merchant = await storage.merchant.get(merchant.uid)
            assert db_merchant.blocked

    class TestBalanceContractNotFoundError:
        @pytest.fixture(autouse=True)
        def create_collateral_mock(self, balance_client_mocker):
            with balance_client_mocker('create_collateral', exc=BalanceContractNotFound) as mock:
                yield mock

        @pytest.mark.asyncio
        async def test_should_not_block_and_should_respond_not_found(self, response, response_data, storage, merchant):
            db_merchant = await storage.merchant.get(merchant.uid)
            assert not db_merchant.blocked and \
                   response.status == 404 and \
                   response_data['message'] == 'CORE_NOT_FOUND' and \
                   response_data['params']['message'] == 'BALANCE_CONTRACT_NOT_FOUND'

    class TestNotAllowedManager:
        @pytest.fixture
        def tvm_uid(self, manager_assessor):
            return manager_assessor.uid

        def test_not_allowed(self, response):
            assert response.status == 403


@pytest.mark.usefixtures('balance_person_mock')
class TestRecreateMerchantModerationManager(BaseTestNotAuthorized):
    @pytest.fixture
    def tvm_uid(self, manager_admin):
        return manager_admin.uid

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
    async def response(self, approved_effective_mock, admin_client, merchant, tvm):
        return await admin_client.post(f'/admin/api/v1/merchant/{merchant.uid}/moderation')

    def test_task_params(self, response, tasks, merchant):
        assert_that(
            tasks,
            has_item(has_properties({
                'params': has_entries({
                    'merchant_uid': merchant.uid,
                }),
                'task_type': TaskType.START_MODERATION,
            }))
        )

    class TestModerationAlreadyExists:
        @pytest.fixture
        def approved(self):
            return True

        def test_already_exists(self, response):
            assert response.status == 400


@pytest.mark.usefixtures('balance_person_mock')
class TestUpdateSupportComment(BaseTestNotAuthorized):
    @pytest.fixture
    def tvm_uid(self, manager_admin):
        return manager_admin.uid

    @pytest.fixture
    def support_comment(self, rands):
        return rands()

    @pytest.fixture
    def json(self, support_comment):
        return {'support_comment': support_comment}

    @pytest.fixture
    async def response(self, admin_client, merchant, tvm, json):
        return await admin_client.post(f'/admin/api/v1/merchant/{merchant.uid}/support_comment', json=json)

    @pytest.mark.asyncio
    async def test_returned_merchant(self, support_comment, response, storage, merchant):
        db_merchant = await storage.merchant.get(merchant.uid)
        assert db_merchant.support_comment == support_comment
