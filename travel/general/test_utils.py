# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

from travel.rasp.train_api.train_partners.base import Tax
from travel.rasp.train_api.train_partners.base.refund_amount import Blank
from travel.rasp.train_api.train_partners.base.train_details.parsers import BaseCoachDetails


def create_blank(id, amount):
    return Blank(
        id=id,
        amount=amount,
        tariff_vat=Tax(Decimal('1.1'), Decimal('11.11')),
        service_vat=Tax(Decimal('2.2'), Decimal('22.22')),
        commission_fee_vat=Tax(Decimal('3.3'), Decimal('33.33')),
        refund_commission_fee_vat=Tax(Decimal('4.4'), Decimal('44.44'))
    )


class CoachDetailsStub(BaseCoachDetails):
    def __init__(self, coach_type=None, number=None, owner=None, places=None, min_tariff=None, max_tariff=None,
                 service_tariff=None, can_choose_bedding=False, base_tariff=None, price_rules=None,
                 place_reservation_type=None):
        super(CoachDetailsStub, self).__init__()
        self.type = coach_type
        self.number = number
        self.owner = owner
        self.places = places or []
        self.min_tariff = min_tariff
        self.max_tariff = max_tariff
        self.service_tariff = service_tariff
        self.can_choose_bedding = can_choose_bedding
        self.price_rules = price_rules or []
        self.base_tariff = base_tariff
        self.place_reservation_type = place_reservation_type
