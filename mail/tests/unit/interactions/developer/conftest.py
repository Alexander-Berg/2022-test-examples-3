import pytest


@pytest.fixture
def developer_client(client_mocker):
    from mail.payments.payments.interactions.developer import DeveloperClient
    return client_mocker(DeveloperClient)
