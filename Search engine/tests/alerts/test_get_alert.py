import uuid

from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, functionality_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_components, create_functionalities, create_alerts

WARDEN_CLIENT = Warden()

functionality_id_1 = uuid.uuid4().__str__()
functionality_id_2 = uuid.uuid4().__str__()

alert_id_1 = uuid.uuid4().__str__()
alert_id_2 = uuid.uuid4().__str__()


class TestWardenGetAlert(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-get-alerts', abc_service_slug='test-get-alerts-slug')),
            component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test-get-alerts__test-service', abc_service_slug='test-service')),
        )
        create_functionalities(
            functionality_pb2.AddFunctionalityRequest(component_name='test-get-alerts', functionality=functionality_pb2.Functionality(id=functionality_id_1, name='test-func-1', weight=1)),
            functionality_pb2.AddFunctionalityRequest(
                parent_component_name='test-get-alerts',
                component_name='test-service',
                functionality=functionality_pb2.Functionality(id=functionality_id_2, name='test-func-2', weight=1)),
        )
        create_alerts(
            alert_message_pb2.AddAlertRequest(functionality_id=functionality_id_1, alert=alert_pb2.Alert(id=alert_id_1, name='test-alert-1', url='https://yasm.yandex-team.ru/alert/test-alert')),
            alert_message_pb2.AddAlertRequest(functionality_id=functionality_id_2, alert=alert_pb2.Alert(id=alert_id_2, name='test-alert-2', url='https://yasm.yandex-team.ru/alert/test-alert-2')),
        )

    @TestCase.mock_auth(login='test-user')
    def test_get_component_alerts(self):
        # test get root component alerts
        response = WARDEN_CLIENT.get_component_alerts(alert_message_pb2.GetComponentAlertsRequest(component_name='test-get-alerts'), context=None)
        self.assertEqual(len(response.objects), 2)

        for obj in response.objects:
            if obj.functionality_id == functionality_id_1:
                self.assertEqual(obj.alert.id, alert_id_1)
                self.assertEqual(obj.alert.url, 'https://yasm.yandex-team.ru/alert/test-alert')
                self.assertEqual(obj.service_name, '')
                self.assertEqual(obj.functionality_name, 'test-func-1')
            else:
                self.assertEqual(obj.functionality_id, functionality_id_2)
                self.assertEqual(obj.alert.id, alert_id_2)
                self.assertEqual(obj.alert.url, 'https://yasm.yandex-team.ru/alert/test-alert-2')
                self.assertEqual(obj.service_name, 'test-service')
                self.assertEqual(obj.functionality_name, 'test-func-2')

        # test get service alerts
        response = WARDEN_CLIENT.get_component_alerts(alert_message_pb2.GetComponentAlertsRequest(component_name='test-service', parent_component_name='test-get-alerts'), context=None)
        self.assertEqual(len(response.objects), 1)

        for obj in response.objects:
            self.assertEqual(obj.alert.id, alert_id_2)
            self.assertEqual(obj.alert.url, 'https://yasm.yandex-team.ru/alert/test-alert-2')
            self.assertEqual(obj.service_name, 'test-service')
            self.assertEqual(obj.functionality_name, 'test-func-2')
