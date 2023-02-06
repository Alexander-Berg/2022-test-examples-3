# -*- coding: utf8 -*-
import pytest
from pyfakefs.fake_filesystem_unittest import Patcher

from travel.avia.admin.lib.script_helpers.environment_detector import environment_detector


@pytest.mark.parametrize('contents, expected_env_type', (
    ('trash', None),
    ('production', 'production'),
    ('development', 'development'),
    ('stress', 'stress'),
    ('testing', 'testing'),
))
def test_environment_detector(contents, expected_env_type):
    with Patcher() as patcher:
        patcher.fs.create_file('/etc/yandex/environment.type', contents=contents)
        assert environment_detector.get_environment_type() == expected_env_type


def test_environment_file_dont_exists():
    with Patcher():
        assert environment_detector.get_environment_type() is None
