from search.martylib.db_utils import session_scope, clear_db, to_model
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, duty_pb2, user_pb2, owner_pb2
from search.mon.warden.sqla.warden.model import Component
from search.mon.warden.src.services.model import Warden

from search.mon.warden.tests.utils import setup
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_components
from search.mon.warden.tests.utils.clients import Clients

WARDEN_CLIENT = Warden()
WARDEN_CLIENT.clients = Clients()


class TestAuthCheck(BaseTestCase):

    @classmethod
    def tearDownClass(cls):
        clear_db()

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        setup.setup_metrics()

        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='test-auth',
                    abc_service_slug='test-auth',
                    owner_list=[owner_pb2.Owner(login='owner')],
                    duty_list=component_pb2.DutyList(on_duty=[duty_pb2.OnDuty(role='', login='duty-user')]),
                )
            )
        )

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_admin_role(self):
        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test-auth').one()
            self.assertTrue(WARDEN_CLIENT.auth.check_component_role(session, component))

    @TestCase.mock_auth(login='owner')
    def test_owner_auth_roles(self):
        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test-auth').one()
            self.assertTrue(WARDEN_CLIENT.auth.check_component_role(session, component))

    @TestCase.mock_auth(login='owner_2')
    def test_no_owner_auth_roles(self):
        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test-auth').one()
            self.assertFalse(WARDEN_CLIENT.auth.check_component_role(session, component))

    @TestCase.mock_auth(login='test-auth-user-1')
    def test_validate_user(self):
        test_data = [
            {'users': ['test-auth-user-1'], 'result': True},
            {'abc': ['test-auth'], 'result': True},
            {'result': False},
            {'users': ['test-auth-user-2'], 'result': False},
        ]
        for test_case in test_data:
            self.assertEqual(WARDEN_CLIENT.auth.validate_user(test_case.get('users', []), test_case.get('abc', [])), test_case['result'])

    @TestCase.mock_auth(login='duty-user')
    def test_duty_auth_role(self):
        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test-auth').one()
            self.assertTrue(WARDEN_CLIENT.auth.check_component_role(session, component))

    @TestCase.mock_auth(login='curator-user')
    def test_curator_auth_role(self):
        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test-auth').one()
            curator = to_model(user_pb2.User(login='curator-user'))
            session.add(curator)
            component.curator_list.append(curator)
            session.commit()
            self.assertTrue(WARDEN_CLIENT.auth.check_curator_role(session, 'test-auth'))
            self.assertTrue(WARDEN_CLIENT.auth.check_component_role(session, component))
