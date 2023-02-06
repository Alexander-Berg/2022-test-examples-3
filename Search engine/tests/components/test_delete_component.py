from search.martylib.db_utils import session_scope
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, functionality_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.proto.structures.component import common_pb2
from search.mon.warden.src.services.model import Warden
from search.mon.warden.sqla.warden.model import Component, Alert
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_components, create_functionalities, create_alerts

WARDEN_CLIENT = Warden()


class TestWardenDeleteComponents(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(component_pb2.CreateComponentRequest(
            component=component_pb2.Component(slug='test_delete_component', abc_service_slug='test_delete_component'))
        )

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_delete_component(self):
        functionality_list = create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                component_name='test_delete_component',
                functionality=functionality_pb2.Functionality(name='test-func-1', weight=1)
            ),
        )
        alert_list = create_alerts(
            alert_message_pb2.AddAlertRequest(
                functionality_id=functionality_list[0],
                alert=alert_pb2.Alert(name='test-alert-1', url='https://yasm.yandex-team.ru/alert/test-alert')
            )
        )

        response = WARDEN_CLIENT.delete_component(common_pb2.ComponentFilter(component_slug='test_delete_component'), context=None)
        self.assertEqual('', response.error)
        with session_scope() as session:
            component = session.query(Component).filter(Component.name == 'test_delete_component').one()
            self.assertEqual(component.state, component_pb2.Component.State[component_pb2.Component.State.DELETED])

            alert = session.query(Alert).filter(Alert.id == alert_list[0]).one()
            self.assertEqual(alert.state, alert_pb2.Alert.State[alert_pb2.Alert.State.DELETED])
