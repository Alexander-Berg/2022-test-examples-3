# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest

from travel.hotels.feeders.lib.base import DefaultLangsProvider


class TestDefaultLangsProvider(object):

    def test_ru_lang(self):
        assert 'ru' == DefaultLangsProvider().choose_room_language('ru', 'ru', 'en')
        assert 'ru' == DefaultLangsProvider().choose_room_language('ru', 'en', 'ru')

    def test_fr_lang(self):
        assert 'EN' == DefaultLangsProvider().choose_room_language('FR', 'FR', 'EN')
        assert 'EN' == DefaultLangsProvider().choose_room_language('FR', 'EN', 'FR')

    def test_aa_lang(self):
        assert 'aa' == DefaultLangsProvider().choose_room_language('aa', 'aa', 'bb')
        assert 'aa' == DefaultLangsProvider().choose_room_language('aa', 'bb', 'aa')

    def test_non_string_argument(self):
        with pytest.raises(ValueError):
            DefaultLangsProvider().choose_room_language('aa', 'bb', 1)
