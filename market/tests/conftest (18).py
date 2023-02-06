import os

import pytest

from argparse import Namespace
from helpers import source_path, output_path, binary_path


@pytest.fixture()
def service_yaml_dict():
    service_yaml_dict = dict(
        java_service=dict(
            service_name='market-java-application-template',
            root_package='ru.yandex.market.template',
            deploy_type='yandex_deploy',
        ),
        trace=dict(module='PYTHON_SCRIPT_TEST')
    )
    return service_yaml_dict


@pytest.fixture()
def tmp_dir(request, mocker):
    test_path = output_path(os.path.join('results', request.module.__name__ + '-' + request.function.__name__))
    args = Namespace(path=test_path,
                     idea=test_path + '_idea',
                     arcadia=source_path(''),
                     generation_folder=None,
                     vcs_add=False,
                     should_call_ya_ide=True,
                     ide_flags='')
    mocker.patch('fill_context.parse_argument', return_value=args)
    return test_path


@pytest.fixture(autouse=True)
def openapi_generator_path(mocker):
    def get_openapi_generator_path():
        return os.path.join(
            binary_path(os.path.join('market', 'infra', 'java-application', 'mj', 'v1',
                                     'generate-project-tool', 'openapi-generator')),
            'openapi-generator')
    mocker.patch('api.prepare_server.get_openapi_generator_path', side_effect=get_openapi_generator_path)


@pytest.fixture()
def find_refs(mocker):
    mocker.patch('modules.modules.find_refs', return_value=[
        '\nSET_APPEND(CLIENT_DEPENDENT_FILES ${ARCADIA_ROOT}/market/infra/java-application/testservice/src/main/resources/openapi/api/api.yaml)'])


@pytest.fixture(autouse=True)
def add_permissions_for_ci_test(mocker):
    def add_permissions(test_path):
        for root, dirs, files in os.walk(test_path):
            for d in dirs:
                os.chmod(os.path.join(root, d), 0o766)
            for f in files:
                os.chmod(os.path.join(root, f), 0o766)
    mocker.patch('utils.add_write_permissions_to_files', side_effect=(lambda path: add_permissions(path)))
