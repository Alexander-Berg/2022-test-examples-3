import os
import pytest
import requests
from mock import Mock

from search.wizard.entitysearch.tools.es_hook_notifier.lib import utils, handlers
from search.wizard.entitysearch.tools.es_hook_notifier.lib.consts import (
    FASTBASE_DIR_KEY,
    FRESH_DIR_KEY,
    MAIN_DELTA_DIR_KEY,
    REALTIME_DIR_KEY,
)


from search.wizard.entitysearch.tools.es_hook_notifier.lib.handlers import (
    update_fresh,
    update_realtime,
    update_realtime_and_fresh,
)


def get_mock_with_status(status_code):
    def get_mock(url, timeout):
        result = Mock()
        result.status_code = status_code
        result.url = url
        result.timeout = timeout
        return result

    return get_mock


def get_resp_mock_with_json(json_object):
    result = Mock()

    def return_json():
        return json_object

    result.json.side_effect = return_json

    return result


def get_service_port_mock(dump_file):
    return 1234


FAKE_ENVIRON = {
    FASTBASE_DIR_KEY: "fresh_prepared",
    FRESH_DIR_KEY: "fresh/fresh",
    MAIN_DELTA_DIR_KEY: "main_delta/main_delta",
    REALTIME_DIR_KEY: "realtime",
}


@pytest.fixture
def required_patches_fixture(monkeypatch):
    monkeypatch.setattr(requests, "get", get_mock_with_status(200))
    monkeypatch.setattr(requests, "post", Mock())
    monkeypatch.setattr(utils, "get_service_port", get_service_port_mock)
    monkeypatch.setattr(handlers, "untar_all", Mock())
    monkeypatch.setattr(handlers, "copy_tree", Mock())
    monkeypatch.setattr(os, "environ", FAKE_ENVIRON)


def test_update_fresh(required_patches_fixture):
    result = update_fresh()
    assert result.status_code == 200
    assert result.url == "http://localhost:1234/admin?action=updatees"


def test_update_realtime(required_patches_fixture):
    result = update_realtime()
    assert result.status_code == 200
    assert result.url == "http://localhost:1234/admin?action=updatert"


def test_update_realtime_and_fresh(required_patches_fixture):
    result = update_realtime_and_fresh()
    assert result.status_code == 200
    assert result.url == "http://localhost:1234/admin?action=updatees"


def test_build_versions_diff():
    resp_version_before = get_resp_mock_with_json(
        {
            "HostName": "sas1-7823-sas-entitysearch-8400.gencfg-c.yandex.net",
            "Port": 8400,
            "DataVersion": {
                "Shard": {
                    "Db": [
                        {
                            "Name": "main_delta",
                            "Version": "20200407-173720",
                            "Path": "/place/db/iss3/instances/8400_sas_production_entitysearch_fdZ4ZQ34UOT/fresh_prepared/main_delta.trie",
                        }
                    ],
                    "Fresh": {
                        "GenerationTime": "07.04.2020 14:47",
                        "Revision": 5634558,
                        "Task": 647648223,
                    },
                    "Realtime": {"Revision": 5629769},
                    "Ner": {},
                },
            },
            "ProgramBuildInfo": {},
        }
    )

    resp_version_after = get_resp_mock_with_json(
        {
            "HostName": "sas1-7823-sas-entitysearch-8400.gencfg-c.yandex.net",
            "Port": 8400,
            "DataVersion": {
                "Shard": {
                    "Db": [
                        {
                            "Name": "main_delta",
                            "Version": "20201212-235959",
                            "Path": "/place/db/iss3/instances/8400_sas_production_entitysearch_fdZ4ZQ34UOT/fresh_prepared/main_delta.trie",
                        }
                    ],
                    "Fresh": {
                        "GenerationTime": "12.12.2020 23:23",
                        "Revision": 6000000,
                        "Task": 677777777,
                    },
                    "Realtime": {"Revision": 6666666},
                    "Ner": {},
                },
            },
            "ProgramBuildInfo": {},
        }
    )

    return handlers.diff_two_version(resp_version_before, resp_version_after)
