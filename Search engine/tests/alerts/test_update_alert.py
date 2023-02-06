import uuid

from search.martylib.db_utils import session_scope
from search.martylib.http.exceptions import BadRequest, NotAuthorized
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, functionality_pb2, metric_pb2, owner_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.sqla.warden.model import Alert
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils.creators import create_components, create_functionalities, create_alerts
from search.mon.warden.tests.utils import setup
from search.mon.warden.tests.utils.base_test_case import BaseTestCase

WARDEN_CLIENT = Warden()

alert_id_1 = uuid.uuid4().__str__()
alert_id_2 = uuid.uuid4().__str__()


class TestWardenUpdateAlert(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='test-update-alerts',
                    abc_service_slug='test-update-alerts-slug',
                    owner_list=[owner_pb2.Owner(login='test-user')],
                )
            )
        )
        functionality_ids = create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                component_name='test-update-alerts',
                functionality=functionality_pb2.Functionality(name='test', weight=1),
            )
        )
        create_alerts(
            alert_message_pb2.AddAlertRequest(
                functionality_id=functionality_ids[0],
                alert=alert_pb2.Alert(
                    id=alert_id_1,
                    name='test',
                    url='https://yasm.yandex-team.ru/alert/test-alert',
                )
            )
        )

    @TestCase.mock_auth(login='test-user')
    def test_update_alert(self):
        test_cases = [
            {
                'alert': alert_pb2.Alert(id=alert_id_1, name='test-update', url='https://solomon.yandex-team.ru/admin/projects/test'),
                'name': 'simple update',
            },
            {
                'alert': alert_pb2.Alert(
                    id=alert_id_1,
                    name='test-update',
                    url='https://solomon.yandex-team.ru/admin/projects/test',
                    beholder_settings=alert_pb2.BeholderAlertSettings(use_weight_function=True, calculate_background_metric=True),
                ),
                'expected_error': 'Crit threshold for solomon alerts with weight function usage could not be zero',
                'name': 'Bad Solomon alert configuration'
            },
            {
                'alert': alert_pb2.Alert(name='test-alert'),
                'expected_error': 'No alert id in request',
                'name': 'Empty alert id',
            },
            {
                'alert': alert_pb2.Alert(name='test-alert', id=alert_id_2),
                'expected_error': f'Alert with id={alert_id_2} does not exist',
                'name': 'Wrong alert id',
            },
            {
                'alert': alert_pb2.Alert(
                    id=alert_id_1,
                    metric=metric_pb2.Metric(
                        type=setup.PRIVATE_METRIC_TYPE,
                        key=setup.PRIVATE_METRIC_KEY,
                    ),
                ),
                'expected_exception': NotAuthorized,
                'name': 'Unauthorized user metric update',
            },
            {
                'alert': alert_pb2.Alert(
                    id=alert_id_1,
                    metric=metric_pb2.Metric(
                        type=setup.PRIVATE_METRIC_TYPE,
                        key=setup.ALLOWED_METRIC_KEY,
                    ),
                ),
                'name': 'Authorized user metric update',
            },
        ]
        self.run_test_cases(test_cases)

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_update_private_metric_by_admin(self):
        test_cases = [
            {
                'alert': alert_pb2.Alert(
                    id=alert_id_1,
                    metric=metric_pb2.Metric(
                        type=setup.PRIVATE_METRIC_TYPE,
                        key=setup.PRIVATE_METRIC_KEY,
                    ),
                ),
                'name': 'Admin update for private metric',
            },
        ]
        self.run_test_cases(test_cases)

    def run_test_cases(self, test_cases):
        for test_case in test_cases:
            if test_case.get('expected_exception'):
                self.assertRaises(
                    test_case['expected_exception'],
                    WARDEN_CLIENT.update_alert,
                    request=alert_message_pb2.UpdateAlertRequest(alert=test_case['alert']),
                    context=None,
                )
            elif test_case.get('expected_error'):
                try:
                    WARDEN_CLIENT.update_alert(alert_message_pb2.UpdateAlertRequest(alert=test_case['alert']), context=None)
                except BadRequest as ex:
                    self.assertIn(test_case['expected_error'], str(ex))
                else:
                    self.assertFalse(f'No expected exception in test case: {test_case["name"]}')
            else:
                WARDEN_CLIENT.update_alert(alert_message_pb2.UpdateAlertRequest(alert=test_case['alert']), context=None)
                with session_scope() as session:
                    alert = session.query(Alert).filter(Alert.id == alert_id_1).one_or_none()
                    self.assertEqual(alert.name, test_case['alert'].name)
                    self.assertEqual(alert.url, test_case['alert'].url)
                    self.assertEqual(alert.metric.key, test_case['alert'].metric.key)
