import pytest


@pytest.fixture
def oauth_client(client_mocker):
    from mail.payments.payments.interactions.oauth import OAuthClient
    yield client_mocker(OAuthClient)
