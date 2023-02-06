import asyncio

import pytest
from sqlalchemy import func

from mail.payments.payments.core.entities.enums import PayStatus, TransactionStatus
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.interactions.trust.exceptions import TrustException
from mail.payments.payments.storage.writers import PaymentsPushers
from mail.payments.payments.taskq.app import ArbiterWorker, BaseWorkerApplication
from mail.payments.payments.taskq.workers.transaction_updater import TransactionUpdater


@pytest.fixture(autouse=True)
def mock_constants():
    ArbiterWorker.KILL_ON_CLEANUP = False  # Иначе тесты умрут по сигналу SIGALRM
    Transaction._CHECK_INITIAL_DELAY = 3600
    TransactionUpdater.pause_period = 0.1


@pytest.fixture
def app_cls(mocker):
    class App(BaseWorkerApplication):
        workers = [
            (TransactionUpdater, 1),
        ]

        async def close_engine(self, _):
            pass

    return App


@pytest.fixture(autouse=True)
async def start_app(aiohttp_server, mocker, app_cls, db_engine, loop):
    app = app_cls(db_engine=db_engine, pushers=PaymentsPushers(loop), crypto=mocker.Mock(),
                  partner_crypto=mocker.Mock())
    return await aiohttp_server(app)


@pytest.fixture
def wait_task(storage):
    async def _wait_task(transaction, retries=5):
        transaction.check_at = func.now()
        transaction = await storage.transaction.create(transaction)
        initial_check_at = transaction.check_at
        for _ in range(retries):
            t = await storage.transaction.get(transaction.uid, transaction.tx_id)
            if t.check_at > initial_check_at:
                return t

            await asyncio.sleep(0.1)

        assert False, 'Was not completed'

    return _wait_task


@pytest.fixture(autouse=True)
async def delay_other_transactions(db_conn):
    await db_conn.execute("update payments.transactions set check_at = now() + interval '1 day'")


class TestOnTrustException:
    @pytest.fixture
    async def order(self, storage, order):
        order.pay_status = PayStatus.PAID
        return await storage.order.save(order)

    @pytest.fixture
    def transaction(self, order):
        return Transaction(
            uid=order.uid,
            order_id=order.order_id,
            check_tries=0,
            trust_purchase_token='token',
            status=TransactionStatus.ACTIVE,
        )

    @pytest.fixture(autouse=True)
    def mock_trust(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'payment_get', exc=TrustException(method='method')) as mock:
            yield mock

    @pytest.fixture
    async def waited(self, start_app, mock_trust, wait_task, transaction):
        return await wait_task(transaction)

    def test_task_check_retries_inc(self, waited):
        assert waited.check_tries == 1
