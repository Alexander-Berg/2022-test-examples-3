from search.martylib.db_utils import session_scope, to_model, generate_field_name as F

from search.mon.warden.proto.structures import component_pb2, component_check_pb2
from search.mon.warden.src.services.model import Warden
from search.mon.warden.sqla.warden.model import Component
from search.mon.warden.tests.utils.base_test_case import BaseTestCase

WARDEN_CLIENT = Warden()


class TestWardenGetComponent(BaseTestCase):

    @staticmethod
    def load_to_db():
        with session_scope() as session:
            component_model = to_model(
                component_pb2.Component(name='test_component_2', abc_service_slug='test_abc_service', slug='test_component_2'),
                exclude=(F(Component.value_stream_id), ),
            )
            session.add(component_model)
            component_check = to_model(component_check_pb2.ComponentCheck(name='test-check-2'))
            component_check.component = component_model
            session.add(component_check)

    def test_get_component_with_checks(self):

        component_resp = WARDEN_CLIENT.get_component(component_pb2.GetComponentRequest(name='test_component_2'), context=None)
        self.assertEqual(len(component_resp.component.component_checks), 1)
        self.assertEqual(component_resp.component.component_checks[0].name, 'test-check-2')
        self.assertTrue(component_resp.component.component_checks[0].id)
