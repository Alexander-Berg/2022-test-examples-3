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
from crypta.siberia.bin.custom_audience.apps_clustering.lib import apps
from crypta.siberia.bin.custom_audience.apps_clustering.lib.test.utils import yt_schemas
from crypta.siberia.bin.custom_audience.apps_clustering.lib.utils import config as ac_config
from crypta.siberia.bin.custom_audience.lib.python.clustering import (
    utils,
    test_utils,
)


today = '2022-05-16'


@freeze_time(today)
def test_apps_update(patched_yt_client):
    with mock.patch('crypta.siberia.bin.custom_audience.apps_clustering.lib.utils.config.MIN_CLUSTER_SIZE', 1):

        return tests.yt_test_func(
            yt_client=patched_yt_client,
            func=functools.partial(
                apps.update,
                yt_client=patched_yt_client,
            ),
            data_path=yatest.common.test_source_path('data/test_apps_update'),
            input_tables=[
                (
                    tables.YsonTable(
                        'segments_vectors_with_info.yson',
                        utils.get_segments_vectors_with_info_table_path(ac_config.APPS_UPDATE_STAGES_DIR),
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
                                'last_update_planned_date': date_helpers.get_date_from_past(today, days=1),
                            },
                        ),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.YsonTable(
                        'apps.yson',
                        ac_config.CLUSTERING_TABLE,
                        on_write=tables.OnWrite(
                            attributes={
                                'schema': yt_schemas.clustering_schema,
                                'generate_date': date_helpers.get_date_from_past(today, days=1),
                            },
                        ),
                    ),
                    tests.Exists(),
                ),
            ],
            output_tables=[
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
                        'daily_apps.today.yson',
                        os.path.join(ac_config.DAILY_CLUSTERING_DIR, today),
                    ),
                    tests.Diff(),
                ),
            ],
        )
