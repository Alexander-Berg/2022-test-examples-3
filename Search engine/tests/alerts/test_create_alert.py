import uuid

from search.martylib.db_utils import session_scope, to_model, generate_field_name as F
from search.martylib.http.exceptions import BadRequest, NotAuthorized
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, functionality_pb2, metric_pb2, owner_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.sqla.warden.model import Alert, Component
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils import setup
from search.mon.warden.tests.utils.base_test_case import BaseTestCase

WARDEN_CLIENT = Warden()

functionality_1_uuid = uuid.uuid4().__str__()
functionality_2_uuid = uuid.uuid4().__str__()
functionality_3_uuid = uuid.uuid4().__str__()
functionality_4_uuid = uuid.uuid4().__str__()


class TestWardenCreateAlert(BaseTestCase):
    maxDiff = None

    @staticmethod
    def load_to_db():
        with session_scope() as session:
            component_model_with_disabled_alerts_creations = to_model(
                component_pb2.Component(
                    name='test_component_with_disabled_alert_manual_creation',
                    abc_service_slug='create_alert_test_abc_service_1',
                    deny_alerts_manual_creation=True,
                    owner_list=[owner_pb2.Owner(login='test-user')],
                    slug='test_component_with_disabled_alert_manual_creation',
                ),
                exclude=(F(Component.value_stream_id),),
            )
            session.add(component_model_with_disabled_alerts_creations)
            functionality_model = to_model(functionality_pb2.Functionality(slug='create_alert_test_test-functionality-1', name='create_alert_test_test-functionality-1', id=functionality_1_uuid))
            functionality_model.component = component_model_with_disabled_alerts_creations
            session.add(functionality_model)

            component_model_with_enabled_alerts_creation = to_model(
                component_pb2.Component(
                    name='test_component_with_enabled_alert_manual_creation',
                    abc_service_slug='create_alert_test_abc_service_2',
                    deny_alerts_manual_creation=False,
                    owner_list=[owner_pb2.Owner(login='test-user')],
                    slug='test_component_with_enabled_alert_manual_creation',
                ),
                exclude=(F(Component.value_stream_id),),
            )
            session.add(component_model_with_enabled_alerts_creation)
            functionality_model = to_model(functionality_pb2.Functionality(slug='create_alert_test_test-functionality-2', name='create_alert_test_test-functionality-2', id=functionality_2_uuid))
            functionality_model.component = component_model_with_enabled_alerts_creation
            session.add(functionality_model)

            component_model_with_enabled_alerts_creation_in_parent = to_model(
                component_pb2.Component(
                    name='test_component_with_enabled_alert_manual_creation_and_enabled_in_parent',
                    abc_service_slug='create_alert_test_abc_service_3',
                    deny_alerts_manual_creation=False,
                    parent_component_name='test_component_with_enabled_alert_manual_creation',
                    owner_list=[owner_pb2.Owner(login='test-user')],
                    slug='test_component_with_enabled_alert_manual_creation__test_component_with_enabled_alert_manual_creation_and_enabled_in_parent',
                ),
                exclude=(F(Component.value_stream_id),),
            )
            session.add(component_model_with_enabled_alerts_creation_in_parent)
            functionality_model = to_model(functionality_pb2.Functionality(slug='create_alert_test_test-functionality-3', name='create_alert_test_test-functionality-3', id=functionality_3_uuid))
            functionality_model.component = component_model_with_enabled_alerts_creation_in_parent
            session.add(functionality_model)

            component_model_with_disabled_alerts_creation_in_parent = to_model(
                component_pb2.Component(
                    name='test_component_with_enabled_alert_manual_creation_and_disabled_in_parent',
                    abc_service_slug='create_alert_test_abc_service_4',
                    deny_alerts_manual_creation=True,
                    parent_component_name='test_component_with_disabled_alert_manual_creation',
                    owner_list=[owner_pb2.Owner(login='test-user')],
                    slug='test_component_with_disabled_alert_manual_creation__test_component_with_enabled_alert_manual_creation_and_disabled_in_parent',
                ),
                exclude=(F(Component.value_stream_id),),
            )
            session.add(component_model_with_disabled_alerts_creation_in_parent)
            functionality_model = to_model(functionality_pb2.Functionality(slug='create_alert_test_test-functionality-4', name='create_alert_test_test-functionality-4', id=functionality_4_uuid))
            functionality_model.component = component_model_with_disabled_alerts_creation_in_parent
            session.add(functionality_model)

    @TestCase.mock_auth(login='test-user')
    def test_create_alert(self):
        unknown_functionality_id = uuid.uuid4().__str__()
        alert_id_1 = uuid.uuid4().__str__()
        alert_id_2 = uuid.uuid4().__str__()
        alert_id_3 = uuid.uuid4().__str__()
        alert_id_4 = uuid.uuid4().__str__()
        alert_id_5 = uuid.uuid4().__str__()
        alert_id_6 = uuid.uuid4().__str__()
        alert_id_7 = uuid.uuid4().__str__()

        test_cases = [
            {
                'name': 'Functionality doesnt exist',
                'request': alert_message_pb2.AddAlertRequest(
                    functionality_id=unknown_functionality_id,
                    alert=alert_pb2.Alert(
                        id=alert_id_1
                    ),
                ),
                'expected_error': f'Functionality with id {unknown_functionality_id} does not exist',
                'expected_response': alert_message_pb2.AddAlertResponse(),
                'expected_alert': None,
            },
            {
                'name': 'Bad solomon alert configuration',
                'request': alert_message_pb2.AddAlertRequest(
                    functionality_id=unknown_functionality_id,
                    alert=alert_pb2.Alert(
                        id=alert_id_1,
                        url='solomon.yandex-team.ru/admin/projects/test',
                        beholder_settings=alert_pb2.BeholderAlertSettings(
                            use_weight_function=True,
                            calculate_background_metric=True,
                        )
                    ),
                ),
                'expected_error': 'Crit threshold for solomon alerts with weight function usage could not be zero',
                'expected_response': alert_message_pb2.AddAlertResponse(),
                'expected_alert': None,
            },
            {
                'name': 'Component with disabled alerts manual creation',
                'request': alert_message_pb2.AddAlertRequest(
                    functionality_id=functionality_1_uuid,
                    alert=alert_pb2.Alert(
                        id=alert_id_2
                    ),
                ),
                'expected_error': '',
                'expected_response': alert_message_pb2.AddAlertResponse(
                    error='Component with name test_component_with_disabled_alert_manual_creation has disabled manual alerts creation'
                ),
                'expected_alert': None,
            },
            {
                'name': 'Component with enabled alerts manual creation',
                'request': alert_message_pb2.AddAlertRequest(
                    functionality_id=functionality_2_uuid,
                    alert=alert_pb2.Alert(
                        id=alert_id_3
                    ),
                ),
                'expected_error': '',
                'expected_response': alert_message_pb2.AddAlertResponse(alert_id=alert_id_3),
                'expected_alert': alert_pb2.Alert(id=alert_id_3),
            },
            {
                'name': 'Component with enabled alerts manual creation and enabled in parent',
                'request': alert_message_pb2.AddAlertRequest(
                    functionality_id=functionality_3_uuid,
                    alert=alert_pb2.Alert(
                        id=alert_id_4
                    ),
                ),
                'expected_error': '',
                'expected_response': alert_message_pb2.AddAlertResponse(alert_id=alert_id_4),
                'expected_alert': alert_pb2.Alert(id=alert_id_4),
            },
            {
                'name': 'Component with enabled alerts manual creation and disabled in parent',
                'request': alert_message_pb2.AddAlertRequest(
                    functionality_id=functionality_4_uuid,
                    alert=alert_pb2.Alert(
                        id=alert_id_5
                    ),
                ),
                'expected_response': alert_message_pb2.AddAlertResponse(
                    error='Component with name test_component_with_enabled_alert_manual_creation_and_disabled_in_parent has disabled manual alerts creation',
                ),
                'expected_error': '',
                'expected_alert': None,
            },
            {
                'name': 'User not authorized for private metric',
                'request': alert_message_pb2.AddAlertRequest(
                    functionality_id=functionality_2_uuid,
                    alert=alert_pb2.Alert(
                        id=alert_id_6,
                        metric=metric_pb2.Metric(
                            type=setup.PRIVATE_METRIC_TYPE,
                            key=setup.PRIVATE_METRIC_KEY,
                        )
                    ),
                ),
                'expected_exception': NotAuthorized,
            },
            {
                'name': 'Authorized user for private metric',
                'request': alert_message_pb2.AddAlertRequest(
                    functionality_id=functionality_2_uuid,
                    alert=alert_pb2.Alert(
                        id=alert_id_7,
                        metric=metric_pb2.Metric(
                            type=setup.PRIVATE_METRIC_TYPE,
                            key=setup.ALLOWED_METRIC_KEY,
                        )
                    ),
                ),
                'expected_response': alert_message_pb2.AddAlertResponse(alert_id=alert_id_7),
                'expected_error': '',
                'expected_alert': alert_pb2.Alert(id=alert_id_7),
            },
        ]
        self.run_test_cases(test_cases)

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_create_alert_admin(self):
        alert_id_1 = uuid.uuid4().__str__()
        test_cases = [
            {
                'name': 'Admin user for private metric',
                'request': alert_message_pb2.AddAlertRequest(
                    functionality_id=functionality_2_uuid,
                    alert=alert_pb2.Alert(
                        id=alert_id_1,
                        metric=metric_pb2.Metric(
                            type=setup.PRIVATE_METRIC_TYPE,
                            key=setup.PRIVATE_METRIC_KEY,
                        )
                    ),
                ),
                'expected_response': alert_message_pb2.AddAlertResponse(alert_id=alert_id_1),
                'expected_alert': alert_pb2.Alert(id=alert_id_1),
                'expected_error': '',
            },
        ]
        self.run_test_cases(test_cases)

    def run_test_cases(self, test_cases):
        for test_case in test_cases:
            if test_case.get('expected_exception'):
                self.assertRaises(
                    test_case['expected_exception'],
                    WARDEN_CLIENT.add_alert,
                    request=test_case['request'],
                    context='',
                )
            elif test_case['expected_error']:
                try:
                    WARDEN_CLIENT.add_alert(test_case['request'], '')
                except BadRequest as e:
                    self.assertTrue(test_case['expected_error'] in e.__str__())

                    with session_scope() as session:
                        alert = session.query(Alert).filter(Alert.id == test_case['request'].alert.id).one_or_none()
                        self.assertIsNone(alert)

            else:
                resp = WARDEN_CLIENT.add_alert(test_case['request'], ' ')
                self.assertEqual(resp, test_case['expected_response'])
                with session_scope() as session:
                    alert = session.query(Alert).filter(Alert.id == test_case['request'].alert.id).one_or_none()
                    if test_case['expected_alert']:
                        self.assertEqual(alert.id.__str__(), test_case['expected_alert'].id)
                    else:
                        self.assertIsNone(alert)
