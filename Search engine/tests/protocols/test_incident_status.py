import pytest

import sqlalchemy as sa

from bot.modules.protocols.models import ProtoTimer, Protocol
from bot.modules.protocols import timers
from bot.modules.protocols.const.incidents import IncidentStatus

from bot.aiowarden import Functionality
from bot.api import Request

from mocks.bot import DChat, TChat, TUser, TMessage, TCallbackQuery
from mocks.context import temp_components, fast_component, temp_users, user


@pytest.fixture(scope='function', autouse=True)
def set_dependencies(get_context, proto_module):
    proto_module.data = get_context.data
    get_context.startrek._spies.clear()

    components = [
        fast_component(
            name='parent',
            owners=['test_parent_owner'],  # todo: create test user
            spi_chat='https://t.me/joinchat/AAAAAAAAAAAAAAAAAAAAAA',
            onduty=['test_parent_duty_a', 'test_parent_duty_b'],
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
        user('marty', is_marty=True)
    ):
        with temp_components(get_context, *components):
            yield None


async def start_protocol(get_context):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto'
    ))

    proto_chat = list(get_context.bot.chats.values())[-1]
    return proto_chat


@pytest.mark.asyncio
async def test_diagnostics_manualy(get_context):
    proto_chat = await start_protocol(get_context)

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_chat(conn, proto_chat.chat.id)
        assert proto.incident_status == IncidentStatus.diagnostics


@pytest.mark.asyncio
async def test_diagnostics_auto(get_context, proto_module):
    incident_id = 'parent@flwalkflawkd:2o13124124'
    ticket = 'SPI-0000'
    request = Request({"Authorization": "Token fake"}, **{
        'incident_id': incident_id,
        'description': 'Тестирование апи протоколов',
        'functionality_id': 'test_api_funct',
        'check': {
            'service': 'test-api-service',
            'host': 'test-api-host'
        },
        'ticket_task_id': ticket,
        'test_mode': False,
    })

    await proto_module.proto_context.incidents.start_incident(request, ctx=None)
    async with await get_context.data.connect() as conn:
        protocols = await Protocol.filter(conn, sa.select([Protocol]).where(Protocol.incident_id == incident_id))
        proto = protocols[0]
        assert proto.incident_status is None
    proto_chat = get_context.bot.chats[proto.chat_id]
    pin = proto_chat.messages[-1]

    button = pin.reply_markup.find_by_text('Принять инцидент')
    await get_context.bot.call_callback_query(TCallbackQuery(
        message=pin,
        user=TUser('marty'),
        data=button.callback_data
    ))

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get(conn, proto.id)
        assert proto.incident_status is IncidentStatus.diagnostics

    assert get_context.startrek._spies[ticket]['status'] == 'diagnostics'

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='/proto status все ок'
    ))

    assert get_context.startrek._spies[ticket]['status'] == 'serviceRestored'


@pytest.mark.asyncio
async def test_service_restored(get_context):
    proto_chat = await start_protocol(get_context)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='/proto status Сервис восстановлен'
    ))

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_chat(conn, proto_chat.chat.id)
        assert proto.incident_status == IncidentStatus.service_restored
        assert not await ProtoTimer.get(conn, timers.SetETA.key, proto.id)
        assert not await ProtoTimer.get(conn, timers.ETADismissed.key, proto.id)


@pytest.mark.asyncio
async def test_service_restored_by_finish(get_context):
    proto_chat = await start_protocol(get_context)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='/proto finish'
    ))

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_chat(conn, proto_chat.chat.id, only_running=False)
        assert proto.incident_status == IncidentStatus.service_restored


@pytest.mark.asyncio
async def test_pin_messages(get_context):
    proto_chat = await start_protocol(get_context)

    pin = proto_chat.messages[1]
    assert 'Диагностика' in pin.text

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='/proto status не воспроизводится'
    ))

    assert 'Не воспроизводится' in pin.text

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='/proto status все ок'
    ))

    assert 'Сервис восстановлен' in pin.text
