import pytest

from mocks.bot import DChat, TChat, TUser, TMessage
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
        user('marty2', is_marty=True)
    ):
        with temp_components(get_context, *components):
            yield None


@pytest.mark.asyncio
async def test_marty_update(get_context, proto_module):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)
    proto_module.data = get_context.data
    await get_context.modules.marty.update_marty()

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/proto start'
    ))

    proto_chat = list(get_context.bot.chats.values())[-1]

    get_context.shiftinator.marty = 'marty2'
    await get_context.modules.marty.update_marty()

    assert 'marty2' in proto_chat.messages[1].text  # новый марти обновился в ролях
    assert 'Смена дежурства марти' in proto_chat.messages[-1].text  # оповещение добавлено в чат
