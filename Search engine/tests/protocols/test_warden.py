import pytest

from bot.aiowarden import Functionality, OnDuty
from mocks.bot import DChat, TChat, TMessage, TUser, TCallbackQuery
from mocks.context import fast_component, temp_users, user, temp_components


@pytest.fixture(scope='session', autouse=True)
def register_warden(get_context):
    get_context.bot.register_modules(get_context.warden)


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


@pytest.mark.asyncio
async def test_notify_changes(get_context):
    component = get_context.warden._components[('', 'parent')]

    marty_id = 12345
    created_chat = DChat(TChat(id=marty_id, type='private'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/catalogset'
    ))
    assert len(created_chat.messages) == 1

    # Subscribe
    keyboard = created_chat.messages[-1]
    await get_context.bot.call_callback_query(TCallbackQuery(
        message=keyboard,
        user=TUser('marty'),
        data='warden|catalog:parent:enable_all'
    ))

    # Setup chat
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/catalogonchange'
    ))
    assert len(created_chat.messages) == 2

    keyboard = created_chat.messages[-1]
    await get_context.bot.call_callback_query(TCallbackQuery(
        message=keyboard,
        user=TUser('marty'),
        data='warden|catalog:onchangeparent/'
    ))
    await get_context.bot.call_callback_query(TCallbackQuery(
        message=keyboard,
        user=TUser('marty'),
        data='warden|catalog:onchangepin'
    ))
    await get_context.bot.call_callback_query(TCallbackQuery(
        message=keyboard,
        user=TUser('marty'),
        data='warden|catalog:onchangeenable'
    ))
    await get_context.warden.fetch_subscribers()

    # Initialize catalog
    await get_context.warden.notify_changes(component)
    assert len(created_chat.messages) == 2

    # Modify component:
    component.onduty.append(OnDuty(role='dutyrole', login='marty'))

    # 3 tries
    for _ in range(2):
        await get_context.warden.notify_changes(component)
        assert len(created_chat.messages) == 2
    await get_context.warden.notify_changes(component)
    assert len(created_chat.messages) == 3

    # Check for message
    changed = created_chat.messages[-1]
    assert 'У сервиса обновился список дежурных.' in changed.text
    assert created_chat.pinned_message == changed.message_id
