import pytest
from bot.aiowarden import ProtocolSettings

from mocks.bot import DChat, TChat, TUser, TMessage, TCallbackQuery
from mocks.context import temp_components, fast_component, temp_users, user


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
            proto_settings=ProtocolSettings(enable_pr_protocols=False)
        ),
        fast_component(
            name='component2',
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
            owners=['test_parent_owner'],  # todo: create test user
            spi_chat='https://t.me/joinchat/AAAAAAAAAAAAAAAAAAAAAA',
            onduty=['test_parent_duty_a', 'test_parent_duty_b'],
            support=['test_support_duty'],
            flow=['test_flow_duty'],
            pr=['test_pr_duty'],
            proto_settings=ProtocolSettings(enable_pr_protocols=False)
        ),
        fast_component(
            name='secure',
            parent_name='other',
            owners=['test_parent_owner'],  # todo: create test user
            spi_chat='https://t.me/joinchat/AAAAAAAAAAAAAAAAAAAAAA',
            onduty=['test_parent_duty_a', 'test_parent_duty_b'],
            support=['test_support_duty'],
            flow=['test_flow_duty'],
            pr=['test_pr_duty'],
            proto_settings=ProtocolSettings(enable_pr_protocols=True)
        ),
        fast_component(
            name='pr_test',
            parent_name='spi-tools-test',
            owners=['test_pr_owner'],  # todo: create test user
        ),
        fast_component(
            name='smm_test',
            parent_name='spi-tools-test',
            owners=['test_smm_owner'],  # todo: create test user
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
async def test_start_pr(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    # import logging
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/crisis parent'
    ))

    pr_found = False
    for id_, chat in get_context.bot.chats.items():
        for m in chat.messages:
            if 'test_pr_duty' in m.text:
                pr_found = True

    assert pr_found


@pytest.mark.asyncio
async def test_pr_positions(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto position OLOLO'
    ))

    position_found = False
    for msg in protocol_chat.messages:
        if '<b>Позиция PR:</b> OLOLO' in msg.text:
            position_found = True

    assert position_found


@pytest.mark.asyncio
async def test_short_position(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/position OLOLO'
    ))

    position_found = False
    for msg in protocol_chat.messages:
        if '<b>Позиция PR:</b> OLOLO' in msg.text:
            position_found = True

    assert position_found


@pytest.mark.asyncio
async def test_pick_non_pr_component(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto pr'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto component parent'
    ))

    keyboard = protocol_chat.messages[-1]

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=keyboard,
        user=TUser('marty'),
        data='protocols|pick_component:pick:1:4658263254135485168'
    ))

    assert 'Для этой компоненты выключены PR протоколы.' in protocol_chat.messages[-1].text

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto component component2'
    ))

    keyboard = protocol_chat.messages[-1]

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=keyboard,
        user=TUser('marty'),
        data='protocols|pick_component:pick:1:9952628855875482552'
    ))

    found = False
    for msg in protocol_chat.messages:
        if 'Установил компоненту инцидента: component2' == msg.text:
            found = True
            break
    assert found


@pytest.mark.asyncio
async def test_pr_escalate_critical(get_context, proto_module):
    '''
    В PR-протоколы, при повышении уровня на красный, призываются дополнительные люди: владельцы PR- и SMM-компонент.
    '''

    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto pr component2'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    members = {i.username for i in protocol_chat.members}

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto pr red'
    ))
    assert 'Установил уровень кризиса: ❤️ Красный' in protocol_chat.messages[-1].text

    new_members = {i.username for i in protocol_chat.members}.difference(members)
    assert new_members == {'test_pr_owner', 'test_smm_owner'}


@pytest.mark.asyncio
async def test_secure_with_component_escalate_critical(get_context, proto_module):
    '''
    Если у секьюрного протокола указана компонента, отличная от `other/secure`, то призываем людей как обычно.
    '''

    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/secure pr component2'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]
    members = {i.username for i in protocol_chat.members}

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto pr red'
    ))
    assert 'Установил уровень кризиса: ❤️ Красный' in protocol_chat.messages[-1].text

    new_members = {i.username for i in protocol_chat.members}.difference(members)
    assert new_members == {'test_pr_owner', 'test_smm_owner'}


@pytest.mark.asyncio
async def test_secure_escalate_critical(get_context, proto_module):
    '''
    Если у секьюрного протокола одновременно секьюрная же компонента, то никого при повышении уровня не призываем.
    '''

    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/secure'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto pr'
    ))
    members = {i.username for i in protocol_chat.members}

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto pr red'
    ))
    assert 'Установил уровень кризиса: ❤️ Красный' in protocol_chat.messages[-1].text

    new_members = {i.username for i in protocol_chat.members}.difference(members)
    assert new_members == set()
