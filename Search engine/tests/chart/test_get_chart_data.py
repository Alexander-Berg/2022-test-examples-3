from datetime import datetime, timedelta

from search.martylib.core.date_utils import now
from search.martylib.db_utils import session_scope
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import chart_pb2, preset_pb2, date_pb2
from search.mon.warden.sqla.warden.model import Incident, Component, MetricValue
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils import setup
from search.mon.warden.tests.utils.base_test_case import BaseTestCase


WARDEN_CLIENT = Warden()

timestamp = int(now().timestamp())
yesterday_timestamp = timestamp - timedelta(days=1).total_seconds()
two_days_ago_timestamp = timestamp - timedelta(days=2).total_seconds()


class TestGetChartData(BaseTestCase):

    def load_to_db():
        with session_scope() as session:
            component = Component(name='test_component', slug='test_component')
            setup.setup_metrics()

            incident = Incident(
                key='TEST_CHART',
                created=two_days_ago_timestamp,
                status="status",
                summary="summary",
                incident_time=two_days_ago_timestamp,
                yandex_downtime=2,
                component=[component]
            )
            metric = MetricValue(metric_key="ydt", value=1.2)
            metric2 = MetricValue(metric_key="ydt", value=1.3)
            incident.metrics = [metric, metric2]

            incident2 = Incident(
                key='TEST_CHART2',
                created=yesterday_timestamp,
                status="status2",
                summary="summary2",
                incident_time=yesterday_timestamp,
                yandex_downtime=3,
                component=[component]
            )
            metric3 = MetricValue(metric_key="ydt", value=1.22)
            metric4 = MetricValue(metric_key="ydt", value=1.32)
            incident2.metrics = [metric3, metric4]

            component.incidents = [incident, incident2]
            session.add(component)

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_get_chart_data_case(self):
        chart_data = WARDEN_CLIENT.get_chart_data(
            chart_pb2.getChartDataRequest(
                component_name='test_component',
                metric_key="ydt",
                period=preset_pb2.DatePreset.TWO_WEEKS,
                custom_period_interval=date_pb2.FilterInterval(start=0, end=0),
                tags=[],
                tier="",
            ),
            context=None,
        )

        self.assertEqual(1, len(chart_data.all_components))
        self.assertEqual("test_component", chart_data.all_components[0])
        self.assertEqual(4, len(chart_data.points))

        today = datetime.fromtimestamp(timestamp).strftime('%Y.%m.%d')
        yesterday = datetime.fromtimestamp(yesterday_timestamp).strftime('%Y.%m.%d')
        two_days_ago = datetime.fromtimestamp(two_days_ago_timestamp).strftime('%Y.%m.%d')
        two_weeks_ago = datetime.fromtimestamp(timestamp - timedelta(days=14).total_seconds()).strftime('%Y.%m.%d')

        self.assertEqual(two_weeks_ago, chart_data.points[0].date)

        correct_points = (
            (two_days_ago, 1, two_days_ago_timestamp, 'TEST_CHART', 'status', 'summary', 2.5, 1, 'test_component', 2, 2, 2.5, 2.5, round((1 - 2 / timedelta(days=1).total_seconds()) * 100, 2)),
            (yesterday, 1, yesterday_timestamp, 'TEST_CHART2', 'status2', 'summary2', 2.54, 1, 'test_component', 5, 5, 5.04, 5.04, round((1 - 3 / timedelta(days=1).total_seconds()) * 100, 2)),
            (today, 1, yesterday_timestamp, 'TEST_CHART2', 'status2', 'summary2', 2.54, 1, 'test_component', 5, 5, 5.04, 5.04, round((1 - 3 / timedelta(days=14).total_seconds()) * 100, 2))
        )
        for i, correct_point in enumerate(correct_points):
            self.assertEqual(correct_point[0], chart_data.points[i + 1].date)
            self.assertEqual(correct_point[1], len(chart_data.points[i + 1].incidents))
            self.assertEqual(correct_point[2], chart_data.points[i + 1].incidents[0].created)
            self.assertEqual(correct_point[3], chart_data.points[i + 1].incidents[0].key)
            self.assertEqual(correct_point[4], chart_data.points[i + 1].incidents[0].status)
            self.assertEqual(correct_point[5], chart_data.points[i + 1].incidents[0].summary)
            self.assertEqual(correct_point[6], round(chart_data.points[i + 1].incidents[0].metric_val, 2))
            self.assertEqual(correct_point[7], len(chart_data.points[i + 1].incidents[0].components))
            self.assertEqual(correct_point[8], chart_data.points[i + 1].incidents[0].components[0])
            self.assertEqual(correct_point[9], chart_data.points[i + 1].vertical_downtimes['test_component'])
            self.assertEqual(correct_point[10], chart_data.points[i + 1].total_ydt)
            self.assertEqual(correct_point[11], round(chart_data.points[i + 1].total_metric_val, 2))
            self.assertEqual(correct_point[12], round(chart_data.points[i + 1].vertical_metrics['test_component'], 2))
            self.assertEqual(correct_point[13], round(chart_data.points[i + 1].stability, 2))
