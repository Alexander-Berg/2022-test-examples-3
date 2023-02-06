from shutil import copy2

import yatest.common
import os
import yaml
from utils import copy_tree_with_permissions_added

test_resources_dir = 'resources'


def source_path(path):
    try:
        return yatest.common.source_path(path)
    except AttributeError:
        # only for local pycharm tests
        return os.path.abspath(os.path.join(os.environ["PWD"], '../../../../../../../../', path))


def binary_path(path):
    try:
        return yatest.common.binary_path(path)
    except AttributeError:
        # only for local pycharm tests
        # TODO fix path
        return os.path.join(os.environ["PWD"], path)


def work_path(path):
    try:
        return yatest.common.work_path(path)
    except AttributeError:
        # only for local pycharm tests
        return os.path.join(os.environ["PWD"], path)


def output_path(path):
    try:
        return yatest.common.output_path(path)
    except AttributeError:
        # only for local pycharm tests
        # TODO fix path
        return os.path.join(os.environ["PWD"], 'test-results', path)


def source_test_path(path):
    try:
        return yatest.common.test_source_path(path)
    except AttributeError:
        # only for local pycharm tests
        return os.path.join(os.environ["PWD"], path)


def prepare_service(test_path, data):
    print('Create test service in dir: ' + test_path)
    copy_tree_with_permissions_added(source_test_path(os.path.join(test_resources_dir, 'test_service')), test_path)
    with open(os.path.join(test_path, 'service.yaml'), 'w') as file:
        yaml.dump(data, file, default_flow_style=False)


def add_api_yaml(test_path):
    copy2(source_test_path(os.path.join(test_resources_dir, 'test_api_yaml', 'api.yaml')),
          os.path.join(test_path, 'src', 'main', 'resources', 'openapi', 'api', 'api.yaml'))
