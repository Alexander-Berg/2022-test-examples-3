import logging
import os

import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib.socdem_helpers import (
    test_utils,
)
from crypta.profile.lib.socdem_helpers.train_utils import training

logger = logging.getLogger(__name__)


def get_input_tables():
    on_write = tables.OnWrite(
        attributes={
            'schema': schema_utils.yt_schema_from_dict({
                'yandexuid': 'uint64',
                'income_segment': 'string',
                'vector': 'string',
            }),
        },
        row_transformer=test_utils.row_transformer_to_add_vector,
    )
    return {
        'plain_train_sample_table': tables.YsonTable('plain_socdem_train_sample.yson',
                                                     '//home/socdem/plain_train_sample',
                                                     on_write=on_write),
        'general_population_table': tables.YsonTable('general_population.yson',
                                                     '//home/socdem/general_population',
                                                     on_write=on_write),
    }


def test_weights_calculation(yt_stuff):
    os.environ['YT_TOKEN'] = '__FAKE_YT_TOKEN__'
    yt_client = yt_stuff.get_yt_client()

    input_tables = get_input_tables()
    with yt_client.TempTable() as train_sample_with_weights_table:
        tests.yt_test_func(
            yt_client=yt_client,
            func=lambda: training.add_weights_to_training_sample(
                yt_client=yt_client,
                sample_without_weights=input_tables['plain_train_sample_table'].cypress_path,
                sample_with_weights=train_sample_with_weights_table,
                general_population_table=input_tables['general_population_table'].cypress_path,
                logger=logger,
                is_mobile=False,
                random_seed=0,
            ),
            data_path=yatest.common.test_source_path('data'),
            return_result=False,
            input_tables=[(table, tests.TableIsNotChanged()) for table in input_tables.values()],
        )

        for row in yt_client.read_table(train_sample_with_weights_table):
            assert 0 <= row['income_segment_weight'] <= 1, \
                'Weight for income should be defined and should be in range [0, 1]'

        assert yt_client.row_count(train_sample_with_weights_table) > 5, 'Sample with weights should not be empty'
