# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.data_api.file_wrapper.config import get_wrapper_creator, FileWrapperConfigs
from common.data_api.file_wrapper.wrappers import SandboxFileWrapper, LocalFileWrapper
from common.settings.configuration import Configuration
from common.tester.utils.replace_setting import replace_setting


FILE_KEY, RES_TYPE = 'RES', 'RES_TYPE'
FileWrapperConfigs.register_config(
    key=FILE_KEY,
    config={
        Configuration.DEVELOPMENT: {
            'class': LocalFileWrapper,
        },
        'default': {
            'class': SandboxFileWrapper,
            'kwargs': {
                'resource_type': RES_TYPE,
                'ttl': 90,
                'attrs': {
                    'environment': Configuration.TESTING
                }
            }
        }
    }
)


def test_get_file_wrapper():
    with replace_setting('APPLIED_CONFIG', Configuration.TESTING):
        wrapper_creator = get_wrapper_creator(FILE_KEY)
        assert wrapper_creator.wrapper_class == SandboxFileWrapper
        sandbox_wrapper = wrapper_creator.get_file_wrapper(file_path='/xxx/123')
        assert sandbox_wrapper.path == '/xxx/123'
        assert sandbox_wrapper.resource_type == RES_TYPE
        assert sandbox_wrapper.ttl == 90
        assert sandbox_wrapper.attrs == FileWrapperConfigs.get_config(FILE_KEY)['default']['kwargs']['attrs']

        wrapper_creator = get_wrapper_creator(FILE_KEY, attrs={'attr_1': 'value_1'})
        sandbox_wrapper = wrapper_creator.get_file_wrapper(file_path='/xxx/123')
        assert sandbox_wrapper.path == '/xxx/123'
        assert sandbox_wrapper.attrs == {'attr_1': 'value_1'}

    wrapper_creator = get_wrapper_creator(FILE_KEY)
    assert wrapper_creator.wrapper_class == LocalFileWrapper
    local_file_wrapper = wrapper_creator.get_file_wrapper(file_path='/xxx/123')
    assert not hasattr(local_file_wrapper, 'resource_type')


def test_file_wrapper_configs():
    FileWrapperConfigs.register_config('key_1', {'config': 'params'})
    assert FileWrapperConfigs.configs['key_1'] == {'config': 'params'}

    with pytest.raises(AssertionError):
        FileWrapperConfigs.register_config('key_1', {'config_55': 'params'})

    FileWrapperConfigs.register_config('key_2', {'config_55': 'params'})
    assert FileWrapperConfigs.configs['key_2'] == {'config_55': 'params'}

    assert FileWrapperConfigs.get_config('key_1') == {'config': 'params'}
    with pytest.raises(KeyError):
        FileWrapperConfigs.get_config('key_3')
