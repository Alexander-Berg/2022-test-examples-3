import os
from unittest.mock import MagicMock, AsyncMock, Mock

import pytest
from _pytest.python_api import raises
from telethon.errors import ChannelsTooMuchError, FloodWaitError, UserDeactivatedBanError, AuthKeyDuplicatedError

SUCCESS = 'success'


class NotRPCError(Exception):
    """Some not telegram RPC exception"""


@pytest.fixture
def msg():
    rotator = Mock()
    rotator.sessionmaker = Mock(return_value=AsyncMock())

    message = AsyncMock()
    message.bot.__getitem__ = lambda *args, **kwargs: rotator

    return message


@pytest.fixture(scope='function')
def mock_bot(mocker):
    os.environ['TELEGRAM_BOT_TOKEN'] = 'kek'
    os.environ['TELEGRAM_BOT_USERNAME'] = 'kek'
    mocker.patch('aiogram.bot.base.BaseBot.__init__', lambda self, *args, **kwargs: None)


@pytest.fixture(scope='function')
def creator_setup_ctx(mocker, mock_bot):
    pool_mock = AsyncMock()

    def stub_fake_user_init(self, *args, **kwargs):
        self.pool = pool_mock

    mocker.patch('lib.services.telegram_account_rotator.decorators.FakeUserCreator.__init__', stub_fake_user_init)
    mocker.patch('lib.services.telegram_account_rotator.decorators.FakeUserCreator.connect', AsyncMock())
    return {'pool_mock': pool_mock}


@pytest.mark.asyncio
async def test_run_with_creator_expected_errors(creator_setup_ctx, msg):
    from lib.services.telegram_account_rotator.decorators import run_with_creator

    pool_mock = creator_setup_ctx['pool_mock']

    handler = AsyncMock(side_effect=[
        ChannelsTooMuchError(None),
        FloodWaitError(None),
        UserDeactivatedBanError(None),
        AuthKeyDuplicatedError(None),
        SUCCESS,
    ])

    assert await run_with_creator(handler, msg) == SUCCESS
    assert handler.call_count == 4
    assert pool_mock.get_clean_client.call_count == 4


@pytest.mark.asyncio
async def test_run_with_creator_unexpected_errors(creator_setup_ctx, msg):
    from lib.services.telegram_account_rotator.decorators import run_with_creator

    pool_mock = creator_setup_ctx['pool_mock']
    handler = AsyncMock(side_effect=[
        ChannelsTooMuchError(None),
        FloodWaitError(None),
        UserDeactivatedBanError(None),
        AuthKeyDuplicatedError(None),
        NotRPCError,
        SUCCESS,
    ])

    with raises(NotRPCError):
        await run_with_creator(handler, msg)
    assert handler.call_count == 4
    assert pool_mock.get_clean_client.call_count == 4


@pytest.fixture(scope='function')
def admin_setup_ctx(mocker, mock_bot):
    pool_mock = AsyncMock()

    def stub_fake_user_init(self, *args, **kwargs):
        self.pool = pool_mock
        self.new = False
        self.chat = MagicMock()

    mocker.patch('lib.services.telegram_account_rotator.decorators.bot', AsyncMock())
    mocker.patch('lib.services.telegram_account_rotator.decorators.ChatManager.get', AsyncMock())
    mocker.patch('lib.services.telegram_account_rotator.decorators.ChatManager.__init__', Mock(return_value=None))
    mocker.patch('lib.services.telegram_account_rotator.decorators.FakeUserAdmin.__init__', stub_fake_user_init)
    mocker.patch('lib.services.telegram_account_rotator.decorators.FakeUserAdmin.connect', AsyncMock())
    return {'pool_mock': pool_mock}


@pytest.mark.asyncio
async def test_run_with_chat_admin_expected_errors(admin_setup_ctx, msg):
    from lib.services.telegram_account_rotator.decorators import run_with_chat_admin

    pool_mock = admin_setup_ctx['pool_mock']
    handler = AsyncMock(side_effect=[
        UserDeactivatedBanError(None),
        SUCCESS,
    ])

    assert await run_with_chat_admin(handler, msg) == SUCCESS
    assert handler.call_count == 2
    assert pool_mock.get_client.call_count == 1
    assert pool_mock.get_clean_or_maxed_out_limits_client.call_count == 1


@pytest.mark.asyncio
async def test_run_with_chat_admin_unexpected_errors(admin_setup_ctx, msg):
    from lib.services.telegram_account_rotator.decorators import run_with_chat_admin

    pool_mock = admin_setup_ctx['pool_mock']
    handler = AsyncMock(side_effect=[
        UserDeactivatedBanError(None),
        NotRPCError,
        SUCCESS,
    ])

    with raises(NotRPCError):
        await run_with_chat_admin(handler, msg)
    assert handler.call_count == 2
    assert pool_mock.get_client.call_count == 1
    assert pool_mock.get_clean_or_maxed_out_limits_client.call_count == 1
