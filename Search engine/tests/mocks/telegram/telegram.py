from mocks.bot import MockBot, DChat, TChat, TUser
from typing import Iterable, Union
import uuid

from bot.modules.telegram import ChatID, Telegram
from bot.modules.telegram.types import InviteUsersResponse, KickUsersResponse

import logging


class NoChat(AssertionError):
    def __init__(self, chat_id):
        super().__init__(f'unable to locate chat {chat_id}')


class MockTelegram(Telegram):
    def __init__(self, context):
        self._bot: MockBot = context.bot
        self.bot_username = self._bot.name
        self._user = TUser('test_robot', id=1)

    async def is_participant(self, chat_id: int, robot: str = None) -> str or None:
        return self.bot_username

    async def get_channel_participants(self, chat_id: int, robot: str = None):
        chat = self._bot.find_chat(chat_id)
        if not chat:
            return set()

        return {m.username for m in chat.members}

    async def get_admins(self, chat_id: int, robot: str = None):
        chat = self._bot.find_chat(chat_id)
        if not chat:
            return []
        return [m for m in chat.members if not m.is_bot]

    async def kick_member(self, chat_id: int, username: str, robot: str = None):
        chat = self._bot.find_chat(chat_id)
        if not chat:
            return

        user = None
        for u in chat.members:
            if u.username.lower() == username.lower():
                user = u
                break

        chat.members.remove(user)

    async def grant_admin(self, chat_id: int, username: str, robot: str = None, rank: str = None):
        chat = self._get_chat(chat_id)

        for member in chat.members:
            if member.username == username:
                chat.admins.add(username)
                chat.admin_ranks[username] = rank

    async def leave_chat(self, chat_id, robot: str = None, remove_bot=False, revoke_admins=False):
        chat = self._get_chat(chat_id)
        chat.is_robot_member = False
        if revoke_admins:
            chat.admins.clear()

    # chats

    async def change_supergroup_title(self, title: str, chat_id: int, robot: str = None):
        chat = self._get_chat(chat_id)
        chat.chat.title = title

    async def create_supergroup(self, title: str, description: str = None, is_slack=False):
        chat = TChat(title=title) if title else TChat()
        chat_data = DChat(chat, is_robot_member=True)
        self._bot.add_chat(chat_data)

        return chat_data.chat, None

    async def delete_chat(self, chat_id: int, robot: str = None):
        self._bot.chats.pop(chat_id, None)

    async def update_photo(self, chat_id: int, avatar: bytes, robot: str):
        return

    async def toggle_no_forwards(self, chat_id: int, enabled: bool, robot: str = None):
        self._bot.chats[chat_id].toggle_no_forwards = enabled

    async def join_chat(self, url: str):
        for chat in self._bot.chats.values():  # type: DChat
            if chat.invite_link == url:
                chat.is_robot_member = True
                logging.info(f'found chat {chat.chat.id}')
                return chat.chat.id, None

        chat, _ = await self.create_supergroup('')
        logging.info(f'created supergroup {chat.id}')
        return chat.id, None

    async def create_join_url(self, chat_id: int, robot: str = None):
        chat = self._get_chat(chat_id)

        chat.invite_link = f'https://t.me/joinchat/{str(uuid.uuid4())[:22]}'
        return chat.invite_link

    # invites

    async def invite_users(self, chat_id: int, usernames: Iterable[str], **kwargs):
        chat = self._get_chat(chat_id)

        usernames = set(u.lower() for u in usernames)
        participants_before = {m.username.lower() for m in chat.members}
        already_invited = usernames.intersection(participants_before)

        usernames = usernames.difference(already_invited)

        users = [TUser(username=u) for u in usernames]
        chat.members.update(users)
        rsp = InviteUsersResponse(robot=None, invited=usernames, already_invited=already_invited, not_invited=[], errors_to_reply=[])

        return rsp

    async def kick_users(self,
                         chat_id: Union[int, str],
                         usernames: Iterable[str],
                         robot: str = None,
                         ) -> KickUsersResponse:
        chat = self._get_chat(chat_id)

        usernames = set(u.lower() for u in usernames)
        participants_before = {m.username.lower() for m in chat.members}
        need_to_kick = usernames.intersection(participants_before)
        not_participated = usernames.difference(need_to_kick)

        usernames = participants_before.difference(need_to_kick)
        users = [TUser(username=u) for u in usernames]
        chat.members = users

        rsp = KickUsersResponse(kicked=need_to_kick, not_participated=not_participated, robot=robot, errors_to_reply=[])

        return rsp

    async def revoke_admin(self, chat_id: int, username: str, robot: str = None):
        chat = self._get_chat(chat_id)

        chat.admins.remove(username)
        chat.admin_ranks.pop(username)

    # messages

    async def send_message(self, text: str, chat_id: int, robot: str, **kwargs):
        raise NotImplementedError

    async def pin_message(self, message_id: int, chat_id: int, robot: str = None):
        chat = self._get_chat(chat_id)

        for message in chat.messages:
            if message.message_id == message_id:
                chat.pinned_message = message_id
                return

    def _get_chat(self, chat_id) -> DChat:
        chat_id = ChatID.from_any(chat_id).id_for_bot
        chat = self._bot.find_chat(chat_id)
        if not chat:
            raise NoChat(chat_id)

        return chat
