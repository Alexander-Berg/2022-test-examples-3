import mock
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

from crypta.lookalike.lib.python.test_utils import schemas
from crypta.lookalike.lib.python.utils import yt_schemas
from crypta.lookalike.lib.python.utils.mobile_config import config as mobile_config
from crypta.lookalike.services.mobile_lookalike_training.lib import current_centroids


def test_get_current_centroids(yt_client, yql_client):
    output_table = tables.YsonTable(
        'cluster_centroids_vectors.yson',
        mobile_config.CLUSTER_CENTROIDS_VECTORS,
        yson_format='pretty',
    )

    with mock.patch('crypta.lookalike.lib.python.utils.utils.get_yt_client', return_value=yt_client), \
            mock.patch('crypta.lookalike.lib.python.utils.utils.get_yql_client', return_value=yql_client):
        return tests.yt_test_func(
            yt_client=yt_client,
            func=lambda: current_centroids.get(nv_params=None),
            data_path=yatest.common.test_source_path('data'),
            return_result=False,
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        'app_dssm_vectors.yson',
                        mobile_config.APP_DSSM_VECTORS.replace('testing', 'production'),
                        schemas.app_dssm_vectors_schema,
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'app_dssm_vectors.yson',
                        mobile_config.APP_DSSM_VECTORS,
                        schemas.app_dssm_vectors_schema,
                    ),
                    tests.Exists(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'cluster_centroids.yson',
                        mobile_config.CLUSTERS_INFO,
                        yt_schemas.get_clusters_info_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[(output_table, tests.Diff())],
        )
