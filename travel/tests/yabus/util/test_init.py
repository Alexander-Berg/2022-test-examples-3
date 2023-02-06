# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
from yabus.util import LazyStr


class TestLazyStr(object):
    def test_str(self):
        m_func = mock.Mock(return_value='string value')
        s = LazyStr(m_func)

        assert not m_func.called
        assert str(s) == 'string value'
        assert m_func.call_count == 1
        assert str(s) == 'string value'
        assert m_func.call_count == 1
