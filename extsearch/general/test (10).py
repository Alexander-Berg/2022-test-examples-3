import os
import yatest.common
import yt_utils

from mr_utils import TableSpec


def test(yt_stuff):
    return yt_utils.yt_test(
        yatest.common.binary_path('extsearch/video/robot/videoplusquery/tools/extract_frames_v2/extract_frames_v2'),
        [
            '--cluster', yt_stuff.get_server(),
            '--input-table', '//input_table',
            '--output-table', '//output_table',
            '--errors-table', '//errors_table',
            '--aspects-file', yatest.common.source_path('extsearch/video/robot/videoplusquery/tools/apply_cnn/tensorflow.conf'),
        ],
        os.getcwd(),
        input_tables=[
            TableSpec('input_table', table_name='//input_table', mapreduce_io_flags=['-format', 'yson', '-tablewriter', '{"max_row_weight": 128000000}'], sort_on_read=False),
        ],
        output_tables=[
            TableSpec('output_table', table_name='//output_table', mapreduce_io_flags=['-format', 'yson'], sortby=['ContentHash']),
            TableSpec('errors_table', table_name='//errors_table', mapreduce_io_flags=['-format', 'yson'], sortby=['ContentHash']),
        ],
        yt_stuff=yt_stuff,
        diff_tool=[
            yatest.common.binary_path('extsearch/video/robot/videoplusquery/diff_tool/diff_tool'),
            '--proto', 'NVideo::NVpq::TMultipleFramesRow',
        ],
    )
