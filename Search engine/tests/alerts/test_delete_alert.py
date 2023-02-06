from search.martylib.db_utils import session_scope
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, functionality_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.src.services.model import Warden
from search.mon.warden.sqla.warden.model import Alert
from search.mon.warden.tests.utils.creators import create_components, create_functionalities, create_alerts
from search.mon.warden.tests.utils.base_test_case import BaseTestCase

WARDEN_CLIENT = Warden()


class TestWardenDeleteAlert(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-delete-alerts', abc_service_slug='test-delete-alerts-slug')))

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_delete_component_alerts(self):
        # Create and delete alert
        functionality_ids = create_functionalities(
            functionality_pb2.AddFunctionalityRequest(component_name='test-delete-alerts', functionality=functionality_pb2.Functionality(name='test-func-1', weight=1)),
        )
        alert_ids = create_alerts(
            alert_message_pb2.AddAlertRequest(functionality_id=functionality_ids[0], alert=alert_pb2.Alert(name='test-alert-1', url='https://yasm.yandex-team.ru/alert/test-alert')),
        )

        WARDEN_CLIENT.delete_alert(alert_message_pb2.DeleteAlertRequest(alert_id=alert_ids[0]), context=None)
        with session_scope() as session:
            alert = session.query(Alert).filter(Alert.id == alert_ids[0]).first()
            alert_proto = alert.to_protobuf()
            self.assertEqual(alert_proto.state, alert_pb2.Alert.State.DELETED)
