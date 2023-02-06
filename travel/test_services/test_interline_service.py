import pytest
from hamcrest import assert_that, contains, has_entry, empty, is_not, has_key

from travel.rasp.pathfinder_proxy.tests.utils import (  # noqa: F401
    cache, create_interline_collector_entry, create_ticket_daemon_service_entry, CollectorStubCreator, transfer_variants
)
from travel.rasp.pathfinder_proxy.tests.utils.factories import InterlineServiceFactory


@pytest.mark.asyncio
async def test_get_variants_with_tariffs(cache, transfer_variants):  # noqa: F811
    interline_service = InterlineServiceFactory(cache=cache)
    interline_service.get_collector = CollectorStubCreator(
        [
            create_interline_collector_entry(
                [(9866615, 9600216, '2019-08-20T18:30', 1), (9600216, 9600370, '2019-08-21T07:00', 2)], 1
            )
        ],
        False
    )
    async for _ in interline_service.iter_variants_with_tariffs(transfer_variants):
        pass

    assert_that(
        transfer_variants[1],
        has_entry('tariffs', create_ticket_daemon_service_entry(1)),
    )


@pytest.mark.asyncio
async def test_get_variants_with_tariffs_empty(cache):  # noqa: F811
    interline_service = InterlineServiceFactory(cache=cache)
    _transfer_variants = []
    async for _ in interline_service.iter_variants_with_tariffs(_transfer_variants):
        pass

    assert_that(_transfer_variants, empty())


@pytest.mark.asyncio
async def test_get_variants_with_tariffs_not_found(cache, transfer_variants):  # noqa: F811
    interline_service = InterlineServiceFactory(cache=cache)
    interline_service.get_collector = CollectorStubCreator(
        [
            create_interline_collector_entry(
                [(9866615, 9600216, '2019-08-20T18:30', 1), (9600216, 9600370, '2019-08-21T05:00', 2)], 1
            )
        ],
        False
    )
    async for _ in interline_service.iter_variants_with_tariffs(transfer_variants):
        pass

    assert_that(
        transfer_variants[1],
        contains(is_not(has_key('tariffs')))
    )
