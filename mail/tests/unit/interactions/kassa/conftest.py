import pytest


@pytest.fixture
def kassa_client(client_mocker):
    from mail.payments.payments.interactions.kassa import KassaClient
    yield client_mocker(KassaClient)
