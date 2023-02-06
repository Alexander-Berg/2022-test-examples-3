from search.mon.warden.proto.structures import component_pb2
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_components

WARDEN_CLIENT = Warden()


class TestWardenGetChildComponents(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test_get_child_components', abc_service_slug='test_get_child_components')))
        create_components(component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test_get_child_components__test')))

    def test_get_child_component_list(self):
        component_list = WARDEN_CLIENT.get_child_components(component_pb2.GetChildComponentsRequest(parent_component='test_get_child_components'), context=None)
        self.assertEqual(len(component_list.objects), 1)
