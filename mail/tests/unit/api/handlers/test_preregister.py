import inspect
import uuid

import pytest
import pytz

from hamcrest import assert_that, equal_to, has_entries, has_entry, instance_of, match_equality, not_none

from mail.payments.payments.core.actions.merchant.preregister import (
    GetMerchantPreregistrationAction, PreregisterMerchantAction
)
from mail.payments.payments.core.entities.enums import YandexPayPaymentGatewayType
from mail.payments.payments.core.entities.functionality import (
    Functionalities, PaymentsFunctionalityData, YandexPayMerchantFunctionalityData,
    YandexPayPaymentGatewayFunctionalityData
)
from mail.payments.payments.core.entities.merchant import Merchant, MerchantData, OrganizationData


def request_json_action_kwargs_cases():
    """
    Тестируем передачу аргументов из запроса в экшен, поэтому можем писать любые id сущностей.
    """

    def no_changes(json):
        return json

    def change_inn(json):
        inn = '949494949'
        assert json['inn'] != inn
        json['inn'] = inn
        return json

    def change_services(json):
        services = [45]
        assert json['services'] != services
        json['services'] = services
        return json

    def change_categories(json):
        categories = [56]
        assert json['categories'] != categories
        json['categories'] = categories
        return json

    def change_require_online(json):
        json['require_online'] = not json['require_online']
        return json

    params = [
        pytest.param(f, id=f.__name__)
        for f in locals().values()
        if inspect.isfunction(f)
    ]

    return params


class TestPostRequest:
    @pytest.fixture
    async def preregistered_merchant(self, create_preregistered_merchant, merchant_preregistration):
        merchant_entity = Merchant(
            token=str(uuid.uuid4()),
            uid=merchant_preregistration.uid,
            data=MerchantData(
                organization=OrganizationData(
                    inn=merchant_preregistration.data.preregister_data.inn,
                ),
            ),
            functionalities=Functionalities(
                payments=PaymentsFunctionalityData(),
                yandex_pay=YandexPayPaymentGatewayFunctionalityData(
                    payment_gateway_type=YandexPayPaymentGatewayType.PSP,
                    gateway_id='gw-id',
                    partner_id=uuid.UUID('0f29662b-e928-47b9-a2df-7471a5cc6f6f')
                ),
            )
        )
        merchant_entity.preregistration = merchant_preregistration
        merchant_entity.load_data()
        merchant_entity.load_parent()
        return merchant_entity

    @pytest.fixture(autouse=True)
    def action(self, mock_action, preregistered_merchant):
        return mock_action(PreregisterMerchantAction, preregistered_merchant)

    @pytest.fixture
    def contact_data(self):
        return {
            'name': 'Foo',
            'email': 'contact@yandex.test',
            'phone': '+0 123 456 78 90',
            'surname': 'Bar',
        }

    @pytest.fixture
    def request_json(self, preregister_data, request, contact_data):
        json = dict(
            require_online=preregister_data.require_online,
            services=preregister_data.services,
            inn=preregister_data.inn,
            categories=preregister_data.categories,
            functionality={'type': 'payments'},
            contact=contact_data,
        )
        mutator = getattr(request, 'param', None)
        if callable(mutator):
            json = mutator(json)
        return json

    @pytest.fixture
    async def make_preregister_request(self, payments_client):
        async def _inner(uid, json):
            return await payments_client.post(f'/v1/merchant/{uid}/preregister', json=json)

        return _inner

    @pytest.mark.asyncio
    async def test_response_status(self, make_preregister_request, preregister_data, preregistered_merchant,
                                   request_json):
        response = await make_preregister_request(uid=preregistered_merchant.uid, json=request_json)
        assert response.status == 200

    @pytest.mark.asyncio
    async def test_inn_is_optional(self, make_preregister_request, preregister_data, preregistered_merchant,
                                   request_json):
        request_json['inn'] = None

        response = await make_preregister_request(uid=preregistered_merchant.uid, json=request_json)

        assert response.status == 200

    @pytest.mark.asyncio
    async def test_response_format(
        self, make_preregister_request, merchant_preregistration, preregistered_merchant, request_json,
    ):
        response = await make_preregister_request(uid=preregistered_merchant.uid, json=request_json)
        response_json = await response.json()

        assert_that(response_json, has_entries({
            'data': has_entries({
                'registration_route': preregistered_merchant.registration_route.value,
                'username': None,
                'registered': preregistered_merchant.registered,
                'created': preregistered_merchant.created.astimezone(pytz.UTC).isoformat(),
                'updated': preregistered_merchant.updated.astimezone(pytz.UTC).isoformat(),
                'persons': None,
                'bank': None,
                'options': has_entries({
                    'allow_create_orders_via_ui': instance_of(bool),
                    'allow_create_service_merchants': instance_of(bool),
                    'hide_commission': instance_of(bool),
                }),
                'revision': preregistered_merchant.revision,
                'organization': {
                    'scheduleText': None,
                    'englishName': None,
                    'siteUrl': None,
                    'name': None,
                    'inn': preregistered_merchant.data.organization.inn,
                    'ogrn': None,
                    'kpp': None,
                    'fullName': None,
                    'type': None,
                    'description': None,
                },
                'acquirer': preregistered_merchant.acquirer.value,
                'name': preregistered_merchant.name,
                'addresses': None,
                'parent_uid': None,
                'merchant_id': preregistered_merchant.merchant_id,
                'uid': preregistered_merchant.uid,
                'status': preregistered_merchant.status.value,
                'preregister_data': dict(
                    inn=merchant_preregistration.preregister_data.inn,
                    services=merchant_preregistration.preregister_data.services,
                    categories=merchant_preregistration.preregister_data.categories,
                    require_online=merchant_preregistration.preregister_data.require_online,
                ),
                'functionalities': has_entries({
                    'payments': {'type': 'payments'},
                    'yandex_pay': not_none(),
                }),
            })
        }))

    @pytest.mark.asyncio
    async def test_response_format_for_yandex_pay_functionality_merchant_partner(
        self, make_preregister_request, preregistered_merchant, request_json,
    ):
        preregistered_merchant.functionalities = Functionalities(
            payments=PaymentsFunctionalityData(),
            yandex_pay=YandexPayMerchantFunctionalityData(
                merchant_gateway_id='gegw-id',
                merchant_desired_gateway='Парни подключите сбер плиз',
                partner_id=uuid.UUID('0f29662b-e928-47b9-a2df-7471a5cc6f6f'),
            ),
        )
        response = await make_preregister_request(uid=preregistered_merchant.uid, json=request_json)
        response_json = await response.json()

        assert_that(
            response_json,
            equal_to({
                'code': 200,
                'status': 'success',
                'data': match_equality(has_entries({
                    'functionalities': {
                        'payments': {'type': 'payments'},
                        'yandex_pay': {
                            'type': 'yandex_pay',
                            'partner_type': 'merchant',
                            'merchant_desired_gateway': 'Парни подключите сбер плиз',
                            'merchant_gateway_id': 'gegw-id',
                            'partner_id': '0f29662b-e928-47b9-a2df-7471a5cc6f6f',
                        },
                    },
                }))
            })
        )

    @pytest.mark.asyncio
    async def test_response_format_for_yandex_pay_payment_gateway_partner(
        self, make_preregister_request, preregistered_merchant, request_json,
    ):
        response = await make_preregister_request(uid=preregistered_merchant.uid, json=request_json)
        response_json = await response.json()

        assert_that(
            response_json,
            equal_to({
                'code': 200,
                'status': 'success',
                'data': match_equality(has_entries({
                    'functionalities': {
                        'payments': {'type': 'payments'},
                        'yandex_pay': {
                            'type': 'yandex_pay',
                            'partner_type': 'payment_gateway',
                            'payment_gateway_type': 'psp',
                            'gateway_id': 'gw-id',
                            'partner_id': '0f29662b-e928-47b9-a2df-7471a5cc6f6f',
                        },
                    },
                }))
            }),
        )

    @pytest.mark.asyncio
    async def test_request_format_for_yandex_pay_payment_gateway(
        self, make_preregister_request, preregistered_merchant, request_json,
    ):
        request_json['functionality'] = {
            'type': 'yandex_pay',
            'partner_type': 'payment_gateway',
            'payment_gateway_type': 'wtf',
        }
        response = await make_preregister_request(uid=preregistered_merchant.uid, json=request_json)
        response_json = await response.json()

        assert_that(
            response_json,
            equal_to({
                'code': 400,
                'status': 'fail',
                'data': {
                    'message': 'Bad Request',
                    'params': {
                        'functionality': {
                            'payment_gateway_type': ['Invalid enum value wtf'],
                            'gateway_id': ['Missing data for required field.'],
                        }
                    },
                }
            })
        )

    @pytest.mark.asyncio
    async def test_request_format_for_yandex_pay_merchant(
        self, make_preregister_request, preregistered_merchant, request_json,
    ):
        request_json['functionality'] = {
            'type': 'yandex_pay',
            'partner_type': 'merchant',
            'merchant_desired_gateway': 1,
            'merchant_gateway_id': 2,
        }
        response = await make_preregister_request(uid=preregistered_merchant.uid, json=request_json)
        response_json = await response.json()

        assert_that(
            response_json,
            equal_to({
                'code': 400,
                'status': 'fail',
                'data': {
                    'message': 'Bad Request',
                    'params': {
                        'functionality': {
                            'merchant_desired_gateway': ['Not a valid string.'],
                            'merchant_gateway_id': ['Not a valid string.'],
                        }
                    }
                }
            }),
        )

    @pytest.mark.parametrize('request_json', request_json_action_kwargs_cases(), indirect=True)
    @pytest.mark.asyncio
    async def test_action_kwargs(self, make_preregister_request, request_json, action,
                                 preregistered_merchant):
        await make_preregister_request(uid=preregistered_merchant.uid, json=request_json)
        kwargs = action.call_args[1]
        assert_that(
            kwargs,
            has_entries(dict(
                inn=request_json['inn'],
                categories=request_json['categories'],
                services=request_json['services'],
                require_online=request_json['require_online'],
            ))
        )

    @pytest.mark.asyncio
    async def test_default_functionality(self, make_preregister_request, request_json, action):
        del request_json['functionality']
        await make_preregister_request(uid=123, json=request_json)

        assert_that(
            action.call_args[1],
            has_entry('functionality', PaymentsFunctionalityData()),
        )


class TestGetPreregistration:
    @pytest.fixture(autouse=True)
    def action(self, mock_action, merchant_preregistration):
        return mock_action(GetMerchantPreregistrationAction, merchant_preregistration)

    @pytest.fixture
    async def make_request(self, payments_client, merchant_uid):
        async def _inner(uid=merchant_uid, json=None):
            return await payments_client.get(f'/v1/merchant/{uid}/preregister', json=json)

        return _inner

    def _get_expected_response(self, merchant_preregistration):
        preregister_data = merchant_preregistration.preregister_data
        raw_preregister_data = merchant_preregistration.raw_preregister_data
        expected_response = {
            'data': {
                'preregister_data': {
                    'inn': preregister_data.inn,
                    'require_online': preregister_data.require_online,
                    'categories': preregister_data.categories,
                    'services': preregister_data.services,
                },
                'raw_preregister_data': {
                    'inn': raw_preregister_data.inn,
                    'require_online': raw_preregister_data.require_online,
                    'categories': raw_preregister_data.categories,
                    'services': raw_preregister_data.services,
                }
            }
        }
        return expected_response

    @pytest.mark.asyncio
    async def test_response(self, make_request, merchant_preregistration):
        response = await make_request()
        response_json = await response.json()
        assert response.status == 200
        assert response_json == self._get_expected_response(merchant_preregistration)
