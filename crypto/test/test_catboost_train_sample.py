import functools

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.prism.lib.config import config
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.training.lib import catboost_train_sample


def test_prepare(yt_client, raw_train_sample_table):
    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            catboost_train_sample.prepare,
            yt_client=yt_client,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='features_mapping_table.yson',
                    cypress_path=config.FEATURES_MAPPING_TABLE,
                    schema=schemas.feature_mapping_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                raw_train_sample_table,
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='column_description.yson',
                    cypress_path=config.CATBOOST_COLUMN_DESCRIPTION_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    file_path='catboost_train.yson',
                    cypress_path=config.CATBOOST_TRAIN_SAMPLE_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    file_path='catboost_val.yson',
                    cypress_path=config.CATBOOST_VAL_SAMPLE_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
