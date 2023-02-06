from search.martylib.core.date_utils import now
from search.martylib.test_utils import TestCase
from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.mon.warden.proto.structures import report_pb2, incident_pb2
from search.mon.warden.proto.structures.component import common_pb2
from search.mon.warden.src.services.model import Warden
from search.mon.warden.sqla.warden.model import Incident, Component, ActionItem, LSR, ComponentTag
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils import setup

WARDEN_CLIENT = Warden()


class TestWardenGetReport(BaseTestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()

        with session_scope() as session:
            # Tests 1-2, 5
            component = Component(
                name='test_component',
                parent_component_name='parent_component',
                abc_service_slug='test',
                slug='parent_component__test_component',
            )
            incident = Incident(
                key='TEST_SPI',
                created=int(now().timestamp()),
            )
            action_item = ActionItem(
                key='TEST_ITEM',
                resolved=True,
            )
            lsr = LSR(
                key='TEST_LSR',
                created=int(now().timestamp()),
            )
            component.incidents.append(incident)
            component.lsrs.append(lsr)
            incident.action_items.append(action_item)
            session.add(component)
            session.commit()
            # Test 3
            tag = ComponentTag(
                tag='test',
            )
            component2 = Component(
                name='component2',
                abc_service_slug='test2',
                tags=[tag],
                slug='component2',
            )
            incident2 = Incident(
                key='SPI_2',
                created=int(now().timestamp()),
                yandex_downtime=20.5,
            )
            action_item2 = ActionItem(
                key='ITEM_2',
                resolved=True,
            )
            component2.incidents.append(incident2)
            incident2.action_items.append(action_item2)
            incident3 = Incident(
                key='SPI_3',
                created=int(now().timestamp()),
                solved=True,
                solved_time=(now().timestamp() + 86400),
                yandex_downtime=10,
            )
            incident3.action_items += [
                ActionItem(
                    resolved=True,
                    key='ITEM_3'
                ),
                ActionItem(
                    resolved=False,
                    key='ITEM_4'
                ),
            ]
            component2.incidents.append(incident3)
            component2.incidents.append(Incident(
                key='SPI_old',
                created=int(now().timestamp() - 3000000)  # a little more than a month
            ))
            session.add(component2)

            component3 = Component(
                name='component3',
                abc_service_slug='test3',
                tags=[tag],
                slug='component3',
            )
            component3.incidents.append(incident3)
            incident4 = Incident(
                key='SPI_4',
                created=int(now().timestamp()),
                solved=False,
            )
            component3.incidents.append(incident4)
            session.add(component3)

            # Test 4 (report statistic: auto_created_downtime)
            component4 = Component(
                name='test-metrics',
                abc_service_slug='test4',
                slug='test-metrics',
            )

            component4.incidents.append(
                Incident(
                    key='test-metrics-1',
                    created=int(now().timestamp()),
                    yandex_downtime=25,
                    markup=incident_pb2.IncidentMarkup(auto_calculated_downtime=True).SerializeToString(),
                )
            )
            component4.incidents.append(
                Incident(
                    key='test-metrics-2',
                    created=int(now().timestamp()),
                    yandex_downtime=15,
                )
            )
            component4.incidents.append(
                Incident(
                    key='test-metrics-3',
                    created=int(now().timestamp()),
                    markup=incident_pb2.IncidentMarkup(auto_calculated_downtime=True).SerializeToString(),
                )
            )

            session.add(component4)

            setup.setup_metric_values(incident_key='test-metrics-1')
            setup.setup_metric_values(incident_key='test-metrics-2')

    @classmethod
    def tearDownClass(cls):
        clear_db()

    @TestCase.mock_auth(login='test-user')
    def test_get_report_case(self):
        report = WARDEN_CLIENT.get_report(
            report_pb2.GetReportRequest(
                component_name='test_component',
                parent_component_name='parent_component',
                period=report_pb2.ReportPeriod.review,
            ),
            context=None,
        )

        self.assertEqual(len(report.incidents), 1)
        self.assertEqual(report.incidents[0].key, 'TEST_SPI')
        self.assertEqual(len(report.lsrs), 1)
        self.assertEqual(report.lsrs[0].key, 'TEST_LSR')

    @TestCase.mock_auth(login='test-user')
    def test_get_report_all(self):
        # check that there is no problem with processing period All (WORKPLACE-691)
        report = WARDEN_CLIENT.get_report(
            report_pb2.GetReportRequest(
                component_name='test_component',
                parent_component_name='parent_component',
                period=report_pb2.ReportPeriod.all,
            ),
            context=None,
        )
        self.assertEqual(len(report.incidents), 1)

    @TestCase.mock_auth(login='test-user')
    def test_get_components_report(self):
        reports = WARDEN_CLIENT.get_components_report(
            report_pb2.GetComponentsReportRequest(
                components=[common_pb2.ComponentFilter(
                    component_name='test_component',
                    parent_component_name='parent_component',
                )],
                period=report_pb2.CustomPeriod(
                    start=int(now().timestamp()) - 2592000,  # about month ago
                    end=int(now().timestamp()),
                )
            ),
            context=None,
        )

        self.assertIn('test_component:parent_component', reports.component_reports)
        component_report = reports.component_reports['test_component:parent_component']
        self.assertEqual(component_report.name, 'test_component')
        self.assertEqual(component_report.parent_name, 'parent_component')
        report = component_report.report
        self.assertEqual(len(report), 4)
        self.assertIn('spi', report)
        self.assertIn('ai', report)
        self.assertIn('ydt', report)
        self.assertIn('custom_metric', report)
        self.assertEqual(len(report['spi'].stats), 2)
        self.assertEqual(report['spi'].stats['total'], 1)
        self.assertEqual(report['spi'].stats['solved'], 0)
        self.assertEqual(len(report['ai'].stats), 2)
        self.assertEqual(report['ai'].stats['total'], 1)
        self.assertEqual(report['ai'].stats['resolved'], 1)
        self.assertEqual(len(report['ydt'].stats), 1)
        self.assertEqual(len(report['custom_metric'].stats), 1)

    @TestCase.mock_auth(login='test-user')
    def test_get_components_report_many(self):

        reports = WARDEN_CLIENT.get_components_report(
            report_pb2.GetComponentsReportRequest(
                tags=['test'],
                period=report_pb2.CustomPeriod(
                    start=int(now().timestamp()) - 2592000,  # about month ago
                    end=int(now().timestamp()),
                )
            ),
            context=None,
        )

        self.assertIn('component2:', reports.component_reports)
        self.assertIn('component3:', reports.component_reports)
        report2 = reports.component_reports['component2:'].report
        self.assertEqual(report2['spi'].stats['total'], 2)
        self.assertEqual(report2['spi'].stats['solved'], 1)
        self.assertEqual(report2['ai'].stats['total'], 3)
        self.assertEqual(report2['ai'].stats['resolved'], 2)
        self.assertEqual(report2['ydt'].stats['total'], 30.5)
        report3 = reports.component_reports['component3:'].report
        self.assertEqual(report3['spi'].stats['total'], 2)
        self.assertEqual(report3['spi'].stats['solved'], 1)
        self.assertEqual(report3['ai'].stats['total'], 2)
        self.assertEqual(report3['ai'].stats['resolved'], 1)

    @TestCase.mock_auth(login='test-user')
    def test_auto_created_downtime(self):
        request = report_pb2.GetReportRequest(
            component_name='test-metrics',
            parent_component_name='',
            period=report_pb2.ReportPeriod.month,
        )

        expected_statistic = report_pb2.StatisticStatus(
            value=100.0 * 25 / 40,
            absolute_value=25,
            total_value=40,
            target=0,
            status=True,
        )

        report = WARDEN_CLIENT.get_report(request=request, context=None)

        self.assertEqual(report.statistic.auto_calculated_downtime, expected_statistic)

    @TestCase.mock_auth(login='test-user')
    def test_auto_created_downtime_empty(self):
        request = report_pb2.GetReportRequest(
            component_name='test_component',
            parent_component_name='parent_component',
            period=report_pb2.ReportPeriod.month,
        )

        expected_statistic = report_pb2.StatisticStatus(
            value=100.0,
            absolute_value=0,
            total_value=0,
            target=0,
            status=True,
        )

        report = WARDEN_CLIENT.get_report(request=request, context=None)

        self.assertEqual(report.statistic.auto_calculated_downtime, expected_statistic)

    def _get_private_metric_stats(self):
        request = report_pb2.GetReportRequest(
            component_name='test-metrics',
            parent_component_name='',
            period=report_pb2.ReportPeriod.month,
        )

        report = WARDEN_CLIENT.get_report(request=request, context=None)

        return {
            stat.metric.key: stat.value
            for stat in report.metric_stats
        }

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_private_additive_metrics_admin(self):
        values = self._get_private_metric_stats()
        expected_values = {
            key: setup.METRIC_VALUES[key] * 2
            for key in (setup.PRIVATE_METRIC_KEY, setup.ALLOWED_METRIC_KEY)
        }
        self.assertEqual(values, expected_values)

    @TestCase.mock_auth(login='test-user')
    def test_private_additive_metrics_authorized(self):
        values = self._get_private_metric_stats()
        expected_values = {
            key: setup.METRIC_VALUES[key] * 2
            for key in (setup.ALLOWED_METRIC_KEY, )
        }
        self.assertEqual(values, expected_values)

    @TestCase.mock_auth(login='not-authorized-user')
    def test_private_additive_metrics_unauthorized(self):
        values = self._get_private_metric_stats()
        expected_values = {}
        self.assertEqual(values, expected_values)
