import os

import yatest.common

from crypta.utils.rtmr_resource_service.lib.file_client import FileClient


def get_dirs(root):
    return {
        os.path.join(resource_name)
        for resource_name in os.listdir(root)
    }


def touch(filename):
    open(filename, "w").close()


def test_file_client():
    root = yatest.common.test_output_path("test")
    client = FileClient(root)

    resource1 = "resource1"
    resource2 = "resource2"
    resource3 = "resource3"

    client.init([resource2, resource3])
    assert {resource2, resource3} == get_dirs(root)

    client.init([resource1, resource2])
    assert {resource1, resource2} == get_dirs(root)

    id_ = 123

    assert not client.is_present(resource1, id_)
    filename = client.get_resource_filename(resource1, id_)
    touch(filename)
    assert client.is_present(resource1, id_)

    touch(client.get_resource_filename(resource2, id_))
    assert client.is_present(resource2, id_)

    client.remove_extra({(resource1, id_)})
    assert client.is_present(resource1, id_)
    assert not client.is_present(resource2, id_)
