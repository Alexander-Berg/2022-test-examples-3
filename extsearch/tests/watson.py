#!/usr/bin/env python

import os
import yatest.common

BINARY_PATH = yatest.common.binary_path(
    'extsearch/video/robot/authorfromjson/tool/authorfromjson'
)
TIKTOK_CONFIG = yatest.common.source_path(
    'extsearch/video/robot/rthub/config/author_json_api/watson_tiktok_channel.json'
)
INSTAGRAM_CONFIG = yatest.common.source_path(
    'extsearch/video/robot/rthub/config/author_json_api/watson_instagram_channel.json'
)

def _run_test(input_json, config):
    output_json = 'result.json'
    yatest.common.execute([
        BINARY_PATH,
        '-i', input_json,
        '-c', config,
        '-o', output_json
    ])

    return yatest.common.canonical_file(output_json)

class TestTiktok():
    def test_social_networks(self):
        return _run_test('tiktok.json', TIKTOK_CONFIG)
    def test_social_networks2(self):
        return _run_test('tiktok2.json', TIKTOK_CONFIG)

class TestInstagram():
    def test_social_networks(self):
        return _run_test('instagram.json', INSTAGRAM_CONFIG)
    def test_social_networks2(self):
        return _run_test('instagram2.json', INSTAGRAM_CONFIG)
    def test_social_networks3(self):
        return _run_test('instagram3.json', INSTAGRAM_CONFIG)
