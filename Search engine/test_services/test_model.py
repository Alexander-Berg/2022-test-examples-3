from search.martylib.core.exceptions import ValidationError

from search.morty.proto.structures import component_pb2

from search.morty.src.services.model import validate_unique_flows
from search.morty.tests.utils.test_case import MortyTestCase


class TestSupplierSpotter(MortyTestCase):
    @staticmethod
    def api_method(self, request, context):
        return

    def test_validate_unique_flows(self):
        validate_flow_id = validate_unique_flows(self.api_method)

        # test no flows
        component = component_pb2.Component()
        validate_flow_id(self, component, None)

        # test flow with empty id
        component.flows.objects.extend((component_pb2.Flow(), ))
        try:
            validate_flow_id(self, component, None)
            assert False
        except ValidationError:
            component.flows.objects.pop()
            pass

        # test two different flows
        component.flows.objects.extend((component_pb2.Flow(id='test'),))
        validate_flow_id(self, component, None)

        # test two flow with same id
        component.flows.objects.extend((component_pb2.Flow(id='test'),))
        try:
            validate_flow_id(self, component, None)
            assert False
        except ValidationError:
            pass
