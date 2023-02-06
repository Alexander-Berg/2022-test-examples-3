# -*- coding: utf-8 -*-

import json
import os.path
import subprocess
import yatest.common

from mapreduce.yt.python.yt_stuff import (
    YtConfig,
    YtStuff
)
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix
)
from yt.wrapper import (
    ypath_join
)


def prepare_json_data(output_filename, data):
    with open(output_filename, 'w') as output_file:
        json.dump(data, output_file)


def run_pbsncat(magic, input_filename, input_format, output_filename, output_format):
    pbsncat_bin = yatest.common.binary_path(os.path.join(
        'market',
        'idx',
        'tools',
        'pbsncat',
        'bin',
        'pbsncat'
    ))

    with open(input_filename) as input_file:
        with open(output_filename, 'w') as output_file:
            subprocess.check_call(
                args=[
                    pbsncat_bin,
                    '--input-format', input_format,
                    '--format', output_format,
                    '--magic', magic
                ],
                stdin=input_file,
                stdout=output_file
            )


def run_update_dynamic_filter_outlet(yt_outlet_data):
    yt_server = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    yt_server.start_local_yt()
    yt_client = yt_server.get_yt_client()

    yt_table_path = ypath_join(get_yt_prefix(), 'market', 'logistics_management_service', 'yt_outlet')
    yt_client.create('table', yt_table_path, ignore_existing=True, recursive=True)
    yt_client.write_table(yt_table_path, yt_outlet_data)

    update_dynamic_filter_outlet_bin = yatest.common.binary_path(os.path.join(
        'market',
        'tools',
        'update_dynamic_filter_outlet',
        'update_dynamic_filter_outlet'
    ))

    output_filename = yatest.common.output_path('fast_data_outlets.pb.sn')
    out = yatest.common.output_path('update_dynamic_filter_outlet.log')
    err = yatest.common.output_path('update_dynamic_filter_outlet.err')

    with open(output_filename, 'w'):
        with open(out, 'w') as out_file:
            with open(err, 'w') as err_file:
                subprocess.check_call(
                    args=[
                        update_dynamic_filter_outlet_bin,
                        '--cluster', yt_server.get_server(),
                        '--pb', output_filename,
                        '--table', yt_table_path
                    ],
                    stdout=out_file,
                    stderr=err_file
                )

    return output_filename, out, err
