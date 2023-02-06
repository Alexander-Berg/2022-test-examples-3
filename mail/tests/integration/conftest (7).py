from abc import abstractmethod
import logging
import pytest
import random
import string
import sys
import jsonschema
import json
import os

from library.python.testing.pyremock.lib.pyremock import MockHttpServer

from mail.devpack.lib.components.sharpei import SharpeiWithBlackboxMock, SharpeiCloud
from mail.devpack.tests.helpers.fixtures import coordinator_context

import tests_common.pytest_bdd

from mail.devpack.lib import config_master
from yatest.common import network, output_path, source_path

from util import assign_default_values_to_missed_properties

from hamcrest import (
    anything,
    equal_to,
    has_entries,
)

from library.python.testing.pyremock.lib.pyremock import MatchRequest, MockResponse


def generate_instance_with_role(context, role):
    return {"type": role, "port": context.pm.get_port()}


def generate_cloud_shard_cfg(context, shard_id, hosts_info):
    return {
        "dbs": [generate_instance_with_role(context, role) for role in ['master'] * hosts_info['master_hosts'] + ['replica'] * hosts_info['replica_hosts']],
        "name": "shard" + str(shard_id),
        "id": shard_id
    }


def generate_shard_cfg(context, shard_id, hosts_info):
    return {
        "dbs": [generate_instance_with_role(context, role) for role in ['master'] * hosts_info['master_hosts'] + ['replica'] * hosts_info['replica_hosts']],
        "reg_weight": 1,
        "name": "shard" + str(shard_id),
        "id": shard_id
    }


def get_test_settings(module_name):
    TEST_SETTINGS = 'settings'
    imp = __import__(module_name, None, None, [TEST_SETTINGS], 0)
    settings = getattr(imp,  TEST_SETTINGS, dict())
    settings_schema = json.load(open(source_path('mail/sharpei/tests/integration/schemas/test_settings.json'), 'r'))
    assign_default_values_to_missed_properties(settings, settings_schema)
    jsonschema.validate(settings, schema=settings_schema)
    return settings


def get_logger(name):
    log = logging.getLogger(name)
    stderr_handler = logging.StreamHandler(sys.stderr)
    stderr_handler.setFormatter(logging.Formatter('[%(asctime)s] %(message)s'))
    log.addHandler(stderr_handler)
    log.setLevel(logging.DEBUG)
    return log


class BaseContextProvider:
    @abstractmethod
    def context(self, request, test_settings):
        pass

    @abstractmethod
    def generate_config(self, context, test_settings, sharpei_type):
        pass

    @abstractmethod
    def finalizer(self, context):
        pass

    def prepare_config(self, context, test_settings, sharpei_type):
        config = self.generate_config(context, test_settings, sharpei_type)
        config['kind'] = test_settings['kind']
        config_path = output_path('config.yml')
        config_master.write(config, config_path)
        return config_path


class CloudSharpeiContextProvider(BaseContextProvider):
    def context(self, request, test_settings):
        context = tests_common.pytest_bdd.context
        context.provider = self
        context.id_generator = make_id_generator()
        context.request_id = None
        context.log = get_logger(request.module.__name__)
        config_path = self.prepare_config(context, test_settings, sharpei_type=SharpeiCloud)
        context.iam_server = MockHttpServer(self.__iam_port)
        context.yc_server = MockHttpServer(self.__yc_port)
        context.iam_server.start()
        context.yc_server.start()
        CloudSharpeiContextProvider.setup_mocks(context.iam_server, context.yc_server)
        with coordinator_context(SharpeiCloud, use_test_env=True, config_path=config_path) as coord:
            context.coord = coord
            yield context

    def generate_config(self, context, test_settings, sharpei_type):
        context.pm = network.PortManager()
        gp = lambda port: context.pm.get_port(port)
        config = config_master.generate_config(gp, "devpack", sharpei_type)
        config['cloud_cluster']['shards'] = [generate_cloud_shard_cfg(context, shard_id + 1, hosts_info) for shard_id, hosts_info in enumerate(test_settings['shards'])]
        assert len(config['cloud_cluster']['shards']) == 1 and len(config['cloud_cluster']['shards'][0]['dbs']) == 1
        # assert is needed, because you can't configure the service to have multiple databases on the same host.
        # TODO: delete assert when it becomes possible to configure the service differently
        self.__iam_port = context.pm.get_port()
        self.__yc_port = context.pm.get_port()
        config['sharpei_cloud']['iam_port'] = self.__iam_port
        config['sharpei_cloud']['yc_port'] = self.__yc_port
        return config

    def finalizer(self, context):
        context.iam_server.stop()
        context.yc_server.stop()

    @staticmethod
    def read_file_from_integration(rel_path):
        with open(source_path(os.path.join('mail/sharpei/tests/integration', rel_path))) as file:
            return file.read()

    @staticmethod
    def setup_mocks(iam_server, yc_server):
        yc_server.expect(
            request=MatchRequest(
                method=equal_to('get'),
                path=equal_to('/yc/managed-postgresql/v1/clusters/my_cluster_id/hosts'),
                headers=has_entries(**{
                'X-Request-Id': anything(),
                'Authorization': equal_to(['Bearer iam_token'])
                })
            ),
            response=MockResponse(status=200, body=CloudSharpeiContextProvider.read_file_from_integration('cloud/enviroment/hosts_response.json')),
            times=1000000
        )
        iam_server.expect(
            request=MatchRequest(
                method=equal_to('post'),
                path=equal_to('/iam/v1/tokens'),
                headers=has_entries(**{
                    'X-Request-Id': anything(),
                    'Content-Type': equal_to(['application/json'])
                }),
            ),
            response=MockResponse(status=200, body=CloudSharpeiContextProvider.read_file_from_integration('cloud/enviroment/iam_tokens_response.json')),
            times=1000000
        )


class MainSharpeiContextProvider(BaseContextProvider):
    def context(self, request, test_settings):
        context = tests_common.pytest_bdd.context
        context.provider = self
        context.id_generator = make_id_generator()
        context.request_id = None
        context.log = get_logger(request.module.__name__)
        config_path = self.prepare_config(context, test_settings, sharpei_type=SharpeiWithBlackboxMock)
        with coordinator_context(SharpeiWithBlackboxMock, use_test_env=True, config_path=config_path) as coord:
            context.coord = coord
            context.pyremock = MockHttpServer(coord.components[SharpeiWithBlackboxMock].pyremock_port())
            context.pyremock.start()
            yield context

    def generate_config(self, context, test_settings, sharpei_type):
        context.pm = network.PortManager()
        gp = lambda port: context.pm.get_port(port)
        config = config_master.generate_config(gp, "devpack", sharpei_type)
        config['mdb']['shards'] = [generate_shard_cfg(context, shard_id + 1, hosts_info) for shard_id, hosts_info in enumerate(test_settings['shards'])]
        return config

    def finalizer(self, context):
        context.pyremock.stop()


def ContextProviderFactory(kind):
    if kind == 'cloud':
        return CloudSharpeiContextProvider()
    else:
        return MainSharpeiContextProvider()


@pytest.fixture(scope='module', autouse=True)
def context(request):
    test_settings = get_test_settings(request.module.__name__)
    context_provider = ContextProviderFactory(test_settings['kind'])
    for ctx in context_provider.context(request, test_settings):
        yield ctx


@pytest.fixture(scope='module', autouse=True)
def setup_killer(request, context):
    request.addfinalizer(lambda: context.provider.finalizer(context))


def pytest_bdd_before_scenario(request, feature, scenario):
    request_id = generate_request_id()
    context = request.getfixturevalue('context')
    context.log.debug('Scenario: %s [%s]', scenario.name, request_id)
    context.scenario_name = scenario.name
    context.request_id = request_id


def pytest_bdd_before_step_call(request, feature, scenario, step, step_func, step_func_args):
    context = request.getfixturevalue('context')
    context.log.debug('Step %s [%s]', step.name, context.request_id)


def generate_request_id(length=8):
    return ''.join(random.choice(string.letters) for _ in xrange(length))


def make_id_generator():
    value = [random.randint(1, 100)]

    def impl():
        value[0] += random.randint(1, 100)
        return value[0]
    return impl
