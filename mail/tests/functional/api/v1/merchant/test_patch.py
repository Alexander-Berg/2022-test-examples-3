import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.core.entities.merchant import MerchantData, OrganizationData
from mail.payments.payments.tests.base import BaseTestMerchantRoles


class TestMerchantPatch:
    @pytest.fixture
    def merchant_name(self):
        return 'test-merchant-patch-name'

    @pytest.fixture
    def request_json(self, merchant_name):
        return {
            'name': merchant_name,
            'addresses': {
                'legal': {
                    'city': 'Moscow',
                    'country': 'RUS',
                    'home': '16',
                    'street': 'Lva Tolstogo',
                    'zip': '123456',
                },
            },
            'bank': {
                'account': '111111',
                'bik': '222222',
                'correspondentAccount': '333333',
                'name': '444444',
            },
            'organization': {
                'type': 'ooo',
                'name': 'Hoofs & Horns',
                'englishName': 'HH',
                'fullName': 'H & H',
                'kpp': '1234567890',
                'ogrn': '1234567890123',
                'scheduleText': 'test-merchant-schedule-text',
                'siteUrl': 'sell.yandex.ru',
                'description': 'test-merchant-description',
            },
            'persons': {
                'ceo': {
                    'name': 'Name',
                    'email': 'email@ya.ru',
                    'phone': '+711_phone',
                    'surname': 'Surname',
                    'patronymic': 'Patronymic',
                    'birthDate': '1900-01-02',
                },
                'contact': {
                    'name': 'Name1',
                    'email': 'email@ya.ru4',
                    'phone': '+711_phone5',
                    'surname': 'Surname2',
                    'patronymic': 'Patronymic3',
                    'birthDate': '1901-01-02',
                },
            },
            'username': 'test-merchant-username',
        }

    @pytest.fixture
    def response_func(self, storage, merchant_uid, client, tvm, request_json):
        async def _inner(uid=merchant_uid):
            return await client.patch(f'/v1/merchant/{uid}', json=request_json)

        return _inner

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    @pytest.fixture
    async def response_data(self, response):
        return await response.json()

    @pytest.fixture
    async def updated_merchant(self, storage, response_data):
        merchant = await storage.merchant.get(response_data['data']['uid'])
        merchant.load_data()
        return merchant

    @pytest.fixture
    def merchant_data(self, merchant_data):
        merchant_data.organization.inn = '0123456789'  # to pass schema check
        return merchant_data

    @pytest.fixture
    async def merchant(self, merchant, merchant_data, storage):
        # Emulate preregistered merchant
        merchant.data = MerchantData(
            registered=False,
            organization=OrganizationData(inn=merchant_data.organization.inn),
        )
        merchant.name = None
        merchant = await storage.merchant.save(merchant)
        merchant.load_data()
        return merchant

    def test_not_found(self, response_data):
        assert response_data['code'] == 404

    class TestEmptyPatch(BaseTestMerchantRoles):
        ALLOWED_ROLES = (
            MerchantRole.ADMIN,
            MerchantRole.OWNER,
        )

        @pytest.fixture
        def request_json(self, request_json):
            return {}

        def test_empty_patch_status(self, merchant, response):
            assert response.status == 200

        def test_empty_patch_response(self, merchant, response_data):
            assert_that(response_data, has_entries({
                'data': has_entries({
                    'registered': False,
                    'addresses': None,
                    'username': None,
                    'bank': None,
                    'persons': None,
                    'organization': has_entries({
                        'inn': merchant.organization.inn
                    })
                })
            }))

        def test_empty_patch_no_changes(self, merchant, updated_merchant):
            assert merchant.data == updated_merchant.data \
                and merchant.name == updated_merchant.name

    class TestFullPatch(BaseTestMerchantRoles):
        ALLOWED_ROLES = (
            MerchantRole.ADMIN,
            MerchantRole.OWNER,
        )

        @pytest.fixture
        def merchant_data(self, merchant_data):
            merchant_data.registered = False
            merchant_data.organization.kpp = '1234567890'
            merchant_data.organization.schedule_text = 'test-merchant-schedule-text'
            return merchant_data

        def test_full_patch_status(self, merchant, response):
            assert response.status == 200

        def test_full_patch_response(self, merchant, response_data, request_json):
            # inn must not be changed since preregistration
            request_json['organization']['inn'] = merchant.organization.inn
            assert_that(response_data, has_entries({
                'data': has_entries({
                    'registered': False,
                    **request_json
                })
            }))

        def test_full_patch_stored(self, merchant, updated_merchant, merchant_name, merchant_data):
            assert updated_merchant.data == merchant_data and updated_merchant.name == merchant_name

    class TestChangeInn(BaseTestMerchantRoles):
        ALLOWED_ROLES = (
            MerchantRole.ADMIN,
            MerchantRole.OWNER,
        )

        @pytest.fixture
        def request_json(self, request_json, merchant, modify):
            inn_new = merchant.organization.inn
            if modify:
                inn_new += '0'
            request_json['organization']['inn'] = inn_new
            return request_json

        @pytest.mark.parametrize('modify', (False,))
        def test_change_inn_same_value_status(self, merchant, response):
            assert response.status == 200

        @pytest.mark.parametrize('modify', (False,))
        def test_change_inn_same_value_response(self, merchant, response_data, request_json):
            # inn must not be changed since preregistration
            request_json['organization']['inn'] = merchant.organization.inn
            assert_that(response_data, has_entries({
                'data': has_entries({
                    'registered': False,
                    **request_json
                })
            }))

        @pytest.mark.parametrize('modify', (True,))
        def test_change_inn_new_value_status(self, merchant, response):
            assert response.status == 400
