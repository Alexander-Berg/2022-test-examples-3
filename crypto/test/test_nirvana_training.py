import pytest
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib.socdem_helpers import test_utils
from crypta.profile.services.train_socdem_models.lib.common import utils


class StdoutTest(tests.YtTest):
    def teardown(self, stdout_filename, yt_stuff):
        return [yatest.common.canonical_file(stdout_filename, local=True)]


@pytest.fixture
def context(yt_server):
    return {
        'yt_proxy': yt_server,
        'yt_pool': 'fake_pool',
        'is_mobile': 'False',
        'custom_folder_path': '//home/crypta/socdem_training/socdem',
        'environment': 'local_testing',
        'socdem_type': 'age',
        'job_name': 'calculate_and_send_metrics',
        'timestamp': '1627313005',
    }


def test_calculate_and_send_metrics(yt_client_for_binary, context, nirvana_operation_environ_for_binary):
    config_for_training = utils.get_proto_config(context)
    env = nirvana_operation_environ_for_binary
    env.update(context)

    return tests.yt_test(
        yt_client=yt_client_for_binary,
        binary=yatest.common.binary_path('crypta/profile/services/train_socdem_models/bin/train-socdem-models'),
        args=[],
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    'catboost_predictions.yson',
                    config_for_training.PathsInfo.PredictionsBySocdem,
                    schema_utils.yt_schema_from_dict(
                        test_utils.get_predictions_table_schema(config_for_training.SocdemType),
                    ),
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        env=env,
        stdout_fname=yatest.common.test_output_path('stdout.txt'),
        stdout_test=StdoutTest(),
    )
