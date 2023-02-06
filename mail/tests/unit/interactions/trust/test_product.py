from random import choice

import pytest

from hamcrest import assert_that, ends_with, equal_to

from mail.payments.payments.core.entities.enums import PeriodUnit
from mail.payments.payments.tests.utils import dummy_coro
from mail.payments.payments.utils.datetime import utcnow


@pytest.mark.asyncio
async def test__product_create(trust_client, uid, acquirer):
    data = {'key': 'value'}
    await trust_client._product_create(uid=uid, acquirer=acquirer, product_data=data)
    assert_that(trust_client.call_args[1], equal_to('POST'))
    assert_that(trust_client.call_args[2], ends_with('/products'))
    assert_that(trust_client.call_kwargs['json'], equal_to(data))
    assert_that(trust_client.call_kwargs['uid'], equal_to(uid))


@pytest.mark.asyncio
async def test__product_get(trust_client, uid, acquirer, randn):
    product_id = randn()
    await trust_client._product_get(uid=uid, acquirer=acquirer, product_id=product_id)
    assert_that(trust_client.call_args[1], equal_to('GET'))
    assert_that(trust_client.call_args[2], ends_with(f'/products/{product_id}'))
    assert_that(trust_client.call_kwargs['uid'], equal_to(uid))


@pytest.mark.asyncio
@pytest.mark.parametrize('service_fee', (None, 1))
async def test_product_create(mocker, trust_client, uid, acquirer, randn, service_fee):
    create_mock = mocker.patch.object(
        trust_client,
        '_product_create',
        mocker.Mock(return_value=dummy_coro('result')),
    )

    partner_id = randn()
    nds = 'xxx'
    inn = '12345'

    assert_that(
        await trust_client.product_create(uid=uid, acquirer=acquirer, partner_id=partner_id, nds=nds, inn=inn,
                                          service_fee=service_fee),
        equal_to('result'),
    )

    create_mock.assert_called_once_with(uid=uid, acquirer=acquirer, product_data={
        'name': f'NDS_{nds}',
        'fiscal_inn': inn,
        'fiscal_nds': nds,
        'fiscal_title': f'NDS_{nds}',
        'product_id': f'{uid}.{inn}.{partner_id}.{nds}' + (f'.{service_fee}' if service_fee else ''),
        'partner_id': f'{partner_id}',
        **({'service_fee': service_fee} if service_fee else {})
    })


@pytest.mark.parametrize('with_trial_period', (True, False))
@pytest.mark.asyncio
async def test_product_subscription_create(mocker, randn, with_trial_period, payments_settings, subscription, merchant,
                                           trust_client, acquirer):
    now = utcnow()
    mocker.patch('mail.payments.payments.interactions.trust.base.utcnow', mocker.Mock(return_value=now))

    create_mock = mocker.patch.object(
        trust_client,
        '_product_create',
        mocker.Mock(return_value=dummy_coro('result')),
    )

    if with_trial_period:
        subscription.trial_period_amount = randn(min=1, max=100)
        subscription.trial_period_units = choice(list(PeriodUnit))
    else:
        subscription.trial_period_amount = subscription.trial_period_units = None

    product_data = {
        "product_id": subscription.product_id,
        "name": subscription.title,
        "product_type": "subs",
        "subs_period": subscription.period,
        "prices": [
            {
                "currency": price.currency,
                "price": f'{price.price}',
                "region_id": price.region_id,
                "start_ts": str(int(now.timestamp()))
            } for price in subscription.prices
        ],
        "fiscal_nds": subscription.nds.value,
        "fiscal_title": subscription.fiscal_title,
        "partner_id": merchant.client_id,
        'aggregated_charging': int(payments_settings.TRUST_AGGREGATED_CHARGING),
        'subs_retry_charging_limit': payments_settings.TRUST_SUBS_RETRY_CHARGING_LIMIT,
        'subs_retry_charging_delay': payments_settings.TRUST_SUBS_RETRY_CHARGING_DELAY,
    }
    if with_trial_period:
        product_data['subs_trial_period'] = subscription.trial_period
        product_data['single_purchase'] = 1

    assert_that(await trust_client.product_subscription_create(merchant.uid, acquirer, merchant, subscription),
                equal_to('result'))
    create_mock.assert_called_once_with(uid=merchant.uid, acquirer=acquirer, product_data=product_data)
