import datetime
import json

import pytest
from mock import Mock
from hamcrest import assert_that, equal_to, is_not, contains, has_key, has_entry

from travel.rasp.library.python.morda_backend_client.test_utils import MordaBackendClientStub
from travel.rasp.pathfinder_proxy.const import CacheType, Status
from travel.rasp.pathfinder_proxy.serialization import TransferVariantsWithPricesQuerySchema
from travel.rasp.pathfinder_proxy.tests.utils import (  # noqa: F401
    cache, request_not_bot, request_is_bot, transfer_variants, transfer_variants_with_prices, TrainFeeServiceStub
)
from travel.rasp.pathfinder_proxy.tests.utils.factories import HandlerFactory


@pytest.mark.asyncio
async def test_transfers_with_prices_from_cache(cache, request_not_bot):  # noqa: F811
    cache_key = CacheType.MORDA_BACKEND, 'c39', 'c54', datetime.date(2019, 8, 20), 'ru', 'ru'
    await cache.set_cache(*cache_key, json.dumps([{'test': 'from cache'}], ensure_ascii=False))

    handler = HandlerFactory(cache=cache, train_fee_service=TrainFeeServiceStub())

    result = await handler.transfers_with_prices(request_not_bot)
    assert_that(
        json.loads(result.text),
        equal_to({'transfer_variants': [{'test': 'from cache'}], 'status': 'querying'})
    )


@pytest.mark.asyncio
async def test_transfers_with_prices_empty_morda(cache, request_not_bot):  # noqa: F811
    morda_backend_client = MordaBackendClientStub()
    handler = HandlerFactory(morda_backend_client=morda_backend_client, cache=cache,
                             train_fee_service=TrainFeeServiceStub())
    result = await handler.transfers_with_prices(request_not_bot)
    assert_that(result.status, 204)


@pytest.mark.asyncio
async def test_transfers_with_prices(cache, request_not_bot, transfer_variants):  # noqa: F811
    morda_backend_client = MordaBackendClientStub(
        [('c39', 'c54', '2019-08-20', ('train', 'plane'), 'ru')],
        [transfer_variants]
    )

    handler = HandlerFactory(morda_backend_client=morda_backend_client, cache=cache,
                             train_fee_service=TrainFeeServiceStub())
    handler._price_collector.collect = Mock()

    result = await handler.transfers_with_prices(request_not_bot)
    result = json.loads(result.text)

    status = result['status']

    assert_that(status, Status.QUERYING.value)
    assert_that(
        transfer_variants[0]['segments'],
        is_not(contains(has_key('tariffs')))
    )

    query = TransferVariantsWithPricesQuerySchema(strict=True).load(request_not_bot.query).data
    query.pop('is_bot')
    query.pop('include_price_fee')
    handler._price_collector.collect.assert_called_once_with(transfer_variants, query)


@pytest.mark.asyncio
async def test_transfers_with_prices_is_bot(cache, request_is_bot, transfer_variants):  # noqa: F811
    morda_backend_client = MordaBackendClientStub(
        [('c39', 'c54', '2019-08-20', ('train', 'plane'), 'ru')],
        [transfer_variants]
    )

    handler = HandlerFactory(morda_backend_client=morda_backend_client, cache=cache,
                             train_fee_service=TrainFeeServiceStub())
    handler._price_collector.collect = Mock()

    result = await handler.transfers_with_prices(request_is_bot)
    result = json.loads(result.text)

    status = result['status']

    assert_that(status, Status.DONE.value)
    assert_that(
        transfer_variants[0]['segments'],
        is_not(contains(has_key('tariffs')))
    )

    query = TransferVariantsWithPricesQuerySchema(strict=True).load(request_is_bot.query).data
    query.pop('is_bot')
    query.pop('include_price_fee')
    handler._price_collector.collect.assert_not_called()


@pytest.mark.asyncio
async def test_transfers_with_prices_with_fee(cache, request_not_bot, transfer_variants_with_prices):  # noqa: F811
    cache_key = CacheType.MORDA_BACKEND, 'c39', 'c54', datetime.date(2019, 8, 20), 'ru', 'ru'
    await cache.set_cache(*cache_key, json.dumps(transfer_variants_with_prices, ensure_ascii=False))

    handler = HandlerFactory(cache=cache, train_fee_service=TrainFeeServiceStub(5))

    result = await handler.transfers_with_prices(request_not_bot)
    assert_that(
        json.loads(result.text)['transfer_variants'][0]['segments'][0],
        has_entry(
            'tariffs', has_entry(
                'classes', has_entry(
                    'compartment', has_entry(
                        'price', has_entry(
                            'value', equal_to(1005.1)
                        )
                    )
                )
            )
        )
    )
