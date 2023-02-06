from search.martylib.test_utils import TestCase
from search.martylib.db_utils import session_scope
from search.mon.warden.proto.structures import functionality_pb2
from search.mon.warden.src.services.model import Warden

from search.mon.warden.proto.structures import component_pb2, owner_pb2
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_components, create_functionalities
from search.mon.warden.src.workers.alert_generator import AlertGenerator

WARDEN_CLIENT = Warden()


class TestAlertGenerator(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='test_alert_generator',
                    weight=0.1,
                    abc_service_slug='test_alert_generator',
                    owner_list=[owner_pb2.Owner(login='test-user')],
                )
            )
        )

        create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    name='test',
                    description='test functionality',
                    weight=0.2,
                    slug='test_alert_generator__functionality'
                ),
                component_name='test_alert_generator'
            ),
        )

    @TestCase.mock_auth(login='test-user')
    def test_patch_alert(self):
        alert_generator = AlertGenerator()
        with session_scope() as session:
            alert = {
                'name': 'test',
                'juggler_check': {
                    'tags': []
                }
            }
            alert_generator._patch_alert(session, alert, 'test_alert_generator', 'test_alert_generator__functionality', 'test_namespace')
            self.assertEqual(alert['juggler_check']['notifications'][0]['template_kwargs']['login'], ['@svc_test_alert_generator'])
