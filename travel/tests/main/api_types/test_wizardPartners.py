# -*- encoding: utf-8 -*-
from __future__ import absolute_import

import pytest

from travel.avia.library.python.tester.factories import create_partner, create_dohop_vendor, create_amadeus_merchant

from travel.avia.backend.main.api_types.wizardPartners import WizardPartnersHandler

WIZARD_NV = ('ru', 'ua', 'kz', 'tr')


def _get_logos(partner_id):
    return dict(
        dict.fromkeys(
            map(lambda nv: ('logo2_svg_{}'.format(nv)), WIZARD_NV),
            'logo_{}'.format(partner_id)
        ).items() +
        dict.fromkeys(
            # Чтобы не спамили стэктрейсы из process_svg2png_images в логи тестов во время создания AmadeusMerchant
            map(lambda nv: ('logo2_svg2png_{}'.format(nv)), WIZARD_NV),
            'logo_{}'.format(partner_id)
        ).items()
    )


def _make_partner(partner_id, create_func, **kwargs):
    enabled_in_wizard = dict.fromkeys(
        map(lambda nv: ('enabled_in_wizard_{}'.format(nv)), WIZARD_NV),
        True,
    )

    return create_func(
        code='partner_{}'.format(partner_id),
        id=partner_id,
        site_url='site_url_{}'.format(partner_id),
        title='title_{}'.format(partner_id),
        is_aviacompany=False,
        icon_svg='icon_{}'.format(partner_id),
        **dict(enabled_in_wizard.items() + _get_logos(partner_id).items() + kwargs.items())
    )


class TestWizardPartners:
    @pytest.mark.dbuser
    def test_wizard_partners(self, faker):
        def billing_ids(partner_id):
            return dict(billing_order_id=partner_id)

        partner1 = _make_partner(1, create_partner, **billing_ids(1))
        partner2 = _make_partner(2, create_partner, **billing_ids(2))
        dohop_meta = create_partner(code='dohop', id=666, **billing_ids(666))
        dohop_vendor = _make_partner(3, create_dohop_vendor, dohop_id=3, enabled=True)
        amadeus_meta = create_partner(code='amadeus', id=777, **billing_ids(777))
        amadeus_merchant = _make_partner(4, create_amadeus_merchant, merchant_id='ok', enabled=True)

        actual_response_content = WizardPartnersHandler()(None, None)
        expected_content = [{
            u'id': partner1.id,
            u'code': u'partner_1',
            u'enabled': True,
            u'siteUrl': u'site_url_1',
            u'isAviacompany': False,
            u'enabledInWizard': dict.fromkeys(WIZARD_NV, True),
            u'logosSvg': dict.fromkeys(WIZARD_NV, u'logo_{}/orig'.format(partner1.id)),
            u'iconSvg': u'icon_{}/orig'.format(partner1.id),
            u'titles': dict.fromkeys(WIZARD_NV, u'title_{}'.format(partner1.id)),
            u'billingOrderId': partner1.id,
        }, {
            u'id': partner2.id,
            u'code': u'partner_2',
            u'enabled': True,
            u'siteUrl': u'site_url_2',
            u'isAviacompany': False,
            u'enabledInWizard': dict.fromkeys(WIZARD_NV, True),
            u'logosSvg': dict.fromkeys(WIZARD_NV, u'logo_{}/orig'.format(partner2.id)),
            u'iconSvg': u'icon_{}/orig'.format(partner2.id),
            u'titles': dict.fromkeys(WIZARD_NV, u'title_{}'.format(partner2.id)),
            u'billingOrderId': partner2.id,
        }, {
            u'id': dohop_meta.id,
            u'code': u'dohop',
            u'enabled': True,
            u'siteUrl': None,
            u'isAviacompany': False,
            u'enabledInWizard': dict.fromkeys(WIZARD_NV, True),
            u'logosSvg': dict.fromkeys(WIZARD_NV, None),
            u'iconSvg': u'',
            u'titles': dict.fromkeys(WIZARD_NV, u''),
            u'billingOrderId': dohop_meta.id,
        }, {
            u'id': amadeus_meta.id,
            u'code': u'amadeus',
            u'enabled': True,
            u'siteUrl': None,
            u'isAviacompany': False,
            u'enabledInWizard': dict.fromkeys(WIZARD_NV, True),
            u'logosSvg': dict.fromkeys(WIZARD_NV, None),
            u'iconSvg': u'',
            u'titles': dict.fromkeys(WIZARD_NV, u''),
            u'billingOrderId': amadeus_meta.id,
        }, {
            u'id': dohop_vendor.id,
            u'code': u'dohop_3',
            u'enabled': True,
            u'siteUrl': u'site_url_3',
            u'isAviacompany': False,
            u'enabledInWizard': dict.fromkeys(WIZARD_NV, True),
            u'logosSvg': dict.fromkeys(WIZARD_NV, u'logo_{}/orig'.format(dohop_vendor.id)),
            u'iconSvg': u'icon_{}/orig'.format(dohop_vendor.id),
            u'titles': dict.fromkeys(WIZARD_NV, u'title_{}'.format(dohop_vendor.id)),
        }, {
            u'id': amadeus_merchant.id,
            u'code': u'amadeus_ok',
            u'enabled': True,
            u'siteUrl': u'site_url_4',
            u'isAviacompany': False,
            u'enabledInWizard': dict.fromkeys(WIZARD_NV, True),
            u'logosSvg': dict.fromkeys(WIZARD_NV, u'logo_{}/orig'.format(amadeus_merchant.id)),
            u'iconSvg': u'icon_{}/orig'.format(amadeus_merchant.id),
            u'titles': dict.fromkeys(WIZARD_NV, u'title_{}'.format(amadeus_merchant.id)),
        }]

        assert actual_response_content == expected_content
