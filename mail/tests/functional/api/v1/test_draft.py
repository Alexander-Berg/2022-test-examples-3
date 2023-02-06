from copy import copy

import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.actions.merchant.create import InitClientAction, InitSubmerchantAction
from mail.payments.payments.core.entities.enums import MerchantRole, MerchantStatus
from mail.payments.payments.core.entities.service import ServiceMerchant
from mail.payments.payments.core.entities.userinfo import UserInfo
from mail.payments.payments.interactions.spark_suggest import SparkSuggestItem
from mail.payments.payments.tests.base import BaseTestMerchantRoles
from mail.payments.payments.tests.utils import MERCHANT_DATA_TEST_CASES
from mail.payments.payments.utils.helpers import without_none


@pytest.fixture
def acting_uid(randn):
    return randn()


@pytest.fixture(autouse=True)
def blackbox_mock(blackbox_client_mocker, acting_uid, rands):
    user_info = UserInfo(uid=acting_uid, default_email=rands())
    with blackbox_client_mocker('userinfo', user_info, multiple_calls=True) as mock:
        yield mock


@pytest.fixture
def suggest_list():
    return [
        SparkSuggestItem(
            spark_id=1,
            name='Name',
            full_name='Full Name',
            inn='1234567890',
            ogrn='1234567890123',
            address='119021, г. Москва, ул. Льва Толстого, д. 16',
            leader_name='Leader Name',
            region_name='Москва',
        )
    ]


@pytest.fixture(autouse=True)
def spark_suggest_get_hint_mock(spark_suggest_client_mocker, suggest_list):
    with spark_suggest_client_mocker('get_hint', result=suggest_list) as mock:
        yield mock


class BaseTestCreateDraft:

    @pytest.fixture
    def request_json_update(self):
        return {}

    @pytest.fixture(params=MERCHANT_DATA_TEST_CASES)
    def request_json(self, request, request_json_update):
        result = copy(request.param)
        result.update(request_json_update)
        return result

    @pytest.fixture
    def acting_uid(self, merchant_uid):
        return merchant_uid

    @pytest.fixture
    def url(self, acting_uid):
        return f'/v1/merchant/{acting_uid}/draft'

    @pytest.fixture
    def response_func(self, url, client, request_json, tvm):
        async def _inner():
            return await client.post(url, json=request_json)

        return _inner

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    @pytest.fixture
    async def response_data(self, response):
        assert response.status == 200
        return await response.json()

    @pytest.fixture
    async def created_merchant(self, storage, response_data):
        return await storage.merchant.get(response_data['data']['uid'])

    @pytest.fixture
    async def created_user_role(self, storage, response_data):
        return await storage.user_role.get(
            uid=response_data['data']['uid'],
            merchant_id=response_data['data']['merchant_id'],
        )

    def test_created_merchant_is_draft(self, created_merchant):
        assert created_merchant.status == MerchantStatus.DRAFT

    def test_created_user_role_is_owner(self, created_user_role):
        assert created_user_role.role == MerchantRole.OWNER

    def test_response(self, request_json, response_data):
        has_inn = request_json.get('organization') and request_json['organization'].get('inn')
        has_legal_address = request_json.get('addresses') and request_json['addresses'].get('legal')
        if has_inn:
            request_json.pop('organization')
            response_data['data']['addresses'].pop('legal', None)
            if has_legal_address:
                request_json['addresses'].pop('legal')
        assert_that(without_none(response_data['data']), has_entries(request_json))


class TestCreateDraft(BaseTestCreateDraft):
    class TestCreateDraftForDifferentUID:
        @pytest.fixture
        def url(self, unique_rand, randn):
            return f'/v1/merchant/{unique_rand(randn, basket="uid")}/draft'

        def test_forbidden(self, response):
            assert response.status == 403


class TestUpdateDraft(BaseTestMerchantRoles, BaseTestCreateDraft):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
    )

    @pytest.fixture
    def merchant_id(self, merchant_draft):
        return merchant_draft.merchant_id

    @pytest.fixture
    def acting_uid(self, merchant_draft_uid):
        return merchant_draft_uid


class TestInternalCreateDraft(BaseTestCreateDraft):
    @pytest.fixture(params=(True, False))
    def autoenable(self):
        return False

    @pytest.fixture
    def tvm_uid(self, acting_uid):
        return acting_uid

    @pytest.fixture
    def url(self, acting_uid):
        return f'/v1/internal/merchant/{acting_uid}/draft'

    @pytest.fixture
    def entity_id(self, rands):
        return rands()

    @pytest.fixture
    async def created_merchant(self, storage, response_data):
        return await storage.merchant.get(response_data['data']['merchant']['uid'])

    @pytest.fixture
    async def created_user_role(self, storage, response_data):
        return await storage.user_role.get(
            uid=response_data['data']['merchant']['uid'],
            merchant_id=response_data['data']['merchant']['merchant_id'],
        )

    @pytest.fixture
    def request_json_update(self, entity_id, autoenable):
        return {"description": "Service-Merchant Link",
                "entity_id": entity_id,
                "autoenable": autoenable}

    def test_response(self, request_json, response_data, request_json_update):
        for k, _ in request_json_update.items():
            request_json.pop(k, None)
        has_inn = request_json.get('organization') and request_json['organization'].get('inn')
        has_legal_address = request_json.get('addresses') and request_json['addresses'].get('legal')
        if has_inn:
            request_json.pop('organization')
            response_data['data']['merchant']['addresses'].pop('legal', None)
            if has_legal_address:
                request_json['addresses'].pop('legal')
        assert_that(without_none(response_data['data']['merchant']), has_entries(request_json))

    def test_prefill_organization(self, request_json, response_data, request_json_update, suggest_list):
        has_inn = request_json.get('organization') and request_json['organization'].get('inn')
        if has_inn:
            organization_request = request_json['organization']
            organization_response = response_data['data']['merchant']['organization']
            for field in ('name', 'inn', 'ogrn'):
                expected = organization_request.get(field) or getattr(suggest_list[0], field)
                assert organization_response[field] == expected
            expected = organization_request.get('fullName') or getattr(suggest_list[0], 'full_name')
            assert organization_response['fullName'] == expected
            assert_that(organization_response, has_entries(organization_request))

    def test_prefill_legal_address(self, request_json, response_data, request_json_update):
        has_inn = request_json.get('organization') and request_json['organization'].get('inn')
        has_legal_address = request_json.get('addresses') and request_json['addresses'].get('legal')
        if has_inn:
            legal_address_response = response_data['data']['merchant']['addresses']['legal']
            for field in ('zip', 'city', 'street', 'home'):
                assert legal_address_response[field] is not None
            if has_legal_address:
                legal_address_request = request_json['addresses']['legal']
                assert_that(legal_address_response, has_entries(legal_address_request))

    class TestDeny:
        @pytest.fixture
        def autoenable(self):
            return True

        @pytest.fixture
        def tvm_uid(self, randn):
            return randn()

        def test_deny(self, response):
            assert response.status == 403


@pytest.mark.usefixtures('tvm')
class TestInternalUpdateDraft(TestInternalCreateDraft):
    @pytest.fixture(autouse=True)
    async def setup(self, storage, service, merchant_draft, entity_id, rands):
        await storage.service_merchant.create(
            ServiceMerchant(service_id=service.service_id,
                            uid=merchant_draft.uid,
                            entity_id=entity_id,
                            description=rands())
        )

    @pytest.fixture
    def acting_uid(self, merchant_draft_uid):
        return merchant_draft_uid


class TestUpdateRegisteredMerchantAsDraft:
    @pytest.fixture(params=MERCHANT_DATA_TEST_CASES)
    def request_json(self, request, merchant):
        return request.param

    @pytest.fixture
    def response_func(self, merchant, client, request_json, tvm):
        async def _inner():
            return await client.post(f'/v1/merchant/{merchant.uid}/draft', json=request_json)

        return _inner

    @pytest.mark.asyncio
    async def test_response_data(self, response_func):
        response = await response_func()
        resp_json = await response.json()
        assert all((response.status == 400,
                    resp_json['data']['message'] == 'MERCHANT_IS_ALREADY_REGISTERED'))


@pytest.mark.usefixtures('tvm')
class TestInternalUpdateRegisteredMerchantAsDraft(TestUpdateRegisteredMerchantAsDraft):
    @pytest.fixture
    def request_json_update(self, entity_id):
        return {"description": "Service-Merchant Link", "entity_id": entity_id}


class TestGetMerchantDraft(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    def merchant_id(self, merchant_draft):
        return merchant_draft.uid

    @pytest.fixture
    def response_func(self, merchant_draft, client, tvm):
        async def _inner():
            return await client.get(f'/v1/merchant/{merchant_draft.uid}')

        return _inner

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    @pytest.fixture
    async def response_data(self, response):
        assert response.status == 200
        return await response.json()

    @pytest.mark.asyncio
    async def test_response_data(self, response_data, merchant_data_draft):
        assert_that(without_none(response_data['data']), has_entries(merchant_data_draft))


class TestRegisterDraftAsMerchant(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
    )

    @pytest.fixture
    def client_id(self):
        return 'test-post-merchant-client_id'

    @pytest.fixture
    def person_id(self):
        return 'test-post-merchant-person_id'

    @pytest.fixture
    def submerchant_id(self):
        return 'test-post-merchant-submerchant_id'

    @pytest.fixture(autouse=True)
    def run_action_mock(self, mocker, client_id, person_id, submerchant_id):
        async def dummy_init_client(self):
            self.merchant.client_id = client_id
            self.merchant.person_id = person_id

        async def dummy_init_submerchant(self):
            self.merchant.submerchant_id = submerchant_id

        mocker.patch.object(InitClientAction, 'run', dummy_init_client)
        mocker.patch.object(InitSubmerchantAction, 'run', dummy_init_submerchant)

    @pytest.fixture
    def request_json(self):
        return {
            'name': 'test-merchant-post-name',
            'addresses': {
                'legal': {
                    'city': 'test-merchant-post-city',
                    'country': 'RUS',
                    'home': 'test-merchant-post-home',
                    'street': 'test-merchant-post-street',
                    'zip': '123456',
                },
            },
            'bank': {
                'account': 'test-merchant-post-account',
                'bik': '123456789',
                'correspondentAccount': '12345678901234567890',
                'name': 'test-merchant-post-name',
            },
            'organization': {
                'type': 'ooo',
                'name': 'test-merchant-post-name',
                'englishName': 'english_name',
                'fullName': 'test-merchant-post-full_name',
                'inn': '1234567890',
                'kpp': '0987654321',
                'ogrn': '1234567890123',
                'scheduleText': 'test-merchant-schedule-text',
                'siteUrl': 'test-merchant-post-site_url',
                'description': 'test-merchant-post-description',
            },
            'persons': {
                'ceo': {
                    'name': 'test-merchant-post-ceo-name',
                    'email': 'test-merchant-post-ceo-email@mail.ru',
                    'phone': 'test-merchant-post-ceo-phone',
                    'surname': 'test-merchant-post-ceo-surname',
                    'patronymic': 'test-merchant-post-ceo-patronymic',
                    'birthDate': '2019-03-14',
                },
                'signer': {
                    'name': 'test-merchant-post-signer-name',
                    'email': 'test-merchant-post-signer-email@gmail.com',
                    'phone': 'test-merchant-post-signer-phone',
                    'surname': 'test-merchant-post-signer-surname',
                    'patronymic': 'test-merchant-post-signer-patronymic',
                    'birthDate': '2019-03-13',
                },
            },
            'username': 'test-merchant-username',
        }

    @pytest.fixture
    def merchant_id(self, merchant_draft):
        return merchant_draft.merchant_id

    @pytest.fixture
    def response_func(self, merchant_draft, client, request_json, tvm):
        async def _inner():
            return await client.post(f'/v1/merchant/{merchant_draft.uid}', json=request_json)

        return _inner

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    @pytest.fixture
    async def response_data(self, response):
        assert response.status == 200
        return await response.json()

    def test_response(self, request_json, response_data):
        assert_that(response_data['data'], has_entries(request_json))

    def test_new_status(self, response_data):
        assert response_data['data']['status'] == 'new'


class TestPostMerchantDraftModerationNotAllowed(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
    )

    @pytest.fixture
    def merchant_id(self, merchant_draft):
        return merchant_draft.merchant_id

    @pytest.fixture
    def response_func(self, client, merchant_draft, tvm):
        async def _inner():
            r = await client.post(f'/v1/merchant/{merchant_draft.uid}/moderation')
            assert r.status == 403
            return await r.json()

        return _inner

    @pytest.mark.asyncio
    async def test_already_registered(self, response_func):
        response = await response_func()
        assert response['data']['message'] == 'Action access denied'


class TestGetMerchantDraftTokenNotAllowed(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    def merchant_id(self, merchant_draft):
        return merchant_draft.merchant_id

    @pytest.fixture
    def response_func(self, client, merchant_draft, tvm):
        async def _inner():
            r = await client.get(f'/v1/merchant/{merchant_draft.uid}/token')
            assert r.status == 403
            return await r.json()

        return _inner

    @pytest.mark.asyncio
    async def test_already_registered(self, response_func):
        response = await response_func()
        assert response['data']['message'] == 'Action access denied'
