# coding: utf-8
from search.martylib.core.exceptions import NotFound

from search.zephyr.proto.structures import stage_pb2
from search.zephyr.src.services.storage import Storage

from search.zephyr.tests.utils.test_case import ZephyrDBTestCase


class TestStorageStage(ZephyrDBTestCase):

    @staticmethod
    def remove_access_key(stage):
        stage_copy = stage_pb2.Stage()
        stage_copy.CopyFrom(stage)
        stage_copy.access_key = ''
        return stage_copy

    def get_stage_by_filter(self, storage, stage_filter):
        storage.stage_state.sync()
        with self.mock_request() as ctx:
            return storage.get_stage(stage_filter, ctx)

    def test_storage_stage_get(self):
        """
        Uploads stage, then tries to get it either by (name, project) or
        access_key or access_key_hash from storage
        """
        # create storage object
        storage = Storage()

        # create first stage
        stage = stage_pb2.Stage(
            name='stage1',
            project='test_project',
            redirect='myhost',
            static_bundle_version='0.0.1',
        )

        # add new stage to the storage
        with self.mock_request() as ctx:
            processed_stage = storage.put_stage(stage, ctx)

        # make sure access key has been set
        assert processed_stage.access_key
        assert processed_stage.access_key_hash

        # make sure that processed_stage is correct
        stage.access_key_hash = processed_stage.access_key_hash
        assert self.remove_access_key(processed_stage) == self.remove_access_key(stage)

        # get processed stage from storage by name and project
        found_stage = self.get_stage_by_filter(
            storage,
            stage_pb2.StageFilter(
                name=processed_stage.name,
                project=processed_stage.project,
            ),
        )
        # verify response
        assert self.remove_access_key(processed_stage) == self.remove_access_key(found_stage)

        # get processed stage from storage by access_key
        found_stage = self.get_stage_by_filter(
            storage,
            stage_pb2.StageFilter(
                access_key=processed_stage.access_key,
            ),
        )
        # verify response
        assert self.remove_access_key(processed_stage) == self.remove_access_key(found_stage)

        # get processed stage from storage by access_key_hash
        found_stage = self.get_stage_by_filter(
            storage,
            stage_pb2.StageFilter(
                access_key_hash=processed_stage.access_key_hash
            ),
        )
        # verify response
        assert self.remove_access_key(processed_stage) == self.remove_access_key(found_stage)

        # attempt to get processed stage from storage by incorrect access_key
        with self.assertRaises(NotFound):
            self.get_stage_by_filter(storage,  stage_pb2.StageFilter(access_key='incorrect_access_key'))

        # attempt to get processed stage from storage by empty filter
        with self.assertRaises(NotFound):
            self.get_stage_by_filter(storage,  stage_pb2.StageFilter())

    def test_storage_stage_delete(self):
        """
        Uploads stage, then deletes it
        """
        # create storage object
        storage = Storage()

        # create first stage
        stage = stage_pb2.Stage(
            name='stage1',
            project='test_project',
            access_key='access_key',
            redirect='myhost',
            static_bundle_version='0.0.1',
        )

        # add new stage to storage
        with self.mock_request() as ctx:
            processed_stage = storage.put_stage(stage, ctx)

        storage.stage_state.sync()

        # delete stage from storage
        with self.mock_request() as ctx:
            storage.delete_stage(
                stage_pb2.StageFilter(
                    access_key=processed_stage.access_key,
                    name=processed_stage.name,
                    project=processed_stage.project,
                ),
                ctx
            )

        # stage must be deleted,
        # attempt to get stage from the storage
        with self.assertRaises(NotFound):
            self.get_stage_by_filter(storage, stage_pb2.StageFilter(access_key=processed_stage.access_key))
