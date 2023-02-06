import pytest

from mocks.context import temp_components, fast_component, temp_users, user
from mocks.bot import DChat, TChat, TUser, TMessage, TCallbackQuery

import sqlalchemy as sa

from bot.aiowarden import OnDuty, Functionality
from bot.api import Request
from bot.modules.protocols.models import Protocol


@pytest.fixture(scope='function', autouse=True)
def set_dependencies(get_context, proto_module):
    proto_module.data = get_context.data

    components = [
        fast_component(
            name='parent',
            owners=['test_parent_owner'],  # todo: create test user
            spi_chat='https://t.me/joinchat/AAAAAAAAAAAAAAAAAAAAAA',
            onduty=['test_parent_duty_a', 'test_parent_duty_b', OnDuty('Marty', 'marty', 'marty_duty')],
            functionalities=[Functionality('test_api_funct', 'Тестирование апи протоколов')],
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
        user('marty', is_marty=True),
        user('user')
    ):
        with temp_components(get_context, *components):
            yield None


@pytest.mark.asyncio
async def test_marty_line(get_context, proto_module):
    incident_id = 'parent@flwalkflawkd:2o13124124'
    request = Request({"Authorization": "Token fake"}, **{
        'incident_id': incident_id,
        'description': 'Тестирование апи протоколов',
        'functionality_id': 'test_api_funct',
        'check': {
            'service': 'test-api-service',
            'host': 'test-api-host'
        },
        'ticket_task_id': None,
        'test_mode': False,
    })

    await proto_module.proto_context.incidents.start_incident(request, ctx=None)
    async with await get_context.data.connect() as conn:
        protocol = await Protocol.filter(conn, sa.select([Protocol]).where(Protocol.incident_id == incident_id))
        protocol = protocol[0]
        assert protocol.is_marty_first_line


@pytest.mark.asyncio
async def test_manual(get_context):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('user'),
        chat=created_chat.chat,
        text='/proto'
    ))

    proto_chat = list(get_context.bot.chats.values())[-1]

    await get_context.bot.call_command(TMessage(
        user=TUser('user'),
        chat=proto_chat.chat,
        text='/proto component parent'
    ))

    keyboard = proto_chat.messages[-1]

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=keyboard,
        user=TUser('user'),
        data=keyboard.reply_markup.find_by_text('parent').callback_data
    ))

    async with await get_context.data.connect() as conn:
        protocol = await Protocol.get_by_chat(conn, proto_chat.chat.id)
        assert protocol.is_marty_first_line


@pytest.mark.asyncio
async def test_manual_determine_component(get_context):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('test_parent_owner'),
        chat=created_chat.chat,
        text='/proto'
    ))

    proto_chat = list(get_context.bot.chats.values())[-1]

    async with await get_context.data.connect() as conn:
        protocol = await Protocol.get_by_chat(conn, proto_chat.chat.id)
        assert protocol.is_marty_first_line
