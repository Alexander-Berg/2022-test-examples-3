# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest

from lxml import etree

from travel.avia.library.python.common.utils.lxmlutils import get_sub_tag_text, SubTagNotFound


def test_get_sub_tag_text():
    xml = etree.fromstring
    assert get_sub_tag_text(xml('<a><b>some text</b></a>'), 'b') == 'some text'
    assert get_sub_tag_text(xml('<a><b/></a>'), 'b') is None
    assert get_sub_tag_text(xml('<a><b></b></a>'), 'b') is None
    with pytest.raises(SubTagNotFound):
        assert get_sub_tag_text(xml('<a></a>'), 'b')

    assert get_sub_tag_text(xml('<a></a>'), 'b', 'default') == 'default'
    assert get_sub_tag_text(xml('<a><b/></a>'), 'b', 'default') == 'default'
    assert get_sub_tag_text(xml('<a><b></b></a>'), 'b', 'default') == 'default'

    assert get_sub_tag_text(xml('<a></a>'), 'b', if_not_found='if_not_found') == 'if_not_found'
    assert get_sub_tag_text(xml('<a><b/></a>'), 'b', if_not_found='if_not_found') is None

    with pytest.raises(SubTagNotFound):
        assert get_sub_tag_text(xml('<a></a>'), 'b', if_text_is_empty='if_text_is_empty')
    assert get_sub_tag_text(xml('<a><b/></a>'), 'b', if_text_is_empty='if_text_is_empty') == 'if_text_is_empty'
