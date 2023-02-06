#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, Currency, HyperCategory, HyperCategoryType, MarketSku, Model, Offer, Shop, Tax, Vat
from core.testcase import TestCase, main
from core.types.cms_promo import CmsFeaturedMsku, CmsPromo
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(
                fesh=1,
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
                fesh=3,
                datafeed_id=3,
                priority_region=2,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=1,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.1.1',
                        randx=1101,
                    )
                ],
                randx=1100,
            ),
            MarketSku(
                title="blue offer sku2",
                hyperid=2,
                sku=2,
                blue_offers=[
                    BlueOffer(
                        price=6,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.2.1',
                        randx=1001,
                    )
                ],
                randx=1000,
            ),
            # СКУ не участвующий в акциях
            MarketSku(
                title="blue offer sku3",
                hyperid=3,
                sku=3,
                blue_offers=[
                    BlueOffer(
                        price=10,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.3.1',
                        randx=1201,
                    )
                ],
                randx=1200,
            ),
        ]

        cls.index.cms_promos += [
            CmsPromo(
                promo_id='both',
                featured_mskus=[
                    CmsFeaturedMsku(msku=1, picture='pic_1', description='descr_1'),
                    CmsFeaturedMsku(msku=2, picture='pic_2', description='descr_2'),
                ],
                relevance_forced_mskus=[1, 2],
            ),
            CmsPromo(
                promo_id='firstOnly',
                featured_mskus=[
                    CmsFeaturedMsku(msku=1, picture='pic_1', description='descr_1'),
                ],
                relevance_forced_mskus=[1],
            ),
            CmsPromo(
                promo_id='second_only',
                featured_mskus=[
                    CmsFeaturedMsku(msku=2, picture='pic_2', description='descr_2'),
                ],
                relevance_forced_mskus=[2],
            ),
            CmsPromo(
                promo_id='without_force',
                relevance_forced_mskus=[],
            ),
        ]

    def check_cms_featured_msku(self, promo_id, first_offer, second_offer):
        arg_str = '&show_promo_features={}'.format(promo_id) if promo_id else ''

        resp = self.report.request_json('rgb=Blue&place=prime&text=blue+offer' + arg_str)
        self.assertFragmentIn(
            resp,
            {
                'entity': 'offer',
                'marketSku': '1',
                'features': first_offer,
            },
        )

        self.assertFragmentIn(
            resp,
            {
                'entity': 'offer',
                'marketSku': '2',
                'features': second_offer,
            },
        )

    def test_cms_featured_mskus(self):
        '''
        Проверяем вывод доп. информации об оферах, участвующих в акции
        Данные акций выгружаются из CMS.
        Для оферов, имеющих записи в акции выводится: ссылка на картинку, описание СКУ
        '''

        def promo(_id):
            return {
                'type': 'promo',
                'picture': 'pic_{}'.format(_id),
                'description': 'descr_{}'.format(_id),
            }

        # акция работает для обоих оферов
        self.check_cms_featured_msku('both', [promo(1)], [promo(2)])

        # В акции настроен только первый офер
        self.check_cms_featured_msku('firstOnly', [promo(1)], Absent())

        # В акции настроен только второй офер
        self.check_cms_featured_msku('second_only', Absent(), [promo(2)])

        # акции не существут
        self.check_cms_featured_msku('missed_promo', Absent(), Absent())

        # Без запрошенной акции ничего не показывается
        self.check_cms_featured_msku(None, Absent(), Absent())

    def check_cms_force_relevance(self, promo_id, msku1, msku2, msku3):
        arg_str = '&force-relevance-promo={}'.format(promo_id) if promo_id else ''

        how_popular = ''  # Сортировка по-умолчанию
        how_popular_guru = '&force-guru-popularity=1'  # Сортировка по гуру популярности
        how_aprice = '&how=aprice'
        how_dprice = '&how=dprice'

        for arg_how in [how_popular, how_popular_guru]:
            resp = self.report.request_json(
                'rgb=Blue&place=prime&text=blue+offer&allow-collapsing=0' + arg_how + arg_str
            )
            self.assertFragmentIn(
                resp,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'marketSku': str(msku1),
                        },
                        {
                            'entity': 'offer',
                            'marketSku': str(msku2),
                        },
                        {
                            'entity': 'offer',
                            'marketSku': str(msku3),
                        },
                    ]
                },
                preserve_order=True,
            )

        # При сортировке по убыванию цены изменение ранжирования по промо не работает. Первое МСКУ на первом месте
        resp = self.report.request_json(
            'rgb=Blue&place=prime&text=blue+offer&allow-collapsing=0' + how_aprice + arg_str
        )
        self.assertFragmentIn(
            resp,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'marketSku': '1',
                    },
                    {
                        'entity': 'offer',
                        'marketSku': '2',
                    },
                    {
                        'entity': 'offer',
                        'marketSku': '3',
                    },
                ]
            },
            preserve_order=True,
        )

        # При сортировке по убыванию цены изменение ранжирования по промо не работает. Второе МСКУ на первом месте
        resp = self.report.request_json(
            'rgb=Blue&place=prime&text=blue+offer&allow-collapsing=0' + how_dprice + arg_str
        )
        self.assertFragmentIn(
            resp,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'marketSku': '3',
                    },
                    {
                        'entity': 'offer',
                        'marketSku': '2',
                    },
                    {
                        'entity': 'offer',
                        'marketSku': '1',
                    },
                ]
            },
            preserve_order=True,
        )

    def test_cms_force_relevance(self):
        '''
        Проверяем изменение порядка ранжирования для оферов, участвующих в акции
        Оферы, отмеченные в промо акции в cms идут на первый план
        '''
        # Берется дефолтное ранжирование:
        # * Нет приоритета МСКУ
        # * акции не существут
        # * Без запрошенной акции
        for promo_id in ['without_force', 'missed_promo', None]:
            self.check_cms_force_relevance(promo_id, 3, 1, 2)

        # Приоритетен 1 МСКУ
        self.check_cms_force_relevance('firstOnly', 1, 3, 2)

        # Приоритетен 2 МСКУ
        self.check_cms_force_relevance('second_only', 2, 3, 1)

        # Приоритетны оба МСКУ
        self.check_cms_force_relevance('both', 1, 2, 3)

    @classmethod
    def prepare_cms_msku_promo_search(cls):
        cls.index.mskus += [
            MarketSku(
                title="with_promo_cms",
                hyperid=1,
                sku=11,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.1.11',
                    )
                ],
                cms_promo_literal="cms_promo_1",
            ),
            MarketSku(
                title="with_promo_cms_2",
                hyperid=2,
                sku=12,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.1.12',
                    )
                ],
                # Смена регистра кодируется символом ^ перед символом, сменившим регистр.
                # См. https://a.yandex-team.ru/arc/trunk/arcadia/market/library/offer_id_rewriter/offer_id_rewriter.cpp
                cms_promo_literal="^c^ms^p^romo2",
            ),
            MarketSku(
                title="with_promo_cms_3",
                hyperid=3,
                sku=13,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.1.13',
                    )
                ],
                cms_promo_literal="cmspromo2",
            ),
        ]

    def __check_cms_msku_promo_search(self, promo_id, result_mskus):
        resp = self.report.request_json('rgb=Blue&place=prime&filter-by-cms-promo={}'.format(promo_id))
        self.assertFragmentIn(
            resp,
            {
                'results': [
                    {
                        'entity': 'product',
                        'offers': {
                            'items': [
                                {
                                    'entity': 'offer',
                                    'marketSku': str(msku),
                                }
                            ],
                        },
                    }
                    for msku in result_mskus
                ]
            },
            allow_different_len=False,
        )

    def test_cms_msku_promo_search(self):
        """
        Проверяем выдачу prime с ограничением по акции, заведенной в cms
        """
        self.__check_cms_msku_promo_search('cms_promo_1', [11])

        # Проверяем, что регистр влияет на выдачу
        self.__check_cms_msku_promo_search('CmsPromo2', [12])
        self.__check_cms_msku_promo_search('cmspromo2', [13])

        # Выдача нескольких акций сразу
        self.__check_cms_msku_promo_search('CmsPromo2,cmspromo2,cms_promo_1', [11, 12, 13])

    @classmethod
    def prepare_cms_model_promo_search(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=1,
                children=[
                    HyperCategory(hid=90569, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=12345, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=67890, output_type=HyperCategoryType.SIMPLE),
                    HyperCategory(hid=42424, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        cls.index.shops += [Shop(fesh=720, priority_region=213), Shop(fesh=721)]
        cls.index.models += [
            Model(hyperid=14115324, title="Фен Saturn ST-HC7355", hid=90569, cms_promo_literal="cmspromo2000"),
        ]

        cls.index.offers += [
            Offer(hyperid=14115324, title="Фен Saturn ST-HC7355", fesh=720, price=342, cms_promo_literal="123"),
        ]

    def test_cms_model_promo_search(self):
        """
        Проверяем выдачу prime с ограничением по акции, заведенной в cms
        """
        resp = self.report.request_json('rgb=green_with_blue&place=prime&filter-by-cms-promo=cmspromo2000')
        self.assertFragmentIn(
            resp,
            {
                'results': [
                    {
                        'entity': 'product',
                    }
                ]
            },
            allow_different_len=False,
        )

        resp = self.report.request_json('rgb=green_with_blue&place=prime&filter-by-cms-promo=123')
        self.assertFragmentIn(
            resp,
            {
                'results': [
                    {
                        'entity': 'offer',
                    }
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
