import pytest
from hamcrest import assert_that, has_items, empty

from travel.rasp.library.python.ticket_daemon_client.test_utils import (
    TicketDaemonClientStub, TicketDaemonClientExceptionStub, create_ticket_daemon_client_json
)
from travel.rasp.pathfinder_proxy.tests.utils import cache, transfer_variants, create_ticket_daemon_collector_entry  # noqa: F401
from travel.rasp.pathfinder_proxy.tests.utils.factories import TicketDaemonCollectorFactory


@pytest.mark.asyncio
async def test_get_tariffs_for_variants(cache, transfer_variants):  # noqa: F811
    ticket_daemon_client = TicketDaemonClientStub(
        [
            ('s9866615', 's9600216', '2019-08-20T18:30', False, 'ru', 'ru'),
            ('s9600216', 's9600370', '2019-08-21T07:00', False, 'ru', 'ru')
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
            )
        ]
    )
    ticket_daemon_collector = TicketDaemonCollectorFactory(client=ticket_daemon_client, cache=cache)
    tariff_infos, querying = await ticket_daemon_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is False
    assert_that(
        tariff_infos,
        has_items(
            create_ticket_daemon_collector_entry(9866615, 9600216, '2019-08-20T18:30', 1),
            create_ticket_daemon_collector_entry(9600216, 9600370, '2019-08-21T07:00', 2)
        )
    )


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_empty(cache):  # noqa: F811
    ticket_daemon_collector = TicketDaemonCollectorFactory(client=TicketDaemonClientStub(), cache=cache)
    tariff_infos, querying = await ticket_daemon_collector.get_tariffs_for_variants([], 'ru', 'ru')

    assert querying is False
    assert_that(tariff_infos, empty())


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_with_poll(cache, transfer_variants):  # noqa: F811
    ticket_daemon_client = TicketDaemonClientStub(
        [
            ('s9866615', 's9600216', '2019-08-20T18:30', False, 'ru', 'ru'),
            ('s9600216', 's9600370', '2019-08-21T07:00', False, 'ru', 'ru'),
            ('s9600216', 's9600370', '2019-08-21T07:00', True, 'ru', 'ru')
        ],
        [
            create_ticket_daemon_client_json(
                False,
                {'test_itinerary_1': ['test_key_1']},
                {'test_itinerary_1': 1},
                {'test_key_1': (9866615, 9600216, '2019-08-20 18:30', '2019-08-20 20:15', 1)}
            ),
            create_ticket_daemon_client_json(True, {}, {}, {}),
            create_ticket_daemon_client_json(
                False,
                {'test_itinerary_2': ['test_key_2']},
                {'test_itinerary_2': 2},
                {'test_key_2': (9600216, 9600370, '2019-08-21 07:00', '2019-08-21 09:20', 2)}
            )
        ]
    )
    ticket_daemon_collector = TicketDaemonCollectorFactory(client=ticket_daemon_client, cache=cache)
    tariff_infos, querying = await ticket_daemon_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is False
    assert_that(
        tariff_infos,
        has_items(
            create_ticket_daemon_collector_entry(9866615, 9600216, '2019-08-20T18:30', 1),
            create_ticket_daemon_collector_entry(9600216, 9600370, '2019-08-21T07:00', 2)
        )
    )


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_partial(cache, transfer_variants):  # noqa: F811
    ticket_daemon_client = TicketDaemonClientStub(
        [
            ('s9866615', 's9600216', '2019-08-20T18:30', False, 'ru', 'ru'),
            ('s9600216', 's9600370', '2019-08-21T07:00', False, 'ru', 'ru')
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
                {'test_itinerary_2': ['test_key_2', 'test_key_3']},
                {'test_itinerary_2': 'wrong'},
                {
                    'test_key_2': (9600216, 9600370, '2019-08-21 05:00', '2019-08-21 09:20', 2),
                    'test_key_3': (9600216, 9600370, '2019-08-21 09:00', '2019-08-21 09:20', 2)
                }
            )
        ]
    )
    ticket_daemon_collector = TicketDaemonCollectorFactory(client=ticket_daemon_client, cache=cache)
    tariff_infos, querying = await ticket_daemon_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is False
    assert_that(
        tariff_infos,
        has_items(
            create_ticket_daemon_collector_entry(9866615, 9600216, '2019-08-20T18:30', 1),
            create_ticket_daemon_collector_entry(9600216, 9600370, '2019-08-21T05:00', 'wrong'),
            create_ticket_daemon_collector_entry(9600216, 9600370, '2019-08-21T09:00', 'wrong')
        )
    )


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_exception(cache, transfer_variants):  # noqa: F811
    ticket_daemon_client = TicketDaemonClientExceptionStub(
        [
            ('s9866615', 's9600216', '2019-08-20T18:30', False, 'ru', 'ru'),
            ('s9600216', 's9600370', '2019-08-21T07:00', True, 'ru', 'ru')
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
            )
        ]
    )

    ticket_daemon_collector = TicketDaemonCollectorFactory(client=ticket_daemon_client, cache=cache)
    tariff_infos, querying = await ticket_daemon_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is False
    assert_that(
        tariff_infos,
        has_items(
            create_ticket_daemon_collector_entry(9866615, 9600216, '2019-08-20T18:30', 1),
            create_ticket_daemon_collector_entry(9600216, 9600370, '2019-08-21T07:00', 2)
        )
    )


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_querying(cache, transfer_variants):  # noqa: F811
    ticket_daemon_client = TicketDaemonClientExceptionStub(
        [
            ('s9866615', 's9600216', '2019-08-20T18:30', False, 'ru', 'ru'),
            ('s9600216', 's9600370', '2019-08-21T07:00', True, 'ru', 'ru')
        ],
        [
            create_ticket_daemon_client_json(
                False,
                {'test_itinerary_1': ['test_key_1']},
                {'test_itinerary_1': 1},
                {'test_key_1': (9866615, 9600216, '2019-08-20 18:30', '2019-08-20 20:15', 1)}
            ),
            create_ticket_daemon_client_json(
                True,
                {'test_itinerary_2': ['test_key_2']},
                {'test_itinerary_2': 2},
                {'test_key_2': (9600216, 9600370, '2019-08-21 07:00', '2019-08-21 09:20', 2)}
            )
        ]
    )
    ticket_daemon_collector = TicketDaemonCollectorFactory(client=ticket_daemon_client, cache=cache)
    ticket_daemon_collector._MAX_ATTEMPTS = 1
    tariff_infos, querying = await ticket_daemon_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is True
    assert_that(
        tariff_infos,
        has_items(
            create_ticket_daemon_collector_entry(9866615, 9600216, '2019-08-20T18:30', 1)
        )
    )
