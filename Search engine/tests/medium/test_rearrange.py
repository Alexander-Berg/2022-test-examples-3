import yatest.common

import common

import json
import os


def test_host_classifier_dat_files():
    files = {
        "afisha_event.dat": 5,
        "auto.dat": 5,
        "books.dat": 4,
        "explain.dat": 5,
        "music.dat": 4,
        "vacancy.dat": 4,
        "video.dat": 4,
    }
    for filename in files.keys():
        file_path = yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "host_classifier", "dat_source", filename))
        for line in open(file_path, "r"):
            parts = line.split("\t")
            assert len(parts) == files[filename]
            common.check_some_type_items(parts[1:], int)


def test_same_services_checker_configs():
    rearrange_file_path = yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "same_services_checker", "config.json"))
    fast_file_path = yatest.common.build_path(os.path.join(common.REARRANGE_FAST_BUILD_PREFIX, "same_services_checker", "config.json"))
    rearrange_file = open(rearrange_file_path, "r")
    fast_file = open(fast_file_path, "r")
    rearrange_keys = set(json.loads(rearrange_file.read()).keys())
    fast_keys = set(json.loads(fast_file.read()).keys())
    assert len(fast_keys & rearrange_keys) == 0


def test_rearrange_same_services_checker_json():
    def check_duplicate_keys(ordered_pairs):
        s = set()
        for k, v in ordered_pairs:
            assert k not in s
            s.add(k)
    rearrange_file_path = yatest.common.build_path(os.path.join(common.REARRANGE_BUILD_PREFIX, "same_services_checker", "config.json"))
    rearrange_file = open(rearrange_file_path, "r")
    json.loads(rearrange_file.read(), object_pairs_hook=check_duplicate_keys)


def test_fast_same_services_checker_json():
    def check_duplicate_keys(ordered_pairs):
        s = set()
        for k, v in ordered_pairs:
            assert k not in s
            s.add(k)
    fast_file_path = yatest.common.build_path(os.path.join(common.REARRANGE_FAST_BUILD_PREFIX, "same_services_checker", "config.json"))
    fast_file = open(fast_file_path, "r")
    json.loads(fast_file.read(), object_pairs_hook=check_duplicate_keys)


def test_fast_mobile_applications_to_replace_config():
    os_types = set(["ios", "android", "harmonyos"])
    fast_file_path = yatest.common.build_path(os.path.join(common.REARRANGE_FAST_BUILD_PREFIX, "unisearch", "applications_to_replace.json"))
    fast_file = open(fast_file_path, "r")
    data = json.loads(fast_file.read())
    for os_type, os_conf in data.iteritems():
        assert os_type in os_types
        for conf in os_conf:
            assert "From" in conf and len(conf["From"]) > 0
            assert "To" in conf and len(conf["To"]) > 0
            assert ("NotAvailable" in conf and conf["NotAvailable"]) or ("IsRemoved" in conf and conf["IsRemoved"])


def test_fast_stocks_data_json():
    def validate_value(value):
        for key in ['##instrument_id', '##display_name', '##ticker', '##logo_url',
                    '##canonical_url', '##exchange', '##currency_code']:
            assert key in value

    def check_duplicate_keys(ordered_pairs):
        s = set()
        for k, v in ordered_pairs:
            assert k not in s
            s.add(k)
        return ordered_pairs

    fast_file_path = yatest.common.build_path(os.path.join(common.REARRANGE_FAST_BUILD_PREFIX, "stocks", "stocks_data.json"))
    with open(fast_file_path, "r") as f:
        row_data = f.read()
        json.loads(row_data, object_pairs_hook=check_duplicate_keys)
        data = json.loads(row_data)
        for value in data.values():
            validate_value(value)


def test_fast_static_wizard_data_json():
    def validate_value(value):
        for key in ['SerpData', 'VerticalSettings', 'Enabled']:
            assert key in value
        for key in ['blender_url']:
            assert key in value['SerpData']
        for key in ['name']:
            assert key in value['VerticalSettings']

    def check_duplicate_keys(ordered_pairs):
        s = set()
        for k, v in ordered_pairs:
            assert k not in s
            s.add(k)
        return ordered_pairs

    fast_file_path = yatest.common.build_path(os.path.join(common.REARRANGE_FAST_BUILD_PREFIX, "static_wizard", "static_wizard_data.json"))
    with open(fast_file_path, "r") as f:
        row_data = f.read()
        json.loads(row_data, object_pairs_hook=check_duplicate_keys)
        data = json.loads(row_data)
        for value in data.values():
            validate_value(value)
