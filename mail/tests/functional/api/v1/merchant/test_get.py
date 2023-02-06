import uuid
from datetime import timedelta

import pytest

from sendr_pytest.matchers import convert_then_match
from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, instance_of, match_equality

from mail.payments.payments.core.entities.enums import (
    FunctionalityType, MerchantOAuthMode, MerchantRole, ShopType, YandexPayPartnerType, YandexPayPaymentGatewayType
)
from mail.payments.payments.core.entities.functionality import (
    MerchantFunctionality, PaymentsFunctionalityData, YandexPayPaymentGatewayFunctionalityData
)
from mail.payments.payments.core.entities.merchant import AddressData
from mail.payments.payments.core.entities.merchant_oauth import MerchantOAuth
from mail.payments.payments.core.entities.moderation import Moderation, ModerationType
from mail.payments.payments.interactions.developer.exceptions import DeveloperKeyAccessDeny
from mail.payments.payments.tests.base import BaseTestMerchantRoles
from mail.payments.payments.tests.utils import check_merchant, check_merchant_from_person


@pytest.mark.usefixtures('balance_person_mock')
class BaseTestGetMerchant:
    @pytest.fixture
    def response_func(self, client, tvm, merchant, person_entity):
        async def _inner():
            r = await client.get(f'/v1/merchant/{merchant.uid}')
            assert r.status == 200
            return await r.json()

        return _inner

    @pytest.mark.asyncio
    async def test_without_post_address(self, merchant, stored_person_entity, response_func):
        response = await response_func()
        check_merchant_from_person(merchant, stored_person_entity, response['data'])

    @pytest.mark.asyncio
    async def test_with_post_address(self, merchant, stored_person_entity, response_func, storage):
        stored_person_entity.address_city = 'person-address_city'
        stored_person_entity.address_home = 'person-address_home'
        stored_person_entity.address_postcode = 'person-address_postcode'
        stored_person_entity.address_street = 'person-address_street'

        merchant.data.addresses.append(AddressData(
            type='post',
            city=stored_person_entity.address_city,
            country='RUS',
            home=stored_person_entity.address_home,
            street=stored_person_entity.address_street,
            zip=stored_person_entity.address_postcode,
        ))
        merchant = await storage.merchant.save(merchant)
        merchant.load_data()

        response = await response_func()
        check_merchant_from_person(merchant, stored_person_entity, response['data'])

    @pytest.mark.asyncio
    async def test_key_error(self, balance_client_mocker, merchant, response_func):
        with balance_client_mocker('get_person', exc=KeyError('some key')):
            response = await response_func()
        check_merchant(merchant, response['data'])

    @pytest.mark.parametrize('moderations_data,response_moderation', (
        pytest.param(
            [],
            {'approved': False, 'reasons': [], 'hasOngoing': False, 'hasModeration': False},
            id='no-moderations',
        ),
        pytest.param(
            [{'approved': False, 'reasons': [1, 2]}, {'approved': None}],
            {'approved': False, 'reasons': [1, 2], 'hasOngoing': True, 'hasModeration': True},
            id='disapproved-ongoing',
        ),
        pytest.param(
            [{'approved': False, 'reasons': [1, 2]}, {'approved': True}],
            {'approved': True, 'reasons': [], 'hasOngoing': False, 'hasModeration': True},
            id='disapproved-approved',
        ),
    ))
    @pytest.mark.asyncio
    async def test_moderation(self, storage, merchant, response_func, moderations_data, response_moderation):
        for moderation_data in moderations_data:
            merchant = await storage.merchant.save(merchant)
            await storage.moderation.create(Moderation(
                uid=merchant.uid,
                moderation_type=ModerationType.MERCHANT,
                functionality_type=FunctionalityType.PAYMENTS,
                revision=merchant.revision,
                **moderation_data,
            ))
        response = await response_func()
        assert response['data']['moderation'] == response_moderation

    @pytest.mark.asyncio
    async def test_moderations_for_functionalities(
        self, storage, merchant, response_func
    ):
        await storage.moderation.create(Moderation(
            uid=merchant.uid,
            moderation_type=ModerationType.MERCHANT,
            functionality_type=FunctionalityType.PAYMENTS,
            revision=merchant.revision,
            approved=True,
        ))
        await storage.moderation.create(Moderation(
            uid=merchant.uid,
            moderation_type=ModerationType.MERCHANT,
            functionality_type=FunctionalityType.YANDEX_PAY,
            revision=merchant.revision,
            approved=False,
        ))

        response = await response_func()

        expected_moderations = {
            'payments': {
                'approved': True, 'reasons': [], 'hasOngoing': False, 'hasModeration': True,
            },
            'yandex_pay': {
                'approved': False, 'reasons': [], 'hasOngoing': False, 'hasModeration': True,
            },
        }
        assert response['data']['moderations'] == expected_moderations

    @pytest.mark.asyncio
    async def test_functionalities(
        self, storage, merchant, response_func
    ):
        await storage.functionality.create(MerchantFunctionality(
            uid=merchant.uid,
            functionality_type=FunctionalityType.PAYMENTS,
            data=PaymentsFunctionalityData(),
        ))
        await storage.functionality.create(MerchantFunctionality(
            uid=merchant.uid,
            functionality_type=FunctionalityType.YANDEX_PAY,
            data=YandexPayPaymentGatewayFunctionalityData(
                partner_id=uuid.uuid4(),
                gateway_id='whatever',
                payment_gateway_type=YandexPayPaymentGatewayType.PSP,
            ),
        ))

        response = await response_func()

        expected_functionalities = {
            'payments': {'type': 'payments'},
            'yandex_pay': {
                'type': 'yandex_pay',
                'partner_type': YandexPayPartnerType.PAYMENT_GATEWAY.value,
                'gateway_id': 'whatever',
                'payment_gateway_type': YandexPayPaymentGatewayType.PSP.value,
                'partner_id': match_equality(convert_then_match(uuid.UUID, instance_of(uuid.UUID))),
            },
        }
        assert_that(
            response['data']['functionalities'],
            equal_to(expected_functionalities),
        )

    @pytest.mark.asyncio
    async def test_without_oauth(self, merchant, response_func):
        assert (await response_func())['data']['oauth'] == []

    @pytest.mark.parametrize('expired', (True, False))
    @pytest.mark.asyncio
    async def test_with_oauth(self, storage, merchant, response_func, rands, expired, default_merchant_shops):
        delta = timedelta(days=1)
        if expired:
            delta *= -1
        merchant_oauth = MerchantOAuth(
            uid=merchant.uid,
            shop_id=default_merchant_shops[ShopType.PROD].shop_id,
            poll=True,
            expires=utcnow() + delta,
        )
        merchant_oauth.decrypted_access_token = rands()
        merchant_oauth.decrypted_refresh_token = rands()
        await storage.merchant_oauth.create(merchant_oauth)
        assert (await response_func())['data']['oauth'] == [{
            'mode': MerchantOAuthMode.PROD.value,
            'expired': expired,
        }]


class TestGetMerchant(BaseTestMerchantRoles, BaseTestGetMerchant):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )


class TestGetMerchantByDeveloperKey(BaseTestGetMerchant):
    @pytest.fixture(autouse=True)
    def setup(self, developer_client_mocker, merchant):
        with developer_client_mocker('check_key', merchant.uid) as mock:
            yield mock

    @pytest.fixture
    def response_func(self, client, merchant):
        async def _inner(status=200):
            r = await client.get('/v1/merchant_by_key/somekey')
            assert r.status == status
            return await r.json()

        return _inner

    @pytest.mark.asyncio
    async def test_invalid_key(self, developer_client_mocker, response_func):
        with developer_client_mocker('check_key', exc=DeveloperKeyAccessDeny):
            response = await response_func(403)
            assert response['data']['message'] == 'DEVELOPER_KEY_ACCESS_DENY'
