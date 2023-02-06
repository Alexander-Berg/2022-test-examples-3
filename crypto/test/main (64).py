import datetime
import logging
import os

import yatest.common

from crypta.lib.python import crypta_env
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.services.upload_direct_exports_tanker_names_to_yt import lib

logger = logging.getLogger(__name__)


def test_basic(local_yt, config_file, config, yt_stuff, frozen_time, lab_mock):
    local_yt_client = local_yt.get_yt_client()
    binary_path = yatest.common.binary_path(
        'crypta/profile/services/upload_direct_exports_tanker_names_to_yt/bin/'
        'crypta-profile-upload-direct-exports-tanker-names-to-yt'
    )

    # Use task class directly to get output and errors tables
    task = lib.UploadDirectExportsTankerKeysToYt(config=config, logger=None)

    return tests.yt_test(
        yt_client=local_yt_client,
        binary=binary_path,
        args=['--config', config_file],
        env={
            crypta_env.EnvNames.crypta_api_oauth: 'FAKE',
            'YT_TOKEN': 'FAKE',
        },
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (tables.YsonTable(
                'direct_crypta_goals.yson',
                config.DirectCryptaGoalsTable,
                yson_format='pretty',
            ), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable(
                'tanker_keys.yson',
                task.output_table,
                yson_format='pretty',
            ), tests.Diff()),
            (tables.YsonTable(
                'tanker_keys_replica.yson',
                task.output_table,
                yson_format='pretty',
            ), tests.Diff(yt_client=yt_stuff.get_yt_client())),
            (tables.YsonTable(
                f'errors_{os.path.basename(task.errors_table)}.yson',
                task.errors_table,
                yson_format='pretty',
            ), (
                tests.Diff(),
                tests.ExpirationTime(ttl=datetime.timedelta(days=config.ErrorsTtlDays)),
            )),
        ],
    )
