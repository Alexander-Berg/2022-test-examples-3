# -*- coding: utf-8 -*-
from test.base import DiskTestCase
from test.fixtures.users import user_3

from mpfs.frontend.api.disk import Default

Default.user_ip = ''


class CommonShardingMethods(DiskTestCase):
    old_scheme_uid = user_3.uid
    mongodb_unit1 = 'disk_test_mongodb3-unit1'
    mongodb_unit2 = 'disk_test_mongodb3-unit2'

    def run_000_user_check(self, *args, **kwargs):
        pass

    def teardown_method(self, method):
        self.remove_created_users()
        super(CommonShardingMethods, self).teardown_method(method)
