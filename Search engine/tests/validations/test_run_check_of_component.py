from search.martylib.db_utils import session_scope, to_model
from sqlalchemy.orm import joinedload

from search.mon.warden.proto.structures import component_pb2, functionality_pb2, component_check_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.proto.structures.component import common_pb2
from search.mon.warden.sqla.warden.model import Component
from search.mon.warden.src import const
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_alerts, create_components

WARDEN_CLIENT = Warden()


class TestWardenRunCheckOfComponent(BaseTestCase):
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
        )

    def test_run_check_of_component(self):
        # Create checks
        resp = WARDEN_CLIENT.run_check_of_component(common_pb2.ComponentFilter(
            component_slug='test_component_3',
        ), context=None)

        self.assertEqual(len(resp.component_checks), 22)
        # Let's check one record in details
        autocreate_check = resp.component_checks[0]
        for check in resp.component_checks:
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

        with session_scope() as session:
            component_model = (
                session.query(Component)
                .filter(
                    Component.name == 'test_component_3'
                )
                .options(joinedload(Component.component_checks), joinedload(Component.functionality_list))
                .one_or_none()
            )

            component = component_model.to_protobuf()

            self.assertEqual(len(component.component_checks), 22)
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

            # Update checks
            with session_scope() as session:
                component_model = (
                    session.query(Component)
                    .filter(
                        Component.name == 'test_component_3'
                    )
                    .options(joinedload(Component.component_checks), joinedload(Component.functionality_list))
                    .one_or_none()
                )
                functionality_model = to_model(functionality_pb2.Functionality(name='test-functionality-3', slug='test-functionality-3'))
                functionality_model.component = component_model
                session.add(functionality_model)
                create_alerts(alert_message_pb2.AddAlertRequest(
                    functionality_id=str(functionality_model.id),
                    alert=alert_pb2.Alert(name='test-alert', beholder_settings=alert_pb2.BeholderAlertSettings(create_spi=True)),
                ))

            WARDEN_CLIENT.run_check_of_component(common_pb2.ComponentFilter(
                component_slug='test_component_3',
            ), context=None)

            with session_scope() as session:
                component_model = (
                    session.query(Component)
                    .filter(
                        Component.name == 'test_component_3'
                    )
                    .options(joinedload(Component.component_checks), joinedload(Component.functionality_list))
                    .one_or_none()
                )

                component = component_model.to_protobuf()

                self.assertEqual(len(component.component_checks), 22)
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
