# coding: utf-8

import os
import yatest.common
import mr_utils
import yt_utils


def test():
    data_path = yatest.common.data_path("extsearch/video/quality/vegas")
    os.environ["YT_PREFIX"] = '//'
    return yt_utils.yt_test(
        yatest.common.binary_path("extsearch/video/quality/vegas/vegas"),
        args=[
            'CalcUrlFactors',
            '-s', 'local',
            '-i', 'urlbase_media',
            '-i', 'urlbase_media2',
            '-u', 'url_factors',
            '-r', 'recommender_data',
            '-d', 'out_url_factors',
            '-e', 'errors',
            '-c', 'config'
        ],
        data_path=data_path,
        input_tables=[
            mr_utils.TableSpec('urlbase_media', mapreduce_io_flags=['-subkey'], sort_on_load=True),
            mr_utils.TableSpec('urlbase_media2', mapreduce_io_flags=['-subkey'], sort_on_load=True),
            mr_utils.TableSpec('url_factors', sort_on_load=True),
            mr_utils.TableSpec('recommender_data', sort_on_load=True)
        ],
        output_tables=[
            mr_utils.TableSpec('out_url_factors'),
            mr_utils.TableSpec('errors', mapreduce_io_flags=['-subkey'])
        ],
        timeout=600,
    )
