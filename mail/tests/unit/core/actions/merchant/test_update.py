from copy import deepcopy
from dataclasses import asdict
from datetime import date, timedelta

import pytest

from hamcrest import assert_that, has_properties, has_property, match_equality

from mail.payments.payments.core.actions.merchant.functionality import (
    PutPaymentsMerchantFunctionalityAction, PutYandexPayMerchantFunctionalityAction
)
from mail.payments.payments.core.actions.merchant.update import UpdateMerchantAction
from mail.payments.payments.core.entities.enums import CallbackMessageType, PersonType, YandexPayPaymentGatewayType
from mail.payments.payments.core.entities.functionality import (
    PaymentsFunctionalityData, YandexPayPaymentGatewayFunctionalityData
)
from mail.payments.payments.core.entities.merchant import MerchantData
from mail.payments.payments.core.exceptions import CoreFieldError, InnModificationError, MerchantIsAlreadyRegistered
from mail.payments.payments.tests.base import BaseTestRequiresNoModeration

address_fields = ['city', 'country', 'home', 'street', 'zip']
bank_fields = ['account', 'bik', 'correspondent_account', 'name']
person_fields = ['name', 'surname', 'patronymic', 'email', 'birth_date', 'phone']
organization_fields = ['name', 'english_name', 'full_name', 'kpp', 'ogrn', 'site_url', 'description']


@pytest.fixture
def merchant_data_params(merchant_data):
    if merchant_data.addresses is None:
        addresses = None
    else:
        addresses = {}
        for address in merchant_data.addresses:
            address_dict = asdict(address)
            addresses[address_dict.pop('type')] = address_dict

    if merchant_data.persons is None:
        persons = None
    else:
        persons = {}
        for person in merchant_data.persons:
            person_dict = asdict(person)
            persons[person_dict.pop('type').value] = person_dict

    return {
        'addresses': addresses,
        'organization': None if merchant_data.organization is None else asdict(merchant_data.organization),
        'bank': None if merchant_data.bank is None else asdict(merchant_data.bank),
        'persons': persons,
        'username': merchant_data.username,
    }


@pytest.fixture
def merchant_data_expected(merchant_data):
    return deepcopy(merchant_data)


class BaseTest(BaseTestRequiresNoModeration):
    @pytest.fixture
    def send_notifications(self):
        return False

    @pytest.fixture
    def returned_func(self, action_params, send_notifications):
        async def _inner():
            return await UpdateMerchantAction(action_params, send_notifications=send_notifications).run()

        return _inner

    @pytest.fixture
    def merchant_name(self):
        return 'some-merchant-name'

    @pytest.fixture
    def merchant_data(self, merchant_data):
        merchant_data.registered = False
        return merchant_data

    @pytest.fixture
    def action_params(self, merchant_uid, merchant_name, merchant_data_params):
        return {
            'uid': merchant_uid,
            'name': merchant_name,
            **merchant_data_params,
        }

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.fixture
    async def updated(self, storage, returned):
        merchant = await storage.merchant.get(returned.uid)
        return merchant


class BaseTestSuccess(BaseTest):
    @pytest.mark.asyncio
    async def test_returned(self, merchant, merchant_name, merchant_data_expected, returned):
        assert_that(
            returned,
            has_properties({
                'registered': False,
                'uid': merchant.uid,
                'name': merchant_name,
                'data': merchant_data_expected,
            })
        )

    @pytest.mark.asyncio
    async def test_updated(self, merchant, updated, returned):
        assert updated == returned


class TestFillEmptyMerchantData(BaseTestSuccess):
    @pytest.fixture(autouse=True)
    async def clear_merchant_data(self, merchant, storage):
        merchant.data = MerchantData(registered=False)
        await storage.merchant.save(merchant)


class TestClearTopLevelFields(BaseTestSuccess):
    @pytest.fixture(params=['addresses', 'bank', 'persons', 'username'])
    def merchant_data_params(self, request, merchant_data_expected):
        setattr(merchant_data_expected, request.param, None)
        return {request.param: None}


class TestUpdateUsername(BaseTestSuccess):
    @pytest.fixture(params=['new-username', None])
    def merchant_data_params(self, request, merchant_data_expected):
        setattr(merchant_data_expected, 'username', request.param)
        return {'username': request.param}


class TestUpdateName(BaseTestSuccess):
    @pytest.fixture
    def merchant_data_params(self, merchant_data_expected):
        return {}

    @pytest.fixture(params=['new-name', None])
    def merchant_name(self, request):
        return request.param


class TestNoChangeNoName(BaseTestSuccess):
    @pytest.fixture
    def merchant_name(self, merchant_entity):
        return merchant_entity.name

    @pytest.fixture
    def action_params(self, merchant_uid, merchant_data_params):
        return {
            'uid': merchant_uid,
            **merchant_data_params,
        }


class TestNoChangeEmptyMerchantData(BaseTestSuccess):
    @pytest.fixture
    def merchant_data_params(self):
        return {}


class TestNoChangeNoTopLevelField(BaseTestSuccess):
    @pytest.fixture(params=['addresses', 'bank', 'persons', 'organization', 'username'])
    def merchant_data_params(self, request, merchant_data_params):
        merchant_data_params.pop(request.param)
        return merchant_data_params


class TestNoChangeEmptyTopLevelField(BaseTestSuccess):
    @pytest.fixture(params=['addresses', 'bank', 'persons', 'organization'])  # only objects
    def merchant_data_params(self, request, merchant_data_params):
        merchant_data_params[request.param] = {}
        return merchant_data_params


class TestNoChangeEmptyAddressType(BaseTestSuccess):
    @pytest.fixture
    def merchant_data_params(self, merchant_data_params):
        merchant_data_params['addresses']['legal'] = {}
        return merchant_data_params


class TestNoChangeEmptyPersonType(BaseTestSuccess):
    @pytest.fixture
    def merchant_data_params(self, merchant_data_params):
        merchant_data_params['persons']['ceo'] = {}
        return merchant_data_params


class BaseTestUpdateField(BaseTestSuccess):
    @pytest.fixture
    def new_value(self):
        return 'new-value'


class TestUpdateAddressField(BaseTestUpdateField):
    @pytest.fixture(params=address_fields)
    def field(self, request):
        return request.param

    @pytest.fixture
    def merchant_data_params(self, field, new_value, merchant_data_params):
        return {
            'addresses': {
                'legal': {
                    field: new_value
                }
            }
        }

    @pytest.fixture
    def merchant_data_expected(self, field, new_value, merchant_data_expected):
        setattr(merchant_data_expected.addresses[0], field, new_value)
        return merchant_data_expected


class TestUpdatePersonField(BaseTestUpdateField):
    @pytest.fixture(params=person_fields)
    def field(self, request):
        return request.param

    @pytest.fixture
    def new_value(self, field, merchant_data):
        old_value = getattr(merchant_data.persons[0], field)
        if isinstance(old_value, date):
            return old_value + timedelta(days=1)
        return old_value + '-changed'

    @pytest.fixture
    def merchant_data_params(self, field, new_value, merchant_data_params):
        return {
            'persons': {
                'ceo': {
                    field: new_value
                }
            }
        }

    @pytest.fixture
    def merchant_data_expected(self, field, new_value, merchant_data_expected):
        setattr(merchant_data_expected.persons[0], field, new_value)
        return merchant_data_expected


class TestUpdateBankField(BaseTestUpdateField):
    @pytest.fixture(params=bank_fields)
    def field(self, request):
        return request.param

    @pytest.fixture
    def merchant_data_params(self, field, new_value, merchant_data_params):
        return {
            'bank': {
                field: new_value
            }
        }

    @pytest.fixture
    def merchant_data_expected(self, field, new_value, merchant_data_expected):
        setattr(merchant_data_expected.bank, field, new_value)
        return merchant_data_expected


class TestUpdateOrganizationField(BaseTestUpdateField):
    @pytest.fixture(params=organization_fields)
    def field(self, request):
        return request.param

    @pytest.fixture
    def merchant_data_params(self, field, new_value, merchant_data_params):
        return {
            'organization': {
                field: new_value
            }
        }

    @pytest.fixture
    def merchant_data_expected(self, field, new_value, merchant_data_expected):
        setattr(merchant_data_expected.organization, field, new_value)
        return merchant_data_expected


class TestClearAddressType(BaseTestUpdateField):
    @pytest.fixture
    def merchant_data_params(self):
        return {
            'addresses': {
                'legal': None
            }
        }

    @pytest.fixture
    def merchant_data_expected(self, merchant_data_expected):
        del merchant_data_expected.addresses[0]
        return merchant_data_expected


class TestClearPersonType(BaseTestSuccess):
    @pytest.fixture(params=[PersonType.CEO, PersonType.CONTACT])
    def person_type(self, request):
        return request.param

    @pytest.fixture
    def merchant_data_params(self, person_type):
        return {
            'persons': {
                person_type.value: None
            }
        }

    @pytest.fixture
    def merchant_data_expected(self, person_type, merchant_data_expected):
        person_to_remove = filter(lambda person: person.type == person_type, merchant_data_expected.persons)
        merchant_data_expected.persons.remove(next(person_to_remove))
        return merchant_data_expected


class TestClearAddressField(BaseTestSuccess):
    @pytest.fixture(params=address_fields)
    def field(self, request):
        return request.param

    @pytest.fixture
    def merchant_data_params(self, field):
        return {
            'addresses': {
                'legal': {
                    field: None
                }
            }
        }

    @pytest.fixture
    def merchant_data_expected(self, field, merchant_data_expected):
        setattr(merchant_data_expected.addresses[0], field, None)
        return merchant_data_expected


class TestClearPersonField(BaseTestSuccess):
    @pytest.fixture(params=person_fields)
    def field(self, request):
        return request.param

    @pytest.fixture
    def merchant_data_params(self, field):
        return {
            'persons': {
                'ceo': {
                    field: None
                }
            }
        }

    @pytest.fixture
    def merchant_data_expected(self, field, merchant_data_expected):
        setattr(merchant_data_expected.persons[0], field, None)
        return merchant_data_expected


class TestClearBankField(BaseTestSuccess):
    @pytest.fixture(params=bank_fields)
    def field(self, request):
        return request.param

    @pytest.fixture
    def merchant_data_params(self, field):
        return {
            'bank': {
                field: None
            }
        }

    @pytest.fixture
    def merchant_data_expected(self, field, merchant_data_expected):
        setattr(merchant_data_expected.bank, field, None)
        return merchant_data_expected


class TestClearOrganizationField(BaseTestSuccess):
    @pytest.fixture(params=organization_fields)
    def field(self, request):
        return request.param

    @pytest.fixture
    def merchant_data_params(self, field):
        return {
            'organization': {
                field: None
            }
        }

    @pytest.fixture
    def merchant_data_expected(self, field, merchant_data_expected):
        setattr(merchant_data_expected.organization, field, None)
        return merchant_data_expected


class TestFailRegistered(BaseTest):
    @pytest.fixture(autouse=True)
    async def set_registered(self, merchant, storage, merchant_data, merchant_name):
        merchant = await storage.merchant.get(uid=merchant.uid)
        merchant.data.registered = True
        merchant = await storage.merchant.save(merchant)

    @pytest.mark.asyncio
    async def test_fail(self, returned_func, merchant_data_params):
        with pytest.raises(MerchantIsAlreadyRegistered):
            await returned_func()


class TestFailNoRequiredFieldsToInstantiateDataclass(BaseTest):
    ''' Test failing if can't create new data instance '''

    @pytest.fixture(autouse=True)
    async def clear_merchant_data(self, merchant, storage):
        merchant.data = MerchantData(registered=False)
        await storage.merchant.save(merchant)

    @pytest.mark.parametrize('merchant_data_params', (
        {
            'addresses': {
                'legal': {
                    'country': 'RUS'
                }
            }
        },
        {
            'persons': {
                'ceo': {
                    'phone': '012345'
                }
            }
        },
        {
            'bank': {
                'name': 'bank-name'
            }
        }

    ))
    @pytest.mark.asyncio
    async def test_fail(self, merchant, returned_func, merchant_data_params):
        with pytest.raises(CoreFieldError):
            await returned_func()


class TestFailBadFieldsToUpdate(BaseTest):
    @pytest.mark.parametrize('merchant_data_params', (
        {
            'addresses': {
                'legal': {
                    'abc': 'def'
                }
            }
        },
        {
            'persons': {
                'ceo': {
                    '123': '456'
                }
            }
        },
        {
            'bank': {
                'some-key': 'some-value'
            }
        },
    ))
    @pytest.mark.asyncio
    async def test_fail(self, merchant, returned_func, merchant_data_params):
        with pytest.raises(CoreFieldError):
            await returned_func()


class TestFailInnChange(BaseTest):
    @pytest.mark.parametrize('merchant_data_params', (
        {
            'organization': {
                'inn': 'some-new-value'
            }
        },
        {
            'organization': {
                'inn': None
            }
        },
        {
            'organization': None
        }
    ))
    @pytest.mark.asyncio
    async def test_fail_inn_change(self, merchant, returned_func):
        with pytest.raises(InnModificationError):
            await returned_func()


class TestUpdatesFunctionality(BaseTest):
    @pytest.mark.parametrize('merchant_data_params, action', (
        (
            {
                'functionality': PaymentsFunctionalityData(),
            },
            PutPaymentsMerchantFunctionalityAction,
        ),
        (
            {
                'functionality': YandexPayPaymentGatewayFunctionalityData(
                    payment_gateway_type=YandexPayPaymentGatewayType.PSP,
                    gateway_id='gw-id'
                )
            },
            PutYandexPayMerchantFunctionalityAction,
        ),
    ))
    @pytest.mark.asyncio
    async def test_updates_functionality(self, mock_action, merchant, returned_func, action, merchant_data_params):
        mock = mock_action(action)

        await returned_func()

        mock.assert_called_once_with(
            merchant=match_equality(has_property('uid', merchant.uid)),
            data=merchant_data_params['functionality'],
        )


class TestSendsNotification(BaseTest):
    @pytest.fixture
    def send_notifications(self):
        return True

    @pytest.mark.asyncio
    async def test_sends_notification(self, storage, service_merchant, service_client, returned):
        task = await storage.task.find().__anext__()
        expected_params = dict(tvm_id=service_client.tvm_id,
                               callback_message_type=CallbackMessageType.MERCHANT_REQUISITES_UPDATED.value,
                               callback_url=service_client.api_callback_url,
                               message={'service_merchant_id': service_merchant.service_merchant_id})
        assert task.params == expected_params
