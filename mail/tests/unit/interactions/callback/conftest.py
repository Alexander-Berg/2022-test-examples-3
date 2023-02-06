import pytest


@pytest.fixture
def callback_client(client_mocker):
    from mail.payments.payments.interactions.callback import CallbackClient
    return client_mocker(CallbackClient)
