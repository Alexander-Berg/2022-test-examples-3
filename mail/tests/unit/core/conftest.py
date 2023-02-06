import pytest

from mail.payments.payments.core.actions.base.action import BaseAction
from mail.payments.payments.core.actions.base.merchant import BaseMerchantAction


@pytest.fixture
def request_id(rands):
    return rands()


@pytest.fixture(autouse=True)
def action_context_setup(test_logger, db_engine, pushers_mock, crypto_mock, partner_crypto_mock, request_id):
    BaseAction.context.logger = test_logger
    BaseAction.context.request_id = request_id
    BaseAction.context.db_engine = db_engine
    BaseAction.context.pushers = pushers_mock
    BaseAction.context.crypto = crypto_mock
    BaseAction.context.partner_crypto = partner_crypto_mock

    assert BaseAction.context.storage is None and BaseAction.context.merchant_user is None


@pytest.fixture
def base_merchant_action_data_mock(mocker):
    async def load_data_mock(self):
        self.merchant.load_data()

    mocker.patch.object(BaseMerchantAction, '_load_data', load_data_mock)


@pytest.fixture
def content_type():
    return 'test-download-document-content-type'


@pytest.fixture
def data():
    return 'test-download-document-data'
