#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, NewShopRating, Offer, Picture, Promo, PromoPurchase, PromoType, Shop, Vendor
from core.matcher import Absent, NotEmpty
from core.types.picture import thumbnails_config


from datetime import datetime


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.hypertree += [
            HyperCategory(hid=1, uniq_name='Тракторы'),
            HyperCategory(hid=2, uniq_name='Котики'),
            HyperCategory(hid=4, uniq_name='Наковальни'),
            HyperCategory(
                hid=5,
                uniq_name='Чашки',
                children=[HyperCategory(hid=51, uniq_name='Ручки от чашек')],
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                name="Котиковый магазин",
                domain="kotik.ru",
                shop_logo_retina_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/orig',
                shop_logo_info='14:30:PNG',
                shop_logo_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/small',
            ),
            Shop(
                fesh=4,
                name="Круглосуточная доставка наковален",
                domain="anvil-delivery.com",
            ),
            Shop(
                fesh=431782,
                name="Беру, который уже почти Покупки",
                domain="beru.pokupki.market.yandex.ru",
            ),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=1, name='Vendor'),
        ]

        cls.index.offers += [
            Offer(
                title='offer 1',
                hid=1,
                fesh=1,
                vendor_id=1,
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    key='promo1034701_01_key000',
                    title="title 1",
                    description="description 1",
                    url="url 1",
                    start_date=datetime(1984, 1, 1),
                    end_date=datetime(2084, 1, 1),
                    gift_offers=[42],
                    required_quantity=3,
                    free_quantity=4,
                ),
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
            ),
            Offer(
                title='offer 2',
                hid=2,
                fesh=1,
                price=500,
                promo=Promo(
                    promo_type=PromoType.PROMO_CODE,
                    key='promo1034701_02_key000',
                    promo_code="my promo code",
                    discount_value=300,
                    discount_currency='RUR',
                    purchases=[
                        PromoPurchase(category_id=2),
                    ],
                ),
            ),
            Offer(
                title='offer 3',
                hid=3,
                price=500,
                promo=Promo(
                    promo_type=PromoType.PROMO_CODE,
                    key='promo1034701_03_key000',
                    discount_value=300,
                    discount_currency='RUR',
                ),
            ),
            Offer(offerid='fesh4_1', hid=4, fesh=4, promo_key="fesh4_promo1"),
            Offer(offerid='fesh4_2', hid=4, fesh=4, promo_key="fesh4_promo2"),
            Offer(offerid='fesh5_1', hid=5, fesh=5, promo_key="fesh5_promo1"),
            Offer(offerid='fesh51_1', hid=51, fesh=51, promo_key="fesh51_promo1"),
            Offer(offerid='fesh431782_1', hid=431782, fesh=431782, promo_key="fesh431782_promo1"),
        ]

        cls.index.promos += [
            Promo(
                promo_type=PromoType.FLASH_DISCOUNT,
                key='fesh4_promo1',
                min_purchases_price=5000,
                max_purchases_price=10000,
            ),
            Promo(
                promo_type=PromoType.PROMO_CODE,
                key='fesh4_promo2',
                discount_value=50,
            ),
            Promo(
                promo_type=PromoType.PROMO_CODE,
                key='fesh5_promo1',
                discount_value=50,
            ),
            Promo(
                promo_type=PromoType.PROMO_CODE,
                key='fesh51_promo1',
                discount_value=50,
            ),
            Promo(
                promo_type=PromoType.PROMO_CODE,
                key='fesh431782_promo1',
                discount_value=50,
            ),
        ]

    def test_format(self):
        response = self.report.request_json('place=promo_list&hid=1')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "key": "promo1034701_01_key000",
                    "type": "n-plus-m",
                    "url": "url 1",
                    "startDate": "1984-01-01T00:00:00Z",
                    "endDate": "2084-01-01T00:00:00Z",
                    "gifts_num": 1,
                    "requiredQuantity": 3,
                    "freeQuantity": 4,
                    "pictures": NotEmpty(),
                    "shop": NotEmpty(),
                }
            ],
        )

        response = self.report.request_json('place=promo_list&hid=2')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "key": "promo1034701_02_key000",
                    "type": "promo-code",
                    "promo-code": {"text": "my promo code", "value": 300, "currency": "RUR"},
                }
            ],
        )

    def test_filters(self):
        response = self.report.request_json('place=promo_list&hid=1,2,3')
        self.assertFragmentIn(response, [{"entity": "promo"}] * 2, allow_different_len=False)  # same shop will collapse

        # test that category excluding works
        response = self.report.request_json('place=promo_list&fesh=1&hid=-1')
        self.assertEqual(response.count({"entity": "promo"}), 1)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "key": "promo1034701_02_key000",
                    "type": PromoType.PROMO_CODE,
                }
            ],
        )

        response = self.report.request_json('place=promo_list&fesh=1&hid=-2')
        self.assertEqual(response.count({"entity": "promo"}), 1)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "key": "promo1034701_01_key000",
                    "type": PromoType.N_PLUS_ONE,
                }
            ],
        )
        # check that we can subtract subcategory
        response = self.report.request_json('place=promo_list&fesh=5,51&hid=5')
        self.assertEqual(response.count({"entity": "promo"}), 2)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "key": "fesh5_promo1",
                    "type": PromoType.PROMO_CODE,
                },
                {
                    "entity": "promo",
                    "key": "fesh51_promo1",
                    "type": PromoType.PROMO_CODE,
                },
            ],
        )
        response = self.report.request_json('place=promo_list&fesh=5,51&hid=5,-51')
        self.assertEqual(response.count({"entity": "promo"}), 1)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "key": "fesh5_promo1",
                    "type": PromoType.PROMO_CODE,
                }
            ],
        )

        response = self.report.request_json('place=promo_list&hid=1')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "key": "promo1034701_01_key000",
                    "type": "n-plus-m",
                    "url": "url 1",
                }
            ],
        )

        response = self.report.request_json('place=promo_list&fesh=1')
        self.assertFragmentIn(
            response,
            [
                {"shop": {"id": 1}},
            ],
        )

        response = self.report.request_json('place=promo_list&vendor_id=1')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "key": "promo1034701_01_key000",
                    "type": "n-plus-m",
                    "url": "url 1",
                }
            ],
        )

        response = self.report.request_json('place=promo_list&hid=1&fesh=1&vendor_id=1')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "key": "promo1034701_01_key000",
                    "type": "n-plus-m",
                    "url": "url 1",
                }
            ],
        )

        response = self.report.request_json('place=promo_list&hid=1,2,3&promo-type=promo-code,n-plus-m')
        self.assertFragmentIn(response, [{"entity": "promo"}] * 2, allow_different_len=False)

        response = self.report.request_json(
            'place=promo_list&hid=1,2,3&promo-type=promo-code,gift-with-purchase,n-plus-m'
        )
        self.assertFragmentIn(response, [{"entity": "promo"}] * 2, allow_different_len=False)

        response = self.report.request_json('place=promo_list&hid=1,2,3&promo-type=n-plus-m,gift-with-purchase')
        self.assertFragmentIn(response, [{"entity": "promo"}] * 1, allow_different_len=False)

        response = self.report.request_json('place=promo_list&hid=1,2,3&promo-type=gift-with-purchase')
        self.assertFragmentNotIn(response, [{"entity": "promo"}])

        response = self.report.request_json('place=promo_list&hid=1&fesh=1&vendor_id=1&promo-type=n-plus-m')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "key": "promo1034701_01_key000",
                    "type": "n-plus-m",
                    "url": "url 1",
                }
            ],
        )

    def test_paging(self):
        response = self.report.request_json('place=promo_list&on-page=2&hid=1,2,3')
        self.assertFragmentIn(response, [{"entity": "promo"}] * 2, allow_different_len=False)

        response = self.report.request_json('place=promo_list&page=1&on-page=2&hid=1,2,3')
        self.assertFragmentIn(response, [{"entity": "promo"}] * 2, allow_different_len=False)

        response = self.report.request_json('place=promo_list&page=1&on-page=1&hid=1,2,3')
        self.assertFragmentIn(response, [{"entity": "promo"}], allow_different_len=False)

        response = self.report.request_json('place=promo_list&page=2&on-page=1&hid=1,2,3')
        self.assertFragmentIn(response, [{"entity": "promo"}], allow_different_len=False)

        response = self.report.request_json('place=promo_list&page=1&on-page=100&hid=1,2,3')
        self.assertFragmentIn(response, [{"entity": "promo"}] * 2, allow_different_len=False)

    def test_debug_available(self):
        response = self.report.request_json('place=promo_list&page=2&on-page=2&hid=1,2,3&debug=1')
        self.assertFragmentIn(
            response,
            {
                "results": [],
                "debug": {},
            },
        )

    def test_pokupki_domain(self):
        """Check that under remove_beru_from_promo_list flag we hide beru/pokupki promo's from output"""
        base_request = 'place=promo_list&hid=431782'

        response = self.report.request_json(base_request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo",
                        "key": "fesh431782_promo1",
                        "type": "promo-code",
                    }
                ],
            },
        )

        response = self.report.request_json(base_request + '&rearr-factors=remove_beru_from_promo_list=1')
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo",
                        "key": "fesh431782_promo1",
                        "type": "promo-code",
                    }
                ],
            },
        )

    @classmethod
    def prepare_shop_mix_data(cls):

        cls.index.hypertree += [
            HyperCategory(hid=2385601, uniq_name='панды'),
            HyperCategory(hid=2385602, uniq_name='мачеты'),
        ]

        cls.index.shops += [
            Shop(
                fesh=2385601,
                name="Первый магазин",
                new_shop_rating=NewShopRating(new_rating=5),
            ),
            Shop(
                fesh=2385602,
                name="Второй магазин",
                new_shop_rating=NewShopRating(new_rating=3),
            ),
            Shop(
                fesh=2385603,
                name="Третий магазин",
                new_shop_rating=NewShopRating(new_rating=1),
            ),
        ]

        cls.index.offers += [
            # first has 8 offers, 4 different promo (should collapse to 4 offer)
            Offer(title='offer001', hid=2385601, fesh=2385601, promo_key='promo2385601_01_key001'),
            Offer(title='offer001', hid=2385601, fesh=2385601, promo_key='promo2385601_01_key002'),
            Offer(title='offer001', hid=2385601, fesh=2385601, promo_key='promo2385601_01_key003'),
            Offer(title='offer001', hid=2385601, fesh=2385601, promo_key='promo2385601_01_key004'),
            Offer(title='offer001', hid=2385601, fesh=2385601, promo_key='promo2385601_01_key001'),
            Offer(title='offer001', hid=2385601, fesh=2385601, promo_key='promo2385601_01_key002'),
            Offer(title='offer001', hid=2385601, fesh=2385601, promo_key='promo2385601_01_key001'),
            Offer(title='offer001', hid=2385601, fesh=2385601, promo_key='promo2385601_01_key002'),
            Offer(title='offer001', hid=2385601, fesh=2385601, promo_key='promo2385601_01_key005'),
            # second has 3 offers, all promo are different
            Offer(title='offer001', hid=2385601, fesh=2385602, promo_key='promo2385602_01_key001'),
            Offer(title='offer001', hid=2385601, fesh=2385602, promo_key='promo2385602_01_key002'),
            Offer(title='offer001', hid=2385601, fesh=2385602, promo_key='promo2385602_01_key003'),
            # third has 3 offers, all promo are different
            Offer(title='offer001', hid=2385601, fesh=2385603, promo_key='promo2385603_01_key001'),
            Offer(title='offer001', hid=2385601, fesh=2385603, promo_key='promo2385603_01_key002'),
            Offer(title='offer001', hid=2385601, fesh=2385603, promo_key='promo2385603_01_key003'),
        ]

        cls.index.promos += [
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2385601_01_key001', discount_value=50),
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2385601_01_key002', discount_value=50),
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2385601_01_key003', discount_value=50),
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2385601_01_key004', discount_value=50),
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2385601_01_key005', discount_value=50),
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2385602_01_key001', discount_value=50),
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2385602_01_key002', discount_value=50),
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2385602_01_key003', discount_value=50),
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2385603_01_key001', discount_value=50),
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2385603_01_key002', discount_value=50),
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2385603_01_key003', discount_value=50),
        ]

    def test_shop_mix(self):
        response = self.report.request_json('place=promo_list&hid=2385601&page=1&on-page=20')
        self.assertFragmentIn(
            response,
            [
                {"shop": {"id": 2385601}},
                {"shop": {"id": 2385602}},
                {"shop": {"id": 2385603}},
            ],
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json('place=promo_list&hid=2385601&page=1&on-page=1')
        self.assertFragmentIn(
            response,
            [
                {"shop": {"id": 2385601}},
            ],
            allow_different_len=False,
        )

    @classmethod
    def prepare_promo_or_discount_search(cls):

        cls.index.hypertree += [
            HyperCategory(hid=2401301, uniq_name='24013панды'),
        ]

        cls.index.shops += [
            Shop(fesh=2401301, name="Первый магазин"),
            Shop(fesh=2401302, name="Первый магазин"),
            Shop(fesh=2401303, name="Первый магазин"),
        ]

        cls.index.offers += [
            # one discount, one promo, one flash-discount (it's like a promo, but why not)
            Offer(
                title='offer001',
                hid=2401301,
                fesh=2401301,
                price=50,
                price_old=100,
                price_history=100,
                promo=Promo(promo_type=PromoType.BUNDLE, key='promo1034701_01_key007'),
            ),
            Offer(
                title='offer002',
                hid=2401301,
                fesh=2401302,
                price=50,
                price_old=100,
                price_history=100,
                promo_key='promo2401301_01_key002',
            ),
            Offer(
                title='offer003',
                hid=2401301,
                fesh=2401303,
                price=150,
                price_old=200,
                promo_price=49,
                promo=Promo(promo_type=PromoType.FLASH_DISCOUNT, key='promo2401301_01_key003'),
            ),
        ]

        cls.index.promos += [Promo(promo_type=PromoType.PROMO_CODE, key='promo2401301_01_key002', discount_value=50)]

    def test_promo_or_discount_search_work(self):
        # если спрашиваем скидки - акции не пролезают
        response = self.report.request_json('place=promo_list&hid=2401301&page=1&on-page=20&promo-type=discount')
        self.assertFragmentIn(
            response,
            [
                {"type": "discount"},
                {"type": "discount"},
                {"type": "discount"},
            ],
            allow_different_len=False,
        )

        # пролезает только нужное
        response = self.report.request_json(
            'place=promo_list&hid=2401301&page=1&on-page=20&promo-type=discount,promo-code'
        )
        self.assertFragmentIn(
            response,
            [
                {"type": "discount"},
                {"type": "promo-code"},
                {"type": "discount"},
            ],
            allow_different_len=False,
        )

        # если спрашиваем акции - скидки не пролезают
        response = self.report.request_json('place=promo_list&hid=2401301&page=1&on-page=20&promo-type=promo-code')
        self.assertFragmentIn(
            response,
            [
                {
                    "type": "promo-code",
                },
            ],
            allow_different_len=False,
        )

        response = self.report.request_json('place=promo_list&hid=2401301&page=1&on-page=20')
        self.assertFragmentIn(
            response,
            [
                {
                    "type": "flash-discount",
                    "discount_prc": 67,
                },
                {"type": "promo-code"},
                {
                    "type": "discount",
                    "discount_prc": 50,
                },
            ],
            allow_different_len=False,
        )

        response = self.report.request_json('place=promo_list&hid=2401301&page=1&on-page=20&promo-type=market')
        self.assertFragmentIn(
            response,
            [
                {
                    "type": "flash-discount",
                    "discount_prc": 67,
                },
                {"type": "promo-code"},
                {
                    "type": "discount",
                    "discount_prc": 50,
                },
            ],
            allow_different_len=False,
        )

    @classmethod
    def prepare_grades_count_rearrange(cls):

        cls.index.hypertree += [
            HyperCategory(hid=2433300, uniq_name='24013панды'),
        ]

        cls.index.shops += [
            Shop(
                fesh=2433301,
                name="Первый магазин",
                new_shop_rating=NewShopRating(new_rating=5, new_grades_count_3m=15),
            ),
            Shop(
                fesh=2433302,
                name="Второй магазин",
                new_shop_rating=NewShopRating(new_rating=3, new_grades_count_3m=22),
            ),
        ]

        cls.index.offers += [
            Offer(title='offer001', hid=2433300, fesh=2433301, promo_key='promo2433301_01_key001'),
            Offer(title='offer001', hid=2433300, fesh=2433302, promo_key='promo2433302_01_key001'),
        ]

        cls.index.promos += [
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2433301_01_key001', discount_value=50),
            Promo(promo_type=PromoType.PROMO_CODE, key='promo2433302_01_key001', discount_value=50),
        ]

    def test_grades_count_rearrange(self):
        response = self.report.request_json('place=promo_list&hid=2433300&page=1&on-page=20')
        self.assertFragmentIn(
            response,
            [
                {"shop": {"id": 2433302}},
                {"shop": {"id": 2433301}},
            ],
            preserve_order=True,
        )

    def test_min_max_purchase_prices(self):
        response = self.report.request_json(
            'place=promo_list&hid=4&page=1&on-page=20&promo-type=%s' % PromoType.FLASH_DISCOUNT
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "key": "fesh4_promo1",
                    "min_purchases_price": "5000",
                    "max_purchases_price": "10000",
                },
            ],
        )

        response = self.report.request_json(
            'place=promo_list&hid=4&page=1&on-page=20&promo-type=%s' % PromoType.PROMO_CODE
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "key": "fesh4_promo2",
                    "min_purchases_price": "0",
                    "max_purchases_price": "0",
                },
            ],
        )

    def test_only_promo_category(self):
        response = self.report.request_json('place=promo_list&hid=1')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "onlyPromoCategory": Absent(),
                }
            ],
        )

        response = self.report.request_json('place=promo_list&hid=2')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "promo",
                    "onlyPromoCategory": {"hid": 2, "name": "Котики"},
                }
            ],
        )


if __name__ == '__main__':
    main()
