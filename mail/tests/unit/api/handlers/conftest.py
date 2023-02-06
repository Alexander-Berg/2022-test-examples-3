import pytest


@pytest.fixture
def run_action_result():
    return None


@pytest.fixture
def tvm(base_tvm, merchant, tvm_client_id):
    base_tvm.src = tvm_client_id
    base_tvm.default_uid = merchant.uid if merchant else None
    return base_tvm


@pytest.fixture(autouse=True)
def tvm_mock(mocker, tvm):
    mocker.patch('mail.payments.payments.api.handlers.base.BaseHandler.tvm', tvm)


@pytest.fixture
def context(run_action_calls):
    return run_action_calls[0][1]


@pytest.fixture
async def payments_client(aiohttp_client, payments_app, db_engine, crypto_mock, payments_settings):
    return await aiohttp_client(payments_app)
