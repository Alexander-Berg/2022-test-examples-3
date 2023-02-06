from dataclasses import dataclass, field, asdict
from typing import Optional, List
import time
import mocks.bot.pools as pool


class JSONMixin:
    def json(self):
        # https://www.python.org/dev/peps/pep-0008/#method-names-and-instance-variables
        class_name = self.__class__.__name__
        fields_json_map = getattr(self, f'_{class_name}__fields_json_map', {})
        clear_empty_fields = getattr(self, f'_{class_name}__clear_empty_fields', False)

        def dict_factory(items):
            result = {}
            for key, value in items:
                if clear_empty_fields and not value:
                    continue
                key = fields_json_map.get(key, key)
                result[key] = value
            return result

        return asdict(self, dict_factory=dict_factory)


@dataclass
class TUser(JSONMixin):
    username: str = field(default_factory=pool.Usernames.generate)

    id: int = field(default_factory=pool.UserIDs.generate)

    is_bot: bool = False
    first_name: str = 'FirstNameMock'
    last_name: str = 'LastNameMock'

    def __post_init__(self):
        pool.Usernames.insert(self.username)
        pool.UserIDs.insert(self.id)

    def __hash__(self):
        return hash(self.id)

    def __eq__(self, other):
        if hash(self) == hash(other):
            return True
        else:
            return self.id == other.id


@dataclass
class TChat(JSONMixin):
    id: int = field(default_factory=pool.SupergroupIDs.generate)
    type: str = 'supergroup'
    title: str = field(default_factory=pool.Titles.generate)

    messages_pool = None

    def __post_init__(self):
        pool.SupergroupIDs.insert(self.id)
        self.messages_pool = pool.MessageIDs()


@dataclass
class TChatMessage(JSONMixin):
    sender: dict
    admins: list
    type: str = 'supergroup'

    async def reply(self, text, parse_mode=''):
        pass

    async def get_chat_administrators(self):
        return [{'user': {'username': x}} for x in self.admins]


@dataclass
class TInlineKeyboardButton(JSONMixin):
    text: str
    url: str = None
    callback_data: str = None

    def press(self, bot):
        pass


@dataclass
class TInlineKeyboardMarkup(JSONMixin):
    inline_keyboard: List[List[TInlineKeyboardButton]] = field(default_factory=list)

    def find_by_text(self, text: str) -> Optional[TInlineKeyboardButton]:
        for row in self.inline_keyboard:
            for btn in row:
                if btn.text == text:
                    return btn
        return None


@dataclass
class TPhotoSize(JSONMixin):
    file_id: str
    file_unique_id: str = 'file_unique_id'
    width: int = 800
    height: int = 600
    file_size: Optional[int] = None


@dataclass
class TMessage(JSONMixin):
    __fields_json_map = {
        'user': 'from'
    }

    user: TUser
    chat: TChat
    text: str

    date: int = field(default_factory=lambda: int(time.time()))

    forward_from: TUser = None
    reply_to_message: 'TMessage' = None

    message_id: int = None

    reply_markup: Optional[TInlineKeyboardMarkup] = None
    photo: Optional[List[TPhotoSize]] = None

    def __post_init__(self):
        if not self.message_id:
            # noinspection PyTypeChecker
            self.message_id = self.chat.messages_pool.generate()
        else:
            self.chat.messages_pool.insert(self.message_id)


callbacks_pool = pool.MessageIDs()


@dataclass
class TCallbackQuery(JSONMixin):
    __fields_json_map = {
        'user': 'from'
    }

    user: TUser
    message: TMessage
    data: str

    id: str = None

    def __post_init__(self):
        if not self.id:
            # noinspection PyTypeChecker
            self.id = callbacks_pool.generate()
        else:
            callbacks_pool.insert(self.id)


@dataclass
class TInlineQuery(JSONMixin):
    __fields_json_map = {
        'user': 'from'
    }

    id: str
    user: TUser
    query: str
    offset: str = ''


@dataclass
class TUpdate(JSONMixin):
    update_id: int

    message: Optional[TMessage] = None
    callback_query: Optional[TCallbackQuery] = None
    inline_query: Optional[TInlineQuery] = None
