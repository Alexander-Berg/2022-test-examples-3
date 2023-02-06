import re
from uuid import UUID

import pytest
from aioresponses import CallbackResult

from hamcrest import assert_that, has_entry, has_properties

from mail.payments.payments.core.entities.enums import YandexPayPartnerType, YandexPayPaymentGatewayType
from mail.payments.payments.interactions.yandex_pay_admin.entities import Contact, Document, YandexPayAdminDocumentType
from mail.payments.payments.interactions.yandex_pay_admin.exceptions import (
    PartnerTypeChangedYaPayAdminError, PSPExternalIDChangedYaPayAdminError, PSPExternalIDEmptyYaPayAdminError,
    SchemaValidationYaPayAdminError
)


@pytest.mark.asyncio
async def test_put_partner_call(
    aioresponses_mocker,
    yandex_pay_admin_client,
    payments_settings,
):
    call = None

    def callback(url, **kwargs):
        nonlocal call
        call = kwargs
        return CallbackResult(status=200, payload={'status': 'success', 'code': 200, 'data': {}})

    aioresponses_mocker.put(
        re.compile(f"^{payments_settings.YANDEX_PAY_ADMIN_URL.rstrip('/')}/api/v1/partner$"),
        callback=callback,
    )

    await yandex_pay_admin_client.put_partner(
        partner_id=UUID('0aa36b83-3237-4634-b9dd-ddbe9118c3dc'),
        name='name',
        psp_external_id='gw-id',
        partner_type=YandexPayPartnerType.MERCHANT,
        payment_gateway_type=YandexPayPaymentGatewayType.DIRECT_MERCHANT,
        merchant_gateway_id='m-gw-id',
        merchant_desired_gateway='sberbank',
        uid=123123,
        contact=Contact(
            email='email@test',
            phone='+1(000)555-0100',
            name='John',
            surname='Doe',
            patronymic='Татьянович',
        ),
    )

    assert_that(
        call,
        has_entry(
            'json',
            {
                'partner_id': '0aa36b83-3237-4634-b9dd-ddbe9118c3dc',
                'name': 'name',
                'psp_external_id': 'gw-id',
                'type': YandexPayPartnerType.MERCHANT.value,
                'payment_gateway_type': YandexPayPaymentGatewayType.DIRECT_MERCHANT.value,
                'merchant_gateway_id': 'm-gw-id',
                'merchant_desired_gateway': 'sberbank',
                'uid': 123123,
                'contact': {
                    'email': 'email@test',
                    'phone': '+1(000)555-0100',
                    'name': 'John',
                    'surname': 'Doe',
                    'patronymic': 'Татьянович',
                },
            }
        )
    )


@pytest.mark.asyncio
async def test_put_partner_optional_params(
    aioresponses_mocker,
    yandex_pay_admin_client,
    payments_settings,
):
    call = None

    def callback(url, **kwargs):
        nonlocal call
        call = kwargs
        return CallbackResult(status=200, payload={'status': 'success', 'code': 200, 'data': {}})

    aioresponses_mocker.put(
        re.compile(f"^{payments_settings.YANDEX_PAY_ADMIN_URL.rstrip('/')}/api/v1/partner$"),
        callback=callback,
    )

    await yandex_pay_admin_client.put_partner(
        partner_id=UUID('0aa36b83-3237-4634-b9dd-ddbe9118c3dc'),
        name='name',
        partner_type=YandexPayPartnerType.MERCHANT,
        uid=123123,
        contact=Contact(
            email='email@test',
            phone='+1(000)555-0100',
            name='John',
            surname='Doe',
        ),
    )

    assert_that(
        call,
        has_entry(
            'json',
            {
                'partner_id': '0aa36b83-3237-4634-b9dd-ddbe9118c3dc',
                'name': 'name',
                'type': YandexPayPartnerType.MERCHANT.value,
                'uid': 123123,
                'contact': {
                    'email': 'email@test',
                    'phone': '+1(000)555-0100',
                    'name': 'John',
                    'surname': 'Doe',
                },
            }
        )
    )


@pytest.mark.asyncio
async def test_update_moderation(
    aioresponses_mocker,
    yandex_pay_admin_client,
    payments_settings,
):
    call = None

    def callback(url, **kwargs):
        nonlocal call
        call = kwargs
        return CallbackResult(status=200, payload={'status': 'success', 'code': 200, 'data': {}})

    aioresponses_mocker.post(
        re.compile(
            f"^{payments_settings.YANDEX_PAY_ADMIN_URL.rstrip('/')}"
            '/api/v1/partner/0aa36b83-3237-4634-b9dd-ddbe9118c3dc/moderation$'
        ),
        callback=callback,
    )

    await yandex_pay_admin_client.update_moderation(
        partner_id=UUID('0aa36b83-3237-4634-b9dd-ddbe9118c3dc'),
        verified=True,
        documents=[
            Document(
                path='foo',
                name='bar',
                type=YandexPayAdminDocumentType.OFFER,
            ),
            Document(
                path='baz',
                name=None,
                type=YandexPayAdminDocumentType.PCI_DSS_CERT,
            ),
        ]
    )

    assert_that(
        call,
        has_entry(
            'json',
            {
                'verified': True,
                'documents': [
                    {'path': 'foo', 'name': 'bar', 'type': 'offer'},
                    {'path': 'baz', 'name': None, 'type': 'pci_dss_cert'},
                ]
            }
        )
    )


@pytest.mark.parametrize('body_data, expected_cls', (
    ({'message': 'PSP_EXTERNAL_ID_CHANGED'}, PSPExternalIDChangedYaPayAdminError),
    ({'message': 'PSP_EXTERNAL_ID_EMPTY'}, PSPExternalIDEmptyYaPayAdminError),
    ({'message': 'PARTNER_TYPE_CHANGED'}, PartnerTypeChangedYaPayAdminError),
    ({'message': 'SCHEMA_VALIDATION_ERROR', 'params': {'the': {'schema': 'error'}}}, SchemaValidationYaPayAdminError),
))
@pytest.mark.asyncio
async def test_exceptions(
    aioresponses_mocker,
    yandex_pay_admin_client,
    payments_settings,
    body_data,
    expected_cls,
):
    def callback(url, **kwargs):
        return CallbackResult(method='POST', status=499, payload={'status': 'fail', 'code': 499, 'data': body_data})

    url = yandex_pay_admin_client.endpoint_url('/exception')
    aioresponses_mocker.post(
        re.compile(f'^{url}$'),
        callback=callback,
    )

    with pytest.raises(expected_cls) as exc_info:
        await yandex_pay_admin_client.post(
            'exception_method',
            url,
        )

    assert_that(
        exc_info.value,
        has_properties(
            status_code=499,
            service=yandex_pay_admin_client.SERVICE,
            method='POST',
            params=body_data.get('params'),
        ),
    )


@pytest.fixture
async def yandex_pay_admin_client(create_client):
    from mail.payments.payments.interactions import YandexPayAdminClient
    client = create_client(YandexPayAdminClient)
    yield client
    await client.close()
