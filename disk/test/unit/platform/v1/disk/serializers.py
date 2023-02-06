# -*- coding: utf-8 -*-
import json
import copy
from mpfs.platform.v1.disk.serializers import ResourceSerializer
from test.unit.base import NoDBTestCase


class ResourceIdTestCase(NoDBTestCase):
    def setup_method(self, method):
        with open('fixtures/json/root_resource_info_response.json') as fd:
            self.mpfs_response = json.loads(fd.read())

    def test_resource_id_in_resource_meta(self):
        obj = copy.deepcopy(self.mpfs_response)
        serializer = ResourceSerializer(obj=obj)
        self.assertEqual(serializer.data['resource_id'], obj['meta']['resource_id'])

    def test_no_resource_id_in_resource_meta(self):
        obj = copy.deepcopy(self.mpfs_response)
        serializer = ResourceSerializer(obj=obj)
        obj['meta'].pop('resource_id')
        obj['meta'].pop('file_id')
        self.assertEqual('resource_id' in serializer.data, False)
