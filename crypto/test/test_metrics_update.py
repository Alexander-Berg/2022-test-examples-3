import os

from freezegun import freeze_time
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import tables, tests
from crypta.siberia.bin.custom_audience.lib.python.clustering import metrics as clustering_metrics
from crypta.siberia.bin.custom_audience.lib.python.clustering.test.utils import (
    config as clustering_test_config,
    fields as clsutering_test_fields,
    yt_schemas as clustering_test_yt_schemas,
)


@freeze_time('2021-12-27')
def test_metrics_update(patched_yt_client, patched_yql_client):
    yt_client = patched_yt_client
    yql_client = patched_yql_client

    with yt_client.Transaction() as transaction:
        return tests.yt_test_func(
            yt_client=yt_client,
            func=lambda: clustering_metrics.update(
                yt_client=yt_client,
                transaction=transaction,
                yql_client=yql_client,
                centroids_table=clustering_test_config.CENTROIDS_TABLE,
                monthly_clustering_dir=clustering_test_config.MONTHLY_CLUSTERING_DIR,
                metrics_table=clustering_test_config.DATALENS_METRICS_TABLE,
                service_name=clustering_test_config.SERVICE_NAME,
                clustering_fields=clsutering_test_fields,
            ),
            data_path=yatest.common.test_source_path('data'),
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        'test_metrics_update_centroids.yson',
                        clustering_test_config.CENTROIDS_TABLE,
                        schema=schema_utils.yt_schema_from_dict(clustering_test_yt_schemas.get_centroids_schema()),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'test_metrics_update_old_clustering.yson',
                        os.path.join(clustering_test_config.MONTHLY_CLUSTERING_DIR, '2021-12-26'),
                        schema=schema_utils.yt_schema_from_dict(clustering_test_yt_schemas.get_clustering_schema()),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'test_metrics_update_new_clustering.yson',
                        os.path.join(clustering_test_config.MONTHLY_CLUSTERING_DIR, '2021-12-27'),
                        schema=schema_utils.yt_schema_from_dict(clustering_test_yt_schemas.get_clustering_schema()),
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[
                (
                    tables.YsonTable(
                        'metrics.yson',
                        clustering_test_config.DATALENS_METRICS_TABLE,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
            ],
        )
