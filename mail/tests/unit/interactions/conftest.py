import pytest

from mail.payments.payments.tests.utils import dummy_async_function


@pytest.fixture
async def client_mocker(loop, mocker, test_logger, response_mock, pushers_mock):
    def _inner(cls, **kwargs):
        """Mocks _make_request async method on Client class"""
        calls = []
        cls.calls = property(lambda self: calls)
        cls.call_args = property(lambda self: calls[0][0])
        cls.call_kwargs = property(lambda self: calls[0][1])
        client = cls(logger=test_logger, request_id='test', pushers=pushers_mock, **kwargs)
        mocker.patch.object(client, '_make_request', dummy_async_function(response_mock, calls=calls))
        mocker.spy(client, '_make_request')
        return client

    return _inner
