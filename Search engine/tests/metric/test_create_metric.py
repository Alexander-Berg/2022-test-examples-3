from search.martylib.core.exceptions import NotAuthorized
from search.martylib.db_utils import prepare_db, session_scope
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import metric_pb2
from search.mon.warden.sqla.warden.model import MetricType, Metric
from search.mon.warden.src.services.model import Warden

from search.mon.warden.tests.utils.creators import create_metric_types

WARDEN_CLIENT = Warden()


class TestMetricCreate(TestCase):

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
            is_private=True,
        )
        create_metric_types((metric_type_1, ))

    def tearDown(self):
        with session_scope() as session:
            session.query(Metric).filter(Metric.type == 'test').delete()

    def _check_metric(self, metric: metric_pb2.Metric):
        self.assertEqual(len(metric.owners.logins), 4)
        self.assertEqual(metric.key, 'test')
        self.assertEqual(metric.name, 'Test')
        self.assertEqual(metric.type, 'test')
        self.assertIn('test-user', metric.owners.logins)
        self.assertIn('test-user-1', metric.owners.logins)
        self.assertIn('test-user-2', metric.owners.logins)
        self.assertIn('test-user-3', metric.owners.logins)

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_metric_create(self):
        response = WARDEN_CLIENT.create_metric(
            metric_pb2.Metric(
                type='test',
                name='Test',
                key='test',
                owners=metric_pb2.MetricOwners(logins=['test-user-2', 'test-user-3'])
            ),
            context=None
        )
        self._check_metric(response.metric)
        self.assertEqual(response.error, '')

        with session_scope() as session:
            metric = session.query(Metric).filter(Metric.type == 'test', Metric.key == 'test').one()
            self._check_metric(metric.to_protobuf())

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_metric_create_not_found(self):
        response = WARDEN_CLIENT.create_metric(
            metric_pb2.Metric(
                type='test-not-found',
                name='Test',
                key='test',
                owners=metric_pb2.MetricOwners(logins=['test-user-2', 'test-user-3'])
            ),
            context=None
        )
        self.assertEqual(response.error, 'Metric type not found')

    @TestCase.mock_auth(login='test-user-2')
    def test_metric_create_not_authorized(self):
        try:
            WARDEN_CLIENT.create_metric(
                metric_pb2.Metric(
                    type='test',
                    name='Test',
                    key='test-not-authorized',
                    owners=metric_pb2.MetricOwners(logins=['test-user-2', 'test-user-3'])
                ),
                context=None
            )
        except NotAuthorized:
            pass
        else:
            self.assertFalse('Only admins allowed to create private metrics, test case not passed')
