from google.protobuf.json_format import MessageToDict

from load.projects.cloud.loadtesting.server.obfuscator.base import Obfuscator
from load.projects.cloud.loadtesting.server.obfuscator.tank_job import _tank_job_data
from yandex.cloud.priv.loadtesting.agent.v1 import test_pb2
from yandex.cloud.priv.loadtesting.agent.v1 import test_service_pb2


class Test(Obfuscator):
    target_class = test_pb2.Test

    def data(self) -> dict:
        response: test_pb2.Test = self.original

        return _tank_job_data(response)


def _autostop_data(autostop: test_pb2.Autostop):
    return {
        'autostop_type': test_pb2.Autostop.AutostopType.Name(autostop.autostop_type),
        'autostop_criteria': autostop.autostop_criteria,
        'autostop_case': autostop.autostop_case,
    }


class CreateTestRequest(Obfuscator):
    target_class = test_service_pb2.CreateTestRequest

    def data(self) -> dict:
        request: test_service_pb2.CreateTestRequest = self.original
        return {
            'folder_id': request.folder_id,
            'name': request.name,
            'description': request.description,
            'labels': dict(request.labels),
            'generator': test_pb2.Test.Generator.Name(request.generator),
            'tank_instance_id': request.tank_instance_id,
            'target_address': request.target_address,
            'target_port': request.target_port,
            'target_version': request.target_version,
            'instances': request.instances,
            'load_schedule': MessageToDict(request.load_schedule),
            'ammo_id': request.ammo_id,
            'ammo_urls': list(request.ammo_urls),
            'ammo_type': test_pb2.AmmoType.Name(request.ammo_type),
            'ssl': request.ssl,
            'logging_log_group_id': request.logging_log_group_id,
            'imbalance_point': request.imbalance_point,
            'imbalance_ts': request.imbalance_ts,
            'autostops': [_autostop_data(i) for i in request.autostops],
        }


class UpdateTestRequest(Obfuscator):
    target_class = test_service_pb2.UpdateTestRequest

    def data(self) -> dict:
        request: test_service_pb2.UpdateTankJobRequest = self.original

        return {
            'test_id': request.test_id,
            'update_mask': MessageToDict(request.update_mask),
            'name': request.name,
            'description': request.description,
            'labels': dict(request.labels),
            'favourite': request.favorite,
            'target_version': request.target_version,
            'imbalance_point': request.imbalance_point,
            'imbalance_ts': request.imbalance_ts,
            'imbalance_comment': request.imbalance_comment,
        }
