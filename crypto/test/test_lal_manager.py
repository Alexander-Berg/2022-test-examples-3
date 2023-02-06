import itertools
import time

from library.python.protobuf.json import proto2json
import pytest
import yatest.common
import yt.wrapper as yt

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
import crypta.lookalike.lib.python.test_utils as lal_test_utils
from crypta.lookalike.lib.python.utils import yt_schemas
from crypta.lookalike.proto.lal_parent_pb2 import TLalParent
from crypta.lookalike.services.lal_manager.commands.add_lal_cmd_pb2 import TAddLalCmd
from crypta.lookalike.services.lal_manager.commands.describe_lal_cmd_pb2 import TDescribeLalCmd
from crypta.lookalike.services.lal_manager.commands.lal_cmd_pb2 import TLalCmd
from crypta.lookalike.services.lal_manager.commands.remove_lal_cmd_pb2 import TRemoveLalCmd
from crypta.lookalike.services.lal_manager.commands.change_newness_cmd_pb2 import TChangeNewnessCmd


counter = itertools.count(1)
FROZEN_TIME = 1500000000


def get_goal_id(lal_id):
    return 2 if lal_id != 2 else 3


def run_test(commands, logbroker_client, yt_stuff, yt_working_dir, audience_segments_table_path, cdp_segments_table_path):
    def run():
        lb_producer = logbroker_client.create_producer()
        result = lb_producer.write(counter.next(), "\n".join([proto2json.proto2json(x) for x in commands])).result(timeout=10)
        assert result.HasField("ack")

        time.sleep(30)

        assert not consumer_utils.read_all(logbroker_client.create_consumer())

    def get_working_path(path):
        return yt.ypath_join(yt_working_dir, path)

    lals_file_path = "lals.yson"
    scope_yt_path = get_working_path("lookalike/scopes/direct")
    lals_yt_path = yt.ypath_join(scope_yt_path, "lals")

    return tests.yt_test_func(
        yt_stuff.get_yt_client(),
        run,
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema(
                "goal_audiences.yson",
                get_working_path("lookalike/goal_audiences/goal_audiences"),
                yt_schemas.get_goal_audiences_schema(),
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "metrika_counter_audiences.yson",
                get_working_path("lookalike/metrika_counter_audiences/metrika_counter_audiences"),
                yt_schemas.get_metrika_counter_audiences_schema(),
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "metrika_segments.yson",
                get_working_path("lookalike/metrika_segments/metrika_segments"),
                yt_schemas.get_metrika_segments_schema(),
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "metrika_ecommerce.yson",
                get_working_path("lookalike/metrika_ecommerce/metrika_ecommerce"),
                yt_schemas.get_metrika_segments_schema(),
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "mobile_event.yson",
                get_working_path("lookalike/metrika_event/metrika_event"),
                yt_schemas.get_metrika_segments_schema(),
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "cdp_segments.yson",
                cdp_segments_table_path,
                yt_schemas.get_cdp_segments_schema(),
            ), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema(
                "custom_audiences.yson",
                yt.ypath_join(scope_yt_path, "ca_bindings"),
                yt_schemas.get_ca_bindings_schema(),
            ), tests.TableIsNotChanged()),
            (tables.DynamicYsonTable(
                "audience_segments.yson",
                audience_segments_table_path,
                on_write=lal_test_utils.audience_segments_on_write(),
            ), tests.TableIsNotChanged()),
            (tables.DynamicYsonTable(
                lals_file_path,
                lals_yt_path,
                on_write=lal_test_utils.lals_on_write(),
            ), None),
        ],
        output_tables=[
            (tables.DynamicYsonTable(
                lals_file_path,
                lals_yt_path,
                on_read=lal_test_utils.lals_on_read(),
            ), tests.Diff()),
        ]
    )


@pytest.mark.parametrize("commands", [
    pytest.param([TLalCmd(
        AddLalCmd=TAddLalCmd(LalId=lal_id, Parent=TLalParent(Id=get_goal_id(lal_id), Type=TLalParent.GOAL))
    ) for lal_id in range(2, 8)], id="add"),
    pytest.param([TLalCmd(
        DescribeLalCmd=TDescribeLalCmd(LalId=lal_id)
    ) for lal_id in range(2, 8)], id="describe"),
    pytest.param([TLalCmd(
        RemoveLalCmd=TRemoveLalCmd(LalId=lal_id)
    ) for lal_id in range(2, 8)], id="remove"),
    pytest.param([TLalCmd(
        ChangeNewnessCmd=TChangeNewnessCmd(LalId=lal_id, NewnessState=False)
    ) for lal_id in range(2, 8)], id="change_newness"),
    pytest.param([
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=8, Parent=TLalParent(Id=1, Type=TLalParent.AUDIENCE_SEGMENT))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=9, Parent=TLalParent(Id=2, Type=TLalParent.AUDIENCE_SEGMENT))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=10, Parent=TLalParent(Id=3, Type=TLalParent.AUDIENCE_SEGMENT))),
    ], id="add_audience_segment"),
    pytest.param([
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=8, Parent=TLalParent(Id=1000000001, Type=TLalParent.METRIKA_SEGMENT))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=9, Parent=TLalParent(Id=1000000002, Type=TLalParent.METRIKA_SEGMENT))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=10, Parent=TLalParent(Id=1000000003, Type=TLalParent.METRIKA_SEGMENT))),
    ], id="add_metrika_segment"),
    pytest.param([
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=8, Parent=TLalParent(Id=1, Type=TLalParent.METRIKA_ECOMMERCE))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=9, Parent=TLalParent(Id=2, Type=TLalParent.METRIKA_ECOMMERCE))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=10, Parent=TLalParent(Id=3, Type=TLalParent.METRIKA_ECOMMERCE))),
    ], id="add_metrika_ecommerce"),
    pytest.param([
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=8, Parent=TLalParent(Id=1, Type=TLalParent.METRIKA_COUNTER))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=9, Parent=TLalParent(Id=2, Type=TLalParent.METRIKA_COUNTER))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=10, Parent=TLalParent(Id=3, Type=TLalParent.METRIKA_COUNTER))),
    ], id="add_metrika_counter"),
    pytest.param([
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=8, Parent=TLalParent(Id=1900000001, Type=TLalParent.MOBILE_EVENT))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=9, Parent=TLalParent(Id=1900000002, Type=TLalParent.MOBILE_EVENT))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=10, Parent=TLalParent(Id=1900000003, Type=TLalParent.MOBILE_EVENT))),
    ], id="add_mobile_event"),
    pytest.param([
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=8, Parent=TLalParent(Id=1, Type=TLalParent.CDP_SEGMENT))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=9, Parent=TLalParent(Id=2, Type=TLalParent.CDP_SEGMENT))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=10, Parent=TLalParent(Id=3, Type=TLalParent.CDP_SEGMENT))),
    ], id="add_cdp_segment"),
    pytest.param([
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=8, Parent=TLalParent(Id=1, Type=TLalParent.CUSTOM_AUDIENCE, Rule='{"Words": ["football_1"]}'))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=9, Parent=TLalParent(Id=2, Type=TLalParent.CUSTOM_AUDIENCE, Rule='{"Words": ["football_2"]}'))),
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=10, Parent=TLalParent(Id=3, Type=TLalParent.CUSTOM_AUDIENCE, Rule='{"Words": ["football_3"]}'))),
    ], id="add_custom_audience"),
])
def test_lal_lal_manager(commands, local_lal_lal_manager_slow, mock_siberia_core_server, logbroker_client, yt_stuff, yt_working_dir, audience_segments_table_path, cdp_segments_table_path):
    return run_test(commands, logbroker_client, yt_stuff, yt_working_dir, audience_segments_table_path, cdp_segments_table_path)


@pytest.mark.parametrize("commands", [
    pytest.param([
        TLalCmd(AddLalCmd=TAddLalCmd(LalId=8, Parent=TLalParent(Id=1, Type=TLalParent.CUSTOM_AUDIENCE, Rule='{"Words": ["football_1"]}'))),
    ], id="add_custom_audience"),
])
def test_lal_lal_manager_fast(commands, local_lal_lal_manager_fast, mock_siberia_core_server, mock_custom_audience_server, logbroker_client, yt_stuff,
                              yt_working_dir, audience_segments_table_path, cdp_segments_table_path):
    return {
        "test_results": run_test(commands, logbroker_client, yt_stuff, yt_working_dir, audience_segments_table_path, cdp_segments_table_path),
        "siberia_requests": mock_siberia_core_server.commands,
    }
