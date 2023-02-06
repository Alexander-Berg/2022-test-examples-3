# -*- coding: utf-8 -*-

import datetime

from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.scripts.schedule.bus.buscomua_cysix import get_vehicle_recommended_title


class TkvcMasksTest(TestCase):

    def setUp(self):
        self.get_new_title = get_vehicle_recommended_title

    def testAll(self):
        titles = [
            (u'МІЦУБІСІ', u'Мицубиси'),
            (u'ЄЛЬЧ', u'Ельч'),
            (u'ЄТАЛОН', u'Еталон'),
            (u'ЛАЗ-695#', u'ЛАЗ-695'),
            (u'Мерседес', u'Мерседес'),
            (u'БАЗ25', u'БАЗ 25'),
            (u'ЛАЗ699', u'ЛАЗ 699'),
            (u'ПАЗ672П', u'ПАЗ 672П'),
            (u'МерседесБенц413', u'Мерседес Бенц 413'),
            (u'ГАЗ -322132(13)', u'ГАЗ-322132 (13)'),
            (u'ГАЗ    -322132(13)', u'ГАЗ-322132 (13)'),
            (u'ГАЗ-    322132(13)', u'ГАЗ-322132 (13)'),
            (u'ГАЗ(13)', u'ГАЗ (13)'),
            (u'МаркоПоло', u'Марко Поло'),
            (u'мерседесБенц413', u'Мерседес Бенц 413'),
            (u'МерседесБенц413', u'Мерседес Бенц 413'),
            (u'ПАЗ', u'ПАЗ'),
            (u'ЛиАЗ', u'Лиаз'),
            (u'БогАЗ', u'Богаз'),
            (u'КИТ(YOU)-30', u'Кит(You)-30'),
            (u'ПАЗ-672(Ж)-24', u'ПАЗ-672(Ж)-24'),
            (u'MERSEDES BENZ0304', u'Mersedes Benz 0304'),

            (u'СЕТРА S315', u'Сетра S315'),
            (u'БАЗ АО79-29', u'БАЗ АО79-29'),
            (u'БАЗ АО79.04', u'БАЗ АО79.04'),
            (u'ФОЛЬЦВАГ-ЛТ35', u'Фольцваг-ЛТ35'),

            (u'МІЦУБІСІSAFIR', u'Мицубиси Safir'),

            (u'БАЗ"ЕТАЛОН"', u'БАЗ "Еталон"')
        ]

        for title, correct_title in titles:
            new_title = self.get_new_title(title)
            self.assertEqual(correct_title, new_title)