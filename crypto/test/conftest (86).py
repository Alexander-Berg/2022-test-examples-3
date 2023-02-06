import itertools
import logging
import os

import flask
from library.python.protobuf.json import proto2json
import pytest
import yatest.common

from crypta.lib.proto.identifiers import id_pb2
from crypta.lib.proto.user_data import user_data_stats_pb2
from crypta.lib.python import test_utils
from crypta.lookalike.services.lal_manager.bin.test.test_helpers.lal_manager import LalManager
from crypta.siberia.bin.common.describing.proto.describing_info_pb2 import TDescribingInfo
from crypta.siberia.bin.common.proto.describe_ids_response_pb2 import TDescribeIdsResponse
from crypta.siberia.bin.common.proto.stats_pb2 import TStats
from crypta.siberia.bin.custom_audience.fast.grpc import (
    custom_audience_service_pb2,
    custom_audience_service_pb2_grpc,
)
from crypta.lib.python.logging import logging_helpers


pytest_plugins = [
    "crypta.lib.python.logbroker.test_helpers.fixtures",
    "crypta.lib.python.tvm.test_utils.fixtures",
    "crypta.lib.python.yt.test_helpers.fixtures",
]


logging_helpers.configure_stdout_logger(logging.getLogger())


@pytest.fixture(scope="session")
def audience_segments_table_path():
    return "//home/crypta/qa/audience/Stats"


@pytest.fixture(scope="session")
def cdp_segments_table_path():
    return "//home/metrika/cdp/qa/export/user_segments"


@pytest.fixture(scope="session")
def yt_working_dir():
    return "//home/crypta/qa"


def get_lal_manager(request, logbroker_config, tvm_api, mock_siberia_core_server, mock_custom_audience_server, yt_stuff,
                    yt_working_dir, audience_segments_table_path, cdp_segments_table_path, describing_mode):
    return LalManager(working_dir=os.path.join(yatest.common.test_output_path(), "lal_manager"),
                      logbroker_config=logbroker_config,
                      tvm_api=tvm_api,
                      self_tvm_id=1000501,
                      siberia_tvm_id=1000502,
                      local_siberia=mock_siberia_core_server,
                      local_ca=mock_custom_audience_server,
                      yt_stuff=yt_stuff,
                      yt_working_dir=yt_working_dir,
                      audience_segments_table_path=audience_segments_table_path,
                      cdp_segments_table_path=cdp_segments_table_path,
                      frozen_time=getattr(request.module, "FROZEN_TIME", None),
                      describing_mode=describing_mode)


@pytest.fixture(scope="function")
def local_lal_lal_manager_slow(request, logbroker_config, tvm_api, mock_siberia_core_server, mock_custom_audience_server, yt_stuff,
                               yt_working_dir, audience_segments_table_path, cdp_segments_table_path):
    with get_lal_manager(request, logbroker_config, tvm_api, mock_siberia_core_server, mock_custom_audience_server, yt_stuff,
                         yt_working_dir, audience_segments_table_path, cdp_segments_table_path, "slow") as processor:
        yield processor


@pytest.fixture(scope="function")
def local_lal_lal_manager_fast(request, logbroker_config, tvm_api, mock_siberia_core_server, mock_custom_audience_server, yt_stuff,
                               yt_working_dir, audience_segments_table_path, cdp_segments_table_path):
    with get_lal_manager(request, logbroker_config, tvm_api, mock_siberia_core_server, mock_custom_audience_server, yt_stuff,
                         yt_working_dir, audience_segments_table_path, cdp_segments_table_path, "fast") as processor:
        yield processor


@pytest.fixture(scope="function")
def mock_siberia_core_server():
    class MockSiberiaCoreServer(test_utils.FlaskMockServer):
        def __init__(self):
            super(MockSiberiaCoreServer, self).__init__("Siberia Core")
            self.commands = []
            self.user_set_id = itertools.count(1)

            @self.app.route('/user_sets/describe_ids', methods=["POST"])
            def describe_ids():
                user_set_id = str(self.user_set_id.next())

                self.commands.append({
                    "type": "describe_ids",
                    "user_set_id": user_set_id,
                    "ids": flask.request.json["Ids"],
                })
                return proto2json.proto2json(TDescribeIdsResponse(UserSetId=user_set_id))

            @self.app.route('/user_sets/get_stats', methods=["GET"])
            def get_user_set_stats():
                assert 1 == len(flask.request.args.getlist("user_set_id"))

                user_set_id = flask.request.args["user_set_id"]

                self.commands.append({
                    "type": "get_stats",
                    "user_set_id": user_set_id,
                })

                stats = TStats(
                    UserDataStats=user_data_stats_pb2.TUserDataStats(Counts=user_data_stats_pb2.TUserDataStats.TCounts(Total=1)),
                    Info=TDescribingInfo(ProcessedUsersCount=1, Ready=True)
                )

                return proto2json.proto2json(stats)

    with MockSiberiaCoreServer() as mock:
        yield mock


@pytest.fixture(scope="function")
def mock_custom_audience_server():
    class CustomAudienceServicer(custom_audience_service_pb2_grpc.TCustomAudienceServiceServicer):
        base_servicer_class = custom_audience_service_pb2_grpc.TCustomAudienceServiceServicer

        def Ping(self, request, context):
            return custom_audience_service_pb2.TPingResponse(Message="OK")

        def GetIds(self, request, context):
            return id_pb2.TPlainIds(Ids=["crypta_id_1", "crypta_id_2", "crypta_id_3"])

        def GetStats(self, request, context):
            return user_data_stats_pb2.TUserDataStats(Counts=user_data_stats_pb2.TUserDataStats.TCounts(Total=10))

    with test_utils.GrpcMockServer(CustomAudienceServicer) as mock:
        yield mock
