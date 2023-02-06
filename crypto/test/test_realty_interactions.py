import os
import yatest.common

from library.python.protobuf import yql

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)
from crypta.profile.runners.segments.lib.coded_segments import realty_interactions
from crypta.profile.runners.segments.lib.coded_segments.test.proto import (
    realty_events_pb2,
    realty_sites_pb2,
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


def test_realty(local_yt, patched_config, date):
    task = realty_interactions.ProcessedRealtyLogForRealtyInteractions(date=date)

    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=task.run,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (get_input_table(
                file_name='realty_events.yson',
                path=task.input()['realty_events'].table,
                proto=realty_events_pb2.Event,
                attrs={
                    '_yql_proto_field_user_info': yql.yql_proto_field(realty_events_pb2.UserInfo, enum_mode='full_name'),
                    '_yql_proto_field_object_info': yql.yql_proto_field(realty_events_pb2.ObjectInfo, enum_mode='full_name'),
                }
            ), tests.TableIsNotChanged()),
            (get_input_table(
                file_name='realty_sites.yson',
                path=task.input()['realty_sites'].table,
                proto=realty_sites_pb2.Property,
                attrs={
                    '_yql_proto_field_site': yql.yql_proto_field(realty_sites_pb2.Site, enum_mode='full_name'),
                }
            ), tests.TableIsNotChanged()),
        ],
        output_tables=get_outputs(task),
    )


def test_output(local_yt, patched_config, date):
    task = realty_interactions.RealtyInteractions(date=date)

    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=task.run,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (tables.YsonTable(
                'realty_log_processor.yson',
                task.input().table,
            ), tests.TableIsNotChanged()),
        ],
        output_tables=get_outputs(task),
    )
