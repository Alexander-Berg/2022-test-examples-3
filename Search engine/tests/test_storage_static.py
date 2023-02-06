# coding: utf-8
import base64

from grpc import StatusCode

from search.martylib.zipping import compress

from search.zephyr.proto.structures import stage_pb2, static_pb2
from search.zephyr.src.services.storage import Storage
from search.zephyr.tests.utils.test_case import ZephyrDBTestCase


class TestStorageStatic(ZephyrDBTestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()

        # request static
        cls.index_html = '<link rel="stylesheet" href="/zephyr/style.css">'
        cls.script_js = 'alert("test")'

        # result index_content
        cls.index_content = '<link rel="stylesheet" href="/zephyr/test_project-stage-0.0.1-style.css">'

    def test_storage_static(self):
        """
        Creates stage, then uploads static bundle and sets it as current
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

        # upload static bundle
        with self.mock_request(StatusCode.OK) as ctx:
            static_bundle = storage.upload_static_bundle(
                static_pb2.UploadStaticBundleRequest(
                    access_key=processed_stage.access_key,
                    version='0.0.1',
                    files={
                        'index.html': base64.encodebytes(self.index_html.encode('utf-8')),
                        'script_js': base64.encodebytes(compress(self.script_js)),  # also test compression support
                    }
                ),
                ctx
            )
        # verify static bundle
        assert static_bundle.index_content == self.index_content

        # set uploaded static bundle
        with self.mock_request(StatusCode.OK) as ctx:
            storage.set_static_bundle(
                static_pb2.SetStaticBundleRequest(
                    access_key=processed_stage.access_key,
                    version='0.0.1',
                ),
                ctx
            )

        storage.stage_state.sync()

        # get stage
        with self.mock_request(StatusCode.OK) as ctx:
            new_stage = storage.get_stage(
                stage_pb2.StageFilter(
                    access_key=processed_stage.access_key,
                ),
                ctx
            )

        storage.static_bundle_cache.sync()

        # static bundle should be set in stage
        assert new_stage.static_bundle_version == static_bundle.version
