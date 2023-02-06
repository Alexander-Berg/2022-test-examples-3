# -*- coding: utf-8 -*-
import mock
import urlparse

from contextlib import contextmanager

import re

from requests import Request
from requests import Response
from sqlalchemy import text

from mpfs.common.util import to_json

DEVNULL_EMAIL = 'devnull@yandex.ru'


def without_querystring(url):
    """Убирает из URL query string."""
    return url.split("?", 1)[0]


class URLHelper(object):
    def __init__(self, url):
        self.url = url

    @property
    def query_string(self):
        parsed = urlparse.urlparse(self.url)
        return urlparse.parse_qs(parsed.query)

    @property
    def path(self):
        return urlparse.urlparse(self.url).path


@contextmanager
def ignored(*exceptions):
    try:
        yield
    except exceptions:
        pass


class StringWithSuppressedFormating(str):
    def __mod__(self, other):
        """Eat everything from formatting

        Example:
              >>> s = StringWithSuppressedFormating('')
              >>> s % (123, 555)
              ''
              >>>
        """
        return self


@contextmanager
def catch_return_values(cls, method_name):
    """Запатчить метод и получить результаты его вызовов.

    :type cls: type
    :type method_name: str
    :rtype: tuple[:class:`mock.MagicMock`, list]

    :Example:
    >>> class A(object):
    ...     def f(self):
    ...         return 123
    >>> with catch_return_values(A, 'f') as (mocked, return_values):
    >>>     assert A().f() == 123
    >>>     assert mocked.call_count == 1
    >>>     assert return_values[0] == 123

    """

    orig = getattr(cls, method_name)
    return_values = []

    def wrapper(*args, **kwargs):
        result = orig(*args, **kwargs)
        return_values.append(result)
        return result

    with mock.patch.object(cls, method_name, wraps=wrapper) as mocked:
        yield mocked, return_values


def create_table(conn, create_table_query):
    conn.execute(create_table_query)


def drop_table(conn, drop_table_query):
    conn.execute(drop_table_query)


def create_table_and_insert_values(conn, create_table_query, table_name):
    create_table(conn, create_table_query)
    for i in xrange(20):
        conn.execute(text('INSERT INTO %s (uid,name) VALUES (:val0,:val1)' % table_name), val0=i, val1='test%d' % i)


def check_task_called(original_apply_async, task_name):
    def fake_apply_async(self, *args, **kwargs):
        if self.name == task_name:
            fake_apply_async.called = True
            fake_apply_async.call_count += 1
            fake_apply_async.call_args = kwargs['kwargs']
        return original_apply_async(self, *args, **kwargs)

    fake_apply_async.called = False
    fake_apply_async.call_count = 0
    fake_apply_async.call_args = None

    return fake_apply_async


def disk_path_to_area_path(disk_path):
    pattern = re.compile('^/(?P<area>[^/]+)(?P<path>.+)?$')
    result = pattern.match(disk_path)
    if result is None:
        raise RuntimeError('Wrong path format: %s' % disk_path)
    area = result.group('area')
    path = result.group('path')
    if path is None:
        # если путь - корень, то дописываем слэш
        path = '/'
    return '%s:%s' % (area, path)


def filter_dict(d, fields):
    filtered = {}
    for k, v in d.iteritems():
        if k in fields:
            filtered[k] = v
    return filtered


def construct_requests_resp(status=200, content='',
                            request_method='GET', request_url='http://checkform2-test.n.yandex-team.ru',
                            request_params=None, request_data=None, request_headers=None, request_body=None):
    request = Request(request_method, request_url,
                      params=request_params, data=request_data, headers=request_headers)
    request.body = request_body

    resp = Response()
    resp.status_code = status
    resp.request = request
    if isinstance(content, dict):
        content = to_json(content)
    resp._content = content

    return resp
