# -*- coding: utf-8 -*-
import cjson
import urlparse

from mpfs.core.services.search_service import DiskSearch
from test.base_suit import set_up_open_url, tear_down_open_url
from test.helpers.stubs.services import SearchIndexerStub
from test.helpers.stubs.manager import StubsManager
from test.parallelly.json_api.base import CommonJsonApiTestCase


class ManipulationJsonApiTestCase(CommonJsonApiTestCase):

    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {SearchIndexerStub})

    def teardown_method(self, method):
        DiskSearch().delete(self.uid)
        super(ManipulationJsonApiTestCase, self).teardown_method(method)

    def test_rename_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder_with_file'})

        opts = {
            'uid': self.uid,
            'src': '/disk/folder_with_file',
            'dst': '/disk/folder_without_file',
        }
        open_url_data = set_up_open_url()
        oid = self.json_ok('async_move', opts)['oid']
        tear_down_open_url()
        opts = {
            'uid': self.uid,
            'oid': oid,
        }
        operation_status = self.json_ok('status', opts)
        self.assertEqual(operation_status['status'], 'DONE')
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_without_file',
            'meta': '',
        }
        folder_resource_id = self.json_ok('info', opts)['meta']['resource_id']

        found = False
        for k, v in open_url_data.iteritems():
            if self.search_url not in k:
                continue
            for each in v:
                qs_params = urlparse.parse_qs(urlparse.urlparse(each['args'][0]).query)
                found |= qs_params['resource_id'][0] == folder_resource_id
        self.assertTrue(found)
