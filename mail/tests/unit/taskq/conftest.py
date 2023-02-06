import pytest

from mail.payments.payments.core.actions.base.action import BaseAction
from mail.payments.payments.tests.utils import dummy_async_function
from mail.payments.payments.utils.helpers import temp_setattr


@pytest.fixture(autouse=True)
def action_context_setup(test_logger, db_engine, rands, storage, pushers_mock):
    BaseAction.context.logger = test_logger
    BaseAction.context.request_id = rands()
    BaseAction.context.db_engine = db_engine
    BaseAction.context.pushers = pushers_mock
    BaseAction.context.storage = storage
    with temp_setattr(BaseAction.context, 'storage', storage):
        yield


@pytest.fixture
def app(pushers_mock, partner_crypto_mock, crypto_mock, db_engine):
    from mail.payments.payments.taskq.app import PaymentsWorkerApplication
    return PaymentsWorkerApplication(
        pushers=pushers_mock,
        partner_crypto=partner_crypto_mock,
        crypto=crypto_mock,
        db_engine=db_engine
    )


@pytest.fixture
def moderation_app(lb_factory_mock, partner_crypto_mock, crypto_mock, pushers_mock, db_engine):
    from mail.payments.payments.taskq.app import ModerationWorkerApplication
    app = ModerationWorkerApplication(
        pushers=pushers_mock,
        partner_crypto=partner_crypto_mock,
        crypto=crypto_mock,
        db_engine=db_engine
    )
    app['lb_factory'] = lb_factory_mock
    return app


@pytest.fixture(autouse=True)
def heartbeat_mock(mocker):
    mocker.patch(
        'mail.payments.payments.taskq.workers.base.BaseWorker.heartbeat',
        dummy_async_function(calls=[]),
    )
