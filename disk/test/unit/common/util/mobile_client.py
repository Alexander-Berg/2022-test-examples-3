# -*- coding: utf-8 -*-

from unittest import TestCase

from nose_parameterized import parameterized

from mpfs.common.util.mobile_client import MobileClientVersion
from mpfs.common.util.user_agent_parser import UserAgentParser


class MobileClientVersionTestCase(TestCase):

    @parameterized.expand([
        ('1.2', '1.1'),
        ('2.1', '1.1'),
        ('2.2', '1.1'),
        ('2.2.1234', '1.1'),
        ('2.2', '1.1.234'),
        ('2.10', '2.2'),
        ('4.15.0-10000', '4.14.9-10000'),
    ])
    def test_comparison_unequal(self, left_version, right_version):
        assert (MobileClientVersion.build_from_version(left_version)
                > MobileClientVersion.build_from_version(right_version))

    @parameterized.expand([
        ('2.2', '2.2'),
        ('2.2', '02.02'),
        ('2.2.3', '2.2.4'),
        ('2.2.3', '2.2'),
        ('4.14.3', '4.14.0-10000'),
        ('4.14.8-10000', '4.14.0-10000'),
    ])
    def test_comparison_equal(self, left_version, right_version):
        assert (MobileClientVersion.build_from_version(left_version)
                == MobileClientVersion.build_from_version(right_version))

    @parameterized.expand([
        ('Yandex.Disk {"os":"iOS","vsn":"2.31"}',),
        ('Yandex.Disk {"os":"iOS","vsn":"2.31.213"}',),
        ('Yandex.Disk {"os":"iOS","vsn":"2.31-213"}',),
        ('Yandex.Disk {"os":"iOS","vsn":"2.31-213"}',),
        ('Yandex.Disk {"os":"iOS","vsn":"02.31-213"}',),
        ('Yandex.Disk {"os":"android","vsn":"2.31"}',),
        ('Yandex.Disk {"os":"android","vsn":"2.31-123"}',),
        ('Yandex.Disk {"os":"android","vsn":"2.31.123"}',),
        ('Yandex.Disk {"os":"android","vsn":"4.14.0-10000"}',),
        ('Yandex.Disk {"os":"android","vsn":"4.14.12-10000"}',),
    ])
    def test_valid_versions(self, raw_user_agent):
        user_agent = UserAgentParser.parse(raw_user_agent)
        assert user_agent.get_version() is not None

    @parameterized.expand([
        ('Yandex.Disk {"os":"iOS","vsn":"2-31"}',),
        ('Yandex.Disk {"os":"android","vsn":"2-31"}',),
        ('Yandex.Disk {"os":"fake","vsn":"2.31"}',),
        ('Yandex.Disk {"os":"iOS","vsn":"2.3test1"}',),
        ('Yandex.Disk {"os":"android","vsn":"231"}',),
        ('Yandex.Disk {"os":"android","vsn":"1.2.3.4"}',),
        ('Yandex.Disk {"os":"android","vsn":"4.14.1.10000"}',),
    ])
    def test_invalid_versions(self, raw_user_agent):
        user_agent = UserAgentParser.parse(raw_user_agent)
        assert user_agent.get_version() is None
