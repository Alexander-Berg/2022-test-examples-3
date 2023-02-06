# coding: utf-8
import base64

from grpc import StatusCode

from search.zephyr.proto.structures import stage_pb2, static_pb2, instance_pb2
from search.zephyr.src.services.storage import Storage
from search.zephyr.src.services.discovery import ServiceDiscovery
from search.zephyr.src.generator.generator import Generator

from search.zephyr.tests.utils.mock_data import get_instances
from search.zephyr.tests.utils.test_case import ZephyrDBTestCase


class TestConfigGenerator(ZephyrDBTestCase):

    def test_config_generator(self):
        """
        Uploads instances, static and stage, then creates config by ConfigGenerator.build_config
        """
        storage = Storage()

        # add stage to the storage
        with self.mock_request(StatusCode.OK) as ctx:
            processed_stage = storage.put_stage(
                stage_pb2.Stage(
                    name='stage',
                    project='test_project',
                    access_key='access_key',
                    redirect='myhost',
                ), ctx
            )
        storage.stage_state.sync()

        # add static
        with self.mock_request(StatusCode.OK) as ctx:
            storage.upload_static_bundle(
                static_pb2.UploadStaticBundleRequest(
                    access_key=processed_stage.access_key,
                    version='0.0.1',
                    files={
                        'index.html': base64.encodebytes('test'.encode('utf-8')),
                        'script_js': base64.encodebytes('test'.encode('utf-8')),
                    }
                ),
                ctx
            )

        # create service discovery
        service_discovery = ServiceDiscovery()

        # Init instances
        instance_a, _ = get_instances()

        # load instances to service discovery
        with self.mock_request(StatusCode.OK) as ctx:
            service_discovery.report_health(instance_pb2.HealthReport(
                access_key=processed_stage.access_key,
                instance=instance_a,
            ), ctx)

        # create config generator
        config_generator = Generator()

        # build serval config
        serval_config = config_generator.generate_config()

        assert len(serval_config) == 4  # root, rpc, backends, ...
