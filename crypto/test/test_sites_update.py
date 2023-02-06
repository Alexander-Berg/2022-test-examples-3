import os

from freezegun import freeze_time
import mock
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.lookalike.lib.python.utils.config import config as lal_config
from crypta.profile.lib import date_helpers
from crypta.siberia.bin.custom_audience.lib.python.clustering import (
    test_utils,
    utils as utils,
)
from crypta.siberia.bin.custom_audience.sites_clustering.lib import sites
from crypta.siberia.bin.custom_audience.sites_clustering.lib.test.utils import yt_schemas
from crypta.siberia.bin.custom_audience.sites_clustering.lib.utils import config as sc_config


today = '2022-05-16'


@freeze_time(today)
def test_sites_update(patched_yt_client, patched_yql_client, crypta_id_user_data_table, dssm_lal_model_with_sandbox_link_attr):
    with test_utils.mock_sandbox_dssm_lookalike_model() as mocked_sandbox_dssm_lookalike_model, \
            mock.patch('crypta.siberia.bin.custom_audience.sites_clustering.lib.utils.config.MIN_CLUSTER_SIZE', 1):

        get_segment_dict, get_dssm_lal_model = dssm_lal_model_with_sandbox_link_attr

        return tests.yt_test_func(
            yt_client=patched_yt_client,
            func=lambda: sites.update(),
            data_path=yatest.common.test_source_path('data/test_sites_update'),
            input_tables=[
                (
                    tables.YsonTable(
                        'metrics_crypta_id_flattened_hits.yson',
                        sc_config.METRICS_CRYPTA_ID_FLATTEN_HITS_TABLE,
                        on_write=tables.OnWrite(
                            attributes={
                                'schema': yt_schemas.metrics_crypta_id_flattened_hits_schema,
                                'generate_date': today,
                            },
                        ),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.YsonTable(
                        'bar_crypta_id_flattened_hits.yson',
                        sc_config.BAR_CRYPTA_ID_FLATTEN_HITS_TABLE,
                        on_write=tables.OnWrite(
                            attributes={
                                'schema': yt_schemas.bar_crypta_id_flattened_hits_schema,
                                'generate_date': today,
                            },
                        ),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.YsonTable(
                        'crypta_id_metrica_browser_visitor_counter.yson',
                        sc_config.CRYPTA_ID_METRICA_BROWSER_VISITOR_COUNTER_TABLE,
                        on_write=tables.OnWrite(
                            attributes={
                                'schema': yt_schemas.crypta_id_metrica_browser_visitor_counter_schema,
                                'generate_date': today,
                            },
                        ),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'site_dict.yson',
                        sc_config.SITE_DICT_TABLE,
                        yt_schemas.site_dict_schema,
                    ),
                    tests.Exists(),
                ),
                (
                    tables.YsonTable(
                        'id_to_crypta_id.yson',
                        sc_config.ID_TO_CRYPTA_ID_TABLE,
                        on_write=tables.OnWrite(
                            attributes={
                                'schema': yt_schemas.id_to_crypta_id_schema,
                            },
                            sort_by=['id', 'id_type'],
                        ),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    crypta_id_user_data_table('crypta_id_user_data.yson', lal_config.FOR_DESCRIPTION_BY_CRYPTAID_TABLE),
                    tests.TableIsNotChanged(),
                ),
                (
                    get_segment_dict(sc_config.DSSM_LAL_MODEL_SEGMENT_DICT_FILE),
                    tests.Exists(),
                ),
                (
                    get_dssm_lal_model(sc_config.DSSM_LAL_MODEL_APPLIER_FILE, mocked_sandbox_dssm_lookalike_model),
                    tests.Exists(),
                ),
                (
                    tables.YsonTable(
                        'centroids.yson',
                        sc_config.CENTROIDS_TABLE,
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
                        'sites.yson',
                        sc_config.CLUSTERING_TABLE,
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
                    tables.YsonTable(
                        'segments_with_info.yson',
                        utils.get_segments_with_info_table_path(sc_config.SITES_UPDATE_STAGES_DIR),
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
                (
                    tables.YsonTable(
                        'site_dict.yson',
                        sc_config.SITE_DICT_TABLE,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
                (
                    tables.YsonTable(
                        'segments_stats.yson',
                        utils.get_segments_stats_table_path(sc_config.SITES_UPDATE_STAGES_DIR),
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
                (
                    test_utils.yson_table_with_float_values(
                        'segments_vectors_with_info.yson',
                        utils.get_segments_vectors_with_info_table_path(sc_config.SITES_UPDATE_STAGES_DIR),
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
                (
                    test_utils.yson_table_with_float_values(
                        'centroids.yson',
                        sc_config.CENTROIDS_TABLE,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
                (
                    test_utils.yson_table_with_float_values(
                        'sites.yson',
                        sc_config.CLUSTERING_TABLE,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
                (
                    test_utils.yson_table_with_float_values(
                        'daily_sites.today.yson',
                        os.path.join(sc_config.DAILY_CLUSTERING_DIR, today),
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
            ],
        )
