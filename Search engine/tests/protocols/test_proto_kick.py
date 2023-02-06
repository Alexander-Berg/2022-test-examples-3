import pytest

from bot.modules.protocols.models import Protocol, ProtoChat
from mocks.bot import DChat, TChat, TUser, TChatMessage
from mocks.context import user


@pytest.fixture(scope='function')
async def prepare_data(get_context):
    data = get_context.data

    created_chat = DChat(TChat(), members={TUser('existed_login'),
                                           TUser('not_existed_login'),
                                           TUser('to_kick'),
                                           TUser('to_kick2')})

    get_context.bot.add_chat(created_chat)

    async with await data.connect() as conn:
        proto_chat = await ProtoChat.create(
            conn,
            id=created_chat.chat.id,
            join_url=None,
            title='👻 тестовая функциональность',
            pin_message_id=None,
            pin_message_text='запин'
        )
        created = await Protocol.create(
            conn,
            is_chat_create=True,
            summary='created',
            chat_id=created_chat.chat.id,
            robot_username='mock_robot',
            _chat=proto_chat.record_id,
        )

    try:
        yield created, created_chat
    finally:
        get_context.bot.chats.pop(created_chat.chat.id, None)

        async with await data.connect() as conn:
            await conn.execute(f'DELETE from protocols where id in ({created.id});')


@pytest.mark.asyncio
async def test_proto_kick_by_marty(prepare_data, get_context, proto_module):
    created, created_chat = prepare_data
    chat_message = TChatMessage(sender={'username': 'existed_login'}, admins=[])

    assert len(created_chat.members) == 4

    await proto_module.kick({'text': 'to_kick'}, created, chat_message, user('marty', is_marty=True))

    assert len(created_chat.members) == 3


@pytest.mark.asyncio
async def test_proto_kick_by_not_admin(prepare_data, get_context, proto_module):
    created, created_chat = prepare_data
    chat_message = TChatMessage(sender={'username': 'existed_login'}, admins=[])

    assert len(created_chat.members) == 4

    await proto_module.kick({'text': 'to_kick'}, created, chat_message, user('not_marty', is_marty=False))

    assert len(created_chat.members) == 4


@pytest.mark.asyncio
async def test_proto_kick_by_admin(prepare_data, get_context, proto_module):
    created, created_chat = prepare_data
    chat_message = TChatMessage(sender={'username': 'existed_login'}, admins=['existed_login'])

    assert len(created_chat.members) == 4

    await proto_module.kick({'text': 'to_kick'}, created, chat_message, user('not_marty', is_marty=False))

    assert len(created_chat.members) == 3


@pytest.mark.asyncio
async def test_proto_kicks_by_admin(prepare_data, get_context, proto_module):
    created, created_chat = prepare_data
    chat_message = TChatMessage(sender={'username': 'existed_login'}, admins=['existed_login'])

    assert len(created_chat.members) == 4

    await proto_module.kick({'text': 'to_kick to_kick2'}, created, chat_message, user('not_marty', is_marty=False))

    assert len(created_chat.members) == 2


@pytest.mark.asyncio
async def test_proto_kick_admin_by_admin(prepare_data, get_context, proto_module):
    created, created_chat = prepare_data
    chat_message = TChatMessage(sender={'username': 'existed_login'}, admins=['existed_login'])

    assert len(created_chat.members) == 4

    await proto_module.kick({'text': 'existed_login'}, created, chat_message, user('not_marty', is_marty=False))

    assert len(created_chat.members) == 4
