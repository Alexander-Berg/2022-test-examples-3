# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from rest_framework import serializers

from common.tester.utils.replace_setting import replace_setting
from travel.rasp.wizards.wizard_lib.serialization.language import parse_language


@replace_setting('LANGUAGE_CODE', 'eo')
def test_parse_language_default():
    assert parse_language(None) == 'eo'


@replace_setting('FRONTEND_LANGUAGES', ['ru', 'eo', 'vo'])
def test_parse_language_validation():
    with pytest.raises(serializers.ValidationError) as excinfo:
        parse_language('en')
    assert excinfo.value.detail == ["invalid language value: it should be one of [u'ru', u'eo', u'vo']"]

    assert parse_language('ru') == 'ru'
