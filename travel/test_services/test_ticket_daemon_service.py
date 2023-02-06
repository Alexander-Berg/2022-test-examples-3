import pytest
from hamcrest import assert_that, contains, empty, has_entry, has_key, is_not

from travel.rasp.pathfinder_proxy.tests.utils import (  # noqa: F401
    cache, create_ticket_daemon_service_entry, create_ticket_daemon_collector_entry, transfer_variants,
    CollectorStubCreator
)
from travel.rasp.pathfinder_proxy.tests.utils.factories import TicketDaemonServiceFactory


@pytest.mark.asyncio
async def test_get_variants_with_tariffs(cache, transfer_variants):  # noqa: F811
    ticket_daemon_service = TicketDaemonServiceFactory(cache=cache)
    ticket_daemon_service.get_collector = CollectorStubCreator(
        [
            create_ticket_daemon_collector_entry(9866615, 9600216, '2019-08-20T18:30', 1),
            create_ticket_daemon_collector_entry(9600216, 9600370, '2019-08-21T07:00', 2)
        ],
        False
    )
    async for _ in ticket_daemon_service.iter_variants_with_tariffs(transfer_variants):
        pass

    assert_that(
        transfer_variants[1]['segments'],
        contains(
            has_entry('tariffs', create_ticket_daemon_service_entry(1)),
            has_entry('tariffs', create_ticket_daemon_service_entry(2))
        )
    )


@pytest.mark.asyncio
async def test_get_variants_with_tariffs_empty(cache):  # noqa: F811
    ticket_daemon_service = TicketDaemonServiceFactory(cache=cache)
    _transfer_variants = []
    async for _ in ticket_daemon_service.iter_variants_with_tariffs(_transfer_variants):
        pass

    assert_that(_transfer_variants, empty())


@pytest.mark.asyncio
async def test_get_variants_with_tariffs_partial(cache, transfer_variants):  # noqa: F811
    ticket_daemon_service = TicketDaemonServiceFactory(cache=cache)
    ticket_daemon_service.get_collector = CollectorStubCreator(
        [
            create_ticket_daemon_collector_entry(9866615, 9600216, '2019-08-20T18:30', 1),
            create_ticket_daemon_collector_entry(9600216, 9600370, '2019-08-21T05:00', 'wrong'),
            create_ticket_daemon_collector_entry(9600216, 9600370, '2019-08-21T09:00', 'wrong')
        ],
        False
    )
    async for _ in ticket_daemon_service.iter_variants_with_tariffs(transfer_variants):
        pass

    assert_that(
        transfer_variants[1]['segments'],
        contains(
            has_entry('tariffs', create_ticket_daemon_service_entry(1)),
            is_not(has_key('tariffs')),
        )
    )
