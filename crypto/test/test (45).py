import random

import pytest
from yt import yson

from crypta.lib.proto.identifiers import (
    ext_pb2,
    id_type_pb2,
)
from crypta.lib.proto.user_data import user_data_stats_pb2
from crypta.lib.python import native_yt
from crypta.lib.python.identifiers import identifiers
from crypta.siberia.bin.common.create_user_set_from_sample_reducer import py
from crypta.siberia.bin.common.describing.mode.python import describing_mode
from library.python import bloom


GROUP_ID_COLUMN = "group"
ID_COLUMN = "id"
ID_TYPE_COLUMN = "id_type"
ID_TYPE = id_type_pb2.EIdType.YANDEXUID
SOURCE = "//source"
DESTINATION = "//destination"
USER_SET_ID = "user_set_id"
ACTUAL_SEGMENT_SIZE = "actual_segment_size"
FILTER = "filter"
DESCRIBING_MODE = describing_mode.FAST
EXPERIMENT = "by_crypta_id"


def input_schema(has_id_type):
    schema = [
        {"name": GROUP_ID_COLUMN, "type": "string", "required": True},
        {"name": ID_COLUMN, "type": "string", "required": True},
    ]
    if has_id_type:
        schema.append({"name": ID_TYPE_COLUMN, "type": "string", "required": True})

    schema = yson.YsonList(schema)
    schema.attributes["strict"] = True
    return schema


def get_id_type_name(id_type_int):
    return id_type_pb2.EIdType.DESCRIPTOR.values_by_number[id_type_int].GetOptions().Extensions[ext_pb2.Name]


def id_to_row(group_id, use_id_type_column):
    result = {GROUP_ID_COLUMN: group_id, ID_COLUMN: identifiers.Yandexuid.next()}
    if use_id_type_column:
        result[ID_TYPE_COLUMN] = get_id_type_name(identifiers.Yandexuid.ID_TYPE)
    return result


def group_rows_generator(group_id, yuid_count, use_id_type_column):
    return (id_to_row(group_id, use_id_type_column) for _ in xrange(yuid_count))


def rows_generator(cases, use_id_type_column):
    for case in cases:
        for row in group_rows_generator(*case, use_id_type_column=use_id_type_column):
            yield row


def run_map_reduce(yt_stuff, rows, sample_size, tvm_api, tvm_src_id, tvm_dst_id, mock_siberia_core_server, use_id_type_column, skip_rate=0.0):
    user_data_stats_options = user_data_stats_pb2.TUserDataStatsOptions()
    for row in rows:
        filter_options = user_data_stats_options.Segments[row[GROUP_ID_COLUMN]].FilterOptions
        filter_options.Capacity = 1000
        filter_options.ErrorRate = 1e-3

    user_data_stats_options.SamplingOptions.SkipRate = skip_rate

    yt_client = yt_stuff.get_yt_client()
    yt_client.write_table(yt_client.TablePath(SOURCE, schema=input_schema(use_id_type_column)), rows)

    with yt_client.Transaction() as tx:
        py.create_user_set_from_sample(
            yt_client,
            lambda **kwargs: native_yt.run_native_map_reduce_with_combiner(
                proxy=yt_stuff.get_server(),
                transaction=str(tx.transaction_id),
                token="FAKE",
                **kwargs
            ),
            lambda **kwargs: native_yt.run_native_map(
                proxy=yt_stuff.get_server(),
                transaction=str(tx.transaction_id),
                token="FAKE",
                **kwargs
            ),
            source=SOURCE,
            destination=DESTINATION,
            group_id_column=GROUP_ID_COLUMN,
            id_column=ID_COLUMN,
            id_type=None if use_id_type_column else ID_TYPE,
            id_type_column=ID_TYPE_COLUMN if use_id_type_column else None,
            sample_size=sample_size,
            tvm_settings={
                "source_id": tvm_src_id,
                "destination_id": tvm_dst_id,
                "secret": tvm_api.get_secret(tvm_src_id),
                "host": tvm_api.host,
                "port": tvm_api.port,
            },
            siberia_host=mock_siberia_core_server.host,
            siberia_port=mock_siberia_core_server.port,
            user_data_stats_options=user_data_stats_options,
            describing_mode=DESCRIBING_MODE,
            experiment=EXPERIMENT,
        )


def check_commands(input_rows, yt_client, mock_siberia_core_server, use_id_type_column):
    output_table = {
        row[USER_SET_ID]: row
        for row in yt_client.read_table(DESTINATION)
    }

    result = []

    for command in mock_siberia_core_server.commands:
        user_set_id = command[USER_SET_ID]
        ids = command["ids"]
        output_row = output_table[user_set_id]
        group = output_row[GROUP_ID_COLUMN]

        filter_proto = user_data_stats_pb2.TUserDataStats.TFilter()
        filter_proto.ParseFromString(output_row[FILTER])
        bloom_filter = bloom.loads(filter_proto.BloomFilter)

        result.append({GROUP_ID_COLUMN: group, "count": len(ids)})

        for id in ids:
            row = {GROUP_ID_COLUMN: group, ID_COLUMN: id["Value"]}
            if use_id_type_column:
                row[ID_TYPE_COLUMN] = id["Type"]

            assert row in input_rows

        count = 0
        for input_row in input_rows:
            if input_row[GROUP_ID_COLUMN] == group:
                count += 1
                assert bloom_filter.has(str(input_row[ID_COLUMN]))

        assert count == output_row[ACTUAL_SEGMENT_SIZE]
        assert DESCRIBING_MODE == command["mode"]

    return result


@pytest.mark.parametrize("use_id_type_column", [True, False])
def test_create_sample_user_set(yt_stuff, mock_siberia_core_server, tvm_api, tvm_src_id, tvm_dst_id, use_id_type_column):
    cases = [
        ("3 yuids", 3),
        ("5 yuids", 5),
        ("10 yuids, 5 in output", 10),
    ]
    rows = list(rows_generator(cases, use_id_type_column))
    random.shuffle(rows)
    print rows

    run_map_reduce(yt_stuff, rows, 5, tvm_api, tvm_src_id, tvm_dst_id, mock_siberia_core_server, use_id_type_column)

    return check_commands(rows, yt_stuff.get_yt_client(), mock_siberia_core_server, use_id_type_column)


def test_create_sample_user_set_with_different_id_types(yt_stuff, mock_siberia_core_server, tvm_api, tvm_src_id, tvm_dst_id):
    rows = [
        {GROUP_ID_COLUMN: "group", ID_TYPE_COLUMN: get_id_type_name(cls.ID_TYPE), ID_COLUMN: cls.next()}
        for cls in [
            identifiers.Yandexuid,
            identifiers.Idfa,
            identifiers.Gaid,
        ]
    ]
    random.shuffle(rows)

    run_map_reduce(yt_stuff, rows, 3, tvm_api, tvm_src_id, tvm_dst_id, mock_siberia_core_server, True)

    return check_commands(rows, yt_stuff.get_yt_client(), mock_siberia_core_server, True)


def test_create_sample_user_set_with_skip_rate(yt_stuff, mock_siberia_core_server, tvm_api, tvm_src_id, tvm_dst_id):
    sample_size = 1000
    base_yuid = 472679231552712336

    rows = [
        {GROUP_ID_COLUMN: "group", ID_COLUMN: str(base_yuid + i)}
        for i in range(sample_size)
    ]
    random.shuffle(rows)

    run_map_reduce(yt_stuff, rows, sample_size, tvm_api, tvm_src_id, tvm_dst_id, mock_siberia_core_server, False, 0.3)

    return check_commands(rows, yt_stuff.get_yt_client(), mock_siberia_core_server, False)
