from datetime import datetime

import pytest

from mail.payments.payments.core.actions.merchant.update import UpdateMerchantAction
from mail.payments.payments.core.entities.enums import MerchantType
from mail.payments.payments.core.entities.functionality import PaymentsFunctionalityData

optional_address_types = [
    ('post', 'post'),
]
optional_address_fields = []
optional_bank_fields = []
optional_person_types = [
    ('contact', 'contact'),
    ('signer', 'signer'),
]
optional_person_fields = [
    ('patronymic', 'patronymic'),
]
optional_organization_fields = [
    ('kpp', 'kpp'),
    ('schedule_text', 'scheduleText'),
    ('site_url', 'siteUrl'),
]


@pytest.fixture
def request_json():
    return {
        'name': 'test-merchant-patch-name',
        'addresses': {
            'legal': {
                'city': 'test-merchant-patch-city',
                'country': 'RUS',
                'home': 'test-merchant-patch-home',
                'street': 'test-merchant-patch-street',
                'zip': '123456',
            },
        },
        'bank': {
            'account': 'test-merchant-patch-account',
            'bik': '123456789',
            'correspondentAccount': '12345678901234567890',
            'name': 'test-merchant-patch-name',
        },
        'organization': {
            'type': 'ooo',
            'name': 'test-merchant-patch-name',
            'englishName': 'english_name',
            'fullName': 'test-merchant-patch-full_name',
            'inn': '1234567890',
            'kpp': '0987654321',
            'ogrn': '1234567890123',
            'scheduleText': 'test-merchant-schedule-text',
            'siteUrl': 'test-merchant-patch-site_url',
            'description': 'test-merchant-patch-description',
        },
        'persons': {
            'ceo': {
                'name': 'test-merchant-patch-ceo-name',
                'email': 'test-merchant-patch-ceo-email@mail.ru',
                'phone': 'test-merchant-patch-ceo-phone',
                'surname': 'test-merchant-patch-ceo-surname',
                'patronymic': 'test-merchant-patch-ceo-patronymic',
                'birthDate': '2019-03-14',
            },
            'signer': {
                'name': 'test-merchant-patch-signer-name',
                'email': 'test-merchant-patch-signer-email@gmail.com',
                'phone': 'test-merchant-patch-signer-phone',
                'surname': 'test-merchant-patch-signer-surname',
                'patronymic': 'test-merchant-patch-signer-patronymic',
                'birthDate': '2019-03-13',
            },
        },
        'username': 'test-merchant-username',
        'functionality': {
            'type': 'payments',
        }
    }


class BaseMerchantPatch:
    @pytest.fixture
    def action(self, mock_action, merchant):
        return mock_action(UpdateMerchantAction, merchant)

    @pytest.fixture
    async def response(self, action, merchant, payments_client, request_json):
        return await payments_client.patch(f'/v1/merchant/{merchant.uid}', json=request_json)

    @pytest.fixture
    def request_json(self, request_json):
        request_json['addresses']['post'] = {
            'city': 'test-merchant-patch-city-1',
            'country': 'RUS',
            'home': 'test-merchant-patch-home-1',
            'street': 'test-merchant-patch-street-1',
            'zip': '234567',
        }
        request_json['persons']['contact'] = {
            'name': 'test-merchant-patch-contact-name',
            'email': 'test-merchant-patch-contact-email@gmail.com',
            'phone': 'test-merchant-patch-contact-phone',
            'surname': 'test-merchant-patch-contact-surname',
            'patronymic': 'test-merchant-patch-contact-patronymic',
            'birthDate': '2019-03-13',
        }
        return request_json

    @pytest.fixture
    def expected_call_args(self, request_json, merchant):
        res = {
            'uid': merchant.uid,
            'name': request_json['name'],
            'username': request_json['username'],
            'addresses': {k: None if v is None else v.copy() for k, v in request_json['addresses'].items()},
            'bank': {
                'account': 'test-merchant-patch-account',
                'bik': '123456789',
                'correspondent_account': '12345678901234567890',
                'name': 'test-merchant-patch-name',
            },
            'organization': {
                'type': MerchantType.OOO,
                'name': 'test-merchant-patch-name',
                'english_name': 'english_name',
                'full_name': 'test-merchant-patch-full_name',
                'inn': '1234567890',
                'kpp': '0987654321',
                'ogrn': '1234567890123',
                'schedule_text': 'test-merchant-schedule-text',
                'site_url': 'test-merchant-patch-site_url',
                'description': 'test-merchant-patch-description',
            },
            'persons': {
                person_type: None if person_data is None else {
                    'name': person_data['name'],
                    'email': person_data['email'],
                    'phone': person_data['phone'],
                    'surname': person_data['surname'],
                    'patronymic': person_data['patronymic'],
                    'birth_date': datetime.fromisoformat(person_data['birthDate']).date(),
                }
                for person_type, person_data in request_json['persons'].items()
            },
            'functionality': PaymentsFunctionalityData(),
        }
        return res


class TestMerchantPatchEmptySuccess(BaseMerchantPatch):
    @pytest.fixture
    def request_json(self):
        return {}

    @pytest.fixture
    def expected_call_args(self, merchant):
        return {'uid': merchant.uid}

    def test_empty_success__status(self, response):
        assert response.status == 200

    def test_empty_success__call_args(self, response, action, expected_call_args):
        assert action.call_args[1] == {'params': expected_call_args}


class MerchantPatchSetupNone:
    @pytest.fixture
    def setup_func(self):
        def _inner(data, key):
            data[key] = None

        return _inner


class MerchantPatchSetupPop:
    @pytest.fixture
    def setup_func(self):
        def _inner(data, key):
            data.pop(key)

        return _inner


class BaseTestMerchantPatchTopLevelSuccess(BaseMerchantPatch):
    ''' Try to exclude top-level fields or set them to None '''
    @pytest.fixture(autouse=True, params=(
        'name', 'username', 'addresses', 'bank', 'organization', 'persons'
    ))
    def setup(self, setup_func, request, request_json, expected_call_args):
        setup_func(request_json, request.param)
        setup_func(expected_call_args, request.param)

    def test_top_level_success__status(self, response):
        assert response.status == 200

    def test_top_level_success__call_args(self, response, action, expected_call_args):
        assert action.call_args[1] == {'params': expected_call_args}


class TestMerchantPatchMissingTopLevelSuccess(BaseTestMerchantPatchTopLevelSuccess, MerchantPatchSetupPop):
    pass


class TestMerchantPatchNoneTopLevelSuccess(BaseTestMerchantPatchTopLevelSuccess, MerchantPatchSetupNone):
    pass


class BaseTestMerchantPatchNestedSuccess(BaseMerchantPatch):
    ''' Try to exclude nested fields or set them to None '''
    @pytest.fixture(params=optional_address_types)
    def address_type(self, request):
        return request.param

    @pytest.fixture(params=optional_address_fields)
    def address_field(self, request):
        return request.param

    @pytest.fixture(params=optional_bank_fields)
    def bank_field(self, request):
        return request.param

    @pytest.fixture(params=optional_person_types)
    def person_type(self, request):
        return request.param

    @pytest.fixture(params=optional_person_fields)
    def person_field(self, request):
        return request.param

    @pytest.fixture(params=optional_organization_fields)
    def organization_field(self, request):
        return request.param

    class TestAddressType:
        @pytest.fixture(autouse=True)
        def setup(self, setup_func, address_type, request_json, expected_call_args):
            setup_func(request_json['addresses'], address_type[1])
            setup_func(expected_call_args['addresses'], address_type[0])

        def test_address_type__status(self, response):
            assert response.status == 200

        def test_address_type__call_args(self, response, action, expected_call_args):
            assert action.call_args[1] == {'params': expected_call_args}

    class TestAddressFields:
        @pytest.fixture(autouse=True)
        def setup(self, setup_func, address_field, request_json, expected_call_args):
            setup_func(request_json['addresses']['legal'], address_field[1])
            setup_func(expected_call_args['addresses']['legal'], address_field[0])

        def test_address_fields__status(self, response):
            assert response.status == 200

        def test_address_fields__call_args(self, response, action, expected_call_args):
            assert action.call_args[1] == {'params': expected_call_args}

    class TestBankFields:
        @pytest.fixture(autouse=True)
        def setup(self, setup_func, bank_field, request_json, expected_call_args):
            setup_func(request_json['bank'], bank_field[1])
            setup_func(expected_call_args['bank'], bank_field[0])

        def test_bank_fields__status(self, response):
            assert response.status == 200

        def test_bank_fields__call_args(self, response, action, expected_call_args):
            assert action.call_args[1] == {'params': expected_call_args}

    class TestPersonTypes:
        @pytest.fixture(autouse=True)
        def setup(self, setup_func, person_type, request_json, expected_call_args):
            setup_func(request_json['persons'], person_type[1])
            setup_func(expected_call_args['persons'], person_type[0])

        def test_person_types__status(self, response):
            assert response.status == 200

        def test_person_types__call_args(self, response, action, expected_call_args):
            assert action.call_args[1] == {'params': expected_call_args}

    class TestPersonFields:
        @pytest.fixture(autouse=True)
        def setup(self, setup_func, person_field, request_json, expected_call_args):
            setup_func(request_json['persons']['ceo'], person_field[1])
            setup_func(expected_call_args['persons']['ceo'], person_field[0])

        def test_person_fields__status(self, response):
            assert response.status == 200

        def test_person_fields__call_args(self, response, action, expected_call_args):
            assert action.call_args[1] == {'params': expected_call_args}

    class TestOrganizationFields:
        @pytest.fixture(autouse=True)
        def setup(self, setup_func, organization_field, request_json, expected_call_args):
            setup_func(request_json['organization'], organization_field[1])
            setup_func(expected_call_args['organization'], organization_field[0])

        def test_organization_fields__status(self, response):
            assert response.status == 200

        def test_organization_fields__call_args(self, response, action, expected_call_args):
            assert action.call_args[1] == {'params': expected_call_args}


class TestMerchantPatchNestedSuccessNone(BaseTestMerchantPatchNestedSuccess, MerchantPatchSetupNone):
    pass


class TestMerchantPatchNestedSuccessPop(BaseTestMerchantPatchNestedSuccess, MerchantPatchSetupPop):
    @pytest.fixture(params=optional_address_types + [('legal', 'legal')])
    def address_type(self, request):
        return request.param

    @pytest.fixture(params=optional_address_fields + [
        ('city', 'city'),
        ('country', 'country'),
        ('home', 'home'),
        ('street', 'street'),
        ('zip', 'zip'),
    ])
    def address_field(self, request):
        return request.param

    @pytest.fixture(params=optional_bank_fields + [
        ('account', 'account'),
        ('bik', 'bik'),
        ('correspondent_account', 'correspondentAccount'),
        ('name', 'name'),
    ])
    def bank_field(self, request):
        return request.param

    @pytest.fixture(params=optional_person_types + [('ceo', 'ceo')])
    def person_type(self, request):
        return request.param

    @pytest.fixture(params=optional_person_fields + [
        ('name', 'name'),
        ('email', 'email'),
        ('phone', 'phone'),
        ('surname', 'surname'),
        ('birth_date', 'birthDate'),
    ])
    def person_field(self, request):
        return request.param

    @pytest.fixture(params=optional_organization_fields + [
        ('type', 'type'),
        ('name', 'name'),
        ('english_name', 'englishName'),
        ('full_name', 'fullName'),
        ('ogrn', 'ogrn'),
        ('description', 'description'),
        ('inn', 'inn'),
    ])
    def organization_field(self, request):
        return request.param


class TestMerchantPatchFailNoneAddressType(BaseMerchantPatch, MerchantPatchSetupNone):
    @pytest.fixture(autouse=True, params=['legal'])
    def setup(self, setup_func, request, request_json):
        setup_func(request_json['addresses'], request.param)

    def test_status(self, response):
        assert response.status == 400


class TestMerchantPatchFailNoneAddressFields(BaseMerchantPatch, MerchantPatchSetupNone):
    @pytest.fixture(autouse=True, params=[
        'city', 'country', 'home', 'street', 'zip'
    ])
    def setup(self, setup_func, request, request_json):
        setup_func(request_json['addresses']['legal'], request.param)

    def test_status(self, response):
        assert response.status == 400


class TestMerchantPatchFailNonePersonsType(BaseMerchantPatch, MerchantPatchSetupNone):
    @pytest.fixture(autouse=True, params=['ceo'])
    def setup(self, setup_func, request, request_json):
        setup_func(request_json['persons'], request.param)

    def test_status(self, response):
        assert response.status == 400


class TestMerchantPatchFailNoneBankFields(BaseMerchantPatch, MerchantPatchSetupNone):
    @pytest.fixture(autouse=True, params=['account', 'bik'])
    def setup(self, setup_func, request, request_json):
        setup_func(request_json['bank'], request.param)

    def test_status(self, response):
        assert response.status == 400
