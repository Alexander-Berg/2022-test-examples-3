import yatest.common

import fill_context
import package_json
from helpers import prepare_service


def test_package_json(tmp_dir, service_yaml_dict):
    service_yaml_dict['java_service']['deploy_type'] = 'nanny'
    prepare_service(tmp_dir, service_yaml_dict)
    fill_context.fill_context()
    package_json.process_package_json()
    return yatest.common.canonical_dir(tmp_dir)
