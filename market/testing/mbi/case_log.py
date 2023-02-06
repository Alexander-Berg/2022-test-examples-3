#!/usr/bin/env python
# coding=utf-8

""" Протокол теста

Цели:
    1. Приложить в тикет лог, как проводился тест
    2. Дебаг теста разработчиком
    3. Формулирование тикетов для исправления найденных тестом багов

Фичи:
    √ Под каждый тест пишется лог
    √ Логи обнуляется для каждого теста
    Пишется общий лог (для релизного тикета, например) и заливается на paste
    Одиночный тест сразу копируется в буфер обмена/заливается на paste
    Сразу форматирование под wiki
    √ Таймстемпы
    curl-формат подачи запроса
    √ Пролог для каждого теста
    √ Попытка детектить xml/json и попытка форматировать. Фолбек на RAW-формат
"""


import unittest
import datetime
import sys
import os.path
import os
import json
from lxml import etree


class CaseLogger:
    logger = None

    @classmethod
    def get(cls):
        if cls.logger is None:
            cls.logger = cls()
        return cls.logger

    def __init__(self, file_name=None):
        self.file_name = file_name if file_name else _generate_test_log_filename()
        self.file = open(self.file_name, 'w')

    def log_request(self, r, content):
        now = datetime.datetime.now()
        line = _get_current_test_source_line()
        self.file.write('Request time: {}\n'.format(now))
        _write_request_explanation(r, content, self.file)
        self.file.write('\n')
        self.file.write('-' * 80)
        self.file.write('\n')
        self.file.write('\n')

    def log(self, message):
        self.file.write(message)
        self.file.write('\n')


def _generate_test_log_filename(fn=None):
    if fn is None:
        fn = sys.argv[0]
    exe_fn = fn if fn else 'test_sometest.py'
    return '{}.log'.format(os.path.splitext(os.path.split(exe_fn)[1])[0])


def _get_current_test_source_line():
    return '(no source line)'


def _write_request_explanation(r, content, f):
    # TODO: see https://stackoverflow.com/questions/20658572/python-requests-print-entire-http-request-raw
    f.write('{} {}\n'.format(r.request.method, r.request.url))
    f.write('Request headers:\n')
    for hdr, value in sorted(r.request.headers.items()):
        f.write('    {}: {}\n'.format(hdr, value))
    if r.request.body:
        f.write('Request body:\n')
        f.write(r.request.body)
        if not r.request.body.endswith('\n'):
            f.write('\n')
    f.write('Response Status code: {}\n'.format(r.status_code))
    f.write('Response headers:\n')
    for hdr, value in sorted(r.headers.items()):
        f.write('    {}: {}\n'.format(hdr, value))
    f.write('Response body:\n')
    try:
        f.write(maybe_format(content))
    except:
        f.write('UnicodeEncodeError, cannot write response body')

    f.write('\n')


def maybe_format(raw):
    try:
        d = json.loads(raw)
        return json.dumps(d, ensure_ascii=False, indent=4)
    except:
        try:
            dom = etree.fromstring(raw)
            return etree.tostring(dom, pretty_print=True, encoding='utf-8', xml_declaration=True)
        except:
            return raw


class T(unittest.TestCase):
    def test_generate_test_log_filename(self):
        self.assertEqual(_generate_test_log_filename('test_mbi_21352_general_cutoff.py'), 'test_mbi_21352_general_cutoff.log')
        self.assertEqual(_generate_test_log_filename('sdfsdf/sdfsdf/test_mbi_21352_general_cutoff.py'), 'test_mbi_21352_general_cutoff.log')
        self.assertEqual(_generate_test_log_filename(''), 'test_sometest.log')

    def test_get_current_test_source_line(self):
        assert _get_current_test_source_line() is not None

    def test_maybe_format(self):
        s = maybe_format('{"a": [1,2,3]}')
        self.assertTrue('  "a": [\n' in s)
        s = maybe_format('<?xml version="1.0" encoding="UTF-8"?><data servant="market-payment" version="0" host="Хвост" actions="[preCampaignCreate]" executing-time="[572]"><pre-campaign><agency-id>0</agency-id><campaign-id>1086000572</campaign-id><datasource-id>44400170</datasource-id><manager-id>-2</manager-id><owner-id>505977008</owner-id><owner-login/><url>fantamp1497027888.98.yandex.ru</url></pre-campaign></data>')
        self.assertTrue('  <pre-campaign>\n' in s)
        s = maybe_format('__s dfadfds {"a": [1,2,3]}')
        self.assertEqual(s, '__s dfadfds {"a": [1,2,3]}')



if __name__ == '__main__':
    unittest.main()
