# coding: utf-8


from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import argparse
import contextlib
import glob
import logging
import os
import time

from search.martylib.config_utils import run_ipython
from search.martylib.core.logging_utils import configure_binlog
from search.martylib.trace import trace


def main():
    print(logging.getLevelName(os.environ.get('LOGGING_LEVEL', 'INFO')))

    assert not os.environ.get('IN_MEMORY_BINLOG', 0)
    assert os.environ.get('MODE', 'PROD') == 'PROD'

    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--ipython', action='store_true')

    args = parser.parse_args()
    if args.ipython:
        run_ipython()
        return

    for log_file in glob.glob('test.*'):
        os.remove(log_file)

    for log_file in glob.glob('test-*'):
        os.remove(log_file)

    configure_binlog(
        'test',
        loggers=('test.trash',),
        indexed_fields=('index_1',),
        file_size_mb=4,
        backup_count=4,
        dedicated_backup_count=1,
    )

    logger = logging.getLogger('test.trash')

    with trace('foo', bar=1, _level=logging.CRITICAL) as tm:
        logger.info('some log')

        with trace('bar', bar=2):
            logger.info('info log')

        logger.info('some other log')
        logger.info('some other log')
        logger.info('some other log')
        logger.info('some other log')
        logger.info('some other log')

        tm.last_layer['new_bar'] = 5

        with trace('bar', bar=3):
            logger.info('info log3')

    with trace('foo_debug', bar=2, _level=logging.DEBUG):
        logger.debug('some debug log')
        logger.info('some info log')

    with contextlib.suppress(StopAsyncIteration):
        with trace('foo_debug_1', bar=4, _level=logging.DEBUG):
            logger.debug('something')

            with trace('foo_debug_2', bar=5, _level=logging.DEBUG):
                logger.exception('exception will be here!')
                raise StopAsyncIteration

    with trace('hello', bar=2):
        with trace('there', x='y', _level=logging.DEBUG) as tm:
            logger.debug('some debug log')

            tm.change_trace_level(logging.CRITICAL)
            logger.critical('some critical log')

    with trace('oh'):
        with trace('noo', _level=logging.DEBUG):
            logger.info('YES!')

    with trace('index_without_error', index_1='kek'):
        logger.info('no error here')

    with contextlib.suppress(Exception):
        with trace('index_with_some_error', index_1='kek'):
            logger.info('error will be here')
            raise Exception('Error!')

    with contextlib.suppress(Exception):
        with trace('index_with_specific_error', index_1='kek'):
            logger.info('specific error 1')
            raise StopAsyncIteration('Lol')

    with contextlib.suppress(Exception):
        with trace('index_with_specific_error', index_1='kek'):
            logger.info('specific error 2')
            raise StopAsyncIteration('Kek')

    with trace('long_trace'):
        time.sleep(3)

    with trace('not_so_long_trace'):
        time.sleep(1)

    with trace('trace_with_logger_exception'):
        # noinspection PyBroadException
        try:
            raise StopAsyncIteration('lol kek cheburek')
        except Exception:
            logger.exception('got exception!')

    time.sleep(1)
