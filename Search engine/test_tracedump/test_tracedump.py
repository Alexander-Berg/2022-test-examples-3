# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import collections
import glob
import os
import random
import re
import subprocess
import typing

from search.martylib.core.logging_utils.binlog import BinlogManager
from search.martylib.proto.structures import trace_pb2
from search.martylib.test_utils import TestCase
from search.martylib.trace.tracedump.emitters import ContainerEmitter
from search.martylib.trace.tracedump.readers import ReverseFileReader, FileReader, BidirectionalFileReader
from search.martylib.trace.tracedump.tracedumper import TraceDumper


def get_martylib_path():
    cur_path = os.getcwd()

    while cur_path != '/':
        split_path = os.path.split(cur_path)
        if split_path[1].lower() == 'martylib':
            break
        cur_path = split_path[0]
    if cur_path == '/':
        raise ValueError('The script was not launched from the arcadia directory')
    return cur_path


# noinspection PyPep8Naming
class _FakeItem:
    def __init__(self, obj: bytes):
        self.obj = obj

    def SerializeToString(self):
        return self.obj


class TestTracedump(TestCase):

    IDX_FILE = re.compile(r'.*\.idx')

    logs_path = os.path.join(get_martylib_path(), 'tests/py3only/test_tracedump/logs/')
    warning_logs_path = os.path.join(get_martylib_path(), 'tests/py3only/test_tracedump/warnings/')

    @property
    def files_to_parse(self):
        files_to_parse = set(glob.glob(os.path.join(self.logs_path, '*.blog*')))
        return [file for file in files_to_parse if not self.IDX_FILE.match(file)]

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.maxDiff = None

        proc = subprocess.Popen(
            (os.path.join(cls.logs_path, 'tests_log_maker'), ),
            stderr=subprocess.PIPE, stdout=subprocess.PIPE,
            cwd=cls.logs_path,
            env={
                **os.environ,
                'MODE': 'TEST',
            },
        )

        out, err = proc.communicate()
        cls.logger.info(out.decode())
        cls.logger.error(err.decode())

        proc = subprocess.Popen(
            (os.path.join(cls.warning_logs_path, 'tests_warnings_log_maker'), ),
            stderr=subprocess.PIPE, stdout=subprocess.PIPE,
            cwd=cls.warning_logs_path,
            env={},
        )

        out, err = proc.communicate()
        cls.logger.info(out.decode())
        cls.logger.error(err.decode())

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        for log_file in glob.glob(os.path.join(cls.logs_path, 'test.*')):
            os.remove(log_file)

    def test_base_file_readers(self):
        file = os.path.join(self.logs_path, 'test.blog')

        reader = ReverseFileReader(fd=open(file, mode='rb'))
        self.logger.info(next(reader))
        self.logger.info(next(reader))
        self.logger.info(next(reader))
        reader.stop()

        reader = FileReader(fd=open(file, mode='rb'))
        self.logger.info(next(reader))
        self.logger.info(next(reader))
        self.logger.info(next(reader))
        reader.stop()

        reader = BidirectionalFileReader(file_names_forward=(file,), file_names_reverse=(file,))
        self.logger.info(next(reader))
        self.logger.info(next(reader))
        self.logger.info(next(reader))
        reader.stop()

    def test_file_reader(self):
        to_write = [_FakeItem(os.urandom(random.randint(5, 100))) for _ in range(20)]
        files = [os.path.join(self.logs_path, f'test{i}.t') for i in range(1, 5)]

        with open(files[0], 'wb') as fd:
            for i in range(0, 5):
                # noinspection PyTypeChecker
                fd.write(BinlogManager.encode_record(to_write[i]))

        with open(files[1], 'wb') as fd:
            # noinspection PyTypeChecker
            fd.write(BinlogManager.encode_record(to_write[5]))

        with open(files[2], 'wb') as fd:
            for i in range(6, 19):
                # noinspection PyTypeChecker
                fd.write(BinlogManager.encode_record(to_write[i]))

        with open(files[3], 'wb') as fd:
            # noinspection PyTypeChecker
            fd.write(BinlogManager.encode_record(to_write[19]))

        reader = ReverseFileReader(file_names=reversed(files))

        result = [item for item in reader]
        result.reverse()

        self.assertEqual([item.obj for item in to_write], result)

        reader = FileReader(file_names=files)
        result = [item for item in reader]

        self.assertEqual([item.obj for item in to_write], result)

    def test_no_children(self):
        context = trace_pb2.ParserContext(
            filenames='logs/sample.blog',
        )

        result, _ = self._run_tracedump(context)

        child_frames = 0
        for t in result:
            child_frames += len(t.child_frames)
        self.assertNotEqual(child_frames, 0)

        context = trace_pb2.ParserContext(
            filenames='logs/sample.blog',
            no_children=True
        )

        result, _ = self._run_tracedump(context)

        child_frames = 0
        for t in result:
            child_frames += len(t.child_frames)
        self.assertEqual(child_frames, 0)

    def test_tracedump_index(self):
        context = trace_pb2.ParserContext(
            index_field="index_1",
            index_value="withp0000.*",
            regex_index_lookup=True,
            no_children=False,
            start="2010-10-10 10:10",
            end="2012-12-12 12:12",
        )

        with self.assertRaises(AttributeError):
            self._run_tracedump(context)

        context = trace_pb2.ParserContext(
            index_field="index_1",
            index_value="withp0000.*",
            regex_index_lookup=True,
            no_children=False,
            start="1111-01-01 10:10",
            end="2222-02-02 20:20",
        )

        result, _ = self._run_tracedump(context)
        self.assertEqual(len(result), 9)

        context = trace_pb2.ParserContext(
            expand_frame=True,
            index_field="index_1",
            index_value="trace00001",
        )

        result, _ = self._run_tracedump(context)
        self.assertEqual(len(result), 1)

        context = trace_pb2.ParserContext(
            expand_frame=True,
            index_field="index_1",
            index_value="trace0000.*",
            max_index_offsets=15,
            max_emitted_frames=15,
            regex_index_lookup=True,
        )

        result, _ = self._run_tracedump(context)

        self.logger.info(result)
        self.assertEqual(len(result), 9)

        context = trace_pb2.ParserContext(
            index_field="index_1",
            index_value="withp0000.*",
            max_index_offsets=15,
            max_emitted_frames=15,
            regex_index_lookup=True,
        )

        result, _ = self._run_tracedump(context)
        self.assertEqual(len(result), 9)

    def test_tracedump_index_with_filters(self):
        context = trace_pb2.ParserContext(
            index_field="index_1",
            index_value="kek",
            max_index_offsets=15,
            max_emitted_frames=15,
            filters=[
                trace_pb2.FilterConfiguration(
                    type=trace_pb2.FilterType.ExceptionFilter,
                )
            ]
        )

        result, _ = self._run_tracedump(context, files=[os.path.join(self.warning_logs_path, 'test.blog')])
        self.assertEqual(len(result), 3)

        context.filters[0].query = 'Async'
        result, _ = self._run_tracedump(context, files=[os.path.join(self.warning_logs_path, 'test.blog')])
        self.assertEqual(len(result), 2)

    def test_logger_exception(self):
        context = trace_pb2.ParserContext(
            expand_frame=True,
            max_emitted_frames=15,
            reverse=True,
            filters=[
                trace_pb2.FilterConfiguration(
                    type=trace_pb2.FilterType.NameFilter,
                    name_regexp='trace_with_logger_exception'
                )
            ],
        )

        result, _ = self._run_tracedump(context, [os.path.join(self.warning_logs_path, 'test.blog')])
        self.assertEqual(len(result), 1)
        self.assertEqual(
            result[0].end.exception,
            'StopAsyncIteration: lol kek cheburek:\n  File "search/martylib/tests/py3only/test_tracedump/warnings/logs_maker.py", line 118, in main\n    '
            'raise StopAsyncIteration(\'lol kek cheburek\')\n'
        )

    def test_query_filter(self):
        context = trace_pb2.ParserContext(
            expand_frame=True,
            max_emitted_frames=15,
            reverse=True,
            filters=[
                trace_pb2.FilterConfiguration(
                    type=trace_pb2.FilterType.QueryFilter,
                    query='index_1~=trace0000.*'
                )
            ],
        )

        result, _ = self._run_tracedump(context)
        self.assertEqual(len(result), 9)

        context = trace_pb2.ParserContext(
            expand_frame=True,
            reverse=True,
            max_emitted_frames=15,
            filters=[
                trace_pb2.FilterConfiguration(
                    type=trace_pb2.FilterType.QueryFilter,
                    query='index_1~=.*0000.* && !(index_1~=withp.*)'
                )
            ],
        )

        result, _ = self._run_tracedump(context)
        self.assertEqual(len(result), 9)

        context = trace_pb2.ParserContext(
            expand_frame=True,
            reverse=True,
            max_emitted_frames=2,
            filters=[
                trace_pb2.FilterConfiguration(
                    type=trace_pb2.FilterType.QueryFilter,
                    query='#bar == 3'
                )
            ],
        )

        result, _ = self._run_tracedump(context, files=[os.path.join(self.warning_logs_path, 'test.blog')])
        self.assertEqual(len(result), 1)

        context = trace_pb2.ParserContext(
            expand_frame=True,
            reverse=True,
            max_emitted_frames=2,
            filters=[
                trace_pb2.FilterConfiguration(
                    type=trace_pb2.FilterType.QueryFilter,
                    query='$.end.time - $.start.time > <float> 0.5'
                )
            ],
        )

        result, _ = self._run_tracedump(context, files=[os.path.join(self.warning_logs_path, 'test.blog')])
        self.assertEqual(len(result), 2)

    # FIXME: pufit@
    # def test_warning_logs(self):
    #     context = trace_pb2.ParserContext(
    #         expand_frame=True,
    #         no_children=True,
    #     )
    #
    #     result, _ = self._run_tracedump(
    #         context,
    #         files=[os.path.join(self.warning_logs_path, 'test-dedicated.blog')],
    #         check_sizes=False,
    #     )
    #
    #     for frame in result:
    #         frame: trace_pb2.Frame
    #         self.assertEqual(len(list(frame.child_frames)), 0)
    #
    #     self.assertEqual(
    #         [
    #             (
    #                 frame.start.name,
    #                 len(list(frame.child_frames)),
    #                 len(frame.logs),
    #                 len(frame.end.exception)
    #             )
    #             for frame in result
    #         ],
    #         [
    #             ('foo', 0, 0, 0),
    #             ('foo_debug_2', 0, 1, 295),
    #             ('foo_debug_1', 0, 0, 295),
    #             ('there', 0, 1, 0)
    #         ]
    #     )
    #
    #     context = trace_pb2.ParserContext(
    #         expand_frame=True,
    #     )
    #
    #     result, _ = self._run_tracedump(
    #         context,
    #         files=[os.path.join(self.warning_logs_path, 'test-dedicated.blog')],
    #         check_sizes=False
    #     )
    #
    #     self.assertTrue(any(list(frame.child_frames) for frame in result))
    #
    #     result, _ = self._run_tracedump(
    #         context,
    #         files=[os.path.join(self.warning_logs_path, 'test-dedicated.blog')],
    #         check_sizes=False,
    #     )
    #     self.assertEqual(len(result), 2)
    #
    #     result, _ = self._run_tracedump(
    #         context,
    #         files=[os.path.join(self.warning_logs_path, 'test-dedicated.blog')],
    #         check_sizes=False,
    #     )
    #
    #     self.assertEqual(len(result), 2)
    #     self.assertEqual(
    #         [frame.start.name for frame in result],
    #         ['foo', 'foo_debug_1']
    #     )

    def _run_tracedump(self, context, files=None, check_sizes=True):
        result: typing.Deque[typing.Any] = collections.deque()
        emitters = (ContainerEmitter(result), )

        td = TraceDumper.from_proto(
            proto=context,
            files=files or self.files_to_parse,
            emitters=emitters,
        )
        td.run()

        td.ctxw.inject_stats(context)
        self._validate_frames(frames=result, check_sizes=check_sizes)
        return result, td.ctxw.context

    def _validate_frames(self, frames, check_sizes=True):
        for frame in frames:
            self.assertIsNotNone(frame.start)
            self.assertIsNotNone(frame.end)
            if check_sizes:
                self.assertEqual(frame.end.log_count, len(frame.logs))

            for child in frame.iter_children:
                self.assertIsNotNone(child.start)
                self.assertIsNotNone(child.end)
                if check_sizes:
                    self.assertEqual(child.end.log_count, len(child.logs))
