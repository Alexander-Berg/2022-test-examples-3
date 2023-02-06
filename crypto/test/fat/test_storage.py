import datetime
import json

from google.protobuf import json_format
import pytest
import yatest.common
import yt.wrapper as yt

import crypta.audience.lib.storage as tasks
from crypta.audience.proto import storage_pb2
from crypta.lib.python.bt import test_helpers
from crypta.lib.python.bt.conf import conf
from crypta.lib.python.yql import proto_field
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)


def user_segments_row_transformer(row):
    user_segments = storage_pb2.TUserSegments()
    user_segments.ParseFromString(row["UserSegments"])
    row["UserSegments"] = json.loads(json_format.MessageToJson(user_segments))
    return row


SORT_BY = ("Id", "IdType")


def sorted_on_write():
    return tables.OnWrite(sort_by=SORT_BY)


def canon_sorted_test():
    return tests.Diff(), tests.AttrEquals("sorted_by", SORT_BY)


def sample_log_test():
    return canon_sorted_test() + (tests.ExpirationTime(datetime.timedelta(days=conf.proto.Options.Storage.SampleLogTTLDays)),)


def sample_collector_test():
    return (tests.Diff(), tests.ExpirationTime(datetime.timedelta(days=conf.proto.Options.Storage.SampleLogTTLDays)))


@pytest.mark.parametrize("ids_meta_exists", [False, True])
@pytest.mark.parametrize("overwrite_all", [False, True])
def test_update_full_yandexuid(prepared_local_yt, frozen_time, overwrite_all, ids_meta_exists):
    yt_client = prepared_local_yt.get_yt_client()

    input_tables = [
        (tables.YsonTable("for_full1.yson", yt.ypath_join(conf.paths.storage.for_full, "1588000000")), tests.IsAbsent()),
        (tables.YsonTable("for_full2.yson", yt.ypath_join(conf.paths.storage.for_full, "1589000000")), tests.IsAbsent()),

        (tables.YsonTable("full.yson", conf.paths.storage.full_yandexuid, on_write=sorted_on_write()), None),
        (tables.YsonTable("segments_full.yson", conf.paths.storage.meta.segments.full_yandexuid), None),
    ]

    if ids_meta_exists:
        input_tables.append((tables.YsonTable("ids_full.yson", conf.paths.storage.meta.ids.full_yandexuid, on_write=sorted_on_write()), None))

    return tests.yt_test_func(
        yt_client=yt_client,
        func=lambda: test_helpers.execute(tasks.UpdateFullYandexuid(overwrite_all=str(overwrite_all).lower())),
        data_path=yatest.common.test_source_path("data/update_full_yandexuid"),
        input_tables=input_tables,
        output_tables=[
            (tables.YsonTable("full.yson", conf.paths.storage.full_yandexuid, yson_format="pretty"), canon_sorted_test()),
            (tables.YsonTable("segments_full.yson", conf.paths.storage.meta.segments.full_yandexuid, yson_format="pretty"), tests.Diff()),
            (tables.YsonTable("ids_full.yson", conf.paths.storage.meta.ids.full_yandexuid, yson_format="pretty"), canon_sorted_test()),
            (tables.YsonTable(
                "to_bigb.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.to_bigb.yandexuid, int(frozen_time)),
                on_read=tables.OnRead(row_transformer=user_segments_row_transformer),
                yson_format="pretty",
            ), (
                tests.Diff(),
                tests.ExpirationTime(datetime.timedelta(days=conf.proto.Options.Storage.BigbTTLDays))
            )),
            (tables.YsonTable(
                "sample_log.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.sample_log.yandexuid, int(frozen_time)),
                yson_format="pretty"
            ), sample_log_test()),
            (tables.YsonTable(
                "sample_to_bigb.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.sample_collector.yandexuid, int(frozen_time)),
                on_read=tables.OnRead(row_transformer=user_segments_row_transformer),
                yson_format="pretty",
            ), sample_collector_test()),
        ],
    )


def test_update_full_devices(prepared_local_yt, frozen_time):
    yt_client = prepared_local_yt.get_yt_client()
    matching_schema = schema_utils.get_strict_schema([
        {"name": "Gaid", "required": False, "type": "string"},
        {"name": "Hash", "required": False, "type": "string"},
        {"name": "Idfa", "required": False, "type": "string"},
    ])

    return tests.yt_test_func(
        yt_client=yt_client,
        func=lambda: test_helpers.execute(tasks.UpdateFullDevices()),
        data_path=yatest.common.test_source_path("data/update_full_devices"),
        input_tables=[
            (tables.YsonTable("for_full1.yson", yt.ypath_join(conf.paths.storage.device_queue, "1588000000")), tests.IsAbsent()),
            (tables.YsonTable("for_full2.yson", yt.ypath_join(conf.paths.storage.device_queue, "1589000000")), tests.IsAbsent()),

            (tables.YsonTable("full_devices.yson", conf.paths.storage.full_devices, on_write=sorted_on_write()), None),
            (tables.YsonTable("segments_full.yson", conf.paths.storage.meta.segments.full_devices), None),
            (tables.YsonTable("ids_full.yson", conf.paths.storage.meta.ids.full_devices, on_write=sorted_on_write()), None),
            (tables.get_yson_table_with_schema("device_hashes.yson", conf.paths.audience.matching.device_hashes, matching_schema), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable("full_devices.yson", conf.paths.storage.full_devices, yson_format="pretty"), tests.Diff()),
            (tables.YsonTable("segments_full.yson", conf.paths.storage.meta.segments.full_devices, yson_format="pretty"), tests.Diff()),
            (tables.YsonTable("ids_full.yson", conf.paths.storage.meta.ids.full_devices, yson_format="pretty"), tests.Diff()),
            (
                tables.YsonTable(
                    "to_bigb.yson",
                    tasks.get_ts_path(yt_client, conf.paths.storage.to_bigb.devices, int(frozen_time)),
                    on_read=tables.OnRead(row_transformer=user_segments_row_transformer),
                    yson_format="pretty",
                ),
                (
                    tests.Diff(),
                    tests.ExpirationTime(datetime.timedelta(days=conf.proto.Options.Storage.BigbTTLDays))
                )
            ),
            (tables.YsonTable(
                "sample_log.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.sample_log.devices, int(frozen_time)),
                yson_format="pretty"
            ), sample_log_test()),
            (tables.YsonTable(
                "sample_to_bigb.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.sample_collector.devices, int(frozen_time)),
                on_read=tables.OnRead(row_transformer=user_segments_row_transformer),
                yson_format="pretty",
            ), sample_collector_test()),
        ],
    )


@pytest.mark.parametrize("overwrite_all", [False, True])
def test_update_full_crypta_id(prepared_local_yt, frozen_time, overwrite_all):
    yt_client = prepared_local_yt.get_yt_client()

    return tests.yt_test_func(
        yt_client=yt_client,
        func=lambda: test_helpers.execute(tasks.UpdateFullCryptaId(overwrite_all=str(overwrite_all).lower())),
        data_path=yatest.common.test_source_path("data/update_full_crypta_id"),
        input_tables=[
            (tables.YsonTable("queue_by_email_phone_1.yson", yt.ypath_join(conf.paths.storage.email_phone_queue, "1588000000")), tests.IsAbsent()),
            (tables.YsonTable("queue_by_email_phone_2.yson", yt.ypath_join(conf.paths.storage.email_phone_queue, "1589000000")), tests.IsAbsent()),

            (tables.YsonTable("queue_by_crypta_id_1.yson", yt.ypath_join(conf.paths.storage.crypta_id_queue, "1588000000")), tests.IsAbsent()),
            (tables.YsonTable("queue_by_crypta_id_2.yson", yt.ypath_join(conf.paths.storage.crypta_id_queue, "1589000000")), tests.IsAbsent()),

            (tables.YsonTable("private_full_crypta_id.yson", conf.paths.storage.public_full_crypta_id, on_write=sorted_on_write()), None),
            (tables.YsonTable("meta_segments_full_crypta_id.yson", conf.paths.storage.meta.segments.full_crypta_id), None),
            (tables.YsonTable("ids_full.yson", conf.paths.storage.meta.ids.full_crypta_id, on_write=sorted_on_write()), None),
            (tables.YsonTable("matching_emails.yson", conf.paths.audience.matching.cryptaid.emails), tests.TableIsNotChanged()),
            (tables.YsonTable("matching_phones.yson", conf.paths.audience.matching.cryptaid.phones), tests.TableIsNotChanged()),
            (tables.YsonTable("private_full_crypta_id.yson", conf.paths.storage.full_crypta_id, on_write=sorted_on_write()), None),
        ],
        output_tables=[
            (tables.YsonTable("private_full_crypta_id.yson", conf.paths.storage.full_crypta_id, yson_format="pretty"), tests.Diff()),
            (tables.YsonTable("full_crypta_id.yson", conf.paths.storage.public_full_crypta_id, yson_format="pretty"), tests.Diff()),
            (tables.YsonTable("meta_segments_full_crypta_id.yson", conf.paths.storage.meta.segments.full_crypta_id, yson_format="pretty"), tests.Diff()),
            (tables.YsonTable("ids_full.yson", conf.paths.storage.meta.ids.full_crypta_id, yson_format="pretty"), tests.Diff()),

            (tables.YsonTable(
                "to_bigb.yson",
                yt.ypath_join(conf.paths.storage.to_bigb.crypta_id, datetime.datetime.fromtimestamp(int(frozen_time)).isoformat()),
                on_read=tables.OnRead(row_transformer=user_segments_row_transformer),
                yson_format="pretty",
            ), (
                tests.Diff(),
                tests.ExpirationTime(datetime.timedelta(days=conf.proto.Options.Storage.BigbTTLDays)),
            )),
            (tables.YsonTable(
                "sample_log.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.sample_log.crypta_id, int(frozen_time)),
                yson_format="pretty",
            ), sample_log_test()),
            (tables.YsonTable(
                "sample_to_bigb.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.sample_collector.crypta_id, int(frozen_time)),
                on_read=tables.OnRead(row_transformer=user_segments_row_transformer),
                yson_format="pretty",
            ), sample_collector_test()),
        ],
    )


def test_update_full_puid(prepared_local_yt, frozen_time):
    yt_client = prepared_local_yt.get_yt_client()

    return tests.yt_test_func(
        yt_client=yt_client,
        func=lambda: test_helpers.execute(tasks.UpdateFullPuid()),
        data_path=yatest.common.test_source_path("data/update_full_puid"),
        input_tables=[
            (tables.YsonTable("for_full1.yson", yt.ypath_join(conf.paths.storage.puid_queue, "1588000000")), tests.IsAbsent()),
            (tables.YsonTable("for_full2.yson", yt.ypath_join(conf.paths.storage.puid_queue, "1589000000")), tests.IsAbsent()),

            (tables.YsonTable("full.yson", conf.paths.storage.full_puid, on_write=sorted_on_write()), None),
            (tables.YsonTable("segments_full.yson", conf.paths.storage.meta.segments.full_puid), None),
            (tables.YsonTable("ids_full.yson", conf.paths.storage.meta.ids.full_puid, on_write=sorted_on_write()), None),
        ],
        output_tables=[
            (tables.YsonTable("full.yson", conf.paths.storage.full_puid, yson_format="pretty"), tests.Diff()),
            (tables.YsonTable("segments_full.yson", conf.paths.storage.meta.segments.full_puid, yson_format="pretty"), tests.Diff()),
            (tables.YsonTable("ids_full.yson", conf.paths.storage.meta.ids.full_puid, yson_format="pretty"), tests.Diff()),
            (tables.YsonTable(
                "to_bigb.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.to_bigb.puid, int(frozen_time)),
                on_read=tables.OnRead(row_transformer=user_segments_row_transformer),
                yson_format="pretty",
            ), (
                tests.Diff(),
                tests.ExpirationTime(datetime.timedelta(days=conf.proto.Options.Storage.BigbTTLDays)),
            )),
            (tables.YsonTable(
                "sample_log.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.sample_log.puid, int(frozen_time)),
                yson_format="pretty",
            ), sample_log_test()),
            (tables.YsonTable(
                "sample_to_bigb.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.sample_collector.puid, int(frozen_time)),
                on_read=tables.OnRead(row_transformer=user_segments_row_transformer),
                yson_format="pretty",
            ), sample_collector_test()),
        ],
    )


def test_upload_segment_priorities_to_sandbox(prepared_local_yt, mock_sandbox_client):
    def direct_input_table(filename):
        path = yt.ypath_join(conf.paths.storage.direct_current, "ppc:1/straight", filename.split(".")[0])
        return tables.YsonTable(filename, path), tests.TableIsNotChanged()

    tests.yt_test_func(
        yt_client=prepared_local_yt.get_yt_client(),
        func=lambda: test_helpers.execute(tasks.UploadSegmentPrioritiesToSandbox()),
        data_path=yatest.common.test_source_path("data/upload_segment_priorities_to_sandbox"),
        input_tables=[
            (tables.YsonTable("segment_info.yson", conf.paths.storage.meta.segments_info), tests.TableIsNotChanged()),
            (tables.YsonTable("segments_full_yandexuid.yson", conf.paths.storage.meta.segments.full_yandexuid), tests.TableIsNotChanged()),
            (tables.YsonTable("segments_full_crypta_id.yson", conf.paths.storage.meta.segments.full_crypta_id), tests.TableIsNotChanged()),
            (tables.YsonTable("segments_full_devices.yson", conf.paths.storage.meta.segments.full_devices), tests.TableIsNotChanged()),
            (tables.YsonTable("segments_simple.yson", conf.paths.audience.segments_simple_dyntable), tests.TableIsNotChanged()),
        ] + [
            direct_input_table(filename) for filename in (
                "bids_retargeting.yson", "campaigns.yson", "hierarchical_multipliers.yson",
                "retargeting_conditions.yson", "retargeting_goals.yson", "retargeting_multiplier_values.yson",
            )
        ],
    )
    return mock_sandbox_client.uploads


def test_update_limited(prepared_local_yt, frozen_time):
    return tests.yt_test_func(
        yt_client=prepared_local_yt.get_yt_client(),
        func=lambda: test_helpers.execute(tasks.UpdateLimited()),
        data_path=yatest.common.test_source_path("data/update_limited"),
        input_tables=[
            (tables.YsonTable("queue1.yson", yt.ypath_join(conf.paths.storage.queue, "1588000000")), tests.IsAbsent()),
            (tables.YsonTable("queue2.yson", yt.ypath_join(conf.paths.storage.queue, "1589000000")), tests.IsAbsent()),
            (tables.YsonTable("info1.yson", yt.ypath_join(conf.paths.storage.queue_segments_info, "1588000000")), tests.IsAbsent()),
            (tables.YsonTable("info2.yson", yt.ypath_join(conf.paths.storage.queue_segments_info, "1589000000")), tests.IsAbsent()),
            (tables.YsonTable("segments_info.yson", conf.paths.storage.meta.segments_info), None),
            (tables.YsonTable("segments_simple.yson", conf.paths.audience.segments_simple), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable("segments_info.yson", conf.paths.storage.meta.segments_info), tests.Diff()),
            (tables.YsonTable("yandexuid.yson", yt.ypath_join(conf.paths.storage.for_full, frozen_time)), tests.Diff()),
            (tables.YsonTable("crypta_id.yson", yt.ypath_join(conf.paths.storage.crypta_id_queue, frozen_time)), tests.Diff()),
        ],
    )


def test_dump_bigb_sample(prepared_local_yt, frozen_time):
    yt_client = prepared_local_yt.get_yt_client()

    return tests.yt_test_func(
        yt_client=yt_client,
        func=lambda: test_helpers.execute(tasks.DumpBigbSample()),
        data_path=yatest.common.test_source_path("data/dump_bigb_sample"),
        input_tables=[
            (tables.get_yson_table_with_schema(
                "full.yson",
                conf.paths.storage.public_full_yandexuid,
                schema=schema_utils.get_schema_from_proto(storage_pb2.TFullBinding),
            ), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable(
                "bigb_dump_log.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.bigb_sample_dump_log.yandexuid, int(frozen_time)),
                yson_format="pretty",
            ), tests.Diff()),
        ],
    )


def test_check_desync(prepared_local_yt, frozen_time, mock_solomon_server):
    bigb_dump_schema = schema_utils.get_schema_from_proto(storage_pb2.TBigbDump)
    sample_schema = schema_utils.get_schema_from_proto(storage_pb2.TFullBinding)
    yt_client = prepared_local_yt.get_yt_client()

    yt_results = tests.yt_test_func(
        yt_client=yt_client,
        func=lambda: test_helpers.execute(tasks.CheckDesync()),
        data_path=yatest.common.test_source_path("data/check_desync"),
        input_tables=[
            (tables.YsonTable(
                "to_bigb.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.sample_collector.yandexuid, int(frozen_time)),
                on_write=tables.OnWrite(
                    row_transformer=row_transformers.proto_dict_to_yson(storage_pb2.TUserSegmentsRow),
                    attributes=proto_field.get_attrs(storage_pb2.TUserSegmentsRow),
                ),
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "sample_log_1.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.sample_log.yandexuid, int(frozen_time)-1),
                sample_schema,
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "sample_log_2.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.sample_log.yandexuid, int(frozen_time)),
                sample_schema,
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "bigb_sample_dump_log_1.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.bigb_sample_dump_log.yandexuid, int(frozen_time)-1),
                bigb_dump_schema,
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "bigb_sample_dump_log_2.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.bigb_sample_dump_log.yandexuid, int(frozen_time)),
                bigb_dump_schema,
            ), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable(
                "check_desync.yson",
                tasks.get_ts_path(yt_client, conf.paths.storage.check_desync_log.yandexuid, int(frozen_time)),
                yson_format="pretty",
            ), tests.Diff()),
        ],
    )

    return {
        "solomon": mock_solomon_server.dump_push_requests(),
        "yt": yt_results,
    }
