import json

import pytest

import test_utils
from profile_utils import get_profile, get_parsed_profile


def test_load_profile():
    loaded_profile = json.loads(get_profile("dummy"))
    expected = test_utils.read_json_test_data("dummy.json", "search/scraper/profile/")
    assert loaded_profile == expected


def test_get_parsed_profile():
    parsed_profile = get_parsed_profile("dummy")
    expected = test_utils.read_json_test_data("dummy.json", "search/scraper/profile/")
    assert parsed_profile == expected


def test_get_missing_profile():
    with pytest.raises(ValueError):
        get_parsed_profile("missing_profile")
