from search.martylib.db_utils import prepare_db, clear_db, session_scope
from search.martylib.test_utils import TestCase
from search.martylib.core.date_utils import now

from search.mon.warden.proto.structures import metric_pb2, incident_pb2
from search.mon.warden.sqla.warden.model import Incident, Component
from search.mon.warden.src.services.model import Warden

from search.mon.warden.tests.utils import setup

WARDEN_CLIENT = Warden()


class TestMetricTypeMetricValues(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        incidents_number = 2
        with session_scope() as session:
            setup.setup_metrics()
            component = Component(
                name='test_component',
            )
            for i in range(incidents_number):
                incident = Incident(
                    created=int(now().timestamp()) - i * 3600,
                    key=f'TEST-KEY-{i+1}',
                )
                component.incidents.append(incident)
                session.add(incident)

            session.add(component)

            cls.add_values()

    @staticmethod
    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def add_values():
        WARDEN_CLIENT.add_metric_value(
            metric_pb2.AddMetricValueRequest(
                incident_key='TEST-KEY-1',
                metric_key=setup.PRIVATE_METRIC_KEY,
                value=1,
            ),
            context=None,
        )
        WARDEN_CLIENT.add_metric_value(
            metric_pb2.AddMetricValueRequest(
                incident_key='TEST-KEY-1',
                metric_key=setup.ALLOWED_METRIC_KEY,
                value=2,
            ),
            context=None,
        )
        WARDEN_CLIENT.add_metric_value(
            metric_pb2.AddMetricValueRequest(
                incident_key='TEST-KEY-2',
                metric_key=setup.PRIVATE_METRIC_KEY,
                value=3,
            ),
            context=None,
        )

    @classmethod
    def tearDownClass(cls):
        clear_db()

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_admin_user(self):
        response = WARDEN_CLIENT.get_metric_type_metric_values(
            incident_pb2.GetMetricTypeMetricValuesRequest(
                key=setup.PRIVATE_METRIC_TYPE
            ),
            context=None,
        )
        self.assertEqual(len(response.objects), 2)

    @TestCase.mock_auth(login='test-user')
    def test_authorized_user(self):
        response = WARDEN_CLIENT.get_metric_type_metric_values(
            incident_pb2.GetMetricTypeMetricValuesRequest(
                key=setup.PRIVATE_METRIC_TYPE
            ),
            context=None,
        )
        self.assertEqual(len(response.objects), 1)

    @TestCase.mock_auth(login='unauthorized-user')
    def test_unauthorized_user(self):
        response = WARDEN_CLIENT.get_metric_type_metric_values(
            incident_pb2.GetMetricTypeMetricValuesRequest(
                key=setup.PRIVATE_METRIC_TYPE
            ),
            context=None,
        )
        self.assertEqual(len(response.objects), 0)
