# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from django.utils import translation

from common.models.schedule import RThread
from common.models.transport import TransportType
from common.utils.title_generator import build_simple_title_common, DASH
from common.tester.factories import create_settlement, create_station
from common.tester.testcase import TestCase
from stationschedule.models import ZTablo2


class TestTabloLTitle(TestCase):
    def setUp(self):
        self.sett_from = create_settlement(title_ru=u'Русс откуда', title_uk=u'Ук відкіль')
        self.sett_to = create_settlement(title_ru=u'Русс куда', title_uk=u'Ук куди')
        self.aero_to = create_station(title_ru=u'Аэропорт куда', title_uk=u'Летоâище куди')

        self.title_common_settlements = build_simple_title_common(TransportType.objects.get(pk=TransportType.PLANE_ID),
                                                                  [self.sett_from, self.sett_to])
        self.title_common_mix = build_simple_title_common(TransportType.objects.get(pk=TransportType.PLANE_ID),
                                                          [self.sett_from, self.aero_to])

    def test_l_tablo_title(self):
        z_tablo = ZTablo2(title=u'Русское название табло', title_common=self.title_common_mix)

        with translation.override('ru'):
            assert z_tablo.L_title() == z_tablo.L_tablo_title() == u'Русское название табло'

        with translation.override('uk'):
            assert z_tablo.L_title() == z_tablo.L_tablo_title() == u'Ук відкіль {} Летоâище куди'.format(DASH)

        z_tablo.thread = RThread(title=u'Русское название нитки', title_common=self.title_common_settlements,
                                 is_manual_title=False)

        with translation.override('ru'):
            assert z_tablo.L_title() == u'Русское название нитки'
            assert z_tablo.L_tablo_title() == u'Русское название табло'
            assert z_tablo._L_title() == u'Русское название табло'

        with translation.override('uk'):
            assert z_tablo.L_title() == u'Ук відкіль {} Ук куди'.format(DASH)
            assert z_tablo.L_tablo_title() == u'Ук відкіль {} Летоâище куди'.format(DASH)
            assert z_tablo._L_title() == u'Ук відкіль {} Летоâище куди'.format(DASH)

        with translation.override('ru'):
            assert z_tablo.get_popular_title() == u'Русс откуда {} Русс куда'.format(DASH)
