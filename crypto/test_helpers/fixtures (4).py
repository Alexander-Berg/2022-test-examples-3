import hashlib
import json

import flask
from library.python.protobuf.json import proto2json
from library.python import resource
import pytest
import yatest
import yatest.common.network

from crypta.lab.rule_estimator.lib.test_helpers.rule_estimator_api import RuleEstimatorApi
from crypta.lab.rule_estimator.lib.test_helpers.rule_estimator_worker import RuleEstimatorWorker
from crypta.lib.python import (
    test_utils,
    time_utils,
)
from crypta.siberia.bin.common.proto.describe_ids_response_pb2 import TDescribeIdsResponse


pytest_plugins = [
    "crypta.lib.python.chyt.test_helpers.fixtures",
    "crypta.lib.python.logbroker.test_helpers.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
    "crypta.profile.lib.test_helpers.fixtures",
]


@pytest.fixture
def frozen_time():
    yield "1600000000"


def get_hash(id_):
    return str(int(hashlib.md5(id_).hexdigest()[:16], 16))


@pytest.fixture
def mock_siberia_core_server():
    class MockSiberiaCoreServer(test_utils.FlaskMockServer):
        def __init__(self):
            super(MockSiberiaCoreServer, self).__init__("Siberia Core")
            self.commands = []

            @self.app.route('/user_sets/describe_ids', methods=["POST"])
            def describe_ids():
                ids_json = flask.request.get_json(force=True)
                user_set_id = min(x["Value"] for x in ids_json["Ids"])
                user_set_id = user_set_id if user_set_id.isdigit() else get_hash(user_set_id)

                self.commands.append({
                    "type": "describe_ids",
                    "user_set_id": user_set_id,
                    "ids": ids_json["Ids"],
                })
                return proto2json.proto2json(TDescribeIdsResponse(UserSetId=user_set_id))

    with MockSiberiaCoreServer() as mock:
        yield mock


@pytest.fixture
def mock_crypta_api():
    class MockCryptaApi(test_utils.FlaskMockServer):
        def __init__(self):
            super(MockCryptaApi, self).__init__("Crypta API")
            self.rule_conditions = {}
            self.swagger = json.loads(resource.find("/swagger.json"))

            @self.app.route('/lab/constructor/rule/condition/<int:revision>')
            def get_rule_condition(revision):
                return self.rule_conditions[revision]

            @self.app.route("/swagger.json")
            @self.app.route("/")
            def swagger():
                return self.swagger

    with MockCryptaApi() as mock:
        yield mock


@pytest.fixture
def rule_estimator_api(logbroker_config, local_yt, local_yt_and_yql_env, frozen_time):
    env = dict(local_yt_and_yql_env)
    env[time_utils.CRYPTA_FROZEN_TIME_ENV] = frozen_time
    with yatest.common.network.PortManager() as port_manager:
        with RuleEstimatorApi(
            working_dir=yatest.common.test_output_path(),
            port=port_manager.get_port(),
            logbroker_config=logbroker_config,
            yt_proxy=local_yt.get_server(),
            env=env,
        ) as service:
            yield service


@pytest.fixture
def rule_estimator_api_client(rule_estimator_api):
    return rule_estimator_api.create_client()


@pytest.fixture
def rule_estimator_api_config(rule_estimator_api):
    return rule_estimator_api.config


@pytest.fixture
def rule_estimator_worker(logbroker_config, clean_local_yt_with_chyt, rule_estimator_api, mock_siberia_core_server, mock_crypta_api, local_yt_and_yql_env, frozen_time):
    env = dict(local_yt_and_yql_env)
    env[time_utils.CRYPTA_FROZEN_TIME_ENV] = frozen_time
    with yatest.common.network.PortManager() as port_manager:
        with RuleEstimatorWorker(
            working_dir=yatest.common.test_output_path(),
            rule_estimator_api=rule_estimator_api,
            logbroker_config=logbroker_config,
            yt_proxy=clean_local_yt_with_chyt.get_server(),
            yt_operation_owners=[],
            siberia_host=mock_siberia_core_server.host,
            siberia_port=mock_siberia_core_server.port,
            api_url=mock_crypta_api.url_prefix,
            stats_host="localhost",
            stats_port=port_manager.get_port(),
            env=env,
        ) as service:
            yield service


@pytest.fixture
def rule_estimator_worker_config(rule_estimator_worker):
    return rule_estimator_worker.config
