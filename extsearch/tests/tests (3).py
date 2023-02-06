# -*- coding: utf-8 -*-

from mapreduce.yt.python.yt_stuff import yt_stuff
import json
import yatest.common


def get_trie_ops_binary_path():
    return yatest.common.binary_path('extsearch/video/quality/delayed_view/trie_ops/trie_ops')


def build_entity(yt_proxy):
    trie_ops_binary_path = get_trie_ops_binary_path()
    trie_file_path = 'entity_base_trie'
    build_entity_params = ['build_entity', '--proxy', yt_proxy, '--base_table',\
        '//entity_base', '--trie_path', trie_file_path]
    yatest.common.execute([trie_ops_binary_path] + build_entity_params)
    return trie_file_path


def build_serial(yt_proxy):
    trie_ops_binary_path = get_trie_ops_binary_path()
    trie_file_path = 'serial_base_trie'
    build_serial_params = ['build_serial', '--proxy', yt_proxy, '--base_table',\
        '//serial_base', '--trie_path', trie_file_path]
    yatest.common.execute([trie_ops_binary_path] + build_serial_params)
    return trie_file_path


def configure_yt_env(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    for base_file_path in ['entity_base', 'serial_base']:
        base = open(base_file_path, 'rb').read()
        yt_client.write_table('//' + base_file_path, base, format='json', raw=True)
    return yt_stuff.get_server()


def test_delayed_view_build_entity(yt_stuff):
    yt_proxy = configure_yt_env(yt_stuff)
    return yatest.common.canonical_file(build_entity(yt_proxy))


def test_delayed_view_print_entity(yt_stuff):
    yt_proxy = configure_yt_env(yt_stuff)
    trie_ops_binary_path = get_trie_ops_binary_path()
    print_entity_params = ['print_entity', '--trie_path', build_entity(yt_proxy)]
    return yatest.common.canonical_execute(trie_ops_binary_path, print_entity_params)


def test_delayed_view_build_serial(yt_stuff):
    yt_proxy = configure_yt_env(yt_stuff)
    return yatest.common.canonical_file(build_serial(yt_proxy))


def test_delayed_view_print_serial(yt_stuff):
    yt_proxy = configure_yt_env(yt_stuff)
    trie_ops_binary_path = get_trie_ops_binary_path()
    print_serial_params = ['print_serial', '--trie_path', build_serial(yt_proxy)]
    return yatest.common.canonical_execute(trie_ops_binary_path, print_serial_params)
