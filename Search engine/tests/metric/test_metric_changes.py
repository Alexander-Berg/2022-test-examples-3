from search.martylib.db_utils import prepare_db, clear_db, session_scope  # session_scope,
from search.martylib.test_utils import TestCase
from search.martylib.core.date_utils import now

from search.mon.warden.proto.structures import metric_pb2
from search.mon.warden.sqla.warden.model import Incident, Component, MetricValueChange, MetricValue
from search.mon.warden.src.services.model import Warden

from search.mon.warden.tests.utils.creators import create_metric_types, create_metrics

WARDEN_CLIENT = Warden()


class TestMetricChanges(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        with session_scope() as session:
            metric_type = metric_pb2.MetricType(
                key='test-type',
                name='test-name',
                owners=metric_pb2.MetricOwners(logins=['test-user-1', 'test-user-2']),
                is_private=True,
            )
            create_metric_types((metric_type,))
            create_metrics((
                metric_pb2.Metric(type='test-type', key='test-metric', name='Test 1'),
            ))
            incident = Incident(
                created=int(now().timestamp()),
                key='TEST-KEY',
            )
            component = Component(
                name='test_component',
                slug='test_component',
            )
            component.incidents.append(incident)
            session.add(incident)
            session.add(component)

    @classmethod
    def tearDownClass(cls):
        clear_db()

    def tearDown(self) -> None:
        with session_scope() as session:
            session.query(MetricValue).delete()
            session.query(MetricValueChange).delete()

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_add_metric_value(self):
        WARDEN_CLIENT.add_metric_value(metric_pb2.AddMetricValueRequest(
            incident_key='TEST-KEY',
            metric_key='test-metric',
            value=12,
        ), context=None)

        # metric_change.action becomes int after this call
        metric_changes = WARDEN_CLIENT.get_metric_value_change_list(request=None, context=None).objects
        self.assertEqual(len(metric_changes), 1)
        metric_change = metric_changes[-1]
        self.assertEqual(metric_change.incident_id, 'TEST-KEY')
        self.assertEqual(metric_change.action, metric_pb2.MetricValueChange.ActionType.add)
        self.assertEqual(metric_change.metric_key, 'test-metric')
        self.assertEqual(metric_change.value, 12)
        self.assertEqual(metric_change.user, 'test-user')

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_change_metric_value(self):
        WARDEN_CLIENT.add_metric_value(metric_pb2.AddMetricValueRequest(
            incident_key='TEST-KEY',
            metric_key='test-metric',
            value=12,
        ), context=None)
        WARDEN_CLIENT.add_metric_value(metric_pb2.AddMetricValueRequest(
            incident_key='TEST-KEY',
            metric_key='test-metric',
            value=13,
        ), context=None)
        metric_changes = WARDEN_CLIENT.get_metric_value_change_list(request=None, context=None).objects
        self.assertEqual(len(metric_changes), 2)
        metric_change = metric_changes[-1]
        self.assertEqual(metric_change.incident_id, 'TEST-KEY')
        self.assertEqual(metric_change.action, metric_pb2.MetricValueChange.ActionType.modify)
        self.assertEqual(metric_change.metric_key, 'test-metric')
        self.assertEqual(metric_change.value, 13)
        self.assertEqual(metric_change.user, 'test-user')

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_delete_metric_value(self):
        WARDEN_CLIENT.add_metric_value(metric_pb2.AddMetricValueRequest(
            incident_key='TEST-KEY',
            metric_key='test-metric',
            value=12,
        ), context=None)
        WARDEN_CLIENT.delete_metric_value(metric_pb2.DeleteMetricValueRequest(
            incident_key='TEST-KEY',
            metric_key='test-metric',
        ), context=None)
        metric_changes = WARDEN_CLIENT.get_metric_value_change_list(request=None, context=None).objects
        self.assertEqual(len(metric_changes), 2)
        metric_change = metric_changes[-1]
        self.assertEqual(metric_change.incident_id, 'TEST-KEY')
        self.assertEqual(metric_change.action, metric_pb2.MetricValueChange.ActionType.delete)
        self.assertEqual(metric_change.metric_key, 'test-metric')
        self.assertEqual(metric_change.value, 0)
        self.assertEqual(metric_change.user, 'test-user')
