import pytest

from bot.modules.protocols.feedback import FeedbackHelper, Protocol
from mocks.bot import DChat, TChat, TUser
from mocks.context import temp_users


@pytest.mark.skip
@pytest.fixture(scope='function', autouse=True)
def set_dependencies(get_context):
    with temp_users(get_context, 'existed_login'):
        try:
            yield
        finally:
            pass


@pytest.mark.skip
@pytest.fixture(scope='function')
@pytest.mark.asyncio
async def prepare_data(get_context):
    data = get_context.data

    created_chat = DChat(TChat(), members={TUser('existed_login'), TUser('not_existed_login')})

    get_context.bot.add_chat(created_chat)

    async with await data.connect() as conn:
        created_proto = await Protocol.create(
            conn,
            is_chat_create=True,
            summary='created',
            chat_id=created_chat.chat.id,
            robot_username='mock_robot',
        )

    feedback_helper = FeedbackHelper(_callback=None, context=get_context)
    feedback_helper.context = get_context

    try:
        yield created_chat, created_proto, feedback_helper
    finally:
        get_context.bot.chats.pop(created_chat.chat.id, None)

        async with await data.connect() as conn:
            await conn.execute(f'DELETE from protocols where id={created_proto.id};')


@pytest.mark.skip
@pytest.mark.asyncio
async def test_feedback_start(prepare_data, get_context, proto_module):
    chat, proto, fb_helper = prepare_data

    await fb_helper.feedback(args={}, chat=chat, proto=proto, person={'login': 'some-login123'})
