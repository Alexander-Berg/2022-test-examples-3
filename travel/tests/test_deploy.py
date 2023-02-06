# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
import logging
import os

import pytest
from django.http import QueryDict
from django.test import RequestFactory
from django.utils.encoding import force_text
from hamcrest import has_entries, assert_that, has_entry

from travel.rasp.library.python.common23.logging.deploy import DeployJsonFormatter

locals = {}
exec('''
import sys
try:
    a = 1
    raise Exception('bla')
except Exception:
    exc_info = sys.exc_info()
''', {}, locals)
exc_info = locals['exc_info']


SIMPLE_STACKTRACE = '''\
Traceback (most recent call last):
  File "<string>", line 5, in <module>
Exception: bla'''


class TestDeployJsonFormatter(object):
    @pytest.mark.parametrize('extra_base_fields, tag, extra_context, exc_info, result', [
        ((), None, None, None, has_entries({
            'message': 'msg',
            'loggerName': 'mog.log',
            'levelStr': 'DEBUG',
            'level': 10,
            '@fields': has_entry('std', has_entries({
                'process': os.getpid(),
                'file': has_entries({'funcName': None})
            }))
        })),
        ((), None, None, exc_info, has_entries({'message': 'msg', 'stackTrace': SIMPLE_STACKTRACE})),
        (('pathname',), None, (), None, has_entries({
            'message': 'msg',
            'loggerName': 'mog.log',
            '@fields': has_entry('std', has_entries({
                'process': os.getpid(),
                'file': has_entries({
                    'funcName': None,
                    'pathname': 'aa/ss',
                })
            }))
        })),
        ((), None, {'ctx_key': 25}, None, has_entries({
            'message': 'msg',
            'loggerName': 'mog.log',
            '@fields': has_entries({
                'std': has_entries({
                    'processName': 'MainProcess'
                }),
                'context': has_entry('ctx_key', 25)
            }),
        })),
        ((), 'my_tag', None, None, has_entries({
            'message': 'msg',
            'loggerName': 'mog.log',
            '@fields': has_entries({
                'std': has_entries({
                    'module': 'ss'
                }),
                'tag': 'my_tag'
            }),
        }))
    ])
    def test_simple(self, extra_base_fields, tag, extra_context, exc_info, result):
        formatter = DeployJsonFormatter(extra_base_fields=extra_base_fields, tag=tag, extra_context=extra_context)
        lr = logging.LogRecord('mog.log', logging.DEBUG, 'aa/ss', 20,
                               'msg', (), exc_info, func=None)
        parsed = json.loads(formatter.format(lr))
        assert_that(parsed, result)

    def test_context(self):
        formatter = DeployJsonFormatter()
        lr = logging.LogRecord('mog.log', logging.DEBUG, 'aa/ss', 20,
                               'msg', (), None, func=None)
        lr.context = {'ctx_key': 40}
        parsed = json.loads(formatter.format(lr))
        assert_that(parsed, has_entry('@fields', has_entry('context', has_entry('ctx_key', 40))))

    @pytest.mark.parametrize('ctx', [
        'aa', 10, (1,),
    ])
    def test_bad_context(self, ctx):
        formatter = DeployJsonFormatter()
        lr = logging.LogRecord('mog.log', logging.DEBUG, 'aa/ss', 20,
                               'msg', (), None, func=None)
        lr.context = ctx
        parsed = json.loads(formatter.format(lr))
        assert_that(parsed, has_entry('@fields', has_entry('context', has_entries({
            'context_value': repr(ctx),
            'bad_context': True
        }))))


class TestDeployExceptionFormatter(object):
    def test_exception_simple_format(self):
        formatter = DeployJsonFormatter()
        lr = logging.LogRecord('mog.log', logging.DEBUG, 'aa/ss', 20,
                               'msg', (), exc_info, func=None)
        parsed = json.loads(formatter.format(lr))
        assert parsed['stackTrace'] == SIMPLE_STACKTRACE

    @pytest.mark.parametrize('wsgi_request, context_entries', [
        (RequestFactory().get('/aaa/bbb', {'a': 10}), {
            'method': 'GET',
            'path': '/aaa/bbb',
            'query': force_text(QueryDict('a=10')),
            'post': force_text(QueryDict())
        }),
        (RequestFactory().post('/aaa/bbb', {'a': 10}), {
            'method': 'POST',
            'path': '/aaa/bbb',
            'query': force_text(QueryDict()),
            'post': force_text(QueryDict('a=10'))
        }),
        (RequestFactory().post('/aaa/bbb', '{"a": 101}', content_type='application/json'), {
            'method': 'POST',
            'path': '/aaa/bbb',
            'query': force_text(QueryDict()),
            'post': '{"a": 101}'
        }),
    ])
    def test_request_context(self, wsgi_request, context_entries):
        formatter = DeployJsonFormatter(add_request_info=True)
        lr = logging.LogRecord('mog.log', logging.DEBUG, 'aa/ss', 20,
                               'msg', (), exc_info, func=None)
        lr.request = wsgi_request
        parsed = json.loads(formatter.format(lr))
        assert_that(parsed['@fields']['context'], has_entries(context_entries))
