# coding: utf-8

import os
import yt_utils
import yatest.common
from mr_utils import TableSpec


def test():
    data_path = yatest.common.data_path("extsearch/video/quality/vegas")
    os.environ["YT_PREFIX"] = '//'
    return yt_utils.yt_test(
        yatest.common.binary_path("extsearch/video/quality/vegas/vegas"),
        args=[
            'CalcAuthorFactors',
            '-s', 'local',
            '-i', 'urlbase_authors',
            '-u', 'urlFactors_authors',
            '-d', 'out_author_factors',
            '--url2author', 'url2author',
            '-e', 'errors',
            '-c', 'config'
        ],
        data_path=data_path,
        input_tables=[
            TableSpec('urlbase_authors', mapreduce_io_flags=['-subkey'], sort_on_load=True),
            TableSpec('urlFactors_authors', sort_on_load=True)
        ],
        output_tables=[
            TableSpec('out_author_factors'),
            TableSpec('url2author'),
            TableSpec('errors')
        ],
    )


def test_aggregate_author_info():
    data_path = yatest.common.data_path("extsearch/video/quality/vegas")
    os.environ["YT_PREFIX"] = '//'
    return yt_utils.yt_test(
        yatest.common.binary_path("extsearch/video/quality/vegas/vegas"),
        args=[
            'CalcAuthorFactors',
            '-s', 'local',
            '-i', 'urlbase_authors2',
            '-u', 'urlFactors_authors',
            '-d', 'out_author_factors',
            '--url2author', 'url2author',
            '-e', 'errors',
            '-c', 'config'
        ],
        data_path=data_path,
        input_tables=[
            TableSpec('urlbase_authors2', mapreduce_io_flags=['-subkey'], sort_on_load=True),
            TableSpec('urlFactors_authors', sort_on_load=True)
        ],
        output_tables=[
            TableSpec('out_author_factors'),
            TableSpec('url2author'),
            TableSpec('errors')
        ],
    )
