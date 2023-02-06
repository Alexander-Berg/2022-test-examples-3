#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, HyperCategoryType, Model, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import Greater


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    @classmethod
    def prepare_discount_for_alcohol(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=16155381,
                name='Алкоголь',
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=16155466, name='Вино', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155647, name='Виски, бурбон', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155455, name='Водка', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155448, name='Коньяк, арманьяк, бренди', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155587, name='Крепкий алкоголь', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155526, name='Креплёное вино', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155651, name='Ликёры, настойки, аперитивы', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155476, name='Пиво и пивные напитки', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155504, name='Слабоалкогольные напитки', output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=16155560, name='Шампанское и игристое вино', output_type=HyperCategoryType.GURU),
                ],
            )
        ]

        cls.index.models += [
            Model(hyperid=100, hid=16155381, title='Алкашка'),
            Model(hyperid=101, hid=16155466, title='Винишко'),
            Model(hyperid=102, hid=16155647, title='Вискарик'),
            Model(hyperid=103, hid=16155455, title='Водочка'),
            Model(hyperid=104, hid=16155448, title='Коньячок'),
            Model(hyperid=105, hid=16155587, title='Крепак'),
            Model(hyperid=106, hid=16155526, title='Кабернешечка'),
            Model(hyperid=107, hid=16155651, title='Бейлис'),
            Model(hyperid=108, hid=16155476, title='Пивасик'),
            Model(hyperid=109, hid=16155504, title='Слабоалкашка'),
            Model(hyperid=110, hid=16155560, title='Шампусик'),
        ]

        cls.index.shops += [
            Shop(
                fesh=10774,
                priority_region=213,
                phone="+7222998989",
                phone_display_options='*',
                alcohol_status=Shop.ALCOHOL_REAL,
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=10774,
                hyperid=100,
                price=1000,
                price_old=1500,
                price_history=1600,
                feedid=200,
                offerid=300,
                alcohol=True,
            ),
            Offer(
                fesh=10774,
                hyperid=101,
                price=1000,
                price_old=1500,
                price_history=1600,
                feedid=200,
                offerid=301,
                alcohol=True,
            ),
            Offer(
                fesh=10774,
                hyperid=102,
                price=1000,
                price_old=1500,
                price_history=1600,
                feedid=200,
                offerid=302,
                alcohol=True,
            ),
            Offer(
                fesh=10774,
                hyperid=103,
                price=1000,
                price_old=1500,
                price_history=1600,
                feedid=200,
                offerid=303,
                alcohol=True,
            ),
            Offer(
                fesh=10774,
                hyperid=104,
                price=1000,
                price_old=1500,
                price_history=1600,
                feedid=200,
                offerid=304,
                alcohol=True,
            ),
            Offer(
                fesh=10774,
                hyperid=105,
                price=1000,
                price_old=1500,
                price_history=1600,
                feedid=200,
                offerid=305,
                alcohol=True,
            ),
            Offer(
                fesh=10774,
                hyperid=106,
                price=1000,
                price_old=1500,
                price_history=1600,
                feedid=200,
                offerid=306,
                alcohol=True,
            ),
            Offer(
                fesh=10774,
                hyperid=107,
                price=1000,
                price_old=1500,
                price_history=1600,
                feedid=200,
                offerid=307,
                alcohol=True,
            ),
            Offer(
                fesh=10774,
                hyperid=108,
                price=1000,
                price_old=1500,
                price_history=1600,
                feedid=200,
                offerid=308,
                alcohol=True,
            ),
            Offer(
                fesh=10774,
                hyperid=109,
                price=1000,
                price_old=1500,
                price_history=1600,
                feedid=200,
                offerid=309,
                alcohol=True,
            ),
            Offer(
                fesh=10774,
                hyperid=110,
                price=1000,
                price_old=1500,
                price_history=1600,
                feedid=200,
                offerid=310,
                alcohol=True,
            ),
        ]

    def test_alcohol_discount_in_offer_search(self):
        """
        Проверяем, что скидка на алкоголь есть
        в офферном поиске
        """
        response = self.report.request_json('place=prime&hyperid=100&rids=213&adult=1')
        self.assertFragmentIn(response, {'discount': {}})

    def test_alcohol_search_with_filter(self):
        """
        Проверяем, что в поиске нет ничего при установленном фильтре filter-promo-or-discount=1
        """
        response = self.report.request_json('place=prime&hid=16155381&filter-promo-or-discount=1')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 0,
                    'totalOffersBeforeFilters': Greater(0),  # до фильтрации есть хотя бы один
                }
            },
        )


if __name__ == '__main__':
    main()
