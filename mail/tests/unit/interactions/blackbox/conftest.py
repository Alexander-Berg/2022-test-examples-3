import pytest


@pytest.fixture
def blackbox_client(client_mocker):
    from mail.payments.payments.interactions import BlackBoxClient
    return client_mocker(BlackBoxClient)
