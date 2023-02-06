#!/usr/bin/env python

import os
import yatest.common

BINARY_PATH = yatest.common.binary_path(
    'extsearch/video/robot/authorfromjson/tool/authorfromjson'
)
CONFIG_PATH = yatest.common.source_path(
    'extsearch/video/robot/rthub/config/author_json_api/api.youtube.channel.json'
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

class TestYoutubeChannel():
    def test_empty(self):
        return _run_test('youtube.api.channel.empty.response.json')

    def test_sample(self):
        return _run_test('youtube.api.channel.sample.json')

    def test_hidden_subscribers(self):
        return _run_test('youtube.api.channel.sample.with.hidden.subscribers.json')

    def test_multiple_texts(self):
        return _run_test('youtube.api.channel.multiple.texts.json')

    def test_country(self):
        return _run_test('youtube.api.channel.with.country.json')
