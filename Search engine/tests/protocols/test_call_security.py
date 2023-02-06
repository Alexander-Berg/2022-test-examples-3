import pytest

from bot.modules.onduty import Onduty as OndutyModule
from bot.aiowarden import OnDuty
from bot.modules.protocols import Protocol, ProtoLevel
from mocks.bot import DChat, TChat, TMessage, TUser, TCallbackQuery
from mocks.context import temp_users, user, fast_component, temp_components


@pytest.fixture(scope='function', autouse=True)
def set_dependencies(get_context, proto_module):
    proto_module.data = get_context.data
    components = [
        fast_component(
            name='security_test',
            parent_name='spi-tools-test',
            owners=['test_security_owner'],  # todo: create test user
            onduty=[
                OnDuty(role="dutyrole1", login="dutyuser1"),
                OnDuty(role="dutyrole2", login="dutyuser2"),
            ],
        )
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


@pytest.fixture(scope='session', autouse=True)
def register_onduty(get_context):
    onduty = OndutyModule(get_context, {})
    get_context.modules.onduty = onduty
    get_context.bot.register_modules(onduty)


async def start_proto(get_context, command) -> DChat:
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text=command
    ))

    return list(get_context.bot.chats.values())[-1]


@pytest.mark.asyncio
async def test_call_security_no(get_context):
    get_context.modules.onduty.data = get_context.data

    protocol_chat = await start_proto(get_context, '/green start')
    msg = protocol_chat.find_message_containing('Затрагивает ли инцидент безопасность сервиса или персональные данные?')
    assert msg is not None

    people = protocol_chat.members
    no = msg.reply_markup.find_by_text('Нет')
    assert no is not None

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=msg,
        user=TUser('marty'),
        data=no.callback_data
    ))

    assert not msg.reply_markup
    assert '> Нет' in msg.text

    async with await get_context.data.connect() as conn:
        protocol = await Protocol.get_by_chat(conn, protocol_chat.chat.id)
        assert protocol.level == ProtoLevel.green
    assert protocol_chat.members == people


@pytest.mark.asyncio
async def test_call_security_yes(get_context):
    protocol_chat = await start_proto(get_context, '/green start')
    msg = protocol_chat.find_message_containing('Затрагивает ли инцидент безопасность сервиса или персональные данные?')
    assert msg is not None

    people = {i.username for i in protocol_chat.members}
    yes = msg.reply_markup.find_by_text('Да')
    assert yes is not None

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=msg,
        user=TUser('marty'),
        data=yes.callback_data
    ))

    assert not msg.reply_markup
    assert '> Да' in msg.text
    assert any('Поменял уровень протокола на <b>Желтый</b>.' in msg.text for msg in protocol_chat.messages)

    escalate = protocol_chat.find_message_containing('выбери, кого призвать')
    assert escalate is not None

    buttons = [btn.text for row in escalate.reply_markup.inline_keyboard for btn in row]
    assert buttons == ['dutyrole1', 'dutyrole2', 'Удалить сообщение 🗑️']

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=escalate,
        user=TUser('marty'),
        data=escalate.reply_markup.find_by_text('dutyrole1').callback_data
    ))

    new_people = {i.username for i in protocol_chat.members} - people
    assert new_people == {'dutyuser1'}


@pytest.mark.asyncio
async def test_call_security_role2(get_context):
    protocol_chat = await start_proto(get_context, '/green start')
    msg = protocol_chat.find_message_containing('Затрагивает ли инцидент безопасность сервиса или персональные данные?')
    assert msg is not None

    people = {i.username for i in protocol_chat.members}
    yes = msg.reply_markup.find_by_text('Да')
    assert yes is not None

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=msg,
        user=TUser('marty'),
        data=yes.callback_data
    ))

    escalate = protocol_chat.find_message_containing('выбери, кого призвать')
    await get_context.bot.call_callback_query(TCallbackQuery(
        message=escalate,
        user=TUser('marty'),
        data=escalate.reply_markup.find_by_text('dutyrole2').callback_data
    ))

    new_people = {i.username for i in protocol_chat.members} - people
    assert new_people == {'dutyuser2'}


@pytest.mark.asyncio
async def test_call_security_crisis(get_context):
    protocol_chat = await start_proto(get_context, '/green pr')
    msg = protocol_chat.find_message_containing('Затрагивает ли инцидент безопасность сервиса или персональные данные?')
    assert msg is not None

    people = {i.username for i in protocol_chat.members}
    yes = msg.reply_markup.find_by_text('Да')
    assert yes is not None

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=msg,
        user=TUser('marty'),
        data=yes.callback_data
    ))

    escalate = protocol_chat.find_message_containing('выбери, кого призвать')
    await get_context.bot.call_callback_query(TCallbackQuery(
        message=escalate,
        user=TUser('marty'),
        data=escalate.reply_markup.find_by_text('dutyrole2').callback_data
    ))

    new_people = {i.username for i in protocol_chat.members} - people
    assert new_people == {'dutyuser2'}
