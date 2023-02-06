import os

from freezegun import freeze_time
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib import date_helpers
from crypta.profile.runners.vectors_update.lib import site2vec
from crypta.profile.runners.vectors_update.lib.utils import yt_schemas
from crypta.profile.utils.config import config
from crypta.siberia.bin.custom_audience.sites_clustering.lib.test.utils import yt_schemas as clustering_yt_schemas
from crypta.siberia.bin.custom_audience.sites_clustering.lib.utils import config as clustering_config


today = '2022-05-16'


@freeze_time(today)
def test_site2vec_update(patched_yt_yql_clients):
    return tests.yt_test_func(
        yt_client=patched_yt_yql_clients[0],
        func=lambda: site2vec.update(patched_yt_yql_clients[0], patched_yt_yql_clients[1]),
        data_path=yatest.common.test_source_path('data/test_site2vec_update'),
        return_result=False,
        input_tables=[
            (
                tables.YsonTable(
                    'site2vec.yson',
                    config.SITE2VEC_VECTORS_TABLE,
                    on_write=tables.OnWrite(
                        attributes={
                            'schema': schema_utils.yt_schema_from_dict(yt_schemas.get_site2vec_schema()),
                            'generate_date': date_helpers.get_date_from_past(today, months=1),
                        },
                    ),
                ),
                tests.Exists(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'app2vec.yson',
                    config.APP2VEC_VECTORS_TABLE,
                    schema_utils.yt_schema_from_dict(yt_schemas.get_app2vec_schema()),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.YsonTable(
                    'centroids.yson',
                    clustering_config.CENTROIDS_TABLE,
                    on_write=tables.OnWrite(
                        attributes={
                            'schema': clustering_yt_schemas.centroids_schema,
                            'last_update_planned_date': date_helpers.get_date_from_past(today, days=1),
                        },
                    ),
                ),
                tests.Exists(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'old_clustering.yson',
                    os.path.join(clustering_config.MONTHLY_CLUSTERING_DIR, date_helpers.get_date_from_past(today, months=1)),
                    clustering_yt_schemas.clustering_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'new_clustering.yson',
                    os.path.join(clustering_config.MONTHLY_CLUSTERING_DIR, date_helpers.get_date_from_past(today, days=1)),
                    clustering_yt_schemas.clustering_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'site2vec.yson',
                    config.SITE2VEC_VECTORS_TABLE,
                    yson_format='pretty',
                ),
                tests.RowCount(7),
            ),
            (
                tables.YsonTable(
                    'site2vec_app2vec.yson',
                    config.SITE2VEC_APP2VEC_VECTORS_TABLE,
                    yson_format='pretty',
                ),
                tests.RowCount(19),
            ),
            (
                tables.YsonTable(
                    'site2vec_app2vec_in_the_same_dimension_by_dates.today.yson',
                    os.path.join(config.SITE2VEC_APP2VEC_VECTORS_FOLDER, today),
                    yson_format='pretty',
                ),
                tests.RowCount(19),
            ),
        ],
    )
