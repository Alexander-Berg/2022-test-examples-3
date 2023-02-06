# -*- coding: utf-8 -*-

from mpfs.engine.http.client import format_log_string


def test_format_log_string():
    method = u'POST'
    url = b'http://lenta-loader01h.dst.yandex.net:21890/api/save_file_from_public'
    data = b'Строка с русскими символами'
    cookie = {}
    code = 500
    answer = None
    time_ = 0.00211596488953
    log_data = True
    logged_data = None
    headers = {'X-Request-Attempt': '0'}
    # не должно быть UnicodeDecodeError
    format_log_string(
        method, url, data,
        cookie, code, answer,
        time_, log_data, logged_data, headers
    )
