# -*- coding: utf-8 -*-
from nose_parameterized import parameterized
from mpfs.core.services.clck_service import Clck
from test.unit.base import NoDBTestCase


class ClckServiceTestCase(NoDBTestCase):

    @parameterized.expand([
        ('https://yadi.sk/d/MgOQW9Eog-8lsw', True),
        ('http://yadi.sk/d/MgOQW9Eog-8lsw', True),
        ('https://yadi.sk/j/MgOQW9Eog-8lsw', True),
        ('https://yadi.sk/a/MgOQW9Eog-8lsw', True),
        ('https://yadi.sk/i/MgOQW9Eog-8lsw', True),
        ('https://telemost.yandex.ru/j/MgOQW9Eog-8lsw', True),
        ('https://telemost.dst.yandex.ru/j/MgOQW9Eog-8lsw', True),
        ('https://clck.dst.yandex.net/d/MgOQW9Eog-8lsw', True),
        ('https://yadi.sk/z/MgOQW9Eog-8lsw', False),
        ('ok /pinghttps://yadi.sk/i/u6beuqcETcLaNw', False),
        ('https://yadi.sk/i/eMiFwThFV4wk_w<!DOCTYPE HTML PUBLIC', False),
    ])
    def test_validate_short_url(self, short_url, is_valid):
        assert Clck.is_valid_short_url(short_url) is is_valid
