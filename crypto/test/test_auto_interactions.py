import os
import yatest.common

from library.python.protobuf import yql

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)
from crypta.profile.runners.segments.lib.coded_segments import auto_interactions
from crypta.profile.runners.segments.lib.coded_segments.test.proto import (
    auto_events_pb2,
)


def get_outputs(task):
    table = task.output().table
    return [
        (tables.YsonTable(
            '{}.yson'.format(os.path.basename(table)),
            table,
            yson_format='pretty',
        ), tests.Diff()),
    ]


def get_input_table(file_name, path, proto, attrs):
    attrs["schema"] = schema_utils.get_schema_from_proto(proto)

    on_write = tables.OnWrite(
        row_transformer=row_transformers.proto_dict_to_yson(proto),
        attributes=attrs,
    )
    return tables.YsonTable(file_name, path, on_write=on_write)


def test_auto(local_yt, patched_config, date):
    task = auto_interactions.ProcessedAutoLogForAutoInteractions(date=date)

    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=task.run,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (get_input_table(
                file_name='auto_events.yson',
                path=task.input()['auto_events'].table,
                proto=auto_events_pb2.Event,
                attrs={
                    '_yql_proto_field_user_info': yql.yql_proto_field(auto_events_pb2.UserInfo, enum_mode='full_name'),
                    '_yql_proto_field_offer': yql.yql_proto_field(auto_events_pb2.Offer, enum_mode='full_name'),
                }
            ), tests.TableIsNotChanged()),
        ],
        output_tables=get_outputs(task),
    )


def test_output(local_yt, patched_config, date):
    task = auto_interactions.AutoInteractions(date=date)

    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=task.run,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (tables.YsonTable(
                'auto_log_processor.yson',
                task.input().table,
            ), tests.TableIsNotChanged()),
        ],
        output_tables=get_outputs(task),
    )
