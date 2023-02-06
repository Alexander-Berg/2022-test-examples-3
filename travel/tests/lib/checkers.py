# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from copy import copy
import json
import logging
import os

from pytest_localserver.http import WSGIServer
from werkzeug.wrappers import Request, Response
from werkzeug.utils import redirect
import toml
import yatest.common

from kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api import ConsumerMessageType
from kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api import PQStreamingAPI
from kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api import ConsumerConfigurator
from kikimr.public.tools.lib.pq_util import create_full_topic_name, get_lb_client, translate_new_topic_name
from kikimr.public.tools.lib.pq_util import DEFAULT_TOPIC_PARTITIONS, DEFAULT_DC_NAME
from travel.cpa.lib.common import json_bytes_to_unicode


CONVERTERS = {
    None: lambda x: x,
}


class LocalChecker(object):
    """
    This "checker" exists to convert arcadia paths to absolute paths, because the yatest.common.source_path cannot be
    used outside tests runtime
    """
    def __init__(self, checker_name, test_data, task_checker):
        relative_paths = test_data['arc_paths']
        for path in relative_paths:
            task_checker.arg_patches[path] = yatest.common.source_path(relative_paths[path])

    def prepare(self):
        pass

    def check(self):
        pass

    def stop(self):
        pass


class HttpChecker(object):
    def __init__(self, checker_name, test_data, task_checker):
        self.test_data_dir = task_checker.test_data_dir
        self.test_data = test_data
        self.task_checker = task_checker
        self.mime_type = self.test_data['mime_type']
        self.responses = list()
        self.server = WSGIServer(application=self.app)
        self.server.start()
        task_checker.arg_patches[checker_name + '_server_url'] = self.server.url
        self.read_test_data()

    def prepare(self):
        pass

    def check(self):
        pass

    def stop(self):
        self.server.stop()

    @Request.application
    def app(self, request):
        return self.get_response(request)

    def read_test_data(self):
        for response in self.test_data['responses']:
            logging.info('Expected response: %r', response)
            data_file = response.get('data_file')
            if data_file is None:
                self.responses.append(response)
                continue
            fn = os.path.join(self.test_data_dir, data_file)
            with open(fn) as f:
                data = f.read()
            data = data % self.task_checker.arg_patches
            response['data'] = data
            self.responses.append(response)
        assert len(self.responses) > 0

    def get_response(self, request):
        logging.info(request)
        for response in self.responses:
            method = response.get('method', 'GET')
            if request.method != method:
                logging.debug('Unmatched method. Expected %s actual %s', request.method, method)
                continue

            expected_args = response.get('args', {}).items()
            actual_args = request.args.items()
            unmatched_args = set(expected_args) - set(actual_args)

            expected_form = set()
            actual_form = set()
            if method == 'POST':
                expected_form = response.get('form', {}).items()
                actual_form = {(key.decode('utf8'), value) for key, value in request.form.items()}
            unmatched_form = set(expected_form) - actual_form

            expected_headers = response.get('headers', {}).items()
            actual_headers = request.headers
            unmatched_headers = set(expected_headers) - set(actual_headers)
            response_path = response.get('path', '/')
            if response_path != request.path:
                logging.debug('Unmatched path. Expected %r actual %r', request.path, response_path)
                continue
            if unmatched_args:
                logging.debug('Unmatched args. Expected %r actual %r', expected_args, actual_args)
                continue
            if unmatched_form:
                logging.debug(
                    'Unmatched form. Expected %r, actual %r, diff %r',
                    expected_form,
                    actual_form,
                    unmatched_form
                )
                continue
            if unmatched_headers:
                logging.debug('Unmatched headers. Expected %r actual %r', expected_headers, actual_headers)
                continue
            redirect_url = response.get('redirect_url')
            if redirect_url is not None:
                return redirect(redirect_url)
            return Response(response['data'], mimetype=self.mime_type)
        raise Exception('No matched data for request {}'.format(request))


class LogBrokerChecker(object):
    TIMEOUT_SECONDS = 5

    def __init__(self, checker_name, test_data, task_checker):
        self.test_data = test_data
        with open('ydb_endpoint.txt') as f:
            self.port = f.read().strip()
        self.topic = test_data['topic']
        full_topic_name = create_full_topic_name(DEFAULT_DC_NAME, translate_new_topic_name(self.topic))
        self.lb_control_plane_client = get_lb_client('localhost', self.port)
        self.lb_control_plane_client.create_topic(full_topic_name, DEFAULT_TOPIC_PARTITIONS)
        task_checker.arg_patches['lb_port'] = self.port
        task_checker.arg_patches['lb_topic'] = self.topic

    def prepare(self):
        pass

    def check(self):
        exp_data = self.test_data.get('expected')
        if exp_data is None:
            raise Exception('Expected data for lb checker not specified')
        fields_to_skip = self.test_data.get('fields_to_skip', [])
        fields_to_skip.extend(['hash', '_timestamp', 'updated_at', 'last_seen'])
        data = list()
        api = PQStreamingAPI('localhost', self.port)
        configurator = ConsumerConfigurator(self.topic, 'test-client')
        try:
            f = api.start()
            f.result(self.TIMEOUT_SECONDS)

            reader = api.create_consumer(configurator)

            response = reader.start().result(timeout=self.TIMEOUT_SECONDS)
            if not response.HasField('init'):
                raise Exception('Failed to initialize logbroker connection')
            result = reader.next_event().result(timeout=10)
            assert result.type == ConsumerMessageType.MSG_DATA
            extracted_messages = self._process_read_result(result.message)
            data.extend(extracted_messages)
        finally:
            api.stop()
        ignore_unknown_fields = True
        TaskChecker.compare_results('LB queue', data, exp_data, ignore_unknown_fields, fields_to_skip)

    def stop(self):
        pass

    @staticmethod
    def _process_read_result(consumer_message):
        ret = []
        for batch in consumer_message.data.message_batch:
            for message in batch.message:
                ret.append(json.loads(message.data))
        return ret


class YtChecker(object):
    def __init__(self, checker_name, test_data, task_checker):
        self.yt_client = task_checker.yt_client
        self.yt_proxy = task_checker.yt_proxy
        self.test_data = test_data
        self.test_root = '//home/tests'
        task_checker.arg_patches['yt_proxy'] = self.yt_proxy

    def prepare(self):
        self.yt_client.remove(self.test_root, recursive=True, force=True)
        input_data = self.test_data.get('input', {})
        for table, info in input_data.items():
            path = info.get('path')
            if path is None:
                raise Exception('No path specified for input node {!r}'.format(table))
            link_to = info.get('link_to')
            if link_to is not None:
                self.yt_client.link(link_to, path, recursive=True, ignore_existing=True)
                continue
            converter_name = info.get('converter')
            converter = CONVERTERS.get(converter_name)
            if converter is None:
                raise ValueError('No such converter: {}'.format(converter_name))
            data = info.get('data')
            if data is None:
                raise Exception('No data specified for input table {!r}'.format(table))
            data = (converter(row) for row in data)
            self.yt_client.create('table', path, recursive=True)
            self.yt_client.write_table(path, data, format='json')
            sort_by = info.get('sort_by')
            if sort_by is not None:
                self.yt_client.run_sort(path, destination_table=path, sort_by=sort_by)

    def check(self):
        output_data = self.test_data.get('output', {})
        for table, info in output_data.items():
            source = 'table {}'.format(table)
            path = info.get('path')
            if path is None:
                raise Exception('No path specified for output table {!r}'.format(table))
            exp_data = info.get('data')
            if exp_data is None:
                raise Exception('No data specified for output table {!r}'.format(table))
            ignore_unknown_fields = info.get('ignore_unknown_fields')
            fields_to_skip = info.get('fields_to_skip', [])
            assert self.yt_client.exists(path)
            data = self.yt_client.read_table(path, format='json')
            data = [json_bytes_to_unicode(row) for row in data]
            TaskChecker.compare_results(source, data, exp_data, ignore_unknown_fields, fields_to_skip)

    def stop(self):
        pass


CHECKERS = {
    'http': HttpChecker,
    'lb': LogBrokerChecker,
    'yt': YtChecker,
    'local': LocalChecker
}


class TaskChecker(object):
    def __init__(self, test_fn, yt_client=None, yt_proxy=None):
        fn = yatest.common.source_path(test_fn)
        self.test_data_dir = os.path.dirname(fn)
        self.yt_client = yt_client
        self.yt_proxy = yt_proxy
        with open(fn) as f:
            self.test_data = toml.load(f)
        self.arg_patches = dict()
        self.checkers = list()
        for checker_name in self.test_data['checkers']:
            checker_test_data = self.test_data[checker_name]
            check_type = checker_test_data.get('check_type', checker_name)
            checker_cls = CHECKERS[check_type]
            self.checkers.append(checker_cls(checker_name, checker_test_data, self))
        try:
            self.prepare()
            self.run()
            self.check()
        finally:
            self.stop()

    def prepare(self):
        for checker in self.checkers:
            checker.prepare()

    def run(self):
        args = [yatest.common.binary_path(self.test_data['bin_path'])]
        args.append('--fail-fast')
        args.extend(copy(self.test_data['args']))
        args = self.patched_args(args, self.arg_patches)
        yatest.common.execute(args, wait=True)

    def check(self):
        for checker in self.checkers:
            checker.check()

    def stop(self):
        for checker in self.checkers:
            checker.stop()

    @staticmethod
    def patched_args(args, patches):
        return [arg % patches for arg in args]

    @staticmethod
    def compare_results(source, data, exp_data, ignore_unknown_fields, fields_to_skip=None):
        if fields_to_skip is None:
            fields_to_skip = list()
        fields_to_skip = set(fields_to_skip)
        for index, (expected, actual) in enumerate(zip(exp_data, data)):
            diff = TaskChecker.get_row_diff(expected, actual, ignore_unknown_fields, fields_to_skip)
            if not diff:
                continue
            msg = 'Test results for {!r} differ from expected\nfirst difference at index {}\n{}'.format(
                source, index, diff
            )
            raise Exception(msg)
        len_expected = len(exp_data)
        len_actual = len(data)
        if len_expected > len_actual:
            diff = '\n\n'.join(TaskChecker.get_rec_text(rec) for rec in exp_data[len_actual:])
            msg = 'More expected rows than actual for {!r}\nextra rows from index {}\n{}'.format(
                source, len_actual, diff
            )
            raise Exception(msg)
        if len_actual > len_expected:
            diff = '\n\n'.join(TaskChecker.get_rec_text(rec) for rec in data[len_expected:])
            msg = 'More actual rows than expected for {!r}\nextra rows from index {}\n{}'.format(
                source, len_expected, diff
            )
            raise Exception(msg)

    @staticmethod
    def get_row_diff(expected, actual, ignore_unknown_fields, fields_to_skip):
        default_expected = None if ignore_unknown_fields else 'NO_DATA'
        diff = list()
        expected_keys = set(expected.keys())
        actual_keys = set(actual.keys())
        for key in expected_keys | actual_keys:
            if key in fields_to_skip:
                continue
            expected_value = expected.get(key, default_expected)
            actual_value = actual.get(key, 'NO_DATA')
            if expected_value == actual_value:
                continue
            line = '{:25} {!r:25} {!r:25}'.format(key, expected_value, actual_value)
            diff.append(line)
        return '\n'.join(diff)

    @staticmethod
    def get_rec_text(rec):
        return '\n'.join('{:25} {!r:25}'.format(key, value) for key, value in rec.items())
