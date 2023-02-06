from search.morty.src.model.process.flow_parser import FlowParser
from search.morty.src.model.process import utils
from search.morty.proto.structures import common_pb2, component_pb2

from search.morty.tests.utils.test_case import MortyTestCase


class TestGenerator(MortyTestCase):
    def test_patch_process(self):
        component = component_pb2.Component(
            notifications=common_pb2.NotificationConfig(
                notify_marty=True,
            )
        )
        process = utils.get_default_process()
        FlowParser.patch_process(process, component)
        assert process.params.notifications.notify_marty is True

        component.notifications.notify_marty = False
        FlowParser.patch_process(process, component)
        assert process.params.notifications.notify_marty is False
