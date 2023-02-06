# coding=utf-8
from datetime import date

from travel.avia.library.python.tester.factories import get_model_factory, create_settlement
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.avia_data.models.national_version import NationalVersion

from travel.avia.library.python.common.models.partner import PriceList, DefaultClickPrice
from travel.avia.library.python.common.cache.price_list import PriceListCache


class TestPriceListRepository(TestCase):
    def setUp(self):
        self.default_price = 30
        self.price = 100
        price_list_factory = get_model_factory(PriceList)
        click_price_factory = get_model_factory(DefaultClickPrice)
        self.national_versions = {
            nv.code: nv for nv in NationalVersion.objects.all()
        }
        nv = self.national_versions['ru']
        s1 = create_settlement(id=1)
        s2 = create_settlement(id=2)
        price_list_factory(
            settlement_from=s1, settlement_to=s2, month=4,
            is_one_way=True,
            price=self.price,
            national_version=nv
        )
        for nv in self.national_versions.itervalues():
            click_price_factory(
                billing_price=self.default_price, national_version=nv
            )
        self._repository = PriceListCache()
        self._repository.pre_cache()

    def test_price_from_price_list(self):
        price = self._repository.get_click_price(
            1, 2, 'ru', date(2018, 4, 20), None, adults=1, children=0
        )
        assert self.price / 100. == price['price']
        assert self.price == price['price_cpa']

    def test_default_price(self):
        price = self._repository.get_click_price(
            1, 2, 'ru', date(2018, 1, 20), None, adults=1, children=0
        )
        assert self.default_price / 100. == price['price']
        assert self.default_price == price['price_cpa']

    def test_passengers_price(self):
        price = self._repository.get_click_price(
            1, 2, 'ru', date(2018, 4, 20), None, adults=2, children=1
        )
        assert self.price * 3 / 100. == price['price']
        assert self.price == price['price_cpa']

    def test_default_price_not_ru(self):
        for nv in self.national_versions:
            if nv != 'ru':
                price = self._repository.get_click_price(
                    1, 2, nv, date(2018, 4, 20), None, adults=2, children=1
                )
                assert self.default_price / 100. == price['price']
                assert self.default_price == price['price_cpa']

    def test_default_price_with_passengers(self):
        """не умножаем на пассажиров, если цена не из прайс-листа"""
        price = self._repository.get_click_price(
            1, 2, 'ru', date(2018, 1, 20), None, adults=2, children=1
        )
        assert self.default_price / 100. == price['price']
        assert self.default_price == price['price_cpa']
