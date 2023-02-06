from .objects import TChat, TUser, TMessage
from typing import List, Set, Optional, Dict
from dataclasses import dataclass, field


@dataclass
class DChat:
    chat: TChat
    members: Set[TUser] = field(default_factory=set)
    admins: Set[str] = field(default_factory=set)
    admin_ranks: Dict[str, str] = field(default_factory=dict)
    messages: List[TMessage] = field(default_factory=list)
    pinned_message: int = None
    invite_link: str = None
    is_robot_member: bool = False
    is_bot_member: bool = False
    toggle_no_forwards: bool = False

    def __hash__(self):
        return hash(self.chat.id)

    def __eq__(self, other):
        if hash(self) == hash(other):
            return True
        else:
            return self.chat.id == other.chat.id

    def member_in(self, username: str) -> bool:
        for m in self.members:
            if m.username == username:
                return True

        return False

    def get_message(self, message_id: int) -> Optional[TMessage]:
        for m in self.messages:
            if m.message_id == message_id:
                return m

        return None

    def add_message(self, user, text, *args, **kwargs) -> TMessage:
        message = TMessage(user, self.chat, text, *args, **kwargs)
        self.messages.append(message)
        return message

    def find_message_containing(self, substring) -> Optional[TMessage]:
        for msg in self.messages:
            if substring in msg.text:
                return msg
        return None


def chats(*ids: int) -> List[DChat]:
    result = []
    for chat_id in ids:
        result.append(DChat(TChat(chat_id), is_bot_member=True, is_robot_member=True))

    return result
