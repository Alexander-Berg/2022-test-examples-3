# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
from rest_framework import serializers

from travel.rasp.wizards.wizard_lib.serialization.thread_express_type import ThreadExpressType, parse_thread_express_type


def test_parse_thread_express_type_validation():
    with pytest.raises(serializers.ValidationError) as excinfo:
        parse_thread_express_type('invalid')
    assert excinfo.value.detail == ["invalid thread express type: u'invalid'"]

    assert parse_thread_express_type('aeroexpress') == ThreadExpressType.AEROEXPRESS
    assert parse_thread_express_type('express') == ThreadExpressType.EXPRESS
