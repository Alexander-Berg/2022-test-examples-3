import functools
import os

import mock
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.prism.lib.config import config
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.training.lib import raw_train_sample


@mock.patch.object(config, 'BIGB_SAMPLING_RATE', 100.0)
def test_get(yt_client, yql_client, date, get_table_with_beh_profile, mock_sandbox_server):
    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            raw_train_sample.get,
            yt_client=yt_client,
            yql_client=yql_client,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='user_weights.yson',
                    cypress_path=os.path.join(config.PRISM_OFFLINE_USER_WEIGHTS_DIR, date),
                    schema=schemas.user_weights_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                get_table_with_beh_profile(
                    file_path='hit_log.yson',
                    cypress_path=os.path.join(config.BEH_HIT_HOUR_LOG_DIR, '{}T15:00:00'.format(date)),
                    schema=schemas.hit_log_schema,
                    beh_profile_field='ProfileDump',
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='raw_train_sample.yson',
                    cypress_path=config.RAW_TRAIN_SAMPLE_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
