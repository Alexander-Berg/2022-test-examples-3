# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting
from travel.rasp.library.python.common23.xgettext.i18n import tformat, _reload_keysets_arcadia, _keysets


def test_tformat():
    assert tformat('text') == 'text'
    assert tformat('text <tag/>') == 'text <tag></tag>'
    assert tformat('text <tag>content</tag>') == 'text <tag>content</tag>'
    assert tformat('text <tag/>', tag='tag value') == 'text tag value'

    handler = mock.Mock(return_value='tag value')
    assert tformat('text <tag/>', tag=handler) == 'text tag value'
    handler.assert_called_once_with()

    handler = mock.Mock(return_value='tag value')
    assert tformat('text <tag attr="value"/>', tag=handler) == 'text tag value'
    handler.assert_called_once_with(attr='value')

    handler = mock.Mock(return_value='tag value')
    assert tformat('text <tag attr="value">content</tag>', tag=handler) == 'text tag value'
    handler.assert_called_once_with('content', attr='value')

    handler = mock.Mock(return_value='tag value')
    assert tformat('text <tag attr="value">text <tag2>content</tag2> text</tag>', tag=handler) == 'text tag value'
    handler.assert_called_once_with('text <tag2>content</tag2> text', attr='value')


def test_reload_keysets_arcadia():
    xgettext_keysets = {
        'common_test': {
            'filename': 'keyset_test.json',
        }
    }

    assert 'common_test' not in _keysets

    with replace_setting('XGETTEXT_KEYSETS', xgettext_keysets):
        res = _reload_keysets_arcadia()
        assert res is True
        assert 'common_test' in _keysets

        res = _reload_keysets_arcadia()
        assert res is False
        assert 'common_test' in _keysets
