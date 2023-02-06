import functools
import os

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.prism.lib.config import config
from crypta.prism.services.training.lib import thresholds


def test_find(yt_client, yql_client, run_and_write_output_to_yt, raw_train_sample_table, date):
    thresholds_file = files.YtFile(
        file_path='thresholds.json',
        cypress_path='//thresholds.json',
    )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            run_and_write_output_to_yt,
            func=functools.partial(
                thresholds.find,
                yt_client=yt_client,
                yql_client=yql_client,
                date=date,
            ),
            cypress_path=thresholds_file.cypress_path,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (raw_train_sample_table, tests.TableIsNotChanged()),
            (
                tables.get_yson_table_with_schema(
                    file_path='model_prediction.yson',
                    cypress_path=config.MODEL_PREDICTIONS_TABLE,
                    schema=[
                        {'name': 'Probability:Class=0', 'type': 'double'},
                        {'name': 'Probability:Class=1', 'type': 'double'},
                        {'name': 'Probability:Class=2', 'type': 'double'},
                        {'name': 'Probability:Class=3', 'type': 'double'},
                        {'name': 'Probability:Class=4', 'type': 'double'},
                        {'name': 'crypta_id', 'type': 'string'},
                    ],
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='distribution_for_thresholds.yson',
                    cypress_path=config.DISTRIBUTION_FOR_THRESHOLDS_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    file_path='model_predictions_for_thresholds.yson',
                    cypress_path=config.MODEL_PREDICTIONS_FOR_THRESHOLDS_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    file_path='classification_thresholds.yson',
                    cypress_path=os.path.join(config.DATALENS_REALTIME_PRISM_DIR, 'classification_thresholds'),
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    file_path='train_distribution.yson',
                    cypress_path=os.path.join(config.DATALENS_REALTIME_PRISM_DIR, 'train_distribution'),
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (thresholds_file, tests.Diff()),
        ],
    )
