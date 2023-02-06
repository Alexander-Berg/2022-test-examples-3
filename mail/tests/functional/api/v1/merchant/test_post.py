import pytest

from hamcrest import assert_that, has_entries, has_properties

from mail.payments.payments.core.actions.merchant.create import InitClientAction, InitSubmerchantAction
from mail.payments.payments.core.entities.enums import FunctionalityType, MerchantRole, ModerationType
from mail.payments.payments.core.entities.moderation import Moderation
from mail.payments.payments.core.entities.userinfo import UserInfo
from mail.payments.payments.storage.exceptions import MerchantNotFound
from mail.payments.payments.tests.base import BaseTestMerchantRoles
from mail.payments.payments.utils.const import (
    ENTREPRENEUR_PREFIX_FULL, ENTREPRENEUR_PREFIX_SHORT, MAX_LENGTH_ENGLISH_NAME
)
from mail.payments.payments.utils.helpers import transliterate_to_eng


class TestPostMerchant:
    @pytest.fixture(autouse=True)
    def blackbox_mock(self, blackbox_client_mocker, merchant_uid, rands):
        user_info = UserInfo(uid=merchant_uid, default_email=rands())
        with blackbox_client_mocker('userinfo', user_info) as mock:
            yield mock

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
            'fast_moderation': True,
        }

    @pytest.fixture
    def response_func(self, storage, merchant_uid, client, tvm, request_json):
        async def _inner():
            return await client.post(f'/v1/merchant/{merchant_uid}', json=request_json)

        return _inner

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    @pytest.fixture
    async def response_data(self, response):
        return await response.json()

    @pytest.fixture
    async def created_merchant(self, storage, response_data):
        return await storage.merchant.get(response_data['data']['uid'])

    @pytest.fixture
    async def created_user_role(self, storage, created_merchant):
        return await storage.user_role.get(
            uid=created_merchant.uid,
            merchant_id=created_merchant.merchant_id,
        )

    class TestCreate:
        @pytest.fixture
        def response_func(self, storage, merchant_uid, response_func):
            async def _inner():
                try:
                    merchant = await storage.merchant.get(merchant_uid)
                except MerchantNotFound:
                    merchant = None
                assert merchant is None, 'Merchant must not exist during creation tests.'
                return await response_func()

            return _inner

        def test_response(self, request_json, response_data):
            assert_that(response_data['data'], has_entries(request_json))

        @pytest.mark.asyncio
        async def test_without_site_url(self, request_json, response_func):
            request_json['organization'].pop('siteUrl')
            r = await response_func()
            request_json['organization']['siteUrl'] = None
            assert_that((await r.json())['data'], has_entries(request_json))

        @pytest.mark.asyncio
        async def test_without_description(self, request_json, response_func):
            request_json['organization'].pop('description')
            r = await response_func()
            request_json['organization']['description'] = None
            assert_that((await r.json())['data'], has_entries(request_json))

        @pytest.mark.asyncio
        async def test_without_patronymic(self, request_json, response_func):
            request_json['persons']['ceo'].pop('patronymic')
            r = await response_func()
            request_json['persons']['ceo']['patronymic'] = None
            assert_that((await r.json())['data'], has_entries(request_json))

        @pytest.mark.asyncio
        async def test_oauth(self, response_data):
            assert_that(response_data['data'], has_entries({'oauth': []}))

        def test_creates_merchant(self, client_id, person_id, submerchant_id, created_merchant):
            assert_that(
                created_merchant,
                has_properties({
                    'client_id': client_id,
                    'person_id': person_id,
                    'submerchant_id': submerchant_id,
                })
            )

        def test_creates_user_role(self, created_user_role):
            assert created_user_role.role == MerchantRole.OWNER

        class TestUIDMismatch:
            @pytest.fixture
            def tvm_uid(self, unique_rand, randn):
                return unique_rand(randn, basket='uid')

            @pytest.mark.asyncio
            async def test_forbidden(self, response_func):
                r = await response_func()
                assert r.status == 403

        class TestFillEntrepreneurOrganizationNames:
            @pytest.fixture
            def request_json(self, request_json):
                organization = request_json['organization']
                organization.pop('englishName')
                organization.pop('fullName')
                organization.pop('name')
                organization['inn'] = '01234567890'  # entrepreneur
                return request_json

            def test_fill_entrepreneur_organization_names__response(self, request_json, response_data):
                ceo = request_json['persons']['ceo']
                name = f"{ENTREPRENEUR_PREFIX_SHORT}{ceo['surname']} {ceo['name'][0]}.{ceo['patronymic'][0]}."

                request_json['organization'].update({
                    'englishName': transliterate_to_eng(name)[0:MAX_LENGTH_ENGLISH_NAME],
                    'fullName': f"{ENTREPRENEUR_PREFIX_FULL}{ceo['surname']} {ceo['name']} {ceo['patronymic']}",
                    'name': name
                })
                assert_that(response_data['data'], has_entries(request_json))

    class TestUpdate(BaseTestMerchantRoles):
        ALLOWED_ROLES = (
            MerchantRole.OWNER,
            MerchantRole.ADMIN,
        )

        @pytest.mark.asyncio
        async def test_updates_merchant(self, storage, merchant, request_json, response_func):
            merchant.name = 'name-before'
            await storage.merchant.save(merchant)
            request_json['name'] = 'name-after'
            request_json['organization']['inn'] = merchant.organization.inn
            await response_func()
            merchant = await storage.merchant.get(merchant.uid)
            assert merchant.name == request_json['name']


@pytest.mark.parametrize('functionality_type, json_data', (
    (FunctionalityType.PAYMENTS, None),
    (FunctionalityType.PAYMENTS, {'functionality_type': FunctionalityType.PAYMENTS.value}),
    (FunctionalityType.YANDEX_PAY, {'functionality_type': FunctionalityType.YANDEX_PAY.value}),
))
@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestPostMerchantModeration(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
    )

    @pytest.fixture
    def response_func(self, client, merchant, functionality_type, tvm, json_data):
        async def _inner(status=200):
            r = await client.post(f'/v1/merchant/{merchant.uid}/moderation', json=json_data)
            assert r.status == status
            return await r.json()

        return _inner

    @pytest.mark.asyncio
    async def test_already_approved(self, moderation, response_func):
        response = await response_func(403)
        assert response['data']['message'] == 'Action access denied'

    @pytest.mark.asyncio
    async def test_success(self, response_func):
        response = await response_func()
        assert response['data'] is None

    @pytest.fixture
    async def moderation(self, storage, merchant, functionality_type):
        await storage.moderation.create(Moderation(
            uid=merchant.uid,
            revision=merchant.revision,
            moderation_type=ModerationType.MERCHANT,
            functionality_type=functionality_type,
            approved=True,
        ))
