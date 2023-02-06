from datetime import datetime
from typing import NamedTuple, Type

import pytest

from bot.aiowarden import Functionality, ProtocolSettings
from bot.modules.context import Context
from bot.modules.protocols import actions, types, Protocols, const
from bot.modules.protocols.escalations import escalations
from bot.modules.protocols.models import Protocol, ProtoLevel, ProtoIncident, ProtoChat
from mocks.bot import DChat, TChat, temp_chats
from mocks.context import temp_components, fast_component, temp_users, user

COMPONENT_CHAT_ID = -100911999912
PROTO_CHAT_ID = -100949281951
COMPONENT_JOIN_URL = 'https://t.me/joinchat/AAAAAAAAAAAAAAAAAAAAAA'
PROTO_JOIN_URL = 'https://t.me/joinchat/BBBBBBBBBBBBBBBBBBBBBB'


@pytest.fixture(scope='function', autouse=True)
@pytest.mark.asyncio
def prepare_environment(get_context, monkeypatch):
    components = [
        fast_component(
            name='test-component',
            owners=['test-owner'],
            spi_chat=COMPONENT_JOIN_URL,
            onduty=['onduty-one', 'onduty-two'],
            support=['support-duty', 'smm-duty'],
            pr=['pr-duty'],
            functionalities=[Functionality('test-funct', 'тестовая функциональность')],
            curators=['test-curator'],
            proto_settings=ProtocolSettings(extra_responsible=['test-manager'])
        ),
        fast_component(
            name='test-component-with-flow',
            onduty=['onduty-one', 'onduty-two'],
            flow=['onduty-one'],
        )
    ]

    with temp_users(
        get_context,
        user('test-owner'),
        user('onduty-one'),
        user('onduty-two'),
        user('support-duty'),
        user('smm-duty'),
        user('pr-duty'),
        user('test-curator'),
        user('test-manager'),
        user('marty', is_marty=True),
        user('reserve-marty-one', is_marty=True),
        user('reserve-marty-two', is_marty=True),
    ):
        monkeypatch.setattr(get_context.modules.marty, '_current', get_context.auth._by_login['marty'])
        monkeypatch.setattr(get_context.modules.marty, '_reserve', [get_context.auth._by_login[k] for k in ('reserve-marty-one', 'reserve-marty-two')])

        async def _init(*args, **kwargs):
            return

        monkeypatch.setattr(get_context.modules.marty, '_init_marty', _init)
        # get_context.modules.marty._init_marty.set_result(None)

        with temp_components(get_context, *components):
            with temp_chats(get_context, [
                DChat(TChat(COMPONENT_CHAT_ID, title='component chat'), is_bot_member=True, is_robot_member=True, invite_link=COMPONENT_JOIN_URL),
                DChat(TChat(PROTO_CHAT_ID, title='protocol chat'), is_bot_member=True, is_robot_member=True, invite_link='')
            ]):
                yield


async def start_incident(get_context, component_name: str) -> (Protocol, ProtoIncident):
    start_point = datetime.now()
    async with await get_context.data.connect() as conn:
        incident = await ProtoIncident.create(
            conn,
            id='test-proto-incident:812851kaaffjasdk',
            functionality_id='test-funct',
            component_name=component_name,
            description='тестовая функциональность',
            received_at=start_point,
            juggler_check=types.JugglerCheck('test-host-alert', 'test-service-alert')
        )

        pin_message = await get_context.bot.send_message(PROTO_CHAT_ID, 'запин')
        proto_chat = await ProtoChat.create(
            conn,
            id=int(str(PROTO_CHAT_ID)[4:]),
            join_url=PROTO_JOIN_URL,
            title='👻 тестовая функциональность',
            pin_message_id=pin_message['result']['message_id'],
            pin_message_text='запин'
        )
        proto = await Protocol.create(
            conn,
            summary='test-protocol',
            chat_id=PROTO_CHAT_ID,
            robot_username='mock_robot',
            level=ProtoLevel.green,
            component_name=component_name,
            incident_id=incident.id,
            started_at=start_point,
            _chat=proto_chat.record_id,
        )

    yield proto, incident


@pytest.fixture(scope='function')
@pytest.mark.asyncio
async def protocol_incident(get_context) -> (Protocol, ProtoIncident):
    async for i in start_incident(get_context, 'test-component'):
        yield i


@pytest.fixture(scope='function')
@pytest.mark.asyncio
async def protocol_incident_with_flow(get_context) -> (Protocol, ProtoIncident):
    async for i in start_incident(get_context, 'test-component-with-flow'):
        yield i


class ExpectedResult(NamedTuple):
    escalation: Type[escalations.BaseEscalation]
    escalation_result: dict
    proto_id: int
    chat_id: int
    text: str
    action: Type[actions.BaseAction] = None
    role_logins: list = None
    call_point_attr: str = None
    should_reply_pin: bool = False


async def assert_escalation(module: Protocols, context: Context, expected: ExpectedResult):
    assert bool(expected.escalation_result)

    message_id = expected.escalation_result.get('result', {}).get('message_id', None)
    chat: DChat = context.bot.find_chat(expected.chat_id)

    message = None
    for m in chat.messages:
        if m.message_id == message_id:
            message = m
            break

    assert bool(message)
    assert message.text == expected.text

    async with await context.data.connect() as conn:
        proto = await Protocol.get(conn, expected.proto_id)
        proto_chat = await proto.chat(conn)

        if expected.should_reply_pin:
            assert message.reply_to_message.message_id == proto_chat.pin_message_id

        if expected.escalation.full_buttons:
            assert len(proto.keyboard_messages) == 1
            for chat_id, message_id in proto.keyboard_messages:
                assert chat_id == proto.chat_id
                assert message_id == message.message_id

        elif expected.escalation.marty_buttons:
            assert proto.martychat_message_id == message.message_id
            assert message.chat.id == module.proto_context.config.martychat

        if expected.escalation.role:
            role = expected.escalation.role
            roles = await proto.roles(conn)
            chat: DChat = context.bot.find_chat(proto.chat_id)

            attr = getattr(roles, role.value)
            if role in const.roles.ONE_PER_ROLE:
                assert attr.login == expected.role_logins[0]
                if attr.login in chat.admins:
                    assert chat.admin_ranks[attr.login] == role.human_single
            else:
                assert len(attr) == len(expected.role_logins)
                for role_value in attr:
                    assert role_value.login in expected.role_logins
                    if role_value.login in chat.admins:
                        assert chat.admin_ranks[role_value.login] == role.human_single

        if expected.call_point_attr:
            incident = await ProtoIncident.get(conn, proto.incident_id)

            call_point = getattr(incident, expected.call_point_attr)
            assert bool(call_point)

        if expected.action:
            actions_list = await actions.filter_actions(conn, proto.id, expected.action)
            assert len(actions_list) == 1


@pytest.mark.asyncio
async def test_marty_escalation(get_context, protocol_incident, proto_module):
    protocol, incident = protocol_incident

    result = await escalations.Marty.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)

    await assert_escalation(proto_module, get_context, ExpectedResult(
        escalation=escalations.Marty,
        escalation_result=result,
        proto_id=protocol.id,
        chat_id=protocol.chat_id,
        text=('#proto #escalation\n'
              'Эскалировано на Марти\n\n'
              'cc @marty'),
        action=actions.EscalatedToMarty,
        role_logins=['marty'],
        call_point_attr='marty_escalated_at',
        should_reply_pin=True
    ))


@pytest.mark.asyncio
async def test_reserve_marty_escalation(get_context, protocol_incident, proto_module):
    protocol, incident = protocol_incident

    result = await escalations.MartyReserve.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)

    await assert_escalation(proto_module, get_context, ExpectedResult(
        escalation=escalations.MartyReserve,
        escalation_result=result,
        proto_id=protocol.id,
        chat_id=proto_module.proto_context.config.internal_martychat,
        text=(f'#proto #escalation'
              f'\nЭскалировано на резервных Марти'
              f'\nЧат: <a href="{PROTO_JOIN_URL}">👻 тестовая функциональность</a>'
              f'\nКомпонента: <a href="https://warden.z.yandex-team.ru/components/test-component">test-component</a>'
              f'\n\ncc @reserve-marty-one @reserve-marty-two'),
        action=actions.Escalated,
        role_logins=['reserve-marty-one', 'reserve-marty-two'],
        should_reply_pin=False
    ))


@pytest.mark.asyncio
async def test_duty_escalation(get_context, protocol_incident, proto_module):
    protocol, incident = protocol_incident

    result = await escalations.Duty.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)

    await assert_escalation(proto_module, get_context, ExpectedResult(
        escalation=escalations.Duty,
        escalation_result=result,
        proto_id=protocol.id,
        chat_id=PROTO_CHAT_ID,
        text=('#proto #escalation'
              '\nЭскалировано на дежурных'
              '\n\ncc @onduty-one @onduty-two'),
        action=actions.Escalated,
        role_logins=['onduty-one', 'onduty-two'],
        call_point_attr='duty_escalated_at',
        should_reply_pin=True
    ))


@pytest.mark.asyncio
async def test_flow_escalation_with_flow(get_context, protocol_incident_with_flow, proto_module):
    protocol, incident = protocol_incident_with_flow

    result = await escalations.Duty.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)

    await assert_escalation(proto_module, get_context, ExpectedResult(
        escalation=escalations.Duty,
        escalation_result=result,
        proto_id=protocol.id,
        chat_id=PROTO_CHAT_ID,
        text=('#proto #escalation'
              '\nЭскалировано на дежурных'
              '\n\ncc @onduty-one'),
        action=actions.Escalated,
        role_logins=['onduty-one'],
        call_point_attr='duty_escalated_at',
        should_reply_pin=True
    ))


@pytest.mark.asyncio
async def test_component_owner_escalation(get_context, protocol_incident, proto_module):
    protocol, incident = protocol_incident

    result = await escalations.ComponentOwner.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)

    await assert_escalation(proto_module, get_context, ExpectedResult(
        escalation=escalations.ComponentOwner,
        escalation_result=result,
        proto_id=protocol.id,
        chat_id=PROTO_CHAT_ID,
        text=('#proto #escalation'
              '\nЭскалировано на ответственных за компоненту'
              '\n\ncc @test-owner'),
        action=actions.Escalated,
        role_logins=['test-owner'],
        should_reply_pin=True
    ))


@pytest.mark.asyncio
async def test_vertical_escalation(get_context, protocol_incident, proto_module):
    protocol, incident = protocol_incident

    result = await escalations.Vertical.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)

    await assert_escalation(proto_module, get_context, ExpectedResult(
        escalation=escalations.Vertical,
        escalation_result=result,
        proto_id=protocol.id,
        chat_id=PROTO_CHAT_ID,
        text=('#proto #escalation'
              '\nЭскалировано на ответственных за вертикаль'
              '\n\ncc @test-owner'),
        action=actions.Escalated,
        role_logins=['test-owner'],
        call_point_attr='vertical_escalated_at',
        should_reply_pin=True
    ))


@pytest.mark.asyncio
async def test_curator_escalation(get_context, protocol_incident, proto_module):
    protocol, incident = protocol_incident

    result = await escalations.Curator.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)

    await assert_escalation(proto_module, get_context, ExpectedResult(
        escalation=escalations.Curator,
        escalation_result=result,
        proto_id=protocol.id,
        chat_id=PROTO_CHAT_ID,
        text=('#proto #escalation'
              '\nЭскалировано на кураторов компоненты'
              '\n\ncc @test-curator'),
        action=actions.Escalated,
        role_logins=['test-curator'],
        should_reply_pin=True
    ))


@pytest.mark.asyncio
async def test_managers_escalation(get_context, protocol_incident, proto_module):
    protocol, incident = protocol_incident

    result = await escalations.Managers.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)

    await assert_escalation(proto_module, get_context, ExpectedResult(
        escalation=escalations.Managers,
        escalation_result=result,
        proto_id=protocol.id,
        chat_id=PROTO_CHAT_ID,
        text=('#proto #escalation'
              '\nЭскалировано на менеджеров стабильности компоненты'
              '\n\ncc @test-manager'),
        action=actions.Escalated,
        role_logins=['test-manager'],
        should_reply_pin=True
    ))


@pytest.mark.asyncio
async def test_stability_managers_escalation(get_context, protocol_incident, proto_module):
    protocol, incident = protocol_incident

    result = await escalations.StabilityManagers.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)

    await assert_escalation(proto_module, get_context, ExpectedResult(
        escalation=escalations.StabilityManagers,
        escalation_result=result,
        proto_id=protocol.id,
        chat_id=proto_module.proto_context.config.managers_chat,
        text=('#proto #escalation'
              '\nКритичный инцидент (достигнут уровень потерь в 10 секунд YDT)'
              '\nДежурный не принял инцидент в работу.'
              f'\nЧат: <a href="{PROTO_JOIN_URL}">👻 тестовая функциональность</a>'
              '\nКомпонента: <a href="https://warden.z.yandex-team.ru/components/test-component">test-component</a>'),
        action=actions.Escalated,
    ))


@pytest.mark.asyncio
async def test_marty_managers_escalation(get_context, protocol_incident, proto_module):
    protocol, incident = protocol_incident

    result = await escalations.MartyManagers.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)

    await assert_escalation(proto_module, get_context, ExpectedResult(
        escalation=escalations.MartyManagers,
        escalation_result=result,
        proto_id=protocol.id,
        chat_id=proto_module.proto_context.config.seniormarty_chat,
        text=('#proto #escalation'
              '\nМарти не успел взять инцидент в работу'
              f'\nЧат: <a href="{PROTO_JOIN_URL}">👻 тестовая функциональность</a>'
              '\nКомпонента: <a href="https://warden.z.yandex-team.ru/components/test-component">test-component</a>'),
        action=actions.Escalated,
    ))


@pytest.mark.asyncio
async def test_marty_notification_escalation(get_context, protocol_incident, proto_module):
    protocol, incident = protocol_incident

    result = await escalations.MartyNotification.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)

    await assert_escalation(proto_module, get_context, ExpectedResult(
        escalation=escalations.MartyNotification,
        escalation_result=result,
        proto_id=protocol.id,
        chat_id=proto_module.proto_context.config.martychat,
        text=('#proto #escalation'
              '\nЭскалировано на Марти'
              f'\nЧат: <a href="{PROTO_JOIN_URL}">👻 тестовая функциональность</a>'
              '\nКомпонента: <a href="https://warden.z.yandex-team.ru/components/test-component">test-component</a>'
              '\n\ncc @marty'),
    ))


@pytest.mark.asyncio
async def test_duty_notification_escalation(get_context, protocol_incident, proto_module):
    protocol, incident = protocol_incident

    result = await escalations.DutyNotification.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)

    await assert_escalation(proto_module, get_context, ExpectedResult(
        escalation=escalations.DutyNotification,
        escalation_result=result,
        proto_id=protocol.id,
        chat_id=COMPONENT_CHAT_ID,
        role_logins=['onduty-one', 'onduty-two'],
        text=('#proto #escalation'
              f'\nСработала проверка: {incident.description}'
              f'\nЧат: <a href="{PROTO_JOIN_URL}">👻 тестовая функциональность</a>'
              '\nКомпонента: <a href="https://warden.z.yandex-team.ru/components/test-component">test-component</a>'
              '\n\ncc @onduty-one @onduty-two'),
    ))


@pytest.mark.asyncio
async def test_escalation_on_protocol_without_component(get_context, protocol_incident, proto_module):
    protocol, incident = protocol_incident
    async with await get_context.data.connect() as conn:
        protocol.component_name = None
        await protocol.commit(conn, include={'component_name'})

    await escalations.ComponentOwner.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)
    await escalations.Curator.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)
    await escalations.Managers.send(get_context, protocol, incident, proto_module._callback, proto_module.proto_context, raise_errors=True)
