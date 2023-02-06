import functools
import os

from freezegun import freeze_time
import mock
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib import date_helpers
from crypta.siberia.bin.custom_audience.apps_clustering.lib import clusters
from crypta.siberia.bin.custom_audience.apps_clustering.lib.test.utils import yt_schemas
from crypta.siberia.bin.custom_audience.apps_clustering.lib.utils import config as ac_config
from crypta.siberia.bin.custom_audience.lib.python.clustering import (
    test_utils,
    utils,
)


today = '2022-05-15'

metrics_lower_bounds = {
    'old_clusterid_share': 0.,
}
metrics_upper_bounds = {
    'distance': 2.,
}


@freeze_time(today)
def test_clusters_update(patched_yt_client, patched_yql_client):
    with mock.patch('crypta.siberia.bin.custom_audience.apps_clustering.lib.utils.config.MIN_CLUSTER_SIZE', 1), \
            mock.patch('crypta.siberia.bin.custom_audience.apps_clustering.lib.utils.config.METRICS_LOWER_BOUNDS', metrics_lower_bounds), \
            mock.patch('crypta.siberia.bin.custom_audience.apps_clustering.lib.utils.config.METRICS_UPPER_BOUNDS', metrics_upper_bounds):

        return tests.yt_test_func(
            yt_client=patched_yt_client,
            func=functools.partial(
                clusters.update,
                yt_client=patched_yt_client,
                yql_client=patched_yql_client,
            ),
            data_path=yatest.common.test_source_path('data/test_clusters_update'),
            input_tables=[
                (
                    tables.YsonTable(
                        'segments_vectors_with_info.yson',
                        utils.get_segments_vectors_with_info_table_path(ac_config.CLUSTERING_UPDATE_STAGES_DIR),
                        on_write=tables.OnWrite(
                            attributes={
                                'schema': yt_schemas.segments_vectors_with_info_schema,
                            },
                        ),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.YsonTable(
                        'centroids.yson',
                        ac_config.CENTROIDS_TABLE,
                        on_write=tables.OnWrite(
                            attributes={
                                'schema': yt_schemas.centroids_schema,
                                'last_update_planned_date': date_helpers.get_date_from_past(today, months=1),
                            },
                        ),
                    ),
                    tests.Exists(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'monthly_apps.latest_clustering.yson',
                        os.path.join(ac_config.MONTHLY_CLUSTERING_DIR, date_helpers.get_date_from_past(today, months=1)),
                        yt_schemas.clustering_schema,
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[
                (
                    test_utils.yson_table_with_float_values(
                        'flatten_centroids_in_new_space.yson',
                        utils.get_flatten_centroids_in_new_space_table_path(ac_config.CLUSTERING_UPDATE_STAGES_DIR),
                    ),
                    tests.Diff(),
                ),
                (
                    test_utils.yson_table_with_float_values(
                        'centroids.yson',
                        ac_config.CENTROIDS_TABLE,
                    ),
                    tests.Diff(),
                ),
                (
                    test_utils.yson_table_with_float_values(
                        'apps.yson',
                        ac_config.CLUSTERING_TABLE,
                    ),
                    tests.Diff(),
                ),
                (
                    test_utils.yson_table_with_float_values(
                        'monthly_apps.today.yson',
                        os.path.join(ac_config.MONTHLY_CLUSTERING_DIR, today),
                    ),
                    tests.Diff(),
                ),
                (
                    test_utils.yson_table_with_float_values(
                        'daily_apps.today.yson',
                        os.path.join(ac_config.DAILY_CLUSTERING_DIR, today),
                    ),
                    tests.Diff(),
                ),
                (
                    tables.YsonTable(
                        'metrics.yson',
                        ac_config.DATALENS_METRICS_TABLE,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
            ],
        )
