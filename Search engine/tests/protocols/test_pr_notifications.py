import pytest

from bot.aiowarden import ProtocolSettings
from mocks.bot import TChat, DChat, TUser, TMessage, TCallbackQuery
from mocks.context import fast_component, temp_users, user, temp_components
from protocols.const import IMPORTANT_PROTOCOLS_CHAT


@pytest.fixture(scope='function', autouse=True)
def set_dependencies(get_context):
    components = [
        fast_component(
            name='parent',
            owners=['test_parent_owner'],  # todo: create test user
            spi_chat='https://t.me/joinchat/AAAAAAAAAAAAAAAAAAAAAA',
            onduty=['test_parent_duty_a', 'test_parent_duty_b'],
            support=['test_support_duty'],
            flow=['test_flow_duty'],
            pr=['test_pr_duty'],
            proto_settings=ProtocolSettings(enable_pr_protocols=True)
        ),
        fast_component(
            name='other',
            proto_settings=ProtocolSettings(enable_pr_protocols=False)
        ),
        fast_component(
            name='secure',
            parent_name='other',
            proto_settings=ProtocolSettings(enable_pr_protocols=True)
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

    with temp_users(
        get_context,
        *(user(x) for x in _users),
        user('marty', is_marty=True)
    ):
        with temp_components(get_context, *components):
            yield None


@pytest.mark.asyncio
async def test_start_red_protocol(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(IMPORTANT_PROTOCOLS_CHAT)
    chat.messages.clear()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/red start'
    ))

    assert len(chat.messages) == 1
    notification = chat.messages[0]
    # Красные технические протоколы автоматически получают флажок is_secure,
    # но при этом это не делает их действительно секьюрными
    assert 'Запущен <b>красный</b> протокол!' in notification.text


@pytest.mark.asyncio
async def test_upgrade_protocol(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(IMPORTANT_PROTOCOLS_CHAT)
    chat.messages.clear()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/red'
    ))

    confirmation = protocol_chat.messages[-1]
    button = confirmation.reply_markup.find_by_text('Трансформировать')
    await get_context.bot.call_callback_query(TCallbackQuery(
        message=confirmation,
        user=TUser('marty'),
        data=button.callback_data
    ))

    assert len(chat.messages) == 1
    notification = chat.messages[0]
    assert 'Запущен <b>красный</b> протокол!' in notification.text


@pytest.mark.asyncio
async def test_start_red_crisis(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(IMPORTANT_PROTOCOLS_CHAT)
    chat.messages.clear()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/crisis red parent'
    ))

    assert len(chat.messages) == 1
    notification = chat.messages[0]
    assert 'Запущен <b>красный</b> протокол!' in notification.text
    assert 'Компонента: <a href="https://warden.z.yandex-team.ru/components/parent">parent</a>' in notification.text


@pytest.mark.asyncio
async def test_upgrade_crisis(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(IMPORTANT_PROTOCOLS_CHAT)
    chat.messages.clear()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/crisis'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto pr red'
    ))

    assert len(chat.messages) == 1
    notification = chat.messages[0]
    assert 'Запущен <b>красный</b> протокол!' in notification.text


@pytest.mark.asyncio
async def test_start_secure(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(IMPORTANT_PROTOCOLS_CHAT)
    chat.messages.clear()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/secure'
    ))

    assert len(chat.messages) == 1
    notification = chat.messages[0]
    assert 'Запущен <b>черный</b> протокол!' in notification.text


@pytest.mark.asyncio
async def test_finish_protocol(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(IMPORTANT_PROTOCOLS_CHAT)
    chat.messages.clear()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/secure'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/finish'
    ))

    assert len(chat.messages) == 1
    notification = chat.messages[0]
    assert 'Завершен <b>черный</b> протокол!' in notification.text


@pytest.mark.asyncio
async def test_make_secure(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(IMPORTANT_PROTOCOLS_CHAT)
    chat.messages.clear()

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

    # /proto secure ничего не значит
    assert len(chat.messages) == 0


@pytest.mark.asyncio
async def test_make_pr_secure(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(IMPORTANT_PROTOCOLS_CHAT)
    chat.messages.clear()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto pr'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto secure'
    ))

    # /proto secure ничего не значит
    assert len(chat.messages) == 0


@pytest.mark.asyncio
async def test_set_status(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(IMPORTANT_PROTOCOLS_CHAT)
    chat.messages.clear()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/secure'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto status foobar'
    ))

    assert len(chat.messages) == 1
    notification = chat.messages[0]
    assert 'Статус: foobar' in notification.text


@pytest.mark.asyncio
async def test_set_position(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(IMPORTANT_PROTOCOLS_CHAT)
    chat.messages.clear()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto pr red'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto position foobar'
    ))

    assert len(chat.messages) == 1
    notification = chat.messages[0]
    assert 'Позиция PR: foobar' in notification.text


@pytest.mark.asyncio
async def test_set_ticket(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(IMPORTANT_PROTOCOLS_CHAT)
    chat.messages.clear()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/secure'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto ticket TEST-1234'
    ))

    assert len(chat.messages) == 1
    notification = chat.messages[0]
    assert '<a href="https://st.yandex-team.ru/TEST-1234">TEST-1234</a>' in notification.text


@pytest.mark.asyncio
async def test_set_tech_chat(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(IMPORTANT_PROTOCOLS_CHAT)
    chat.messages.clear()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto pr red'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/set_tech_chat https://t.me/joinchat/aaaaaaaaaaaaaaaa'
    ))

    assert len(chat.messages) == 1
    notification = chat.messages[0]
    assert 'https://t.me/joinchat/aaaaaaaaaaaaaaaa' in notification.text
