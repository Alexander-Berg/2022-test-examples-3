import mock
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.lookalike.lib.python.utils import (
    fields,
    yt_schemas,
)
from crypta.lookalike.lib.python.utils.mobile_config import config as mobile_config
from crypta.lookalike.services.top_common_lal_apps.lib import recommendations


def calculate_all_recommendations():
    recommendations.get_for_apps_and_categories(nv_params=None)
    recommendations.get_for_segments(nv_params=None)


def test_calculate_recommendations(yt_client, yql_client):
    recommendations_output_table = tables.YsonTable(
        'apps_recommendations.yson',
        mobile_config.NEW_RECOMMENDATIONS_TABLE,
        yson_format='pretty',
    )

    installers_output_table = tables.YsonTable(
        'installers_by_ad_scores.yson',
        mobile_config.NEW_INSTALLS_BY_AD_SCORES,
        yson_format='pretty',
    )

    with mock.patch('crypta.lookalike.lib.python.utils.mobile_utils.get_yt_client', return_value=yt_client), \
            mock.patch('crypta.lookalike.lib.python.utils.mobile_utils.get_yql_client', return_value=yql_client), \
            mock.patch('crypta.lookalike.lib.python.utils.mobile_utils.check_date', return_value=False), \
            mock.patch('crypta.lookalike.lib.python.utils.mobile_utils.get_date_from_nv_parameters',
                       return_value='2021-08-31'):
        return tests.yt_test_func(
            yt_client=yt_client,
            func=calculate_all_recommendations,
            data_path=yatest.common.test_source_path('data'),
            return_result=False,
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        'lal_distances_categories.yson',
                        mobile_config.CATEGORIES_LAL_DISTANCES,
                        schema_utils.yt_schema_from_dict(
                            yt_schemas.lal_distances_categories_schema(),
                            sort_by=[fields.id_type, fields.cryptaId],
                        ),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'lal_distances_promoted_apps.yson',
                        mobile_config.PROMOTED_APPS_LAL_DISTANCES,
                        schema_utils.yt_schema_from_dict(
                            yt_schemas.lal_distances_apps_schema(),
                            sort_by=[fields.id_type, fields.cryptaId],
                        ),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'lal_distances_top_apps.yson',
                        mobile_config.TOP_APPS_LAL_DISTANCES,
                        schema_utils.yt_schema_from_dict(
                            yt_schemas.lal_distances_apps_schema(),
                            sort_by=[fields.id_type, fields.cryptaId],
                        ),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'lal_distances_installers_by_ad.yson',
                        mobile_config.INSTALLERS_BY_AD_LAL_DISTANCES,
                        schema_utils.yt_schema_from_dict(
                            yt_schemas.lal_distances_segments_schema(),
                            sort_by=[fields.id_type, fields.cryptaId],
                        ),
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[(recommendations_output_table, tests.Diff()), (installers_output_table, tests.Diff())],
        )
