import asyncio
from functools import partial

from bot.modules.base import Chat
from bot.structlog import get_logger
from bot.utils import Keyboard
from bot.modules.chatmessages import TEST_MODE_MESSAGE
from .helper import BaseHelper, Callback
from .models import Protocol

_logger = get_logger(__name__)


class TestModeHelper(BaseHelper):
    @property
    def callback_handlers(self):
        return (
            Callback('switch_test_flag', self._switch_test_flag),
        )

    def build_test_mode_keyboard(self, proto: Protocol):
        c = partial(self._callback, action='switch_test_flag')
        flags = proto.test_mode_flags
        onoff = Keyboard.onoff

        buttons = [
            [{'text': f'Добавлять Дежурных DevOps: {onoff(flags.call_onduty)}', 'callback_data': c(args=(proto.id, 'call_onduty'))}],
            [{'text': f'Добавлять ответственных компоненты: {onoff(flags.call_responsible)}', 'callback_data': c(args=(proto.id, 'call_responsible'))}],
            [{'text': f'Добавлять ответственных вертикали: {onoff(flags.call_vertical)}', 'callback_data': c(args=(proto.id, 'call_vertical'))}],
            [{'text': f'Добавлять Марти: {onoff(flags.call_marty)}', 'callback_data': c(args=(proto.id, 'call_marty'))}],
            [{'text': f'Добавлять роли (координатор, ...): {onoff(flags.call_roles)}', 'callback_data': c(args=(proto.id, 'call_roles'))}],
            [{'text': f'Добавлять призываемых людей: {onoff(flags.call_people)}', 'callback_data': c(args=(proto.id, 'call_people'))}],
            [{'text': f'Отправлять комментарии в тикет: {onoff(flags.send_comments_to_startrek)}', 'callback_data': c(args=(proto.id, 'send_comments_to_startrek'))}],
            [{'text': f'Логировать таймеры: {onoff(flags.log_timers)}', 'callback_data': c(args=(proto.id, 'log_timers'))}],
            [{'text': f'Логировать все действия: {onoff(flags.log_actions)}', 'callback_data': c(args=(proto.id, 'log_actions'))}],
            [{'text': f'Добавлять PR: {onoff(flags.call_pr)}', 'callback_data': c(args=(proto.id, 'call_pr'))}],
            [{'text': f'Добавлять Support и SMM: {onoff(flags.call_support)}', 'callback_data': c(args=(proto.id, 'call_support'))}],
            [{'text': f'Добавлять RTC: {onoff(flags.call_rtc)}', 'callback_data': c(args=(proto.id, 'call_rtc'))}],
            [{'text': f'Добавлять NOC: {onoff(flags.call_noc)}', 'callback_data': c(args=(proto.id, 'call_noc'))}],
            [{'text': f'Уведомлять чат стейкхолдеров: {onoff(flags.notify_stakeholder_chat)}', 'callback_data': c(args=(proto.id, 'notify_stakeholder_chat'))}],
            [{'text': f'Добавлять pr owner в чат стейкхолдеров: {onoff(flags.call_to_stakeholder_chat)}', 'callback_data': c(args=(proto.id, 'call_to_stakeholder_chat'))}],
            [{'text': f'Уведомлять кризисный чат: {onoff(flags.notify_pr_crisis_chat)}', 'callback_data': c(args=(proto.id, 'notify_pr_crisis_chat'))}],
        ]

        if proto.is_chat_created:
            buttons.append([{'text': f'Удалить чат при завершении: {onoff(flags.remove_chat_on_finish)}', 'callback_data': c(args=(proto.id, 'remove_chat_on_finish'))}])

        return buttons

    async def _switch_test_flag(self, chat: Chat, args: tuple, person: str, ctx=None):
        logger = _logger.with_fields(ctx)

        proto_id, attr = args

        async with await self.context.data.connect(logger=logger) as conn:
            proto = await Protocol.get(conn, proto_id)

            flag = getattr(proto.test_mode_flags, attr, None)
            if flag is None:
                return

            new_flag = not flag
            setattr(proto.test_mode_flags, attr, new_flag)
            await proto.commit(conn, include={'test_mode_flags'})

            if new_flag and attr in ('call_onduty', 'call_responsible', 'call_vertical', 'call_marty', 'call_rtc', 'call_support', 'call_pr', 'call_noc'):
                asyncio.create_task(self.proto_context.chats.start_invite_users(proto, await proto.chat(conn, logger), '', logger=logger, on_create=False))

        keyboard = self.build_test_mode_keyboard(proto)
        await chat.edit_text(text=TEST_MODE_MESSAGE, markup={'inline_keyboard': keyboard})
