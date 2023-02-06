# -*- coding: utf-8 -*-

import pytest

from django.conf import settings

from travel.avia.library.python.tester.utils.replace_setting import replace_setting


SETTING_NAME = 'TEST_SETTING'
NEW_VALUE = 'new_value'
OLD_VALUE = 'old_value'

_UNDEFINED = object()


def undefine_setting():
    if hasattr(settings, SETTING_NAME):
        delattr(settings, SETTING_NAME)


@pytest.fixture(scope='module', autouse=True)
def manage_setting(request):
    old_value = getattr(settings, SETTING_NAME, _UNDEFINED)

    def fin():
        if old_value == _UNDEFINED:
            undefine_setting()
        else:
            setattr(settings, SETTING_NAME, old_value)

    request.addfinalizer(fin)


def test_decorator():
    @replace_setting(SETTING_NAME, NEW_VALUE)
    def check_under_decorator():
        assert getattr(settings, SETTING_NAME) == NEW_VALUE

    undefine_setting()
    check_under_decorator()
    assert not hasattr(settings, SETTING_NAME)

    setattr(settings, SETTING_NAME, OLD_VALUE)
    check_under_decorator()
    assert getattr(settings, SETTING_NAME) == OLD_VALUE


def test_context_mananger_undefined():
    undefine_setting()

    with replace_setting(SETTING_NAME, NEW_VALUE) as new_value:
        assert new_value == NEW_VALUE
        assert getattr(settings, SETTING_NAME) == NEW_VALUE

    assert not hasattr(settings, SETTING_NAME)


def test_context_mananger_with_old_value():
    setattr(settings, SETTING_NAME, OLD_VALUE)

    with replace_setting(SETTING_NAME, NEW_VALUE) as new_value:
        assert new_value == NEW_VALUE
        assert getattr(settings, SETTING_NAME) == NEW_VALUE

    assert getattr(settings, SETTING_NAME) == OLD_VALUE


@pytest.fixture()
def some_fixture():
    return 'yeah'


@replace_setting(SETTING_NAME, NEW_VALUE)
def test_fixture_with_decorator(some_fixture):
    assert getattr(settings, SETTING_NAME) == NEW_VALUE
    assert some_fixture == 'yeah'


class TestWithClassMethod(object):
    @replace_setting(SETTING_NAME, NEW_VALUE)
    def test_decorator(self, some_fixture):
        assert getattr(settings, SETTING_NAME) == NEW_VALUE
        assert some_fixture == 'yeah'
