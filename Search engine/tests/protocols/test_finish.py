import pytest

from bot.aiowarden import ProtocolSettings
from mocks.bot import DChat, TChat, TMessage, TUser, TCallbackQuery
from mocks.context import fast_component, temp_users, user, temp_components


@pytest.fixture(scope='function', autouse=True)
def set_dependencies(get_context):
    components = [
        fast_component(
            name='test_component',
            pr=['test_pr'],
            proto_settings=ProtocolSettings(enable_pr_protocols=True)
        )
    ]

    with temp_users(get_context, user('marty', is_marty=True), user('test_pr')):
        with temp_components(get_context, *components):
            yield None


@pytest.mark.asyncio
async def test_finish_via_command(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto start'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    status_message = protocol_chat.messages[1]
    assert '#proto' in status_message.text

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto finish'
    ))

    assert 'Протокол завершен по инициативе ' in status_message.text
    assert protocol_chat.chat.title.endswith('[ЗАВЕРШЕН]')


@pytest.mark.asyncio
async def test_finish_via_button(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto start'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    status_message = protocol_chat.messages[1]
    assert '#proto' in status_message.text

    button = status_message.reply_markup.find_by_text('Завершить протокол')
    assert button is not None

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=status_message,
        user=TUser('marty'),
        data=button.callback_data
    ))

    confirmation_message = protocol_chat.messages[-1]
    assert 'точно завершить протокол?' in confirmation_message.text
    button = confirmation_message.reply_markup.find_by_text('Да, завершить')
    assert button is not None

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=status_message,
        user=TUser('marty'),
        data=button.callback_data
    ))

    assert 'Протокол завершен по инициативе ' in status_message.text
    assert protocol_chat.chat.title.endswith('[ЗАВЕРШЕН]')


@pytest.mark.asyncio
async def test_finish_without_protocol(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto finish'
    ))

    status_message = created_chat.messages[0]
    assert 'Неизвестная команда' in status_message.text


@pytest.mark.asyncio
async def test_finish_pr_by_marty(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/crisis test_component'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    status_message = protocol_chat.messages[1]

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto finish'
    ))

    response = protocol_chat.messages[-1]
    assert 'PR-протокол может быть завершен только членом PR-команды.' in response.text
    assert 'Протокол завершен' not in status_message.text


@pytest.mark.asyncio
async def test_finish_pr_by_pr(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/crisis test_component'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    status_message = protocol_chat.messages[1]

    await get_context.bot.call_command(TMessage(
        user=TUser('test_pr'),
        chat=protocol_chat.chat,
        text='/proto finish'
    ))

    assert 'Протокол завершен' in status_message.text
