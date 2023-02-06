import uuid
from datetime import timedelta
from decimal import Decimal
from urllib.parse import urlencode

import pytest
from multidict import MultiDict

from sendr_pytest.matchers import convert_then_match
from sendr_utils import enum_value, utcnow

from hamcrest import (
    assert_that, contains, contains_inanyorder, equal_to, has_entries, has_entry, instance_of, is_, match_equality
)

from mail.payments.payments.core.actions.merchant.create import CreateMerchantAction
from mail.payments.payments.core.actions.merchant.get import GetMerchantAction
from mail.payments.payments.core.actions.merchant.get_by_key import GetMerchantByKeyAction
from mail.payments.payments.core.actions.merchant.token import GetMerchantTokenAction, RegenerateMerchantTokenAction
from mail.payments.payments.core.actions.moderation import ScheduleMerchantModerationAction
from mail.payments.payments.core.entities.enums import FunctionalityType, MerchantOAuthMode, YandexPayPaymentGatewayType
from mail.payments.payments.core.entities.functionality import (
    Functionalities, PaymentsFunctionalityData, YandexPayPaymentGatewayFunctionalityData
)
from mail.payments.payments.core.entities.merchant import MerchantStat
from mail.payments.payments.core.entities.moderation import ModerationData
from mail.payments.payments.core.exceptions import ChildMerchantError, OAuthCodeError
from mail.payments.payments.interactions.spark_suggest.entities import SparkSuggestItem
from mail.payments.payments.utils.helpers import without_none


class TestMerchantGet:
    @pytest.fixture
    def setup_no_oauth(self, merchant):
        merchant.oauth = []

    @pytest.fixture(autouse=True)
    def setup_functionalities(self, merchant):
        merchant.functionalities = Functionalities(
            payments=PaymentsFunctionalityData(),
            yandex_pay=YandexPayPaymentGatewayFunctionalityData(
                payment_gateway_type=YandexPayPaymentGatewayType.PSP,
                gateway_id='123',
                partner_id=uuid.uuid4(),
            ),
        )

    @pytest.fixture(autouse=True)
    def setup_moderations(self, merchant):
        merchant.moderations = {
            FunctionalityType.PAYMENTS: ModerationData(
                approved=True,
                has_moderation=True,
                has_ongoing=False,
                reasons=[5],
            ),
            FunctionalityType.YANDEX_PAY: None,
        }

    @pytest.fixture
    def action(self, mock_action, merchant):
        return mock_action(GetMerchantAction, merchant)

    @pytest.fixture
    async def response(self, action, payments_client, merchant):
        return await payments_client.get(f'/v1/merchant/{merchant.uid}')

    def test_action_call(self, action, merchant, response):
        action.assert_called_once_with(uid=merchant.uid)

    @pytest.mark.asyncio
    async def test_no_oauth(self, setup_no_oauth, response):
        assert_that(
            await response.json(),
            has_entries({
                'data': has_entries({'oauth': []})
            })
        )

    @pytest.mark.asyncio
    async def test_options(self, setup_no_oauth, merchant, response):
        assert_that(
            await response.json(),
            has_entries({
                'data': has_entries({
                    'options': {
                        'allow_create_orders_via_ui': merchant.options.allow_create_orders_via_ui,
                        'allow_create_service_merchants': merchant.options.allow_create_service_merchants,
                        'hide_commission': merchant.options.hide_commission,
                        'can_skip_registration': merchant.options.can_skip_registration,
                    }
                })
            })
        )

    @pytest.mark.asyncio
    async def test_functionalities(self, setup_no_oauth, merchant, response):
        assert_that(
            await response.json(),
            has_entries({
                'data': has_entries({
                    'functionalities': {
                        'payments': {'type': 'payments'},
                        'yandex_pay': {
                            'type': 'yandex_pay',
                            'partner_type': 'payment_gateway',
                            'payment_gateway_type': 'psp',
                            'gateway_id': '123',
                            'partner_id': match_equality(convert_then_match(uuid.UUID, instance_of(uuid.UUID))),
                        },
                    }
                })
            })
        )

    @pytest.mark.asyncio
    async def test_moderations(self, setup_no_oauth, merchant, response):
        assert_that(
            await response.json(),
            has_entries({
                'data': has_entries({
                    'moderations': {
                        'payments': {'approved': True, 'hasOngoing': False, 'hasModeration': True, 'reasons': [5]},
                        'yandex_pay': None,
                    }
                })
            })
        )

    class TestWithOAuth:
        @pytest.fixture
        def setup_oauth(self, merchant, merchant_oauth, now_delta):
            merchant_oauth.expires = utcnow() + now_delta
            merchant.oauth = [merchant_oauth]

        @pytest.mark.parametrize('now_delta,expired', (
            (timedelta(days=1), False),
            (timedelta(days=-1), True),
        ))
        @pytest.mark.asyncio
        async def test_oauth(self, setup_oauth, response, merchant_oauth, expired):
            assert_that(
                (await response.json())['data'],
                has_entries({
                    'oauth': contains(has_entries({'expired': expired, 'mode': merchant_oauth.mode.value}))
                })
            )

    class TestParent:
        @pytest.fixture
        def action(self, mock_action, merchant_with_parent):
            return mock_action(GetMerchantAction, merchant_with_parent)

        @pytest.mark.asyncio
        async def test_response_parent(self, merchant, response):
            assert_that(
                await response.json(),
                has_entries({
                    'data': has_entries({
                        'billing': {
                            'client_id': merchant.client_id,
                            'person_id': merchant.person_id,
                            'contract_id': merchant.contract_id,
                            'trust_partner_id': merchant.client_id,
                            'trust_submerchant_id': merchant.submerchant_id,
                        },
                        'parent_uid': merchant.uid,
                    }),
                })
            )


@pytest.fixture
def fast_moderation_param(randbool):
    return randbool()


@pytest.fixture
def request_json(fast_moderation_param):
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
        'fast_moderation': fast_moderation_param,
        'functionality': {'type': 'payments'},
    }


class TestMerchantPost:
    @pytest.fixture
    def action(self, mock_action, merchant):
        return mock_action(CreateMerchantAction, merchant)

    @pytest.fixture
    async def response(self, action, merchant, payments_client, request_json):
        return await payments_client.post(f'/v1/merchant/{merchant.uid}', json=request_json)

    class TestSuccess:
        @pytest.fixture(autouse=True, params=(
            pytest.param(lambda d: d, id='default'),
            pytest.param(lambda d: d['organization'].pop('siteUrl'), id='without-site-url'),
        ))
        def setup(self, request, request_json):
            request.param(request_json)

        def test_success__ok(self, response):
            assert response.status == 200

        def test_success__fast_moderation_param(self, action, response, fast_moderation_param):
            assert action.call_args[1].get('fast_moderation') == fast_moderation_param

    class TestBadRequest:
        @pytest.fixture(autouse=True, params=(
            pytest.param(lambda d: d.pop('name'), id='missing-name'),
            pytest.param(lambda d: d.pop('addresses'), id='missing-addresses'),
            pytest.param(lambda d: d.pop('bank'), id='missing-bank'),
            pytest.param(lambda d: d.pop('organization'), id='missing-organization'),
            pytest.param(lambda d: d.pop('persons'), id='missing-persons'),
            pytest.param(lambda d: d['persons'].pop('ceo'), id='missing-persons-ceo'),
            pytest.param(lambda d: d['addresses'].pop('legal'), id='missing-addresses-legal'),
            pytest.param(lambda d: d['addresses']['legal'].update({'country': 'RU'}), id='address-country-not-RUS'),
            pytest.param(lambda d: d['organization'].pop('name'), id='missing-organization-name'),
            pytest.param(lambda d: d['organization'].pop('englishName'), id='missing-organization-english-name'),
            pytest.param(lambda d: d['organization'].pop('fullName'), id='missing-organization-full-name'),
        ))
        def setup(self, request, request_json):
            request.param(request_json)

        @pytest.mark.asyncio
        async def test_bad_request(self, response):
            assert response.status == 400

    class TestAllowMissingOrganizationNameForEntrepreneur:
        @pytest.fixture(autouse=True, params=('name', 'englishName', 'fullName'))
        def test_allow_missing_organization_name(self, request, request_json):
            request_json['organization']['inn'] = '01234567890'  # make len > 10
            request_json['organization'].pop(request.param)

        @pytest.mark.asyncio
        async def test_allow_missing_organization_name_for_entrepreneur__ok(self, response):
            assert response.status == 200

    @pytest.mark.asyncio
    async def test_default_functionality(self, payments_client, merchant, request_json, action):
        del request_json['functionality']
        await payments_client.post(f'/v1/merchant/{merchant.uid}', json=request_json)

        assert_that(
            action.call_args[1],
            has_entry('functionality', PaymentsFunctionalityData()),
        )


class TestMerchantByDeveloperKey:
    @pytest.fixture
    def action(self, mock_action, merchant):
        return mock_action(GetMerchantByKeyAction, merchant)

    @pytest.fixture
    def key(self):
        return 'test-merchant-by-developer-key-key'

    @pytest.fixture
    async def response(self, action, payments_client, key):
        return await payments_client.get(f'/v1/merchant_by_key/{key}')

    def test_merchant_by_developer_key__params(self, key, response, action):
        assert_that(
            action.call_args[1],
            has_entries({
                'key': key,
                'user_ip': is_(str),
            }),
        )


class TestMerchantModerationPost:
    @pytest.fixture
    def action(self, mock_action):
        return mock_action(ScheduleMerchantModerationAction)

    @pytest.fixture
    async def response(self, payments_client, merchant, action, data):
        return await payments_client.post(f'/v1/merchant/{merchant.uid}/moderation', json=data)

    @pytest.mark.parametrize('data, expected_func_type', (
        ({}, FunctionalityType.PAYMENTS),
        ({'functionality_type': FunctionalityType.PAYMENTS.value}, FunctionalityType.PAYMENTS),
        ({'functionality_type': FunctionalityType.YANDEX_PAY.value}, FunctionalityType.YANDEX_PAY),
    ))
    @pytest.mark.asyncio
    async def test_merchant_moderation_post__params(self, merchant, response, action, expected_func_type):
        action.assert_called_once_with(uid=merchant.uid, functionality_type=expected_func_type)


class TestMerchantTokenGet:
    @pytest.fixture
    def action(self, mock_action, merchant):
        return mock_action(GetMerchantTokenAction, {'token': merchant.token})

    @pytest.fixture
    async def response(self, action, payments_client, merchant):
        return await payments_client.get(f'/v1/merchant/{merchant.uid}/token')

    def test_merchant_token_get__params(self, merchant, response, action):
        action.assert_called_once_with(uid=merchant.uid)

    @pytest.mark.asyncio
    async def test_token_returned(self, response, merchant):
        assert_that(
            await response.json(),
            has_entries({
                'data': has_entries({
                    'token': merchant.token,
                }),
            })
        )


class TestMerchantTokenPost(TestMerchantTokenGet):
    @pytest.fixture
    def action(self, mock_action, merchant):
        return mock_action(RegenerateMerchantTokenAction, {'token': merchant.token})

    @pytest.fixture
    async def response(self, action, payments_client, merchant):
        return await payments_client.post(f'/v1/merchant/{merchant.uid}/token')


class TestMerchantServiceGet:
    @pytest.fixture(autouse=True)
    def action(self, mock_action, service_merchant):
        from mail.payments.payments.core.actions.service_merchant.get import GetServiceMerchantAction
        return mock_action(GetServiceMerchantAction, service_merchant)

    @pytest.fixture
    def params(self, merchant, service_merchant):
        return {'uid': merchant.uid, 'service_merchant_id': service_merchant.service_merchant_id}

    @pytest.fixture
    def expected(self, service_merchant):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'uid': service_merchant.uid,
            'service_id': service_merchant.service_id,
            'entity_id': service_merchant.entity_id,
        }

    @pytest.fixture
    async def response(self, payments_client, merchant, service_merchant):
        return await payments_client.get(f'/v1/merchant/{merchant.uid}/service/{service_merchant.service_merchant_id}')

    @pytest.mark.asyncio
    async def test_merchant_service_returned(self, response, service_merchant, expected):
        assert_that(
            await response.json(),
            has_entries({
                'data': has_entries(expected),
            })
        )

    def test_params(self, params, response, action):
        action.assert_called_once_with(**params)


class TestMerchantServiceListHandler(TestMerchantServiceGet):
    @pytest.fixture(autouse=True)
    def action(self, mock_action, service_merchant):
        from mail.payments.payments.core.actions.service_merchant.get import GetServiceMerchantListAction
        return mock_action(GetServiceMerchantListAction, [service_merchant])

    @pytest.fixture
    async def response(self, payments_client, merchant):
        return await payments_client.get(f'/v1/merchant/{merchant.uid}/service')

    def test_params(self, merchant, response, action):
        action.assert_called_once_with(uid=merchant.uid)

    @pytest.mark.asyncio
    async def test_merchant_service_returned(self, response, service_merchant, expected):
        data = (await response.json())['data']
        assert_that(
            data[0],
            has_entries(expected)
        )


class TestMerchantServicePost(TestMerchantServiceGet):
    @pytest.fixture(autouse=True)
    def action(self, mock_action, service_merchant):
        from mail.payments.payments.core.actions.service_merchant.update import UpdateServiceMerchantAction
        return mock_action(UpdateServiceMerchantAction, service_merchant)

    @pytest.fixture
    def request_json(self, params):
        request_json = {'enabled': True, 'description': 'some new description'}
        params.update(request_json)
        return request_json

    @pytest.fixture
    async def response(self, payments_client, merchant, service_merchant, request_json):
        return await payments_client.post(
            f'/v1/merchant/{merchant.uid}/service/{service_merchant.service_merchant_id}',
            json=request_json
        )


class TestMerchantServiceDelete(TestMerchantServiceGet):
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.service_merchant.delete import DeleteServiceMerchantAction
        return mock_action(DeleteServiceMerchantAction)

    @pytest.fixture
    async def response(self, payments_client, merchant, service_merchant):
        return await payments_client.delete(
            f'/v1/merchant/{merchant.uid}/service/{service_merchant.service_merchant_id}'
        )

    @pytest.mark.asyncio
    async def test_merchant_service_returned(self, response):
        assert_that(
            await response.json(),
            has_entries({
                'data': {}
            })
        )


class TestMerchantOAuthDelete:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.merchant.oauth_delete import OAuthDeleteMerchantAction
        return mock_action(OAuthDeleteMerchantAction, None)

    @pytest.fixture(params=(None, MerchantOAuthMode.TEST))
    def mode(self, request):
        return request.param

    @pytest.fixture
    async def response(self, payments_client, mode, merchant):
        return await payments_client.delete(
            f'/v1/merchant/{merchant.uid}/oauth',
            json=without_none({'merchant_oauth_mode': enum_value(mode)})
        )

    def test_merchant_oauth_delete__params(self, merchant, response, action):
        action.assert_called_once_with(uid=merchant.uid, merchant_oauth_mode=MerchantOAuthMode.TEST)

    @pytest.mark.parametrize('mode', [MerchantOAuthMode.PROD])
    def test_validation(self, response, action):
        assert response.status == 400


class TestMerchantOAuthStart:
    @pytest.fixture
    def state(self):
        return None

    @pytest.fixture
    def action_result(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def action(self, mock_action, action_result):
        from mail.payments.payments.core.actions.merchant.oauth_start import OAuthStartMerchantAction
        return mock_action(OAuthStartMerchantAction, action_result)

    @pytest.fixture
    async def response(self, payments_client, state, merchant):
        return await payments_client.get(f'/v1/merchant/{merchant.uid}/oauth/start',
                                         params=without_none({'state': state}))

    @pytest.mark.asyncio
    async def test_merchant_oauth_start__params(self, response, merchant, state, action_result):
        assert_that(await response.json(), has_entries({'data': {'url': action_result}}))

    @pytest.mark.parametrize('state', (
        pytest.param(''.join(['!' for _ in range(2020)]), id='state-long'),
    ))
    def test_validation(self, response):
        assert response.status == 400

    class TestParentProblem:
        @pytest.fixture
        def action_result(self):
            return ChildMerchantError

        @pytest.mark.asyncio
        async def test_parent_problem__params(self, response, action):
            assert_that(
                (response.status, await response.json()),
                contains(
                    equal_to(403),
                    has_entries({
                        'code': 403,
                        'data': has_entries({'message': 'CHILD_MERCHANT_DENY'}),
                        'status': 'fail'
                    })
                )
            )


class TestMerchantOAuthComplete:
    @pytest.fixture
    def code(self, rands):
        return rands()

    @pytest.fixture
    def shop_id(self, randn):
        return randn()

    @pytest.fixture
    def action_result(self):
        return None

    @pytest.fixture(autouse=True)
    def action(self, mock_action, action_result):
        from mail.payments.payments.core.actions.merchant.oauth_complete import OAuthCompleteMerchantAction
        return mock_action(OAuthCompleteMerchantAction, action_result)

    @pytest.fixture
    async def response(self, payments_client, shop_id, code, merchant_uid):
        return await payments_client.post(f'/v1/merchant/{merchant_uid}/oauth/complete', json={
            'code': code,
            'shop_id': shop_id,
        })

    def test_merchant_oauth_complete__params(self, code, response, shop_id, merchant_uid, action):
        assert_that(
            action.call_args[1],
            has_entries({
                'code': code,
                'uid': merchant_uid,
                'shop_id': shop_id,
            }),
        )

    class TestCodeProblem:
        @pytest.fixture
        def action_result(self):
            return OAuthCodeError

        @pytest.mark.asyncio
        async def test_code_problem__params(self, response, action):
            assert_that(
                (response.status, await response.json()),
                contains(
                    equal_to(400),
                    has_entries({
                        'code': 400,
                        'data': has_entries({'message': 'CODE_ERROR'}),
                        'status': 'fail'
                    })
                )
            )

    class TestParentProblem:
        @pytest.fixture
        def action_result(self):
            return ChildMerchantError

        @pytest.mark.asyncio
        async def test_parent_problem__params(self, response, action):
            assert_that(
                (response.status, await response.json()),
                contains(
                    equal_to(403),
                    has_entries({
                        'code': 403,
                        'data': has_entries({'message': 'CHILD_MERCHANT_DENY'}),
                        'status': 'fail'
                    })
                )
            )


class TestMcc:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.merchant.get_mcc import GetMccInfoInTrustAction
        return mock_action(GetMccInfoInTrustAction, {'result': []})

    @pytest.fixture
    def params(self, codes):
        p = MultiDict()
        for code in codes:
            p.add('codes[]', code)
        return p

    @pytest.fixture
    async def response(self, params, merchant, payments_client):
        return await payments_client.get(f'/v1/merchant/{merchant.uid}/mcc', params=params)

    @pytest.mark.parametrize('codes', ([1234], [1234, 2345]))
    def test_mcc__params(self, response, codes, action):
        assert_that(
            action.call_args[1]['codes'],
            contains_inanyorder(*codes),
        )


class TestMerchantSuggestHandler:
    @pytest.fixture
    def query(self):
        return '  ИП "Иванов!" #047...    '  # has trailing spaces and punctuations

    @pytest.fixture
    def suggest_list(self):
        return [
            SparkSuggestItem(
                spark_id=i,
                name=f'suggested_name_{i}',
                full_name=f'suggested_full_name_{i}',
                inn=f'suggested_inn_{i}',
                ogrn=f'suggested_ogrn_{i}',
                address=f'suggested_address_{i}',
                leader_name=f'suggested_leader_name_{i}',
                region_name=f'suggested_region_name_{i}',
            ) for i in range(3)
        ]

    @pytest.fixture(autouse=True)
    def action(self, mock_action, suggest_list):
        from mail.payments.payments.core.actions.merchant.suggest import MerchantSuggestAction
        return mock_action(MerchantSuggestAction, suggest_list)

    @pytest.fixture
    async def response(self, payments_client, query):
        return await payments_client.get('/v1/merchant/suggest', params={'query': query})

    @pytest.mark.asyncio
    async def test_merchant_suggest_response(self, suggest_list, response):
        assert [
            {
                'spark_id': item.spark_id,
                'name': item.name,
                'full_name': item.full_name,
                'inn': item.inn,
                'ogrn': item.ogrn,
                'address': item.address,
                'leader_name': item.leader_name,
                'region_name': item.region_name,
            } for item in suggest_list
        ] == (await response.json())['data']

    def test_merchant_suggest_status(self, response):
        assert 200 == response.status

    def test_merchant_suggest_action_call(self, action, query, response):
        action.assert_called_once_with(query='ИП Иванов 047')


class TestOrdersStatsHandler:
    @pytest.fixture
    def orders_sum(self):
        return Decimal(100500)

    @pytest.fixture
    def orders_paid_count(self):
        return 2

    @pytest.fixture(autouse=True)
    def action(self, mock_action, orders_sum, orders_paid_count):
        response = MerchantStat(
            orders_sum=orders_sum,
            orders_paid_count=orders_paid_count,
            orders_created_count=3
        )
        from mail.payments.payments.core.actions.merchant.stats import MerchantOrdersStatsAction
        return mock_action(MerchantOrdersStatsAction, response)

    @pytest.fixture(params=(
        (None, None),
        (utcnow(), None),
        (None, utcnow()),
        (utcnow(), utcnow()),
    ))
    def from_to(self, request):
        return request.param

    @pytest.fixture
    def url(self, merchant_uid, from_to):
        date_from, date_to = from_to
        params = without_none({
            'date_from': None if date_from is None else date_from.isoformat(),
            'date_to': None if date_to is None else date_to.isoformat(),
        })
        url = f'v1/merchant/{merchant_uid}/orders_stats'
        if params:
            url = f'{url}?{urlencode(params)}'
        return url

    @pytest.fixture
    async def response(self, payments_client, url):
        return await payments_client.get(url)

    @pytest.mark.asyncio
    async def test_orders_stats_status(self, response):
        assert 200 == response.status

    @pytest.mark.asyncio
    async def test_orders_stats_action_call(self, response, merchant_uid, action, from_to):
        action.assert_called_once_with(
            uid=merchant_uid,
            **without_none({
                'date_from': from_to[0],
                'date_to': from_to[1]
            })
        )

    @pytest.mark.asyncio
    async def test_orders_stats_response(self, response, orders_sum, orders_paid_count):
        assert_that((await response.json())['data'], has_entries({
            'orders_sum': orders_sum,
            'orders_paid_count': orders_paid_count,
            'orders_created_count': 3,
            'money_average': orders_sum / Decimal(orders_paid_count)
        }))
