from search.martylib.db_utils import prepare_db, session_scope, clear_db, generate_field_name as F
from search.martylib.http.exceptions import NotFound
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import incident_pb2, metric_pb2
from search.mon.warden.sqla.warden.model import Component, Incident, MetricValue
from search.mon.warden.src.services.reducers import IncidentReducer

from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils import setup
from search.mon.warden.src.services.model import Warden


WARDEN_CLIENT = Warden()


EXPECTED_METRICS = [
    {
        'metric': metric_pb2.MetricValue(
            incident_id='k1',
            metric_key='private-metric',
            value=30,
        ),
        'expected_for': ['admin', 'authorized_metric_user']
    },
    {
        'metric': metric_pb2.MetricValue(
            incident_id='k1',
            metric_key='allowed-metric',
            value=35,
        ),
        'expected_for': ['admin', 'other_authorized_metric_user']
    },
    {
        'metric': metric_pb2.MetricValue(
            incident_id='k1',
            metric_key='ydt',
            value=20,
        ),
        'expected_for': ['admin', 'anyone', 'authorized_metric_user', 'other_authorized_metric_user']
    }
]


class TestGetIncident(BaseTestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()

        with session_scope() as session:
            setup.setup_metrics()
            c_1 = Component(
                name='t_c_1',
                parent_component_name="t_c_p_1",
                tier="tier_1",
                abc_service_slug='abc1',
                slug='t_c_p_1__t_c_1',
            )
            session.add(c_1)
            session.commit()

            session.add(
                Incident(
                    key='k1',
                    component=[c_1],
                    created=1293840000,  # 01.01.2011
                )
            )

            session.commit()
            setup.setup_metric_values('k1')
            setup.setup_common_metric_values('k1')

    @classmethod
    def tearDownClass(cls):
        clear_db()

    @TestCase.mock_auth(login='login', roles=['warden/admin'])
    def test_get_existed_incident_admin(self):
        self._test_get_existed('admin')

    @TestCase.mock_auth(login='login')
    def test_get_existed_incident_anyone(self):
        self._test_get_existed('anyone')

    @TestCase.mock_auth(login='owner-user')
    def test_get_existed_incident_authorized(self):
        self._test_get_existed('authorized_metric_user')

    @TestCase.mock_auth(login='test-user')
    def test_get_existed_incident_authorized_other(self):
        self._test_get_existed('other_authorized_metric_user')

    def _test_get_existed(self, expected_for: str):
        res = WARDEN_CLIENT.get_incident(
            incident_pb2.GetIncidentRequest(
                incident_key='k1'
            ),
            context=None,
        )
        self.assertEqual(res.key, 'k1', msg='Incident exists')
        self.assertEqual(res.created, 1293840000, msg='Incident exists')
        got_metrics = list(res.metrics)
        expected_value = [m['metric'] for m in EXPECTED_METRICS if expected_for in m['expected_for']]
        self.assertEqual(len(got_metrics), len(expected_value), msg='Incident exists')
        for metric in got_metrics:
            for field in [F(MetricValue.id), F(MetricValue.calculation_time)]:
                metric.ClearField(field)
        for metric in expected_value:
            self.assertIn(metric, got_metrics)

    def test_get_unexisted_incident(self):
        incident_reducer = IncidentReducer()
        self.assertRaisesWithMessage(
            NotFound,
            incident_reducer.get_incident,
            request=incident_pb2.GetIncidentRequest(incident_key='k_zero'),
            msg='Incident does not exist'
        )
