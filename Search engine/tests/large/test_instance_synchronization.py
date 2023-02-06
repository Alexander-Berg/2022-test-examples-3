# coding: utf-8
import logging
import os
import subprocess
import tempfile
import time

from search.zephyr.enigma.zephyr.models import YStage
from search.zephyr.proto.structures import instance_pb2, stage_pb2
from search.zephyr.services.zephyr import ServiceDiscoveryClient
from search.zephyr.services.zephyr import StorageClient
from search.zephyr.src.util import get_access_key_hash

from search.zephyr.tests.utils.common import get_search_path, remove_zephyr_instances
from search.zephyr.tests.utils.debug import upload_results
from search.zephyr.tests.utils.mock_data import get_instances, get_methods
from search.zephyr.tests.utils.test_case import ZephyrDBTestCase


class TestInstanceSynchronization(ZephyrDBTestCase):
    logger = logging.getLogger('ya.test')

    @classmethod
    def setUpClass(cls):
        super().setUpClass()

        cls.zephyr_path = os.path.join(get_search_path(), 'zephyr/bin/zephyr')
        cls.tracedump_path = os.path.join(get_search_path(), 'martylib/trace/tracedump/bin/tracedump')

        cls.port_1 = '8081'
        cls.port_2 = '8082'

        cls.zephyr_1_health_client = ServiceDiscoveryClient.from_address(address=f'localhost:{cls.port_1}')
        cls.zephyr_2_health_client = ServiceDiscoveryClient.from_address(address=f'localhost:{cls.port_2}')

        cls.zephyr_1_storage_client = StorageClient.from_address(address=f'localhost:{cls.port_1}')
        cls.zephyr_2_storage_client = StorageClient.from_address(address=f'localhost:{cls.port_2}')

        cls.zephyrs = []

    def test_instance_synchronization(self):
        """
        Launches 2 zephyr processes, then registers
        """
        # Init instances
        instance_a, _ = get_instances()

        with tempfile.TemporaryDirectory() as tempdir:
            with upload_results(tracedump_path=self.tracedump_path, tempdir=tempdir, upload=True):
                try:
                    os.mkdir(os.path.join(tempdir, self.port_1))
                    os.mkdir(os.path.join(tempdir, self.port_2))

                    # create base stage to keep our instances
                    YStage(
                        stage_pb2.Stage(
                            name='stage',
                            project='test_project',
                            access_key='access_key',
                            access_key_hash=get_access_key_hash(access_key='access_key'),
                            redirect='myhost',
                            static_bundle_version='0.0.1',
                        )
                    ).save()

                    self.zephyrs = [
                        subprocess.Popen(
                            (
                                self.zephyr_path,
                                '-p', port,
                                '--raft-instances', 'localhost:50056', 'localhost:50057',
                                '--raft-port', '50056' if port == self.port_1 else '50057',
                                'server', 'service-discovery-module', 'storage-module', 'zephyr-raft-module'
                            ),
                            stderr=subprocess.PIPE, stdout=subprocess.PIPE,
                            cwd=os.path.join(tempdir, port),
                            env={
                                **os.environ,
                                'MODE': 'TEST',
                                'YT_PATH': '//tmp',
                                'LOGGING_DIR': os.path.join(tempdir, port),
                            },
                        )
                        for port in (self.port_1, self.port_2, )
                    ]

                    # wait until all daemons are launched
                    time.sleep(20)

                    self.zephyr_2_health_client.report_health(
                        instance_pb2.HealthReport(
                            access_key='access_key',
                            instance=instance_a,
                        ),
                    )

                    time.sleep(10)

                    # check if instance is on both zephyrs
                    assert (
                        remove_zephyr_instances(self.zephyr_1_storage_client.list_instances(instance_pb2.InstanceFilter(with_methods=True))) ==
                        remove_zephyr_instances(self.zephyr_2_storage_client.list_instances(instance_pb2.InstanceFilter(with_methods=True)))
                    )

                    # save previous instance_a
                    copy_instance_a = instance_pb2.Instance()
                    copy_instance_a.CopyFrom(instance_a)

                    # modify instance_a
                    _, m_b = get_methods()

                    instance_a = instance_pb2.Instance(fqdn='alpha', methods=m_b, port=11)

                    # load updated instances to sd_b cache
                    self.zephyr_1_health_client.report_health(
                        instance_pb2.HealthReport(
                            access_key='access_key',
                            instance=instance_a,
                        ),
                    )

                    time.sleep(10)

                    # fetch new instances
                    instances_1 = remove_zephyr_instances(
                        self.zephyr_1_storage_client.list_instances(instance_pb2.InstanceFilter(with_methods=True))
                    ).objects
                    instances_2 = remove_zephyr_instances(
                        self.zephyr_2_storage_client.list_instances(instance_pb2.InstanceFilter(with_methods=True))
                    ).objects

                    # check if there is still only one instance
                    assert len(instances_1) == 1
                    assert len(instances_2) == 1

                    # check if instance was updated on both zephyrs
                    assert copy_instance_a != instances_1[0]
                    assert instances_1 == instances_2

                finally:
                    for zephyr in self.zephyrs:
                        zephyr.kill()
