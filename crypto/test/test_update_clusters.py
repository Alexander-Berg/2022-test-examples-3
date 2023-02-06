import mock
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

from crypta.lookalike.lib.python.test_utils import schemas
from crypta.lookalike.lib.python.utils import yt_schemas
from crypta.lookalike.lib.python.utils.mobile_config import config as mobile_config
from crypta.lookalike.services.mobile_lookalike_training.lib import clusters


def test_update_clusters(yt_client, yql_client):
    output_tables = [
        tables.YsonTable(
            'clusters_info.yson',
            mobile_config.CLUSTERS_INFO,
            yson_format='pretty',
        ),
        tables.YsonTable(
            'apps_clustering.yson',
            mobile_config.APPS_CLUSTERING,
            yson_format='pretty',
        ),
        tables.YsonTable(
            'datalens.yson',
            mobile_config.DATALENS_MOBILE_LAL_METRICS_TABLE,
            yson_format='pretty',
        ),
    ]

    with mock.patch('crypta.lookalike.lib.python.utils.utils.get_yt_client', return_value=yt_client), \
            mock.patch('crypta.lookalike.lib.python.utils.utils.get_yql_client', return_value=yql_client), \
            mock.patch('crypta.lookalike.lib.python.utils.mobile_utils.get_date_from_nv_parameters',
                       return_value='2022-01-10'):
        return tests.yt_test_func(
            yt_client=yt_client,
            func=lambda: clusters.update(nv_params=None),
            data_path=yatest.common.test_source_path('data'),
            return_result=False,
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        'app_dssm_vectors.yson',
                        mobile_config.APP_DSSM_VECTORS,
                        schemas.app_dssm_vectors_schema,
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'cluster_centroids_vectors.yson',
                        mobile_config.CLUSTER_CENTROIDS_VECTORS,
                        schemas.cluster_centroids_vectors_schema,
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'apps_clustering.yson',
                        mobile_config.APPS_CLUSTERING,
                        schema_utils.yt_schema_from_dict(yt_schemas.get_apps_clustering_schema()),
                    ),
                    tests.Exists(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'datalens.yson',
                        mobile_config.DATALENS_MOBILE_LAL_METRICS_TABLE,
                        schema_utils.yt_schema_from_dict(schemas.datalens_data_table_schema),
                    ),
                    tests.Exists(),
                ),
            ],
            output_tables=[(output_table, tests.Diff()) for output_table in output_tables],
        )
