import pytest

from mail.payments.payments.core.entities.enums import AcquirerType
from mail.payments.payments.interactions.base import AbstractInteractionClient
from mail.payments.payments.interactions.trust.base import BaseTrustClient
from mail.payments.payments.tests.utils import dummy_async_function
from mail.payments.payments.utils.helpers import temp_setattr


@pytest.fixture
def acquirer_service_tokens(rands):
    return {
        acquirer_type.value: rands()
        for acquirer_type in AcquirerType
    }


@pytest.fixture
def merchant_service_token(rands):
    return rands()


@pytest.fixture
def set_acquirer_service_tokens(payments_settings, acquirer_service_tokens):
    with temp_setattr(
        payments_settings,
        'TRUST_SERVICE_TOKEN',
        acquirer_service_tokens,
    ):
        yield


@pytest.fixture
def set_merchant_service_token(uid, merchant_service_token, payments_settings):
    with temp_setattr(
        payments_settings,
        'INTERACTION_MERCHANT_SETTINGS',
        {uid: {'trust_service_token': merchant_service_token}},
    ):
        yield


@pytest.fixture
def test_base_client(loop, test_logger, mocker, rands):
    mock = mocker.patch.object(
        AbstractInteractionClient,
        '_make_request',
        side_effect=dummy_async_function(result=None),
    )

    class Client(BaseTrustClient):
        SERVICE = 'test_service'

    client = Client(request_id='test', logger=test_logger)
    client.request_mock = mock
    return client


@pytest.fixture
def returned_func(test_base_client):
    async def _inner(**kwargs):
        return await test_base_client._make_request('interaction_method_name', 'post', 'url', **kwargs)

    return _inner


@pytest.fixture
async def returned(returned_func, acquirer, uid):
    return await returned_func(acquirer=acquirer, uid=uid)


@pytest.fixture
def used_service_token(test_base_client):
    return test_base_client.request_mock.call_args[1]['headers']['X-Service-Token']


@pytest.mark.parametrize('kwargs', (
    pytest.param({}, id='empty_kwargs'),
    pytest.param({'uid': 1}, id='missing_acquirer'),
    pytest.param({'acquirer': 1}, id='missing_uid'),
))
@pytest.mark.asyncio
async def test_request_requires_uid(returned_func, kwargs):
    with pytest.raises(TypeError):
        await returned_func(**kwargs)


def test_service_token_depends_on_acquirer(acquirer_service_tokens,
                                           set_acquirer_service_tokens,
                                           acquirer,
                                           returned,
                                           used_service_token,
                                           ):
    assert used_service_token == acquirer_service_tokens[acquirer.value]


def test_service_token_depends_on_uid(merchant_service_token,
                                      set_acquirer_service_tokens,
                                      set_merchant_service_token,
                                      returned,
                                      used_service_token,
                                      ):
    assert used_service_token == merchant_service_token
