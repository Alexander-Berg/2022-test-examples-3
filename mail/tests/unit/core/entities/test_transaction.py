from datetime import datetime, timedelta, timezone
from importlib import reload

import pytest

import mail.payments.payments.core.entities.transaction
from mail.payments.payments.core.entities.enums import AcquirerType, TransactionStatus


@pytest.fixture
def now(mocker):
    now = datetime(2019, 1, 2, 3, 4, 5, tzinfo=timezone.utc)
    mocker.patch('mail.payments.payments.core.entities.transaction.utcnow', mocker.Mock(return_value=now))
    return now


@pytest.fixture
def start_delay_active():
    return 1


@pytest.fixture
def start_delay_held():
    return 1


@pytest.fixture
def initial_delay():
    return 1


@pytest.fixture
def delay_multiplier():
    return 2


@pytest.fixture
def max_delay():
    return 1


@pytest.fixture(autouse=True)
def setup_settings(payments_settings,
                   start_delay_active,
                   start_delay_held,
                   initial_delay,
                   delay_multiplier,
                   max_delay,
                   ):
    payments_settings['TRANSACTION_CHECK_START_DELAY_ACTIVE'] = start_delay_active
    payments_settings['TRANSACTION_CHECK_START_DELAY_HELD'] = start_delay_held
    payments_settings['TRANSACTION_CHECK_INITIAL_DELAY'] = initial_delay
    payments_settings['TRANSACTION_CHECK_DELAY_MULTIPLIER'] = delay_multiplier
    payments_settings['TRANSACTION_CHECK_MAX_DELAY'] = max_delay
    reload(mail.payments.payments.core.entities.transaction)


@pytest.fixture
def transaction(randn, setup_settings):
    from mail.payments.payments.core.entities.transaction import Transaction
    return Transaction(uid=randn(), order_id=randn())


class TestResetCheckTries:
    def test_sets_check_tries_to_0(self, transaction):
        transaction.check_tries = 10
        transaction.reset_check_tries()
        assert transaction.check_tries == 0

    @pytest.mark.parametrize('status,start_delay_active,start_delay_held,expected_delay', (
        (TransactionStatus.ACTIVE, 123, 456, 123),
        (TransactionStatus.HELD, 123, 456, 456),
    ))
    def test_check_start_delay(self, now, transaction, status, expected_delay):
        transaction.status = status
        transaction.reset_check_tries()
        assert transaction.check_at == now + timedelta(seconds=expected_delay)

    @pytest.mark.parametrize('status', (
        pytest.param(status, id=status.value)
        for status in TransactionStatus
        if status not in (TransactionStatus.ACTIVE, TransactionStatus.HELD)
    ))
    def test_check_start_instant(self, now, transaction, status):
        transaction.status = status
        transaction.reset_check_tries()
        assert transaction.check_at == now


class TestIncrementCheckTries:
    def test_increments_check_tries(self, transaction):
        transaction.check_tries = 10
        transaction.increment_check_tries()
        assert transaction.check_tries == 11

    @pytest.mark.parametrize('initial_delay,delay_multiplier,max_delay', (
        (10, 2, 10 * 2 ** 30),
    ))
    def test_exponential_backoff(self, now, transaction, initial_delay, delay_multiplier):
        transaction.check_tries = check_tries = 25
        transaction.increment_check_tries()
        assert transaction.check_at == now + timedelta(seconds=initial_delay * delay_multiplier ** check_tries)

    @pytest.mark.parametrize('initial_delay,delay_multiplier,max_delay', (
        (10, 2, 3600),
    ))
    def test_max_delay(self, now, transaction, max_delay):
        transaction.check_tries = 1000
        transaction.increment_check_tries()
        assert transaction.check_at == now + timedelta(seconds=max_delay)


@pytest.mark.parametrize(('acquirer', 'expected_view', 'expected_download'), (
    (None, None, None),
    (AcquirerType.KASSA, None, None),
    (
        AcquirerType.TINKOFF,
        'http://trust-receipt.test/checks/abcde/receipts/abcde/',
        'http://trust-receipt.test/checks/abcde/receipts/abcde/?mode=pdf',
    ),
))
def test_set_trust_receipt_urls(transaction, acquirer, expected_view, expected_download, payments_settings):
    payments_settings.TRUST_RECEIPT_BASE_URL = 'http://trust-receipt.test'
    transaction.trust_purchase_token = 'abcde'

    transaction.set_trust_receipt_urls(acquirer)

    assert all((
        transaction.trust_receipt_view_url == expected_view,
        transaction.trust_receipt_download_url == expected_download,
    ))
