import pytest
import yatest.common

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.runners.export_profiles.lib.export import upload_profiles_to_logbroker


@pytest.mark.parametrize("id_type", ("yandexuid", "crypta_id"))
def test_upload_to_logbroker(id_type, local_yt, patch_config, logbroker_client, date):
    task = upload_profiles_to_logbroker.UploadProfilesToLogbroker(date=date, id_type=id_type)

    yt_results = tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=task.run,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (tables.YsonTable(
                'logbroker_profiles.yson',
                task.input()['Export'].table,
            ), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable('logs.yson', task.output().table, yson_format='pretty'), tests.Diff()),
        ],
    )

    return {
        "yt": yt_results,
        "logbroker": consumer_utils.read_all(logbroker_client.create_consumer(), timeout=30),
    }
