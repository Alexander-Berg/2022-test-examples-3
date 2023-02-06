import pytest

from travel.rasp.library.python.train_api_client.test_utils import (TrainApiClientStub, TrainApiClientExceptionStub,
                                                                    create_train_api_client_json)
from travel.rasp.pathfinder_maps.models.route import Route
from travel.rasp.pathfinder_maps.services.polling_services.train_api_service import TrainApiService

TEST_TRAVEL_PREFIX = 'test-service'


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
    train_api_client = TrainApiClientStub(
        [
            ('s9609235', 's9605179', '2019-08-06T15:10', False, 'ru', 'ru'),
            ('s9609235', 's9605179', '2019-08-06T15:10', True, 'ru', 'ru')
        ],
        [
            create_train_api_client_json(True, []),
            create_train_api_client_json(False, [(9609235, 9605179, 1, '2019-08-06T15:10', '2019-08-08T07:20', 1)])
        ]
    )
    train_api_service = TrainApiService(train_api_client, TEST_TRAVEL_PREFIX)
    polling_answer = await train_api_service.get_price(test_route, False)
    assert polling_answer.price is None
    assert polling_answer.order_url is None
    assert polling_answer.is_polling is True

    polling_answer = await train_api_service.get_price(test_route, True)
    assert polling_answer.price == 1
    assert polling_answer.order_url == f'{TEST_TRAVEL_PREFIX}/'
    assert polling_answer.is_polling is False


@pytest.mark.asyncio
async def test_get_price_exception(test_route):
    train_api_client = TrainApiClientExceptionStub([], [])
    train_api_service = TrainApiService(train_api_client, TEST_TRAVEL_PREFIX)
    polling_answer = await train_api_service.get_price(test_route, True)
    assert polling_answer.price is None
    assert polling_answer.order_url is None
    assert polling_answer.is_polling is False
