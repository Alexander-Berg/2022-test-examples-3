from sqlalchemy.orm import joinedload

from search.martylib.db_utils import session_scope, to_model

from search.mon.warden.proto.structures import component_pb2, functionality_pb2, component_check_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.sqla.warden.model import Component
from search.mon.warden.src import const
from search.mon.warden.src.services.model import Warden
from search.mon.warden.src.services.reducers.validators import functionality as v_functionality
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_alerts, create_components, create_functionalities

WARDEN_CLIENT = Warden()


class TestWardenRunCheckOfAllComponents(BaseTestCase):
    maxDiff = None

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='test_component_3',
                    abc_service_slug='test_abc_service_3',
                    tier='D',
                ),
            ),
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='test_component_4',
                    abc_service_slug='test_abc_service_4',
                    tier='D',
                )
            ),
        )

        functionality_ids = create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                component_name='test_component_3',
                functionality=functionality_pb2.Functionality(name='test-functionality-3', slug='test_component_3_test-functionality-3'),
            )
        )

        create_alerts(alert_message_pb2.AddAlertRequest(
            functionality_id=functionality_ids[0],
            alert=alert_pb2.Alert(name='test-alert', beholder_settings=alert_pb2.BeholderAlertSettings(create_spi=True), state=alert_pb2.Alert.State.VALID),
        ))

    def test_run_check_of_all_component_functionalities(self):
        with session_scope() as session:
            # Create all checks
            v_functionality.Functionality().run_check_of_all_components(session)
            component_model_4 = (
                session.query(Component)
                .filter(
                    Component.name == 'test_component_4'
                )
                .options(joinedload(Component.component_checks), joinedload(Component.functionality_list))
                .one_or_none()
            )

            component = component_model_4.to_protobuf()

            self.assertEqual(len(component.component_checks), 8)
            autocreate_check = component.component_checks[0]
            for check in component.component_checks:
                if check.name == const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT:
                    autocreate_check = check
                    break

            self.assertEqual(autocreate_check.name, const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT)
            self.assertEqual(autocreate_check.check_type, component_check_pb2.ComponentCheck.CheckType.FUNCTIONALITIES)
            self.assertEqual(autocreate_check.status, component_check_pb2.ComponentCheck.Status.ERROR)
            self.assertEqual(autocreate_check.description, const.CHECK_DESCRIPTION_MAP[const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT])
            self.assertEqual(autocreate_check.priority, component_check_pb2.ComponentCheck.Priority.CRITICAL)
            self.assertEqual(len(autocreate_check.causes.functional_ids), 0)
            self.assertEqual(len(autocreate_check.causes.alert_ids), 0)

            component_model_3 = (
                session.query(Component)
                .filter(
                    Component.name == 'test_component_3'
                )
                .options(joinedload(Component.component_checks), joinedload(Component.functionality_list))
                .one_or_none()
            )

            component = component_model_3.to_protobuf()

            autocreate_check = component.component_checks[0]
            for check in component.component_checks:
                if check.name == const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT:
                    autocreate_check = check
                    break

            self.assertEqual(len(component.component_checks), 8)
            self.assertEqual(autocreate_check.name, const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT)
            self.assertEqual(autocreate_check.check_type, component_check_pb2.ComponentCheck.CheckType.FUNCTIONALITIES)
            self.assertEqual(autocreate_check.status, component_check_pb2.ComponentCheck.Status.OK)
            self.assertEqual(autocreate_check.description, const.CHECK_DESCRIPTION_MAP[const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT])
            self.assertEqual(autocreate_check.priority, component_check_pb2.ComponentCheck.Priority.CRITICAL)
            self.assertEqual(len(autocreate_check.causes.functional_ids), 1)
            self.assertEqual(len(autocreate_check.causes.alert_ids), 1)

            # Update checks
            with session_scope() as session:
                functionality_model = to_model(functionality_pb2.Functionality(name='test-functionality-4', slug='test-functionality-4'))
                functionality_model.component = component_model_4
                session.add(functionality_model)
                create_alerts(alert_message_pb2.AddAlertRequest(
                    functionality_id=str(functionality_model.id),
                    alert=alert_pb2.Alert(name='test-alert-2', beholder_settings=alert_pb2.BeholderAlertSettings(create_spi=True), state=alert_pb2.Alert.State.VALID),
                ))

                v_functionality.Functionality().run_check_of_all_components(session, component_pb2.Component.State.INVALID)

            with session_scope() as session:
                component_model = (
                    session.query(Component)
                    .filter(
                        Component.name == 'test_component_4'
                    )
                    .options(joinedload(Component.component_checks), joinedload(Component.functionality_list))
                    .one_or_none()
                )

                component = component_model.to_protobuf()

                self.assertEqual(len(component.component_checks), 8)

                autocreate_check = component.component_checks[0]
                for check in component.component_checks:
                    if check.name == const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT:
                        autocreate_check = check
                        break

                self.assertEqual(autocreate_check.name, const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT)
                self.assertEqual(autocreate_check.check_type, component_check_pb2.ComponentCheck.CheckType.FUNCTIONALITIES)
                self.assertEqual(autocreate_check.status, component_check_pb2.ComponentCheck.Status.OK)
                self.assertEqual(autocreate_check.description, const.CHECK_DESCRIPTION_MAP[const.HAS_FUNCTIONALITY_WITH_AUTOCREATE_ALERT])
                self.assertEqual(autocreate_check.priority, component_check_pb2.ComponentCheck.Priority.CRITICAL)
                self.assertEqual(len(autocreate_check.causes.functional_ids), 1)
                self.assertEqual(len(autocreate_check.causes.alert_ids), 1)
