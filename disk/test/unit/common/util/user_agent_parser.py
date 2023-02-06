# -*- coding: utf-8 -*-

from unittest import TestCase
from nose_parameterized import parameterized

from mpfs.common.util.user_agent_parser import UserAgentParser


class UserAgentParserTestCase(TestCase):
    @parameterized.expand([
        ('Yandex.Disk {"os":"iOS","src":"disk.mobile","vsn":"2.14.7215","id":"E9C69BDA-0837-4867-A9B0-3AFCAAC3342A","device":"tablet"}', True),
        ('Yandex.Disk {"os":"android 6.0.1","device":"phone","src":"disk.mobile","vsn":"3.04-20002","id":"f412ce07323db938d57a19aba1dbe1d1"}', True),
        ('Yandex.Disk {"os":"win"}', False),
        ('Google Chrome', False),
        ('', False),
        (None, False),
    ])
    def test_is_yandex_disk_mobile(self, user_agent, expected):
        assert expected == UserAgentParser.is_yandex_disk_mobile(user_agent)

    @parameterized.expand([
        ('Yandex.Disk {"os":"iOS","src":"disk.mobile","vsn":"2.14.7215","id":"E9C69BDA-0837-4867-A9B0-3AFCAAC3342A","device":"tablet"}', 'E9C69BDA-0837-4867-A9B0-3AFCAAC3342A'),
        ('Yandex.Disk {"os":"android 6.0.1","device":"phone","src":"disk.mobile","vsn":"3.04-20002","id":"f412ce07323db938d57a19aba1dbe1d1"}', 'f412ce07323db938d57a19aba1dbe1d1'),
        ('Yandex.Disk {"os":"android 6.0.1","device":"phone","src":"disk.mobile","vsn":"3.04-20002",}', None),
        ('Google Chrome', None),
        ('', None),
        (None, None),
    ])
    def test_get_unique_id(self, user_agent, expected):
        assert expected == UserAgentParser.get_unique_id(user_agent)
