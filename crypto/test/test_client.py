import os
import six.moves.urllib.parse as urlparse

import mock
import pytest
import yatest.common

import crypta.lib.python.sandbox.client as sandbox_client

REGISTRY = {
    "STABLE_WITH_UPDATE_IN_TESTING": {
        "stable": 1,
        "testing": 2
    },
    "TESTING_ONLY": {
        "testing": 3
    },
    "STABLE_ONLY": {
        "stable": 4
    },
    "LATEST_IN_STABLE": {
        "stable": 6,
        "testing": 5
    }
}


def mock_rest_client():
    class MockSandbox(object):
        class MockResource(object):
            def __init__(self, client):
                self.client = client

            def read(self, **kwargs):
                self.client.requests.append(kwargs)
                version = REGISTRY[kwargs["type"]].get(kwargs["attrs"]["released"])
                return {"items": [] if version is None else [{"id": version}]}

        def __init__(self, **_):
            self.resource = self.MockResource(self)
            self.requests = []

    return mock.patch('crypta.lib.python.sandbox.client.rest.Client', side_effect=MockSandbox)


def mock_file_response(file_path, url_path):
    def get(url, *args, **kwargs):
        assert url_path == urlparse.urlparse(url).path
        return MockResponse()

    class MockResponse(object):
        def raise_for_status(self):
            pass

        def __iter__(self):
            with open(file_path, "rb") as f:
                while True:
                    chunk = f.read(5)
                    if not chunk:
                        return
                    yield chunk

    return mock.patch('crypta.lib.python.sandbox.client.requests.get', get)


@pytest.mark.parametrize("resource_name", REGISTRY.keys())
@pytest.mark.parametrize("release_type", ["stable", "testing"])
def test_get_last_released_resource_id(resource_name, release_type):
    with mock_rest_client():
        client = sandbox_client.SandboxClient()
        return {
            "requests": client.sandbox.requests,
            "version": client.get_last_released_resource_id(resource_name, release_type)
        }


@pytest.mark.parametrize("test_file,bundle_file,resource_path,url_path", [
    ("plain_resource.txt", None, None, "/1"),
    ("archive_resource.tar.gz", "plain_resource.txt", "archive_resource.tar.gz", "/1/archive_resource.tar.gz")
])
def test_load_sandbox(test_file, bundle_file, resource_path, url_path):
    with mock_file_response(os.path.join(yatest.common.test_source_path("data"), test_file), url_path):
        client = sandbox_client.SandboxClient()
        file_path = yatest.common.test_output_path("resource")
        client.load_resource(1, file_path, resource_path=resource_path, bundle_file=bundle_file)
        return yatest.common.canonical_file(file_path, local=True)
