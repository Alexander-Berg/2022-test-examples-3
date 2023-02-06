import pytest

from bot.modules.protocols.models import Protocol
from mocks.bot import DChat, TChat, TUser, TMessage
from mocks.context import temp_users, user


@pytest.fixture(scope='function', autouse=True)
def set_dependencies(get_context):
    with temp_users(get_context, 'existed_login', ):
        try:
            yield
        finally:
            pass


@pytest.fixture(scope='function')
@pytest.mark.asyncio
async def prepare_data(get_context):
    data = get_context.data

    created_chat = DChat(TChat(), members={TUser('existed_login'), TUser('not_existed_login')})
    existed_chat = DChat(TChat(), members={TUser('existed_login'), TUser('not_existed_login')})

    get_context.bot.add_chat(created_chat)
    get_context.bot.add_chat(existed_chat)

    async with await data.connect() as conn:
        existed = await Protocol.create(
            conn,
            summary='existed',
            chat_id=existed_chat.chat.id,
            robot_username='mock_robot',
        )
        existed.is_chat_created = False
        await existed.commit(conn)

        created = await Protocol.create(
            conn,
            is_chat_create=True,
            summary='created',
            chat_id=created_chat.chat.id,
            robot_username='mock_robot',
        )

    try:
        yield existed_chat, created_chat
    finally:
        get_context.bot.chats.pop(created_chat.chat.id, None)
        get_context.bot.chats.pop(existed_chat.chat.id, None)

        async with await data.connect() as conn:
            await conn.execute(f'DELETE from protocols where id in ({existed.id}, {created.id});')


@pytest.mark.asyncio
async def test_secure_chats(prepare_data, get_context, proto_module):
    existed, created = prepare_data

    await proto_module.proto_context.chats.secure_chats()

    assert len(existed.members) == 2
    assert len(created.members) == 1


@pytest.mark.asyncio
async def test_toggle_forwarding(get_context, proto_module):
    with temp_users(
        get_context,
        user('marty', is_marty=True)
    ):
        created_chat = DChat(TChat(type='private'))
        get_context.bot.add_chat(created_chat)
        proto_module.data = get_context.data

        await get_context.bot.call_command(TMessage(
            user=TUser('marty'),
            chat=created_chat.chat,
            text='/proto secure'
        ))

        protocol_chat = list(get_context.bot.chats.values())[-1]
        assert protocol_chat.toggle_no_forwards

        await get_context.bot.call_command(TMessage(
            user=TUser('marty'),
            chat=created_chat.chat,
            text='/proto'
        ))

        protocol_chat = list(get_context.bot.chats.values())[-1]

        await get_context.bot.call_command(TMessage(
            user=TUser('marty'),
            chat=protocol_chat.chat,
            text='/proto secure'
        ))
        assert protocol_chat.toggle_no_forwards
