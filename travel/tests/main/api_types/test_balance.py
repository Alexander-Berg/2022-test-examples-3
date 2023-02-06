# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

from datetime import datetime

from travel.avia.library.python.avia_data.models import BalanceRedirect
from travel.avia.library.python.common.models.partner import Partner
from travel.avia.backend.tests.main.api_test import TestApiHandler


FAKE_DATE = datetime(2000, 1, 2, 12, 15, 0)
NATIONAL_VERSION = 'ru'
PP = 123


def prepare_simple_test():
    partner = Partner.objects.create(
        title="Test partner",
        code="test",
        billing_order_id=12345,
    )

    BalanceRedirect.objects.create(
        billing_order_id=partner.billing_order_id,
        partner=partner,
        eventdate=FAKE_DATE.date(),
        eventtime=FAKE_DATE.time(),
        filtered=False,
        national_version=NATIONAL_VERSION,
        pp=PP,
        show_id="",
    )

    return partner


def create_data(partner):
    return [{
        "name": "balance",
        "params": {
            "left_date": FAKE_DATE.strftime('%Y-%m-%d'),
            "right_date": FAKE_DATE.strftime('%Y-%m-%d'),
            "partner_code_list": [partner.code],
            "national_version_list": [NATIONAL_VERSION],
        },
        "fields": [
            "partner",
            "total"
        ]
    }]


class TestRecipeOffersHandler(TestApiHandler):
    def test_api_response(self):
        partner = prepare_simple_test()

        expected = {
            "status": "success",
            "data": [
                [
                    {
                        "partner": {
                            "logo": partner.get_national_logo(NATIONAL_VERSION),
                            "id": int(partner.id),
                            "logoSvg": partner.get_national_logo_svg(NATIONAL_VERSION),
                            "iconSvg": partner.get_icon_svg(),
                            "title": partner.title
                        },
                        "total": 1
                    }
                ]
            ]
        }

        data = self.api_data(create_data(partner))

        assert data == expected
