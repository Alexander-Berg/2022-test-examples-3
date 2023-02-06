# -*- coding: utf-8 -*-
from httplib import LOCKED
from datetime import datetime
from dateutil.relativedelta import relativedelta
from hamcrest import assert_that, equal_to

from test.base import DiskTestCase, time_machine

from mpfs.common.util.limits.errors import UploadTrafficLimitExceeded
from mpfs.common.util.limits.utils import UploadLimitPeriods
from mpfs.common.util.size.datasize import DataSize
from mpfs.core.filesystem.quota import Quota


class UploadTrafficLimitTestCase(DiskTestCase):
    def setup_method(self, method):
        super(UploadTrafficLimitTestCase, self).setup_method(method)
        self.quota = Quota()

    def test_update_upload_traffic(self):
        first_update = DataSize.parse('11GB').to_bytes()
        self.quota.update_upload_traffic(self.uid, first_update)
        second_update = DataSize.parse('3GB').to_bytes()
        self.quota.update_upload_traffic(self.uid, second_update)

        for period in UploadLimitPeriods:
            assert_that(self.quota.upload_traffic(self.uid, period.value),
                        equal_to(first_update + second_update))

    def test_reset_upload_traffic(self):
        first_update = DataSize.parse('11GB').to_bytes()
        self.quota.update_upload_traffic(self.uid, first_update)

        now_dt = datetime.now()
        second_update = DataSize.parse('11GB').to_bytes()
        with time_machine(now_dt + relativedelta(days=15)):
            self.quota.update_upload_traffic(self.uid, second_update)

        assert_that(self.quota.upload_traffic(self.uid, UploadLimitPeriods.DAILY.value),
                    equal_to(second_update))
        assert_that(self.quota.upload_traffic(self.uid, UploadLimitPeriods.WEEKLY.value),
                    equal_to(second_update))

        assert_that(self.quota.upload_traffic(self.uid, UploadLimitPeriods.MONTHLY.value),
                    equal_to(first_update + second_update))

    def test_checker(self):
        already_uploaded_traffic = DataSize.parse('21GB').to_bytes()
        self.quota.update_upload_traffic(self.uid, already_uploaded_traffic)

        self.json_error('store', {'uid': self.uid, 'path': '/disk/test.txt'},
                        status=LOCKED, code=UploadTrafficLimitExceeded.code)
