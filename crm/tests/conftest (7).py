import pytest

from crm.agency_cabinet.common.bunker import BunkerClient


@pytest.fixture
def bunker_host():
    return 'bunker-api-dot.yandex.net'


@pytest.fixture
def bunker_project():
    return 'agency-cabinet'


@pytest.fixture
def bunker_client(bunker_host, bunker_project):
    return BunkerClient(project=bunker_project, host=bunker_host)
