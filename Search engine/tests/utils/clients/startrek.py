import logging
import re
import typing
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum

from search.martylib.core.date_utils import now
from search.mon.warden.src.clients import StartrekQueueCollectionMock, StartrekComponentCollectionMock
from search.mon.warden.src.utils.startrek import from_st_str_time_to_timestamp, ST_DATE_FMT

LAST_UPDATE_FMT = '%Y-%m-%d %H:%M:%S'

INCIDENT_RE_PARAMS = re.compile(r'Queue: \"([\w]+)\".*updated: >= \"([\d :-]+)\".*')
ACTION_ITEMS_RE_PARAMS = re.compile(r'\(Tags: "spi:actionitem" OR Tags: "lsr:actionitem"\).*updated: >= \"([\d :-]+)\".*')
"""Matches queue from group 1 and updated from group 2"""

QUEUE_COUNTERS = defaultdict(lambda: 0)
LOGGER = logging.getLogger('warden.tests.issue_mock')


@dataclass
class DisplayedValue(object):
    display: str
    key: str

    def __init__(self, display: str, key: str = ''):
        self.display = display
        self.key = key or display

    def __eq__(self, other):
        return self.display == other.display


@dataclass
class LoginValue(object):
    login: str

    def __eq__(self, other):
        return self.login == other.login


@dataclass
class KeyValue(object):
    key: str

    def __eq__(self, other):
        return self.key == other.key


@dataclass(init=False)
class KeyDisplay(object):
    key: str
    display: str

    def __init__(self, value):
        self.key = value.name
        self.display = value.value

    def __eq__(self, other):
        return self.key == other.key


@dataclass
class Link(object):
    object: 'MockTicket' = None

    def __eq__(self, other):
        return self.object == other.object


class Status(Enum):
    closed = 'Закрыт'
    in_work = 'В разработке'  # SPI
    in_work_lsr = 'В работе'  # LSR
    other = 'Открыт'  # does not participating in workflow
    newGoal = 'New Goal'


class Resolution(Enum):
    solved = 'Решен'
    duplicated = 'Дубликат'
    relapse = 'Рецидив'
    not_appear = 'Не воспроизводится'


class Priority(Enum):
    trivial = 'Незначительный'
    minor = 'Низкий'
    middle = 'normal'
    critical = 'Критичный'
    blocker = 'Блокер'


class MinusDc(Enum):
    yes = 'Да'
    no = 'Нет'


@dataclass
class ChangeMock(object):
    updatedAt: str
    fields: list


class ChangelogMock(object):

    def __init__(self, changes: typing.Iterable[ChangeMock]):
        self.changes = changes

    def get_all(self, field: typing.Iterable[str], sort: str = 'asc') -> typing.Iterable[ChangeMock]:
        return self.changes


@dataclass
class MockTicket(object):
    key: str
    createdAt: str
    updatedAt: str
    status: DisplayedValue
    sreBeginTime: str = None
    sreEndTime: str = None
    resolution: DisplayedValue = None
    minusDc: str = MinusDc.no.value
    ydt: int = 0

    tags: typing.List[str] = field(default_factory=list)
    components: typing.List[DisplayedValue] = field(default_factory=list)
    links: typing.List[Link] = field(default_factory=list)
    summary: str = ''
    assignee: LoginValue = LoginValue('')
    createdBy: LoginValue = LoginValue(''),
    duty: typing.Optional[typing.List[LoginValue]] = None
    queue: KeyValue = KeyValue('SPI')
    aliases: typing.List[str] = None
    priority: DisplayedValue = DisplayedValue(display=Priority.middle.value, key=Priority.middle.value)
    customers: typing.List[LoginValue] = None
    deadline: str = ''
    wardenAlerts: typing.List[str] = None

    def __init__(
        self,
        queue: str,
        created_at: datetime,
        status: Status,
        updated_at: typing.Optional[datetime] = None,
        resolution: Resolution = None,
        tags: typing.List[str] = None,
        components: typing.List[DisplayedValue] = None,
        ydt: int = 0,
        minus_dc: MinusDc = MinusDc.no,
        summary: str = '',
        assignee: LoginValue = LoginValue(''),
        created_by: LoginValue = LoginValue(''),
        duty: typing.List[LoginValue] = None,
        aliases: typing.List[str] = None,
        priority: Priority = Priority.middle,
        description: str = '',
        customers: typing.List[LoginValue] = None,
        deadline: str = '',
        wardenAlerts: typing.List[str] = None,
    ):
        queue = queue.upper()
        key_num = QUEUE_COUNTERS[queue]
        QUEUE_COUNTERS[queue] += 1

        self.key = f'{queue.upper()}-{key_num}'
        self.createdAt = created_at.strftime(ST_DATE_FMT)
        self.status = DisplayedValue(display=status.value, key=status.name)
        self.resolution = DisplayedValue(resolution.value, key=resolution.name) if resolution else None
        self.ydt = ydt
        self.minusDc = minus_dc.value
        self.tags = tags or []
        self.components = components or []
        self.summary = summary
        self.assignee = assignee
        self.createdBy = created_by
        self.duty = duty or []
        self.queue = KeyValue(queue.upper())
        self.links = []
        self.aliases = aliases or []
        self.priority = KeyDisplay(priority) if priority else None
        self.description = description
        self.customers = customers or []
        self.deadline = ''
        self.wardenAlerts = wardenAlerts or []

        if updated_at:
            self.updatedAt = updated_at.strftime('%Y-%m-%dT%H:%M:%S.%f+0000')

        LOGGER.info('updatedAt: %s', self.updatedAt)

    def __setattr__(self, key, value):
        if key != 'updatedAt':
            self.updatedAt = datetime.now(timezone.utc).strftime(ST_DATE_FMT)

        object.__setattr__(self, key, value)

    def __eq__(self, other):
        return ((self.key, self.createdAt, self.updatedAt, self.status, self.resolution, self.minusDc, self.ydt, self.tags, self.links) ==
                (other.key, other.createdAt, other.updatedAt, other.status, other.resolution, other.minusDc, other.ydt, other.tags, other.links))

    def postpone_link(self, obj: 'MockTicket'):
        self.links.append(Link(obj))

    def delete_link(self, obj: 'MockTicket'):
        self.links = [link for link in self.links if link.object.key != obj.key]

    @classmethod
    def from_json(cls, data: dict):
        kwargs = {}
        for key, desc in cls.__dataclass_fields__.items():
            if key not in data:
                continue

            value = data[key]
            if desc.type == DisplayedValue:
                value = DisplayedValue(value)
            elif key == 'links':
                value = [Link(v) for v in value]

            kwargs[key] = value

    @classmethod
    def from_list(cls, data: list):
        return [cls.from_json(d) for d in data]

    @property
    def changelog(self) -> ChangelogMock:
        return ChangelogMock((
            ChangeMock(now().strftime('%Y-%m-%dT%H:%M:%S.%f+0000'), []),
        ))


class MockIssuesCollection(object):
    def __init__(self, *args, **kwargs):
        self.data: typing.Dict[str, MockTicket] = {}
        self.logger = logging.getLogger('warden.tests.issue_mock')

    @staticmethod
    def create(queue: str, summary, description, components=None, tags=None, createdBy: str = '', assignee: str = '', **kwargs):
        status = Status.newGoal if 'GOAL' in queue else Status.other
        new_obj = MockTicket(queue, datetime.now(), status, assignee=LoginValue(login=assignee))
        return new_obj

    def set_data(self, data: typing.Iterable[MockTicket]):
        # self.logger.info('data: %s', data)
        self.data = {t.key: t for t in data}
        for t in data:
            if t.aliases:
                for alias in t.aliases:
                    self.data.setdefault(alias, t)

    def clear_data(self):
        self.data = {}

    def find(self, query=None, *args, **kwargs) -> typing.List[typing.Union[MockTicket, typing.ValuesView[MockTicket]]]:
        if not query:
            return [self.data.values()]

        tags = []
        queue = None
        match = INCIDENT_RE_PARAMS.fullmatch(query)  # type: typing.Match
        if not match:
            match = ACTION_ITEMS_RE_PARAMS.fullmatch(query)
            if not match:
                raise RuntimeError(f"query doesn't match: {query}")
            updated = match.group(1)
            tags = ['spi:actionitem', 'lsr:actionitem']
        else:
            queue = match.group(1)
            updated = match.group(2)
        self.logger.info('queue: %s updated: %s', queue, updated)
        return self._sorted(queue, updated, tags)

    def _sorted(self, queue=None, updated=None, tags: typing.List[str] = None) -> typing.List[MockTicket]:
        if updated:
            updated = datetime.strptime(updated, LAST_UPDATE_FMT).timestamp()
        self.logger.info('updated: %s', updated)
        values: typing.Iterable[MockTicket] = self.data.values()
        self.logger.info('values: %s', values)
        values = [
            t for t in values if (
                (not queue or t.key.startswith(queue.upper()))
                and (not updated or datetime.strptime(t.updatedAt, ST_DATE_FMT).timestamp() >= updated)
                and (not tags or any(tag in tags for tag in t.tags))
            )
        ]

        return sorted(values, key=lambda t: int(from_st_str_time_to_timestamp(t.updatedAt)))

    def __getitem__(self, issue_key) -> MockTicket:
        return self.data[issue_key]


class StClientMock(object):
    def __init__(self, name='st_mock', *args, **kwargs):
        self.issues = MockIssuesCollection()
        self.queues = StartrekQueueCollectionMock()
        self.components = StartrekComponentCollectionMock()
        self.name = name

    def set_data(self, data: typing.Iterable[MockTicket]):
        self.issues.set_data(data)

    def clear_data(self):
        self.issues.clear_data()

    @staticmethod
    def add_checklist_items(key: str, items: typing.List[str] = None):
        return {'checklistItems': []}
