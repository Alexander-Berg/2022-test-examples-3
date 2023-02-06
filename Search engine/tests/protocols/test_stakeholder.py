import pytest
from bot.aiowarden import ProtocolSettings
from bot.modules.protocols.models import ProtoPR, Protocol, StakeholderLink

from mocks.bot import DChat, TChat, TUser, TMessage
from mocks.bot.objects import TPhotoSize
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


@pytest.fixture(scope='function')
@pytest.mark.asyncio
async def crisis_with_stakeholder(get_context, proto_module) -> (DChat, DChat):
    proto_module.data = get_context.data

    private_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(private_chat)

    stakeholder_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(stakeholder_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=private_chat.chat,
        text='/crisis component2'
    ))
    protocol_chat = list(get_context.bot.chats.values())[-1]

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_chat(conn, protocol_chat.chat.id)
        pr = await ProtoPR.get(conn, proto.pr_id)
        pr.stakeholder_chat_id = stakeholder_chat.chat.id
        await pr.commit(conn, include={'stakeholder_chat_id'})

        stakeholder_link = await StakeholderLink.create(
            conn,
            chat_id=stakeholder_chat.chat.id,
            components=['component2'],
            created_by='test_parent_owner',
        )

    yield protocol_chat, stakeholder_chat

    async with await get_context.data.connect() as conn:
        await stakeholder_link.drop(conn)


@pytest.mark.asyncio
async def test_sendpr(get_context, crisis_with_stakeholder):
    protocol_chat, stakeholder_chat = crisis_with_stakeholder

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/sendpr To stakeholders.'
    ))

    assert 'To stakeholders' in stakeholder_chat.messages[-1].text


@pytest.mark.asyncio
async def test_sendpr_auto(get_context, crisis_with_stakeholder):
    protocol_chat, stakeholder_chat = crisis_with_stakeholder

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/send To stakeholders.'
    ))

    assert 'To stakeholders' in stakeholder_chat.messages[-1].text


@pytest.mark.asyncio
async def test_sendproto(get_context, crisis_with_stakeholder):
    protocol_chat, stakeholder_chat = crisis_with_stakeholder

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=stakeholder_chat.chat,
        text='/sendproto To protocol.'
    ))

    assert 'To protocol' in protocol_chat.messages[-1].text


@pytest.mark.asyncio
async def test_sendproto_auto(get_context, crisis_with_stakeholder):
    protocol_chat, stakeholder_chat = crisis_with_stakeholder

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=stakeholder_chat.chat,
        text='/send To protocol.'
    ))

    assert 'To protocol' in protocol_chat.messages[-1].text


@pytest.mark.asyncio
async def test_sendpr_photo(get_context, crisis_with_stakeholder):
    protocol_chat, stakeholder_chat = crisis_with_stakeholder

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/sendpr To stakeholders.',
        photo=[TPhotoSize('some_random_photo_id')]
    ))

    assert 'To stakeholders' in stakeholder_chat.messages[-2].text
    assert 'some_random_photo_id' == stakeholder_chat.messages[-1].photo[0].file_id


@pytest.mark.asyncio
async def test_sendpr_photo_auto(get_context, crisis_with_stakeholder):
    protocol_chat, stakeholder_chat = crisis_with_stakeholder

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=protocol_chat.chat,
        text='/send',
        photo=[TPhotoSize('some_random_photo_id')]
    ))

    assert 'some_random_photo_id' == stakeholder_chat.messages[-1].photo[0].file_id


@pytest.mark.asyncio
async def test_sendproto_photo(get_context, crisis_with_stakeholder):
    protocol_chat, stakeholder_chat = crisis_with_stakeholder

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=stakeholder_chat.chat,
        text='/sendproto',
        photo=[TPhotoSize('some_random_photo_id')]
    ))

    assert 'some_random_photo_id' == protocol_chat.messages[-1].photo[0].file_id


@pytest.mark.asyncio
async def test_sendproto_photo_auto(get_context, crisis_with_stakeholder):
    protocol_chat, stakeholder_chat = crisis_with_stakeholder

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=stakeholder_chat.chat,
        text='/send',
        photo=[TPhotoSize('some_random_photo_id')]
    ))

    assert 'some_random_photo_id' == protocol_chat.messages[-1].photo[0].file_id
