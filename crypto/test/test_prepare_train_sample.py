from functools import partial
import os

import pytest
import yatest.common

import crypta.lib.python.yql.client as yql_helpers
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

from crypta.lib.python.custom_ml import training_config
from crypta.lib.python.custom_ml.classification.prepare_train_sample import prepare_sample_by_puid
from crypta.lib.python.custom_ml.tools.utils import get_stderr_logger


def get_matching_tables():
    matching_tables = []
    for table_type in ['phone_md5', 'phone', 'gaid', 'yandexuid']:
        matching_tables.append(
            tables.get_yson_table_with_schema(
                'matching_{}.yson'.format(table_type),
                training_config.MATCHING_TABLE_TEMPLATE.format(table_type),
                schema_utils.yt_schema_from_dict({column: 'string' for column in ['id', 'id_type', 'target_id']}),
            )
        )

    matching_tables.append(
        tables.get_yson_table_with_schema(
            'duid_yandexuid_matching.yson',
            training_config.DUID_MATCHING,
            schema_utils.yt_schema_from_dict({column: 'uint64' for column in ['duid', 'yandexuid']}),
        )
    )

    return matching_tables


@pytest.mark.parametrize('table_type,table_columns', [
    (
        'inner_format_with_retro_date',
        ['id', 'id_type', 'retro_date', 'segment_name'],
    ),
    (
        'inner_format_without_retro_date',
        ['id', 'id_type', 'segment_name'],
    ),
    (
        'main_format_simple',
        ['phone', 'retro_date', 'target'],
    ),
    (
        'main_format_several_id_types',
        ['phone', 'GAID', 'ClientID', 'retro_date', 'target'],
    ),
], ids=[
    'inner_format_with_retro_date',
    'inner_format_without_retro_date',
    'main_format_simple',
    'main_format_several_id_types',
])
def test_input_tables(clean_local_yt, local_yt_and_yql_env, mock_sandbox_server_with_identifiers_udf, table_type, table_columns):
    os.environ.update(local_yt_and_yql_env)
    yt_client = clean_local_yt.get_yt_client()
    yql_client = yql_helpers.create_yql_client(
        yt_proxy=clean_local_yt.get_server(),
        pool='fake_pool',
        token=os.getenv('YQL_TOKEN'),
    )

    common_path = '//home/crypta/custom_training'
    raw_sample_table = tables.get_yson_table_with_schema(
        'raw_sample_{}.yson'.format(table_type),
        os.path.join(common_path, 'raw_sample_{}'.format(table_type)),
        schema_utils.yt_schema_from_dict({column: 'string' for column in table_columns}),
    )
    sample_by_puid_table = tables.YsonTable(
        'sample_by_puid_table.yson',
        os.path.join(common_path, 'sample_by_puid_{}'.format(table_type)),
        yson_format='pretty',
    )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=partial(
            prepare_sample_by_puid,
            yt_client=yt_client,
            yql_client=yql_client,
            raw_sample_table=raw_sample_table.cypress_path,
            sample_by_puid_table=sample_by_puid_table.cypress_path,
            crypta_identifier_udf_url=mock_sandbox_server_with_identifiers_udf.get_udf_url(),
            logger=get_stderr_logger(),
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[(table, tests.TableIsNotChanged()) for table in get_matching_tables() + [raw_sample_table]],
        output_tables=[(sample_by_puid_table, tests.Diff())],
    )
