import pytest

from hamcrest import assert_that, contains, ends_with, equal_to, has_entries

from mail.payments.payments.interactions.trust import TrustPaymentMethodsClient
from mail.payments.payments.tests.utils import dummy_coro
from mail.payments.payments.utils.helpers import without_none


@pytest.mark.asyncio
async def test__get_payment_methods(trust_client, uid, acquirer):
    headers = {'X-UID': str(uid)}
    expected_params = {'show-plates': False,
                       'show-enabled': True,
                       'show-bound': False,
                       'show_trust_payment_id': False}

    await trust_client._get_payment_methods(uid=uid, acquirer=acquirer, headers=headers)
    assert_that(
        (trust_client.call_args[1], trust_client.call_args[2], trust_client.call_kwargs),
        contains(
            'GET',
            ends_with('/payment-methods'),
            has_entries({
                'headers': headers,
                'params': expected_params,
                'uid': uid,
                'acquirer': acquirer,
            })
        )
    )


@pytest.fixture
def trust_response_with_several_payment_methods():
    return {
        "status": "success",
        "enabled_payment_methods": [
            {
                "firm_id": 7,
                "max_amount": "5000.00",
                "payment_method": "card",
                "currency": "USD"
            },
            {
                "firm_id": 5,
                "max_amount": "10000.00",
                "payment_method": "card",
                "currency": "EUR"
            }
        ]
    }


@pytest.fixture
def enabled_currency():
    return 'USD'


@pytest.fixture
def trust_response(enabled_currency):
    return {
        "status": "success",
        "enabled_payment_methods": [
            {
                "firm_id": 7,
                "max_amount": "5000.00",
                "payment_method": "card",
                "currency": enabled_currency
            }
        ]
    }


@pytest.mark.asyncio
async def test__get_payment_methods_calls(mocker, trust_client, uid, trust_response, acquirer):
    _get_payment_methods = mocker.patch.object(
        trust_client,
        '_get_payment_methods',
        mocker.Mock(return_value=dummy_coro(trust_response)),
    )
    await trust_client.get_enabled_currency(uid=uid, acquirer=acquirer)
    expected_headers = {'X-UID': str(uid)}
    _get_payment_methods.assert_called_once_with(uid=uid, acquirer=acquirer, headers=expected_headers)


@pytest.mark.asyncio
async def test_get_enabled_currency(mocker, trust_client, uid, enabled_currency, trust_response, acquirer):
    mocker.patch.object(
        trust_client,
        '_get_payment_methods',
        mocker.Mock(return_value=dummy_coro(trust_response)),
    )

    assert_that(
        await trust_client.get_enabled_currency(uid=uid, acquirer=acquirer),
        equal_to(enabled_currency),
    )


@pytest.mark.parametrize('default_currency', [None, 'GBP'])
@pytest.mark.asyncio
async def test_get_enabled_currency_if_several_currencies(mocker,
                                                          trust_client,
                                                          uid,
                                                          default_currency,
                                                          trust_response_with_several_payment_methods,
                                                          acquirer,
                                                          ):
    mocker.patch.object(
        trust_client,
        '_get_payment_methods',
        mocker.Mock(return_value=dummy_coro(trust_response_with_several_payment_methods)),
    )
    expected_currency = TrustPaymentMethodsClient.DEFAULT_TRUST_CURRENCY if default_currency is None \
        else default_currency

    kwargs = without_none({'uid': uid, 'acquirer': acquirer, 'default_currency': default_currency})
    assert_that(
        await trust_client.get_enabled_currency(**kwargs),
        equal_to(expected_currency),
    )
