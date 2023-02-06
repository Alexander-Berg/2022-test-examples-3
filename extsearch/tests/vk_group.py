#!/usr/bin/env python

import os
import yatest.common

BINARY_PATH = yatest.common.binary_path(
    'extsearch/video/robot/authorfromjson/tool/authorfromjson'
)
CONFIG_PATH = yatest.common.source_path(
    'extsearch/video/robot/rthub/config/author_json_api/api.vk.group.json'
)

def _run_test(input_json):
    output_json = 'result.json'
    yatest.common.execute([
        BINARY_PATH,
        '-i', input_json,
        '-c', CONFIG_PATH,
        '-o', output_json
    ])

    return yatest.common.canonical_file(output_json)

class TestVkGroup():
    # def test_sample(self):
    #     return _run_test('vk.api.group.sample.json')

    def test_social_networks(self):
        return _run_test('vk.api.group.with.social.networks.json')

    def test_closed(self):
        return _run_test('vk.api.group.closed.json')

    def test_deleted(self):
        return _run_test('vk.api.group.deleted.json')
