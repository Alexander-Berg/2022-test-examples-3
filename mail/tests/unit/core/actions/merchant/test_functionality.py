from uuid import UUID

import pytest

from hamcrest import assert_that, equal_to, has_properties, instance_of, match_equality

from mail.payments.payments.core.actions.merchant.functionality import (
    PutPaymentsMerchantFunctionalityAction, PutYandexPayMerchantFunctionalityAction
)
from mail.payments.payments.core.entities.enums import (
    FunctionalityType, YandexPayPartnerType, YandexPayPaymentGatewayType
)
from mail.payments.payments.core.entities.functionality import (
    PaymentsFunctionalityData, YandexPayMerchantFunctionalityData, YandexPayPaymentGatewayFunctionalityData
)
from mail.payments.payments.core.exceptions import (
    MerchantContactIsEmptyError, YandexPayPartnerTypeChangedError, YandexPayPSPExternalIDChangedError,
    YandexPayPSPExternalIDEmptyError, YandexPaySchemaValidationError
)
from mail.payments.payments.interactions.yandex_pay_admin import YandexPayAdminClient
from mail.payments.payments.interactions.yandex_pay_admin.entities import Contact
from mail.payments.payments.interactions.yandex_pay_admin.exceptions import (
    PartnerTypeChangedYaPayAdminError, PSPExternalIDChangedYaPayAdminError, PSPExternalIDEmptyYaPayAdminError,
    SchemaValidationYaPayAdminError
)


class TestYandexPayFunctionality:
    @pytest.fixture(autouse=True)
    def mock_yandex_pay_admin(self, mocker):
        return mocker.patch.object(YandexPayAdminClient, 'put_partner', mocker.AsyncMock())

    @pytest.mark.parametrize('func_data, expected_result_data', (
        (
            YandexPayMerchantFunctionalityData(),
            equal_to(
                YandexPayMerchantFunctionalityData(
                    partner_id=match_equality(instance_of(UUID)),
                )
            ),
        ),
        (
            YandexPayMerchantFunctionalityData(
                merchant_desired_gateway='sberbank please',
                merchant_gateway_id='payture',
            ),
            equal_to(
                YandexPayMerchantFunctionalityData(
                    partner_id=match_equality(instance_of(UUID)),
                    merchant_desired_gateway='sberbank please',
                    merchant_gateway_id='payture',
                )
            ),
        ),
        (
            YandexPayPaymentGatewayFunctionalityData(
                payment_gateway_type=YandexPayPaymentGatewayType.DIRECT_MERCHANT,
                gateway_id='123',
            ),
            equal_to(
                YandexPayPaymentGatewayFunctionalityData(
                    partner_id=match_equality(instance_of(UUID)),
                    gateway_id='123',
                    payment_gateway_type=YandexPayPaymentGatewayType.DIRECT_MERCHANT,
                )
            ),
        ),
    ))
    @pytest.mark.asyncio
    async def test_creates_functionality(self, merchant, storage, func_data, expected_result_data):
        await PutYandexPayMerchantFunctionalityAction(merchant=merchant, data=func_data).run()

        assert_that(
            await storage.functionality.get(merchant.uid, FunctionalityType.YANDEX_PAY),
            has_properties({
                'data': expected_result_data,
            })
        )

    @pytest.mark.asyncio
    async def test_updates_functionality(self, merchant, storage):
        await PutYandexPayMerchantFunctionalityAction(
            merchant=merchant,
            data=YandexPayPaymentGatewayFunctionalityData(
                gateway_id='123',
                payment_gateway_type=YandexPayPaymentGatewayType.PSP,
            )
        ).run()
        new_func_data = YandexPayPaymentGatewayFunctionalityData(
            gateway_id='123456',
            payment_gateway_type=YandexPayPaymentGatewayType.DIRECT_MERCHANT,
        )

        await PutYandexPayMerchantFunctionalityAction(merchant=merchant, data=new_func_data).run()

        assert_that(
            await storage.functionality.get(merchant.uid, FunctionalityType.YANDEX_PAY),
            has_properties({
                'data': new_func_data,
            })
        )

    @pytest.mark.asyncio
    async def test_calls_put_partner_for_payment_gateway(self, merchant, mock_yandex_pay_admin):
        func_data = YandexPayPaymentGatewayFunctionalityData(
            payment_gateway_type=YandexPayPaymentGatewayType.DIRECT_MERCHANT,
            gateway_id='123',
        )
        functionality = await PutYandexPayMerchantFunctionalityAction(merchant=merchant, data=func_data).run()

        mock_yandex_pay_admin.assert_called_once_with(
            partner_id=functionality.data.partner_id,
            psp_external_id='123',
            payment_gateway_type=YandexPayPaymentGatewayType.DIRECT_MERCHANT,
            partner_type=YandexPayPartnerType.PAYMENT_GATEWAY,
            name=merchant.name,
            uid=merchant.uid,
            contact=Contact(
                email=merchant.contact.email,
                phone=merchant.contact.phone,
                name=merchant.contact.name,
                surname=merchant.contact.surname,
                patronymic=merchant.contact.patronymic,
            ),
        )

    @pytest.mark.asyncio
    async def test_calls_put_partner_for_merchant(self, merchant, mock_yandex_pay_admin):
        func_data = YandexPayMerchantFunctionalityData(
            merchant_gateway_id='mgw-id',
            merchant_desired_gateway='desired-gw',
        )
        functionality = await PutYandexPayMerchantFunctionalityAction(merchant=merchant, data=func_data).run()

        mock_yandex_pay_admin.assert_called_once_with(
            partner_id=functionality.data.partner_id,
            partner_type=YandexPayPartnerType.MERCHANT,
            merchant_gateway_id='mgw-id',
            merchant_desired_gateway='desired-gw',
            name=merchant.name,
            uid=merchant.uid,
            contact=Contact(
                email=merchant.contact.email,
                phone=merchant.contact.phone,
                name=merchant.contact.name,
                surname=merchant.contact.surname,
                patronymic=merchant.contact.patronymic,
            ),
        )

    @pytest.mark.asyncio
    async def test_calls_put_partner__when_contact_is_empty(self, merchant, mock_yandex_pay_admin):
        func_data = YandexPayPaymentGatewayFunctionalityData(
            payment_gateway_type=YandexPayPaymentGatewayType.DIRECT_MERCHANT,
            gateway_id='123',
        )
        merchant.data.persons = []

        with pytest.raises(MerchantContactIsEmptyError):
            await PutYandexPayMerchantFunctionalityAction(merchant=merchant, data=func_data).run()

    @pytest.mark.asyncio
    async def test_calls_put_partner__when_merchant_name_is_empty(self, merchant, mock_yandex_pay_admin):
        func_data = YandexPayPaymentGatewayFunctionalityData(
            payment_gateway_type=YandexPayPaymentGatewayType.DIRECT_MERCHANT,
            gateway_id='123',
        )
        merchant.name = None
        merchant.data.organization.full_name = 'ABCDE'

        await PutYandexPayMerchantFunctionalityAction(merchant=merchant, data=func_data).run()

        assert mock_yandex_pay_admin.call_args.kwargs['name'] == 'ABCDE'

    @pytest.mark.asyncio
    async def test_calls_put_partner__when_all_merchant_names_are_empty(self, merchant, mock_yandex_pay_admin):
        func_data = YandexPayPaymentGatewayFunctionalityData(
            payment_gateway_type=YandexPayPaymentGatewayType.DIRECT_MERCHANT,
            gateway_id='123',
        )
        merchant.name = None
        merchant.data.organization.full_name = None

        await PutYandexPayMerchantFunctionalityAction(merchant=merchant, data=func_data).run()

        assert mock_yandex_pay_admin.call_args.kwargs['name'] == ''

    @pytest.mark.parametrize('exc, expected_exc, expected_params', (
        (PartnerTypeChangedYaPayAdminError, YandexPayPartnerTypeChangedError, {}),
        (PSPExternalIDChangedYaPayAdminError, YandexPayPSPExternalIDChangedError, {}),
        (SchemaValidationYaPayAdminError, YandexPaySchemaValidationError, {'error': 'rrore'}),
        (PSPExternalIDEmptyYaPayAdminError, YandexPayPSPExternalIDEmptyError, {}),
    ))
    @pytest.mark.asyncio
    async def test_on_interaction_error__raises_core_error(self, mocker, merchant, exc, expected_exc, expected_params):
        exc = exc(service='ya-pay-admin', method='POST', params={'error': 'rrore'}, status_code=500)
        mocker.patch.object(YandexPayAdminClient, 'put_partner', mocker.AsyncMock(side_effect=exc))
        func_data = YandexPayPaymentGatewayFunctionalityData(
            payment_gateway_type=YandexPayPaymentGatewayType.DIRECT_MERCHANT,
            gateway_id='123',
        )

        with pytest.raises(expected_exc) as exc_info:
            await PutYandexPayMerchantFunctionalityAction(merchant=merchant, data=func_data).run()

        assert_that(
            getattr(exc_info.value, 'params'),
            equal_to(expected_params),
        )


class TestPaymentsFunctionality:
    @pytest.mark.asyncio
    async def test_creates(self, merchant, storage):
        await PutPaymentsMerchantFunctionalityAction(
            merchant=merchant,
            data=PaymentsFunctionalityData(),
        ).run()

        assert_that(
            await storage.functionality.get(merchant.uid, FunctionalityType.PAYMENTS),
            has_properties({
                'data': PaymentsFunctionalityData(),
            })
        )

    @pytest.mark.asyncio
    async def test_updates(self, merchant, storage):
        await PutPaymentsMerchantFunctionalityAction(
            merchant=merchant,
            data=PaymentsFunctionalityData()
        ).run()

        new_func_data = PaymentsFunctionalityData()
        await PutPaymentsMerchantFunctionalityAction(
            merchant=merchant,
            data=new_func_data,
        ).run()

        assert_that(
            await storage.functionality.get(merchant.uid, FunctionalityType.PAYMENTS),
            has_properties({
                'data': new_func_data,
            })
        )
