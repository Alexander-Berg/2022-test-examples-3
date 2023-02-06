import pytest


@pytest.fixture
def geobase_client(client_mocker):
    from mail.payments.payments.interactions.geobase import GeobaseClient
    yield client_mocker(GeobaseClient)
