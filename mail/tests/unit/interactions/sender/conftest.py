import pytest


@pytest.fixture
def sender_client(client_mocker):
    from mail.payments.payments.interactions.sender import SenderClient
    return client_mocker(SenderClient)
