from search.martylib.db_utils import session_scope
from search.martylib.http.exceptions import BadRequest
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, functionality_pb2
from search.mon.warden.proto.structures.alert import alert_pb2, message_pb2 as alert_message_pb2
from search.mon.warden.sqla.warden.model import Alert
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_components, create_alerts, create_functionalities


class TestWardenMoveAlert(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='move_alert_test_1',
                    weight=0.1,
                    parent_component_name='',
                    abc_service_slug='hound',
                    functionality_list=[]
                )
            ),
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='move_alert_test_2',
                    weight=0.5,
                    parent_component_name='',
                    abc_service_slug='xiva',
                    functionality_list=[]
                )
            )
        )
        create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    id='38277945-c399-4e83-8de5-7622fa4d194c',
                    name='test',
                    description='test funct',
                    weight=0.2,
                    slug='move_alert_test_1_slug'
                ),
                component_name='move_alert_test_1'
            ),
            functionality_pb2.AddFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    id='38277945-c399-4e83-8de5-7622fa4d194d',
                    name='test_2',
                    description='test funct 2',
                    weight=0.2,
                ),
                component_name='move_alert_test_2'
            )
        )
        create_alerts(
            alert_message_pb2.AddAlertRequest(
                alert=alert_pb2.Alert(
                    id='38277945-c399-4e83-8de5-7622fa4d194c',
                    name='test',
                    type=alert_pb2.Alert.Type.YASM,
                    url='http://ya.ru',
                    beholder_settings=alert_pb2.BeholderAlertSettings(
                        create_spi=True
                    ),
                    category=alert_pb2.Alert.Category.ERROR_PERC,
                ),
                functionality_id='38277945-c399-4e83-8de5-7622fa4d194c'
            )
        )

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_move_alert(self):
        alert_id = '38277945-c399-4e83-8de5-7622fa4d194c'
        functionality_id = '38277945-c399-4e83-8de5-7622fa4d194d'
        request_1 = alert_message_pb2.MoveAlertRequest(
            alert_id=alert_id,
            functionality_id=functionality_id
        )
        Warden().move_alert(request_1, context=None)
        with session_scope() as session:
            alert = session.query(Alert).filter(Alert.id == alert_id).one_or_none()
            if not str(alert.functionality.id) == functionality_id:
                raise BadRequest(
                    f'Functionality does not changed: got "{alert.functionality.id}" '
                    f'of type "{type(alert.functionality.id)}", '
                    f''
                    f'expected "{functionality_id}" of type "{type(functionality_id)}"'
                )

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_move_alert_to_nonexistent_funct(self):
        alert_id = '38277945-c399-4e83-8de5-7622fa4d194c'
        functionality_id = '38277945-c399-4e83-8de5-7622fa4d194e'
        request_1 = alert_message_pb2.MoveAlertRequest(
            alert_id=alert_id,
            functionality_id=functionality_id
        )
        try:
            Warden().move_alert(request_1, context=None)
        except BadRequest:
            pass
        else:
            raise BadRequest('Alert moved to unexistent functionality')
