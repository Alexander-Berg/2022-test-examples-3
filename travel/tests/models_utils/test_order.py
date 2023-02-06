# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import mock

from travel.avia.library.python.avia_data.models import AmadeusMerchant
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.jsonrpc.models_utils.order import get_partners_by_code, get_partner_by_code
from travel.avia.library.python.tester.factories import (
    create_partner, create_dohop_vendor, get_model_factory
)
from travel.avia.library.python.tester.testcase import TestCase


class TestPartners(TestCase):
    def setUp(self):
        reset_all_caches()

    @staticmethod
    def assertLogosEqual(partner, ru=None, com=None, ua=None, tr=None, kz=None):
        assert partner.get_logo(national_version='com') == com
        assert partner.get_logo(national_version='ru') == ru
        assert partner.get_logo(national_version='ua') == ua
        assert partner.get_logo(national_version='tr') == tr
        assert partner.get_logo(national_version='kz') == kz

    def test_empty_get_logo(self):
        p_code = 'test_partner'
        create_partner(code=p_code)
        partners = get_partners_by_code()
        self.assertLogosEqual(partners[p_code])

    def test_get_logo_with_default_from_ru(self):
        p_code = 'test_partner'
        logo2_svg_ru = 'logo_ru_url'
        logo2_svg_com = 'logo_com_url'
        create_partner(
            code=p_code, logo2_svg_ru=logo2_svg_ru, logo2_svg_com=logo2_svg_com
        )
        self.assertLogosEqual(
            get_partners_by_code()[p_code],
            ru=logo2_svg_ru, com=logo2_svg_com, ua=logo2_svg_ru, tr=logo2_svg_ru, kz=logo2_svg_ru
        )

    def test_dohop_get_logo(self):
        dohop_id = 666
        logo2_svg_ru = 'logo_ru_url'
        logo2_svg_com = 'logo_com_url'
        create_partner(code='dohop')
        create_dohop_vendor(
            dohop_id=dohop_id, logo2_svg_ru=logo2_svg_ru, logo2_svg_com=logo2_svg_com
        )
        p_code = 'dohop_%d' % dohop_id
        self.assertLogosEqual(
            get_partners_by_code()[p_code],
            ru=logo2_svg_ru, com=logo2_svg_com, ua=logo2_svg_ru, tr=logo2_svg_ru, kz=logo2_svg_ru
        )

    def test_amadeus_get_logo(self):
        merchant_id = 'zz'
        p_code = 'amadeus_%s' % merchant_id
        logo2_svg_ru = 'logo_ru_url'
        logo2_svg_com = 'logo_com_url'
        with mock.patch('travel.avia.library.python.avia_data.models.AmadeusMerchant.process_svg2png_images'):
            get_model_factory(AmadeusMerchant)(
                merchant_id=merchant_id, logo2_svg_ru=logo2_svg_ru, logo2_svg_com=logo2_svg_com
            )
        self.assertLogosEqual(
            get_partners_by_code()[p_code],
            ru=logo2_svg_ru, com=logo2_svg_com, ua=logo2_svg_ru, tr=logo2_svg_ru, kz=logo2_svg_ru
        )

    def test_get_partners_by_code(self):
        p_code = 'test_partner'
        p = get_partner_by_code(p_code)
        assert p is None
        create_partner(code=p_code)
        p = get_partner_by_code(p_code)  # Cache should be invalidated and be reseted
        assert p is not None
        assert p.code == p_code
