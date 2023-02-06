from typing import Iterable

from search.martylib.test_utils import TestCase
from search.mon.workplace.protoc.structures.calendar.calendar_pb2 import Decision, Event, Participant
from search.mon.workplace.protoc.structures.catalog_pb2 import OndutyRole

from search.mon.workplace.src.libs.catalog.calendar_utils import parse_onduty


def gen_simple_logins(*logins) -> list:
    result = []
    for login in logins:
        result.append(
            OndutyRole(login=login)
        )

    return result


def gen_role_logins(roles, *logins) -> list:
    result = []
    for i, login in enumerate(logins):
        result.append(
            OndutyRole(login=login, role=roles[i])
        )

    return result


class TestCalendarParseLogins(TestCase):
    events = [
        # Simple events
        (Event(name='Дежурный картинок @gvdozd'),),
        (Event(name='Дежурный картинок gvdozd@'),),
        (Event(name='@robot-searchmon'),),
        (Event(name='robot-searchmon@'),),
        (Event(name='robot-searchmon@ @gvdozd'),),
        (Event(name='Тестируем всякое тут'),),

        # Several simple events
        (Event(name='@gvdozd'), Event(name='@epsilond1'),),
        (Event(name='@gvdozd'), Event(name='robot-searchmon@'),),
        (Event(name='@robot-searchmon'), Event(name='robot-searchmon@'),),

        # Roles
        (Event(name='Дежурные 1 - gvdozd@; 2 - epsilond1@.'),),
        (Event(name='Дежурные 1: gvdozd@ 2 - epsilond1@'),),
        (Event(name='gvdozd@; owner: epsilond1@'),),
        (Event(name='1 - robot-searchmon@'),),
        (Event(name='1: @gvdozd'), Event(name='2: epsilond1@'),),

        # Empty
        tuple(),

        # Participants
        (Event(name='Дежурные сервиса',
               participants=(
                   Participant(login='gvdozd', decision=Decision.YES, is_organizer=False),)
               ),
         ),
        (Event(name='Duty',
               participants=(
                   Participant(login='gvdozd', decision=Decision.YES, is_organizer=False),
                   Participant(login='epsilond1', decision=Decision.YES, is_organizer=True)),
               ),
         ),
        (Event(name='Just event',
               participants=(
                   Participant(login='gvdozd', decision=Decision.YES, is_organizer=False),
                   Participant(login='epsilond1', decision=Decision.YES, is_organizer=True)),
               ),
         ),
        (Event(name='duty',
               participants=(
                   Participant(login='gvdozd', decision=Decision.NO, is_organizer=False),
                   Participant(login='epsilond1', decision=Decision.YES, is_organizer=True)),
               ),
         ),
        (Event(name='дежурные',
               participants=(
                   Participant(login='gvdozd', decision=Decision.UNDECIDED, is_organizer=False),
                   Participant(login='epsilond1', decision=Decision.YES, is_organizer=True)),
               ),
         ),

        # Non trivial events from production
        (Event(name='Дежурство в рантайме - Саша П.',
               participants=(
                   Participant(login='asp437', is_organizer=False),
                   Participant(login='luorlova', is_organizer=True)),
               ),
         ),
        (Event(name='Дежурство в рантайме: Саша П.',
               participants=(
                   Participant(login='asp437', is_organizer=False),
                   Participant(login='luorlova', is_organizer=True)),
               ),
         ),
    ]

    logins = [
        # Simple Events
        gen_simple_logins('gvdozd'),
        gen_simple_logins('gvdozd'),
        [],
        [],
        gen_simple_logins('gvdozd'),
        [],

        # Several simple events
        gen_simple_logins('gvdozd', 'epsilond1'),
        gen_simple_logins('gvdozd'),
        [],

        # Roles
        gen_role_logins(('1', '2'), 'gvdozd', 'epsilond1'),
        gen_role_logins(('1', '2'), 'gvdozd', 'epsilond1'),
        gen_role_logins((None, 'owner'), 'gvdozd', 'epsilond1'),
        [],
        gen_role_logins(('1', '2'), 'gvdozd', 'epsilond1'),

        # Empty
        [],

        # Participants
        gen_simple_logins('gvdozd'),
        gen_simple_logins('gvdozd'),
        [],
        [],
        gen_simple_logins('gvdozd'),

        # Non trivial events from production
        gen_simple_logins('asp437'),
        gen_simple_logins('asp437'),
    ]

    def assertEqualLogins(self, from_calendar: Iterable[OndutyRole], expected: Iterable[OndutyRole], test_num: int):
        self.assertEqual(len(from_calendar), len(expected),
                         msg=f'Неверное количество логинов в тесте #{test_num}. Количество: {len(from_calendar)}; '
                         f'ожидалось: {len(expected)}')

        for login1, login2 in zip(from_calendar, expected):
            self.assertEqual(login1.role, login2.role,
                             msg=f'Неверная роль дежурного в тесте #{test_num}. Роль: {login1.role}; '
                             f'ожидалось: {login2.role}')

            self.assertEqual(login1.login, login2.login,
                             msg=f'Неправильный логин в тесте #{test_num}. Логин: {login1.login}; '
                             f'ожидалось: {login2.role}')

    def test_parse_logins(self):
        index = 0
        for events, logins in zip(self.events, self.logins):
            self.assertEqualLogins(parse_onduty(events), logins, index)
            index += 1
