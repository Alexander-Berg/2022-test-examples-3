from datetime import datetime, timedelta
from bot.modules.protocols.eta import parse_time
import pytest

from bot.modules.protocols.models import ProtoTimer, Protocol
from bot.modules.protocols import timers

from mocks.bot import DChat, TChat, TUser, TMessage
from mocks.context import temp_users, user


@pytest.fixture(scope='function', autouse=True)
def set_dependencies(get_context, proto_module):
    proto_module.data = get_context.data
    with temp_users(
        get_context,
        user('marty', is_marty=True),
        user('user')
    ):
        yield None


async def start_protocol(get_context, user='marty'):
    created_chat = DChat(TChat(type='private'))
    get_context.bot.add_chat(created_chat)

    await get_context.bot.call_command(TMessage(
        user=TUser(user),
        chat=created_chat.chat,
        text='/proto'
    ))

    proto_chat = list(get_context.bot.chats.values())[-1]
    return proto_chat


def test_parse_time():
    for test_input, expected in [
        ('1h', timedelta(hours=1)),
        ('1.5h', timedelta(hours=1.5)),
        ('1hour', timedelta(hours=1)),
        ('1 h', timedelta(hours=1)),
        ('1 h 30 min', timedelta(hours=1.5)),
        ('30 min', timedelta(minutes=30)),
        ('1d4h5h3m', timedelta(days=1, hours=5, minutes=3))
    ]:
        assert parse_time(test_input) == expected


@pytest.mark.asyncio
async def test_eta_sending_messages(get_context, proto_module):
    proto_chat = await start_protocol(get_context)

    eta_msg = TMessage(None, proto_chat.chat, None)

    # Таймер должен долбить даже замьюченный протокол
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='/proto mute'
    ))

    for _ in range(2):
        async with await get_context.data.connect() as conn:
            proto = await Protocol.get_by_chat(conn, proto_chat.chat.id)
            timer = await ProtoTimer.get(conn, timers.SetETA.key, proto.id)
            timer.trigger_time = datetime.now()
            await timer.commit(conn, include={'trigger_time'})
            await get_context.zk.set_raw('/protocols/timers/last_run', None)

        await proto_module.trigger_timers()

        new_eta_msg = proto_chat.messages[-1]
        assert new_eta_msg.message_id != eta_msg.message_id, 'Таймер должен был отправить новое сообщение'
        eta_msg = new_eta_msg


@pytest.mark.asyncio
async def test_setting_empty_eta(get_context):
    proto_chat = await start_protocol(get_context)
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='/eta неправильный формат'
    ))
    assert proto_chat.messages[-1].text == 'Надо указать время, например `/eta 2.5h`'


@pytest.mark.parametrize("user,trigger_times", [
    ('user', [60, 15]),  # green
    ('marty', [20, 15])  # yellow
])
async def test_eta_timings(get_context, proto_module, user, trigger_times):
    proto_chat = await start_protocol(get_context, user)

    for i in range(2):
        async with await get_context.data.connect() as conn:
            proto = await Protocol.get_by_chat(conn, proto_chat.chat.id)
            timer = await ProtoTimer.get(conn, timers.SetETA.key, proto.id)
            minutes_approx = ((timer.trigger_time - datetime.now()).total_seconds() + 60) // 60
            assert minutes_approx == trigger_times[i]
            timer.trigger_time = datetime.now()
            await timer.commit(conn, include={'trigger_time'})
            await get_context.zk.set_raw('/protocols/timers/last_run', None)

        await proto_module.trigger_timers()


@pytest.mark.asyncio
async def test_setting_eta_via_command(get_context):
    proto_chat = await start_protocol(get_context)
    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='/eta 1h'
    ))
    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_chat(conn, proto_chat.chat.id)
    assert proto.eta == timedelta(hours=1).total_seconds(), 'ETA не установился'


@pytest.mark.asyncio
async def test_setting_eta_via_reply(get_context, proto_module):
    proto_chat = await start_protocol(get_context)

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_chat(conn, proto_chat.chat.id)
        timer = await ProtoTimer.get(conn, timers.SetETA.key, proto.id)
        timer.trigger_time = datetime.now()
        await timer.commit(conn, include={'trigger_time'})
        await get_context.zk.set_raw('/protocols/timers/last_run', None)

    await proto_module.trigger_timers()

    eta_msg = proto_chat.messages[-1]

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='1h30m',
        reply_to_message=eta_msg
    ))

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_chat(conn, proto_chat.chat.id)
    assert proto.eta == timedelta(hours=1, minutes=30).total_seconds(), 'ETA не установился'


@pytest.mark.asyncio
async def test_dismissed_eta(get_context, proto_module):
    proto_chat = await start_protocol(get_context)

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_chat(conn, proto_chat.chat.id)
        timer = await ProtoTimer.get(conn, timers.SetETA.key, proto.id)
        assert timer, 'SetETA должен быть'

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='/eta 1h 33m'
    ))

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_chat(conn, proto_chat.chat.id)
        timer = await ProtoTimer.get(conn, timers.SetETA.key, proto.id)
        assert timer is None, 'SetETA должен был остановиться'
        timer = await ProtoTimer.get(conn, timers.ETADismissed.key, proto.id)
        assert timer and timer.in_process, 'Таймер не завелся'
        dt = round((timer.trigger_time - timer.start_time).total_seconds())
        assert dt == timedelta(hours=1, minutes=33).total_seconds(), 'Время таймера неправильно'
        timer.trigger_time = datetime.now()
        await timer.commit(conn, include={'trigger_time'})
        await get_context.zk.set_raw('/protocols/timers/last_run', None)

    await proto_module.trigger_timers()

    eta_msg = None
    for msg in proto_chat.messages:
        if 'Укажите новое' in msg.text and 'ETA' in msg.text:
            eta_msg = msg
            break
    assert eta_msg, 'Перезапрос на ETA не был отправлен'

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_chat(conn, proto_chat.chat.id)
        timer = await ProtoTimer.get(conn, timers.SetETA.key, proto.id)
        assert timer.in_process, 'Таймер на установку ETA не вернулся'

    await get_context.bot.call_command(TMessage(
        user=TUser('marty'),
        chat=proto_chat.chat,
        text='1h30m',
        reply_to_message=eta_msg
    ))

    async with await get_context.data.connect() as conn:
        proto = await Protocol.get_by_chat(conn, proto_chat.chat.id)
        assert proto.eta == timedelta(hours=1, minutes=30).total_seconds(), 'Новый таймер не выставил ETA'
