import pytest


@pytest.fixture
def floyd_client(client_mocker):
    from mail.payments.payments.interactions.floyd import FloydClient
    yield client_mocker(FloydClient)
