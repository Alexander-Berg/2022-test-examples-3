from search.martylib.core.exceptions import NotAuthorized
from search.martylib.db_utils import prepare_db, session_scope
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import metric_pb2
from search.mon.warden.sqla.warden.model import MetricType, Metric
from search.mon.warden.src.services.model import Warden

from search.mon.warden.tests.utils import setup

WARDEN_CLIENT = Warden()


class TestMetricUpdate(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        setup.setup_metrics()

    @classmethod
    def tearDownClass(cls):
        with session_scope() as session:
            session.query(MetricType).delete(synchronize_session=False)

    def _check_metric(self, metric: metric_pb2.Metric, expected: metric_pb2.Metric):
        self.assertEqual(len(metric.owners.logins), len(expected.owners.logins))
        self.assertEqual(metric.key, expected.key)
        self.assertEqual(metric.name, expected.name)
        self.assertEqual(metric.type, expected.type)
        self.assertEqual(len(set(metric.owners.logins)), len(set(expected.owners.logins)))

        for owner in metric.owners.logins:
            self.assertIn(owner, expected.owners.logins)

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_metric_update_not_found(self):
        response = WARDEN_CLIENT.update_metric(metric_pb2.Metric(type='test-not-found', key='test'), context=None)
        self.assertEqual(response.error, 'Metric not found')
        self.assertEqual(response.metric.key, '')

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def est_metric_update_all_fields_admin(self):
        update_values = metric_pb2.Metric(
            type='test',
            key='test-1',
            name='Brand New Name',
            owners=metric_pb2.MetricOwners(logins=['new-user-1']),
        )
        with session_scope() as session:
            response = WARDEN_CLIENT.update_metric(
                update_values,
                context=None
            )
            metric = session.query(Metric).filter(Metric.type == 'test', Metric.key == 'test-1').one()

            self.assertEqual(response.error, '')
            self._check_metric(response.metric, update_values)
            self._check_metric(metric.to_protobuf(), update_values)

            session.rollback()

    @TestCase.mock_auth(login='user-not-authorized')
    def test_metric_update_not_authorized(self):
        update_values = metric_pb2.Metric(
            type=setup.PRIVATE_METRIC_TYPE,
            key=setup.PRIVATE_METRIC_KEY,
        )
        self.assertRaises(
            NotAuthorized,
            WARDEN_CLIENT.update_metric,
            request=update_values,
            context=None
        )

    @TestCase.mock_auth(login='test-user')
    def test_metric_update_observer(self):
        update_values = metric_pb2.Metric(
            type=setup.PRIVATE_METRIC_TYPE,
            key=setup.ALLOWED_METRIC_KEY,
            name='Brand New Name',
        )
        self.assertRaises(
            NotAuthorized,
            WARDEN_CLIENT.update_metric,
            request=update_values,
            context=None
        )

    @TestCase.mock_auth(login='owner-user')
    def test_metric_update_owner(self):
        update_values = metric_pb2.Metric(
            type=setup.PRIVATE_METRIC_TYPE,
            key=setup.ALLOWED_METRIC_KEY,
            name='Brand New Name',
        )
        self.assertRaises(
            NotAuthorized,
            WARDEN_CLIENT.update_metric,
            request=update_values,
            context=None
        )
