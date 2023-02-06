# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import logging
import os
import sys

from hamcrest import assert_that, has_entries, anything, ends_with

from common.tester.matchers import has_json
from common.tester.utils.datetime import replace_now
from common.tester.utils.logging import _create_log_record
from travel.rasp.admin.lib.logs import (
    CollectorHandler, stdstream_file_capturing, get_script_log_context, JsonFormatterContextKeys
)


def test_stdstream_file_capturing(tmpdir):
    capture = tmpdir.join('capture.txt')
    stdout = tmpdir.join('stdout.txt')
    stderr = tmpdir.join('stderr.txt')

    pid = os.fork()
    if not pid:
        sys.stdout.flush()
        sys.stderr.flush()

        os.close(1)
        os.close(2)
        with stdout.open('wt') as stdout_file, stderr.open('wt') as stderr_file, capture.open('wt') as capture_file:
            os.dup2(stdout_file.fileno(), 1)
            os.dup2(stderr_file.fileno(), 2)

            os.write(1, 'before stdout\n')
            os.write(2, 'before stderr\n')

            with stdstream_file_capturing(capture_file):
                os.write(1, 'ssss ')
                os.write(2, 'yyyy')

            os.write(1, 'after stdout')
            os.write(2, 'after stderr')

        os._exit(0)

    os.waitpid(pid, 0)

    assert capture.open().read() == 'ssss yyyy'
    assert stdout.open().read() == 'before stdout\nafter stdout'
    assert stderr.open().read() == 'before stderr\nafter stderr'


def test_collector_handler():
    handler = CollectorHandler()
    log = logging.Logger('test_CollectorHandler')
    log.addHandler(handler)
    log.info(u'текст')
    log.info('байты')

    assert handler.get_collected() == u'текст\nбайты\n'


@replace_now('2020-03-05 14:13:12')
def test_get_script_log_context():
    context = get_script_log_context()
    assert_that(context, has_entries(
        start_script_name=ends_with('travel/rasp/admin/tests/lib/test_logs.py'),
        start_script_dt='2020-03-05 14:13:12',
        start_caller_function='test_get_script_log_context',
        start_script_id=anything(),
        parent_pid=anything()
    ))

    context = get_script_log_context(script_name='some_script')
    assert_that(context, has_entries(
        start_script_name='some_script',
    ))


def test_formatter_context_keys():
    log_record = _create_log_record(msg='123')
    log_record.process = 4242
    log_record.my_context = {
        'start_script_name': 'script_name',
        'start_script_dt': '2020-03-05 14:13:12',
        'start_caller_function': 'func',
        'start_script_id': 'script_id',
        'additional_field': 'field_value',
        'parent_pid': 666
    }
    result = JsonFormatterContextKeys(context_field='my_context', keys_to_log={'levelname', 'process', 'my_context', 'name'}).format(log_record)
    assert_that(result, has_json(has_entries(
        name='name',
        levelname='DEBUG',
        message='123',
        pid='4242',
        parent_pid='666',
        start_script_name='script_name',
        start_script_dt='2020-03-05 14:13:12',
        start_caller_function='func',
        start_script_id='script_id',
        context=has_entries(
            additional_field='field_value',
        )
    )))
