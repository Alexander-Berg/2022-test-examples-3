import typing

from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import duty_pb2
from search.mon.warden.src import const
from search.mon.warden.src.services.reducers import duty


class MockDutiesCalendarClient:
    EXPECT_CALL_TO_NOC = 'NOC'
    EXPECT_CALL_TO_DC = 'DC'
    EXPECT_CALL_TO_DC_ENG = 'DC_ENG'

    expected_calls = []
    actual_calls = []

    def __init__(self, list_of_expected_calls: list):
        self.expected_calls = []
        self.actual_calls = []
        self.expected_calls = list_of_expected_calls

    def get_noc_roles(self) -> typing.List[duty_pb2.OnDuty]:
        self.actual_calls.append(self.EXPECT_CALL_TO_NOC)
        return [duty_pb2.OnDuty(role='test1', login='tester_1')]

    def get_dc_roles(self, dc_type: str) -> typing.List[duty_pb2.OnDuty]:
        if dc_type == const.DC_TYPE_BASIC:
            self.actual_calls.append(self.EXPECT_CALL_TO_DC)
            return [duty_pb2.OnDuty(role='test2', login='tester_2')]
        if dc_type == const.DC_TYPE_ENG:
            self.actual_calls.append(self.EXPECT_CALL_TO_DC_ENG)
            return [duty_pb2.OnDuty(role='test3', login='tester_3')]


class TestABSSyncGetCustomDuties(TestCase):
    maxDiff = None

    def test_fill_functionalities_check_map_for_component(self):
        duty_reducer = duty.DutyReducer()

        test_cases = [
            {
                'name': 'Unknown custom name type',
                'custom_name': '123',
                'expected_list': [],
                'expected_calls': []
            },
            {
                'name': 'Custom name with noc 1',
                'custom_name': 'https://noc.hot-desk.hd.yandex-team.ru',
                'expected_list': [duty_pb2.OnDuty(role='test1', login='tester_1')],
                'expected_calls': [MockDutiesCalendarClient.EXPECT_CALL_TO_NOC]
            },
            {
                'name': 'Custom name with noc 2',
                'custom_name': 'https://bot.yandex-team.ru/sip/noc/',
                'expected_list': [duty_pb2.OnDuty(role='test1', login='tester_1')],
                'expected_calls': [MockDutiesCalendarClient.EXPECT_CALL_TO_NOC]
            },
            {
                'name': 'Custom name with dc 1',
                'custom_name': 'https://bot.yandex-team.ru/api/sip/get-agents.php?by=instance_name&name=dc',
                'expected_list': [duty_pb2.OnDuty(role='test2', login='tester_2')],
                'expected_calls': [MockDutiesCalendarClient.EXPECT_CALL_TO_DC]
            },
            {
                'name': 'Custom name with dc 2',
                'custom_name': 'https://bot.yandex-team.ru/sip/dc/?section=overall',
                'expected_list': [duty_pb2.OnDuty(role='test2', login='tester_2')],
                'expected_calls': [MockDutiesCalendarClient.EXPECT_CALL_TO_DC]
            },
            {
                'name': 'Custom name with dc eng 1',
                'custom_name': 'https://bot.yandex-team.ru/sip/dceng/',
                'expected_list': [duty_pb2.OnDuty(role='test3', login='tester_3')],
                'expected_calls': [MockDutiesCalendarClient.EXPECT_CALL_TO_DC_ENG]
            },
            {
                'name': 'Custom name with dc eng 2',
                'custom_name': 'https://bot.yandex-team.ru/api/sip/get-agents.php?by=instance_name&name=dc_eng',
                'expected_list': [duty_pb2.OnDuty(role='test3', login='tester_3')],
                'expected_calls': [MockDutiesCalendarClient.EXPECT_CALL_TO_DC_ENG]
            },

        ]

        for test_case in test_cases:
            client_mock = MockDutiesCalendarClient(list_of_expected_calls=test_case['expected_calls'])

            duty_reducer.clients.noc_duties_calendar = client_mock
            duty_reducer.clients.dc_duties_calendar = client_mock

            response_list = duty_reducer.get_custom_duties(test_case['custom_name'])
            self.assertEqual(response_list, test_case['expected_list'], msg=test_case['name'])
            self.assertEqual(client_mock.expected_calls, client_mock.actual_calls, msg=test_case['name'])
