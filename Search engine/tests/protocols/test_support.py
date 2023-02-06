import pytest

from mocks.bot import DChat, TChat, TUser, TMessage, TCallbackQuery
from mocks.context import temp_users, user


@pytest.fixture(scope='function', autouse=True)
def set_dependencies(get_context, proto_module):
    get_context.bot.register_modules(get_context.modules.support)

    proto_module.data = get_context.data
    with temp_users(
        get_context,
        user('marty', is_marty=True),
        user('other'),
    ):
        yield None

    get_context.tickenator.queues.clear()


async def link_queues_to_chat(get_context, chat):
    queues = ('TEST', 'TESTSPI', 'NOCREQUESTS')
    get_context.modules.support.init_queues(queues)
    for queue in queues:
        await get_context.bot.call_command(TMessage(
            user=TUser('marty'),
            chat=chat.chat,
            text=f'/set_queue {queue}'
        ))


@pytest.mark.asyncio
async def test_create_ticket(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newtickettest Test ticket'
    ))

    assert 'Завёл тикет' in created_chat.messages[0].text
    assert 'TEST-1' in created_chat.messages[0].text

    ticket = get_context.tickenator.find_ticket_by_identifier('TEST-1')
    assert ticket is not None
    assert ticket['author'] == 'marty'
    assert ticket['subject'] == 'Test ticket'


@pytest.mark.asyncio
async def test_create_ticket_with_botname(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newtickettest@YaIncBot Test ticket'
    ))

    assert 'Завёл тикет' in created_chat.messages[0].text
    assert 'TEST-1' in created_chat.messages[0].text

    ticket = get_context.tickenator.find_ticket_by_identifier('TEST-1')
    assert ticket is not None
    assert ticket['author'] == 'marty'
    assert ticket['subject'] == 'Test ticket'


@pytest.mark.asyncio
async def test_create_ticket_with_description(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newtickettest Test ticket\n'
             'With some description after a newline'
    ))

    assert 'Завёл тикет' in created_chat.messages[0].text
    assert 'TEST-1' in created_chat.messages[0].text

    ticket = get_context.tickenator.find_ticket_by_identifier('TEST-1')
    assert ticket is not None
    assert ticket['author'] == 'marty'
    assert ticket['subject'] == 'Test ticket'
    assert ticket['description'] == 'With some description after a newline'


@pytest.mark.asyncio
async def test_create_ticket_no_queue_linked(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newticket Subject'
    ))

    message = created_chat.messages[-1]
    assert 'К этому чату не привязано ни одной очереди' in message.text


@pytest.mark.asyncio
async def test_create_ticket_ask_queue_with_botname(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newticket@YaIncBot Ask me\nplease'
    ))

    message = created_chat.messages[-1]
    assert 'К этому чату не привязано ни одной очереди' in message.text


@pytest.mark.asyncio
async def test_link_queue_search(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/set_queue te'  # `te` is for `TEST`
    ))

    message = created_chat.messages[-1]
    assert 'Выбери нужную очередь' in message.text
    await get_context.bot.call_callback_query(TCallbackQuery(
        message=message,
        user=TUser('marty'),
        data=message.reply_markup.find_by_text('TEST').callback_data
    ))

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newticket Test ticket'
    ))

    assert 'Завёл тикет' in created_chat.messages[-1].text
    assert 'TEST-1' in created_chat.messages[-1].text

    ticket = get_context.tickenator.find_ticket_by_identifier('TEST-1')
    assert ticket is not None
    assert ticket['author'] == 'marty'
    assert ticket['subject'] == 'Test ticket'


@pytest.mark.asyncio
async def test_link_queue_exact(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/set_queue test'
    ))

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newticket Test ticket'
    ))

    assert 'Завёл тикет' in created_chat.messages[-1].text
    assert 'TEST-1' in created_chat.messages[-1].text


@pytest.mark.asyncio
async def test_link_unknown(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)
    get_context.modules.support.init_queues(['TEST'])  # TESTSPI is not known

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/set_queue testspi'
    ))

    message = created_chat.messages[-1]
    assert 'Выбери нужную очередь' in message.text
    await get_context.bot.call_callback_query(TCallbackQuery(
        message=message,
        user=TUser('marty'),
        data=message.reply_markup.find_by_text('testspi').callback_data
    ))

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newticket Test ticket'
    ))
    assert 'Завёл тикет' in created_chat.messages[-1].text
    assert 'TESTSPI-1' in created_chat.messages[-1].text


@pytest.mark.asyncio
async def test_create_by_short_name(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)
    get_context.modules.support.init_queues(['NOCREQUESTS'])

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newticketnocreq Something is bad'
    ))

    assert 'Завёл тикет' in created_chat.messages[-1].text
    assert 'NOCREQUESTS-1' in created_chat.messages[-1].text


@pytest.mark.asyncio
async def test_create_by_reply(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)

    message = created_chat.add_message(
        user=TUser('other'),
        text='Something is very bad\n'
             'We do not know what'
    )
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newtickettest',
        reply_to_message=message
    ))

    assert 'Завёл тикет' in created_chat.messages[-1].text
    assert 'TEST-1' in created_chat.messages[-1].text

    ticket = get_context.tickenator.find_ticket_by_identifier('TEST-1')
    assert ticket is not None
    assert ticket['author'] == 'other'
    assert ticket['subject'] == 'Something is very bad'
    assert ticket['description'] == 'We do not know what'


@pytest.mark.asyncio
async def test_create_by_reply_with_subj(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)

    message = created_chat.add_message(
        user=TUser('other'),
        text='Something is very bad\n'
             'We do not know what'
    )
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newtickettest Crash',
        reply_to_message=message
    ))

    assert 'Завёл тикет' in created_chat.messages[-1].text
    assert 'TEST-1' in created_chat.messages[-1].text

    ticket = get_context.tickenator.find_ticket_by_identifier('TEST-1')
    assert ticket is not None
    assert ticket['author'] == 'other'
    assert ticket['subject'] == 'Crash'
    assert ticket['description'] == 'Сабж.\n\n<[Something is very bad\nWe do not know what]>'


@pytest.mark.asyncio
async def test_list_linked_queues(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)
    await link_queues_to_chat(get_context, created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/set_queue'
    ))

    message = created_chat.messages[-1]
    assert 'К этому чату привязаны следующие очереди' in message.text
    buttons = [btn.text for row in message.reply_markup.inline_keyboard for btn in row]
    assert 'TEST' in buttons
    assert 'TESTSPI' in buttons
    assert 'NOCREQUESTS' in buttons


@pytest.mark.asyncio
async def test_remove_linked_queue(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)
    await link_queues_to_chat(get_context, created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/set_queue'
    ))

    message = created_chat.messages[-1]
    assert 'К этому чату привязаны следующие очереди' in message.text
    buttons = [btn for row in message.reply_markup.inline_keyboard for btn in row]

    buttons = iter(buttons)
    remove_btn = None
    while True:  # this always finite due to StopIteration exception
        if next(buttons).text == 'TESTSPI':
            remove_btn = next(buttons)
            break
    assert remove_btn.text == '🗑️'

    await get_context.bot.call_callback_query(TCallbackQuery(
        message=message,
        user=TUser('marty'),
        data=remove_btn.callback_data
    ))

    buttons = [btn.text for row in message.reply_markup.inline_keyboard for btn in row]
    assert 'TEST' in buttons
    assert 'TESTSPI' not in buttons  # not!
    assert 'NOCREQUESTS' in buttons


@pytest.mark.asyncio
async def test_remove_all_queues(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)
    await link_queues_to_chat(get_context, created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/set_queue'
    ))

    message = created_chat.messages[-1]
    assert 'К этому чату привязаны следующие очереди' in message.text

    for i in range(3):
        buttons = [btn for row in message.reply_markup.inline_keyboard for btn in row]
        wastebuckets = [i for i in buttons if i.text == '🗑️']
        await get_context.bot.call_callback_query(TCallbackQuery(
            message=message,
            user=TUser('marty'),
            data=wastebuckets[0].callback_data
        ))
    assert 'К этому чату не привязано ни одной очереди' in message.text


@pytest.mark.asyncio
async def test_create_by_reply_with_search(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)
    await link_queues_to_chat(get_context, created_chat)

    message = created_chat.add_message(
        user=TUser('other'),
        text='Something is very bad\n'
             'We do not know what'
    )
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newticket',
        reply_to_message=message
    ))

    message = created_chat.messages[-1]
    assert 'Выбери нужную очередь' in message.text
    await get_context.bot.call_callback_query(TCallbackQuery(
        message=message,
        user=TUser('marty'),
        data=message.reply_markup.find_by_text('TEST').callback_data
    ))

    assert 'Завёл тикет' in created_chat.messages[-1].text
    assert 'TEST-1' in created_chat.messages[-1].text

    ticket = get_context.tickenator.find_ticket_by_identifier('TEST-1')
    assert ticket is not None
    assert ticket['author'] == 'other'
    assert ticket['subject'] == 'Something is very bad'
    assert ticket['description'] == 'We do not know what'


@pytest.mark.asyncio
async def test_create_ticket_with_linked(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/set_queue test'
    ))
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newticketTESTSPI Test ticket'
    ))

    assert 'Завёл тикет' in created_chat.messages[-1].text
    assert 'TESTSPI-1' in created_chat.messages[-1].text


@pytest.mark.asyncio
async def test_create_ticket_at(get_context, proto_module):
    created_chat = DChat(TChat(type='supergroup'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/set_queue test'
    ))
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=created_chat.chat,
        text='/newticketat TESTSPI Test ticket'
    ))

    assert 'Завёл тикет' in created_chat.messages[-1].text
    assert 'TESTSPI-1' in created_chat.messages[-1].text
