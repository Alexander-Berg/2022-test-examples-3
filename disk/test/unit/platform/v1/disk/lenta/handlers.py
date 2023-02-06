# -*- coding: utf-8 -*-

from nose_parameterized import parameterized
from unittest import TestCase

from mpfs.common.util.user_agent_parser import UserAgentParser
from mpfs.platform.v1.disk.lenta.handlers import LentaResourcesHandler


class LentaResourceHandlerTestCase(TestCase):

    @parameterized.expand([
        ('ios_not_supported_1', 'Yandex.Disk {"os":"iOS","vsn":"2.32"}', False),
        ('ios_not_supported_2', 'Yandex.Disk {"os":"iOS","vsn":"2.34.1"}', False),
        ('ios_supported_1', 'Yandex.Disk {"os":"iOS","vsn":"2.35"}', True),
        ('ios_supported_2', 'Yandex.Disk {"os":"iOS","vsn":"2.36"}', True),
        ('android_not_supported_1', 'Yandex.Disk {"os":"android","vsn":"3.22"}', False),
        ('android_not_supported_2', 'Yandex.Disk {"os":"android","vsn":"3.24-1"}', False),
        ('android_supported_1', 'Yandex.Disk {"os":"android","vsn":"3.25"}', True),
        ('android_supported_2', 'Yandex.Disk {"os":"android","vsn":"3.26"}', True),
    ])
    def test_comparison(self, case_name, raw_user_agent, result):
        user_agent = UserAgentParser.parse(raw_user_agent)
        assert LentaResourcesHandler._does_mobile_client_support_photounlim(user_agent) is result
