from search.martylib.db_utils import prepare_db, session_scope
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import metric_pb2
from search.mon.warden.sqla.warden.model import MetricType
from search.mon.warden.src.services.model import Warden

from search.mon.warden.tests.utils.setup import setup_metrics
from search.mon.warden.tests.utils.clients import Clients

WARDEN_CLIENT = Warden()
WARDEN_CLIENT.clients = Clients()
WARDEN_CLIENT.auth.clients = Clients()


class TestMetricList(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        setup_metrics(default_metrics=False)

    @classmethod
    def tearDownClass(cls):
        with session_scope() as session:
            session.query(MetricType).delete(synchronize_session=False)

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_get_metrics_admin(self):
        response = WARDEN_CLIENT.get_metric_list(metric_pb2.GetMetricListRequest(), context=None)
        self.assertEqual(len(response.metrics), 2)

    @TestCase.mock_auth(login='test-user-not-authorized')
    def test_get_metrics_not_authorized(self):
        response = WARDEN_CLIENT.get_metric_list(metric_pb2.GetMetricListRequest(), context=None)
        self.assertEqual(len(response.metrics), 0)

    @TestCase.mock_auth(login='test-user')
    def test_get_metrics_observer(self):
        response = WARDEN_CLIENT.get_metric_list(metric_pb2.GetMetricListRequest(), context=None)
        self.assertEqual(len(response.metrics), 1)

    @TestCase.mock_auth(login='test-abc-user-1')
    def test_get_metrics_abc_observer(self):
        response = WARDEN_CLIENT.get_metric_list(metric_pb2.GetMetricListRequest(), context=None)
        self.assertEqual(len(response.metrics), 1)
