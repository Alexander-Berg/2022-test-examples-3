#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import DynamicQPromos, Model, Outlet, Promo, PromoMSKU, PromoType, Region, Shop, Vat
from core.types.sku import MarketSku, BlueOffer
from core.types.payment_methods import Payment
from core.matcher import Absent, Contains
from core.report import REQUEST_TIMESTAMP

from datetime import datetime, timedelta
from unittest import skip


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.creation_time = REQUEST_TIMESTAMP
        cls.index.shops += [
            Shop(fesh=13, priority_region=213, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
        ]
        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
            Region(rid=214, name='Республика Пепястан', region_type=Region.FEDERATIVE_SUBJECT),
        ]
        cls.index.outlets += [
            Outlet(fesh=777, region=213),
        ]

        # оффер удовлетворяет условиям акции:
        blue_offers_1 = BlueOffer(
            waremd5='BlueOffer-1-IN-Promo-w',
            price=10,
            price_old=12,
            feedid=777,
            offerid='shop_sku_1',
            blue_promo_key='JVvklxUgdnawSJPG4UhZGA',
        )
        # оффер вне акции:
        blue_offers_2 = BlueOffer(
            waremd5='BlueOffer-2-NO-Promo-w',
            price=10,
            feedid=777,
            offerid='shop_sku_2',
        )
        # оффер не удовлетворяет условиям акции:
        blue_offers_3 = BlueOffer(
            waremd5='BlueOffer-3-NO-Promo-w',
            price=14,
            feedid=777,
            offerid='shop_sku_3',
        )
        # оффер удовлетворяет условиям акции, но его скидка > 95% : не валидная скидка
        blue_offers_4 = BlueOffer(
            waremd5='BlueOffer-4-NO-Promo-w',
            price=8,
            feedid=777,
            offerid='shop_sku_4',
        )
        # оффер удовлетворяет условиям акции, но его скидка < 5% : не валидная скидка
        blue_offers_5 = BlueOffer(
            waremd5='BlueOffer-5-NO-Promo-w',
            price=192,
            feedid=777,
            offerid='shop_sku_5',
        )
        # оффер удовлетворяет условиям акции, его скидка < 5%, но сумма скидки > 500р : валидная скидка
        blue_offers_6 = BlueOffer(
            waremd5='BlueOffer-6-IN-Promo-w',
            price=14400,
            feedid=777,
            offerid='shop_sku_6',
            blue_promo_key='ForTestingPromosBeingValid02',
        )
        # у оффера есть OldPrice от магазина, но для него есть активная акция, проходит по ее условиям
        blue_offers_7 = BlueOffer(
            waremd5='BlueOffer-7-IN-Promo-w',
            price=9,
            price_old=50,
            price_history=200,
            feedid=777,
            offerid='shop_sku_7',
            blue_promo_key='JVvklxUgdnawSJPG4UhZGA',
        )
        # у оффера есть OldPrice от магазина, но для него есть активная акция, не проходит по ее условиям
        blue_offers_8 = BlueOffer(
            waremd5='BlueOffer-8-NO-Promo-w',
            price=20,
            price_old=50,
            price_history=200,
            feedid=777,
            offerid='shop_sku_8',
        )
        # у оффера есть OldPrice от магазина, нету активной акции
        blue_offers_9 = BlueOffer(
            waremd5='BlueOffer-9-IN-Promo-w',
            price=1425,
            price_old=1500,
            price_history=2000,
            feedid=777,
            offerid='shop_sku_9',
        )
        # оффер удовлетворяет условиям акции, нет oldPrice:
        blue_offers_10 = BlueOffer(
            waremd5='BlueOffer10-IN-Promo-w',
            price=14300,
            price_old=-1,
            feedid=777,
            offerid='shop_sku_10',
            blue_promo_key='ForTestingPromosBeingValid02',
        )
        # оффер для которого проставлен литерал match_blue_promo, но такой акции не существует (например, она истекла и не попала в новый mmap)
        blue_offers_11 = BlueOffer(
            waremd5='BlueOffer11-IN-Promo-w',
            price=14400,
            price_old=-1,
            feedid=777,
            offerid='shop_sku_11',
            blue_promo_key='NonExistingMarketPromo',
        )
        # оффер для которого проставлен литерал match_blue_promo, но его акция уже завершилась
        blue_offers_12 = BlueOffer(
            waremd5='BlueOffer12-IN-Promo-w',
            price=14400,
            price_old=-1,
            feedid=777,
            offerid='shop_sku_12',
            blue_promo_key='ExpiredMarketPromo',
        )
        # оффер для которого проставлен литерал match_blue_promo, но его акция еще не началась
        blue_offers_13 = BlueOffer(
            waremd5='BlueOffer13-IN-Promo-w',
            price=14400,
            price_old=-1,
            feedid=777,
            offerid='shop_sku_13',
            blue_promo_key='FutureMarketPromo',
        )
        cls.index.mskus += [
            MarketSku(
                title='blue market sku1',
                hyperid=1,
                sku=110011,
                waremd5='MarketSku1-IiLVm1goleg',
                blue_offers=[blue_offers_1, blue_offers_3, blue_offers_7, blue_offers_8],
            ),
            MarketSku(
                title='blue market sku2',
                hyperid=1,
                sku=220022,
                waremd5='MarketSku2-IiLVm1goleg',
                blue_offers=[blue_offers_2, blue_offers_9],
            ),
            MarketSku(
                title='blue market sku3',
                hyperid=3,
                sku=330033,
                waremd5='MarketSku3-IiLVm1goleg',
                blue_offers=[blue_offers_4, blue_offers_5],
            ),
            MarketSku(
                title='blue market sku4',
                hyperid=4,
                sku=440044,
                waremd5='MarketSku4-IiLVm1goleg',
                blue_offers=[blue_offers_6, blue_offers_10],
            ),
            MarketSku(
                title='blue market sku5',
                hyperid=5,
                sku=550055,
                waremd5='MarketSku5-IiLVm1goleg',
                blue_offers=[blue_offers_11],
            ),
            MarketSku(
                title='blue market sku6',
                hyperid=6,
                sku=660066,
                waremd5='MarketSku6-IiLVm1goleg',
                blue_offers=[blue_offers_12],
            ),
            MarketSku(
                title='blue market sku7',
                hyperid=7,
                sku=770077,
                waremd5='MarketSku7-IiLVm1goleg',
                blue_offers=[blue_offers_13],
            ),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=1, title='blue and green model'),
        ]

        now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
        cls.index.promos += [
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='JVvklxUgdnawSJPG4UhZGA',
                shop_promo_id='default_3p_flash',
                start_date=now - timedelta(days=100),
                mskus=[
                    PromoMSKU(msku='110011', market_promo_price=10, market_old_price=20),
                    PromoMSKU(msku='999999', market_promo_price=100, market_old_price=400),
                ],
            ),
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='Prehistoric-Promo',
                start_date=now - timedelta(days=300),
                end_date=now - timedelta(days=200),
                allowed_payment_methods=Payment.PT_YANDEX,
                mskus=[
                    PromoMSKU(msku='110011', market_promo_price=10, market_old_price=20),
                ],
            ),
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='ForTestingPromosBeingValid01',
                start_date=now - timedelta(days=100),
                mskus=[
                    PromoMSKU(msku='330033', market_promo_price=100, market_old_price=200),
                ],
            ),
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='ForTestingPromosBeingValid02',
                start_date=now - timedelta(days=100),
                mskus=[
                    PromoMSKU(msku='440044', market_promo_price=14500, market_old_price=15000),
                ],
            ),
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='ExpiredMarketPromo',
                start_date=now - timedelta(days=100),
                end_date=now - timedelta(days=50),
                mskus=[
                    PromoMSKU(msku='660066', market_promo_price=14500, market_old_price=15000),
                ],
            ),
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='FutureMarketPromo',
                start_date=now + timedelta(days=100),
                mskus=[
                    PromoMSKU(msku='770077', market_promo_price=14500, market_old_price=15000),
                ],
            ),
        ]

    def _test_offerinfo_with_flash(self, color):
        wareid = 'BlueOffer-1-IN-Promo-w'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}'
        response = self.report.request_json(params.format(wareid, color))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': wareid,
                    'promos': [
                        {
                            'type': PromoType.BLUE_3P_FLASH_DISCOUNT,
                            'key': 'JVvklxUgdnawSJPG4UhZGA',
                            'shopPromoId': 'default_3p_flash',
                            'extra': Absent(),
                            'parameters': Absent(),
                        }
                    ],
                    "prices": {
                        "discount": {
                            "oldMin": "20",
                            "percent": 50,
                        }
                    },
                }
            ],
        )

    def test_blue_offerinfo_with_flash(self):
        """Проверяем, что акционный оффер в синем поиске содержит информацию об акции и скидку."""
        color = 'BLUE'
        self._test_offerinfo_with_flash(color)

    def test_green_offerinfo_with_flash(self):
        """Проверяем, что акционный оффер в поиске белого содержит информацию об акции и скидку."""
        color = 'GREEN_WITH_BLUE'
        self._test_offerinfo_with_flash(color)

    def test_offerinfo_without_flash(self):
        """Проверяем, что оффер вне акции не содержит информацию об акции."""
        wareid = 'BlueOffer-2-NO-Promo-w'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb=BLUE'
        response = self.report.request_json(params.format(wareid))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-2-NO-Promo-w',
                    'promos': Absent(),
                    'prices': {
                        'discount': Absent(),
                    },
                }
            ],
        )

    def test_offerinfo_unsatisfactory(self):
        """Проверяем, что оффер с ценой больше акционной не содержит информацию об акции."""
        wareid = 'BlueOffer-3-NO-Promo-w'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb=BLUE'
        response = self.report.request_json(params.format(wareid))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-3-NO-Promo-w',
                    'promos': Absent(),
                    'prices': {
                        'discount': Absent(),
                    },
                }
            ],
        )

    def _test_filter_discount(self, color, filter):
        params = (
            'place=prime&text=blue&rids=213&rgb={}&{}=1&rearr-factors=market_do_not_split_promo_filter=1'
            '&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0'
        )
        response = self.report.request_json(params.format(color, filter))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-7-IN-Promo-w',
                    'promos': [
                        {
                            'type': PromoType.BLUE_3P_FLASH_DISCOUNT,
                            'key': 'JVvklxUgdnawSJPG4UhZGA',
                        }
                    ],
                    "prices": {
                        "discount": {
                            "oldMin": "20",
                            "percent": 55,
                        }
                    },
                }
            ],
        )

        self.assertFragmentIn(response, {"filters": [{"id": "filter-promo-or-discount"}]})

    def test_blue_search_discount_filter(self):
        """Проверяем, что оферы в акции на place=prime попадают в скидочный фильтр"""
        color = 'BLUE'
        filter = 'filter-promo-or-discount'
        self._test_filter_discount(color, filter)

    def test_green_search_discount_filter(self):
        """Проверяем, что оферы в акции на place=prime попадают в скидочный фильтр"""
        color = 'GREEN_WITH_BLUE'
        filters = ('filter-discount-only', 'filter-promo-or-discount')
        for filter in filters:
            self._test_filter_discount(color, filter)

    def test_actual_promo_place(self):
        """Проверяем актуальную акцию на place=promo"""
        response = self.report.request_json('place=promo&promoid=JVvklxUgdnawSJPG4UhZGA')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "promos": [
                        {
                            "type": PromoType.BLUE_3P_FLASH_DISCOUNT,
                            "key": "JVvklxUgdnawSJPG4UhZGA",
                        }
                    ],
                }
            },
        )

    def test_old_promo_place(self):
        """Проверяем неактуальную акцию на place=promo"""
        response = self.report.request_json('place=promo&promoid=Prehistoric-Promo')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                    "promos": [],
                }
            },
        )

    def _generate_quick_qpromos(self):
        now = datetime.fromtimestamp(REQUEST_TIMESTAMP)

        self.dynamic.qpromos += [
            DynamicQPromos(
                [
                    Promo(
                        promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                        key='JVvklxUgdnawSJPG4UhZGA',
                        start_date=now - timedelta(days=100),
                        shop_promo_id=777,
                        feed_id=7771,
                        mskus=[
                            PromoMSKU(msku='110011', market_promo_price=10, market_old_price=50),
                        ],
                    ),
                ]
            )
        ]

    def _test_quick_promo(self):
        self._generate_quick_qpromos()
        response = self.report.request_json('place=promo&promoid=JVvklxUgdnawSJPG4UhZGA')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'promos': [
                        {
                            'type': PromoType.BLUE_3P_FLASH_DISCOUNT,
                            'key': 'JVvklxUgdnawSJPG4UhZGA',
                        }
                    ],
                }
            },
        )

        wareid = 'BlueOffer-1-IN-Promo-w'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}'
        response = self.report.request_json(params.format(wareid, 'BLUE'))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-1-IN-Promo-w',
                    'promos': [
                        {
                            'type': PromoType.BLUE_3P_FLASH_DISCOUNT,
                            'key': 'JVvklxUgdnawSJPG4UhZGA',
                        }
                    ],
                    'prices': {
                        'discount': {
                            'oldMin': '50',
                            'percent': 80,
                        }
                    },
                }
            ],
        )

    @skip('MARKETOUT-43145, MMAP с индексными промо не перезагружаются')
    def test_quick_pipeline_old_price_changed(self):
        """Проверяем, что происходит подмена old price на доставленную быстрым pipeline."""
        self._test_quick_promo()

    def test_bad_quick_promos_format(self):
        """Проверяем, что если в быстром пайплайне приходит плохой mmap-ник, то репорт не падает,
        продолжаем отдавать старые акции"""
        self.dynamic.broken_qpromos = True

        err1 = 'Failed to update filter from'
        err2 = 'Failed to load qpromos dynamic file'
        self.error_log.expect(err1)
        self.error_log.expect(err2)
        self.base_logs_storage.error_log.expect(err1)
        self.base_logs_storage.error_log.expect(err2)

        self._test_offerinfo_with_flash('BLUE')

    @skip('MARKETOUT-43145, MMAP с индексными промо не перезагружаются')
    def test_rollback_qpromos(self):
        """Проверяем, что после выполнения команды rollback у нас откатываются быстрые скидки,
        до скидок, которые приехали с большим поколением"""
        self._test_quick_promo()

        response = self.report.request_xml('admin_action=rollbackdata&which=qpromos')
        self.assertFragmentIn(response, "<admin-action>ok</admin-action>")

        response = self.base_search_client.request_xml('admin_action=rollbackdata&which=qpromos')
        self.assertFragmentIn(response, "<admin-action>ok</admin-action>")

        self._test_offerinfo_with_flash('BLUE')

    def test_qpromos_full_generation_newer(self):
        """Проверяем, что во время загрузки будет взято самое последнее поколение скидок.
        Есть mmap-ник от быстрого ПП (old_time), приезжают скидки с большим поколением (new_time).
        В фильтр должны попасть скидки большого поколения, так как они 'свежее'."""
        old_qpromos_time = datetime.fromtimestamp(REQUEST_TIMESTAMP) - timedelta(days=1)
        self.dynamic.qpromos_generation = old_qpromos_time
        self._generate_quick_qpromos()
        self._test_offerinfo_with_flash('BLUE')

    def test_blue_offer_with_huge_invalid_discount(self):
        """Проверяем, что товар попадающий в условие Blue3PFlashDiscount акции, но с очень
        большой скидкой (> 95%) не будет считаться акционным"""
        wareid = 'BlueOffer-4-NO-Promo-w'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}'
        response = self.report.request_json(params.format(wareid, 'BLUE'))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-4-NO-Promo-w',
                    'promos': Absent(),
                    'prices': {
                        "discount": Absent(),
                    },
                }
            ],
        )

    def test_blue_offer_with_small_invalid_discount(self):
        """Проверяем, что товар попадающий в условие Blue3PFlashDiscount акции, но с очень
        маленькиой скидкой (< 5%) и с суммой скидки < 500р  не будет считаться акционным"""
        wareid = 'BlueOffer-5-NO-Promo-w'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}'
        response = self.report.request_json(params.format(wareid, 'BLUE'))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-5-NO-Promo-w',
                    'promos': Absent(),
                    'prices': {
                        "discount": Absent(),
                    },
                }
            ],
        )

    def test_blue_offer_with_small_valid_discount(self):
        """Проверяем, что товар попадающий в условие Blue3PFlashDiscount акции, с очень
        маленькиой скидкой (< 5%), но с суммой скидки >= 500р будет считаться акционным"""
        wareid = 'BlueOffer-6-IN-Promo-w'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}'
        response = self.report.request_json(params.format(wareid, 'BLUE'))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-6-IN-Promo-w',
                    'promos': [
                        {
                            'type': PromoType.BLUE_3P_FLASH_DISCOUNT,
                            'key': 'ForTestingPromosBeingValid02',
                        }
                    ],
                    'prices': {
                        'discount': {
                            'oldMin': '15000',
                            'percent': 4,
                        }
                    },
                }
            ],
        )

    def test_blue_offer_with_discount_from_shop_and_in_market_promo(self):
        """Проверяем что OldPrice от магазина отбрасывается, если для MSKU есть актуальная Маркетная акция.
        Маркетные акции - приоритетнее. Если оффер попадает в маркетную акцию, то oldPrice берется из акции."""
        wareid = 'BlueOffer-7-IN-Promo-w'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}'
        response = self.report.request_json(params.format(wareid, 'BLUE'))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-7-IN-Promo-w',
                    'promos': [
                        {
                            'type': PromoType.BLUE_3P_FLASH_DISCOUNT,
                            'key': 'JVvklxUgdnawSJPG4UhZGA',
                        }
                    ],
                    'prices': {
                        'value': '9',
                        'discount': {
                            'oldMin': '20',
                            'percent': 55,
                        },
                    },
                }
            ],
        )

    def test_blue_offer_with_discount_from_shop_and_in_market_promo_trace(self):
        """Проверяем, что при наличии debug=1 можно будет увидеть трейс причины отсутствия скидки."""
        wareid = 'BlueOffer-7-IN-Promo-w'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}&debug=1'
        response = self.report.request_json(params.format(wareid, 'BLUE'))
        self.assertFragmentIn(response, {"logicTrace": [Contains('old price is disabled by active flash promo')]})

    def test_blue_offer_with_discount_from_shop_and_out_market_promo(self):
        """Проверяем что OldPrice от магазина отбрасывается, если для MSKU есть актуальная Маркетная акция.
        Маркетные акции - приоритетнее. Если оффер не попадает в маркетную акцию, то oldPrice остается пустым."""
        wareid = 'BlueOffer-8-NO-Promo-w'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}'
        response = self.report.request_json(params.format(wareid, 'BLUE'))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-8-NO-Promo-w',
                    'promos': Absent(),
                    'prices': {
                        'value': '20',
                        'discount': Absent(),
                    },
                }
            ],
        )

    def test_blue_offer_with_discount_from_shop_and_no_market_promo(self):
        """Проверяем что OldPrice от магазина не отбрасывается, если для MSKU нет актуальной Маркетной акции."""
        wareid = 'BlueOffer-9-IN-Promo-w'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}'
        response = self.report.request_json(params.format(wareid, 'BLUE'))

        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-9-IN-Promo-w',
                    'promos': Absent(),
                    'prices': {
                        'value': '1425',
                        'discount': {
                            'oldMin': '1500',
                            'percent': 5,
                        },
                    },
                }
            ],
        )

    def test_offer_with_promo_discount_only_no_trace(self):
        """Проверяем, что при наличии debug=1 для офферов только со скидой от маркета
        не появится трейс о причины отсутствия скидки."""
        wareid = 'BlueOffer10-IN-Promo-w'
        params = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb={}&debug=1'
        response = self.report.request_json(params.format(wareid, 'BLUE'))
        self.assertFragmentNotIn(
            response, {"logicTrace": [Contains('old price will be disabled by active flash promo')]}
        )

    def test_filter_blue_promos_by_search_literal_match_blue_promo(self):
        """Проверяем, что синие оффера фильтуются по литералу match_blue_promo. Id акции передается через cgi-параметр
        &promoid на плейс prime. На выдаче должны появиться только те акции, которые попадают в заданные.
        BlueOffer-1-IN-Promo-w, BlueOffer-7-IN-Promo-w и BlueOffer-6-IN-Promo-w попадают в акции,
        но только BlueOffer-7-IN-Promo-w и BlueOffer-1-IN-Promo-w в нужную. BlueOffer-1-IN-Promo-w и BlueOffer-7-IN-Promo-w
        принадлежат одному msku, но именно BlueOffer-7-IN-Promo-w выигрывает buybox, поэтому на выдаче будет только он."""
        params = 'place=prime&rids=213&pp=42&rgb=BLUE&promoid=JVvklxUgdnawSJPG4UhZGA'
        response = self.report.request_json(params)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-7-IN-Promo-w',
                    'promos': [
                        {
                            'type': PromoType.BLUE_3P_FLASH_DISCOUNT,
                            'key': 'JVvklxUgdnawSJPG4UhZGA',
                        }
                    ],
                    "prices": {
                        "discount": {
                            "oldMin": "20",
                            "percent": 55,
                        }
                    },
                }
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-1-IN-Promo-w',
                }
            ],
        )

        self.assertFragmentNotIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-6-IN-Promo-w',
                }
            ],
        )

    def test_filter_blue_promos_by_sl_match_blue_promo_multiple(self):
        """Проверяем, что если в cgi-параметре &promoid передали несколько id акций то на выдаче будут оффер
        для всех из них."""
        params = 'place=prime&rids=213&pp=42&rgb=BLUE&promoid=JVvklxUgdnawSJPG4UhZGA,ForTestingPromosBeingValid02'
        response = self.report.request_json(params)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer-7-IN-Promo-w',
                    'promos': [
                        {
                            'type': PromoType.BLUE_3P_FLASH_DISCOUNT,
                            'key': 'JVvklxUgdnawSJPG4UhZGA',
                        }
                    ],
                    "prices": {
                        "discount": {
                            "oldMin": "20",
                            "percent": 55,
                        }
                    },
                }
            ],
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': 'BlueOffer10-IN-Promo-w',
                    'promos': [
                        {
                            'type': PromoType.BLUE_3P_FLASH_DISCOUNT,
                            'key': 'ForTestingPromosBeingValid02',
                        }
                    ],
                    'prices': {
                        'discount': {
                            'oldMin': '15000',
                            'percent': 5,
                        }
                    },
                }
            ],
        )

    def test_filter_blue_promos_by_sl_match_blue_promo_not_exist(self):
        """Проверяем, что на выдачу не попадают оффера, у которых проставлен литерал match_blue_promo,
        но его акции не существует."""
        params = 'place=prime&rids=213&pp=42&rgb=BLUE&promoid=NonExistingMarketPromo'
        response = self.report.request_json(params)
        self.assertFragmentIn(response, {'search': {'total': 0}})

    def test_filter_blue_promos_by_sl_match_blue_promo_expired(self):
        """Проверяем, что на выдачу не попадают оффера, у которых проставлен литерал match_blue_promo,
        но акция уже не активна."""
        params = 'place=prime&rids=213&pp=42&rgb=BLUE&promoid=ExpiredMarketPromo'
        response = self.report.request_json(params)
        self.assertFragmentIn(response, {'search': {'total': 0}})

    def test_filter_blue_promos_by_sl_match_blue_promo_future(self):
        """Проверяем, что на выдачу не попадают оффера, у которых проставлен литерал match_blue_promo,
        но акция еще не активна."""
        params = 'place=prime&rids=213&pp=42&rgb=BLUE&promoid=FutureMarketPromo'
        response = self.report.request_json(params)
        self.assertFragmentIn(response, {'search': {'total': 0}})

    ALICE_PAYMENT_WO_PROMO = 12300
    ALICE_PAYMENT_ALL = 12301
    ALICE_PAYMENT_YANDEX_CASH = 12302
    ALICE_PAYMENT_YANDEX = 12303
    ALICE_PAYMENT_CASH = 12304

    @classmethod
    def prepare_alice_payment_only_filter(cls):
        cls.index.mskus += [
            MarketSku(
                sku=T.ALICE_PAYMENT_WO_PROMO,
                title='payment test without promo',
                hyperid=T.ALICE_PAYMENT_WO_PROMO,
                blue_offers=[
                    BlueOffer(
                        price=10,
                        vat=Vat.VAT_10,
                        feedid=777,
                        offerid='blue.offer.12300',
                    )
                ],
            ),
            MarketSku(
                sku=T.ALICE_PAYMENT_ALL,
                title='payment test all methods',
                hyperid=T.ALICE_PAYMENT_ALL,
                blue_offers=[
                    BlueOffer(
                        price=10,
                        vat=Vat.VAT_10,
                        feedid=777,
                        offerid='blue.offer.12301',
                        # blue_promo_key='Payment-Promo-all',
                    )
                ],
            ),
            MarketSku(
                sku=T.ALICE_PAYMENT_YANDEX_CASH,
                title='payment test yandex + cash',
                hyperid=T.ALICE_PAYMENT_YANDEX_CASH,
                blue_offers=[
                    BlueOffer(
                        price=10,
                        vat=Vat.VAT_10,
                        feedid=777,
                        offerid='blue.offer.12302',
                        # blue_promo_key='Payment-Promo-yandex-cash',
                    )
                ],
            ),
            MarketSku(
                sku=T.ALICE_PAYMENT_YANDEX,
                title='payment test yandex only',
                hyperid=T.ALICE_PAYMENT_YANDEX,
                blue_offers=[
                    BlueOffer(
                        price=10,
                        vat=Vat.VAT_10,
                        feedid=777,
                        offerid='blue.offer.12303',
                        # blue_promo_key='Payment-Promo-yandex',
                    )
                ],
            ),
            MarketSku(
                sku=T.ALICE_PAYMENT_CASH,
                title='payment test cash',
                hyperid=T.ALICE_PAYMENT_CASH,
                blue_offers=[
                    BlueOffer(
                        price=10,
                        vat=Vat.VAT_10,
                        feedid=777,
                        offerid='blue.offer.12304',
                        # blue_promo_key='Payment-Promo-cash',
                    )
                ],
            ),
        ]

        now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
        cls.index.promos += [
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='Payment-Promo-all',
                start_date=now - timedelta(days=100),
                mskus=[PromoMSKU(msku=T.ALICE_PAYMENT_ALL, market_promo_price=10, market_old_price=20)],
                allowed_payment_methods=Payment.PT_ALL,
            ),
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='Payment-Promo-yandex-cash',
                start_date=now - timedelta(days=99),
                mskus=[PromoMSKU(msku=T.ALICE_PAYMENT_YANDEX_CASH, market_promo_price=11, market_old_price=21)],
                allowed_payment_methods=Payment.PT_YANDEX + Payment.PT_CASH_ON_DELIVERY,
            ),
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='Payment-Promo-yandex',
                start_date=now - timedelta(days=98),
                mskus=[PromoMSKU(msku=T.ALICE_PAYMENT_YANDEX, market_promo_price=12, market_old_price=22)],
                allowed_payment_methods=Payment.PT_YANDEX,
            ),
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='Payment-Promo-cash',
                start_date=now - timedelta(days=97),
                mskus=[PromoMSKU(msku=T.ALICE_PAYMENT_CASH, market_promo_price=13, market_old_price=23)],
                allowed_payment_methods=Payment.PT_CASH_ON_DELIVERY,
            ),
        ]

    def test_alice_payment_only_filter(self):
        '''
        Проверяем фильтрацию оферов, участвующих в акциях, имеющих ограничение по оплате Только Предоплата для Алисы.
        Алиса не умеет обрабатывать предоплату https://st.yandex-team.ru/MARKETOUT-22004
        '''
        request = 'place=prime&rgb=blue&text=payment+test&allow-collapsing=0'

        # При обычном запросе показываются все оферы
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'marketSku': str(sku)}
                    for sku in [
                        T.ALICE_PAYMENT_WO_PROMO,
                        T.ALICE_PAYMENT_ALL,
                        T.ALICE_PAYMENT_YANDEX_CASH,
                        T.ALICE_PAYMENT_YANDEX,
                        T.ALICE_PAYMENT_CASH,
                    ]
                ]
            },
            allow_different_len=False,
        )

        # Для запроса от алисы пропускается офер с акцией только предоплаты
        payment_rearr = '&rearr-factors=enable_payment_methods_restriction=1'
        response = self.report.request_json(request + payment_rearr + '&alice=1')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'marketSku': str(sku)}
                    for sku in [
                        T.ALICE_PAYMENT_WO_PROMO,
                        T.ALICE_PAYMENT_ALL,
                        T.ALICE_PAYMENT_YANDEX_CASH,
                        T.ALICE_PAYMENT_YANDEX,
                        T.ALICE_PAYMENT_CASH,
                    ]
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_future_promos(cls):
        cls.index.shops += [
            Shop(fesh=888, datafeed_id=888, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=9258,
                title="futurebluemsku1",
                hyperid=9258,
                waremd5='FutureOffer-1-SkuPro-w',
                blue_offers=[
                    BlueOffer(
                        waremd5='FutureOffer-1-IN-Pro-w',
                        price=20,
                        vat=Vat.VAT_10,
                        price_old=22,
                        feedid=888,
                        offerid='blue.offer.92581',
                        blue_promo_key='Payment-Promo-Future-yt',
                    )
                ],
            ),
        ]

        now = datetime.fromtimestamp(REQUEST_TIMESTAMP)

        future = now + timedelta(days=1)
        more_distant_future = now + timedelta(days=15)
        cls.index.promos += [
            Promo(
                promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
                key='Payment-Promo-Future-yt',
                start_date=future,
                end_date=more_distant_future,
                mskus=[PromoMSKU(msku='9258', market_promo_price=20, market_old_price=50)],
                allowed_payment_methods=Payment.PT_ALL,
            ),
        ]

    def _check_timestamp_promo_enabled(self, avability, timeShift):
        fragment = [
            {
                'entity': 'offer',
                'promos': [
                    {
                        'type': PromoType.BLUE_3P_FLASH_DISCOUNT,
                        'key': 'Payment-Promo-Future-yt',
                    }
                ],
                "prices": {
                    "discount": {
                        "oldMin": "50",
                    }
                },
            }
        ]

        if timeShift is not None:
            fmtstr = "%Y%m%dT%H%M%S"
            now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
            timeWithValidPromo = now + timeShift
            fakeNowStr = timeWithValidPromo.strftime(fmtstr)
            rearr_factor_fmt = "&rearr-factors=market_promo_datetime={}"
            rearr_suffix = rearr_factor_fmt.format(fakeNowStr)
        else:
            rearr_suffix = ""

        params = (
            'place=offerinfo&rids=213&regset=1&show-urls=&pp=42rgb=BLUE&offerid=FutureOffer-1-IN-Pro-w' + rearr_suffix
        )
        response = self.report.request_json(params)

        if avability:
            self.assertFragmentIn(response, fragment)
        else:
            self.assertFragmentNotIn(response, fragment)

    def test_promo_drop_by_time(self):
        '''
        Проверяем что тестируемая акция не доступна без перегрузки времени по rearr-factors
        (акция не показывается)
        '''
        self._check_timestamp_promo_enabled(False, None)

    def test_promo_overwrite_datetime_enable_promo(self):
        '''
        Проверяем перегрузку времени проверки промо через rearr-factors для включения promo
        '''
        self._check_timestamp_promo_enabled(True, timedelta(days=10))

    def test_promo_overwrite_datetime_after_promo_end(self):
        '''
        Проверяем перегрузку времени проверки промо через rearr-factors
        с временем позже конца акции (акция не показывается)
        '''
        self._check_timestamp_promo_enabled(False, timedelta(days=25))

    def test_promo_overwrite_datetime_before_promo_starts(self):
        '''
        Проверяем перегрузку времени проверки промо через rearr-factors
        с временем до начала акции (акция не показывается)
        '''
        self._check_timestamp_promo_enabled(False, timedelta(hours=1))

    def test_promo_overwrite_datetime_before_promo_starts_boarder(self):
        '''
        Проверяем перегрузку времени проверки промо через rearr-factors
        за одну секунду до начала акции (акция не показывается)
        '''
        self._check_timestamp_promo_enabled(False, timedelta(hours=23, minutes=59, seconds=59))
        # минимальный момент времени когда акция доступна
        self._check_timestamp_promo_enabled(True, timedelta(days=1))

    def test_promo_overwrite_datetime_before_promo_ends_boarder(self):
        '''
        Проверяем перегрузку времени проверки промо через rearr-factors
        за одну секунду до конца акции (акция показывается)
        '''
        self._check_timestamp_promo_enabled(True, timedelta(days=14, hours=23, minutes=59, seconds=59))
        # минимальный момент времени, когда акция перестаёт быть доступной
        self._check_timestamp_promo_enabled(False, timedelta(days=15))


if __name__ == '__main__':
    main()
