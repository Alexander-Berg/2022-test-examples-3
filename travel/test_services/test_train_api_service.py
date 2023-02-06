import pytest
from hamcrest import assert_that, contains, empty, has_entry, has_entries, has_key, is_not

from travel.rasp.pathfinder_proxy.settings import Settings
from travel.rasp.pathfinder_proxy.tests.utils import (  # noqa: F401
    cache, create_train_api_collector_entry, create_train_api_service_entry, suburban_transfer_variants,
    transfer_variants, transfer_variants_mixed, CollectorStubCreator
)
from travel.rasp.pathfinder_proxy.tests.utils.factories import TrainApiServiceFactory


@pytest.mark.asyncio
async def test_get_variants_with_tariffs(cache, transfer_variants):  # noqa: F811
    train_api_service = TrainApiServiceFactory(cache=cache)
    train_api_service.get_collector = CollectorStubCreator(
        [
            create_train_api_collector_entry(9609235, 9605179, '2019-08-06T15:10', 1),
            create_train_api_collector_entry(9605179, 9612913, '2019-08-08T07:50', 2)
        ],
        False
    )
    async for _ in train_api_service.iter_variants_with_tariffs(transfer_variants):
        pass

    assert_that(
        transfer_variants[0]['segments'],
        contains(
            has_entry('tariffs', create_train_api_service_entry(1)),
            has_entry('tariffs', create_train_api_service_entry(2)),
        )
    )


@pytest.mark.asyncio
async def test_get_variants_with_tariffs_empty(cache):  # noqa: F811
    train_api_service = TrainApiServiceFactory(cache=cache)
    _transfer_variants = []
    async for _ in train_api_service.iter_variants_with_tariffs(_transfer_variants):
        pass

    assert_that(_transfer_variants, empty())


@pytest.mark.asyncio
async def test_get_variants_with_tariffs_partial(cache, transfer_variants_mixed):  # noqa: F811
    train_api_service = TrainApiServiceFactory(cache=cache)
    train_api_service.get_collector = CollectorStubCreator(
        [
            create_train_api_collector_entry(9609235, 9605179, '2019-08-06T15:10', 1),
            create_train_api_collector_entry(9605179, 9612913, '2019-08-08T05:50', 'wrong'),
            create_train_api_collector_entry(9605179, 9612913, '2019-08-08T12:50', 'wrong too')
        ],
        False
    )
    async for _ in train_api_service.iter_variants_with_tariffs(transfer_variants_mixed):
        pass

    assert_that(
        transfer_variants_mixed[0]['segments'],
        contains(
            has_entry('tariffs', create_train_api_service_entry(1)),
            is_not(has_key('tariffs'))
        )
    )


@pytest.mark.asyncio
async def test_get_variants_with_tariffs_suburban(cache, suburban_transfer_variants):  # noqa: F811
    settings = Settings()
    settings.FRONT_SUBURBAN_TARIFFS = True

    train_api_service = TrainApiServiceFactory(cache=cache, settings=settings)
    train_api_service.get_collector = CollectorStubCreator(
        [
            create_train_api_collector_entry(9609235, 9605179, '2019-08-06T15:10', 1, 'train_1'),
            create_train_api_collector_entry(9605179, 9612913, '2019-08-08T07:50', 2, 'train_2')
        ],
        False
    )
    async for _ in train_api_service.iter_variants_with_tariffs(suburban_transfer_variants):
        pass

    assert_that(
        suburban_transfer_variants[0]['segments'],
        contains(
            has_entries({
                'tariffs': create_train_api_service_entry(1),
                'hasTrainTariffs': True
            }),
            is_not(has_key('tariffs'))
        )
    )
