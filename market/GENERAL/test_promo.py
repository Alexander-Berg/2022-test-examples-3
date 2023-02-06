#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, Model, NewShopRating, Offer, Promo, PromoType, Shop

from core.matcher import Absent, NotEmpty

from datetime import datetime, date


class T(TestCase):
    # MARKETOUT-10347
    @classmethod
    def prepare_promo_place(cls):
        cls.index.shops += [
            Shop(fesh=1034701, priority_region=213),
        ]

        cls.index.offers += [
            Offer(
                title='offer 1034701_01',
                fesh=1034701,
                hyperid=1034701,
                waremd5='offer103470101_waremd5',
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    key='promo1034701_01_key000',
                ),
            )
        ]

    def test_promo_place_simple(self):
        response = self.report.request_json('place=promo&promoid=promo1034701_01_key000')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "promos": [
                        {
                            "type": "n-plus-m",
                            "key": "promo1034701_01_key000",
                            "description": "Promo description for #promo1034701_01_key000",
                            "termsAndConditions": "Terms and conditions for #promo1034701_01_key000",
                            "bonusItems": [],
                        }
                    ],
                }
            },
        )

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='1')

    @classmethod
    def prepare_promo_with_offer(cls):
        cls.index.shops += [Shop(fesh=1184201, priority_region=213)]

        cls.index.offers += [
            Offer(fesh=1184201, hyperid=1184201, price=2000, waremd5='offer_for_gift_1184201'),
            Offer(
                title='offer 11842_01',
                fesh=1184201,
                hyperid=1184202,
                waremd5='offer118420101_waremd5',
                promo=Promo(
                    key='promo1184201_01_key000',
                    promo_type=PromoType.SECOND_OFFER_FOR_FIXED_PRICE,
                ),
            ),
            Offer(fesh=1184201, hyperid=1184203, price=2000, waremd5='offer_for_gift_1184203'),
            Offer(
                title='offer 11842_04',
                fesh=1184201,
                hyperid=1184204,
                waremd5='offer118420404_waremd5',
                promo=Promo(
                    key='promo1184204_01_key000',
                    promo_type=PromoType.SECOND_OFFER_DISCOUNT,
                    start_date=datetime(date.today().year - 2, 1, 1),
                    end_date=datetime(date.today().year + 2, 1, 1),
                ),
            ),
            Offer(
                title='offer 11842_05',
                fesh=1184201,
                hyperid=1184205,
                waremd5='offer118420405_waremd5',
                promo=Promo(
                    key='promo1184205_01_key000',
                    promo_type=PromoType.SECOND_OFFER_DISCOUNT,
                    start_date=datetime(date.today().year + 2, 1, 1),
                    end_date=datetime(date.today().year + 3, 1, 1),
                ),
            ),
            Offer(
                title='offer 11842_06',
                fesh=1184201,
                hyperid=1184206,
                waremd5='offer118420406_waremd5',
                promo=Promo(
                    key='promo1184206_01_key000',
                    promo_type=PromoType.SECOND_OFFER_DISCOUNT,
                    start_date=datetime(date.today().year - 2, 1, 1),
                ),
            ),
            Offer(
                title='offer 11842_07',
                fesh=1184201,
                hyperid=1184207,
                waremd5='offer118420407_waremd5',
                promo=Promo(
                    key='promo1184207_01_key000',
                    promo_type=PromoType.SECOND_OFFER_DISCOUNT,
                    end_date=datetime(date.today().year + 2, 1, 1),
                ),
            ),
            Offer(
                title='offer 11842_08',
                fesh=1184201,
                hyperid=1184208,
                waremd5='offer118420408_waremd5',
                promo=Promo(
                    key='promo1184208_01_key000',
                    promo_type=PromoType.SECOND_OFFER_DISCOUNT,
                    end_date=datetime(date.today().year - 2, 1, 1),
                ),
            ),
            Offer(
                title='offer 11842_09',
                fesh=1184201,
                hyperid=1184209,
                waremd5='offer118420409_waremd5',
                promo=Promo(
                    key='promo1184209_01_key000',
                    promo_type=PromoType.SECOND_OFFER_DISCOUNT,
                    start_date=datetime(date.today().year + 2, 1, 1),
                ),
            ),
        ]

    def test_promo_with_date(self):
        response_with_now_date = self.report.request_json('place=promo&promoid=promo1184204_01_key000')
        self.assertFragmentIn(response_with_now_date, {'promos': [{}]})
        response_with_future_date = self.report.request_json('place=promo&promoid=promo1184205_01_key000')
        self.assertFragmentNotIn(response_with_future_date, {'promos': [{}]})
        response_wo_date = self.report.request_json('place=promo&promoid=promo1184201_01_key000')
        self.assertFragmentIn(response_wo_date, {'promos': [{}]})
        response_wo_end_date = self.report.request_json('place=promo&promoid=promo1184206_01_key000')
        self.assertFragmentIn(response_wo_end_date, {'promos': [{"startDate": NotEmpty()}]})
        self.assertFragmentIn(response_wo_end_date, {'promos': [{"endDate": Absent()}]})
        response_wo_start_date = self.report.request_json('place=promo&promoid=promo1184207_01_key000')
        self.assertFragmentIn(response_wo_start_date, {'promos': [{"endDate": NotEmpty()}]})
        self.assertFragmentIn(response_wo_start_date, {'promos': [{"startDate": Absent()}]})
        response_with_bad_end_date = self.report.request_json('place=promo&promoid=promo1184208_01_key000')
        self.assertFragmentNotIn(response_with_bad_end_date, {'promos': [{}]})
        response_with_bad_start_date = self.report.request_json('place=promo&promoid=promo1184209_01_key000')
        self.assertFragmentNotIn(response_with_bad_start_date, {'promos': [{}]})

    @classmethod
    def prepare(cls):
        cls.index.hypertree += [
            HyperCategory(hid=18757000),
        ]

        cls.index.shops += [
            Shop(
                fesh=18757000, priority_region=214, regions=[225], new_shop_rating=NewShopRating(new_rating_total=1.0)
            ),
        ]

        cls.index.models += [
            Model(hyperid=18757000, title='boring_model', hid=18757000),
        ]

        cls.index.offers += [
            Offer(title='yet_another_productoffers_offer', hyperid=18757000),
        ]

    def test_default_offer_places_support_promo_filters(self):
        # запрашиваем модель, у которой только 1 оффер без скидки, без акции и вообще так себе.
        # проверяем что в ответе пусто (статистика считается нормально, но в results может проникать ДО)
        # тестируем все фильтры сразу (почему бы и нет) - а затем все по-одному
        # в догонку делаем 1 тест со всеми фильтрами и экспериментом по отключению промо-фильтров
        # (&rearr-factors=market_remove_promos=1). Хотя он нам мало чем поможет, оффер то один и тот же попадет.
        cgiPromoFilters = [
            "filter-discount-only=1",
            # "promo-type=all", # не отфильтроввывает простой оффер (может и не должен?)
            "promo-type=market",
            "filter-promo-or-discount=1",
            # "filter-goods-of-the-week=1", # не используется для фильтрации - только задаётся на мете.
            "filter-by-promo-id=xMpQQQC5I4INzFCab3WEmw",
        ]

        places_to_check = ["productoffers", "defaultoffer"]
        for place in places_to_check:
            response = self.report.request_json(
                'place={}&offers-set=list,default&hyperid=18757000&{}'.format(place, "&".join(cgiPromoFilters))
            )
            self.assertFragmentIn(response, {"search": {"total": 0, "results": []}}, allow_different_len=False)

            for cpf in cgiPromoFilters:
                response = self.report.request_json(
                    'place={}&offers-set=list,default&hyperid=18757000&{}'.format(place, cpf)
                )
                self.assertFragmentIn(response, {"search": {"total": 0, "results": []}}, allow_different_len=False)

            response = self.report.request_json(
                'place={}&offers-set=list,default&hyperid=18757000&{}&rearr-factors=market_remove_promos=1'.format(
                    place, "&".join(cgiPromoFilters)
                )
            )
            if place == "productoffers":
                self.assertFragmentIn(response, {"search": {"total": 1, "results": [{}]}}, allow_different_len=False)
            elif place == "defaultoffer":
                self.assertFragmentIn(response, {"search": {"total": 1, "results": [{}]}}, allow_different_len=False)
            else:
                raise RuntimeError("Поддержаны только следующие плейсы: {}".format(",".join(places_to_check)))


if __name__ == '__main__':
    main()
