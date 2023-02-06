from search.martylib.db_utils import prepare_db, clear_db, session_scope
from search.martylib.test_utils import TestCase
from search.martylib.core.date_utils import now
from search.martylib.core.exceptions import NotAuthorized

from search.mon.warden.proto.structures import metric_pb2
from search.mon.warden.sqla.warden.model import Incident, Component, MetricValueChange, MetricValue
from search.mon.warden.src.services.model import Warden

from search.mon.warden.tests.utils import setup

WARDEN_CLIENT = Warden()


class TestMetricValue(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        with session_scope() as session:
            setup.setup_metrics()
            incident = Incident(
                created=int(now().timestamp()),
                key='TEST-KEY',
            )
            component = Component(
                name='test_component',
                slug='test_component',
            )
            component.incidents.append(incident)

            incident_with_values = Incident(
                created=int(now().timestamp()),
                key='INCIDENT-WITH-VALUES',
            )
            for key in (setup.PRIVATE_METRIC_KEY, setup.ALLOWED_METRIC_KEY):
                value = MetricValue(
                    incident_id='INCIDENT-WITH-VALUES',
                    metric_key=key,
                    value=20,
                    incident=incident_with_values,
                )
                session.add(value)

            session.add(incident)
            session.add(incident_with_values)
            session.add(component)

    @classmethod
    def tearDownClass(cls):
        clear_db()

    def tearDown(self) -> None:
        with session_scope() as session:
            session.query(MetricValue).filter(MetricValue.incident_id == 'TEST-KEY').delete()
            session.query(MetricValueChange).delete()

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_add_metric_value_admin(self):
        WARDEN_CLIENT.add_metric_value(
            metric_pb2.AddMetricValueRequest(
                incident_key='TEST-KEY',
                metric_key=setup.PRIVATE_METRIC_KEY,
                value=12,
            ),
            context=None,
        )

        with session_scope() as session:
            metric_value = session.query(MetricValue).filter(MetricValue.incident_id == 'TEST-KEY').one()
            self.assertEqual(metric_value.incident_id, 'TEST-KEY')
            self.assertEqual(metric_value.metric_key, setup.PRIVATE_METRIC_KEY)
            self.assertEqual(metric_value.value, 12)

    @TestCase.mock_auth(login='test-user')
    def test_add_metric_value_not_authorized(self):
        self.assertRaises(
            NotAuthorized,
            WARDEN_CLIENT.add_metric_value,
            request=metric_pb2.AddMetricValueRequest(
                incident_key='TEST-KEY',
                metric_key=setup.PRIVATE_METRIC_KEY,
                value=12,
            ),
            context=None,
        )

        with session_scope() as session:
            metric_value = session.query(MetricValue).filter(MetricValue.incident_id == 'TEST-KEY').one_or_none()
            self.assertIsNone(metric_value)

    @TestCase.mock_auth(login='test-user')
    def test_add_metric_value_authorized(self):
        WARDEN_CLIENT.add_metric_value(
            metric_pb2.AddMetricValueRequest(
                incident_key='TEST-KEY',
                metric_key=setup.ALLOWED_METRIC_KEY,
                value=12,
            ),
            context=None,
        )

        with session_scope() as session:
            metric_value = session.query(MetricValue).filter(MetricValue.incident_id == 'TEST-KEY').one()
            self.assertEqual(metric_value.incident_id, 'TEST-KEY')
            self.assertEqual(metric_value.metric_key, setup.ALLOWED_METRIC_KEY)
            self.assertEqual(metric_value.value, 12)

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_delete_metric_value_admin(self):
        with session_scope() as session:
            WARDEN_CLIENT.delete_metric_value(
                metric_pb2.DeleteMetricValueRequest(
                    incident_key='INCIDENT-WITH-VALUES',
                    metric_key=setup.PRIVATE_METRIC_KEY,
                ),
                context=None,
            )

            metric_value = session.query(MetricValue).filter(
                MetricValue.incident_id == 'INCIDENT-WITH-VALUES',
                MetricValue.metric_key == setup.PRIVATE_METRIC_KEY,
            ).one_or_none()
            session.rollback()
            self.assertIsNone(metric_value)

    @TestCase.mock_auth(login='test-user')
    def test_delete_metric_value_authorized(self):
        with session_scope() as session:
            WARDEN_CLIENT.delete_metric_value(
                metric_pb2.DeleteMetricValueRequest(
                    incident_key='INCIDENT-WITH-VALUES',
                    metric_key=setup.ALLOWED_METRIC_KEY,
                ),
                context=None,
            )

            metric_value = session.query(MetricValue).filter(
                MetricValue.incident_id == 'INCIDENT-WITH-VALUES',
                MetricValue.metric_key == setup.ALLOWED_METRIC_KEY,
            ).one_or_none()
            session.rollback()
            self.assertIsNone(metric_value)

    @TestCase.mock_auth(login='test-user')
    def test_delete_metric_value_not_authorized(self):
        with session_scope() as session:
            self.assertRaises(
                NotAuthorized,
                WARDEN_CLIENT.delete_metric_value,
                request=metric_pb2.DeleteMetricValueRequest(
                    incident_key='INCIDENT-WITH-VALUES',
                    metric_key=setup.PRIVATE_METRIC_KEY,
                ),
                context=None,
            )

            session.rollback()
