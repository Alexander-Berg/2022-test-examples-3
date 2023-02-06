import copy
import itertools
import logging
import os

import flask
from library.python.protobuf.json import proto2json
import pytest
import yt.wrapper as yt

from crypta.lab.lib.samples.test import constants
import crypta.lab.lib.samples.samples as descriptor
from crypta.lab.proto import (
    config_pb2,
    sample_pb2,
    view_pb2,
)
from crypta.lib.python import (
    test_utils,
    time_utils,
)
from crypta.siberia.bin.common.proto import describe_ids_response_pb2


logger = logging.getLogger(__name__)

pytest_plugins = [
    "crypta.siberia.bin.common.test_helpers.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
    "crypta.lib.python.tvm.test_utils.fixtures",
]


@pytest.fixture(scope="session")
def ydb_token():
    return "_FAKE_YDB_TOKEN_"


@pytest.fixture(scope="session")
def tvm_src_id(tvm_api):
    return tvm_api.issue_id()


@pytest.fixture(scope="session")
def tvm_dst_id(tvm_api):
    return tvm_api.issue_id()


@pytest.fixture
def frozen_time():
    result = "1590000000"
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    yield result


class Lab(object):
    samples_db = {
        constants.SAMPLE_ID: {
            "id": constants.SAMPLE_ID,
            "name": "Test sample",
            "idName": "yandexuid",
            "idKey": "ID_KEY",
            "groupingKey": "any_group_name",
            "timestamps": {
                "created": 1600857289,
                "modified": 1600857289
            },
            "author": "robot-crypta-testing",
            "ttl": constants.TTL,
            "accessLevel": "SHARED",
            "siberiaUserSetId": "",
            "userSetIdToGroupingKeyValue": {},
            "maxGroupsCount": 50
        }
    }

    def save_sample_to_db(self, sample_id, sample):
        self.samples_db[sample_id] = sample

    @test_utils.api_result(sample_pb2.Sample)
    def getSample(self, id):  # noqa
        return self.samples_db[id]

    @test_utils.api_result(sample_pb2.Sample)
    def setUserSetId(self, id, user_set_id):  # noqa
        sample = self.samples_db[id]
        sample['siberiaUserSetId'] = user_set_id
        self.save_sample_to_db(id, sample)

        return sample

    @test_utils.api_result(view_pb2.TSampleView)
    def getSampleView(self, id, view_id):  # noqa
        return {
            "ID": view_id,
            "SampleID": "sample-123456",
            "Path": yt.ypath_join("//home/crypta/testing/lab/samples", id, view_id),
            "State": "READY",
            "Options": {
                "DerivedFrom": "source",
                "Matching": {
                    "HashingMethod": "HM_IDENTITY",
                    "IncludeOriginal": True,
                    "IdType": "LAB_ID_YANDEXUID",
                    "Key": "ID_KEY",
                    "Scope": "CROSS_DEVICE"
                }
            },
            "Type": "IDENTITY"
        }

    @test_utils.api_result(view_pb2.TSampleView)
    def updateSampleViewState(self, id, view_id, state):  # noqa
        return {
            "ID": view_id,
            "SampleID": id,
            "Path": yt.ypath_join("//home/crypta/testing/lab/samples", id, view_id),
            "State": state,
            "Options": {
                "DerivedFrom": "source",
                "Matching": {
                    "HashingMethod": "HM_IDENTITY",
                    "IncludeOriginal": True,
                    "IdType": "LAB_ID_YANDEXUID",
                    "Key": "ID_KEY",
                    "Scope": "CROSS_DEVICE"
                }
            },
            "Type": "IDENTITY"
        }

    @test_utils.api_result(sample_pb2.Subsamples)
    def createSubsamples(self, id, ids):  # noqa
        id_pairs_list = [id_map.split(",") for id_map in ids]

        subsamples = {
            "sampleId": id,
            "userSetIdToGroupingKeyValue": {
                user_set_id: grouping_key_value for (user_set_id, grouping_key_value) in id_pairs_list
            },
        }

        return subsamples


class ApiClient(object):
    def __init__(self):
        self.lab = Lab()


@pytest.fixture(scope="function")
def api_client_mock(mocker):
    api_client = ApiClient()

    mocker.patch(
        'crypta.lab.lib.common.WithApi.api',
        new=api_client,
    )

    return api_client


@pytest.fixture(scope="function")
def siberia_mock():
    class SiberiaMock(test_utils.FlaskMockServer):
        def __init__(self):
            super(SiberiaMock, self).__init__("Siberia Core")
            self.user_set_id = itertools.count()
            self.user_set_ids = []

            @self.app.route('/user_sets/describe_ids', methods=["POST"])
            def describe_ids():
                assert constants.TTL == int(flask.request.args["ttl"])
                user_set_id = str(self.user_set_id.next())
                self.user_set_ids.append(user_set_id)

                return proto2json.proto2json(describe_ids_response_pb2.TDescribeIdsResponse(UserSetId=user_set_id))

    with SiberiaMock() as mock:
        yield mock


@pytest.fixture(scope='function')
def get_subsample_info_task(api_client_mock):
    task = descriptor.GetSubsamplesInfo(
        sample_id=constants.SAMPLE_ID,
        src_view_id=constants.SRC_VIEW_ID,
        max_groups_count=10,
    )
    task.api = api_client_mock

    yield task


@pytest.fixture(scope='function')
def prepare_subsamples_task(api_client_mock):
    task = descriptor.PrepareSubsamples(
        sample_id=constants.SAMPLE_ID,
        src_view_id=constants.SRC_VIEW_ID,
        max_groups_count=10,
    )
    task.api = api_client_mock

    yield task


@pytest.fixture(scope='function')
def describe_in_siberia_task(api_client_mock):
    task = descriptor.DescribeSubsamples(
        sample_id=constants.SAMPLE_ID,
        src_view_id=constants.SRC_VIEW_ID,
        max_groups_count=10,
    )
    task.api = api_client_mock

    yield task


@pytest.fixture(scope='function')
def describe_single_sample_task(api_client_mock):
    task = descriptor.DescribeSingleSample(
        sample_id=constants.SAMPLE_ID,
        src_view_id=constants.SRC_VIEW_ID,
    )
    task.api = api_client_mock

    yield task


# TODO(unretrofied): use old name of description task (Describe) due to backward copmpatibility, to be deleted after tests
@pytest.fixture(scope='function')
def describe_subsamples_task_old(api_client_mock):
    task = descriptor.Describe(
        sample_id=constants.SAMPLE_ID,
        src_view_id=constants.SRC_VIEW_ID,
        max_groups_count=10,
    )
    task.api = api_client_mock

    yield task


@pytest.fixture(scope='function')
def create_standard_views_task(api_client_mock):
    task = descriptor.CreateStandardViews(
        sample_id=constants.SAMPLE_ID,
        src_view_id=constants.SRC_VIEW_ID,
        invalid_view_id=constants.INVALID_VIEW_ID,
        yandexuid_view_id=constants.YANDEXUID_VIEW_ID,
    )
    task.api = api_client_mock

    yield task


@pytest.fixture(scope="function")
def conf(siberia_mock, local_yt, local_yt_and_yql_env, tvm_api, tvm_src_id, tvm_dst_id):
    import crypta.lib.python.bt.conf.resource_conf as resource_conf
    import crypta.lib.python.bt.conf.conf as conf

    os.environ["CRYPTA_ENVIRONMENT"] = "testing"
    conf.use(resource_conf.find('/crypta/lab'))
    old_env = copy.deepcopy(os.environ)

    os.environ["YT_USE_SINGLE_TABLET"] = "1"
    os.environ.update(local_yt_and_yql_env)

    proto_config = config_pb2.TLabConfig()
    proto_config.Yt.Token = 'fake'
    proto_config.Yt.Proxy = local_yt.get_server()
    proto_config.Yql.Token = 'fake'

    proto_config.Siberia.Host = siberia_mock.host
    proto_config.Siberia.Port = siberia_mock.port

    proto_config.Tvm.SourceTvmId = tvm_src_id
    proto_config.Siberia.Tvm.DestinationTvmId = tvm_dst_id
    proto_config.Tvm.Secret = tvm_api.get_secret(tvm_src_id)

    conf.use_proto(proto_config)

    yield conf

    os.environ.update(old_env)
