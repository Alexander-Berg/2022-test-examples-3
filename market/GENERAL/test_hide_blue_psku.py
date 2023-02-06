#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, Currency, MarketSku, Model, Offer, Shop, Tax, YamarecPlace, YamarecSettingPartition
from core.testcase import TestCase, main
from core.matcher import Absent, EmptyList


MARKET_CATEGORY_ID = 1
PARTNER_CATEGORY_ID = 2

SUPPLIER_SHOP_ID = 1
VIRTUAL_SHOP_ID = 2

SUPPLIER_FEED_ID = 1
VIRTUAL_SHOP_FEED_ID = 2

# Здесь представлены лишь выборочные place'ы для тестирования
# См. https://st.yandex-team.ru/MARKETOUT-29275#5e3d46b098191a0f5da490ae
WHITE_LIST_PLACE = 'prime'
WHITE_LIST_RECOM_PLACE = 'also_viewed'
FORCE_SHOW_PSKU_PLACE = 'price_recommender'

# Models
MARKET_MODEL = Model(hyperid=11, title="Обычная модель", hid=MARKET_CATEGORY_ID)

PARTNER_MODEL = Model(hyperid=12, title="Партнерская модель", is_pmodel=True, hid=PARTNER_CATEGORY_ID)

ANOTHER_MARKET_MODEL = Model(hyperid=13, title="Еще одная обычная модель", hid=MARKET_CATEGORY_ID)

# Blue offers
MARKET_OFFER = BlueOffer(
    price=10000,
    offerid='Shop1_sku01',
    waremd5='Sku01Price10k-vm1Goleg',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
)

PARTNER_OFFER = BlueOffer(
    price=12000,
    offerid='Shop1_sku02',
    waremd5='Sku02Price12k-vm1Goleg',
    forbidden_market_mask=Offer.IS_PSKU,
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
)

ANOTHER_MARKET_OFFER = BlueOffer(
    price=11000,
    offerid='Shop1_sku03',
    waremd5='Sku03Price11k-vm1Goleg',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
)

MARKET_SKU = MarketSku(
    title="Тестовый синий оффер от Маркета",
    hid=MARKET_CATEGORY_ID,
    hyperid=MARKET_MODEL.hyper,
    sku=1,
    blue_offers=[MARKET_OFFER],
)

PARTNER_SKU = MarketSku(
    title="Тестовый синий оффер от партнера",
    hid=PARTNER_CATEGORY_ID,
    hyperid=PARTNER_MODEL.hyper,
    sku=2,
    blue_offers=[PARTNER_OFFER],
    forbidden_market_mask=Offer.IS_PSKU,
)

ANOTHER_MARKET_SKU = MarketSku(
    title="Еще один тестовый синий оффер от Маркета",
    hid=MARKET_CATEGORY_ID,
    hyperid=ANOTHER_MARKET_MODEL.hyper,
    sku=3,
    blue_offers=[ANOTHER_MARKET_OFFER],
)

REQUEST_BASE = 'place={place}' '&hid={category}' '&pp=18' '&rgb={color}'


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'
        cls.settings.hide_partner_documents = True
        cls.settings.lms_autogenerate = True

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            Shop(
                fesh=SUPPLIER_SHOP_ID,
                datafeed_id=SUPPLIER_FEED_ID,
                name="Тестовый поставщик",
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=VIRTUAL_SHOP_ID,
                datafeed_id=VIRTUAL_SHOP_FEED_ID,
                name="Тестовый виртуальный поставщик",
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [MARKET_SKU, PARTNER_SKU, ANOTHER_MARKET_SKU]

    @classmethod
    def prepare_also_viewed_place(cls):
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[YamarecSettingPartition(params={'version': 'partition_v1'}, splits=[{'split': '1'}])],
            )
        ]
        cls.recommender.on_request_accessory_models(
            model_id=MARKET_MODEL.hyper, item_count=1000, version='partition_v1'
        ).respond({'models': [str(model.hyper) for model in [PARTNER_MODEL, ANOTHER_MARKET_MODEL]]})

    @staticmethod
    def __get_response_fragment(model_offer_list=None):
        return (
            {'results': EmptyList()}
            if model_offer_list is None
            else {
                'results': [
                    {
                        'entity': 'product',
                        'id': model.hyper,
                        'offers': {
                            'items': [{'entity': 'offer', 'wareId': offer.waremd5}] if offer is not None else Absent()
                        },
                    }
                    for model, offer in model_offer_list
                ]
            }
        )

    def test_psku_hidden(self):
        """
        Проверяем, что PSKU скрыт через настройку 'HidePartnerDocuments'
        в конфигурационном файле Report, а MSKU присутствуют на выдаче

        Также проверяем, что CGI-параметр '&show-partner-documents' и rearr флаг 'show_partner_documents'
        имеет больший приоритет, чем настройка в конфигурационном файле
        """
        request = REQUEST_BASE.format(
            place=WHITE_LIST_PLACE,
            category=','.join(str(i) for i in [MARKET_CATEGORY_ID, PARTNER_CATEGORY_ID]),
            color='blue',
        )
        model_offer_list = [(MARKET_MODEL, MARKET_OFFER), (ANOTHER_MARKET_MODEL, ANOTHER_MARKET_OFFER)]

        response = self.report.request_json(request)
        self.assertFragmentIn(response, T.__get_response_fragment(model_offer_list), allow_different_len=False)

        model_offer_list.append((PARTNER_MODEL, PARTNER_OFFER))
        response = self.report.request_json(request + '&show-partner-documents=1')
        self.assertFragmentIn(response, T.__get_response_fragment(model_offer_list), allow_different_len=False)

        response = self.report.request_json(request + '&rearr-factors=market_show_partner_documents=1')
        self.assertFragmentIn(response, T.__get_response_fragment(model_offer_list), allow_different_len=False)

    def test_psku_hidden_for_recom(self):
        """
        Аналогичный тест на скрытие PSKU, но для рекомендательных place'ов

        Обращаем внимание, что  в рекомендательных плейсах модели без дефолтных офферов всегда выфильтровываются.
        """
        request = REQUEST_BASE + '&rearr-factors=split=1&hyperid={model}'
        request = request.format(
            place=WHITE_LIST_RECOM_PLACE,
            category=','.join(str(i) for i in [MARKET_CATEGORY_ID, PARTNER_CATEGORY_ID]),
            color='white',
            model=MARKET_MODEL.hyper,
        )

        model_offer_list = [(ANOTHER_MARKET_MODEL, ANOTHER_MARKET_OFFER)]
        response = self.report.request_json(request)
        self.assertFragmentIn(response, T.__get_response_fragment(model_offer_list), allow_different_len=False)

        model_offer_list = [(ANOTHER_MARKET_MODEL, ANOTHER_MARKET_OFFER), (PARTNER_MODEL, PARTNER_OFFER)]
        response = self.report.request_json(request + '&show-partner-documents=1')
        self.assertFragmentIn(response, T.__get_response_fragment(model_offer_list), allow_different_len=False)

    def test_psku_hidden_for_force_show_places(self):
        """
        Проверяем, что PSKU присутсвуют на выдаче в place'ах, для которых флаг
        'MarketSearch::SortingSpec::show_partner_documents' принудительно установлен
        в true в обход 'NBaseArgs::CreateDefault()'
        """
        request = REQUEST_BASE.format(
            place=FORCE_SHOW_PSKU_PLACE,
            category=','.join(str(i) for i in [MARKET_CATEGORY_ID, PARTNER_CATEGORY_ID]),
            color='blue',
        )

        for msku, price in [(MARKET_SKU.sku, MARKET_OFFER.price), (PARTNER_SKU.sku, PARTNER_OFFER.price)]:
            response = self.report.request_json(request + '&market-sku={msku}'.format(msku=msku))
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'marketSKU': str(msku),
                            'isGoldenMatrixSKU': False,
                            'priceRecommendations': [
                                {
                                    'tag': 'minPriceMarket',
                                    'price': price,
                                    'shows': 0,
                                }
                            ],
                        }
                    ]
                },
            )


if __name__ == '__main__':
    main()
