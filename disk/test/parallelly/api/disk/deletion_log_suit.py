# -*- coding: utf-8 -*-
import mock

from mpfs.common.static import tags
from mpfs.common.util import from_json
from test.base_suit import UserTestCaseMixin
from test.parallelly.api.disk.base import DiskApiTestCase


class DeletionLogHandlerTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        super(DeletionLogHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def test_adding_source_id_proxying_ok(self):
        deletion_log_revision = 1
        new_deletion_log_revision = 3
        return_value = {
            'items': [
                {
                    'resource_id': 'some_resource_id_1',
                    'hid': 'some_hid_1',
                    'uid': self.uid,
                    'is_live_photo': False,
                    'deletion_log_revision': 2,
                    'source_ids': ['111', '222']
                },
                {
                    'resource_id': 'some_resource_id_2',
                    'hid': 'some_hid_2',
                    'uid': self.uid,
                    'is_live_photo': True,
                    'deletion_log_revision': 3,
                    'source_ids': ['333', '444']
                },
            ],
            'total': 2,
            'deletion_log_revision': new_deletion_log_revision,
        }
        with self.specified_client(scopes=['cloud_api:disk.write']), \
                mock.patch('mpfs.core.base.read_deletion_log', return_value=return_value) as endpoint_mock:
            r = self.client.request('GET', 'disk/deletion-log', query={'deletion_log_revision': deletion_log_revision})
        assert endpoint_mock.call_args[0][0].uid == self.uid
        assert endpoint_mock.call_args[0][0].deletion_log_revision == deletion_log_revision
        response_body = from_json(r.content)
        assert response_body['deletion_log_revision'] == new_deletion_log_revision
        assert response_body['total'] == 2
        assert len(set([x['resource_id'] for x in response_body['items']])) == 2
        for x in response_body['items']:
            assert {'resource_id', 'is_live_photo', 'deletion_log_revision', 'source_ids'} == set(x.keys())
        assert (set([source_id['source_id'] for i in response_body['items'] for source_id in i['source_ids']])
                == {'111', '222', '333', '444'})
