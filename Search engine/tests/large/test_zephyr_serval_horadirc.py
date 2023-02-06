# coding: utf-8
import grpc
import os
import shutil
import subprocess
import tempfile
import time
import yatest

from search.martylib.core.exceptions import MaxRetriesReached, NotFound
from search.martylib.core.logging_utils import configure_binlog
from search.martylib.http import InstrumentedSession

from search.horadric2.hmock.proto import hmock_pb2
from search.horadric2.hmock.services.hmock import ServiceClient

from search.zephyr.enigma.zephyr.models import YStage, YStaticBundle
from search.zephyr.proto.structures import stage_pb2
from search.zephyr.proto.structures import static_pb2
from search.zephyr.src.util import get_access_key_hash

from search.zephyr.tests.utils.debug import upload_results
from search.zephyr.tests.utils.test_case import ZephyrDBTestCase
from search.zephyr.tests.utils.common import get_search_path


class TestZephyrServalHoradric(ZephyrDBTestCase):
    search_path = get_search_path()

    zephyr_path = os.path.join(search_path, 'zephyr/bin/zephyr')
    hmock_path = os.path.join(search_path, 'horadric2/hmock/bin/hmock')
    tracedump_path = os.path.join(search_path, 'martylib/trace/tracedump/bin/tracedump')

    root_path = search_path.split('search')[0]

    serval_path = os.path.join(root_path, 'balancer/serval/serval')
    logdump_path = os.path.join(root_path, 'balancer/serval/tools/logdump/logdump')

    hmock_access_key = 'hmock_access_key'
    hmock_access_key_hash = get_access_key_hash(access_key=hmock_access_key)

    serval_config_initial_path = yatest.common.source_path('search/zephyr/tests/large/data/serval_config.yaml')

    @classmethod
    def setUpClass(cls):
        super().setUpClass()

        configure_binlog(
            'zephyr-test',
            stdout=(
                'LOGGING_STDOUT' in os.environ
            ),
        )

        # create client with http connection
        cls.http_client = InstrumentedSession(
            base_url='http://localhost:8081',
            max_retries=3,
        )
        cls.http_client.headers.update({'Host': 'hmock-hmock_test.z.yandex-team.ru'})

        # create client with grpc connection
        cls.grpc_client = ServiceClient.from_address(address='localhost:8081', authority='hmock-hmock_test.z.yandex-team.ru')

        cls.processes = {}

    def test_zephyr_serval_horadric(self):
        """
        Zephyr registers 2 hmock instances and dumps config, then launches serval
        """

        with tempfile.TemporaryDirectory() as tempdir:
            with upload_results(tracedump_path=self.tracedump_path, tempdir=tempdir, upload=False):
                try:
                    # create folder for configs
                    os.mkdir(os.path.join(tempdir, 'configs'))

                    # copy serval config to the tempdir
                    shutil.copy(self.serval_config_initial_path, os.path.join(tempdir, 'serval_config.yaml'))

                    # create symlink to serval binary for zephyr
                    os.symlink(self.serval_path, os.path.join(self.search_path, 'zephyr/bin/serval'))

                    # add stage for Zephyr
                    # Zephyr's own stage is required for initial Serval configuration.
                    YStage(
                        stage_pb2.Stage(
                            name='zephyr_test',
                            project='zephyr',
                            redirect='local',
                            access_key='zephyr_access_key',
                            static_bundle_version='0.0.1',
                        )
                    ).save()

                    # add stage for hmock
                    YStage(
                        stage_pb2.Stage(
                            name='hmock_test',
                            project='hmock',
                            redirect='local',
                            access_key=self.hmock_access_key,
                            access_key_hash=self.hmock_access_key_hash,
                            static_bundle_version='0.0.1',
                        )
                    ).save()

                    # add initial static bundle
                    YStaticBundle(
                        static_pb2.StaticBundle(
                            project='hmock_test',
                            stage='local2',
                            version='0.0.1',
                        )
                    ).save()

                    self.processes['zephyr'] = subprocess.Popen(
                        (self.zephyr_path, '-p', '50051', '--', 'default', '-ZephyrRaftModule'),
                        stderr=subprocess.PIPE, stdout=subprocess.PIPE,
                        cwd=tempdir,
                        env={
                            **os.environ,
                            'MODE': 'TEST',
                            'YT_PATH': '//tmp',
                            'CELLARS_ROOT': os.path.join(tempdir, 'configs'),
                            'RELOAD_SERVAL': '1',
                            'LOGGING_DIR': tempdir,
                        },
                    )
                    time.sleep(80)

                    self.processes['serval'] = subprocess.Popen(
                        (self.serval_path, '-c', 'serval_config.yaml', '-l', 'serval_logs.log'),
                        stderr=subprocess.PIPE, stdout=subprocess.PIPE,
                        cwd=tempdir,
                        env={
                            **os.environ,
                            'MODE': 'TEST',
                        },
                    )

                    self.processes['hmock1'] = subprocess.Popen(
                        (self.hmock_path, '-p', '50054', '-e', 'localhost:50051'),
                        stderr=subprocess.PIPE, stdout=subprocess.PIPE,
                        cwd=tempdir,
                        env={
                            **os.environ,
                            'MODE': 'TEST',
                            'ZEPHYR_ACCESS_KEY': self.hmock_access_key,
                        },
                    )

                    self.processes['hmock2'] = subprocess.Popen(
                        (self.hmock_path, '-p', '50055', '-e', 'localhost:50051', '-o'),
                        stderr=subprocess.PIPE, stdout=subprocess.PIPE,
                        cwd=tempdir,
                        env={
                            **os.environ,
                            'MODE': 'TEST',
                            'ZEPHYR_ACCESS_KEY': self.hmock_access_key,
                        },
                    )

                    # wait until config is built
                    time.sleep(40)

                    # check http ok
                    assert self.http_client.post(
                        '/api/hmock.Service/handleRequest',
                        json={'code': 200},
                    ).status_code == 200

                    # check balancing
                    ports = set()
                    for _ in range(10):
                        ports.add(self.http_client.post(
                            '/api/hmock.Service/handleRequest',
                            json={'code': 200},
                        ).json()['port'])

                    assert len(ports) > 1

                    # check http internal error
                    with self.assertRaises(MaxRetriesReached):
                        self.http_client.post(
                            '/api/hmock.Service/handleRequest',
                            json={'code': 500},
                        )

                    # check http not found
                    with self.assertRaises(NotFound):
                        self.http_client.post(
                            '/api/hmock.Service/handleRequest',
                            json={'code': 404},
                        )

                    # check grpc ok
                    assert self.grpc_client.handle_request(request=hmock_pb2.Request(code=200)).code == 200

                    # check grpc internal error
                    try:
                        self.grpc_client.handle_request(request=hmock_pb2.Request(code=500))
                    except grpc.RpcError as e:
                        assert e.code() == grpc.StatusCode.INTERNAL

                    # check grpc not found
                    try:
                        self.grpc_client.handle_request(request=hmock_pb2.Request(code=404))
                    except grpc.RpcError as e:
                        assert e.code() == grpc.StatusCode.NOT_FOUND

                    # check crash
                    with self.assertRaises(MaxRetriesReached):
                        self.grpc_client.handle_request(request=hmock_pb2.Request(crash=True))

                finally:
                    for pr in self.processes.values():
                        pr.kill()
