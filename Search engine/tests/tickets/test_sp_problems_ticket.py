from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, tickets_pb2
from search.mon.warden.sqla.warden.model import Component
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_components

WARDEN_CLIENT = Warden()


class TestSPProblemTicket(BaseTestCase):
    maxDiff = None

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(slug='test_create_spproblems_ticket_c2', abc_service_slug='test_create_spproblems_ticket_abc_service_2')
            ),
        )

    def test_validate_sp_problem_ticket_info(self):
        test_cases = [
            {
                'name': 'All good',
                'ticket_info': tickets_pb2.SPProblemTicket(
                    problem_name='p1',
                    component_name='c1',
                    weight=4,
                    description='d1',
                ),
                'component': Component(),
                'expected_res': [],
            },
            {
                'name': 'No component',
                'ticket_info': tickets_pb2.SPProblemTicket(
                    problem_name='p1',
                    component_name='c1',
                    weight=4,
                    description='d1',
                ),
                'component': None,
                'expected_res': ['this component does not exist'],
            },
            {
                'name': 'Empty problem name',
                'ticket_info': tickets_pb2.SPProblemTicket(
                    problem_name='',
                    component_name='c1',
                    weight=4,
                    description='d1',
                ),
                'component': Component(),
                'expected_res': ['problem_name should not be empty'],
            },
            {
                'name': 'Empty component name',
                'ticket_info': tickets_pb2.SPProblemTicket(
                    problem_name='p1',
                    component_name='',
                    weight=4,
                    description='d1',
                ),
                'component': Component(),
                'expected_res': ['component_name should not be empty'],
            },
            {
                'name': 'Empty weight',
                'ticket_info': tickets_pb2.SPProblemTicket(
                    problem_name='p1',
                    component_name='c1',
                    weight=0,
                    description='d1',
                ),
                'component': Component(),
                'expected_res': ['weight should not be empty'],
            },
            {
                'name': 'Empty description',
                'ticket_info': tickets_pb2.SPProblemTicket(
                    problem_name='p1',
                    component_name='c1',
                    weight=4,
                    description='',
                ),
                'component': Component(),
                'expected_res': ['description should not be empty'],
            },
            {
                'name': 'Two problems together',
                'ticket_info': tickets_pb2.SPProblemTicket(
                    problem_name='',
                    component_name='c1',
                    weight=4,
                    description='d1',
                ),
                'component': None,
                'expected_res': ['this component does not exist', 'problem_name should not be empty'],
            },
        ]

        for test_case in test_cases:
            res = WARDEN_CLIENT.ticket_reducer.validate_sp_problem_ticket_info(test_case['ticket_info'], test_case['component'])
            self.assertEqual(res, test_case['expected_res'], msg=test_case['name'])

    @TestCase.mock_auth(login='test-user')
    def test_create_spproblem_description(self):
        test_cases = [
            {
                'name': 'Login, problem_name, component_name, weight, description',
                'ticket_info': tickets_pb2.SPProblemTicket(
                    problem_name='p1',
                    component_name='c1',
                    weight=4,
                    description='d1',
                ),
                'expected_res': 'test-user Название проблемы:\n p1 \nКомпонента:\n c1 \n Вес проблемы: \n 4 \n Описание проблемы: \n d1 \n',
            },
            {
                'name': 'Login, problem_name, component_name, weight, description, affected_services',
                'ticket_info': tickets_pb2.SPProblemTicket(
                    problem_name='p1',
                    component_name='c1',
                    weight=4,
                    description='d1',
                    affected_services=['a1', 'a2']
                ),
                'expected_res': 'test-user Название проблемы:\n p1 \nПроблема касается сервисов: \n a1, a2 \nКомпонента:\n c1 \n Вес проблемы: \n 4 \n Описание проблемы: \n d1 \n',
            },
            {
                'name': 'Login, problem_name, component_name, weight, description, docs_for_debug',
                'ticket_info': tickets_pb2.SPProblemTicket(
                    problem_name='p1',
                    component_name='c1',
                    weight=4,
                    description='d1',
                    docs_for_debug='dd1',
                ),
                'expected_res': 'test-user Название проблемы:\n p1 \nКомпонента:\n c1 \n Вес проблемы: \n 4 \n Описание проблемы: \n d1 \nДокументация для дебага: \n dd1 \n',
            },
            {
                'name': 'Login, problem_name, component_name, weight, description, how_to_resolve',
                'ticket_info': tickets_pb2.SPProblemTicket(
                    problem_name='p1',
                    component_name='c1',
                    weight=4,
                    description='d1',
                    how_to_resolve='htd1'
                ),
                'expected_res': 'test-user Название проблемы:\n p1 \nКомпонента:\n c1 \n Вес проблемы: \n 4 \n Описание проблемы: \n d1 \nРешение или что с этим делать: \n htd1 \n',
            },
        ]

        for test_case in test_cases:
            res = WARDEN_CLIENT.ticket_reducer.create_spproblem_description(test_case['ticket_info'])
            self.assertEqual(res, test_case['expected_res'], msg=test_case['name'])

    @TestCase.mock_auth(login='test-user')
    def test_get_sp_problem_ticket_fields(self):
        test_cases = [
            {
                'name': 'All good',
                'ticket_info': tickets_pb2.SPProblemTicket(
                    problem_name='p1',
                    component_name='c1',
                    weight=4,
                    description='d1',
                ),
                'component': component_pb2.Component(name='cc2'),
                'expected_res': {
                    'queue': 'SPPROBLEM',
                    'summary': '[cc2] p1',
                    'description': 'test-user Название проблемы:\n p1 \nКомпонента:\n c1 \n Вес проблемы: \n 4 \n Описание проблемы: \n d1 \n',
                    'components': ['cc2'],
                    'tags': ['spi:problem'],
                    'createdBy': 'test-user',
                },
            },
        ]

        for test_case in test_cases:
            res = WARDEN_CLIENT.ticket_reducer.get_sp_problem_ticket_fields(test_case['ticket_info'], test_case['component'])
            self.assertEqual(res, test_case['expected_res'], msg=test_case['name'])

    @TestCase.mock_auth(login='test-user')
    def test_create_spproblems_ticket(self):
        test_cases = [
            {
                'name': 'Invalid request',
                'request': tickets_pb2.CreateTicketRequest(
                    sp_problem=tickets_pb2.SPProblemTicket(
                        problem_name='p1',
                        component_name='c1',
                        weight=4,
                        description='d1',
                    )
                ),
                'expected_res': tickets_pb2.CreateTicketResponse(
                    error='this component does not exist'
                )
            },
            {
                'name': 'All good',
                'request': tickets_pb2.CreateTicketRequest(
                    sp_problem=tickets_pb2.SPProblemTicket(
                        problem_name='p1',
                        component_name='test_create_spproblems_ticket_c2',
                        weight=4,
                        description='d1',
                    )
                ),
                'expected_res': tickets_pb2.CreateTicketResponse(
                    ticker_number='new_key',
                )
            }

        ]
        for test_case in test_cases:
            res = WARDEN_CLIENT.ticket_reducer.create_ticket(test_case['request'], None)
            self.assertEqual(res, test_case['expected_res'], msg=test_case['name'])
