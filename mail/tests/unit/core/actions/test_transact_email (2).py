import pytest

from mail.payments.payments.core.actions.transact_email import TransactEmailAction


@pytest.fixture(autouse=True)
def send_letter_mock(sender_client_mocker):
    with sender_client_mocker('send_transactional_letter') as mock:
        yield mock


@pytest.fixture
def mailing_id(randn):
    return randn()


@pytest.fixture
def render_context():
    return {
        'hey': 'hey',
    }


@pytest.fixture
def to_email():
    return 'guido@eggs.ni'


@pytest.fixture
def params(to_email, render_context, mailing_id):
    return {
        'to_email': to_email,
        'render_context': render_context,
        'mailing_id': mailing_id,
    }


@pytest.fixture
async def returned(params):
    return await TransactEmailAction(**params).run()


@pytest.mark.asyncio
async def test_calls_sender_client(send_letter_mock, returned, to_email, mailing_id, render_context):
    assert send_letter_mock.called_once_with(
        mailing_id=mailing_id,
        render_context=render_context,
        to_email=to_email,
    )
