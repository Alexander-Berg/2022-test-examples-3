# coding: utf-8

'''
Resonance tester entrypoint.
'''

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import argparse
import logging
import os
import sys
import yaml

from threading import RLock

from google.protobuf import json_format, text_format

from search.martylib.core.date_utils import set_timezone

from search.resonance.tester.core import (
    BackendInfo, EventLog, ExecuteContext, InitContext, RemoteContext, UnistatWatcher,
)
from search.resonance.tester.proto.result_pb2 import TResult
from search.resonance.tester.proto.test_pb2 import TResonanceTest
from search.resonance.tester.tests import TESTS_TABLE


LOGGER = logging.getLogger('resonance.tester')
logging.getLogger('martylib.trace').propagate = False


def parse_args():
    parser = argparse.ArgumentParser('Resonance tester')
    parser.add_argument('-c', '--config', required=True, dest='config_path', help='Tests config path')
    parser.add_argument('-b', '--backends', required=True, dest='backends_path', help='Backends and upstreams config path')
    parser.add_argument('-l', '--loadgen', required=True, dest='loadgen_path', help='Loadgen binary path')
    parser.add_argument('-o', required=True, dest='output_file', help='Output yaml file')
    parser.add_argument('--fetcher', required=True, dest='fetcher_path', help='Fetcher path')
    return parser.parse_args()


def prepare_test(f, tests_root_dir):
    test_conf = text_format.Parse(f.read(), TResonanceTest())

    test = None
    for field, field_value in test_conf.ListFields():
        if field.name in TESTS_TABLE:
            context = InitContext(tests_root_dir, field_value)
            test = TESTS_TABLE[field.name]()
            test.prepare(context)
            break
    if not test:
        LOGGER.fatal('test %s config not found', test_conf.Name)
        raise ValueError()
    return (test_conf, test)


def prepare_remote(f):
    backends_config = yaml.load(f)
    upstreams = tuple(backends_config['upstreams'])
    backends = tuple(
        BackendInfo(
            admin_host=backend['admin'],
            backend_id=backend['name'],
            backend_host=backend['backend'],
        )
        for backend in backends_config.get('backends', ())
    )
    return upstreams, backends


def configure_loggers():
    formatter = logging.Formatter('[%(levelname)s %(name)s %(asctime)s] %(message)s')
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(formatter)
    logging.basicConfig(level=logging.INFO, handlers=(handler,))


def main():
    args = parse_args()

    with open(args.config_path) as f:
        test_conf, test = prepare_test(f, os.path.dirname(args.config_path))
    with open(args.backends_path) as f:
        remote_context = RemoteContext(*prepare_remote(f))

    result = TResult()
    result.Test = test_conf.Name
    result_lock = RLock()
    unistat_watcher = UnistatWatcher(args.fetcher_path, remote_context.backends, result, result_lock)
    event_log = EventLog(result.RootEvent, result_lock)

    execute_context = ExecuteContext(args.loadgen_path)

    test.validate_context(execute_context, remote_context)

    LOGGER.info('start test %s', test_conf.Name)

    unistat_watcher.start()
    test.process(execute_context, remote_context, event_log)
    event_log.end()
    unistat_watcher.stop()

    LOGGER.info('test %s execution complete', test_conf.Name)

    with open(args.output_file, 'w') as f:
        f.write(json_format.MessageToJson(result))


if __name__ == '__main__':
    set_timezone('Europe/Moscow')
    configure_loggers()
    main()
