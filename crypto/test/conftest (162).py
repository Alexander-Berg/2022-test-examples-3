import os

from cached_property import cached_property
from google.protobuf.empty_pb2 import Empty
import grpc
import pytest
import retry
import yatest.common
import yt.wrapper as yt

from crypta.lib.python import (
    templater,
    test_utils,
)
from crypta.lib.python.solomon.test_utils.mock_solomon_server import MockSolomonServer
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import utils
from crypta.siberia.bin.custom_audience.suggester.bin.service.lib.proto import (
    app_pb2,
    host_pb2,
    segment_pb2,
)
from crypta.siberia.bin.custom_audience.suggester.grpc import suggester_service_pb2_grpc

pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
]


def get_hosts_schema():
    return schema_utils.get_schema_from_proto(host_pb2.THost, key_columns=['host'])


def get_segments_schema():
    return schema_utils.get_schema_from_proto(segment_pb2.TSegment)


def get_apps_schema():
    return schema_utils.get_schema_from_proto(app_pb2.TApp)


@pytest.fixture(scope="module")
def update_period_seconds():
    return 5


@pytest.fixture(scope="module")
def hosts_table(module_yt_stuff):
    return utils.write_yson_table_from_file(
        module_yt_stuff.get_yt_client(),
        yatest.common.test_source_path("data/hosts.yson"),
        yt.TablePath("//hosts", schema=get_hosts_schema()),
    )


@pytest.fixture(scope="module")
def segments_table(module_yt_stuff):
    return utils.write_yson_table_from_file(
        module_yt_stuff.get_yt_client(),
        yatest.common.test_source_path("data/segments.yson"),
        yt.TablePath("//segments", schema=get_segments_schema()),
    )


@pytest.fixture(scope="module")
def apps_table(module_yt_stuff):
    return utils.write_yson_table_from_file(
        module_yt_stuff.get_yt_client(),
        yatest.common.test_source_path("data/apps.yson"),
        yt.TablePath("//apps", schema=get_apps_schema()),
    )


class Suggester(test_utils.TestBinaryContextManager):
    bin_path = "crypta/siberia/bin/custom_audience/suggester/bin/service/bin/crypta-siberia-custom-audience-suggester"
    app_config_template_path = "crypta/siberia/bin/custom_audience/suggester/docker/templates/service_config.template.yaml"

    def __init__(self, working_dir, solomon_port, yt_stuff, hosts_table, segments_table, apps_table, update_period_seconds):
        super(Suggester, self).__init__("CustomAudience Suggester", env={"SOLOMON_TOKEN": "fake"})

        self.working_dir = working_dir
        self.solomon_port = solomon_port
        self.yt_proxy = yt_stuff.get_server()
        self.hosts_table = hosts_table
        self.segments_table = segments_table
        self.apps_table = apps_table
        self.update_period_seconds = update_period_seconds

        self.port_manager = yatest.common.network.PortManager()
        self.host = "localhost"

    def _prepare_start(self):
        self.port = self.port_manager.get_port()

        if not os.path.isdir(self.working_dir):
            os.makedirs(self.working_dir)

        self.app_config_path = os.path.join(self.working_dir, "config.yaml")
        self._render_app_config()

        return [
            yatest.common.binary_path(self.bin_path),
            "--config", self.app_config_path,
        ]

    def _on_exit(self):
        self.port_manager.release()
        self.port = None

    def _render_app_config(self):
        template_params = dict({
            "port": self.port,
            "yt_proxies": [self.yt_proxy],
            "environment_type": "qa",

            "hosts_table": self.hosts_table,
            "segments_table": self.segments_table,
            "apps_table": self.apps_table,
            "update_period_seconds": self.update_period_seconds,

            "solomon_schema": "http",
            "solomon_host": "localhost",
            "solomon_port": self.solomon_port,

            "logs_dir": self.working_dir,
        })
        self.logger.info("App config template parameters = %s", template_params)
        templater.render_file(yatest.common.source_path(self.app_config_template_path), self.app_config_path, template_params)

    @cached_property
    def client(self):
        channel = grpc.insecure_channel("{}:{}".format(self.host, self.port))
        return suggester_service_pb2_grpc.TSuggesterServiceStub(channel)

    def _wait_until_up(self):
        @retry.retry(tries=100, delay=0.1)
        def check_is_up():
            assert self.client.Ready(Empty()).Message == "OK"

        check_is_up()


@pytest.fixture(scope="module")
def mock_solomon_server():
    with MockSolomonServer() as mock:
        yield mock


@pytest.fixture(scope="module")
def suggester(mock_solomon_server, module_yt_stuff, hosts_table, segments_table, apps_table, update_period_seconds):
    app_working_dir = yatest.common.test_output_path("suggester")
    with Suggester(
        working_dir=app_working_dir,
        solomon_port=mock_solomon_server.port,
        yt_stuff=module_yt_stuff,
        hosts_table=hosts_table,
        segments_table=segments_table,
        apps_table=apps_table,
        update_period_seconds=update_period_seconds,
    ) as suggester:
        yield suggester
