#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Round, Absent, NotEmpty
from core.types import BlueOffer, Currency, MarketSku, Model, Offer, Region, Shop, Stream, StreamName, Tax
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True

    @classmethod
    def prepare_factor_streams(cls):
        # Факторы по стримам пока под флагом в конфиге
        cls.settings.use_factorann = True

        # Заводим регион 213 -- подрегион 225
        # Заводим регион другой страны
        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
            Region(rid=149, name='Беларусь', region_type=Region.COUNTRY),
        ]

        cls.index.shops += [Shop(fesh=1, priority_region=213)]

        # Заводим ряд офферов и стримов
        # * Оффер со стримом, который будет наматчен по полному тексту запроса (чтобы проверить full-match)
        # * Оффер с несколькими разными стримами, частично совпадающими с запросом (чтобы зафиксировать значения)
        # * Оффер без стримов (чтобы удостовериться, что не ломается на индексе с недостающими документами)
        # * Оффер со стримом с другим регионом (факторы не должны считаться)
        # * Отдельно NHopIsFinal (отличается размером в памяти)
        cls.index.offers += [
            Offer(
                title='Настенный пылесос',
                waremd5='ZRK9Q9nKpuAsmQsKgmUtyg',
                fesh=1,
                factor_streams=[Stream(name=StreamName.BQPR, region=149, annotation='настенный пылесос', weight=0.64)],
            ),
            Offer(title='Двухсторонняя швабра', waremd5='EUhIXt-nprRmCEEWR-cysw', fesh=1),
            Offer(
                title='Затычка для унитаза',
                waremd5='wgrU12_pd1mqJ6DJm_9nEA',
                fesh=1,
                factor_streams=[
                    Stream(
                        name=StreamName.FIRST_CLICK_DT_XF, region=225, annotation='затычка для унитаза', weight=0.55
                    ),
                    Stream(
                        name=StreamName.AVG_DT_WEIGHTED_BY_RANK_MOBILE,
                        region=225,
                        annotation='затычки в быту',
                        weight=0.2,
                    ),
                    Stream(
                        name=StreamName.AVG_DT_WEIGHTED_BY_RANK_MOBILE,
                        region=225,
                        annotation='золочёные унитазы',
                        weight=0.67,
                    ),
                ],
            ),
            Offer(
                title='Трехэтажная табуретка',
                waremd5='ab5-JEtYmUQWRHaWRkEuqw',
                fesh=1,
                factor_streams=[
                    Stream(name=StreamName.NHOP, region=225, annotation='трехэтажная табуретка', weight=0.81)
                ],
            ),
            Offer(
                title='Детский паяльник',
                waremd5='Alcm7_LNZnNZgLveMk8yaA',
                fesh=1,
                factor_streams=[
                    Stream(name=StreamName.NHOP, region=225, annotation='паяльники не только для пыток', weight=0.72),
                    Stream(name=StreamName.NHOP, region=225, annotation='дети не то чем кажутся', weight=0.51),
                    Stream(name=StreamName.NHOP, region=225, annotation='всё лучшее -- детям', weight=0.11),
                ],
            ),
            Offer(
                title='Автомобильный аккумулятор',
                waremd5='wVgGa7pBuryin7aXCENrYg',
                fesh=1,
                factor_streams=[
                    Stream(
                        name=StreamName.MRKT_DESCRIPTION,
                        region=225,
                        annotation='автомобильный аккумулятор',
                        weight=0.74,
                    ),
                    Stream(name=StreamName.MRKT_TITLE, region=225, annotation='автомобильный аккумулятор', weight=1.0),
                    Stream(
                        name=StreamName.MRKT_MARKETING_DESCR,
                        region=225,
                        annotation='автомобильный аккумулятор',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.MRKT_MSKU_OFFER_SEARCH_TEXT,
                        region=225,
                        annotation='автомобильный аккумулятор',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.MRKT_MSKU_OFFER_TITLE,
                        region=225,
                        annotation='автомобильный аккумулятор',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.MRKT_VENDOR_NAME,
                        region=225,
                        annotation='автомобильный аккумулятор',
                        weight=1.0,
                    ),
                ],
            ),
            Offer(
                title='Спортивный автомобиль',
                waremd5='2ZG9LjFOKYfy5Vsx9X3K6w',
                fesh=1,
                factor_streams=[
                    Stream(name=StreamName.MRKT_DESCRIPTION, region=225, annotation='Черный автомобиль', weight=0.66),
                    Stream(name=StreamName.MRKT_TITLE, region=225, annotation='Черный автомобиль', weight=1.0),
                    Stream(
                        name=StreamName.MRKT_MARKETING_DESCR, region=225, annotation='Черный автомобиль', weight=1.0
                    ),
                    Stream(
                        name=StreamName.MRKT_MSKU_OFFER_SEARCH_TEXT,
                        region=225,
                        annotation='Черный автомобиль',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.MRKT_MSKU_OFFER_TITLE, region=225, annotation='Черный автомобиль', weight=1.0
                    ),
                    Stream(name=StreamName.MRKT_VENDOR_NAME, region=225, annotation='Черный автомобиль', weight=1.0),
                ],
            ),
            Offer(
                title='Бритвенный станок Gillette Fusion proglide',
                waremd5='lo9s-qxs5bq_Ldp2mSHcYw',
                fesh=1,
                factor_streams=[
                    Stream(
                        name=StreamName.MRKT_CPA_QUERY,
                        region=225,
                        annotation='джилет фьюжен 5 купить в москве',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.MRKT_CPA_QUERY, region=225, annotation='станок для бритья gillette', weight=1.0
                    ),
                    Stream(
                        name=StreamName.MRKT_CPA_QUERY,
                        region=225,
                        annotation='джилет лучше для мужчины нет',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.MRKT_CPA_QUERY, region=225, annotation='джилет странная реклама', weight=1.0
                    ),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=101010,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=101011,
                datafeed_id=2,
                priority_region=213,
                name='blue_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                fesh=101010,
                title="Голубой вагон бежит качается",
                hyperid=1,
                sku=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[BlueOffer()],
                factor_streams=[
                    Stream(name=StreamName.BLUE_MRKT_MODEL_ALIAS, region=225, annotation='РЖД Екб', weight=1.0),
                    Stream(
                        name=StreamName.BLUE_MRKT_MODEL_TITLE,
                        region=225,
                        annotation='Типовой вагон РЖД (плацкарт)',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.BLUE_MRKT_MARKETING_DESCR,
                        region=225,
                        annotation='Типовой вагон РЖД (плацкарт)',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.BLUE_MRKT_MICRO_MODEL_DESCR,
                        region=225,
                        annotation='Типовой вагон РЖД (плацкарт)',
                        weight=1.0,
                    ),
                ],
            ),
            MarketSku(
                fesh=101010,
                title="Спортивный автомобиль",
                hyperid=2,
                sku=2,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[BlueOffer()],
                factor_streams=[
                    Stream(
                        name=StreamName.BLUE_MRKT_OFFER_DESCRIPTION,
                        region=225,
                        annotation='Черный автомобиль',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.BLUE_MRKT_OFFER_TITLE, region=225, annotation='Черный автомобиль', weight=1.0
                    ),
                    Stream(
                        name=StreamName.BLUE_MRKT_MARKETING_DESCR,
                        region=225,
                        annotation='Черный автомобиль',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.BLUE_MRKT_MICRO_MODEL_DESCR,
                        region=225,
                        annotation='Черный автомобиль',
                        weight=1.0,
                    ),
                ],
            ),
            MarketSku(
                fesh=101010,
                title="Автомобильный аккумулятор",
                hyperid=3,
                sku=3,
                waremd5='Sku3-wdDXWsIiLVm1goleg',
                blue_offers=[BlueOffer()],
                factor_streams=[
                    Stream(
                        name=StreamName.BLUE_MRKT_OFFER_DESCRIPTION,
                        region=225,
                        annotation='автомобильный аккумулятор',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.BLUE_MRKT_OFFER_TITLE,
                        region=225,
                        annotation='автомобильный аккумулятор',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.BLUE_MRKT_MARKETING_DESCR,
                        region=225,
                        annotation='автомобильный аккумулятор',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.BLUE_MRKT_MICRO_MODEL_DESCR,
                        region=225,
                        annotation='автомобильный аккумулятор',
                        weight=1.0,
                    ),
                ],
            ),
            MarketSku(
                fesh=101010,
                title="Беговые кроссовки",
                hyperid=4,
                sku=4,
                waremd5='Sku4-wdDXWsIiLVm1goleg',
                blue_offers=[BlueOffer()],
                factor_streams=[
                    Stream(
                        name=StreamName.BLUE_MRKT_OFFER_DESCRIPTION, region=225, annotation='кроссовки найк', weight=1.0
                    ),
                    Stream(name=StreamName.BLUE_MRKT_OFFER_TITLE, region=225, annotation='кроссовки найк', weight=1.0),
                    Stream(
                        name=StreamName.BLUE_MRKT_MARKETING_DESCR, region=225, annotation='кроссовки найк', weight=1.0
                    ),
                    Stream(
                        name=StreamName.BLUE_MRKT_MICRO_MODEL_DESCR, region=225, annotation='кроссовки найк', weight=1.0
                    ),
                ],
            ),
            MarketSku(
                fesh=101010,
                title="Черные туфли",
                hyperid=5,
                sku=5,
                waremd5='Sku5-wdDXWsIiLVm1goleg',
                blue_offers=[BlueOffer()],
                factor_streams=[
                    Stream(
                        name=StreamName.BLUE_MRKT_OFFER_DESCRIPTION, region=225, annotation='черные туфли', weight=1.0
                    ),
                    Stream(name=StreamName.BLUE_MRKT_OFFER_TITLE, region=225, annotation='черные туфли', weight=1.0),
                    Stream(
                        name=StreamName.BLUE_MRKT_MARKETING_DESCR, region=225, annotation='черные туфли', weight=1.0
                    ),
                    Stream(
                        name=StreamName.BLUE_MRKT_MICRO_MODEL_DESCR, region=225, annotation='черные туфли', weight=1.0
                    ),
                ],
            ),
        ]

        # Для моделей факторы по стримам тоже должны считаться
        # проверяем т.ж. стрим по модельным алиасам
        cls.index.models += [
            Model(hyperid=234),
            Model(hyperid=345),
            Model(
                title='Бесшумный фен',
                hyperid=123,
                factor_streams=[
                    Stream(name=StreamName.SPLIT_DT, region=225, annotation='феноменальные фены', weight=0.23),
                    Stream(name=StreamName.MRKT_MODEL_ALIAS, region=225, annotation='бесшумный фен', weight=1.0),
                ],
            ),
            Model(
                title='Дырчатый чайник',
                hyperid=678,
                factor_streams=[
                    Stream(name=StreamName.MRKT_MODEL_ALIAS, region=225, annotation='чайник для чайников', weight=1.0),
                    Stream(name=StreamName.MRKT_MODEL_ALIAS, region=225, annotation='дырчатый плед', weight=1.0),
                ],
            ),
            Model(
                title='беспроводные наушники',
                hyperid=688,
                factor_streams=[
                    Stream(
                        name=StreamName.MRKT_IMG_SHOWS_TIME, region=225, annotation='беспроводные наушники', weight=1.0
                    ),
                    Stream(
                        name=StreamName.MRKT_IMG_QUERY_SHOWS_TIME,
                        region=225,
                        annotation='беспроводные наушники',
                        weight=0.11,
                    ),
                    Stream(
                        name=StreamName.MRKT_IMG_DWELL_TIME, region=225, annotation='беспроводные наушники', weight=0.3
                    ),
                    Stream(
                        name=StreamName.MRKT_IMG_QUERY_DWELL_TIME,
                        region=225,
                        annotation='беспроводные наушники',
                        weight=0.55,
                    ),
                    Stream(
                        name=StreamName.MRKT_IMG_DOC_DWELL_TIME,
                        region=225,
                        annotation='беспроводные наушники',
                        weight=0.91,
                    ),
                    Stream(name=StreamName.MRKT_IMG_SHOWS, region=225, annotation='беспроводные наушники', weight=0.33),
                    Stream(
                        name=StreamName.MRKT_IMG_CLICKS, region=225, annotation='беспроводные наушники', weight=0.53
                    ),
                    Stream(
                        name=StreamName.MRKT_IMG_QUERY_CLICKS,
                        region=225,
                        annotation='беспроводные наушники',
                        weight=0.71,
                    ),
                ],
            ),
            Model(
                title='Мобильный телефон',
                hyperid=689,
                factor_streams=[
                    Stream(
                        name=StreamName.MRKT_IMG_SHOWS_TIME, region=225, annotation='мобильный аккумулятор', weight=0.1
                    ),
                    Stream(
                        name=StreamName.MRKT_IMG_QUERY_SHOWS_TIME,
                        region=225,
                        annotation='проводной телефон',
                        weight=0.3,
                    ),
                    Stream(
                        name=StreamName.MRKT_IMG_DWELL_TIME, region=225, annotation='беспроводной телефон', weight=0.3
                    ),
                    Stream(
                        name=StreamName.MRKT_IMG_QUERY_DWELL_TIME, region=225, annotation='телефон samsung', weight=0.5
                    ),
                    Stream(
                        name=StreamName.MRKT_IMG_DOC_DWELL_TIME, region=225, annotation='телефон iphone', weight=0.5
                    ),
                    Stream(name=StreamName.MRKT_IMG_SHOWS, region=225, annotation='мобильная зарядка', weight=0.1),
                    Stream(name=StreamName.MRKT_IMG_CLICKS, region=225, annotation='мобильный автомобиль', weight=0.09),
                    Stream(name=StreamName.MRKT_IMG_QUERY_CLICKS, region=225, annotation='черный телефон', weight=0.48),
                ],
            ),
            Model(
                title='Черные туфли',
                hyperid=690,
                factor_streams=[
                    Stream(name=StreamName.MRKT_DESCRIPTION, region=225, annotation='черные туфли', weight=0.32),
                    Stream(name=StreamName.MRKT_WAS_ORDERED, region=225, annotation='черные туфли', weight=1.0),
                    Stream(name=StreamName.MRKT_TITLE, region=225, annotation='черные туфли', weight=1.0),
                    Stream(name=StreamName.MRKT_KNN_5000_9600, region=225, annotation='черные туфли', weight=0.51),
                    Stream(name=StreamName.MRKT_KNN_9600_10000, region=225, annotation='черные туфли', weight=0.52),
                    Stream(name=StreamName.MRKT_KNN_10000_11000, region=225, annotation='черные туфли', weight=0.53),
                    Stream(name=StreamName.MRKT_KNN_11000_20000, region=225, annotation='черные туфли', weight=0.54),
                    Stream(name=StreamName.YA_MARKET_SPLIT_DT, region=225, annotation='черные туфли', weight=0.32),
                    Stream(name=StreamName.MRKT_MARKETING_DESCR, region=225, annotation='черные туфли', weight=1.0),
                    Stream(
                        name=StreamName.MRKT_MSKU_OFFER_SEARCH_TEXT, region=225, annotation='черные туфли', weight=1.0
                    ),
                    Stream(name=StreamName.MRKT_MSKU_OFFER_TITLE, region=225, annotation='черные туфли', weight=1.0),
                    Stream(name=StreamName.MRKT_VENDOR_NAME, region=225, annotation='черные туфли', weight=1.0),
                ],
            ),
            Model(
                title='Беговые кроссовки',
                hyperid=691,
                factor_streams=[
                    Stream(name=StreamName.MRKT_DESCRIPTION, region=225, annotation='кроссовки найк', weight=0.58),
                    Stream(name=StreamName.MRKT_WAS_ORDERED, region=225, annotation='кроссовки найк', weight=1.0),
                    Stream(name=StreamName.MRKT_TITLE, region=225, annotation='кроссовки найк', weight=1.0),
                    Stream(name=StreamName.MRKT_KNN_5000_9600, region=225, annotation='кроссовки найк', weight=1.0),
                    Stream(name=StreamName.MRKT_KNN_9600_10000, region=225, annotation='кроссовки найк', weight=1.0),
                    Stream(name=StreamName.MRKT_KNN_10000_11000, region=225, annotation='кроссовки найк', weight=1.0),
                    Stream(name=StreamName.MRKT_KNN_11000_20000, region=225, annotation='кроссовки найк', weight=1.0),
                    Stream(name=StreamName.YA_MARKET_SPLIT_DT, region=225, annotation='кроссовки найк', weight=0.58),
                    Stream(name=StreamName.MRKT_MARKETING_DESCR, region=225, annotation='кроссовки найк', weight=1.0),
                    Stream(
                        name=StreamName.MRKT_MSKU_OFFER_SEARCH_TEXT, region=225, annotation='кроссовки найк', weight=1.0
                    ),
                    Stream(name=StreamName.MRKT_MSKU_OFFER_TITLE, region=225, annotation='кроссовки найк', weight=1.0),
                    Stream(name=StreamName.MRKT_VENDOR_NAME, region=225, annotation='кроссовки найк', weight=1.0),
                ],
            ),
            Model(
                title='Зимняя куртка',
                hyperid=692,
                factor_streams=[
                    Stream(name=StreamName.YA_MARKET_MANUFACTURER, region=225, annotation='зимняя куртка', weight=0.5),
                    Stream(name=StreamName.YA_MARKET_CATEGORY_NAME, region=225, annotation='зимняя куртка', weight=0.6),
                    Stream(name=StreamName.YA_MARKET_ALIASES, region=225, annotation='зимняя куртка', weight=0.7),
                    Stream(name=StreamName.YA_MARKET_URL, region=225, annotation='зимняя куртка', weight=0.8),
                    Stream(
                        name=StreamName.YA_MARKET_OFFER_DESCRIPTION, region=225, annotation='зимняя куртка', weight=0.45
                    ),
                    Stream(
                        name=StreamName.YA_MARKET_OFFER_SALES_NOTES, region=225, annotation='зимняя куртка', weight=0.38
                    ),
                    Stream(name=StreamName.YA_MARKET_OFFER_TITLE, region=225, annotation='зимняя куртка', weight=0.97),
                    Stream(name=StreamName.YA_MARKET_OFFER_URL, region=225, annotation='зимняя куртка', weight=0.21),
                ],
            ),
            Model(
                title='Фильтр для воды',
                hyperid=693,
                factor_streams=[
                    Stream(
                        name=StreamName.YA_MARKET_MANUFACTURER,
                        region=225,
                        annotation='нагреватель для воды',
                        weight=0.4,
                    ),
                    Stream(
                        name=StreamName.YA_MARKET_CATEGORY_NAME,
                        region=225,
                        annotation='фильтр для жидкости',
                        weight=0.5,
                    ),
                    Stream(
                        name=StreamName.YA_MARKET_ALIASES, region=225, annotation='фильтры для жидкости', weight=0.55
                    ),
                    Stream(name=StreamName.YA_MARKET_URL, region=225, annotation='очиститель для воды', weight=0.3),
                    Stream(
                        name=StreamName.YA_MARKET_OFFER_DESCRIPTION,
                        region=225,
                        annotation='чайник для воды',
                        weight=0.3,
                    ),
                    Stream(
                        name=StreamName.YA_MARKET_OFFER_SALES_NOTES,
                        region=225,
                        annotation='емкость для воды',
                        weight=0.29,
                    ),
                    Stream(
                        name=StreamName.YA_MARKET_OFFER_TITLE, region=225, annotation='фильтр для воздуха', weight=0.31
                    ),
                    Stream(
                        name=StreamName.YA_MARKET_OFFER_URL, region=225, annotation='фильтр для пылесоса', weight=0.31
                    ),
                ],
            ),
            Model(
                title='Джинсовые шорты',
                hyperid=694,
                factor_streams=[
                    Stream(name=StreamName.MRKT_IMG_LINK_DATA, region=225, annotation='джинсовые шорты', weight=0.56),
                    Stream(name=StreamName.MRKT_IMG_ALT_DATA, region=225, annotation='джинсовые шорты', weight=0.49),
                ],
            ),
            Model(
                title='Горный велосипед',
                hyperid=695,
                factor_streams=[
                    Stream(
                        name=StreamName.MRKT_IMG_LINK_DATA, region=225, annotation='прогулочный велосипед', weight=0.46
                    ),
                    Stream(name=StreamName.MRKT_IMG_ALT_DATA, region=225, annotation='дорожный велосипед', weight=0.79),
                ],
            ),
            Model(
                title='Бритвенный станок Gillette Fusion proglide',
                hyperid=1729924063,
                factor_streams=[
                    Stream(
                        name=StreamName.MRKT_CPA_QUERY,
                        region=225,
                        annotation='джилет фьюжен 5 купить в москве',
                        weight=1.0,
                    ),
                    Stream(
                        name=StreamName.MRKT_CPA_QUERY, region=225, annotation='станок для бритья gillette', weight=1.0
                    ),
                ],
            ),
        ]

    def test_factor_streams(self):
        """
        Тест проверяет, что считаются факторы по стримам (indexfactorann)
        """

        # Проверяем для офферов
        self.report.request_json('place=prime&text=затычка+для+унитаза&show-urls=external&rids=213')

        # Смотрим, что full_match_value совпадает со значением стрима, фиксируем значения
        # факторов, посчитанных другими алгоритмами (неочевидно, как по-хорошему их провалидировать,
        # см. https://wiki.yandex-team.ru/jandekspoisk/kachestvopoiska/factordev/web/factors/lingboost/tm)
        self.feature_log.expect(
            first_click_dt_xf_full_match_value=Round(0.55, 2),
            first_click_dt_xf_full_match_any_value=Round(0.55, 2),
            avg_dt_weighted_by_rank_mobile_all_wcm_max_prediction=Round(0.33, 2),
            avg_dt_weighted_by_rank_mobile_atten_v1_bm15_k001=Round(0.95, 2),
            ware_md5='wgrU12_pd1mqJ6DJm_9nEA',
        )

        # Отдельно проверяем, что для региона другой страны фактор считаться не будет
        self.report.request_json('place=prime&text=настенный+пылесос&show-urls=external&rids=213')

        self.feature_log.expect(browser_page_rank_mix_match_weighted_value=Absent(), ware_md5='ZRK9Q9nKpuAsmQsKgmUtyg')

        # Проверяем для вебовских описаний
        self.report.request_json('place=prime&text=автомобильный+аккумулятор&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_description_full_match_value=Round(0.74, 2),
            mrkt_description_all_wcm_match95_avg_value=Round(0.74, 2),
            mrkt_description_atten_v1_bm15_k001=Round(0.98, 2),
            mrkt_description_bm15_max_annotation_k001=Round(0.76, 2),
            ware_md5='wVgGa7pBuryin7aXCENrYg',
        )

        self.report.request_json('place=prime&text=спортивный+автомобиль&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_description_all_wcm_max_prediction=Round(0.13, 2),
            mrkt_description_all_wcm_weighted_value=Round(0.66, 2),
            ware_md5='2ZG9LjFOKYfy5Vsx9X3K6w',
        )

        # Для стрима по тайтлу
        self.report.request_json('place=prime&text=автомобильный+аккумулятор&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_title_full_match_value=Round(1.0, 2),
            mrkt_title_all_wcm_match95_avg_value=Round(1.0, 2),
            mrkt_title_bm15_max_annotation_k001=Round(0.76, 2),
            ware_md5='wVgGa7pBuryin7aXCENrYg',
        )

        self.report.request_json('place=prime&text=спортивный+автомобиль&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_title_all_wcm_max_prediction=Round(0.19, 2),
            mrkt_title_all_wcm_weighted_value=Round(1.0, 2),
            ware_md5='2ZG9LjFOKYfy5Vsx9X3K6w',
        )

        # Для стрима по маркетинговому описанию
        self.report.request_json('place=prime&text=автомобильный+аккумулятор&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_marketing_description_full_match_value=Round(1.0, 2),
            mrkt_marketing_description_all_wcm_match95_avg_value=Round(1.0, 2),
            mrkt_marketing_description_bm15_max_annotation_k001=Round(0.76, 2),
            ware_md5='wVgGa7pBuryin7aXCENrYg',
        )

        self.report.request_json('place=prime&text=спортивный+автомобиль&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_marketing_description_all_wcm_max_prediction=Round(0.19, 2),
            mrkt_marketing_description_all_wcm_weighted_value=Round(1.0, 2),
            ware_md5='2ZG9LjFOKYfy5Vsx9X3K6w',
        )

        # Для стрима по MSKU_OFFER_TITLE
        self.report.request_json('place=prime&text=автомобильный+аккумулятор&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_msku_offer_title_full_match_value=Round(1.0, 2),
            mrkt_msku_offer_title_all_wcm_match95_avg_value=Round(1.0, 2),
            mrkt_msku_offer_title_bm15_max_annotation_k001=Round(0.76, 2),
            ware_md5='wVgGa7pBuryin7aXCENrYg',
        )

        self.report.request_json('place=prime&text=спортивный+автомобиль&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_msku_offer_title_all_wcm_max_prediction=Round(0.19, 2),
            mrkt_msku_offer_title_all_wcm_weighted_value=Round(1.0, 2),
            ware_md5='2ZG9LjFOKYfy5Vsx9X3K6w',
        )

        # Для стрима по MSKU_OFFER_SEARCH_TEXT
        self.report.request_json('place=prime&text=автомобильный+аккумулятор&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_msku_offer_search_text_full_match_value=Round(1.0, 2),
            mrkt_msku_offer_search_text_all_wcm_match95_avg_value=Round(1.0, 2),
            mrkt_msku_offer_search_text_bm15_max_annotation_k001=Round(0.76, 2),
            ware_md5='wVgGa7pBuryin7aXCENrYg',
        )

        self.report.request_json('place=prime&text=спортивный+автомобиль&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_msku_offer_search_text_all_wcm_max_prediction=Round(0.19, 2),
            mrkt_msku_offer_search_text_all_wcm_weighted_value=Round(1.0, 2),
            ware_md5='2ZG9LjFOKYfy5Vsx9X3K6w',
        )

        # Для стрима по VENDOR_NAME
        self.report.request_json('place=prime&text=автомобильный+аккумулятор&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_vendor_name_full_match_value=Round(1.0, 2),
            mrkt_vendor_name_all_wcm_match95_avg_value=Round(1.0, 2),
            mrkt_vendor_name_bm15_max_annotation_k001=Round(0.76, 2),
            ware_md5='wVgGa7pBuryin7aXCENrYg',
        )

        self.report.request_json('place=prime&text=спортивный+автомобиль&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_vendor_name_all_wcm_max_prediction=Round(0.19, 2),
            mrkt_vendor_name_all_wcm_weighted_value=Round(1.0, 2),
            ware_md5='2ZG9LjFOKYfy5Vsx9X3K6w',
        )

        # Проверяем для CPA query стрима
        self.report.request_json('place=prime&text=станок для бритья gillette&show-urls=external&rids=213')

        self.feature_log.expect(
            market_cpa_query_all_wcm_weighted_value=Round(1.0, 2),
            market_cpa_query_cm_match_top5_avg_value=Round(0.4, 2),
            market_cpa_query_bclm_plane_proximity1_bm15_w0_size1_k0_01=Round(0.99, 2),
            market_cpa_query_bclm_weighted_proximity1_bm15_size1_k0_01=Round(0.97, 2),
            ware_md5='lo9s-qxs5bq_Ldp2mSHcYw',
        )

        # Удостоверяемся, что для моделей всё тоже работает
        self.report.request_json('place=prime&text=бесшумный+фен&show-urls=external&rids=213')

        self.feature_log.expect(
            split_dwell_time_all_wcm_max_prediction=Round(0.12, 2),
            split_dwell_time_bm15_max_annotation_k001=Round(0.47, 2),
            model_id=123,
        )

        # Проверяем, что по стриму NHopIsFinal факторы тоже считаются
        self.report.request_json('place=prime&text=трехэтажная+табуретка&show-urls=external&rids=213')

        self.feature_log.expect(nhop_is_final_full_match_value=Round(0.81, 2), ware_md5='ab5-JEtYmUQWRHaWRkEuqw')

        self.report.request_json('place=prime&text=паяльник+детский&show-urls=external&rids=213')

        self.feature_log.expect(
            nhop_is_final_all_wcm_max_prediction=Round(0.57, 2),
            nhop_is_final_all_wcm_weighted_value=Round(0.72, 2),
            nhop_is_final_bm15_max_annotation_k001=Round(0.78, 2),
            ware_md5='Alcm7_LNZnNZgLveMk8yaA',
        )

        # Проверяем, что по стриму по модельным алиасам факторы тоже считаются
        self.report.request_json('place=prime&text=бесшумный+фен&show-urls=external&rids=213')
        # Смотрим, что full_match_value совпадает со значением стрима, фиксируем значения
        # факторов, посчитанных другими алгоритмами
        self.feature_log.expect(
            mrkt_model_alias_full_match_value=Round(1.0, 2),
            mrkt_model_alias_all_wcm_match95_avg_value=Round(1.0, 2),
            mrkt_model_alias_atten_v1_bm15_k001=Round(0.98, 2),
            mrkt_model_alias_bm15_max_annotation_k001=Round(0.49, 2),
            model_id=123,
        )

        self.report.request_json('place=prime&text=дырачатый+чайник&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_model_alias_all_wcm_max_prediction=Round(0.5, 2),
            mrkt_model_alias_all_wcm_weighted_value=Round(1.0, 2),
            model_id=678,
        )

        # Проверяем для вебовских описаний
        self.report.request_json('place=prime&text=черные+туфли&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_description_full_match_value=Round(0.32, 2),
            mrkt_description_all_wcm_match95_avg_value=Round(0.32, 2),
            mrkt_description_atten_v1_bm15_k001=Round(0.96, 2),
            mrkt_description_bm15_max_annotation_k001=Round(0.49, 2),
            model_id=690,
        )

        self.report.request_json('place=prime&text=беговые+кроссовки&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_description_all_wcm_max_prediction=Round(0.29, 2),
            mrkt_description_all_wcm_weighted_value=Round(0.57, 2),
            model_id=691,
        )

        # Для стрима по тайтлу
        self.report.request_json('place=prime&text=черные+туфли&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_title_full_match_value=Round(1.0, 2),
            mrkt_title_all_wcm_match95_avg_value=Round(1.0, 2),
            mrkt_title_atten_v1_bm15_k001=Round(0.99, 2),
            mrkt_title_bm15_max_annotation_k001=Round(0.5, 2),
            model_id=690,
        )

        self.report.request_json('place=prime&text=беговые+кроссовки&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_title_all_wcm_max_prediction=Round(0.5, 2),
            mrkt_title_all_wcm_weighted_value=Round(1.0, 2),
            model_id=691,
        )

        # Для стрима по маркетинговому описанию
        self.report.request_json('place=prime&text=черные+туфли&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_marketing_description_full_match_value=Round(1.0, 2),
            mrkt_marketing_description_all_wcm_match95_avg_value=Round(1.0, 2),
            mrkt_marketing_description_atten_v1_bm15_k001=Round(0.99, 2),
            mrkt_marketing_description_bm15_max_annotation_k001=Round(0.5, 2),
            model_id=690,
        )

        self.report.request_json('place=prime&text=беговые+кроссовки&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_marketing_description_all_wcm_max_prediction=Round(0.5, 2),
            mrkt_marketing_description_all_wcm_weighted_value=Round(1.0, 2),
            model_id=691,
        )

        # Для стрима по MSKU_OFFER_TITLE
        self.report.request_json('place=prime&text=черные+туфли&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_msku_offer_title_full_match_value=Round(1.0, 2),
            mrkt_msku_offer_title_all_wcm_match95_avg_value=Round(1.0, 2),
            mrkt_msku_offer_title_atten_v1_bm15_k001=Round(0.99, 2),
            mrkt_msku_offer_title_bm15_max_annotation_k001=Round(0.5, 2),
            model_id=690,
        )

        self.report.request_json('place=prime&text=беговые+кроссовки&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_msku_offer_title_all_wcm_max_prediction=Round(0.5, 2),
            mrkt_msku_offer_title_all_wcm_weighted_value=Round(1.0, 2),
            model_id=691,
        )

        # Для стрима по MSKU_OFFER_SEARCH_TEXT
        self.report.request_json('place=prime&text=черные+туфли&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_msku_offer_search_text_full_match_value=Round(1.0, 2),
            mrkt_msku_offer_search_text_all_wcm_match95_avg_value=Round(1.0, 2),
            mrkt_msku_offer_search_text_atten_v1_bm15_k001=Round(0.99, 2),
            mrkt_msku_offer_search_text_bm15_max_annotation_k001=Round(0.5, 2),
            model_id=690,
        )

        self.report.request_json('place=prime&text=беговые+кроссовки&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_msku_offer_search_text_all_wcm_max_prediction=Round(0.5, 2),
            mrkt_msku_offer_search_text_all_wcm_weighted_value=Round(1.0, 2),
            model_id=691,
        )

        # Для стрима по VENDOR_NAME
        self.report.request_json('place=prime&text=черные+туфли&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_vendor_name_full_match_value=Round(1.0, 2),
            mrkt_vendor_name_all_wcm_match95_avg_value=Round(1.0, 2),
            mrkt_vendor_name_atten_v1_bm15_k001=Round(0.99, 2),
            mrkt_vendor_name_bm15_max_annotation_k001=Round(0.5, 2),
            model_id=690,
        )

        self.report.request_json('place=prime&text=беговые+кроссовки&show-urls=external&rids=213')
        self.feature_log.expect(
            mrkt_vendor_name_all_wcm_max_prediction=Round(0.5, 2),
            mrkt_vendor_name_all_wcm_weighted_value=Round(1.0, 2),
            model_id=691,
        )

        # Проверяем стримы для синего маркета
        self.report.request_json('place=prime&text=вагон ржд Екб&rids=213&show-urls=external&rgb=blue')
        self.feature_log.expect(
            bmrkt_wm_alias_annotation_max_value_weighted=Round(0.6667, 4),
            bmrkt_wm_alias_atten_v1_bm15_k001=Round(0.6568, 4),
            bmrkt_wm_title_all_wcm_max_prediction=Round(0.6667, 4),
            bmrkt_wm_title_atten_v1_bm15_k001=Round(0.6504, 4),
            bmrkt_wm_title_bclm_plane_proximity1_bm15_w0_size1_k001=Round(0.666, 4),
            bmrkt_wm_title_bm15_max_annotation_k001=Round(0.33, 4),
            bmrkt_marketing_description_of_model_all_wcm_max_prediction=Round(0.6667, 4),
            bmrkt_marketing_description_of_model_atten_v1_bm15_k001=Round(0.6504, 4),
            bmrkt_marketing_description_of_model_bclm_plane_proximity1_bm15_w0_size1_k001=Round(0.666, 4),
            bmrkt_marketing_description_of_model_bm15_max_annotation_k001=Round(0.33, 4),
            bmrkt_micro_description_of_model_all_wcm_max_prediction=Round(0.6667, 4),
            bmrkt_micro_description_of_model_atten_v1_bm15_k001=Round(0.6504, 4),
            bmrkt_micro_description_of_model_bclm_plane_proximity1_bm15_w0_size1_k001=Round(0.666, 4),
            bmrkt_micro_description_of_model_bm15_max_annotation_k001=Round(0.33, 4),
            model_id=1,
        )

        self.report.request_json('place=prime&text=спротивный+автомобиль&rids=213&show-urls=external&rgb=blue')
        self.feature_log.expect(
            bmrkt_marketing_description_of_model_all_wcm_max_prediction=Round(0.19, 2),
            bmrkt_marketing_description_of_model_all_wcm_weighted_value=Round(1.0, 2),
            bmrkt_micro_description_of_model_all_wcm_max_prediction=Round(0.19, 2),
            bmrkt_micro_description_of_model_all_wcm_weighted_value=Round(1.0, 2),
            model_id=2,
        )

        self.report.request_json('place=prime&text=черные+туфли&rids=213&show-urls=external&rgb=blue')
        self.feature_log.expect(
            bmrkt_marketing_description_of_model_full_match_value=Round(1.0, 2),
            bmrkt_marketing_description_of_model_all_wcm_match95_avg_value=Round(1.0, 2),
            bmrkt_marketing_description_of_model_atten_v1_bm15_k001=Round(0.99, 2),
            bmrkt_marketing_description_of_model_bm15_max_annotation_k001=Round(0.5, 2),
            bmrkt_micro_description_of_model_full_match_value=Round(1.0, 2),
            bmrkt_micro_description_of_model_all_wcm_match95_avg_value=Round(1.0, 2),
            bmrkt_micro_description_of_model_atten_v1_bm15_k001=Round(0.99, 2),
            bmrkt_micro_description_of_model_bm15_max_annotation_k001=Round(0.5, 2),
            model_id=5,
        )

        self.report.request_json('place=prime&text=беговые+кроссовки&rids=213&show-urls=external&rgb=blue')
        self.feature_log.expect(
            bmrkt_marketing_description_of_model_all_wcm_max_prediction=Round(0.5, 2),
            bmrkt_marketing_description_of_model_all_wcm_weighted_value=Round(1.0, 2),
            bmrkt_micro_description_of_model_all_wcm_max_prediction=Round(0.5, 2),
            bmrkt_micro_description_of_model_all_wcm_weighted_value=Round(1.0, 2),
            model_id=4,
        )

        # Проверяем для CPA query стрима
        self.report.request_json('place=prime&text=станок+джилет&show-urls=external&rids=213')
        self.feature_log.expect(
            market_cpa_query_all_wcm_weighted_value=Round(1.0, 2),
            market_cpa_query_cm_match_top5_avg_value=Round(0.2, 2),
            market_cpa_query_bclm_plane_proximity1_bm15_w0_size1_k0_01=Round(0.99, 2),
            market_cpa_query_bclm_weighted_proximity1_bm15_size1_k0_01=Round(0.97, 2),
            model_id=1729924063,
        )

    @classmethod
    def prepare_experimental_panther(cls):
        cls.settings.enable_experimental_panther = True

    def test_experimental_panther(self):
        """
        Тест пока просто проверяет, что запрос не падает как при использовании обычного
        пантерного индекса, так и при использовании экспериментального. Факт использования
        пока проверен только вручную. В данный момент в лайте обычный
        и экспериментальный индекс -- одно и то же, а путь до пантерного индекса, который
        используется поиском, глубоко запрятан и не светится даже в дебажной выдаче.
        Когда будет сделана версия конвертера, берущая новые стримы с развесовками,
        эта версия будет включена и в лайте для indexexppanther, и тест станет более
        содержательным.
        """
        self.report.request_json('place=prime&text=ололо')

        self.report.request_json('place=prime&text=ололо&rearr-factors=market_use_exp_panther_index=1')

    def test_experimental_panther2(self):
        """
        Тест проверяет корректную обработку ситуации отсутствия второй экспериментальной пантеры
        """
        self.report.request_json('place=prime&text=ололо')

        self.report.request_json('place=prime&text=ололо&rearr-factors=market_use_exp_panther2_index=1')

    def test_limits_trace(self):
        response = self.report.request_json('place=prime&text=затычка+для+унитаза&show-urls=external&rids=213&debug=da')

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "debug": {
                            "properties": {
                                "TM_LIMITS_RAW": "Total: 0 L0: 0 L0 hits: 5 L1: 0 L1 hits: 0",
                                "TM_LIMITS_WORDS": "Soft: 0 Hard: 0 Hits: 5",
                                "TM_LIMITS_WORDS_ORIGINAL": "Soft: 0 Hard: 0 Hits: 5",
                            }
                        }
                    }
                ]
            },
        )

    @classmethod
    def prepare_factor_streams_api(cls):
        cls.settings.use_factorann = True

        cls.index.offers += [
            Offer(
                title='ручка рука',
                waremd5='o9xZkNS16VL9fhxlqCTH7w',
                fesh=1,
                factor_streams=[Stream(name=StreamName.BQPR, region=225, annotation='ручка рука', weight=0.64)],
            ),
            Offer(
                title='ножка нога',
                waremd5='cUYjHHdl5EqTzSMHIux_Pw',
                fesh=1,
                factor_streams=[Stream(name=StreamName.BQPR, region=225, annotation='ножка нога', weight=0.64)],
            ),
        ]

    def test_factor_streams_api(self):
        """
        Тест проверяет, что на апи факторы по стримам считаются только для приложения
        """

        request = 'place=prime&text=%s&show-urls=external&rids=213'
        api = '&api=content'
        app = '&content-api-client=101'

        self.report.request_json(request % 'ручка рука' + api)

        self.feature_log.expect(
            browser_page_rank_full_match_any_value=NotEmpty(), ware_md5='o9xZkNS16VL9fhxlqCTH7w'
        ).never()

        self.report.request_json(request % 'ножка нога' + api + app)

        self.feature_log.expect(
            browser_page_rank_full_match_any_value=NotEmpty(), ware_md5='cUYjHHdl5EqTzSMHIux_Pw'
        ).once()


if __name__ == '__main__':
    main()
