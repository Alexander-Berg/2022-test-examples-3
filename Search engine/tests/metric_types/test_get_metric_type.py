from search.martylib.db_utils import prepare_db, session_scope
from search.martylib.test_utils import TestCase
from search.martylib.protobuf_utils import replace_in_repeated
from search.martylib.http.exceptions import NotFound

from search.mon.warden.proto.structures import metric_pb2
from search.mon.warden.sqla.warden.model import MetricType
from search.mon.warden.src.services.model import Warden

from search.mon.warden.tests.utils.creators import create_metric_types, create_metrics
from search.mon.warden.tests.utils.clients import Clients

WARDEN_CLIENT = Warden()
WARDEN_CLIENT.clients = Clients()
WARDEN_CLIENT.auth.clients = Clients()


class TestMetricType(TestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        cls.load_to_db()

    @classmethod
    def tearDownClass(cls):
        with session_scope() as session:
            session.query(MetricType).delete(synchronize_session=False)

    @staticmethod
    def load_to_db():
        metric_types = [
            metric_pb2.MetricType(
                key='test',
                name='test-name',
                owners=metric_pb2.MetricOwners(logins=['test-user-1', 'test-user-2'], abc=['test-abc-1']),
                is_private=True,
            ),
            metric_pb2.MetricType(
                key='test-public',
                name='test-name-public',
                owners=metric_pb2.MetricOwners(logins=['test-user-1']),
                is_private=False,
            ),
        ]

        create_metric_types(metric_types)
        create_metrics((
            metric_pb2.Metric(type='test', key='test-1', name='Test 1'),
            metric_pb2.Metric(type='test', key='test-2.1', name='Test 2.1',
                              owners=metric_pb2.MetricOwners(logins=['test-user-3'], abc=['test-abc-2'])),
            metric_pb2.Metric(type='test-public', key='test-3', name='Test 3'),
            metric_pb2.Metric(type='test-public', key='test-4', name='Test 4'),
            metric_pb2.Metric(type='test-public', key='test-5', name='Test 5'),
        ))

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_get_type_not_exists(self):
        try:
            WARDEN_CLIENT.get_metric_type(metric_pb2.GetMetricTypeRequest(key='no_such_type'), context=None)
        except NotFound:
            pass
        else:
            self.assertTrue(False, 'Failed to raise exception on bad metric_type')

    @TestCase.mock_auth(login='test-user-not-authorized')
    def test_get_public_type(self):
        with session_scope() as session:
            response = WARDEN_CLIENT.get_metric_type(metric_pb2.GetMetricTypeRequest(key='test-public'), context=None)
            expected = session.query(MetricType).filter(MetricType.key == 'test-public').first()
            self.assertEqual(len(response.metric_type.instances), len(expected.instances))

    @TestCase.mock_auth(login='test-user-2')
    def test_get_private_type_owner(self):
        with session_scope() as session:
            response = WARDEN_CLIENT.get_metric_type(metric_pb2.GetMetricTypeRequest(key='test'), context=None)
            expected = session.query(MetricType).filter(MetricType.key == 'test').first()
            self.assertEqual(len(response.metric_type.instances), len(expected.instances))

    @TestCase.mock_auth(login='test-abc-1-user')
    def test_get_private_type_abc_owner(self):
        with session_scope() as session:
            response = WARDEN_CLIENT.get_metric_type(metric_pb2.GetMetricTypeRequest(key='test'), context=None)
            expected = session.query(MetricType).filter(MetricType.key == 'test').first()
            self.assertEqual(len(response.metric_type.instances), len(expected.instances))

    @TestCase.mock_auth(login='test-user-not-owner', roles=['warden/admin'])
    def test_get_private_type_admin(self):
        with session_scope() as session:
            response = WARDEN_CLIENT.get_metric_type(metric_pb2.GetMetricTypeRequest(key='test'), context=None)
            expected = session.query(MetricType).filter(MetricType.key == 'test').first()
            self.assertEqual(len(response.metric_type.instances), len(expected.instances))

    @TestCase.mock_auth(login='test-user-3')
    def test_get_private_type_metric_owner(self):
        with session_scope() as session:
            response = WARDEN_CLIENT.get_metric_type(metric_pb2.GetMetricTypeRequest(key='test'), context=None)
            expected = session.query(MetricType).filter(MetricType.key == 'test').first()
            replace_in_repeated(
                expected.instances,
                set(
                    [
                        instance for instance in expected.instances
                        if 'test-user-3' in instance.to_protobuf().owners.logins
                    ]
                )
            )
            self.assertEqual(len(response.metric_type.instances), len(expected.instances))

    @TestCase.mock_auth(login='test-user-not-authorized')
    def test_get_private_type_not_authorized(self):
        response = WARDEN_CLIENT.get_metric_type(metric_pb2.GetMetricTypeRequest(key='test'), context=None)
        self.assertEqual(len(response.metric_type.instances), 0)

    @TestCase.mock_auth(login='test-abc-2-user')
    def test_get_private_type_metric_abc_owner(self):
        with session_scope() as session:
            response = WARDEN_CLIENT.get_metric_type(metric_pb2.GetMetricTypeRequest(key='test'), context=None)
            expected = session.query(MetricType).filter(MetricType.key == 'test').first()
            replace_in_repeated(
                expected.instances,
                set(
                    [
                        instance for instance in expected.instances
                        if 'test-abc-2' in instance.to_protobuf().owners.abc
                    ]
                )
            )
            self.assertEqual(len(response.metric_type.instances), len(expected.instances))
