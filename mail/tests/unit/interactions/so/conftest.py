import pytest


@pytest.fixture
def so_client(client_mocker):
    from mail.payments.payments.interactions.so import SoClient
    yield client_mocker(SoClient)
