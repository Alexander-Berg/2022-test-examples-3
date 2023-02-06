import functools

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.prism.lib.config import config
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.training.lib import features_mapping


def test_compute(yt_client, yql_client, raw_train_sample_table, run_and_write_output_to_yt):
    features_mapping_file = files.YtFile(
        file_path='features_mapping.json',
        cypress_path='//features_mapping.json',
    )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            run_and_write_output_to_yt,
            func=functools.partial(
                features_mapping.compute,
                yt_client=yt_client,
                yql_client=yql_client,
            ),
            cypress_path=features_mapping_file.cypress_path,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (raw_train_sample_table, tests.TableIsNotChanged()),
            (
                tables.get_yson_table_with_schema(
                    file_path='bm_categories_description.yson',
                    cypress_path=config.BM_CATEGORIES_DESCRIPTION_TABLE,
                    schema=schemas.bm_categories_description_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='os_description.yson',
                    cypress_path=config.OS_DESCRIPTION_TABLE,
                    schema=schemas.os_description_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='features_mapping_table.yson',
                    cypress_path=config.FEATURES_MAPPING_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (features_mapping_file, tests.Diff()),
        ],
    )
