# -*- coding: utf-8 -*-
from mpfs.common.static import codes
from test.base import DiskTestCase

from mpfs.core.filesystem.resources.base import Resource

from helpers.stubs.services import SearchDBStub


class ImageDimensionsTestCase(DiskTestCase):
    def test_dimensions_from_database(self):
        file_path = '/disk/image.jpg'
        self.upload_file(self.uid, file_path, width=800, height=600)
        file_info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'file_id'})
        file_id = file_info['meta']['file_id']

        dimensions = self.json_ok('image_dimensions_by_file_id', {'uid': self.uid, 'file_id': file_id})
        self.assertDictEqual(dimensions, {
            'width': 800,
            'height': 600,
        })

    # def test_orient_dimensions_from_database(self):
    #     file_path = '/disk/image.jpg'
    #     self.upload_file(self.uid, file_path, width=800, height=600)
    #     file_info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'file_id'})
    #     file_id = file_info['meta']['file_id']
    #
    #     dimensions = self.json_ok('image_metadata_by_file_id', {'uid': self.uid, 'file_id': file_id})
    #     self.assertDictEqual(dimensions, {
    #         'width': 800,
    #         'height': 600,
    #     })

    def test_dimensions_from_search(self):
        file_path = '/disk/image.jpg'
        self.upload_file(self.uid, file_path)  # no width and height in database
        file_info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'file_id'})
        file_id = file_info['meta']['file_id']

        with SearchDBStub() as stub:
            stub.resources_info_by_file_ids.return_value = {
                'total': 1,
                'items': [{
                        'width': 800,
                        'height': 600,
                }]
            }
            dimensions = self.json_ok('image_dimensions_by_file_id', {'uid': self.uid, 'file_id': file_id})
            self.assertDictEqual(dimensions, {
                'width': 800,
                'height': 600,
            })

    def test_dimensions_for_not_found_in_search(self):
        file_path = '/disk/image.jpg'
        self.upload_file(self.uid, file_path)  # no width and height in database
        file_info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'file_id'})
        file_id = file_info['meta']['file_id']

        with SearchDBStub() as stub:
            stub.resources_info_by_file_ids.return_value = {
                'total': 0,
                'items': []
            }
            dimensions = self.json_ok('image_dimensions_by_file_id', {'uid': self.uid, 'file_id': file_id})
            self.assertDictEqual(dimensions, {
                'width': None,
                'height': None,
            })

    def test_dimensions_for_non_existing_resource(self):
        non_existing_file_id = Resource.generate_file_id(self.uid, '/disk/not-existing-path-tratata.txt')
        self.json_error('image_dimensions_by_file_id', {'uid': self.uid, 'file_id': non_existing_file_id},
                        code=codes.RESOURCE_NOT_FOUND)
