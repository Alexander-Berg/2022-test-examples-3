import pytest

from travel.rasp.library.python.ticket_daemon_client.test_utils import (
    TicketDaemonClientStub, TicketDaemonClientExceptionStub, create_ticket_daemon_client_json
)
from travel.rasp.pathfinder_maps.models.route import Route
from travel.rasp.pathfinder_maps.services.polling_services.ticket_daemon_service import TicketDaemonService


@pytest.fixture
def test_route():
    return Route.from_dict({
        "thread_id": "route1",
        "departure_datetime": "2019-08-06T15:10:00",
        "departure_station_id": 9609235,
        "arrival_datetime": "2019-08-06T17:10:00",
        "arrival_station_id": 9605179,
        "thread_info": ["route1", "route1", 1, [9609235, 9605179], ""]
    })


@pytest.mark.asyncio
async def test_get_price(test_route):
    ticket_daemon_client = TicketDaemonClientStub(
        [
            ('s9609235', 's9605179', '2019-08-06T15:10', True, 'ru', 'ru'),
            ('s9609235', 's9605179', '2019-08-06T15:10', False, 'ru', 'ru')
        ],
        [
            create_ticket_daemon_client_json(
                False,
                {'test_itinerary_1': ['test_key_11']},
                {'test_itinerary_1': 1},
                {'test_key_11': (9609235, 9605179, '2019-08-06 15:10', '2019-08-06 17:10', 1)}
            ),
            create_ticket_daemon_client_json(True, {}, {}, {})
        ]
    )

    ticket_daemon_service = TicketDaemonService(ticket_daemon_client)
    polling_answer = await ticket_daemon_service.get_price(test_route, False)
    assert polling_answer.price is None
    assert polling_answer.order_url is None
    assert polling_answer.is_polling is True

    polling_answer = await ticket_daemon_service.get_price(test_route, True)
    assert polling_answer.price == 1
    assert polling_answer.order_url == 'https://travel.yandex.ru/avia/order/?lang=ru'
    assert polling_answer.is_polling is False


@pytest.mark.asyncio
async def test_get_price_exception(test_route):
    ticket_daemon_client = TicketDaemonClientExceptionStub([], [])
    ticket_daemon_service = TicketDaemonService(ticket_daemon_client)
    polling_answer = await ticket_daemon_service.get_price(test_route, True)
    assert polling_answer.price is None
    assert polling_answer.order_url is None
    assert polling_answer.is_polling is False
