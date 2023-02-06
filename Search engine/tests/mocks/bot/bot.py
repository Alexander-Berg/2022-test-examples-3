from io import BytesIO
import json
import time
from contextlib import contextmanager
from typing import Dict, Iterable, Tuple
from bot.modules.base.base import Inject

from bot.telegram.bot import Bot, CallbackQuery
from utils import full_wait_pending
from .data import DChat
from .objects import TUser, TMessage, TInlineKeyboardMarkup, TInlineKeyboardButton, TCallbackQuery, TPhotoSize
from bot.modules.base import Chat as BaseChat
from bot.modules.base.base import CallbackQuery as BCallbackQuery
from bot.telegram import Chat

import functools
from bot.modules.base import BaseModule
import re


@contextmanager
def temp_chats(context, chats: Iterable[DChat]):
    tmp_chat_ids = {chat.chat.id for chat in chats}

    for chat in chats:
        context.bot.add_chat(chat)

    try:
        yield
    except Exception:
        pass
    finally:
        for chat_id in tmp_chat_ids:
            context.bot.chats.pop(chat_id, None)


class MockBot(Bot):
    def __init__(self, *args, **kwargs):
        super().__init__(None, zk=None, name='mock_telegram_bot')

        self._me = TUser(self.name)

        # patched values:
        self.chats: Dict[int, DChat] = {}
        self._injectors = {}
        self._callbacks = {}
        self._listeners = []

    def register_modules(self, *modules: BaseModule):
        for m in modules:
            commands = m.commands
            if not commands:
                continue

            if m.name not in self._injectors:
                self._injectors[m.name] = lambda data: f'{m.name}|{data}'

            inject = self._injectors[m.name]
            for pattern, command in commands:
                command = self._convert_command(command, inject)

                self._commands.append((pattern, command))

            if m.callback:
                self._register_inject(m)
                self._callbacks[m.name] = m.callback
            if m.listener:
                self._listeners.append(m.listener)

    def _register_inject(self, module: BaseModule) -> Inject:
        """Register inject function for the module."""
        if module.name not in self._injectors:
            inject = functools.partial(self.inject_module_name, module.name)
            self._injectors[module.name] = inject
        return self._injectors[module.name]

    module_name_separator = '|'

    @classmethod
    def inject_module_name(cls, name: str, data: str) -> str:
        """Inject module name into callback query data."""
        return f'{name}{cls.module_name_separator}{data}'

    @classmethod
    def clean_module_name(cls, data: str) -> Tuple[str, str]:
        """Return parsed module name and cleaned data. Empty string will be
        return for module name in case of failure."""
        items = data.split(cls.module_name_separator, 1)
        if len(items) == 1:
            items.insert(0, '')
        return items[0], items[1]

    async def route_callback(self, chat: Chat, cq: CallbackQuery, ctx=None):
        """Route callback query to the right handler."""
        name, data = self.clean_module_name(cq.data)
        # Do not answer to unknown queries.
        if not name or name not in self._callbacks:
            return

        inject = self._injectors[name]
        chat = BaseChat(chat, inject)
        cq.data = data  # XXX: Smells a bit.
        cq = BCallbackQuery(cq, inject)

        await self._callbacks[name](chat, cq, ctx=ctx)

    async def api_call(self, method, **params):
        if method == 'deleteMessage':
            return await self.delete_message(**params)
        elif method == 'getChatAdministrators':
            return await self.get_chat_administrators(**params)
        else:
            raise RuntimeError(f'Unknown api method: `{method}`')

    async def delete_message(self, chat_id, message_id):
        chat = self.chats.get(chat_id)
        if not chat:
            raise RuntimeError('bot left chat')

        for msg in chat.messages:
            if msg.message_id == message_id:
                msg.text = 'Сообщение удалено'
                return

    @staticmethod
    def _convert_command(command, inject_markup=None):

        @functools.wraps(command)
        def wrapper(chat, *args, **kwargs):
            chat = BaseChat(chat, inject_markup)
            return command(chat, *args, **kwargs)

        return wrapper

    @full_wait_pending
    async def call_command(self, message: TMessage):
        """Calls a command and pushes it to the module."""
        chat = Chat.from_message(self, message.json())

        for pattern, handler in self._commands:
            match = re.search(pattern, message.text, re.DOTALL)
            if match:
                return await handler(chat, match, ctx=None)

        update = dict(message=message.json())
        for l in self._listeners:
            await l(update)

    def call_inline_query(self):
        """Implements the call of an inline query."""
        pass

    @full_wait_pending
    async def call_callback_query(self, query: TCallbackQuery):
        """Implements the call of an callback query from button"""

        chat = Chat.from_message(self, query.message.json())
        cq = CallbackQuery(self, query.json())

        await self.route_callback(chat, cq)

    def call_listener(self):
        """Implements the call of an listener."""
        pass

    def add_chat(self, chat: DChat):
        self.chats[chat.chat.id] = chat

    def find_chat(self, chat_id: int) -> DChat:
        return self.chats.get(chat_id, None)

    async def get_me(self):
        return self._me.json()

    async def get_chat(self, chat_id) -> DChat:
        chat = self.chats.get(chat_id)
        if not chat:
            return {}

        return chat.chat.json()

    async def get_invite_link(self, chat_id):
        chat = self.chats.get(chat_id)
        if not chat:
            return None

        return chat.invite_link

    async def get_chat_administrators(self, chat_id):
        """
        Get a list of administrators in a chat. Chat must not be private.
        """
        chat = self.chats.get(int(chat_id))
        if not chat:
            raise RuntimeError('bot left chat')
        result = []
        for admin in chat.admins:
            for member in chat.members:
                if member.username == admin:
                    result.append(member)
                    break
            else:
                raise Exception('Admin not in chat. Probably something went wrong with tests setup')
        return [{'user': i.json()} for i in result]

    async def get_chat_member(self, chat_id, user_id, raise_error: bool = False):
        chat = self.chats.get(chat_id)
        if not chat:
            return {}

        for member in chat.members:
            if member.id == user_id:
                return {
                    'user': member.json(),
                    'status': 'admin' if member.username in chat.admins else 'member',
                }

        return {}

    async def leave_chat(self, chat_id):
        chat = self.chats.pop(chat_id, None)
        return {'success': bool(chat)}

    async def send_photo(self, chat_id: int, photo: BytesIO, caption: str = '', **options):
        chat = self.chats.get(chat_id)
        if not chat:
            raise RuntimeError('bot left chat')

        if isinstance(photo, str):
            photo = [TPhotoSize(file_id=photo)]
        else:
            photo = None
            caption += '\n<PHOTO>'

        message = TMessage(self._me, chat.chat, caption, date=int(time.time()), photo=photo)

        chat.messages.append(message)
        return {'result': message.json()}

    async def send_document(self, chat_id: int, document: BytesIO, caption: str = '', **options):
        chat = self.chats.get(chat_id)
        if not chat:
            raise RuntimeError('bot left chat')
        message = TMessage(self._me, chat.chat, caption+'\n<DOCUMENT>', date=int(time.time()))

        chat.messages.append(message)
        return {'result': message.json()}

    async def send_message(self, chat_id, text, **options):
        chat = self.chats.get(chat_id)
        if not chat:
            raise RuntimeError('bot left chat')

        reply_message_id = options.get('reply_to_message_id', None)
        reply_message = None
        if reply_message_id:
            for message in chat.messages:
                if message.message_id == reply_message_id:
                    reply_message = message
                    break

        message = TMessage(self._me, chat.chat, text, date=int(time.time()), reply_to_message=reply_message)

        MockBot._update_message_markup(message, options)

        chat.messages.append(message)
        return {'result': message.json()}

    @staticmethod
    def _update_message_markup(message, options):
        if 'reply_markup' not in options:
            return

        markup = options['reply_markup']
        if not markup:
            message.reply_markup = markup
            return

        inline_keyboard = TInlineKeyboardMarkup()

        for buttons in markup['inline_keyboard']:
            result = []
            for button in buttons:
                result.append(TInlineKeyboardButton(
                    text=button['text'],
                    url=button.get('url', None),
                    callback_data=button.get('callback_data', None),
                ))

            inline_keyboard.inline_keyboard.append(result)

        message.reply_markup = inline_keyboard

    async def forward_message(self, chat_id, from_chat_id, message_id, **options):
        pass

    async def edit_message_text(self, chat_id, message_id, text, **options):
        chat = self.chats.get(chat_id)
        if not chat:
            raise RuntimeError(f'chat {chat_id} not found')
        for message in chat.messages:
            if message.message_id == message_id:
                message.text = text
                MockBot._update_message_markup(message, options)

                return message.json()

    async def pin_message(self, chat_id: int, message_id: int, silent=False):
        chat = self.chats.get(chat_id)
        if not chat:
            raise RuntimeError(f'chat {chat_id} not found')
        if not any(msg.message_id == message_id for msg in chat.messages):
            raise RuntimeError(f'message {chat_id} not found')
        chat.pinned_message = message_id

    async def unpin_message(self, chat_id: int, message_id: int):
        chat = self.chats.get(chat_id)
        if not chat:
            raise RuntimeError(f'chat {chat_id} not found')
        if not any(msg.message_id == message_id for msg in chat.messages):
            raise RuntimeError(f'message {chat_id} not found')
        chat.pinned_message = None

    async def edit_message_reply_markup(self, chat_id, message_id, reply_markup, **options):
        raise NotImplementedError
