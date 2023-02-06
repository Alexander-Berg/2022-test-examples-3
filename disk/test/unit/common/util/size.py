# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to, calling, raises
from nose_parameterized import parameterized

from mpfs.common.util.size.datasize import DataSize
from test.unit.base import NoDBTestCase


class DataSizeTestCase(NoDBTestCase):
    @parameterized.expand([
        ('negative', '-1', -1),
        ('GB', '7GB', 7 * (1 << 30)),
        ('more_than_kilo', '2000MB', 2000 * (1 << 20)),
        ('KB', '2KB', 2 * (1 << 10)),
        ('TB', '12TB', 12 * (1 << 40)),
        ('long', '7696581394432', 7696581394432),
    ])
    def test_parse(self, case_name, value, exptected_result):
        assert_that(DataSize.parse(value).to_bytes(), equal_to(exptected_result))

    @parameterized.expand([
        ('lower_case', '7gb'),
        ('unknown_suffix', '12T'),
    ])
    def test_parse_invalid(self, case_name, value):
        assert_that(calling(DataSize.parse).with_args(value), raises(ValueError))
