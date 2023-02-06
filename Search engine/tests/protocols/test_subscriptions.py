import pytest

from bot.aiowarden import Functionality

from bot.modules.subscribe import Subscribe
from bot.api import Request

from bot.modules.protocols import Protocols
from mocks.bot import DChat, TChat, TUser, TMessage, TCallbackQuery
from mocks.context import temp_components, fast_component, temp_users, user
from utils import full_wait_pending


@pytest.fixture(scope='session', autouse=True)
def register_subscribe(get_context):
    get_context.bot.register_modules(Subscribe(get_context, {}))


@pytest.fixture(scope='function', autouse=True)
def set_dependencies(get_context):
    components = [
        fast_component(
            name='parent',
            owners=['test_parent_owner'],  # todo: create test user
            spi_chat='https://t.me/joinchat/AAAAAAAAAAAAAAAAAAAAAA',
            functionalities=[Functionality('test_api_funct', 'Тестирование апи протоколов')],
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

    with temp_users(
        get_context,
        *(user(x) for x in _users),
        user('marty', is_marty=True)
    ):
        with temp_components(get_context, *components):
            yield None


async def subscribe(get_context, proto_module, commands):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    await get_context.auth.listen(dict(message=TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/start'
    ).json()))

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/subscribe'
    ))

    keyboard = created_chat.messages[-1]

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=keyboard,
        user=TUser('marty'),
        data='subscribe|proto'
    ))

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=keyboard,
        user=TUser('marty'),
        data='subscribe|proto:is_enabled$$:✅'
    ))

    for cmd in commands:
        await get_context.bot.call_callback_query(TCallbackQuery(
            message=keyboard,
            user=TUser('marty'),
            data=f'subscribe|proto:{cmd}$$:✅'
        ))

    await proto_module.proto_context.cast.load_subscribers()

    return created_chat, keyboard


@full_wait_pending
async def start_incident(module: Protocols) -> str:
    request = Request({"Authorization": "Token fake"}, **{
        'incident_id': 'parent@flwalkflawkd:2o13124124',
        'description': 'Тестирование апи протоколов',
        'functionality_id': 'test_api_funct',
        'check': {
            'service': 'test-api-service',
            'host': 'test-api-host'
        },
        'ticket_task_id': None,
        'test_mode': False,
    })

    res = await module.proto_context.incidents.start_incident(request, ctx=None)

    return res


@pytest.mark.asyncio
@full_wait_pending
async def test_subscribe(get_context, proto_module):
    created_chat, keyboard = await subscribe(get_context, proto_module, ['enable_all'])

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto start parent'
    ))

    assert 'запустил протокол координации инцидента' in created_chat.messages[-1].text


@pytest.mark.asyncio
@full_wait_pending
async def test_subscribe_autoincident(get_context, proto_module):
    created_chat, keyboard = await subscribe(get_context, proto_module, ['enable_all'])
    await start_incident(proto_module)
    assert 'запустил протокол координации инцидента' in created_chat.messages[-1].text


@pytest.mark.asyncio
@full_wait_pending
async def test_subscribe_component_set(get_context, proto_module):
    created_chat, keyboard = await subscribe(get_context, proto_module, ['enable_all'])

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto start'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]

    assert 'запустил протокол координации инцидента' not in created_chat.messages[-1].text
    assert 'указал компоненту инцидента' not in created_chat.messages[-1].text

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/proto component parent'
    ))

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=protocol_chat.messages[-1],
        user=TUser('marty'),
        data='protocols|pick_component:pick:1:4658263254135485168'
    ))

    assert 'указал компоненту инцидента' in created_chat.messages[-1].text


@pytest.mark.asyncio
@full_wait_pending
async def test_subscribe_only_start(get_context, proto_module):
    created_chat, keyboard = await subscribe(get_context, proto_module, ['enable_all', 'only_start'])

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto start parent'
    ))

    protocol_chat = list(get_context.bot.chats.values())[-1]

    assert 'запустил протокол координации инцидента' in created_chat.messages[-1].text

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/martycast Установил позицию инцидента'
    ))

    assert 'Установил позицию инцидента' not in created_chat.messages[-1].text

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=keyboard,
        user=TUser('marty'),
        data='subscribe|proto:only_start$$:❌'
    ))

    await proto_module.proto_context.cast.load_subscribers()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/martycast Установил другую позицию инцидента'
    ))

    assert 'Установил другую позицию инцидента' in created_chat.messages[-1].text


@pytest.mark.asyncio
@full_wait_pending
async def test_subscribe_only_manual(get_context, proto_module):
    created_chat, keyboard = await subscribe(get_context, proto_module, ['enable_all', 'only_manual'])

    await start_incident(proto_module)

    assert 'запустил протокол координации инцидента' not in created_chat.messages[-1].text

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto start parent'
    ))

    assert 'запустил протокол координации инцидента' in created_chat.messages[-1].text
