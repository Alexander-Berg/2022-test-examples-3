import re

from search.martylib.http.exceptions import BadRequest, TooManyRequests

from search.mon.warden.proto.structures import component_check_pb2
from search.mon.warden.src import const
from search.mon.warden.src.services.reducers.validators import functionality as v_functionality, component as v_component
from search.mon.warden.tests.utils.base_test_case import BaseTestCase


class TestWardenValidateComponent(BaseTestCase):
    maxDiff = None

    def test_transform_check_values_to_causes(self):
        test_cases = [
            {
                'name': 'Values have functionalities list',
                'values': {
                    'functionalities': ['1', '2', '3']
                },
                'expected_res': component_check_pb2.ComponentCheckCauses(
                    functional_ids=['1', '2', '3'],
                ),
            },
            {
                'name': 'Values have alerts list',
                'values': {
                    'alerts': ['1', '2', '3']
                },
                'expected_res': component_check_pb2.ComponentCheckCauses(
                    alert_ids=['1', '2', '3'],
                ),
            },
            {
                'name': 'Values have alerts and functionalities list',
                'values': {
                    'alerts': ['1', '2', '3'],
                    'functionalities': ['4', '5', '6']
                },
                'expected_res': component_check_pb2.ComponentCheckCauses(
                    alert_ids=['1', '2', '3'],
                    functional_ids=['4', '5', '6']
                ),
            },
            {
                'name': 'Values are empty',
                'values': {},
                'expected_res': component_check_pb2.ComponentCheckCauses(),
            }

        ]

        for test_case in test_cases:
            res = v_functionality.Functionality().transform_check_values_to_causes(test_case['values'])
            self.assertEqual(
                res,
                test_case['expected_res'],
                msg=test_case['name'],
            )

    def test_get_check_priority(self):
        test_cases = [
            {
                'name': 'Check is in critical list',
                'check_name': const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT,
                'critical_checks': {const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT},
                'expected_res': component_check_pb2.ComponentCheck.Priority.CRITICAL,
            },
            {
                'name': 'Check is not in critical list',
                'check_name': const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT,
                'critical_checks': {const.HAS_FUNCTIONALITY_WITH_BACKGROUND_DOWNTIME_ALERT},
                'expected_res': component_check_pb2.ComponentCheck.Priority.WARNING,
            },
        ]

        for test_case in test_cases:
            res = v_functionality.Functionality().get_check_priority(test_case['check_name'], test_case['critical_checks'])
            self.assertEqual(
                res,
                test_case['expected_res'],
                msg=test_case['name']
            )

    def test_check_logins(self):
        validator = v_component.ComponentValidator()
        validator.clients.staff = StaffMock()
        validator.clients.staff.set_dismissed_logins({'dismissed_chuvak'})

        test_cases = [
            ({'lebedev-aa', 'gvdozd', 'pufit'}, True),  # All correct
            ({'dismissed_chuvak', 'gvdozd'}, False),  # One is dismissed
            ({'lebedev_aa', 'pufit'}, False),  # One is misspelled
        ]
        for logins, expected in test_cases:
            # noinspection PyProtectedMember
            res = validator._check_logins(logins)
            self.assertEqual(res, expected)

    def test_check_chat_link(self):
        validator = v_component.ComponentValidator()
        validator.clients.yaincbot = YaIncBotMock()

        correct_links = [
            'https://t.me/joinchat/thisishash8',
            'tg://join?invite=thisishash8'
        ]
        incorrect_links = [
            'https://yndx-search.slack.com/archives/C01JMFEGAQP'
        ]

        for link in correct_links:
            self.assertTrue(validator._check_chat_link(link), f'failed link {link}')

        for link in incorrect_links:
            self.assertFalse(validator._check_chat_link(link), f'failed link {link}')

        validator.clients.yaincbot.set_flood(True)

        for link in (*correct_links, *incorrect_links):
            self.assertTrue(validator._check_chat_link(link), f'failed link {link}')


class StaffMock:
    def __init__(self):
        self.dismissed_logins = set()

    def set_dismissed_logins(self, logins: set):
        self.dismissed_logins.update(logins)

    def person_details(self, login, short_info=True):
        return {
            'result': [
                {'official': {'is_dismissed': login in self.dismissed_logins}}
            ]
        }


class YaIncBotMock:
    flood = False
    chat_regexp = re.compile(r'^(?:https://t\.me/joinchat/|tg://join\?invite=)([\w-]+)')

    def set_flood(self, value: bool):
        self.flood = value

    def _validate(self, url: str):
        """Mocks response from YaIncBot"""
        if self.flood:
            raise TooManyRequests()

        match = self.chat_regexp.fullmatch(url)
        if not match:
            raise BadRequest(f'match: {match}, url: {url}')

    def validate_join_url(self, join_url: str):
        try:
            self._validate(join_url)
        except TooManyRequests:
            return True
        except BadRequest:
            return False

        return True
