import pytest
from hamcrest import assert_that, empty, has_items

from travel.rasp.library.python.ticket_daemon_client.test_utils import (
    TicketDaemonClientStub, TicketDaemonClientExceptionStub, create_ticket_daemon_client_json
)
from travel.rasp.pathfinder_proxy.tests.utils.factories import InterlineCollectorFactory
from travel.rasp.pathfinder_proxy.tests.utils import cache, transfer_variants, create_interline_collector_entry  # noqa: F401


@pytest.mark.asyncio
async def test_get_tariffs_for_variants(cache, transfer_variants):  # noqa: F811
    ticket_daemon_client = TicketDaemonClientStub(
        [('s9866615', 's9600370', '2019-08-20T18:30', False, 'ru', 'ru')],
        [
            create_ticket_daemon_client_json(
                False,
                {'test_itinerary_1': ['test_key_11', 'test_key_12']},
                {'test_itinerary_1': 1},
                {
                    'test_key_11': (9866615, 9600216, '2019-08-20 18:30', '2019-08-20 20:15', 1),
                    'test_key_12': (9600216, 9600370, '2019-08-21 07:00', '2019-08-21 09:20', 2)
                }
            )
        ]
    )
    interline_collector = InterlineCollectorFactory(client=ticket_daemon_client, cache=cache)
    tariff_infos, querying = await interline_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is False
    assert_that(
        tariff_infos,
        has_items(
            create_interline_collector_entry(
                [(9866615, 9600216, '2019-08-20T18:30', 1), (9600216, 9600370, '2019-08-21T07:00', 2)], 1
            )
        )
    )


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_empty(cache):  # noqa: F811
    interline_collector = InterlineCollectorFactory(client=TicketDaemonClientStub(), cache=cache)
    tariff_infos, querying = await interline_collector.get_tariffs_for_variants([], 'ru', 'ru')

    assert querying is False
    assert_that(tariff_infos, empty())


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_with_poll(cache, transfer_variants):  # noqa: F811
    ticket_daemon_client = TicketDaemonClientStub(
        [
            ('s9866615', 's9600370', '2019-08-20T18:30', False, 'ru', 'ru'),
            ('s9866615', 's9600370', '2019-08-20T18:30', True, 'ru', 'ru'),
        ],
        [
            create_ticket_daemon_client_json(True, {}, {}, {}),
            create_ticket_daemon_client_json(
                False,
                {'test_itinerary_1': ['test_key_11', 'test_key_12']},
                {'test_itinerary_1': 1},
                {
                    'test_key_11': (9866615, 9600216, '2019-08-20 18:30', '2019-08-20 20:15', 1),
                    'test_key_12': (9600216, 9600370, '2019-08-21 07:00', '2019-08-21 09:20', 2)
                }
            )
        ]
    )
    interline_collector = InterlineCollectorFactory(client=ticket_daemon_client, cache=cache)
    tariff_infos, querying = await interline_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is False
    assert_that(
        tariff_infos,
        has_items(
            create_interline_collector_entry(
                [(9866615, 9600216, '2019-08-20T18:30', 1), (9600216, 9600370, '2019-08-21T07:00', 2)], 1
            )
        )
    )


@pytest.mark.asyncio
async def test_get_variants_exception(cache, transfer_variants):  # noqa: F811
    ticket_daemon_client = TicketDaemonClientExceptionStub(
        [('s9866615', 's9600370', '2019-08-20T18:30', True, 'ru', 'ru')],
        [
            create_ticket_daemon_client_json(
                False,
                {'test_itinerary_1': ['test_key_11', 'test_key_12']},
                {'test_itinerary_1': 1},
                {
                    'test_key_11': (9866615, 9600216, '2019-08-20 18:30', '2019-08-20 20:15', 1),
                    'test_key_12': (9600216, 9600370, '2019-08-21 07:00', '2019-08-21 09:20', 2)
                }
            )
        ]
    )
    interline_collector = InterlineCollectorFactory(client=ticket_daemon_client, cache=cache)
    tariff_infos, querying = await interline_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is False
    assert_that(
        tariff_infos,
        has_items(
            create_interline_collector_entry(
                [(9866615, 9600216, '2019-08-20T18:30', 1), (9600216, 9600370, '2019-08-21T07:00', 2)], 1
            )
        )
    )


@pytest.mark.asyncio
async def test_get_tariffs_for_variants_querying(cache, transfer_variants):  # noqa: F811
    ticket_daemon_client = TicketDaemonClientStub(
        [('s9866615', 's9600370', '2019-08-20T18:30', False, 'ru', 'ru')],
        [create_ticket_daemon_client_json(True, {}, {}, {})]
    )
    interline_collector = InterlineCollectorFactory(client=ticket_daemon_client, cache=cache)
    interline_collector._MAX_ATTEMPTS = 1
    tariff_infos, querying = await interline_collector.get_tariffs_for_variants(transfer_variants, 'ru', 'ru')

    assert querying is True
    assert_that(tariff_infos, empty())
