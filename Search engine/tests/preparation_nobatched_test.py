# -*- coding: utf-8 -*-
import json
import urllib
import urllib.parse

import pytest
import yatest
import preparation_batched
from preparation_not_batched import prepare_stream_no_batched

CONFIG_HAMSTER = {
    'cgi': 'nocache=da',
    'host': 'hamster.yandex.ru',
    'timestampCgi': 'pron',
    'preparer': "TestParser"
}

CONFIG_PROD = {
    'cgi': 'foo=bar',
    'host': 'yandex.ru',
    'timestampCgi': 'pron',
    'preparer': "TestParser"
}


def get_path(filename):
    return yatest.common.source_path("search/metrics/monitoring/tests/data/prepare_old/" + filename)


def load_json(filename):
    with open(get_path(filename)) as f:
        return json.load(f)


def test_build_single_config():
    assert preparation_batched.build_configurations({
        'configuration': get_path("config_single.json"),
        'preparer': "TestParser"
    }) == [CONFIG_HAMSTER]


def test_build_multiple_configs():
    assert preparation_batched.build_configurations({
        'configuration': yatest.common.source_path("search/metrics/monitoring/tests/data/prepare/config_multiple.json"),
        'preparer': "TestParser"
    }) == [CONFIG_HAMSTER, CONFIG_PROD]


def remove_cgis(url, cgis_to_ignore={"pron"}):
    schema, host, path, params, query, netloc = urllib.parse.urlparse(url)
    query_parameter_pairs = urllib.parse.parse_qsl(query)
    filtered_query_parameter_pairs = [(k.encode("utf-8"), v.encode("utf-8")) for k, v in query_parameter_pairs if k not in cgis_to_ignore]
    new_query = urllib.parse.urlencode(filtered_query_parameter_pairs)
    return urllib.parse.urlunparse((schema, host, path, params, new_query, netloc))


def compare_curls(curl_1, curl_2):
    assert set(curl_1.keys()) == set(curl_2.keys())
    for key in curl_1:
        if key == "userdata":
            continue
        if key != 'uri':
            if key in {'cookies', 'headers'}:
                assert sorted(curl_1[key]) == sorted(curl_2[key])
            else:
                assert curl_1[key] == curl_2[key]
        # the value at the pron key is a timestamp, it's set at prepare time, so it's removed
        assert remove_cgis(str(curl_1['uri'])) == remove_cgis(curl_2['uri'])


@pytest.mark.parametrize('config_file, queries_file, expected_curls_file', [
    ("config_single.json", "queries.json", "prepared_single.json"),
    ("config_multiple.json", "queries.json", "prepared_multiple.json"),
    ("config_multiple_with_config_ids.json", "queries.json", "prepared_multiple_with_config_ids.json"),
    ("config_profile.json", "queries.json", "prepared_profile.json"),
    ("config_multiple_baskets.json", "queries_multiple_baskets.json", "prepared_multiple_baskets.json"),
    ("config_multiple_baskets.json", "queries_multiple_baskets_with_cgi.json", "prepared_multiple_baskets_with_cgi.json"),
])
def test_prepare(config_file, queries_file, expected_curls_file):
    configurations = preparation_batched.build_configurations(
        {'configuration': get_path(config_file),
         'preparer': "TestParser"})
    curls = list(preparation_batched.prepare_stream(configurations, load_json(queries_file), batched_requests=False))
    expected = load_json(expected_curls_file)
    assert len(curls) == len(expected)
    for curl_1, curl_2 in zip(curls, expected):
        compare_curls(curl_1, curl_2)


@pytest.mark.parametrize('config_file, queries_file', [
    ("config_multiple.json", "queries_with_string_ids.json")
])
def test_prepare_with_string_ids(config_file, queries_file):
    configurations = preparation_batched.build_configurations(
        {'configuration': get_path(config_file),
         'preparer': "TestParser"})
    curls = list(prepare_stream_no_batched(configurations, load_json(queries_file), use_id_from_request=True))
    assert curls[0]['id'] == "10ee055b-48d2-4f13-a054-64807fd6276c"
    assert curls[1]['id'] == "10ee055b-48d2-4f13-a054-64807fd6276c-1"
