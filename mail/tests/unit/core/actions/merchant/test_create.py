from copy import copy

import pytest

from hamcrest import assert_that, contains, has_entries, has_properties, has_property, match_equality

from mail.payments.payments.core.actions.merchant.create import (
    CreateMerchantAction, InitClientAction, InitSubmerchantAction
)
from mail.payments.payments.core.actions.merchant.create_entity import CreateMerchantEntityAction
from mail.payments.payments.core.actions.merchant.functionality import (
    PutPaymentsMerchantFunctionalityAction, PutYandexPayMerchantFunctionalityAction
)
from mail.payments.payments.core.actions.merchant.get import GetMerchantAction
from mail.payments.payments.core.entities.enums import AcquirerType, OrderSource
from mail.payments.payments.core.entities.functionality import (
    PaymentsFunctionalityData, YandexPayMerchantFunctionalityData
)
from mail.payments.payments.core.entities.userinfo import UserInfo
from mail.payments.payments.core.exceptions import InnModificationError
from mail.payments.payments.tests.base import BaseTestRequiresNoModeration, parametrize_acquirer


@pytest.fixture(autouse=True)
def blackbox_userinfo_mock(blackbox_client_mocker, merchant_uid, rands):
    user_info = UserInfo(
        uid=merchant_uid,
        default_email=rands(),
    )
    with blackbox_client_mocker('userinfo', user_info) as mock:
        yield mock


class TestMapMerchantData:
    @pytest.fixture
    def kwargs(self):
        return {
            'addresses': [
                {
                    'type': 'address-type',
                    'city': 'address-city',
                    'country': 'address-country',
                    'home': 'address-home',
                    'street': 'address-street',
                    'zip': 'address-zip',
                },
            ],
            'bank': {
                'account': 'bank-account',
                'bik': 'bank-bik',
                'correspondent_account': 'bank-correspondent_account',
                'name': 'bank-name',
            },
            'organization': {
                'type': 'organization-type',
                'name': 'organization-name',
                'english_name': 'organization-english_name',
                'full_name': 'organization-full_name',
                'inn': 'organization-inn',
                'kpp': 'organization-kpp',
                'ogrn': '1234567890123',
                'schedule_text': 'organization-schedule_text',
                'site_url': 'organization-site_url',
            },
            'persons': [
                {
                    'type': 'person-type',
                    'name': 'person-name',
                    'email': 'person-email',
                    'phone': 'person-phone',
                    'surname': 'person-surname',
                    'patronymic': 'person-patronymic',
                    'birth_date': 'person-birth_date',
                },
            ],
            'username': '-username',
            'fast_moderation': True,
        }

    def test_returned(self, kwargs):
        assert_that(
            CreateMerchantAction._map_merchant_data(**kwargs),
            has_properties({
                'addresses': contains(*[has_properties(address) for address in kwargs['addresses']]),
                'bank': has_properties(kwargs['bank']),
                'organization': has_properties(kwargs['organization']),
                'persons': contains(*[has_properties(person) for person in kwargs['persons']]),
                'username': kwargs['username'],
                'fast_moderation': kwargs['fast_moderation'],
            })
        )

    def test_without_site_url(self, kwargs):
        kwargs['organization'].pop('site_url')
        assert_that(
            CreateMerchantAction._map_merchant_data(**kwargs),
            has_properties({
                'addresses': contains(*[has_properties(address) for address in kwargs['addresses']]),
                'bank': has_properties(kwargs['bank']),
                'organization': has_properties({
                    **kwargs['organization'],
                    'site_url': None,
                }),
                'persons': contains(*[has_properties(person) for person in kwargs['persons']]),
            })
        )

    def test_without_patronymic(self, kwargs):
        kwargs['persons'][0].pop('patronymic')
        mapped = CreateMerchantAction._map_merchant_data(**kwargs)
        kwargs['persons'][0]['patronymic'] = None
        assert_that(
            mapped,
            has_properties({
                'persons': contains(*[has_properties(person) for person in kwargs['persons']]),
            })
        )

    def test_without_legal_address_home(self, kwargs):
        kwargs['addresses'][0].pop('home')
        actual = CreateMerchantAction._map_merchant_data(**kwargs)
        kwargs['addresses'][0]['home'] = None
        assert_that(
            actual,
            has_properties({
                'addresses': contains(has_properties(kwargs['addresses'][0])),
            })
        )


class BaseTestCreateMerchant:
    @pytest.fixture
    def functionality(self):
        return PaymentsFunctionalityData()

    @pytest.fixture
    def functionality_action(self, mock_action):
        return PutYandexPayMerchantFunctionalityAction

    @pytest.fixture
    def mock_put_functionality(self, mock_action, functionality_action):
        return mock_action(functionality_action)

    @pytest.fixture(autouse=True)
    def map_mock(self, mocker, merchant_data):
        return mocker.patch.object(
            CreateMerchantAction,
            '_map_merchant_data',
            mocker.Mock(return_value=merchant_data),
        )

    @pytest.fixture(autouse=True)
    def init_client_mock(self, mock_action):
        return mock_action(InitClientAction)

    @pytest.fixture(autouse=True)
    def init_submerchant_mock(self, mock_action):
        return mock_action(InitSubmerchantAction)

    @pytest.fixture
    def merchant_name(self):
        return 'test-create-merchant-name'

    @pytest.fixture
    def merchant_data_params(self):
        return {
            'addresses': 'test-merchant-data-params-address',
            'bank': 'test-merchant-data-params-bank',
            'organization': 'test-merchant-data-params-organization',
            'persons': 'test-merchant-data-params-persons',
            'username': 'test-merchant-data-params-username',
            'fast_moderation': False,
        }

    @pytest.fixture
    def params(self, merchant_uid, merchant_name, merchant_data_params, functionality):
        return {
            'uid': merchant_uid,
            'name': merchant_name,
            **merchant_data_params,
            'functionality': functionality,
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await CreateMerchantAction(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.fixture
    async def created_merchant(self, storage, returned):
        return await GetMerchantAction(returned.uid, skip_moderation=True).run()

    def test_returned(self, merchant_uid, merchant_name, merchant_data, returned):
        assert_that(
            returned,
            has_properties({
                'registered': True,
                'uid': merchant_uid,
                'name': merchant_name,
                'data': merchant_data,
            })
        )

    def test_created(self, returned, created_merchant):
        assert created_merchant == returned

    def test_map_call(self, map_mock, merchant_data_params, returned):
        map_mock.assert_called_once_with(**merchant_data_params)

    def test_init_client_call(self,
                              init_client_mock,
                              merchant_uid,
                              merchant_name,
                              merchant_data,
                              returned,
                              ):
        assert_that(
            init_client_mock.call_args[1],
            has_entries({
                'merchant': has_properties({
                    'uid': merchant_uid,
                    'name': merchant_name,
                    'data': merchant_data,
                }),
                'save': False,
            })
        )

    def test_init_submerchant_call(self,
                                   init_submerchant_mock,
                                   merchant_uid,
                                   merchant_name,
                                   merchant_data,
                                   returned):
        assert_that(
            init_submerchant_mock.call_args[1],
            has_entries({
                'merchant': has_properties({
                    'uid': merchant_uid,
                    'name': merchant_name,
                    'data': merchant_data,
                }),
                'save': False,
            })
        )

    @pytest.mark.parametrize('functionality, functionality_action', (
        (
            YandexPayMerchantFunctionalityData(),
            PutYandexPayMerchantFunctionalityAction
        ),
        (PaymentsFunctionalityData(), PutPaymentsMerchantFunctionalityAction),
    ))
    def test_ensures_functionality(self, mock_put_functionality, returned, functionality):
        mock_put_functionality.assert_called_once_with(
            merchant=match_equality(has_property('uid', returned.uid)),
            data=functionality,
        )


class TestCreateMerchantNew(BaseTestCreateMerchant):
    @pytest.fixture
    def spy_create_entity(self, mocker):
        return mocker.spy(CreateMerchantEntityAction, 'run')

    def test_create_entity_called(self, spy_create_entity, created_merchant):
        spy_create_entity.assert_called_once()

    @parametrize_acquirer
    @pytest.mark.asyncio
    async def test_default_allowed_order_sources(self, acquirer, payments_settings, returned_func):
        payments_settings.DEFAULT_ACQUIRER = acquirer
        returned = await returned_func()

        assert returned.options.allowed_order_sources == {
            AcquirerType.TINKOFF: None,
            AcquirerType.KASSA: [OrderSource.SDK_API, OrderSource.SERVICE]
        }[acquirer]


@pytest.mark.usefixtures('merchant')
class TestCreateMerchantExisting(BaseTestCreateMerchant, BaseTestRequiresNoModeration):
    @pytest.mark.asyncio
    async def test_create_merchant_existing__change_inn(self, rands, storage, merchant, returned_func):
        merchant.organization.inn = rands()
        await storage.merchant.save(merchant)
        with pytest.raises(InnModificationError):
            await returned_func()

    @pytest.mark.asyncio
    async def test_create_merchant_existing__success(self, storage, merchant, returned_func):
        await storage.merchant.save(merchant)
        actual = await returned_func()
        assert merchant.organization.inn == actual.organization.inn


@pytest.mark.usefixtures('merchant')
class TestCreateMerchantFillEntrepreneurOrganizationNames(BaseTestCreateMerchant):
    @pytest.fixture(params=(False, True))
    def initially_empty(self, request):
        return request.param

    @pytest.fixture
    def merchant_data(self, merchant_data, initially_empty):
        merchant_data.organization.inn = '01234567891'
        if initially_empty:
            merchant_data.organization.full_name = None
            merchant_data.organization.name = None
            merchant_data.organization.english_name = None
        return merchant_data

    @pytest.fixture
    def expected_organization_data(self, merchant_data, initially_empty):
        data = copy(merchant_data.organization)
        if initially_empty:
            data.full_name = 'Индивидуальный предприниматель Surname Name Patronymic'
            data.name = 'ИП Surname N.P.'
            data.english_name = 'IP Surname N.P'
        return data

    def test_returned(self, merchant_uid, merchant_name, merchant_data, expected_organization_data, returned):
        merchant_data.organization = expected_organization_data
        assert_that(
            returned,
            has_properties({
                'registered': True,
                'uid': merchant_uid,
                'name': merchant_name,
                'data': merchant_data,
            })
        )
