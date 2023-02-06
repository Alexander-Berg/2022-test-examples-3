#!/usr/bin/env python
# -*- coding: utf-8 -*-

import yatest.common
import yt_utils

from mr_utils import TableSpec


def test(yt_stuff):
    return yt_utils.yt_test(
        yatest.common.binary_path('extsearch/video/robot/videoplusquery/tools/resize_frames/resize_frames'),
        args=[
            '--config', yatest.common.source_path('extsearch/video/robot/videoplusquery/tools/apply_cnn/tensorflow.conf'),
            '--proxy', yt_stuff.get_server(),
            '--input', '//frames',
            '--output', '//output_frames'
        ],
        data_path=yatest.common.runtime.work_path(),
        input_tables=[
            TableSpec(
                'frames',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_read=False
            )
        ],
        output_tables=[
            TableSpec(
                'output_frames',
                mapreduce_io_flags=['-format', '<columns=[ContentHash;FrameIndex;FrameTimestamp;Width;Height;FrameHash]>schemaful_dsv'],
                sortby=['ContentHash', 'FrameIndex']
            )
        ],
        yt_stuff=yt_stuff
    )
