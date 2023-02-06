# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to
from nose_parameterized import parameterized

from test.unit.base import NoDBTestCase

from mpfs.platform.utils import is_valid_uid


class IsValidUIDTestCase(NoDBTestCase):
    @parameterized.expand([
        ('yateam', 'yateam-1234567'),
        ('yaid', 'yaid-12345'),
        ('device', 'device-d40b39d1AAA3'),
        ('simple', '12350'),
    ])
    def test_valid_uid(self, case_name, uid):
        assert_that(is_valid_uid(uid), equal_to(True))

    @parameterized.expand([
        ('not_int_without_prefix', '32fa3afe1'),
        ('yateam_only_prefix', 'yateam-'),
        ('yaid_only_prefix', 'yaid'),
        ('device_only_prefix', 'device'),
        ('unknown_prefix', 'enot-12345'),
    ])
    def test_invalid_uid(self, case_name, uid):
        assert_that(is_valid_uid(uid), equal_to(False))
