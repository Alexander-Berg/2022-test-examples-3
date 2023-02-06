import functools

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.prism.lib.config import config
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.offline_weighting.lib import (
    lookalike_samples,
    table_paths,
)


def test_lookalike_samples_get(custom_output_dir, yt_client, yql_client, date):
    resolved_table_paths = table_paths.resolve(custom_output_dir, date)

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            lookalike_samples.get_by_date,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
            custom_output_dir=custom_output_dir,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='taxi_user_profile.yson',
                    cypress_path=config.TAXI_USER_PROFILE_TABLE,
                    schema=schemas.taxi_user_profile_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='tx_lavka.yson',
                    cypress_path=config.TX_LAVKA_TABLE,
                    schema=schemas.tx_lavka_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='profiles.yson',
                    cypress_path=config.YANDEXUID_EXPORT_PROFILES_14_DAYS_TABLE,
                    schema=schemas.yandexuid_export_profiles_14_days_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='yandexuid_crypta_id.yson',
                    cypress_path=config.YANDEXUID_CRYPTAID_MATCHING_TABLE,
                    schema=schemas.matching_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='indevice_yandexuid.yson',
                    cypress_path=config.INDEVICE_YANDEXUID_MATCHING_TABLE,
                    schema=schemas.indevice_yandexuid_matching,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'lookalike_samples.yson',
                    resolved_table_paths['lookalike_samples'],
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
