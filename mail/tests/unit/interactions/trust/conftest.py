import pytest

from mail.payments.payments.utils.helpers import temp_setattr


@pytest.fixture
def trust_client(client_mocker):
    from mail.payments.payments.interactions.trust import TrustProductionClient
    client = client_mocker(TrustProductionClient)
    dummy_make_request = client._make_request

    # Enforcing make_request signature
    async def make_request(*args, uid, acquirer, **kwargs):
        return await dummy_make_request(*args, uid=uid, acquirer=acquirer, **kwargs)

    client._make_request = make_request
    return client


@pytest.fixture
def trust_call(trust_client):
    call = trust_client.calls[0]

    return {
        'method': call[0][1],
        'url': call[0][2],
        'headers': call[1].get('headers'),
        'json': call[1].get('json'),
        'uid': call[1].get('uid'),
        'acquirer': call[1].get('acquirer'),
    }


@pytest.fixture
def uid(randn):
    return randn()


@pytest.fixture
def acquirer_token(rands):
    return rands()


@pytest.fixture(autouse=True)
def setup_acquirer_token(payments_settings, acquirer, acquirer_token):
    with temp_setattr(payments_settings, 'TRUST_SERVICE_TOKEN', {acquirer.value: acquirer_token}):
        yield
