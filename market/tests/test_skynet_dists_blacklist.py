# coding: utf-8

from market.idx.pylibrary.skynet_dists_blacklist import SkynetDistsBlacklist

import json


def test_from_empty_json():
    blacklist = SkynetDistsBlacklist.from_json_str('[]')
    assert blacklist.is_good_dist('search-part-base-0')


def test_from_json():
    GOOD_DIST = 'search-part-base-0'
    BAD_DIST = 'search-part-base-forbidden-0'
    blacklist = SkynetDistsBlacklist.from_json_str(r'["^search-part-base-forbidden-\\d+$"]')
    assert blacklist.is_good_dist(GOOD_DIST)
    assert not blacklist.is_good_dist(BAD_DIST)


def test_serialize():
    PATTERN = '[".*"]'
    blacklist = SkynetDistsBlacklist.from_json_str(PATTERN)
    assert PATTERN == str(blacklist)


def test_long_blacklist():
    patterns = json.dumps([
        '^search-model-report-data$',
        '^search-model-stats$',
        '^search-part-\\d+$',
        '^search-snippet-blue-\\d+$',
        '^book-snippet-\\d+$',
        '^model-snippet-\\d+$',
        '^search-snippet-\\d+$'
    ])
    blacklist = SkynetDistsBlacklist.from_json_str(patterns)

    assert not blacklist.is_good_dist('search-model-report-data')
    assert not blacklist.is_good_dist('search-snippet-blue-0')
    assert blacklist.is_good_dist('search-part-base-0')
