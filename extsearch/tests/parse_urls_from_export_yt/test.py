import yatest.common
from mapreduce.yt.python.yt_stuff import yt_stuff  # noqa
from os import environ
import os


from extsearch.geo.indexer.business_indexer_yt.tests.common import upload_data


environ["YT_STUFF_MAX_START_RETRIES"] = "2"


def test_urls_export(yt_stuff):  # noqa
    server = yt_stuff.get_server()
    yt_env = {
        'YT_STORAGE': 'yes',
        'YT_SERVER': server,
        'YT_PROXY': server,
        'YT_PATH': '//',
    }
    yt_client = yt_stuff.get_yt_client()
    print os.path.realpath(__file__)
    company_path = "//snapshot"
    company_table = company_path + '/company'
    result_file = 'urls'

    test_data_path = yatest.common.source_path('extsearch/geo/base/geobasesearch/tests/business/indexer-business/source')
    yt_client.create('map_node', company_path)
    upload_data.upload_companies_data(yt_client, company_table, test_data_path)

    yatest.common.execute(
        [
            yatest.common.binary_path('extsearch/geo/indexer/business_indexer_yt/parse_urls_from_export_yt/parse_urls_from_export_yt'),
            '-s', server,
            '-o', result_file,
            '-i', company_table,
            '--no-gemini'
        ],
        env=yt_env,
    )

    return yatest.common.canonical_file(result_file, local=True)
