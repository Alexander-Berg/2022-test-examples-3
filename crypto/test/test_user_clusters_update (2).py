import os

from freezegun import freeze_time
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib import date_helpers
from crypta.siberia.bin.custom_audience.sites_clustering.lib import user_clusters
from crypta.siberia.bin.custom_audience.sites_clustering.lib.test.utils import yt_schemas
from crypta.siberia.bin.custom_audience.sites_clustering.lib.utils import config


today = '2022-05-15'


@freeze_time(today)
def test_user_clusters_update(patched_yt_client, patched_yql_client):
    return tests.yt_test_func(
        yt_client=patched_yt_client,
        func=lambda: user_clusters.update(),
        data_path=yatest.common.test_source_path('data/test_user_clusters_update'),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    'metrics_yandexuid_flattened_hits.yson',
                    config.METRICS_YANDEXUID_FLATTEN_HITS_TABLE,
                    yt_schemas.metrics_yandexuid_flattened_hits_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'bar_yandexuid_flattened_hits.yson',
                    config.BAR_YANDEXUID_FLATTEN_HITS_TABLE,
                    yt_schemas.bar_yandexuid_flattened_hits_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'metrics_crypta_id_flattened_hits.yson',
                    config.METRICS_CRYPTA_ID_FLATTEN_HITS_TABLE,
                    yt_schemas.metrics_crypta_id_flattened_hits_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'bar_crypta_id_flattened_hits.yson',
                    config.BAR_CRYPTA_ID_FLATTEN_HITS_TABLE,
                    yt_schemas.bar_crypta_id_flattened_hits_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'matching_yandexuid_crypta_id.yson',
                    config.MATCHING_YANDEXUID_CRYPTA_ID_TABLE,
                    yt_schemas.matching_yandexuid_crypta_id_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.YsonTable(
                    'sites.yson',
                    config.CLUSTERING_TABLE,
                    on_write=tables.OnWrite(
                        attributes={
                            'schema': yt_schemas.clustering_schema,
                            'generate_date': today,
                        },
                    ),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.YsonTable(
                    'centroids.yson',
                    config.CENTROIDS_TABLE,
                    on_write=tables.OnWrite(
                        attributes={
                            'schema': yt_schemas.centroids_schema,
                            'last_update_planned_date': date_helpers.get_date_from_past(today, months=1),
                        },
                    ),
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'yandexuid_clusterid.yson',
                    os.path.join(config.YANDEXUID_CLUSTERID_DIR, today),
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    'crypta_id_cluster_id.yson',
                    os.path.join(config.CRYPTAID_CLUSTERID_DIR, today),
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    'centroids.yson',
                    config.CENTROIDS_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
