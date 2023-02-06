import pytest

from test.base import DiskTestCase

from mpfs.dao.session import Session
from test.conftest import INIT_USER_IN_POSTGRES


pytestmark = pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='postgres tests')


class AestheticsTestCase(DiskTestCase):
    def _save_aesthetics(self, uid, path, aesthetics):
        s = Session.create_from_uid(uid)
        s.execute(
            'UPDATE disk.files SET ext_aesthetics=:aesthetics WHERE uid=:uid AND fid=(SELECT fid FROM code.path_to_fid(:path,:uid))',
            {'uid': uid, 'path': path, 'aesthetics': aesthetics}
        )

    def _get_aesthetics(self, uid, path, aesthetics):
        s = Session.create_from_uid(uid)
        return s.execute(
            'SELECT ext_aesthetics FROM disk.files WHERE uid=:uid AND fid=(SELECT fid FROM code.path_to_fid(:path,:uid))',
            {'uid': uid, 'path': path, 'aesthetics': aesthetics}
        ).fetchone()[0]

    def test_aesthetics_on_hardlink(self):
        initial_file_path = '/disk/file.txt'
        aesthetics = 0.123
        self.upload_file(self.uid, initial_file_path)
        self._save_aesthetics(self.uid, initial_file_path, aesthetics)

        info_result = self.json_ok('info', {'uid': self.uid, 'path': initial_file_path, 'meta': 'md5,sha256,size'})
        size, sha256, md5 = info_result['meta']['size'], info_result['meta']['sha256'], info_result['meta']['md5']

        new_file_path = '/disk/new.txt'
        self.upload_file(self.uid, new_file_path, file_data={'size': size, 'sha256': sha256, 'md5': md5}, hardlink=True)

        assert aesthetics == self._get_aesthetics(self.uid, new_file_path, aesthetics)
