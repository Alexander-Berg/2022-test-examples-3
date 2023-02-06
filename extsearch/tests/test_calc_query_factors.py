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
            'CalcQueryUrlFactors',
            '-s', 'local',
            '-i', 'urlbase_mediaQ',
            '-i', 'urlbase_media2',
            '-q', 'qurls',
            '-d', 'out_queryurl_factors',
            '-c', 'config',
            '-e', 'errors',
            '--qu-format', 'state',
            '--qnorm', 'markerdopp',
            '--query-out-table', 'out_query_factors'
        ],
        data_path=data_path,
        input_tables=[
            mr_utils.TableSpec('urlbase_mediaQ', mapreduce_io_flags=['-subkey'], sort_on_load=True),
            mr_utils.TableSpec('urlbase_media2', mapreduce_io_flags=['-subkey'], sort_on_load=True),
            mr_utils.TableSpec('qurls', mapreduce_io_flags=['-subkey'], sort_on_load=True)
        ],
        output_tables=[
            mr_utils.TableSpec('out_query_factors'),
            mr_utils.TableSpec('errors')
        ],
    )
