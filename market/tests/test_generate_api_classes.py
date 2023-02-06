import yatest.common
import pytest

import fill_context
import modules.handle_modules as hm
import api.prepare_server as prepare_server
from helpers import prepare_service, add_api_yaml


@pytest.fixture()
def java_binary(mocker):
    mocker.patch('api.prepare_server.get_java_binary', return_value=yatest.common.java_bin())


def test_api(tmp_dir, service_yaml_dict, java_binary, find_refs):
    prepare_service(tmp_dir, service_yaml_dict)
    add_api_yaml(tmp_dir)
    fill_context.fill_context()
    hm.handle_modules()
    prepare_server.generate_api_classes('java')
    return yatest.common.canonical_dir(tmp_dir)
