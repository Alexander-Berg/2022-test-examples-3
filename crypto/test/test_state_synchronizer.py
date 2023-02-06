import os

from crypta.utils.rtmr_resource_service.bin.state_synchronizer.lib.state_synchronizer import StateSynchronizer
from crypta.utils.rtmr_resource_service.lib.mock_db_client import MockDbClient
from crypta.utils.rtmr_resource_service.lib.resource import Resource


class MockFileClient(object):
    def __init__(self, state):
        self.state = state

    def init(self, resources):
        pass

    def get_resource_filename(self, name, version):
        return os.path.join(name, str(version))

    def is_present(self, name, id_):
        return (name, id_) in self.state

    def remove_extra(self, all_ids):
        self.kept = all_ids


class MockSandboxClient(object):
    def __init__(self):
        self.loaded = {}

    def load_resource(self, resource_id, target_path, resource_path=None, bundle_file=None):
        self.loaded[resource_id] = target_path


def test_upload():
    resources = {
        name: Resource(name, name)
        for name in ["up_to_date", "new", "to_remove"]
    }

    instance_resources = {
        ("up_to_date", 20),
        ("to_remove", 30),
    }

    resources_to_download = {
        ("up_to_date", 20),
        ("new", 10),
    }

    instance = "instance"

    sandbox_client = MockSandboxClient()
    file_client = MockFileClient(instance_resources)

    db_client = MockDbClient()
    db_client.set_instance_resources(instance, instance_resources)
    db_client.set_resources_to_download(resources_to_download)

    synchronizer = StateSynchronizer(instance, resources, file_client, db_client, sandbox_client)
    synchronizer.sync()

    return {
        "db_instances": db_client.instances.keys(),
        "db_instance_resources": {key: sorted(value) for key, value in db_client.instance_resources.iteritems()},
        "loaded": sandbox_client.loaded,
        "ids_kept": sorted(file_client.kept),
    }
