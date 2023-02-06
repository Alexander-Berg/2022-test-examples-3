# -*- coding: utf-8 -*-
from hamcrest import assert_that, has_entries

from mpfs.common.util.size.datasize import DataSize
from test.base import DiskTestCase


class FileSizeLimitTestCase(DiskTestCase):
    def test_default(self):
        space_info = self.json_ok('user_info', {'uid': self.uid})['space']

        assert_that(space_info, has_entries({'paid_filesize_limit': DataSize.parse('50GB').to_bytes(),
                                             'filesize_limit': DataSize.parse('1GB').to_bytes()}))
