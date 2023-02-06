# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import requests

from common.data_api.base import BaseOAuthApi


class TestBaseOAuthApi(object):
    def test_timeout(self):
        with mock.patch.object(requests.Session, 'post') as m_post:
            client = BaseOAuthApi('mytoken', 'http://example.com/', timeout=10)
            client._process_text_query('post', 'my_url')
            m_post.assert_called_once_with('http://example.com/my_url', timeout=10)

            client._process_text_query('post', 'my_url', timeout=42)
            assert len(m_post.call_args_list) == 2
            m_post.assert_called_with('http://example.com/my_url', timeout=42)

            client = BaseOAuthApi('mytoken', 'http://example.com/')
            client._process_text_query('post', 'my_url')
            assert len(m_post.call_args_list) == 3
            m_post.assert_called_with('http://example.com/my_url', timeout=5)
