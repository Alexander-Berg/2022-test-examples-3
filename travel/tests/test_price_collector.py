import asyncio
import json

import pytest
from hamcrest import assert_that, contains, has_entry

from travel.rasp.library.python.ticket_daemon_client.test_utils import (
    create_ticket_daemon_client_json, TicketDaemonClientStub
)
from travel.rasp.library.python.train_api_client.test_utils import create_train_api_client_json, TrainApiClientStub
from travel.rasp.pathfinder_proxy.const import CacheType, Status
from travel.rasp.pathfinder_proxy.serialization import TransferVariantsWithPricesQuerySchema
from travel.rasp.pathfinder_proxy.services.price_collector import PriceCollector, merge_async_iterators
from travel.rasp.pathfinder_proxy.tests.utils import (  # noqa: F401
    cache, request_not_bot, settings, transfer_variants, create_train_api_service_entry, create_ticket_daemon_service_entry
)


@pytest.mark.asyncio
async def test_request_prices_for_transfer_variants(cache, request_not_bot, transfer_variants, settings):  # noqa: F811
    train_api_client = TrainApiClientStub(
        [
            ('s9609235', 's9605179', '2019-08-06T15:10', False, 'ru', 'ru'),
            ('s9605179', 's9612913', '2019-08-08T07:50', False, 'ru', 'ru')
        ],
        [
            create_train_api_client_json(False, [(9609235, 9605179, 1, '2019-08-06T15:10', '2019-08-08T07:20', 1)]),
            create_train_api_client_json(False, [(9605179, 9612913, 2, '2019-08-08T07:50', '2019-08-08T15:19', 2)])
        ]
    )

    ticket_daemon_client = TicketDaemonClientStub(
        [
            ('s9866615', 's9600216', '2019-08-20T18:30', False, 'ru', 'ru'),
            ('s9600216', 's9600370', '2019-08-21T07:00', False, 'ru', 'ru'),
            ('s9866615', 's9600370', '2019-08-20T18:30', False, 'ru', 'ru')
        ],
        [
            create_ticket_daemon_client_json(
                False,
                {'test_itinerary_1': ['test_key_1']},
                {'test_itinerary_1': 1},
                {'test_key_1': (9866615, 9600216, '2019-08-20 18:30', '2019-08-20 20:15', 1)}
            ),
            create_ticket_daemon_client_json(
                False,
                {'test_itinerary_2': ['test_key_2']},
                {'test_itinerary_2': 2},
                {'test_key_2': (9600216, 9600370, '2019-08-21 07:00', '2019-08-21 09:20', 2)}
            ),
            create_ticket_daemon_client_json(
                False,
                {'test_itinerary_1': ['test_key_11', 'test_key_12']},
                {'test_itinerary_1': 1},
                {
                    'test_key_11': (9866615, 9600216, '2019-08-20 18:30', '2019-08-20 20:15', 1),
                    'test_key_12': (9600216, 9600370, '2019-08-21 07:00', '2019-08-21 09:20', 2)
                }
            )
        ])

    price_collector = PriceCollector(cache, train_api_client, ticket_daemon_client, settings=settings)
    query = TransferVariantsWithPricesQuerySchema(strict=True).load(request_not_bot.query).data

    await price_collector._request_prices_for_transfer_variants(transfer_variants, query)
    result = await cache.get_from_cache(
        CacheType.TRANSFERS_WITH_PRICES,
        query['point_from'],
        query['point_to'],
        query['when'],
        query['tld'],
        query['language']
    )
    result = json.loads(result)
    transfer_variants = result['transfer_variants']
    status = result['status']

    assert_that(status, Status.DONE.value)
    assert_that(
        transfer_variants[0]['segments'],
        contains(
            has_entry('tariffs', create_train_api_service_entry(1)),
            has_entry('tariffs', create_train_api_service_entry(2)),
        )
    )
    assert_that(
        transfer_variants[1]['segments'],
        contains(
            has_entry('tariffs', create_ticket_daemon_service_entry(1)),
            has_entry('tariffs', create_ticket_daemon_service_entry(2))
        )
    )
    assert_that(
        transfer_variants[1],
        has_entry('tariffs', create_ticket_daemon_service_entry(1)),
    )


@pytest.mark.asyncio
async def test_merge_async_iterators():
    async def iter_1():
        yield 'iter_1', 1
        await asyncio.sleep(0.2)
        yield 'iter_1', 2

    async def iter_2():
        await asyncio.sleep(0.1)
        yield 'iter_2', 1
        await asyncio.sleep(0.2)
        yield 'iter_2', 2

    result = []
    async for r in merge_async_iterators([iter_1(), iter_2()]):
        result.append(r)

    assert result == [
        ('iter_1', 1),
        ('iter_2', 1),
        ('iter_1', 2),
        ('iter_2', 2),
    ]
