from search.martylib.db_utils import session_scope, to_model, generate_field_name as F

from search.mon.warden.proto.structures import component_pb2, functionality_pb2, component_check_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.src.services.model import Warden
from search.mon.warden.sqla.warden.model import Component
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_alerts


WARDEN_CLIENT = Warden()


class TestWardenListComponents(BaseTestCase):

    @staticmethod
    def load_to_db():
        with session_scope() as session:
            component_model = to_model(
                component_pb2.Component(
                    name='test_component',
                    abc_service_slug='test_abc_service',
                    slug='test_component',
                ),
                exclude=(F(Component.value_stream_id),),
            )
            session.add(component_model)
            functionality_model = to_model(functionality_pb2.Functionality(name='test-functionality'))
            functionality_model.component = component_model
            session.add(functionality_model)
            create_alerts(alert_message_pb2.AddAlertRequest(
                functionality_id=str(functionality_model.id),
                alert=alert_pb2.Alert(name='test-alert'),
            ))
            component_check = to_model(component_check_pb2.ComponentCheck(name='test-check'))
            component_check.component = component_model
            session.add(component_check)

    def test_get_component_list(self):

        component_list = WARDEN_CLIENT.get_component_list(component_pb2.GetComponentListRequest(), context=None)
        self.assertEqual(len(component_list.components), 1)

        for component in component_list.components:
            self.assertEqual(component.name, 'test_component')
            self.assertFalse(component.functionality_list)

    def test_get_component_list_with_functionality(self):

        component_list = WARDEN_CLIENT.get_component_list(component_pb2.GetComponentListRequest(with_functionalities=True), context=None)
        self.assertEqual(len(component_list.components), 1)

        for component in component_list.components:
            self.assertEqual(component.name, 'test_component')
            self.assertEqual(len(component.functionality_list), 1)

            for functionality in component.functionality_list:
                self.assertEqual(functionality.name, 'test-functionality')
                self.assertTrue(functionality.id)
                self.assertEqual(len(functionality.alerts), 1)
                for alert in functionality.alerts:
                    self.assertEqual(alert.name, 'test-alert')

    def test_get_component_list_with_checks(self):

        component_list = WARDEN_CLIENT.get_component_list(component_pb2.GetComponentListRequest(with_checks=True), context=None)
        self.assertEqual(len(component_list.components), 1)

        for component in component_list.components:
            self.assertEqual(component.name, 'test_component')
            self.assertEqual(len(component.component_checks), 1)

            for check in component.component_checks:
                self.assertEqual(check.name, 'test-check')
                self.assertTrue(check.id)
