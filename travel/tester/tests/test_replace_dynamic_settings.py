# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.common23.dynamic_settings.core import DynamicSetting
from travel.rasp.library.python.common23.dynamic_settings.default import conf
from travel.rasp.library.python.common23.tester.utils.replace_dynamic_setting import replace_dynamic_setting_keys


def test_replace_dynamic_setting_keys():
    conf.register_settings(
        TEST_REPLACE_A=DynamicSetting({'key1': 1}),
        TEST_REPLACE_B=DynamicSetting({'key1': 2}),
    )

    assert conf.TEST_REPLACE_A == {'key1': 1}
    assert conf.TEST_REPLACE_B == {'key1': 2}

    with replace_dynamic_setting_keys('TEST_REPLACE_B', key1=42, key2=43):
        assert conf.TEST_REPLACE_A == {'key1': 1}
        assert conf.TEST_REPLACE_B == {'key1': 42, 'key2': 43}

    assert conf.TEST_REPLACE_A == {'key1': 1}
    assert conf.TEST_REPLACE_B == {'key1': 2}
