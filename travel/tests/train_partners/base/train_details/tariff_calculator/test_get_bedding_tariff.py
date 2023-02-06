# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.testcase import TestCase
from travel.rasp.train_api.train_partners.base.train_details.tariff_calculator.tariff_calculator import get_bedding_tariff


class TestGetBeddingTariff(TestCase):
    def test_compartment(self):
        assert get_bedding_tariff('compartment', 150) == 0

    def test_platzkarte(self):
        assert get_bedding_tariff('platzkarte', 150) == 150
