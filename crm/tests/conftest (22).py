import pytest
import datetime
from crm.supskills.common.bunker_client.bunker import Bunker


def pytest_collection_modifyitems(items):
    for item in items:
        item.add_marker(pytest.mark.asyncio)


@pytest.fixture
def bunker_empty():
    bunker = Bunker('testing', datetime.timedelta(0, 0, 0, 0, 5))
    bunker.cache = {}
    return bunker


@pytest.fixture
def bunker_last_check_in_past():
    bunker = Bunker('testing', datetime.timedelta(0, 0, 0, 0, 5))
    bunker.cache = {'new/path': {
        'last_check': datetime.datetime(1111, 1, 1, 1, 1, 1),
        'data': {'NODE_KEY': 'node_old_text'}}}
    return bunker


@pytest.fixture
def bunker_last_check_now():
    bunker = Bunker('testing', datetime.timedelta(0, 0, 0, 0, 5))

    bunker.cache = {'new/path': {
        'last_check': datetime.datetime.now(),
        'data': {'NODE_KEY': 'node_old_text'}}}
    return bunker
