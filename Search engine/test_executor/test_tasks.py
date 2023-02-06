from search.morty.proto.structures import abstract_pb2, resource_pb2, executor_pb2
from search.morty.src.executor.tasks import ControlAlemateNode

from search.morty.tests.utils.test_case import MortyTestCase


class TestControlAlemateTask(MortyTestCase):
    def test_start_notification(self):
        proto = executor_pb2.ExecutionTask()
        task = ControlAlemateNode(proto)
        resources = resource_pb2.ResourceList(objects=[resource_pb2.Resource(locations=[abstract_pb2.Location.SAS, abstract_pb2.Location.VLA])])
        assert task.start_notification(resources) is None

        proto.params.control_alemate.confirm_alemate_task = 'task'
        assert task.start_notification(resources) == 'deploy to SAS, VLA starts'
