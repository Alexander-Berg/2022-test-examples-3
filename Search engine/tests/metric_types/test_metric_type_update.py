from search.martylib.db_utils import prepare_db, session_scope
from search.martylib.core.exceptions import NotAuthorized
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import metric_pb2
from search.mon.warden.sqla.warden.model import MetricType
from search.mon.warden.src.services.model import Warden

from search.mon.warden.tests.utils.creators import create_metric_types

WARDEN_CLIENT = Warden()


class TestMetricTypeUpdate(TestCase):

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
        metric_type_1 = metric_pb2.MetricType(
            key='test',
            name='test-name',
            owners=metric_pb2.MetricOwners(logins=['test-user-1', 'test-user-2']),
        )
        metric_type_2 = metric_pb2.MetricType(
            key='test-2',
            name='test-name',
            owners=metric_pb2.MetricOwners(logins=['test-user-1', 'test-user-2']),
        )
        create_metric_types((metric_type_1, metric_type_2))

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_metric_type_update(self):
        response = WARDEN_CLIENT.update_metric_type(
            metric_pb2.MetricType(
                key='test',
                name='test-name-update',
                owners=metric_pb2.MetricOwners(logins=['test-user-1']),
                is_additive=True,
                is_private=True,
                print_in_spi=True,
                ydt_convert_coefficient=10.4,
                consider_weights=True,
            ),
            context=None,
        )

        self.assertEqual(response.metric_type.key, 'test')
        self.assertEqual(response.metric_type.name, 'test-name-update')
        self.assertEqual(len(response.metric_type.owners.logins), 2)
        self.assertIn('test-user-1', response.metric_type.owners.logins)
        self.assertIn('test-user', response.metric_type.owners.logins)
        self.assertTrue(response.metric_type.print_in_spi)
        self.assertTrue(response.metric_type.is_additive)
        self.assertTrue(response.metric_type.is_private)
        self.assertEqual(response.metric_type.ydt_convert_coefficient, 10.4)
        self.assertTrue(response.metric_type.consider_weights)

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_metric_type_not_found(self):
        response = WARDEN_CLIENT.update_metric_type(metric_pb2.MetricType(key='test-not-found', name='test-name-update'), context=None)
        self.assertEqual(response.error, 'Metric not found')

    @TestCase.mock_auth(login='test-user-2')
    def test_metric_type_not_authorized(self):
        try:
            WARDEN_CLIENT.update_metric_type(metric_pb2.MetricType(key='test-2', name='test-name-update-not-authorized'), context=None)
        except NotAuthorized:
            pass
        else:
            self.assertFalse('Only admin allowed to update metric types,  case not passed')

        with session_scope() as session:
            metric_type = session.query(MetricType).filter(MetricType.key == 'test-2', MetricType.name == 'test-name-update-not-authorized').one_or_none()
            self.assertIsNone(metric_type)

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_metric_type_with_instances(self):
        WARDEN_CLIENT.create_metric(metric_pb2.Metric(key='test-metric', type='test', name='Test Metric'), context=None)
        WARDEN_CLIENT.update_metric_type(
            metric_pb2.MetricType(
                key='test',
                name='test-name-update',
                owners=metric_pb2.MetricOwners(logins=['test-user-1']),
                is_additive=True,
            ),
            context=None,
        )
        with session_scope() as session:
            metric_type = session.query(MetricType).filter(MetricType.key == 'test').one_or_none()
            metric_type_proto = metric_type.to_protobuf()
            self.assertEqual(len(metric_type_proto.instances), 1)
