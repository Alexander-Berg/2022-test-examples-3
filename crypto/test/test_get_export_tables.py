from collections import defaultdict
import json

from ads.bsyeti.libs.log_protos import crypta_profile_pb2
from google.protobuf import json_format
import mock
import pytest
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.runners.export_profiles.lib.export import get_logbroker_export_table
from crypta.profile.utils.config import config


def collector_row_transformer(row):
    user_segments = crypta_profile_pb2.TCryptaLog()
    user_segments.ParseFromString(row['value'])
    row['value'] = json.loads(json_format.MessageToJson(user_segments))
    return row


def get_outputs(task):
    return [
        (tables.YsonTable(
            'logbroker.yson',
            task.output().table,
            yson_format='pretty',
        ), tests.Diff()),
        (tables.YsonTable(
            'collector.yson',
            task.collector_path,
            on_read=tables.OnRead(row_transformer=collector_row_transformer),
            yson_format='pretty',
        ), tests.Diff()),
    ]


@pytest.mark.parametrize(
    'not_exported_segments',
    [
        defaultdict(set, {546: {1305, 1660}}),
        defaultdict(set),
    ],
    ids=['empty_lal_internal', 'without_not_exported_segments'],
)
def test_get_export_tables(yt_stuff, lb_patched_config, not_exported_segments, date):
    config.CRYPTA_YT_PROXY = yt_stuff.get_server()
    yt_client = yt_stuff.get_yt_client()

    task = get_logbroker_export_table.GetExportTables(date=date)

    with mock.patch("crypta.profile.utils.api.segments.get_not_exported_segments",
                    return_value=not_exported_segments), \
            mock.patch("crypta.profile.utils.api.segments.get_trainable_segments",
                       return_value={'1660'}), \
            mock.patch("crypta.profile.runners.export_profiles.lib.export.get_logbroker_export_table.get_trainable_segments_priorities",
                       return_value={}):
        return tests.yt_test_func(
            yt_client=yt_client,
            func=task.run,
            data_path=yatest.common.test_source_path('data'),
            input_tables=[
                (tables.YsonTable(
                    'yandexuid_profiles.yson',
                    task.input()['Vectors'].table,
                    on_write=tables.OnWrite(attributes={
                        task.input()['Vectors'].attribute_name: task.input()['Vectors'].attribute_value,
                    }),
                ), tests.TableIsNotChanged()),
            ],
            output_tables=get_outputs(task),
        )


def test_get_crypta_id_export_tables(local_yt, patch_config, date):
    task = get_logbroker_export_table.GetCryptaIdExportTables(date=date)

    with mock.patch(
        "crypta.profile.runners.export_profiles.lib.export.get_logbroker_export_table.get_trainable_segments_priorities",
        return_value={}
    ):

        return tests.yt_test_func(
            yt_client=local_yt.get_yt_client(),
            func=task.run,
            data_path=yatest.common.test_source_path('data'),
            input_tables=[
                (tables.YsonTable('cryptaid_profiles.yson', task.input()['DailyExport']['daily_export'].table), tests.TableIsNotChanged()),
            ],
            output_tables=get_outputs(task),
        )


def test_get_shortterm_interests_export_tables(local_yt, patch_config):
    task = get_logbroker_export_table.GetShorttermInterestsExportTables(timestamp="1600000000")

    with mock.patch(
        "crypta.profile.runners.export_profiles.lib.export.get_logbroker_export_table.get_trainable_segments_priorities",
        return_value={}
    ):
        return tests.yt_test_func(
            yt_client=local_yt.get_yt_client(),
            func=task.run,
            data_path=yatest.common.test_source_path('data'),
            input_tables=[
                (tables.YsonTable('shortterm_interests.yson', task.input().table), tests.IsAbsent()),
            ],
            output_tables=[
                (tables.YsonTable(
                    'collector.yson',
                    task.output().table,
                    on_read=tables.OnRead(row_transformer=collector_row_transformer),
                    yson_format='pretty',
                ), tests.Diff()),
            ],
        )
