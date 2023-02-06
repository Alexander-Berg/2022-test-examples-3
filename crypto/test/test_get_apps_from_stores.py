import functools
from freezegun import freeze_time
import mock
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib import date_helpers
from crypta.siberia.bin.custom_audience.apps_clustering.lib.test.utils import yt_schemas
from crypta.siberia.bin.custom_audience.apps_clustering.lib.utils import config as ac_config
from crypta.siberia.bin.custom_audience.lib.python.clustering import utils as clustering_utils
from crypta.siberia.bin.custom_audience.apps_clustering.lib.utils import utils as ac_utils


today = '2022-05-15'
segments_dict_path = '//home/segments_dict'


@freeze_time(today)
def test_clusters_update(patched_yt_client, patched_yql_client):
    with mock.patch('crypta.siberia.bin.custom_audience.apps_clustering.lib.utils.config.MIN_DEVIDS_COUNT', 1):

        return tests.yt_test_func(
            yt_client=patched_yt_client,
            func=functools.partial(
                ac_utils.get_apps_from_stores,
                yt_client=patched_yt_client,
                yql_client=patched_yql_client,
                nv_params={'only_new_apps': False},
                output='output.txt',
            ),
            data_path=yatest.common.test_source_path('data/test_get_apps_from_stores'),
            input_tables=[
                (
                    tables.YsonTable(
                        'app_data.yson',
                        ac_config.APP_DATA_TABLE,
                        on_write=tables.OnWrite(
                            attributes={
                                'schema': yt_schemas.app_data_schema,
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
                (
                    tables.YsonTable(
                        'devid_by_app.yson',
                        ac_config.DEVID_BY_APP_TABLE,
                        on_write=tables.OnWrite(
                            attributes={
                                'schema': yt_schemas.devid_by_app_schema,
                                'generate_date': today,
                            },
                        ),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'app_dict.yson',
                        ac_config.APP_DICT_TABLE,
                        yt_schemas.app_dict_schema,
                    ),
                    tests.Exists(),
                ),
            ],
            output_tables=[
                (
                    tables.YsonTable(
                        'segments_with_info.yson',
                        clustering_utils.get_segments_with_info_table_path(ac_config.CLUSTERING_UPDATE_STAGES_DIR),
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
                (
                    tables.YsonTable(
                        'app_dict.yson',
                        ac_config.APP_DICT_TABLE,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
                (
                    tables.YsonTable(
                        'segments_vectors_with_info.yson',
                        clustering_utils.get_segments_vectors_with_info_table_path(ac_config.CLUSTERING_UPDATE_STAGES_DIR),
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
            ],
        )
