# -*- coding: utf-8 -*-
from test.base import DiskTestCase
from mpfs.engine.process import create_sys_users_and_repo
import mpfs.engine.process


class NotesInterfaceTestCase(DiskTestCase):
    def test_notes_init(self):
        share_uid = mpfs.engine.process.share_user()
        create_sys_users_and_repo()

        self.json_ok('mkdir', {'uid': share_uid, 'path': '/share/notes'})
        folder_path = '/share/notes/initial_ru'
        self.json_ok('mkdir', {'uid': share_uid, 'path': folder_path})
        self.json_ok('mkdir', {'uid': share_uid, 'path': '%s/a' % folder_path})
        self.json_ok('mkdir', {'uid': share_uid, 'path': '%s/b' % folder_path})

        # COPY share_uid:/share/notes/initial_ru -> self.uid:/notes/1234_1_926
        self.json_ok('notes_init', {'uid': self.uid, 'src': '/initial_ru', 'dst': '/1234_1_926', 'note_revision_created': '1', 'note_attachment_mtime': '2'})

        resp = self.json_ok('list', {'uid': self.uid, 'path': '/notes/1234_1_926', 'meta': 'note_attachment_mtime,note_revision_created'})
        assert len(resp) == 1 + 2
        for resource in resp[1:]:
            assert 'note_revision_created' in resource['meta']
            assert resource['meta']['note_revision_created'] == '1'
            assert 'note_attachment_mtime' in resource['meta']
            assert resource['meta']['note_attachment_mtime'] == '2'

    def test_overwrite_conflict_on_store_to_notes_returns_405(self):
        self.upload_file(self.uid, '/notes/somefile.txt')
        self.json_error(
            'store', {
                'uid': self.uid,
                'path': '/notes/somefile.txt',
                'force': 1,
            }
        )
        assert self.response.status == 405
