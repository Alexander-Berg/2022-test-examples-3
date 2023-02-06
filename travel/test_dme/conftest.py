import pytest

from travel.avia.flight_status_fetcher.sources.dme import DMEImporter


@pytest.fixture()
def importer():
    return DMEImporter
