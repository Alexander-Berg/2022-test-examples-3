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
from crypta.lookalike.services.top_common_lal_apps.lib import distances


def calculate_all_distances():
    distances.get_for_categories(nv_params=None)
    distances.get_for_apps(nv_params=None, promoted=True)
    distances.get_for_apps(nv_params=None, promoted=False)


def test_calculate_distances(yt_client, yql_client):
    output_tables = [
        tables.YsonTable(
            'lal_distances_categories.yson',
            mobile_config.CATEGORIES_LAL_DISTANCES,
            yson_format='pretty',
        ),
        tables.YsonTable(
            'lal_distances_promoted_apps.yson',
            mobile_config.PROMOTED_APPS_LAL_DISTANCES,
            yson_format='pretty',
        ),
        tables.YsonTable(
            'lal_distances_top_apps.yson',
            mobile_config.TOP_APPS_LAL_DISTANCES,
            yson_format='pretty',
        ),
    ]

    with mock.patch('crypta.lookalike.lib.python.utils.mobile_utils.get_yt_client', return_value=yt_client), \
            mock.patch('crypta.lookalike.lib.python.utils.mobile_utils.get_yql_client', return_value=yql_client), \
            mock.patch('crypta.lookalike.lib.python.utils.mobile_utils.check_date', return_value=False), \
            mock.patch('crypta.lookalike.lib.python.utils.mobile_utils.get_date_from_nv_parameters',
                       return_value='2021-08-31'):
        return tests.yt_test_func(
            yt_client=yt_client,
            func=calculate_all_distances,
            data_path=yatest.common.test_source_path('data'),
            return_result=False,
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        'categories_vectors.yson',
                        mobile_config.CATEGORIES_VECTORS,
                        yt_schemas.categories_vectors_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'users_dssm_vectors_apps.yson',
                        mobile_config.USERS_VECTORS,
                        schema_utils.yt_schema_from_dict(schemas.users_dssm_vectors_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'apps_dssm_vectors_apps.yson',
                        mobile_config.APPS_VECTORS,
                        schema_utils.yt_schema_from_dict(schemas.apps_dssm_vectors_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'apps_info_table.yson',
                        mobile_config.PROMOTED_APPS,
                        schema_utils.yt_schema_from_dict(schemas.apps_info_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'apps_info_table.yson',
                        mobile_config.TOP_COMMON_APPS,
                        schema_utils.yt_schema_from_dict(schemas.apps_info_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[(output_table, tests.Diff()) for output_table in output_tables],
        )
