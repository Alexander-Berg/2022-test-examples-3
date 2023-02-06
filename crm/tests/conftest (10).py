import pytest


pytest_plugins = ['crm.agency_cabinet.common.server.common.pytest.plugin']


def pytest_collection_modifyitems(items):
    for item in items:
        item.add_marker(pytest.mark.asyncio)
