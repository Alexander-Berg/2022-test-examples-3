#!/usr/bin/env python
# -*- coding: utf-8 -*-

import yatest.common
import yt_utils

from mr_utils import TableSpec


def test(yt_stuff):
    return yt_utils.yt_test(
        yatest.common.binary_path('extsearch/video/robot/videoplusquery/tools/extract_frames/bin/extract_frames'),
        args=[
            '--proxy', yt_stuff.get_server(),
            '--input', '//content',
            '--output-frames', '//output_frames',
            '--output-errors', '//output_errors',
            '--step', '6000',
            '--stop-at', '300000',
            '--max-frames', '50'
        ],
        data_path=yatest.common.runtime.work_path(),
        input_tables=[
            TableSpec(
                'content',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_read=False
            )
        ],
        output_tables=[
            TableSpec(
                'output_frames',
                mapreduce_io_flags=['-format', '<columns=[Url;ContentHash;FrameIndex;FrameTimestamp;Width;Height;FrameHash]>schemaful_dsv'],
                sortby=['ContentHash', 'FrameIndex']
            ),
            TableSpec(
                'output_errors',
                mapreduce_io_flags=['-format', '<columns=[Url;ContentHash;Error]>schemaful_dsv'],
                sortby=['ContentHash']
            )
        ],
        yt_stuff=yt_stuff
    )


def test_ocr(yt_stuff):
    return yt_utils.yt_test(
        yatest.common.binary_path('extsearch/video/robot/videoplusquery/tools/extract_frames/bin/extract_frames'),
        args=[
            '--proxy', yt_stuff.get_server(),
            '--input', '//content',
            '--output-frames', '//output_frames',
            '--output-errors', '//output_errors',
            '--step', '1000',
            '--max-frames', '60'
        ],
        data_path=yatest.common.runtime.work_path(),
        input_tables=[
            TableSpec(
                'content',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_read=False
            )
        ],
        output_tables=[
            TableSpec(
                'output_frames',
                mapreduce_io_flags=['-format', '<columns=[Url;ContentHash;FrameIndex;FrameTimestamp;Width;Height;FrameHash]>schemaful_dsv'],
                sortby=['ContentHash', 'FrameIndex']
            ),
            TableSpec(
                'output_errors',
                mapreduce_io_flags=['-format', '<columns=[Url;ContentHash;Error]>schemaful_dsv'],
                sortby=['ContentHash']
            )
        ],
        yt_stuff=yt_stuff
    )


def test_accumulate(yt_stuff):
    return yt_utils.yt_test(
        yatest.common.binary_path('extsearch/video/robot/videoplusquery/tools/extract_frames/bin/extract_frames'),
        args=[
            '--proxy', yt_stuff.get_server(),
            '--input', '//content',
            '--output-frames', '//output_frames',
            '--output-errors', '//output_errors',
            '--step', '6000',
            '--stop-at', '300000',
            '--max-frames', '50',
            '--accumulate'
        ],
        data_path=yatest.common.runtime.work_path(),
        input_tables=[
            TableSpec(
                'content',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_read=False
            )
        ],
        output_tables=[
            TableSpec(
                'output_frames',
                mapreduce_io_flags=['-format', '<columns=[Url;ContentHash;Width;Height]>schemaful_dsv'],
                sortby=['ContentHash']
            ),
            TableSpec(
                'output_errors',
                mapreduce_io_flags=['-format', '<columns=[Url;ContentHash;Error]>schemaful_dsv'],
                sortby=['ContentHash']
            )
        ],
        yt_stuff=yt_stuff
    )


def test_resize(yt_stuff):
    return yt_utils.yt_test(
        yatest.common.binary_path('extsearch/video/robot/videoplusquery/tools/extract_frames/bin/extract_frames'),
        args=[
            '--proxy', yt_stuff.get_server(),
            '--input', '//content',
            '--output-frames', '//output_frames',
            '--output-errors', '//output_errors',
            '--step', '6000',
            '--stop-at', '300000',
            '--max-frames', '50',
            '--accumulate',
            '--aspects-file', yatest.common.source_path('extsearch/video/robot/videoplusquery/tools/apply_cnn/tensorflow.conf')
        ],
        data_path=yatest.common.runtime.work_path(),
        input_tables=[
            TableSpec(
                'content',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_read=False
            )
        ],
        output_tables=[
            TableSpec(
                'output_frames',
                mapreduce_io_flags=['-format', '<columns=[Url;ContentHash;OriginalWidth;OriginalHeight;Width;Height]>schemaful_dsv'],
                sortby=['ContentHash']
            ),
            TableSpec(
                'output_errors',
                mapreduce_io_flags=['-format', '<columns=[Url;ContentHash;Error]>schemaful_dsv'],
                sortby=['ContentHash']
            )
        ],
        yt_stuff=yt_stuff
    )
