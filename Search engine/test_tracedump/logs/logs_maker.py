# coding: utf-8


from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import argparse
import collections
import glob
import itertools
import logging
import os
import random
import string
import time

from search.martylib.config_utils import run_ipython
from search.martylib.core.logging_utils import configure_binlog
from search.martylib.core.logging_utils.binlog import BinlogManager
from search.martylib.core.logging_utils.writer import GlobalAsyncWriter
from search.martylib.mode import Mode, get_mode
from search.martylib.proto.structures import log_pb2
from search.martylib.trace import trace
from search.martylib.trace.storage import TraceStorage


class BaseLogMaker(object):
    logger = logging.getLogger('test.trash')

    SMALL_LOG_LEN = 10
    MEDIUM_LOG_LEN = 100
    LARGE_LOG_LEN = 1000

    def __init__(self, name: str, file_size_mb: int = 10, backup_count: int = 5):
        self.name = name
        self.file_size_mb = file_size_mb
        self.backup_count = backup_count

        configure_binlog(
            base_filename=name,
            loggers=('test.trash', ),
            indexed_fields=('index_1', ),
        )

        self.manager = BinlogManager(
            base_filename=name,
            directory='.',
            indexed_fields=('index_1',),
            max_backups=backup_count,
            max_file_size_mb=file_size_mb,
            writer_class=GlobalAsyncWriter,
        )

        self.log_storage = TraceStorage(open(self.manager.get_path(), 'ab'))
        self.index_storage = {
            field: TraceStorage(open(self.manager.get_path(indexed_field=field), 'a'))
            for field in self.manager.indexed_fields
        }

        with trace(self.generate_random_string(30), index_1='startstart'):
            with trace(self.generate_random_string(30), index_1='startstart'):
                self.logger.info(self.generate_random_string(self.SMALL_LOG_LEN))
                self.logger.info(self.generate_random_string(self.MEDIUM_LOG_LEN))
                self.logger.info(self.generate_random_string(self.LARGE_LOG_LEN))

        time.sleep(0.3)  # due to manager works in another thread

        (
            self.shallow_trace_size,
            _,
            self.small_log_size,
            self.medium_log_size,
            self.large_log_size,
            self.with_parent_trace_size,
            self.end_trace_size,
        ) = map(
            lambda x: len(self.manager.encode_record(x)),
            (self.log_storage[i] for i in range(7))
        )

        print(f'Trace size:\t\t{self.shallow_trace_size}')
        print(f'Parent trace size:\t{self.with_parent_trace_size}')
        print(f'Small log size:\t\t{self.small_log_size}')
        print(f'Med log size:\t\t{self.medium_log_size}')
        print(f'Large log size:\t\t{self.large_log_size}')
        print(f'End trace size:\t\t{self.end_trace_size}')
        self.log_storage.clear()
        for writer in self.index_storage.values():
            writer.clear()

    @staticmethod
    def generate_random_string(size=10):
        return ''.join(random.choice(string.ascii_letters) for _ in range(size))

    @property
    def max_size(self):
        return self.file_size_mb * self.backup_count * 1000 * 1000 - 1000

    def get_storage_size(self):
        return sum(map(lambda x: len(self.manager.encode_record(x)), self.log_storage))

    def get_written_size(self):
        return sum(os.path.getsize(log) for log in itertools.chain(glob.glob(f'{self.name}.blog.*'), (f'{self.name}.blog', )))

    def flush_log_storage(self):
        written_size = 0
        while self.log_storage:
            item = self.log_storage.popleft()
            if isinstance(item, log_pb2.LogRecord):
                item.func_name = '__init__'
                item.lineno = 66
            self.manager << item
            written_size += len(self.manager.encode_record(item))
        return written_size

    def run(self):
        max_size = self.max_size

        trace_count = random.randint(10, min(max_size // (self.shallow_trace_size + self.end_trace_size), 1000))
        max_size -= trace_count * (self.shallow_trace_size + self.end_trace_size)

        with_parent_trace_count = random.randint(10, min(max_size // (self.with_parent_trace_size + self.end_trace_size), 1000))
        max_size -= with_parent_trace_count * (self.with_parent_trace_size + self.end_trace_size)

        small_log_count = random.randint(0, max_size // self.small_log_size)
        max_size -= small_log_count * self.small_log_size

        medium_log_count = random.randint(0, max_size // self.medium_log_size)
        max_size -= medium_log_count * self.medium_log_size

        large_log_count = int(max_size // self.large_log_size)
        max_size -= large_log_count * self.large_log_size

        written_size = 0
        expected_size = self.max_size - max_size

        print(trace_count, with_parent_trace_count, small_log_count, medium_log_count, large_log_count)

        distribution = collections.defaultdict(lambda: [0, 0, 0, 0])

        for _ in range(with_parent_trace_count):
            distribution[random.randint(0, trace_count - 1)][0] += 1

        for _ in range(small_log_count):
            distribution[random.randint(0, trace_count - 1)][1] += 1

        for _ in range(medium_log_count):
            distribution[random.randint(0, trace_count - 1)][2] += 1

        for _ in range(large_log_count):
            distribution[random.randint(0, trace_count - 1)][3] += 1

        for i in range(trace_count):
            with trace(self.generate_random_string(30), index_1=f'trace{trace_count:05d}'):

                trace_count -= 1

                local_parent, local_small, local_medium, local_large = distribution[i]

                s = local_parent + local_small + local_medium + local_large
                while s:
                    action = random.randint(0, 3)

                    if action == 0:
                        if local_parent <= 0:
                            continue
                        local_parent -= 1

                        with trace(self.generate_random_string(30), index_1=f'withp{with_parent_trace_count:05d}'):
                            with_parent_trace_count -= 1

                    if action == 1:
                        if local_small <= 0:
                            continue
                        self.logger.info(self.generate_random_string(self.SMALL_LOG_LEN))
                        local_small -= 1

                    if action == 2:
                        if local_medium <= 0:
                            continue
                        self.logger.info(self.generate_random_string(self.MEDIUM_LOG_LEN))
                        local_medium -= 1

                    if action == 3:
                        if local_large <= 0:
                            continue
                        self.logger.info(self.generate_random_string(self.LARGE_LOG_LEN))
                        local_large -= 1

                    s -= 1

            written_size += self.flush_log_storage()

        time.sleep(10)

        written_size += self.flush_log_storage()
        self.manager.stop()

        print(f'Really written size:\t{self.get_written_size()}')
        print(f'Sent to write size:\t{written_size}')
        print(f'Expected to write size:\t{expected_size}')


def main():
    assert get_mode() == Mode.TEST

    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--ipython', action='store_true')

    args = parser.parse_args()
    if args.ipython:
        run_ipython()
        return

    for log_file in glob.glob('test.*'):
        os.remove(log_file)

    log_maker = BaseLogMaker(
        name='test',
        file_size_mb=4,
        backup_count=4,
    )
    log_maker.run()
