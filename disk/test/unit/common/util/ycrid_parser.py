# -*- coding: utf-8 -*-

from unittest import TestCase
from nose_parameterized import parameterized

from mpfs.common.util.ycrid_parser import YcridParser


class UserAgentParserTestCase(TestCase):
    @parameterized.expand([
        ('andr-252b5bb4be50e863c507b97c688a8b86-webdav6g', True),
        ('rest_andr-1409925f1132983311b019ea608012ba-api01f', True),
        ('ios-252b5bb4be50e863c507b97c688a8b86-webdav6g', True),
        ('rest_ios-fbaecad291b35f636a8050550fe8e02a-api04e', True),
        ('win-ZOOr_50M_NAJ-2-webdav1j', False),
        ('mpfs-0cc998c8828406ea0c64b172a0595ff2-mpfs03f', False),
        ('', False),
        (None, False),
    ])
    def test_is_yandex_disk_mobile(self, user_agent, expceted):
        assert expceted == YcridParser.is_yandex_disk_mobile(user_agent)
