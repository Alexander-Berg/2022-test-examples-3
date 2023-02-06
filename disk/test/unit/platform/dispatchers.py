# -*- coding: utf-8 -*-
from mpfs.platform.dispatchers import BaseDispatcher
from test.unit.base import NoDBTestCase
from nose_parameterized import parameterized


class GetYcridPrefix(NoDBTestCase):
    @parameterized.expand([
        ('Yandex.Disk {"os":"windows"}', 'rest_win'),
        ('Yandex.Disk {"os":"android 6.0.1"}', 'rest_andr'),
        ('Yandex.Disk.2.0.Beta {"os":"windows"}', 'rest_win'),
        ('Yandex.Disk.2.0.Beta {"os":"mac"}', 'rest_mac'),
        ('Mozilla/5.0 (Linux; Android 4.1.2; GT-N8000 Build/JZO54K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.116 Safari/537.36', 'rest'),
        ('curl/7.45.0', 'rest'),
    ])
    def test_correct_ycrid_prefix(self, user_agent, ycrid_prefix):
        assert ycrid_prefix == BaseDispatcher._get_ycrid_prefix(user_agent)
