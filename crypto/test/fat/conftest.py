import json
import itertools
import os

import flask
from library.python.protobuf.json import proto2json
import mock
import pytest

from crypta.audience.lib.storage import segment_priorities
from crypta.audience.lib.tasks.audience.tables import (
    Matching,
    Output,
)
from crypta.audience.lib import storage
from crypta.lib.python import (
    test_utils,
    time_utils,
)
from crypta.siberia.bin.common.proto import (
    describe_ids_response_pb2,
    stats_pb2,
)
from crypta.audience.test.fat import (
    fixtures,
    identifiers,
)

pytest_plugins = [
    "crypta.audience.lib.test_helpers.fixtures",
    "crypta.lib.python.solomon.test_utils.fixtures",
    "crypta.lib.python.tvm.test_utils.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture(scope="session")
def tvm_src_id(tvm_api):
    return tvm_api.issue_id()


@pytest.fixture(scope="session")
def tvm_dst_id(tvm_api):
    return tvm_api.issue_id()


@pytest.fixture(scope="session")
def bigb_tvm_id(tvm_api):
    return tvm_api.issue_id()


@pytest.fixture(scope="function")
def conf(default_conf, mock_siberia_core_server, tvm_api, tvm_src_id, tvm_dst_id, mock_bigb, bigb_tvm_id, sampler_udf_server, mock_solomon_server):
    import crypta.lib.python.bt.conf.conf as conf

    proto_config = default_conf.proto

    proto_config.Siberia.Host = mock_siberia_core_server.host
    proto_config.Siberia.Port = mock_siberia_core_server.port
    proto_config.Siberia.Tvm.DestinationTvmId = tvm_dst_id

    proto_config.Tvm.TvmId = tvm_src_id
    proto_config.Tvm.TvmSecret = tvm_api.get_secret(tvm_src_id)

    proto_config.Options.SiberiaSampling.SampleSize = 1000
    proto_config.Options.SiberiaSampling.MaxIdsPerSecond = 10000
    proto_config.Options.SiberiaSampling.Login = "login"

    proto_config.Options.Input.IncompleteBatchMinAgeSec = 1
    proto_config.Options.Input.MaxBatchSizeInSegments = 5
    proto_config.Options.Input.NumBatches = 1

    proto_config.Options.Storage.AudienceSegmentsHardLimit = 4
    proto_config.Options.Storage.ReuploadIntervalDays = 7
    proto_config.Options.Storage.YuidSampleDenominator = 2
    proto_config.Options.Storage.DeviceSampleDenominator = 2
    proto_config.Options.Storage.CryptaIdSampleDenominator = 2
    proto_config.Options.Storage.PuidSampleDenominator = 2
    proto_config.Options.Storage.SampleRest = 1
    proto_config.Options.Storage.SampleSizeLowerBound = 2

    proto_config.Options.Storage.DumpBigbSample.Handle = mock_bigb.url_prefix
    proto_config.Options.Storage.DumpBigbSample.DestinationTvm.DestinationTvmId = bigb_tvm_id
    proto_config.Options.Storage.DumpBigbSample.SourceTvm.SourceTvmId = tvm_src_id
    proto_config.Options.Storage.DumpBigbSample.SourceTvm.Secret = tvm_api.get_secret(tvm_src_id)

    proto_config.Options.Lookalike.InputIncompleteBatchMinAgeSec = 1
    proto_config.Options.Lookalike.InputShards = 1

    proto_config.Solomon.Url = mock_solomon_server.url_prefix
    proto_config.Solomon.Token = 'fake'

    conf.use_proto(proto_config)

    conf.paths.udfs._I_know_what_I_do_set("sampler", sampler_udf_server.get_udf_url())

    yield conf


@pytest.fixture(scope="function")
def prepared_local_yt(clean_local_yt, conf):
    for path in (
        "//tmp/crypta-audience",
        "//home/crypta/testing/audience",
    ):
        clean_local_yt.get_yt_client().create("map_node", path, recursive=True, ignore_existing=True)

    try:
        yield clean_local_yt
    except:
        pass


@pytest.fixture(scope="session")
def mock_siberia_core_server():
    class MockSiberiaCoreServer(test_utils.FlaskMockServer):
        def __init__(self):
            super(MockSiberiaCoreServer, self).__init__("Siberia Core")
            self.user_set_id = itertools.count()
            self.user_sets = {}

            @self.app.route('/user_sets/describe_ids', methods=["POST"])
            def describe_ids():
                user_set_id = self.user_set_id.next()
                self.user_sets[user_set_id] = [id_ for id_ in flask.request.json["Ids"] if id_["Value"] not in identifiers.JUNK_YANDEXUIDS]

                proto = describe_ids_response_pb2.TDescribeIdsResponse()
                proto.UserSetId = str(user_set_id)

                return proto2json.proto2json(proto)

            @self.app.route('/user_sets/get_stats')
            def get_user_set_stats():
                user_set_id = int(flask.request.args["user_set_id"])

                proto = stats_pb2.TStats()
                proto.Info.ProcessedUsersCount = len(self.user_sets[user_set_id])
                proto.Info.Ready = True

                proto.UserDataStats.Distributions.Main.Mean.Data.extend([1.0]*512)
                proto.UserDataStats.Distributions.Main.Count = proto.Info.ProcessedUsersCount
                region = proto.UserDataStats.Attributes.Region.add()
                region.Region = 1
                region.Count = proto.Info.ProcessedUsersCount

                gender = proto.UserDataStats.Attributes.Gender.add()
                gender.Gender = 1
                gender.Count = proto.Info.ProcessedUsersCount

                age = proto.UserDataStats.Attributes.Age.add()
                age.Age = 2
                age.Count = proto.Info.ProcessedUsersCount

                income = proto.UserDataStats.Attributes.Income.add()
                income.Income = 3
                income.Count = proto.Info.ProcessedUsersCount

                device = proto.UserDataStats.Attributes.Device.add()
                device.Device = 2
                device.Count = proto.Info.ProcessedUsersCount

                gender_age_income = proto.UserDataStats.Attributes.GenderAgeIncome.add()
                gender_age_income.GenderAgeIncome.Gender = 1
                gender_age_income.GenderAgeIncome.Age = 2
                gender_age_income.GenderAgeIncome.Income = 3
                gender_age_income.Count = proto.Info.ProcessedUsersCount

                stratum = proto.UserDataStats.Stratum.Strata.add()
                stratum.Strata.Device = 2
                stratum.Strata.Country = 225
                stratum.Strata.City = 213
                stratum.Strata.HasCryptaID = True
                stratum.Count = proto.Info.ProcessedUsersCount

                segment = stratum.Segment.add()
                segment.Count = proto.Info.ProcessedUsersCount
                segment.Segment.CopyFrom(Output.CRYPTA_INTERESTS[0])

                proto.UserDataStats.Counts.UniqYuid = proto.Info.ProcessedUsersCount
                proto.UserDataStats.Counts.Total = proto.Info.ProcessedUsersCount
                proto.UserDataStats.Counts.WithData = proto.Info.ProcessedUsersCount

                return proto2json.proto2json(proto)

    with MockSiberiaCoreServer() as mock:
        yield mock


@pytest.fixture
def frozen_time():
    result = "1590000000"
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    yield result


@pytest.fixture
def mock_sandbox_client():
    class MockSandboxClient(object):
        uploads = []

        def __init__(self, *args):
            pass

        def upload_to_sandbox(self, path, **kwargs):
            with open(path) as f:
                if kwargs["resource_type"] == storage.PRIORITIES_RESOURCE_TYPE:
                    kwargs["body"] = json.load(f)
                elif kwargs["resource_type"] == storage.VINYL_PRIORITIES_RESOURCE_TYPE:
                    proto = segment_priorities.convert_vinyl_to_proto(f.read())
                    kwargs["body"] = json.loads(proto2json.proto2json(proto, config=proto2json.Proto2JsonConfig(map_as_object=True)))
                else:
                    kwargs["body"] = f.read()

            self.uploads.append(kwargs)
            return 1

    with mock.patch("crypta.audience.lib.storage.SandboxClient", MockSandboxClient):
        yield MockSandboxClient


@pytest.fixture(scope="session")
def mock_bigb():
    class MockBigb(test_utils.FlaskMockServer):
        def __init__(self):
            super(MockBigb, self).__init__("Bigb")

            @self.app.route('/', methods=["GET"])
            def bigb():
                return {
                    "items": [
                        {
                            "keyword_id": 557,
                            "update_time": 1600000000,
                            "uint_values": [2005000000 + i]
                        }
                        for i in range(int(flask.request.args["bigb-uid"]) % 3)
                    ]
                }

    with MockBigb() as mock:
        yield mock


@pytest.fixture(scope="session")
def sampler_udf_server():
    with test_utils.mock_sandbox_server_with_udf("CRYPTA_SAMPLER_UDF", "yql/udfs/crypta/sampler/libcrypta_sampler_udf.so") as mock:
        yield mock


@pytest.fixture
def matching_table(prepared_local_yt, conf):
    data = (
        {
            Matching.Fields.ID_VALUE: id_value,
            Matching.Fields.ID_TYPE: id_type,
            Matching.Fields.YUID: yuid,
        }
        for id_type, ids in (
            (Matching.IDFA_GAID, identifiers.IDFAS_GAIDS),
            (Matching.EMAIL, identifiers.EMAILS),
            (Matching.PHONE, identifiers.PHONES),
        )
        for id_value, yuid in zip(ids, identifiers.YANDEXUIDS)
    )

    matching_table_path = conf.paths.audience.matching.by_id_value

    prepared_local_yt.get_yt_client().create('table', matching_table_path, recursive=True)
    prepared_local_yt.get_yt_client().write_table(matching_table_path, data)

    return matching_table_path


@pytest.fixture
def user_data_stats(prepared_local_yt):
    fixtures.create_userdata_table()
