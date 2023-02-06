import yatest.common
import json

from mapreduce.yt.python.yt_stuff import yt_stuff  # noqa
import shutil

from os import environ
import pytest
import os

environ["YT_STUFF_MAX_START_RETRIES"] = "2"


@pytest.fixture(scope='module')
def context():
    class Context(object):
        def __init__(self):
            poi_categories = 'categories_merged.json'

            shutil.copytree(yatest.common.source_path('extsearch/geo/conveyors/annotations/poisrc_parser'), 'poisrc_parser')
            shutil.copytree(yatest.common.source_path('extsearch/geo/conveyors/annotations/address_extractor'), 'address_extractor')
            shutil.copytree(yatest.common.source_path('extsearch/geo/conveyors/annotations/normalizer'), 'normalizer')

            shutil.copy(yatest.common.source_path('extsearch/geo/conveyors/annotations/tests/categories_merged.json'), poi_categories)
    return Context()


def upload_data(yt_client, test_data_prefix, table_prefix, table):
    table_path = os.path.join(table_prefix, table)
    yt_client.create('table', table_path, recursive=True, ignore_existing=True)
    with open(os.path.join(test_data_prefix, table)) as f:
        yt_client.write_table(
            table_path,
            [json.loads(line) for line in f.readlines()])


def prepare_business_export(yt_client, snapshot_data_path, snapshot_path):

    yt_client.create('map_node', snapshot_path, ignore_existing=True)
    assert yt_client.exists(snapshot_path)

    upload_data(yt_client, snapshot_data_path, snapshot_path, "company")
    upload_data(yt_client, snapshot_data_path, snapshot_path, "company_to_feature")
    upload_data(yt_client, snapshot_data_path, snapshot_path, "feature")
    upload_data(yt_client, snapshot_data_path, snapshot_path, "company_to_rubric")
    upload_data(yt_client, snapshot_data_path, snapshot_path, "rubric")
    upload_data(yt_client, snapshot_data_path, snapshot_path, "feature_enum_value")
    upload_data(yt_client, snapshot_data_path, snapshot_path, "chain")
    upload_data(yt_client, snapshot_data_path, snapshot_path, "company_to_chain")


def test_filtration(yt_stuff, context):  # noqa

    server = yt_stuff.get_server()
    yt_env = {
        'YT_STORAGE': 'yes',
        'YT_SERVER': server,
        'YT_PROXY': server,
        'YT_PATH': '//',
    }
    yt_client = yt_stuff.get_yt_client()

    snapshot_path = "//snapshot"
    result_path = "//result_path"
    tech_path = "//tech_path"

    snapshot_data_path = "test_data/snapshot"
    clicks_data_path = "test_data/click_data/filtration"
    poi_path = "test_data/poisrc"
    poi_categories = 'categories_merged.json'

    runner_path = yatest.common.source_path('extsearch/geo/conveyors/annotations/make_filtration_stream.sh')

    shutil.copytree(yatest.common.source_path('extsearch/geo/conveyors/annotations/tests/test_data'), 'test_data')

    shutil.copytree(yatest.common.source_path('extsearch/geo/conveyors/annotations/metro_extractor'), 'metro_extractor')

    shutil.copytree(yatest.common.source_path('extsearch/geo/conveyors/annotations/oid_ll_extractor'), 'oid_ll_extractor')

    prepare_business_export(yt_client, snapshot_data_path, snapshot_path)

    yt_client.create('map_node', result_path, ignore_existing=True)
    yt_client.create('map_node', tech_path, ignore_existing=True)

    upload_data(yt_client, clicks_data_path, result_path, "maps_clicks_shows")
    upload_data(yt_client, clicks_data_path, result_path, "serp_clicks")

    yatest.common.execute(
        [
            runner_path,
            poi_path,
            poi_categories,
            snapshot_path,
            result_path,
            tech_path,
            server
        ],
        env=yt_env,
    )
    shutil.rmtree('test_data')


def test_ranking(yt_stuff, context):  # noqa
    server = yt_stuff.get_server()
    yt_env = {
        'YT_STORAGE': 'yes',
        'YT_SERVER': server,
        'YT_PROXY': server,
        'YT_PATH': '//',
    }
    yt_client = yt_stuff.get_yt_client()

    print os.path.realpath(__file__)

    snapshot_path = "//snapshot"
    result_path = "//result_path"
    tech_path = "//tech_path"

    snapshot_data_path = "test_data/snapshot"
    clicks_data_path = "test_data/click_data/ranking"
    poi_path = "test_data/poisrc"
    poi_categories = 'categories_merged.json'

    runner_path = yatest.common.source_path('extsearch/geo/conveyors/annotations/make_factor_stream.sh')

    shutil.copytree(yatest.common.source_path('extsearch/geo/conveyors/annotations/tests/test_data'), 'test_data')

    prepare_business_export(yt_client, snapshot_data_path, snapshot_path)

    yt_client.create('map_node', result_path, ignore_existing=True)
    yt_client.create('map_node', tech_path, ignore_existing=True)

    upload_data(yt_client, clicks_data_path, result_path, "maps_clicks_shows")
    upload_data(yt_client, clicks_data_path, result_path, "serp_clicks")
    upload_data(yt_client, clicks_data_path, result_path, "mobile_clicks")

    yatest.common.execute(
        [
            runner_path,
            poi_path,
            poi_categories,
            snapshot_path,
            result_path,
            tech_path,
            server
        ],
        env=yt_env,
    )

    shutil.rmtree('test_data')
