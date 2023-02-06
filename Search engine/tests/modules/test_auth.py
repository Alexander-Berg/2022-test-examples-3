import pytest

from bot.telegram import Chat
from mocks.bot import DChat, TChat, TUser
from mocks.context import user, temp_users


TELEGRAM_SPECIAL_USERS = [
    TUser('GroupAnonymousBot'),  # https://t.me/GroupAnonymousBot
    TUser('Channel_Bot'),  # https://t.me/Channel_Bot
]


@pytest.mark.parametrize("user", TELEGRAM_SPECIAL_USERS)
@pytest.mark.asyncio
async def test_anonymous_error(get_context, user):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)

    message = created_chat.add_message(user, '/sendproto foobar')
    chat = Chat.from_message(get_context.bot, message.json())
    result = await get_context.auth.authorize(chat)
    assert result == (None, False)
    assert len(created_chat.messages) == 2
    assert 'Нельзя использовать бота от имени группы или канала' in created_chat.messages[-1].text


@pytest.mark.parametrize("user", TELEGRAM_SPECIAL_USERS)
@pytest.mark.asyncio
async def test_anonymous_silent(get_context, user):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)

    message = created_chat.add_message(user, '/sendproto foobar')
    chat = Chat.from_message(get_context.bot, message.json())
    result = await get_context.auth.authorize(chat, error_message=False)
    assert result == (None, False)
    assert len(created_chat.messages) == 1


@pytest.mark.asyncio
async def test_authenticate_channel_ok(get_context):
    created_chat = DChat(TChat(type='channel'))
    get_context.bot.add_chat(created_chat)

    created_chat.members.add(TUser('channel_admin'))
    created_chat.admins.add('channel_admin')

    with temp_users(get_context, user('channel_admin')):
        # Note that user is an empty dict
        message = created_chat.add_message(user={}, text='/sendproto foobar')
        chat = Chat.from_message(get_context.bot, message.json())
        person, auth = await get_context.auth.authorize(chat)
        assert auth is True
        assert person is not None


@pytest.mark.asyncio
async def test_authenticate_channel_many_admins(get_context):
    created_chat = DChat(TChat(type='channel'))
    get_context.bot.add_chat(created_chat)

    created_chat.admins = {'channel_admin1', 'channel_admin2'}
    for i in created_chat.admins:
        created_chat.members.add(TUser(i))

    with temp_users(get_context, *(user(i) for i in created_chat.admins)):
        # Note that user is an empty dict
        message = created_chat.add_message(user={}, text='/sendproto foobar')
        chat = Chat.from_message(get_context.bot, message.json())
        person, auth = await get_context.auth.authorize(chat)
        assert auth is False
        assert person is None
