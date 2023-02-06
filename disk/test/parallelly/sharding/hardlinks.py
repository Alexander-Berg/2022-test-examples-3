# -*- coding: utf-8 -*-
import pytest

from base import BaseShardingMethods

from mpfs.config import settings


class HardlinksTestCase(BaseShardingMethods):
    def setup_method(self, method):
        super(HardlinksTestCase, self).setup_method(method)
        settings.mongo['options']['new_registration'] = True
        self.create_user(self.uid, noemail=1)
        settings.mongo['options']['new_registration'] = False
        self.create_user(self.old_scheme_uid, noemail=1)

    def test_upload_hardlinked_file(self):
        self.upload_and_hardlink(self.uid, self.uid)
