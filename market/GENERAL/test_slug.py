#!/usr/bin/python
# -*- coding: utf-8 -*-
import unittest

from market.pylibrary import slug


class TestSlug(unittest.TestCase):
    def test_slug_transliteration(self):
        test_words = {
            'Модель': 'model',
            'ёлка ъ': 'elka',
            'hal9000': 'hal9000',
            '  processor  cores count — two  ': 'processor-cores-count-two',
            'съедобная пальма': 'sedobnaia-palma',
            'Утюг с парогенератором BARELLİ BSM 2000': 'utiug-s-parogeneratorom-barelli-bsm-2000',
            'Ноутбук HP 650 (H5K65EA) (Pentium 2020M 2400 Mhz/15.6\"/1366x768/2048Mb/320Gb/DVD-RW/Wi-Fi/Bluetooth/Linux)':
                'noutbuk-hp-650-h5k65ea-pentium-2020m-2400-mhz-15-6-1366x768-2048mb-320gb-dvd-rw-wi-fi-bluetooth-linux',
            'салон IßÀÁÂÃÄÅÆÇÈÉÊËÌİÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸĀĄČŁŒŚŞŠ¡IƑ': 'salon-issaaaaaaaeceeeeiiiiidnooooooeuuuuythyaacloesss-if',
            'Смесь NAN (Nestlé) Pre FM 85 (с рождения) 70 г': 'smes-nan-nestle-pre-fm-85-s-rozhdeniia-70-g',
            'Ксенон': 'ksenon',
            slug.u('Ксенон'): 'ksenon',
        }
        for title, expected in test_words.items():
            self.assertEqual(expected, slug.translit(title))

    def test_yandex_transliteration(self):
        test_words = {
            'Яндекс станция': 'yandex-stantsiia',
            'У Яндекса': 'u-yandexa',
            'Кому? Яндексу': 'komu-yandexu',
            'Яндекс Яндекс': 'yandex-yandex',
        }
        for title, expected in test_words.items():
            self.assertEqual(expected, slug.translit(title))

    def test_slug_lenth(self):
        """Длина slug не должна превышать 1000 символов"""
        result = slug.translit('Very long string'*1000)
        self.assertEqual(1000, len(result))

        # "-" на конце должен обрезаться
        result = slug.translit('qwe+rty+z+'*110)
        self.assertEqual(999, len(result))
