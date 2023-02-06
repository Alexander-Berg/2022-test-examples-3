from functools import partial
import pytest
from datetime import datetime, timedelta
from bot.modules.protocols.const.proto import ProtoHelp

from bot.modules.protocols.models import Protocol

from mocks.bot import DChat, TChat, TUser, TMessage, TCallbackQuery
from mocks.context import temp_components, fast_component, temp_users, user
from protocols.const import INTERNAL_MARTYCHAT


def marty(*logins):
    return list(map(partial(user, is_marty=True), logins))


@pytest.fixture(scope='function', autouse=True)
def set_dependencies(get_context, proto_module):
    components = [
        fast_component(
            name='parent',
            owners=['test_parent_owner'],  # todo: create test user
            spi_chat='https://t.me/joinchat/AAAAAAAAAAAAAAAAAAAAAA',
            onduty=['test_parent_duty_a', 'test_parent_duty_b'],
            support=['test_support_duty'],
            flow=['test_flow_duty'],
            pr=['test_pr_duty'],
        ),
    ]

    _users = set()
    for component in components:
        _users.update(component.owners)
        _users.update(component.duty)
        _users.update(component.flow)
        _users.update(component.support)
        _users.update(component.pr)
        _users.update(component.smm)

    get_context.modules.marty._current = user('marty', is_marty=True)
    get_context.modules.marty._reserve = marty('reserve_marty-1')
    get_context.modules.marty._managers = marty('marty-manager-1')
    get_context.modules.marty._all = marty('marty', 'marty-2', 'marty-3')
    get_context.modules.marty._head = user('head_marty', is_marty=True)
    proto_module.data = get_context.data

    with temp_users(
        get_context,
        *(user(x) for x in _users),
        user('marty', is_marty=True),
        user('marty-2', is_marty=True),
        user('marty-3', is_marty=True),
        user('reserve-marty-1', is_marty=True),
        user('marty-manager-1', is_marty=True),
        user('rmmagomedov', is_marty=True),
        user('not_marty')
    ):
        with temp_components(get_context, *components):
            yield None


@pytest.mark.asyncio
async def test_helpme_only_marty(get_context):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto start'
    ))

    proto_chat = list(get_context.bot.chats.values())[-1]

    await get_context.bot.call_command(TMessage(
        user=TUser('not_marty'),
        chat=proto_chat.chat,
        text='/helpme'
    ))

    last_message = proto_chat.messages[-1]
    assert last_message.text == 'Только marty может позвать на помощь'


@pytest.mark.asyncio
async def test_helpme_callback(get_context):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto start'
    ))

    proto_chat = list(get_context.bot.chats.values())[-1]

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='/helpme'
    ))

    martychat: DChat = get_context.bot.find_chat(INTERNAL_MARTYCHAT)
    message = martychat.messages[-1]
    assert 'Марти нужна помощь' in message.text

    button = message.reply_markup.find_by_text('Я помогу')
    await get_context.bot.call_callback_query(TCallbackQuery(
        message=message,
        user=TUser('reserve-marty-1'),
        data=button.callback_data
    ))

    assert 'reserve-marty-1 пришел на помощь.' == proto_chat.messages[-1].text


@pytest.mark.asyncio
async def test_helpme_command(get_context):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto start'
    ))

    proto_chat = list(get_context.bot.chats.values())[-1]

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='/helpme'
    ))

    martychat: DChat = get_context.bot.find_chat(INTERNAL_MARTYCHAT)
    message = martychat.messages[-1]
    assert 'Марти нужна помощь' in message.text

    await get_context.bot.call_command(TMessage(
        user=TUser('reserve-marty-1'),
        chat=proto_chat.chat,
        text='/getproto'
    ))

    assert 'reserve-marty-1 пришел на помощь.' == proto_chat.messages[-1].text


@pytest.mark.asyncio
async def test_helpme_ticks(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    martychat: DChat = get_context.bot.find_chat(INTERNAL_MARTYCHAT)
    martychat.messages.clear()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto start'
    ))

    proto_chat = list(get_context.bot.chats.values())[-1]

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='/helpme'
    ))

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_chat(conn, proto_chat.chat.id)

    expected_help = [
        ProtoHelp.reserve_marty,
        ProtoHelp.managers,
        ProtoHelp.marty,
        ProtoHelp.head,
        ProtoHelp.marty,
        ProtoHelp.head,
    ]

    for i in range(6):
        assert len(martychat.messages) == i+1
        message = martychat.messages[-1]
        assert 'Марти нужна помощь' in message.text
        assert expected_help[i].human in message.text
        async with await get_context.data.connect() as conn:
            proto.help_acquired_time = datetime.now() - timedelta(minutes=2)
            await proto.commit(conn, include={'help_acquired_time'})
        await proto_module.proto_context.help.update_helpme_requests()
