import pytest
from hamcrest import assert_that, empty, has_items

from travel.rasp.library.python.train_api_client.test_utils import (
    TrainApiClientStub, TrainApiClientExceptionStub, create_train_api_client_json
)
from travel.rasp.pathfinder_proxy.tests.utils import cache, create_train_api_collector_entry, transfer_variants  # noqa: F401
from travel.rasp.pathfinder_proxy.tests.utils.factories import TrainApiCollectorFactory


@pytest.mark.asyncio
async def test_get_tariffs_for_variants(cache, transfer_variants):  # noqa: F811
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

    train_api_collector = TrainApiCollectorFactory(client=train_api_client, cache=cache)
    tariff_infos, querying = await train_api_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is False
    assert_that(
        tariff_infos,
        has_items(
            create_train_api_collector_entry(9609235, 9605179, '2019-08-06T15:10', 1, 'train_1'),
            create_train_api_collector_entry(9605179, 9612913, '2019-08-08T07:50', 2, 'train_2')
        )
    )


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_empty(cache):  # noqa: F811
    train_api_collector = TrainApiCollectorFactory(client=TrainApiClientStub(), cache=cache)
    tariff_infos, querying = await train_api_collector.get_tariffs_for_variants([], 'ru', 'ru')

    assert querying is False
    assert_that(tariff_infos, empty())


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_with_poll(cache, transfer_variants):  # noqa: F811
    train_api_client = TrainApiClientStub(
        [
            ('s9609235', 's9605179', '2019-08-06T15:10', False, 'ru', 'ru'),
            ('s9605179', 's9612913', '2019-08-08T07:50', False, 'ru', 'ru'),
            ('s9605179', 's9612913', '2019-08-08T07:50', True, 'ru', 'ru')
        ],
        [
            create_train_api_client_json(False, [(9609235, 9605179, 1, '2019-08-06T15:10', '2019-08-08T07:20', 1)]),
            create_train_api_client_json(True, []),
            create_train_api_client_json(False, [(9605179, 9612913, 2, '2019-08-08T07:50', '2019-08-08T15:19', 2)])
        ]
    )
    train_api_collector = TrainApiCollectorFactory(client=train_api_client, cache=cache)
    tariff_infos, querying = await train_api_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is False
    assert_that(
        tariff_infos,
        has_items(
            create_train_api_collector_entry(9609235, 9605179, '2019-08-06T15:10', 1, 'train_1'),
            create_train_api_collector_entry(9605179, 9612913, '2019-08-08T07:50', 2, 'train_2')
        )
    )


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_partial(cache, transfer_variants):  # noqa: F811
    train_api_client = TrainApiClientStub(
        [
            ('s9609235', 's9605179', '2019-08-06T15:10', False, 'ru', 'ru'),
            ('s9605179', 's9612913', '2019-08-08T07:50', False, 'ru', 'ru')
        ],
        [
            create_train_api_client_json(False, [(9609235, 9605179, 1, '2019-08-06T15:10', '2019-08-08T07:20', 1)]),
            create_train_api_client_json(False, [
                (9605179, 9612913, 'wrong', '2019-08-08T05:50', '2019-08-08T15:19', 2),
                (9605179, 9612913, 'wrong too', '2019-08-08T12:50', '2019-08-08T15:19', 2)
            ]),
        ]
    )
    train_api_collector = TrainApiCollectorFactory(client=train_api_client, cache=cache)
    tariff_infos, querying = await train_api_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is False
    assert_that(
        tariff_infos,
        has_items(
            create_train_api_collector_entry(9609235, 9605179, '2019-08-06T15:10', 1, 'train_1'),
            create_train_api_collector_entry(9605179, 9612913, '2019-08-08T05:50', 'wrong', 'train_2'),
            create_train_api_collector_entry(9605179, 9612913, '2019-08-08T12:50', 'wrong too', 'train_2')
        )
    )


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_exception(cache, transfer_variants):  # noqa: F811
    train_api_client = TrainApiClientExceptionStub(
        [
            ('s9609235', 's9605179', '2019-08-06T15:10', False, 'ru', 'ru'),
            ('s9605179', 's9612913', '2019-08-08T07:50', True, 'ru', 'ru')
        ],
        [
            create_train_api_client_json(False, [(9609235, 9605179, 1, '2019-08-06T15:10', '2019-08-08T07:20', 1)]),
            create_train_api_client_json(False, [(9605179, 9612913, 2, '2019-08-08T07:50', '2019-08-08T15:19', 2)])
        ]
    )
    train_api_collector = TrainApiCollectorFactory(client=train_api_client, cache=cache)
    tariff_infos, querying = await train_api_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is False
    assert_that(
        tariff_infos,
        has_items(
            create_train_api_collector_entry(9609235, 9605179, '2019-08-06T15:10', 1, 'train_1'),
            create_train_api_collector_entry(9605179, 9612913, '2019-08-08T07:50', 2, 'train_2')
        )
    )


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_querying(cache, transfer_variants):  # noqa: F811
    train_api_client = TrainApiClientStub(
        [('s9609235', 's9605179', '2019-08-06T15:10', False, 'ru', 'ru')],
        [create_train_api_client_json(False, [(9609235, 9605179, 1, '2019-08-06T15:10', '2019-08-08T07:20', 1)])]
    )
    train_api_collector = TrainApiCollectorFactory(client=train_api_client, cache=cache)
    train_api_collector._MAX_ATTEMPTS = 1
    tariff_infos, querying = await train_api_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is True
    assert_that(
        tariff_infos,
        has_items(create_train_api_collector_entry(9609235, 9605179, '2019-08-06T15:10', 1, 'train_1'))
    )
