import os
import tempfile

import pytest
import yaml

from tankapi.client.cmd_client import define_type, UndefinedConfigType, merge_yaml, add_cli_options

try:
    from yatest import common
    PATH = common.source_path('load/projects/old-yatank-internal-api-client/tankapi/client/tests')
except ImportError:
    PATH = os.path.dirname(__file__)


@pytest.mark.parametrize('cfg_list, expected_type', [
    ([PATH + 'foo/bar/load.yaml', PATH + 'load2.yaml'], '.yaml'),
    ([PATH + 'foo/bar/load.ini', PATH + 'load2.ini'], '.ini')
])
def test_define_type(cfg_list, expected_type):
    assert define_type(cfg_list) == expected_type


def test_undefined_type():
    with pytest.raises(UndefinedConfigType) as e:
        define_type([PATH + 'foo/bar/load.ini', PATH + 'load2.yaml'])
    assert e is not None


def test_merge_yaml():
    merged_cfg = merge_yaml([PATH + 'test1.yaml', PATH + 'test2.yaml'])
    with open(PATH + 'test12.yaml') as expected, open(merged_cfg) as merged:
        assert yaml.load(expected) == yaml.load(merged)


@pytest.mark.parametrize('options, cfg_file, cfg_type, expected', [
    (["phantom.ammofile=https://proxy.sandbox.yandex-team.ru/399736244"],
     PATH + 'test1.yaml', PATH + '.yaml', PATH + 'test1-o.yaml')
])
def test_add_cli_options(options, cfg_file, cfg_type, expected):
    with open(cfg_file) as f:
        fd, copy = tempfile.mkstemp(text=True)
        os.write(fd, f.read())
        os.close(fd)
    with_options = add_cli_options(copy, options, cfg_type)
    with open(with_options) as f1, open(expected) as f2:
        assert yaml.load(f1) == yaml.load(f2)
