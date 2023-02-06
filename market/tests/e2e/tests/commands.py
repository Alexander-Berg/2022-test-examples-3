import re
import time

import pytest
from telethon import TelegramClient
from telethon.errors import UserAlreadyParticipantError
from telethon.tl.custom.message import Message
from telethon.tl.functions.messages import ImportChatInviteRequest, CheckChatInviteRequest
from telethon.tl.types import InputMessagePinned, MessageActionPinMessage, Chat, User, PeerChannel

from lib.clients.startrek import StartrekClient
from lib.conf import settings


@pytest.mark.asyncio
async def test_juggler_notification_send(telegram: TelegramClient):
    channel = await telegram.get_entity(PeerChannel(int(settings.ALERTS_CHAT['ID'])))
    await telegram.send_message(channel, 'We are waiting for a juggler alert in less than 5 min here')


@pytest.fixture(scope='module')
async def bot_user(telegram):
    bot_username = settings.TELEGRAM_BOT['USERNAME']
    bot_user = await telegram.get_input_entity(bot_username)
    return bot_user


@pytest.fixture(scope='module')
async def incident_chat(telegram: TelegramClient, bot_user: User) -> Chat:
    fwd_msg = await telegram.send_message(bot_user, 'This message should be forwarded')
    await telegram.send_message(bot_user, '/run_inc', reply_to=fwd_msg)
    time.sleep(10)
    resp = await _get_response(bot_user, telegram)
    base_url = 'https://t.me/+'
    match_result = re.findall(f'{base_url}.*', resp)
    assert len(match_result) == 1, 'Incident chat creating failed.'
    invite_link = match_result[0]
    channel_hash = invite_link.removeprefix(base_url)
    try:
        await telegram(ImportChatInviteRequest(channel_hash))
    except UserAlreadyParticipantError:
        # локально обычно запускаем тесты от того же пользователя, что и создаёт чаты, так что считаем что это ок
        pass
    incident_chat = (await telegram(CheckChatInviteRequest(channel_hash))).chat
    return incident_chat


@pytest.fixture(scope='module')
async def discuss_chat(telegram: TelegramClient, bot_user: User):
    await telegram.send_message(bot_user, '/discuss_chat')
    time.sleep(10)
    resp = await _get_response(bot_user, telegram)
    base_url = 'https://t.me/+'
    match_result = re.findall(f'{base_url}.*', resp)
    assert len(match_result) == 1, 'Discuss chat creating failed.'
    invite_link = match_result[0]
    channel_hash = invite_link.removeprefix(base_url)
    try:
        await telegram(ImportChatInviteRequest(channel_hash))
    except UserAlreadyParticipantError:
        # локально обычно запускаем тесты от того же пользователя, что и создаёт чаты, так что считаем что это ок
        pass
    discuss_chat = (await telegram(CheckChatInviteRequest(channel_hash))).chat
    return discuss_chat


@pytest.mark.asyncio
async def test_start_command(telegram: TelegramClient, incident_chat: Chat):
    await telegram.send_message(incident_chat, '/start')


@pytest.mark.asyncio
async def test_ticket_binding(telegram: TelegramClient, incident_chat: Chat):
    ticket = 'TEST-1234'
    message = await telegram.send_message(incident_chat, f'/ticket {ticket}')
    resp = await _get_reply(message, incident_chat, telegram)
    assert 'Задача прикреплена к инцидентному протоколу!' in resp
    time.sleep(5)
    message = await telegram.send_message(incident_chat, '/ticket')
    resp = await _get_reply(message, incident_chat, telegram)
    assert resp.removeprefix('Инцидентный тикет: ') == ticket
    pinned = await _get_pinned(incident_chat, telegram)
    assert 'Тикет: TEST-1234' in pinned


@pytest.fixture(scope='module')
async def startrek_ticket(telegram: TelegramClient, incident_chat: Chat):
    message = await telegram.send_message(incident_chat, '/create_ticket')
    resp = await _get_reply(message, incident_chat, telegram)
    assert 'Создан тикет: ' in resp
    return resp.removeprefix('Создан тикет: ')


@pytest.mark.asyncio
async def test_assign_ticket(telegram: TelegramClient, incident_chat: Chat):
    message = await telegram.send_message(incident_chat, '/ticket')
    get_ticket = await _get_reply(message, incident_chat, telegram)
    assert 'Инцидентный тикет: ' in get_ticket
    startrek_ticket = get_ticket.removeprefix('Инцидентный тикет: ')
    test_user = settings.TEST['MANAGEMENT_ROLE_NAME']
    time.sleep(5)
    message = await telegram.send_message(incident_chat, f'/assign_ticket {test_user}')
    push_assignee = await _get_reply(message, incident_chat, telegram)
    assert f'Исполнителем тикета стал {test_user}' in push_assignee
    async with StartrekClient() as startrek:
        ticket = await startrek.get_ticket(startrek_ticket)
        ticket_assignee = ticket['assignee']['id']
    assert test_user in ticket_assignee


@pytest.mark.asyncio
async def test_status(telegram: TelegramClient, incident_chat: Chat):
    new_status = 'popa'
    message = await telegram.send_message(incident_chat, f'/status {new_status}')
    resp = await _get_reply(message, incident_chat, telegram)
    assert 'Готово' in resp
    time.sleep(5)
    message = await telegram.send_message(incident_chat, '/status')
    resp = await _get_reply(message, incident_chat, telegram)
    assert resp.removeprefix('Текущий статус: ') == new_status


@pytest.mark.asyncio
async def test_call_of_duty(telegram: TelegramClient, incident_chat: Chat):
    duty = settings.TEST['DUTIES_CALL_NAMES']
    await telegram.send_message(incident_chat, f'/call_of_duty {duty}')
    time.sleep(10)
    resp = await _get_response(incident_chat, telegram)
    for expect_username in settings.TEST['DUTIES_EXPECT_TG_USERNAMES'].split(' '):
        assert expect_username in resp


@pytest.mark.asyncio
async def test_ai(telegram: TelegramClient, incident_chat: Chat, startrek_ticket: str):
    message = await telegram.send_message(incident_chat, '/ai тестовый AI')
    resp = await _get_reply(message, incident_chat, telegram)
    assert 'AI успешно добавлен в описание тикета' in resp
    async with StartrekClient() as startrek:
        ticket = await startrek.get_ticket(startrek_ticket)
        ticket_description = ticket['description']
        assert 'тестовый AI' in ticket_description


@pytest.mark.asyncio
async def test_summary(telegram: TelegramClient, incident_chat: Chat, startrek_ticket: str):
    new_summary = f'Тестовый инцидент-чат {time.strftime("%d.%m.%Y %H:%M")}'
    message = await telegram.send_message(incident_chat, f'/summary {new_summary}')
    resp = await _get_reply(message, incident_chat, telegram)
    assert 'Готово' in resp
    time.sleep(20)
    chat = await telegram.get_entity(incident_chat)
    assert chat.title == new_summary
    async with StartrekClient() as startrek:
        ticket = await startrek.get_ticket(startrek_ticket)
        ticket_summary = ticket['summary']
        assert new_summary in ticket_summary


@pytest.mark.asyncio
async def test_graph(telegram: TelegramClient, incident_chat: Chat):
    await telegram.send_message(incident_chat, '/graph')
    time.sleep(20)
    resp = await _get_response(incident_chat, telegram)
    assert 'Ссылка на график' in resp
    link = 'https://solomon.yandex-team.ru/?project=monetize&cluster=production&service=blue_money' \
           '&l.sensor=promotion.vendor.clicks.chips&l.period=five_min&l.measure=promotion_vendor_chips_total&graph=auto'
    await telegram.send_message(incident_chat, f'/graph {link}')
    time.sleep(20)
    resp = await _get_response(incident_chat, telegram)
    assert 'Ссылка на график' in resp


@pytest.mark.asyncio
async def test_zoom(telegram: TelegramClient, incident_chat: Chat):
    message = await telegram.send_message(incident_chat, '/zoom')
    resp = await _get_reply(message, incident_chat, telegram)
    assert 'Зум-комната' in resp


@pytest.mark.asyncio
async def test_manager(telegram: TelegramClient, incident_chat: Chat):
    test_user = settings.TEST['MANAGEMENT_ROLE_NAME']
    message = await telegram.send_message(incident_chat, f'/manager {test_user}')
    resp = await _get_reply(message, incident_chat, telegram)
    assert 'Роль успешно назначена!' in resp


@pytest.mark.asyncio
async def test_commander(telegram: TelegramClient, incident_chat: Chat):
    test_user = settings.TEST['MANAGEMENT_ROLE_NAME']
    message = await telegram.send_message(incident_chat, f'/commander {test_user}')
    resp = await _get_reply(message, incident_chat, telegram)
    assert 'Роль успешно назначена!' in resp


@pytest.mark.asyncio
async def test_discover_manager(telegram: TelegramClient, incident_chat: Chat):
    expected_user: str = settings.TEST["EXPECTED_TELEGRAM_MANAGEMENT_ROLE_NAME"]
    if not expected_user.startswith('@'):
        expected_user = '@' + expected_user
    message = await telegram.send_message(incident_chat, '/manager')
    resp = await _get_reply(message, incident_chat, telegram)
    assert f'Инцидент-менеджер - {expected_user}' in resp


@pytest.mark.asyncio
async def test_discover_commander(telegram: TelegramClient, incident_chat: Chat):
    expected_user: str = settings.TEST["EXPECTED_TELEGRAM_MANAGEMENT_ROLE_NAME"]
    if not expected_user.startswith('@'):
        expected_user = '@' + expected_user
    message = await telegram.send_message(incident_chat, '/commander')
    resp = await _get_reply(message, incident_chat, telegram)
    assert f'Коммандир инцидента - {expected_user}' in resp


@pytest.mark.asyncio
async def test_impact(telegram: TelegramClient, incident_chat: Chat):
    message = await telegram.send_message(incident_chat, '/impact тестим влияние')
    resp = await _get_reply(message, incident_chat, telegram)
    assert 'Готово' in resp
    time.sleep(5)
    pinned = await _get_pinned(await message.get_chat(), telegram)
    assert re.search(r'Влияние на Маркет: тестим влияние', pinned), 'No impact field in status message'
    message = await telegram.send_message(incident_chat, '/impact')
    resp = await _get_reply(message, incident_chat, telegram)
    assert 'Влияние на Маркет: тестим влияние' in resp


@pytest.mark.asyncio
async def test_list_statuses(telegram: TelegramClient, incident_chat: Chat):
    message = await telegram.send_message(incident_chat, '/list_statuses')
    resp = await _get_reply(message, incident_chat, telegram)
    assert 'История статусов' in resp


@pytest.mark.asyncio
async def test_timeline_in_ticket(telegram: TelegramClient, incident_chat: Chat):
    message = await telegram.send_message(incident_chat, '/ticket')
    resp = await _get_reply(message, incident_chat, telegram)
    ticket = resp.removeprefix('Инцидентный тикет: ')
    async with StartrekClient() as startrek:
        ticket = await startrek.get_ticket(ticket)
        ticket_description = ticket['description']
        assert 'popa' in ticket_description


@pytest.mark.asyncio
async def test_message_forwarded(telegram: TelegramClient, incident_chat: Chat):
    found_pin = False
    expect_fwd = None
    async for message in telegram.iter_messages(incident_chat, limit=10, reverse=True):
        if found_pin:
            if message.fwd_from:
                expect_fwd = message
            break
        if isinstance(message.action, MessageActionPinMessage):
            found_pin = True
    assert expect_fwd and expect_fwd.message == 'This message should be forwarded'


@pytest.mark.asyncio
async def test_im_onduty(telegram: TelegramClient, incident_chat: Chat):
    message = await telegram.send_message(incident_chat, '/im_onduty')
    resp = await _get_reply(message, incident_chat, telegram)
    assert 'Дежурный по инцидентам' in resp


@pytest.mark.asyncio
async def test_init(telegram: TelegramClient, discuss_chat: Chat):
    await telegram.send_message(discuss_chat, '/init')
    time.sleep(10)
    pinned = await _get_pinned(discuss_chat, telegram)
    assert (resp := pinned), 'Status message is not pinned or absent'
    assert re.search(r'Краткое описание: Чат для обсуждения', resp), 'No summary field in status message'
    assert re.search(r'Текущий статус: Запущен инцидент', resp), 'No status field in status message'
    assert re.search(r'Влияние на Маркет: Неизвестно', resp), 'No impact field in status message'
    assert re.search(r'https?://(t(elegram)?.me|telegram.org)/([\d+]+)', resp), 'No invite link in status message'


@pytest.mark.asyncio
async def test_juggler_notification_receive(telegram: TelegramClient):
    channel = await telegram.get_entity(PeerChannel(int(settings.ALERTS_CHAT['ID'])))
    time.sleep(180)
    resp = await _get_response(channel, telegram)
    assert resp.startswith('CRIT на market_rel_notifications:f2_notification_test в')


async def _get_reply(message: Message, chat: Chat, telegram: TelegramClient, timeout=30):
    """
    Возвращает последний ответ на указанное сообщение. Не работает в личных сообщениях
    """
    delay = 1
    iterations = 4
    while iterations > 0:
        time.sleep(delay)
        reply = await telegram.get_messages(chat, reply_to=message.id)
        try:
            reply = reply.pop()
            return reply.message
        except IndexError:
            pass
        delay = timeout / 2 ** iterations  # Увеличиваем время ожидания для каждого нового запроса
        iterations -= 1
    raise TimeoutError('Can not receive reply in given time')


async def _get_response(chat: Chat, telegram: TelegramClient):
    """
    Возвращает последнее сообщение пользователя (бота) в чате
    Нужно рассчитывать задержку перед получением ответа в каждом отдельном случае
    Делает дорогостоящий GetHistoryRequest, использовать только при невозможности поймать reply
    """
    resp = await telegram.get_messages(chat)
    if isinstance(resp, list):
        resp = resp.pop()
    return resp.message


async def _get_pinned(chat: Chat, telegram: TelegramClient):
    pinned = await telegram.get_messages(chat, ids=InputMessagePinned())
    if isinstance(pinned, list):
        pinned = pinned.pop()
    return pinned.message
