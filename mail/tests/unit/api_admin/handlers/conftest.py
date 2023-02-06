import pytest


@pytest.fixture
def run_action_result():
    return None


@pytest.fixture
def acting_manager(manager_admin):
    return manager_admin


@pytest.fixture
def tvm(base_tvm, acting_manager, tvm_client_id):
    base_tvm.src = tvm_client_id
    base_tvm.default_uid = acting_manager.uid
    return base_tvm


@pytest.fixture(autouse=True)
def tvm_mock(mocker, tvm):
    mocker.patch('mail.payments.payments.api.handlers.base.BaseHandler.tvm', tvm)


@pytest.fixture
async def admin_client(aiohttp_client, crypto_mock, payments_settings, admin_app):
    payments_settings.TVM_CHECK_SERVICE_TICKET = False
    return await aiohttp_client(admin_app)
