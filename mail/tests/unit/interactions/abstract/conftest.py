import pytest


@pytest.fixture
def abstract_client(client_mocker, rands):
    from mail.payments.payments.interactions.base import AbstractInteractionClient

    class TestAbstractInteractionClient(AbstractInteractionClient):
        SERVICE = rands()
        BASE_URL = rands()

    return client_mocker(TestAbstractInteractionClient)
