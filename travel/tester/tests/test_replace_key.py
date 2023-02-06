# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.common23.tester.utils.replace_setting import ReplaceKey

DICT_SETTING = {}
KEY = 'key'
OLD_VALUE = 'old_value'
NEW_VALUE = 'new_value'


def undefine_setting():
    if KEY in DICT_SETTING:
        del DICT_SETTING[KEY]


def test_decorator():
    @ReplaceKey(DICT_SETTING, KEY, NEW_VALUE)
    def check_under_decorator():
        assert DICT_SETTING[KEY] == NEW_VALUE

    undefine_setting()
    check_under_decorator()
    assert KEY not in DICT_SETTING

    DICT_SETTING[KEY] = OLD_VALUE
    check_under_decorator()
    assert DICT_SETTING[KEY] == OLD_VALUE


def test_context_mananger():
    undefine_setting()

    with ReplaceKey(DICT_SETTING, KEY, NEW_VALUE) as new_value:
        assert new_value == NEW_VALUE
        assert DICT_SETTING[KEY] == NEW_VALUE

    assert KEY not in DICT_SETTING

    DICT_SETTING[KEY] = OLD_VALUE

    with ReplaceKey(DICT_SETTING, KEY, NEW_VALUE) as new_value:
        assert new_value == NEW_VALUE
        assert DICT_SETTING[KEY] == NEW_VALUE

    assert DICT_SETTING[KEY] == OLD_VALUE


@ReplaceKey(DICT_SETTING, KEY, NEW_VALUE)
def test_fixture():
    assert DICT_SETTING[KEY] == NEW_VALUE


def test_no_edit_other_values():
    DICT_SETTING = {'key1': 'value1', 'key2': 'value2'}

    with ReplaceKey(DICT_SETTING, KEY, NEW_VALUE):
        assert DICT_SETTING == {'key1': 'value1', 'key2': 'value2', KEY: NEW_VALUE}

    assert DICT_SETTING == {'key1': 'value1', 'key2': 'value2'}
