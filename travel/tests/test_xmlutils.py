# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from lxml import etree

from travel.library.python.xmlutils.xmlutils import get_sub_tag_text, SubTagNotFound, get_bool_from_sub_tag, IncorrectValue


@pytest.mark.parametrize('xml_part, subtag, kwargs, expected', (
    ('<a><b>some text</b></a>', 'b', {}, 'some text'),
    ('<a><b/></a>', 'b', {}, None),
    ('<a><b></b></a>', 'b', {}, None),
    ('<a></a>', 'b', {}, SubTagNotFound),

    ('<a></a>', 'b', {'default': 'default'}, 'default'),
    ('<a><b/></a>', 'b', {'default': 'default'}, 'default'),
    ('<a><b></b></a>', 'b', {'default': 'default'}, 'default'),

    ('<a></a>', 'b', {'if_not_found': 'if_not_found'}, 'if_not_found'),
    ('<a><b/></a>', 'b', {'if_not_found': 'if_not_found'}, None),

    ('<a></a>', 'b', {'if_text_is_empty': 'if_text_is_empty'}, SubTagNotFound),
    ('<a><b/></a>', 'b', {'if_text_is_empty': 'if_text_is_empty'}, 'if_text_is_empty'),
))
def test_get_sub_tag_text(xml_part, subtag, kwargs, expected):
    if not isinstance(expected, type):
        assert get_sub_tag_text(etree.fromstring(xml_part), subtag, **kwargs) == expected
    else:
        with pytest.raises(expected):
            get_sub_tag_text(etree.fromstring(xml_part), subtag, **kwargs)


@pytest.mark.parametrize('xml_part, subtag, kwargs, expected', (
    ('<a><b>1</b></a>', 'b', {}, True),
    ('<a><b>true</b></a>', 'b', {}, True),
    ('<a><b>0</b></a>', 'b', {}, False),
    ('<a><b>false</b></a>', 'b', {}, False),

    ('<a><b>3</b></a>', 'b', {}, IncorrectValue),
    ('<a><b /></a>', 'b', {}, IncorrectValue),
    ('<a></a>', 'b', {}, SubTagNotFound),

    ('<a><b>3</b></a>', 'b', {'default': 'default'}, IncorrectValue),
    ('<a><b /></a>', 'b', {'default': 'default'}, 'default'),
    ('<a></a>', 'b', {'default': 'default'}, 'default'),

    ('<a><b>3</b></a>', 'b', {'if_not_found': 'if_not_found'}, IncorrectValue),
    ('<a><b /></a>', 'b', {'if_not_found': 'if_not_found'}, IncorrectValue),
    ('<a></a>', 'b', {'if_not_found': 'if_not_found'}, 'if_not_found'),

    ('<a><b>3</b></a>', 'b', {'if_text_is_empty': 'if_text_is_empty'}, IncorrectValue),
    ('<a><b /></a>', 'b', {'if_text_is_empty': 'if_text_is_empty'}, 'if_text_is_empty'),
    ('<a></a>', 'b', {'if_text_is_empty': 'if_text_is_empty'}, SubTagNotFound),

    ('<a><b>3</b></a>', 'b', {'if_invalid': 'if_invalid'}, 'if_invalid'),
    ('<a><b /></a>', 'b', {'if_invalid': 'if_invalid'}, 'if_invalid'),
    ('<a></a>', 'b', {'if_invalid': 'if_invalid'}, SubTagNotFound),

    ('<a><b>3</b></a>', 'b', {'default': 'default', 'if_not_found': 'if_not_found',
                              'if_text_is_empty': 'if_text_is_empty', 'if_invalid': 'if_invalid'}, 'if_invalid'),
    ('<a><b /></a>', 'b', {'default': 'default', 'if_not_found': 'if_not_found',
                           'if_text_is_empty': 'if_text_is_empty', 'if_invalid': 'if_invalid'}, 'default'),
    ('<a></a>', 'b', {'default': 'default', 'if_not_found': 'if_not_found',
                      'if_text_is_empty': 'if_text_is_empty', 'if_invalid': 'if_invalid'}, 'default'),
))
def test_get_bool_from_sub_tag(xml_part, subtag, kwargs, expected):
    if not isinstance(expected, type):
        assert get_bool_from_sub_tag(etree.fromstring(xml_part), subtag, **kwargs) == expected
    else:
        with pytest.raises(expected):
            get_bool_from_sub_tag(etree.fromstring(xml_part), subtag, **kwargs)
