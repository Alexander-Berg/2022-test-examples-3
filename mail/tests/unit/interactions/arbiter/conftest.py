import pytest


@pytest.fixture
def arbiter_client(client_mocker, rands):
    from mail.payments.payments.interactions.arbiter import ArbiterClient
    yield client_mocker(ArbiterClient)
