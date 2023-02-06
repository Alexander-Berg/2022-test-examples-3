from datetime import datetime

import pytest
import sqlalchemy as sa

from bot.aiowarden import Functionality, ProtocolSettings
from bot.api import Request
from bot.modules.protocols import Protocols, Protocol, ProtoIncident
from bot.modules.protocols.escalations import by_key
from bot.modules.protocols.models import ProtoMartyReport
from mocks.bot import DChat, TMessage, TUser, TChat
from mocks.clients.aioabc import MockABC
from mocks.context import temp_components, fast_component, temp_users, user
from utils import full_wait_pending
import protocols.const as const


@pytest.fixture(scope='function', autouse=True)
@pytest.mark.asyncio
def context(get_context, monkeypatch):
    with temp_components(
        get_context,
        fast_component(
            name='web',
            owners=['owner'],
            spi_chat='https://t.me/joinchat/aksdji1250090_ads1',
            onduty=['onduty', 'onduty2'],
            curators=['curator'],
            functionalities=[Functionality('test_api_funct', 'Тестирование апи протоколов')],
            proto_settings=ProtocolSettings(not_create_new_chat=False, extra_responsible=['protoresponsible']),
            abc_slug='test_web_slug'
        )
    ):
        users = [
            user('marty', is_marty=True),
            user('owner'),
            user('curator'),
            user('protoresponsible'),
            user('onduty'),
            user('onduty2'),
            user('webchief'),
            user('websubcto'),
            user('webcto'),
            user('webtop'),
        ]
        with temp_users(get_context, *users):

            async def list_chiefs(*args, **kwargs):
                return users[4:]

            async def list_members(*args, **kwargs):
                return [MockABC.member('webchief', 'services_management'),
                        MockABC.member('onduty', 'duty'),
                        MockABC.member('onduty2', 'duty')]

            monkeypatch.setattr(get_context.abc, 'list_members', list_members)
            monkeypatch.setattr(get_context.staff, 'list_chiefs', list_chiefs)

            get_context.modules.marty._current = users[0]

            # disable background tasks
            monkeypatch.setenv('NO_TASKS_RUN', '1')
            try:
                yield None
            finally:
                get_context.modules.marty._current = None
                return


async def start_incident(module: Protocols) -> str:
    request = Request({"Authorization": "Token fake"}, **{
        'incident_id': INCIDENT_ID,
        'description': 'Тестирование апи протоколов',
        'functionality_id': 'test_api_funct',
        'check': {
            'service': 'test-api-service',
            'host': 'test-api-host'
        },
        'ticket_task_id': None,
        'test_mode': False,
    })

    result = await module.proto_context.incidents.start_incident(request, ctx=None)
    return result['etag']


INCIDENT_ID = 'web@flwalkflawkd:2o13124124'


@pytest.mark.asyncio
@full_wait_pending
async def test_api_finish(get_context, proto_module, prepare_database):
    etag = await start_incident(proto_module)
    request = Request({"Authorization": "Token fake"}, **{
        'incident_id': INCIDENT_ID,
        'etag': etag,
        'reason': 'Тестирование API',
        'finished_by': 'marty'
    })

    await proto_module.proto_context.incidents.auto_finish(request, ctx=None)

    async with await get_context.data.connect() as conn:
        protocol = await Protocol.filter(conn, sa.select([Protocol]).where(Protocol.incident_id == INCIDENT_ID))
        protocol = protocol[0]
        incident = await ProtoIncident.get(conn, INCIDENT_ID)
        marty_report = await ProtoMartyReport.get(conn, protocol.id)

    assert incident.finished_by == request.finished_by
    assert protocol.finished_by == request.finished_by
    assert not protocol.in_process
    assert protocol.finished_at
    assert incident.finished_at
    assert marty_report
    assert protocol.etag != etag


@pytest.mark.asyncio
@full_wait_pending
async def test_api_manual_finish(get_context, proto_module, prepare_database):
    await start_incident(proto_module)

    async with await get_context.data.connect() as conn:
        incident: ProtoIncident = await ProtoIncident.get(conn, INCIDENT_ID)
        incident.duty_accepted_at = datetime.now()
        incident.accepted_duty = 'marty'
        await incident.commit(conn, include={'duty_accepted_at', 'accepted_duty'})
        protocol = await Protocol.filter(conn, sa.select([Protocol]).where(Protocol.incident_id == INCIDENT_ID))
        protocol = protocol[0]

    proto_module.data = get_context.data

    chat: DChat = get_context.bot.find_chat(protocol.chat_id)
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=chat.chat,
        text='/proto finish'
    ))

    # чаты в тестах пересекаются и нельзя надеяться, что сообщение будет на определенной позиции
    protocol_pins = filter(lambda m: incident.id in m.text and str(chat.chat.id) in m.text, get_context.bot.find_chat(const.LOGCHAT).messages)
    log_updated = all(map(lambda m: 'завершен по инициативе' in m.text.lower(), protocol_pins))
    protocol_updated = 'завершен по инициативе' in chat.messages[1].text
    assert log_updated and protocol_updated


@pytest.mark.asyncio
@full_wait_pending
async def test_api_send_alert_graph(get_context, proto_module):
    get_context.warden._components[('', 'web')].protocol_settings.send_alert_graph = True
    await start_incident(proto_module)

    async with await get_context.data.connect() as conn:
        protocol = await Protocol.filter(conn, sa.select([Protocol]).where(Protocol.incident_id == INCIDENT_ID))
        protocol = protocol[0]

    chat: DChat = get_context.bot.find_chat(protocol.chat_id)

    photo_found = False
    for m in chat.messages:
        if 'График алерта' in m.text:
            photo_found = True
            break

    assert photo_found


@pytest.mark.asyncio
@full_wait_pending
async def test_api_ydt_post(get_context, proto_module, prepare_database):
    etag = await start_incident(proto_module)
    request = Request({"Authorization": "Token fake"}, **{
        'ydt': 0.1412145125,
        'incident_id': INCIDENT_ID,
    })

    await proto_module.proto_context.incidents.post_ydt(request, ctx=None)

    async with await get_context.data.connect() as conn:
        protocol = await Protocol.filter(conn, sa.select([Protocol]).where(Protocol.incident_id == INCIDENT_ID))
        protocol = protocol[0]

    assert protocol.etag == etag
    assert protocol.ydt == 0.14121


@pytest.mark.asyncio
@full_wait_pending
async def test_api_ydt_escalate(get_context, proto_module, prepare_database):
    etag = await start_incident(proto_module)
    request = Request({"Authorization": "Token fake"}, **{
        'incident_id': INCIDENT_ID,
        'etag': etag,
        'ydt': 20.0,
        'level': -3,
    })

    await proto_module.proto_context.incidents.escalate(request, ctx=None)
    async with await get_context.data.connect() as conn:
        incident: ProtoIncident = await ProtoIncident.get(conn, INCIDENT_ID)

    assert len(incident.escalation_request_messages) == 2
    for chat_id, message_id in incident.escalation_request_messages:
        chat: DChat = get_context.bot.find_chat(chat_id)
        assert chat

        message = chat.get_message(message_id)
        assert message.reply_markup.inline_keyboard
        assert len(message.reply_markup.inline_keyboard[0]) == 2


@pytest.mark.asyncio
@full_wait_pending
async def test_api_ticket_task(get_context, proto_module, prepare_database):
    proto_module.data = get_context.data
    etag = await start_incident(proto_module)
    request = Request({"Authorization": "Token fake"}, **{
        'incident_id': INCIDENT_ID,
        'etag': etag,
        'ticket_task_id': 'tiasltakdaskd_safkadas214'
    })

    await proto_module.append_ticket_task(request, ctx=None)
    async with await get_context.data.connect() as conn:
        protocol = await Protocol.filter(conn, sa.select([Protocol]).where(Protocol.incident_id == INCIDENT_ID))
        protocol = protocol[0]

    assert protocol.ticket_task_id == request.ticket_task_id


@pytest.mark.asyncio
@pytest.mark.parametrize("escalation_key", list(by_key.keys()))
@full_wait_pending
async def test_api_escalate(get_context, proto_module, prepare_database, escalation_key):
    etag = await start_incident(proto_module)
    request = Request({"Authorization": "Token fake"}, **{
        'incident_id': INCIDENT_ID,
        'etag': etag,
        'escalation_key': escalation_key,
        'escalations': [],
        'custom_message': '',
    })

    await proto_module.proto_context.escalations.external_trigger(request, ctx=None)


@pytest.mark.asyncio
@full_wait_pending
async def test_api_complex_escalations(get_context, proto_module, prepare_database):
    etag = await start_incident(proto_module)

    request = Request({"Authorization": "Token fake"}, **{
        'incident_id': INCIDENT_ID,
        'etag': etag,
        'escalation_key': '',
        'escalations': ['curator', 'managers', 'owner'],
        'custom_message': '',
    })

    result = await proto_module.proto_context.escalations.external_trigger(request, ctx=None)
    assert not isinstance(result, list)

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_incident(conn, INCIDENT_ID)

    assert proto.etag != etag
    chat: DChat = get_context.bot.find_chat(proto.chat_id)

    assert len(chat.messages) == 5


@pytest.mark.asyncio
@full_wait_pending
async def test_api_escalations_custom_message(get_context, proto_module, prepare_database):
    etag = await start_incident(proto_module)

    custom_message = 'Проверка тестового сообщения'
    request = Request({"Authorization": "Token fake"}, **{
        'incident_id': INCIDENT_ID,
        'etag': etag,
        'escalation_key': '',
        'escalations': ['curator', 'managers', 'owner', 'duty'],
        'custom_message': 'Проверка тестового сообщения в эскалациях',
    })

    result = await proto_module.proto_context.escalations.external_trigger(request, ctx=None)
    assert not isinstance(result, list)

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_incident(conn, INCIDENT_ID)

    assert proto.etag != etag
    chat: DChat = get_context.bot.find_chat(proto.chat_id)

    assert len(chat.messages) == 6
    for m in chat.messages[-4:]:
        assert custom_message in m.text


@pytest.mark.asyncio
@full_wait_pending
async def test_api_statistics_returns_protocols_without_component(get_context, proto_module, prepare_database):
    # YAINCBOT-1501
    await start_incident(proto_module)

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_incident(conn, INCIDENT_ID)
        proto.component_name = None
        proto.parent_component_name = None
        await proto.commit(conn)

    req = Request({}, **{
        'filters': {
            'protocol_ids': [],
            'incident_ids': [INCIDENT_ID],
            'timestamp_start': None,
            'timestamp_end': None,
            'only_finished': False,
            'only_running': False,
            'include_test_mode': False,
            'components': []
        },
        'reverse': False,
        'limit': 100500,
        'offset': 0,
        'full_dump': False
    })
    resp = await proto_module.proto_context.statistics.dump(req)
    assert len(resp) == 1
    assert resp[0]['incident_id'] == INCIDENT_ID


@pytest.mark.asyncio
async def test_statistics_secured(get_context, proto_module):
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
        text='/proto component web'
    ))

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto secure'
    ))

    req = Request({}, **{
        'filters': None,
        'reverse': False,
        'limit': 100500,
        'offset': 0,
        'full_dump': False
    })
    resp = await proto_module.proto_context.statistics.dump(req)
    dump = resp[0]

    for field in Protocol.secure_fields:
        assert field not in dump

    assert dump['pr'] is True
    assert 'marty' in dump['roles']
