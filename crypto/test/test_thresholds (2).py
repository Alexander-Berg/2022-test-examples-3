import functools
import os
import tempfile

import mock
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.rt_socdem.lib.python.model.config import config
from crypta.rt_socdem.services.training.lib import (
    distribution,
    thresholds,
)


def get_schema_by_num_classes(num_classes):
    return [{'name': 'Probability:Class={}'.format(i), 'type': 'double'} for i in range(num_classes)]


def test_thresholds_get_pool(yt_client, yql_client, date, get_table_with_beh_profile, mock_sandbox_server):
    features_mapping_table = tables.get_yson_table_with_schema(
        file_path='features_mapping_table.yson',
        cypress_path='//home/inputs/features_mapping_table',
        schema=[
            {'name': 'feature_index', 'type': 'uint64'},
            {'name': 'description', 'type': 'string'},
            {'name': 'feature', 'type': 'string'},
        ],
    )

    chevent_log_table = tables.get_yson_table_with_schema(
        file_path='chevent_log.yson',
        cypress_path=os.path.join(config.CHEVENT_LOG_DIR, date),
        schema=[
            {'name': 'uniqid', 'type': 'uint64'},
            {'name': 'cryptaidv2', 'type': 'uint64'},
            {'name': 'fraudbits', 'type': 'uint64'},
            {'name': 'hitlogid', 'type': 'uint64'},
            {'name': 'placeid', 'type': 'int64'},
            {'name': 'regionid', 'type': 'int64'},
        ],
    )

    beh_hit_log_table = get_table_with_beh_profile(
        file_path='beh_hit_log.yson',
        cypress_path=config.BEH_HIT_LOG_TABLE.format(date),
        schema=[
            {'name': 'ProfileDump', 'type': 'string'},
            {'name': 'HitLogID', 'type': 'uint64'},
        ],
        beh_profile_field='ProfileDump',
    )

    socdem_types = config.SOCDEM_TYPES
    pools_dir = '//outputs/pools_dir'
    pools = [tables.YsonTable(
        file_path='thresholds_pool_{}.yson'.format(socdem_type),
        cypress_path=os.path.join(pools_dir, socdem_type),
        yson_format='pretty',
    ) for socdem_type in socdem_types]

    with mock.patch.object(config, 'BIGB_SAMPLING_RATE', 1.0):
        return tests.yt_test_func(
            yt_client=yt_client,
            func=functools.partial(
                thresholds.get_pool,
                yt_client=yt_client,
                yql_client=yql_client,
                yesterday=date,
                pools_dir=pools_dir,
                features_mapping_table_path=features_mapping_table.cypress_path,
                socdem_types=socdem_types,
            ),
            data_path=yatest.common.test_source_path('data'),
            return_result=False,
            input_tables=[
                (features_mapping_table, tests.TableIsNotChanged()),
                (beh_hit_log_table, tests.TableIsNotChanged()),
                (chevent_log_table, tests.TableIsNotChanged()),
            ],
            output_tables=[(pool, tests.Diff()) for pool in pools],
        )


def test_thresholds_find(yt_client, yql_client, date):
    predictions_on_pools_dir = '//outputs/predictions_on_pools_dir'
    socdem_types = config.SOCDEM_TYPES

    segments_proba_tables = [tables.get_yson_table_with_schema(
        file_path='{}_proba.yson'.format(socdem_type),
        cypress_path=os.path.join(os.path.join(predictions_on_pools_dir, socdem_type)),
        schema=get_schema_by_num_classes(len(config.SEGMENT_NAMES_BY_SOCDEM_TYPE[socdem_type])),
    ) for socdem_type in socdem_types]

    thresholds_json_yt_output = files.YtFile(
        file_path='thresholds.json',
        cypress_path='//outputs/thresholds.json',
    )

    thresholds_datalens = tables.YsonTable(
        file_path='thresholds.yson',
        cypress_path=config.DATALENS_REALTIME_SOCDEM_CLASSIFICATION_THRESHOLDS_TABLE,
        yson_format='pretty',
    )

    with tempfile.NamedTemporaryFile() as json_local_file_output_file:
        return tests.yt_test_func(
            yt_client=yt_client,
            func=functools.partial(
                thresholds.find,
                yt_client=yt_client,
                yql_client=yql_client,
                date_for_metrics=date,
                predictions_on_pools_dir=predictions_on_pools_dir,
                socdem_types=socdem_types,
                json_yt_output=thresholds_json_yt_output.cypress_path,
                json_local_file_output=json_local_file_output_file.name,
            ),
            data_path=yatest.common.test_source_path('data'),
            return_result=False,
            input_tables=[
                (segments_proba_table, tests.TableIsNotChanged()) for segments_proba_table in segments_proba_tables
            ],
            output_tables=[
                (thresholds_json_yt_output, tests.Diff()),
                (thresholds_datalens, tests.Diff()),
            ],
        )


def test_distribution_send_metrics(yt_client, yql_client, date):
    predictions = [tables.get_yson_table_with_schema(
        file_path='{}_proba.yson'.format(socdem_type),
        cypress_path=os.path.join(config.THRESHOLDS_PREDICTIONS_ON_POOLS_DIR, socdem_type),
        schema=get_schema_by_num_classes(len(config.SEGMENT_NAMES_BY_SOCDEM_TYPE[socdem_type])),
    ) for socdem_type in ['income_segment', 'age_segment']]

    thresholds_yt_file = files.YtFile(
        file_path='thresholds.json',
        cypress_path=config.THRESHOLDS_FILE,
    )

    distribution_metrics = tables.YsonTable(
        file_path='distribution_metrics.yson',
        cypress_path=config.DATALENS_REALTIME_SOCDEM_DISTRIBUTIONS_TABLE,
        yson_format='pretty',
    )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            distribution.send_metrics,
            yt_client=yt_client,
            yql_client=yql_client,
            date_for_metrics=date,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (table, tests.TableIsNotChanged()) for table in predictions
        ] + [
            (thresholds_yt_file, tests.Exists()),
        ],
        output_tables=[
            (distribution_metrics, tests.Diff()),
        ],
    )
