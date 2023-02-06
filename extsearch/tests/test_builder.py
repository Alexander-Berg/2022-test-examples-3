# -*- coding: utf-8 -*-

import pytest
import yatest.common


def get_test_builder_binary_path():
    return yatest.common.binary_path(
        'extsearch/video/quality/delayed_view/test_builder/test_builder')


def test_auto_get_delayed_views():
    params = ['auto']
    params += ['--profiles', 'user_profiles_vvonly']
    params += ['--entity_trie', 'delayed_view_entity_base_trie']
    params += ['--serial_trie', 'delayed_view_serial_base_trie']
    return yatest.common.canonical_execute(get_test_builder_binary_path(), params)


def get_manual_test_names():
    params = [get_test_builder_binary_path()]
    params += ['manual_test_names']
    test_builder_execution = yatest.common.execute(params)
    return test_builder_execution.std_out.strip().split('\n')


@pytest.mark.parametrize('manual_test_name', get_manual_test_names())
def test_manual_get_delayed_views(manual_test_name):
    params = ['manual']
    params += ['--test_name', manual_test_name]
    params += ['--entity_trie', 'delayed_view_entity_base_trie']
    params += ['--serial_trie', 'delayed_view_serial_base_trie']
    return yatest.common.canonical_execute(get_test_builder_binary_path(), params)
