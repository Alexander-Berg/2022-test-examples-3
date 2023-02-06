import asyncio
import pytest


def pytest_collection_modifyitems(items):
    for item in items:
        item.add_marker(pytest.mark.asyncio)


@pytest.fixture(scope='session', autouse=True)
def event_loop():
    yield asyncio.get_event_loop()
