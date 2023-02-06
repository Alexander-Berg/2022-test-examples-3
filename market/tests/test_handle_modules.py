import pytest
import yatest.common

import modules.handle_modules as hm
import fill_context
from helpers import prepare_service, add_api_yaml


def test_server(tmp_dir, service_yaml_dict):
    prepare_service(tmp_dir, service_yaml_dict)
    fill_context.fill_context()
    hm.handle_modules()
    return yatest.common.canonical_dir(tmp_dir)


def test_postgres(tmp_dir, service_yaml_dict):
    service_yaml_dict['modules'] = {'postgres': ''}
    prepare_service(tmp_dir, service_yaml_dict)
    fill_context.fill_context()
    hm.handle_modules()
    return yatest.common.canonical_dir(tmp_dir)


def test_validation(tmp_dir, service_yaml_dict):
    service_yaml_dict['modules'] = {'quartz': ''}
    prepare_service(tmp_dir, service_yaml_dict)
    fill_context.fill_context()
    with pytest.raises(Exception):
        hm.handle_modules()


def test_quartz(tmp_dir, service_yaml_dict):
    service_yaml_dict['modules'] = {'postgres': '', 'quartz': ''}
    prepare_service(tmp_dir, service_yaml_dict)
    fill_context.fill_context()
    hm.handle_modules()
    return yatest.common.canonical_dir(tmp_dir)


def test_client(tmp_dir, service_yaml_dict, find_refs):
    service_yaml_dict['clients'] = {'list': {
        'testservice': {
            'openapi_spec_path': 'market/infra/java-application/testservice/src/main/resources/openapi/api/api.yaml'
        }
    }
    }
    prepare_service(tmp_dir, service_yaml_dict)
    add_api_yaml(tmp_dir)
    fill_context.fill_context()
    hm.handle_modules()
    return yatest.common.canonical_dir(tmp_dir)
