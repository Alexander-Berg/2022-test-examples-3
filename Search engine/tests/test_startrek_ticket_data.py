import typing

from search.martylib.core.logging_utils import configure_binlog
from search.martylib.test_utils import TestCase

from search.mon.tickenator_on_db.proto.structures import manual_ticket_pb2, tickenator_pb2, task_pb2
from search.mon.tickenator_on_db.src.reducers.progress_steps.utils import ticket_creation
from search.mon.tickenator_on_db.src.reducers.progress_steps.task_runners.manual_ticket_creation_runner import ManualTicketCreationStepRunner
from search.mon.warden.proto.structures import component_pb2, duty_pb2, owner_pb2


class MockStaffClient:
    def __init__(self, external_list: typing.List[str]):
        self.external_list = external_list

    def person_details(self, login):
        return {'official': {'affiliation': 'external' if login in self.external_list else 'yandex'}}


class MockComponentStorageClient:
    def get_component_abc_id(self, abc_slug: str) -> typing.Optional[int]:
        if abc_slug == 'abc_test_parent_component':
            return 1
        elif abc_slug == 'abc_test_component':
            return 2
        return None


class MockStartrekStorageClient:
    def __init__(self):
        self.actual_calls = []

    def prepare_queue_component(self, queue, component):
        self.actual_calls.append((queue, component))


class TestStartrekTicketData(TestCase):

    @classmethod
    def setUpClass(cls):
        configure_binlog(
            'tickenator',
            loggers=('tickenator', 'martylib', 'zephyr'),
            stdout=True,
        )

    def _run_rtc_noc_test_cases(self, test_cases: typing.List[dict], create_ticket_data: str, ticket_type):
        manual_ticket_creation_runner = ManualTicketCreationStepRunner()

        for test_case in test_cases:
            manual_ticket_creation_runner.clients.staff = MockStaffClient(test_case.get('external_list', []))
            manual_ticket_creation_runner.startrek_storage = MockStartrekStorageClient()
            manual_ticket_creation_runner.config.server_config.ticket_author = 'config.server_config.ticket_author'

            response_data = getattr(manual_ticket_creation_runner, create_ticket_data)(ticket_type(**test_case['test_data']))

            self.assertDictEqual(test_case['expected_data'], response_data, msg=test_case['name'])
            self.assertListEqual(manual_ticket_creation_runner.startrek_storage.actual_calls, test_case.get('startrek_storage_calls', []))

    def test_rtc_data(self):
        test_cases = [
            {
                'name': 'Common case',
                'test_data': {
                    'author': 'test_author',
                    'queue': 'test_queue',
                    'title': 'test_title',
                    'ticket_type': 'test_type',
                    'links': [
                        manual_ticket_pb2.Link(relationship='test_relationship1', issue='test_issue1'),
                        manual_ticket_pb2.Link(relationship='test_relationship2', issue='test_issue2'),
                    ],
                    'component': 'test_component',
                    'tags': ['test_tag1', 'test_tag2', 'test_tag3'],
                    'description': 'test description',
                },
                'expected_data': {
                    'author': 'test_author',
                    'queue': 'test_queue',
                    'summary': 'test_title',
                    'type': {'name': 'test_type'},
                    'links': [
                        {'relationship': 'test_relationship1', 'issue': 'test_issue1'},
                        {'relationship': 'test_relationship2', 'issue': 'test_issue2'},
                    ],
                    'tags': ['test_tag1', 'test_tag2', 'test_tag3'],
                    'description': 'test description',
                },
            },
            {
                'name': 'No component and no ticket_type',
                'test_data': {
                    'author': 'test_author',
                    'queue': 'test_queue',
                    'title': 'test_title',
                },
                'expected_data': {
                    'author': 'test_author',
                    'queue': 'test_queue',
                    'summary': 'test_title',
                    'type': {'name': 'Task'},
                },
            },
            {
                'name': 'External author',
                'test_data': {
                    'author': 'test_external_author',
                    'queue': 'test_queue',
                    'title': 'test_title',
                },
                'external_list': ['test_external_author'],
                'expected_data': {
                    'author': 'config.server_config.ticket_author',
                    'queue': 'test_queue',
                    'summary': 'test_title',
                    'type': {'name': 'Task'},
                },
            },
            {
                'name': 'RTC template description',
                'test_data': {
                    'author': 'test_author',
                    'use_template': True,
                    'description': 'test_description',
                    'nanny_service': 'test_nanny_service',
                    'service': 'test_service',
                },
                'expected_data': {
                    'author': 'test_author',
                    'type': {'name': 'Task'},
                    'description': ticket_creation.get_rtc_ticket_description(
                        manual_ticket_pb2.RtcManualTicket(
                            description='test_description',
                            nanny_service='test_nanny_service',
                            service='test_service',
                        )
                    )
                },
            },
        ]

        self._run_rtc_noc_test_cases(test_cases, '_create_rtc_ticket_data', manual_ticket_pb2.RtcManualTicket)

    def test_noc_data(self):
        test_cases = [
            {
                'name': 'Common case',
                'test_data': {
                    'author': 'test_author',
                    'queue': 'test_queue',
                    'title': 'test_title',
                    'links': [
                        manual_ticket_pb2.Link(relationship='test_relationship1', issue='test_issue1'),
                        manual_ticket_pb2.Link(relationship='test_relationship2', issue='test_issue2'),
                    ],
                    'description': 'test description',
                    'location': 'test_location_with__',
                    'broken_subsystem_functionality': 'test_broken_subsystem_functionality_with__'
                },
                'expected_data': {
                    'author': 'test_author',
                    'queue': 'test_queue',
                    'summary': 'test_title',
                    'type': {'name': 'Incident'},
                    'links': [
                        {'relationship': 'test_relationship1', 'issue': 'test_issue1'},
                        {'relationship': 'test_relationship2', 'issue': 'test_issue2'},
                    ],
                    'description': 'test description',
                    'tags': ['test location with  ', 'test:broken:subsystem:functionality:with::']
                },
            },
            {
                'name': 'External author and queue: TEST',
                'test_data': {
                    'author': 'test_external_author',
                    'queue': 'TEST',
                    'title': 'test_title',
                },
                'external_list': ['test_external_author'],
                'expected_data': {
                    'author': 'config.server_config.ticket_author',
                    'queue': 'TEST',
                    'summary': 'test_title',
                    'type': {'name': 'Task'},
                },
            },
            {
                'name': 'NOC template description',
                'test_data': {
                    'author': 'test_author',
                    'use_template': True,
                    'description': 'test_description',
                    'affected_services': 'test_affected_services',
                    'localization': 'test_localization',
                    'additional': 'test_additional',
                },
                'expected_data': {
                    'author': 'test_author',
                    'type': {'name': 'Incident'},
                    'description': ticket_creation.get_noc_ticket_description(
                        manual_ticket_pb2.NocManualTicket(
                            description='test_description',
                            affected_services='test_affected_services',
                            localization='test_localization',
                            additional='test_additional'
                        )
                    ),
                },
            },
        ]

        self._run_rtc_noc_test_cases(test_cases, '_create_noc_ticket_data', manual_ticket_pb2.NocManualTicket)

    def _test_base_ticket_data(
        self,
        ticket_task: task_pb2.TicketTask,
        manual_ticket: manual_ticket_pb2.ManualTicket,
        expected_data: typing.Dict,
        external_list: typing.Optional[typing.List[str]] = None,
        expected_calls: typing.Optional[typing.List[typing.Tuple[str, str]]] = None
    ):
        if external_list is None:
            external_list = []

        manual_ticket_creation_runner = ManualTicketCreationStepRunner()
        manual_ticket_creation_runner.clients.staff = MockStaffClient(external_list)
        manual_ticket_creation_runner.component_storage = MockComponentStorageClient()
        manual_ticket_creation_runner.startrek_storage = MockStartrekStorageClient()
        manual_ticket_creation_runner.config.server_config.ticket_author = 'config.server_config.ticket_author'

        response = manual_ticket_creation_runner._create_base_ticket_data(ticket_task, manual_ticket)
        self.assertDictEqual(response, expected_data)
        if expected_calls is not None:
            self.assertListEqual(manual_ticket_creation_runner.startrek_storage.actual_calls, expected_calls)

    def test_base_data(self):
        manual_ticket = manual_ticket_pb2.ManualTicket(
            author='test_author',
            parent_component='test_parent_component',
            child_component='test_child_component',
            environment='prod',
            notification_time='1640164680.146',
            start_time='1640164653.146',
            end_time='1640164713.146',
            title='test title',
            queue='SPI',
            creation_source=tickenator_pb2.CreationSource.UI,
            support_line=component_pb2.SupportLine.platform,
        )

        ticket_task = task_pb2.TicketTask(
            warden_data=task_pb2.WardenData(
                parent_component=component_pb2.Component(
                    name='test_parent_component',
                    abc_service_slug='abc_test_parent_component',
                    duty_list=component_pb2.DutyList(on_duty=[
                        duty_pb2.OnDuty(login='a'), duty_pb2.OnDuty(login='b'), duty_pb2.OnDuty(login='c')
                    ]),
                    owner_list=[owner_pb2.Owner(login='d')],
                ),
                component=component_pb2.Component(
                    name='test_component',
                    abc_service_slug='abc_test_component',
                )
            ),
            manual_ticket=manual_ticket,
            ticket_queue='SPI',
        )

        expected_data = {
            'notificationTime': 1640164680146,
            'supportLine': 'platform',
            'queue': 'SPI',
            'author': 'test_author',
            'tags': [
                'product:PROD-TEST_PARENT_COMPONENT',
                'service:TEST_CHILD_COMPONENT',
                'env:prod',
                'dc:all',
                'source:manual',
            ],
            'summary': ticket_creation.get_manual_title(manual_ticket),
            'incidentProtocol': 'green',
            'incidentReporter': 'user',
            'description': ticket_creation.create_manual_ticket_description(manual_ticket),
            'assignee': 'd',
            'followers': ['a', 'b', 'c'],
            'type': {'name': 'Incident'},
            'duty': ['a', 'b', 'c'],
            'minusDc': 'Нет',
            'sreBeginTime': 1640164653146,
            'sreEndTime': 1640164713146,
            'abcService': [1, 2],
            'components': ['test_parent_component'],
        }

        external_list = []
        expected_calls = [('SPI', 'test_parent_component')]

        self._test_base_ticket_data(ticket_task, manual_ticket, expected_data, external_list, expected_calls)

        external_list = ['test_author', 'b']
        expected_data['author'] = 'config.server_config.ticket_author'
        expected_data['duty'] = ['a', 'c']
        expected_data['followers'] = ['a', 'c']
        self._test_base_ticket_data(ticket_task, manual_ticket, expected_data, external_list, expected_calls)
