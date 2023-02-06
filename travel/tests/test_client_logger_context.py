# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from ylog.context import get_log_context

from travel.library.python.base_http_client.client_logger_context import client_logger_context


def test_client_logger_context():
    http_client_name = 'some_http_client'

    with client_logger_context(http_client_name=http_client_name):
        context = get_log_context()
        assert context.get('http_client_name') == http_client_name
        assert 'http_client_request_id' in context

    context = get_log_context()
    assert not ('http_client_name' in context or 'http_client_request_id' in context)
