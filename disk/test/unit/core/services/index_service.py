# -*- coding: utf-8 -*-
import datetime
import urlparse

from mpfs.core.services.index_service import SearchIndexer
from test.base import time_machine
from test.base_suit import set_up_open_url, tear_down_open_url
from test.unit.base import NoDBTestCase


class TestIndexerServiceTestCase(NoDBTestCase):
    index_data_sample = [
        {
            'action': 'modify',
            'ctime': 1530527400,
            'etime': None,
            'external_url': None,
            'file_id': '355f6ac1b9e5f626ddd4f71f4a0d53991ae286ebebdcbc5c0ef2948e4c098f6e',
            'id': '/disk/filesystem test folder/testissimo',
            'md5': '83e5cd52e94e3a41054157a6e33226f7',
            'mediatype': 'text',
            'mimetype': 'text/plain',
            'mtime': 1530527400,
            'name': 'testissimo',
            'operation': 'mkfile',
            'resource_id': '415264988:355f6ac1b9e5f626ddd4f71f4a0d53991ae286ebebdcbc5c0ef2948e4c098f6e',
            'size': 10000,
            'stid': '1000003.yadisk:89031628.249690056312488962060095667221',
            'type': 'file',
            'uid': 415264988,
            'version': 1530527400000000,
            'visible': 1
        }
    ]

    def test_pushes_to_indexer_sends_current_timestamp_in_qs(self):
        cur_timestamp = 1530527400
        out_requests = set_up_open_url()
        with time_machine(datetime.datetime.fromtimestamp(cur_timestamp)):
            SearchIndexer().push_change(self.index_data_sample)
        tear_down_open_url()
        assert len(out_requests) == 1
        indexer_push_url = out_requests.keys()[0]
        print(indexer_push_url)
        qs = urlparse.parse_qs(urlparse.urlparse(indexer_push_url).query)
        assert 'timestamp' in qs
        assert len(qs['timestamp']) == 1
        assert int(qs['timestamp'][0]) == cur_timestamp

