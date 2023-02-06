import pytest


@pytest.fixture
def tinkoff_client(client_mocker):
    from mail.payments.payments.interactions.tinkoff import TinkoffClient
    yield client_mocker(TinkoffClient)
    TinkoffClient._token_cache.clear()
