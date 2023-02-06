import logging

import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


logger = logging.getLogger(__name__)


def get_app_info_schema():
    return schema_utils.yt_schema_from_dict({
        "BundleId": "string",
        "PlatformID": "uint64",
        "RegionName": "string",
        "Title": "string",
        "VendorName__raw": "string",
    })


def get_apps_clustering_schema():
    return schema_utils.yt_schema_from_dict({
        "app_id": "uint64",
        "bundle_id": "string",
        "cluster_id": "uint64",
        "devids_count": "uint64",
        "id_type": "string",
    })


def test_build_apps_for_suggester(clean_local_yt, config, config_file, local_yt_and_yql_env):
    return tests.yt_test(
        yt_client=clean_local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/siberia/bin/custom_audience/build_apps_for_suggester/bin/crypta-siberia-custom-audience-build-apps-for-suggester"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema(
                'app_info.yson',
                config.RmpAppsPath,
                schema=get_app_info_schema(),
            ), (tests.TableIsNotChanged())),
            (tables.get_yson_table_with_schema(
                'apps_clustering.yson',
                config.AppsClusteringPath,
                schema=get_apps_clustering_schema(),
            ), (tests.TableIsNotChanged())),
        ],
        output_tables=[
            (tables.YsonTable('apps.yson', config.OutputPath, yson_format="pretty"), [tests.Diff()]),
        ],
        env=local_yt_and_yql_env,
    )
