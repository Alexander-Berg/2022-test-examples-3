# -*- coding: utf-8 -*-
import pytest

from feature_flag_client.ab_content import ABFlagsContent


def test_creation_ab_content():
    assert ABFlagsContent({'TEST': '1'}) is not None


@pytest.mark.parametrize('value, expected', [
    ('1', True),
    ('0', False),
    ('some', False),
    (1, False),
    (0, False),
    (None, False),
])
def test_enabled(value, expected):
    ab_content = ABFlagsContent({'TEST': value})
    assert expected == ab_content.flag_enabled('TEST')


def test_disabled_empty_flag():
    ab_content = ABFlagsContent({'TEST': '1'})
    assert not ab_content.flag_enabled('DISABLED')
