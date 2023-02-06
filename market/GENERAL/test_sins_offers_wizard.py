#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from itertools import chain

from core.types import HyperCategory, MnPlace, Model, ModelGroup, NavCategory, NewShopRating, Offer, Opinion, Shop
from core.testcase import TestCase, main

RELEVANCE_DEFAULT = 0.3
RELEVANCE_LESS = 0.4
RELEVANCE_MORE = 0.6
RELEVANCES = [RELEVANCE_LESS, RELEVANCE_MORE]
ts_list = []
MAX_TITLE_LENGTH = 120
SHOPS = {
    'electronics_shop_1': Shop(
        fesh=1,
        name="Electronics shop 1",
        priority_region=213,
        cpa=Shop.CPA_REAL,
        shop_logo_info='28:28:PNG',
        shop_logo_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/electronics_shop_1/small',
        shop_logo_retina_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/electronics_shop_1/orig',
        new_shop_rating=NewShopRating(new_rating_total=3.2),
    ),
    'electronics_shop_2': Shop(
        fesh=2,
        name="Electronics shop 2",
        priority_region=213,
        cpa=Shop.CPA_REAL,
        shop_logo_info='28:28:PNG',
        shop_logo_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/electronics_shop_2/small',
        shop_logo_retina_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/electronics_shop_2/orig',
        new_shop_rating=NewShopRating(new_rating_total=3.2),
    ),
    'coffee_shop_1': Shop(
        fesh=3,
        name="Coffee shop 1",
        priority_region=213,
        cpa=Shop.CPA_REAL,
        shop_logo_info='28:28:PNG',
        shop_logo_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/coffee_shop_1/small',
        shop_logo_retina_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/coffee_shop_1/orig',
        new_shop_rating=NewShopRating(new_rating_total=3.2),
    ),
    'coffee_shop_2': Shop(
        fesh=4,
        name="Coffee shop 2",
        priority_region=213,
        cpa=Shop.CPA_REAL,
        shop_logo_info='28:28:PNG',
        shop_logo_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/coffee_shop_2/small',
        shop_logo_retina_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/coffee_shop_2/orig',
        new_shop_rating=NewShopRating(new_rating_total=3.2),
    ),
    'grocery_shop_1': Shop(
        fesh=5,
        name="Grocery shop 1",
        priority_region=213,
        cpa=Shop.CPA_REAL,
        shop_logo_info='28:28:PNG',
        shop_logo_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/grocery_shop_1/small',
        shop_logo_retina_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/grocery_shop_1/orig',
        new_shop_rating=NewShopRating(new_rating_total=3.2),
    ),
    'long_name_shop': Shop(
        fesh=6,
        name="Long name shop",
        priority_region=213,
        cpa=Shop.CPA_REAL,
        shop_logo_info='28:28:PNG',
        shop_logo_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/long_name_shop/small',
        shop_logo_retina_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/long_name_shop/orig',
        new_shop_rating=NewShopRating(new_rating_total=3.2),
    ),
    'market': Shop(
        fesh=431782,
        priority_region=213,
        fulfillment_virtual=True,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        name='Яндекс.Маркет',
        cpa=Shop.CPA_REAL,
        supplier_type=Shop.FIRST_PARTY,  # Offers of this shop must be excluded as it's 1p.
    ),
}
SHOP_BY_BSID = {i.fesh: i for i in SHOPS.values()}

CATEGORIES = {
    'root': HyperCategory(
        hid=1,
        name='root',
        children=[
            HyperCategory(
                hid=2,
                name='electronics',
                children=[
                    HyperCategory(
                        hid=3,
                        name='accessories',
                    ),
                    HyperCategory(
                        hid=4,
                        name='ios',
                        children=[
                            HyperCategory(
                                hid=5,
                                name='iphones',
                            )
                        ],
                    ),
                ],
            ),
            HyperCategory(
                hid=6,
                name='food',
                children=[
                    HyperCategory(
                        hid=7,
                        name='drinks',
                    ),
                ],
            ),
            HyperCategory(
                hid=8,
                name='Category with very long name ' * 10,
            ),
        ],
    ),
}
CATEGORIES['electronics'] = CATEGORIES['root'].children[0]
CATEGORIES['food'] = CATEGORIES['root'].children[1]
CATEGORIES['accessories'] = CATEGORIES['electronics'].children[0]
CATEGORIES['ios'] = CATEGORIES['electronics'].children[1]
CATEGORIES['iphones'] = CATEGORIES['ios'].children[0]
CATEGORIES['drinks'] = CATEGORIES['food'].children[0]
CATEGORIES['long name category'] = CATEGORIES['root'].children[2]

MODEL_GROUP_HYPERID = 10000
MODEL_GROUPS = [
    ModelGroup(
        hyperid=MODEL_GROUP_HYPERID,
        title='Iphone Group Model',
        hid=CATEGORIES['iphones'].hid,
        rgb_type=Model.RGB_BLUE,
    ),
]

MODELS = {
    'iphones': [
        Model(
            hid=CATEGORIES['iphones'].hid,
            title="iPhone 0",
            opinion=Opinion(rating=3.0),
        ),
        Model(
            hid=CATEGORIES['iphones'].hid,
            title="iPhone 1",
            opinion=Opinion(rating=3.1),
        ),
        Model(
            hid=CATEGORIES['iphones'].hid,
            title="iPhone 2",
            opinion=Opinion(rating=3.2),
        ),
        Model(
            hid=CATEGORIES['iphones'].hid,
            title="iPhone 3 (blue model)",
            opinion=Opinion(rating=3.3),
            group_hyperid=MODEL_GROUP_HYPERID,
            rgb_type=Model.RGB_BLUE,
        ),
        Model(
            hid=CATEGORIES['iphones'].hid,
            title="iPhone 4",
        ),
    ],
    'accessories': [
        Model(
            hid=CATEGORIES['accessories'].hid,
            title="iPhone accessory 0",
            opinion=Opinion(rating=3.0),
        ),
        Model(
            hid=CATEGORIES['accessories'].hid,
            title="iPhone accessory 1 for CPC offer",
            opinion=Opinion(rating=3.1),
        ),
        Model(
            hid=CATEGORIES['accessories'].hid,
            title="iPhone accessory 2",
            opinion=Opinion(rating=3.2),
        ),
        Model(
            hid=CATEGORIES['accessories'].hid,
            title="iPhone accessory 3",
        ),
    ],
    'coffee': [
        Model(
            hid=CATEGORIES['drinks'].hid,
            title="Coffee 0",
            opinion=Opinion(rating=2.0),
        ),
        Model(
            hid=CATEGORIES['drinks'].hid,
            title="Coffee 1 for CPC offer",
            opinion=Opinion(rating=2.1),
        ),
        Model(
            hid=CATEGORIES['drinks'].hid,
            title="Coffee 2",
            opinion=Opinion(rating=2.2),
        ),
        Model(
            hid=CATEGORIES['drinks'].hid,
            title="Coffee 3",
        ),
    ],
    'chips': [
        Model(
            hid=CATEGORIES['food'].hid,
            title="Chips 0",
            opinion=Opinion(rating=2.0),
        ),
        Model(
            hid=CATEGORIES['food'].hid,
            title="Chips 1 for CPC offer",
            opinion=Opinion(rating=2.1),
        ),
        Model(
            hid=CATEGORIES['food'].hid,
            title="Chips 2",
            opinion=Opinion(rating=2.2),
        ),
        Model(
            hid=CATEGORIES['food'].hid,
            title="Chips 3",
        ),
    ],
    'long name models': [
        Model(
            hid=CATEGORIES['long name category'].hid,
            hyperid=38805000,
            title='Model with very long name ' * 10,
            opinion=Opinion(rating=3.0),
        ),
    ],
}

OFFERS_BY_SHOP = {
    'electronics_shop_1': [
        Offer(
            cpa=Offer.CPA_NO if i == 1 else Offer.CPA_REAL,  # Second offer as CPC
            title=m.title,
            hid=m.hid,
            hyperid=MODEL_GROUP_HYPERID
            if '(blue model)' in m.title
            else m.hid
            if i > 0
            else None,  # First offer without model.
            fesh=SHOPS['electronics_shop_1'].fesh,
            price=100 + i,
            discount=50,
            ts=700 + i,
        )
        for i, m in enumerate(MODELS['iphones'] + [MODELS['accessories'][0]])
    ],
    'electronics_shop_2': [
        Offer(
            cpa=Offer.CPA_NO if i == 1 else Offer.CPA_REAL,  # Second offer as CPC
            title=m.title,
            hid=m.hid,
            hyperid=m.hid if i > 0 else None,  # First offer without model.
            fesh=SHOPS['electronics_shop_2'].fesh,
            price=100 + i,
            discount=50,
            ts=800 + i,
        )
        for i, m in enumerate(MODELS['accessories'])
    ],
    'market': [
        Offer(
            cpa=Offer.CPA_NO if i == 1 else Offer.CPA_REAL,  # Second offer as CPC
            title=m.title,
            hid=m.hid,
            hyperid=m.hid if i > 0 else None,  # First offer without model.
            fesh=SHOPS['market'].fesh,
            price=100 + i,
            discount=50,
            ts=900 + i,
        )
        for i, m in enumerate(MODELS['iphones'])
    ],
    'coffee_shop_1': [
        Offer(
            cpa=Offer.CPA_NO if i == 1 else Offer.CPA_REAL,  # Second offer as CPC
            title=m.title,
            hid=m.hid,
            hyperid=m.hid if i > 0 else None,  # First offer without model.
            fesh=SHOPS['coffee_shop_1'].fesh,
            price=100 + i,
            discount=50,
            ts=1000 + i,
        )
        for i, m in enumerate(MODELS['coffee'])
    ],
    'coffee_shop_2': [
        Offer(
            cpa=Offer.CPA_NO if i == 1 else Offer.CPA_REAL,  # Second offer as CPC
            title=m.title,
            hid=m.hid,
            hyperid=m.hid if i > 0 else None,  # First offer without model.
            fesh=SHOPS['coffee_shop_2'].fesh,
            price=100 + i,
            discount=50,
            ts=1100 + i,
        )
        for i, m in enumerate(MODELS['coffee'])
    ],
    'grocery_shop_1': [
        Offer(
            cpa=Offer.CPA_NO if i == 1 else Offer.CPA_REAL,  # Second offer as CPC
            title=m.title,
            hid=m.hid,
            hyperid=m.hid if i > 0 else None,  # First offer without model.
            fesh=SHOPS['grocery_shop_1'].fesh,
            price=100 + i,
            discount=50,
            ts=1200 + i,
        )
        for i, m in enumerate(MODELS['chips'])
    ],
    'long_name_shop': [
        Offer(
            cpa=Offer.CPA_REAL,
            title=m.title,
            hid=m.hid,
            hyperid=m.hyper,
            fesh=SHOPS['long_name_shop'].fesh,
            price=100 + i,
            discount=50,
            ts=1300 + i,
        )
        for i, m in enumerate(MODELS['long name models'])
    ],
}

EXPECTED_COMMON_CATEGORY_BY_SHOP = {
    SHOPS["electronics_shop_1"].fesh: CATEGORIES['electronics'],
    SHOPS["electronics_shop_2"].fesh: CATEGORIES['accessories'],
    SHOPS["coffee_shop_1"].fesh: CATEGORIES['drinks'],
    SHOPS["coffee_shop_2"].fesh: CATEGORIES['drinks'],
    SHOPS["long_name_shop"].fesh: CATEGORIES['long name category'],
}


def slugify(val):
    return val.lower().replace(' ', '-')


class T(TestCase):
    @classmethod
    def prepare(cls):
        # Build shops.
        cls.index.shops += SHOPS.values()

        # Build categories.
        cls.index.hypertree += [CATEGORIES['root']]
        cls.index.navtree += [
            NavCategory(
                nid=i.hid,
                hid=i.hid,
                name=i.name,
            )
            for i in CATEGORIES.values()
        ]

        cls.index.model_groups += MODEL_GROUPS

        # Build models.
        cls.index.models += chain(*MODELS.values())

        # Build offers.

        cls.index.offers += chain(*OFFERS_BY_SHOP.values())
        for name in OFFERS_BY_SHOP:
            for offer in OFFERS_BY_SHOP[name]:
                ts = offer.ts
                cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(RELEVANCES[ts % 2])

    def test_sins_offers_wizard(self):
        requests = [
            ('iphone', OFFERS_BY_SHOP['electronics_shop_1'] + OFFERS_BY_SHOP['electronics_shop_2']),
            ('coffee', OFFERS_BY_SHOP['coffee_shop_1'] + OFFERS_BY_SHOP['coffee_shop_2']),
        ]
        WPRID = 'some-wprid-value'

        for text, expected_offers in requests:
            request = (
                'place=parallel'
                '&text={text}'
                # Experiment is turned on globally in report.py
                # '&rearr-factors=market_enable_sins_offers_wizard=1;'
                '&wprid={wprid}'
            ).format(wprid=WPRID, text=text)

            response = self.report.request_bs_pb(request)

            expected_showcase_items = []
            for offer in expected_offers:
                if offer.ts:
                    relevance = RELEVANCES[offer.ts % 2]
                else:
                    relevance = RELEVANCE_DEFAULT

                # Expect only cpa offers.
                if not offer.is_cpa():
                    continue
                item_url = "market.yandex.ru/offer/" + offer.waremd5
                if offer.hyperid != 'None' and offer.hyperid != str(MODEL_GROUP_HYPERID):
                    item_url = "market.yandex.ru/product--{model_slug}/{model_id}".format(
                        model_slug=slugify(offer.title),
                        model_id=offer.hyperid,
                    )
                hid = offer.category
                item_url += "?businessId={bsid}&clid=962&hid={hid}&wprid={wprid}".format(
                    bsid=offer.business_id(),
                    wprid=WPRID,
                    hid=hid,
                )
                offer_shop = SHOP_BY_BSID[offer.business_id()]
                common_cat = EXPECTED_COMMON_CATEGORY_BY_SHOP[offer.business_id()]
                common_cat_url = '//market.yandex.ru/catalog--{slug}/{nid}/list?businessId={bsid}&clid=963&text={text}&wprid={wprid}'.format(
                    slug=slugify(common_cat.name),
                    nid=common_cat.nids[0],
                    bsid=offer.business_id(),
                    text=text,
                    wprid=WPRID,
                )
                common_cat_touch_url = '//m.market.yandex.ru/catalog--{slug}/{nid}/list?businessId={bsid}&clid=963&text={text}&wprid={wprid}'.format(
                    slug=slugify(common_cat.name),
                    nid=common_cat.nids[0],
                    bsid=offer.business_id(),
                    text=text,
                    wprid=WPRID,
                )
                item = {
                    'offersCommonCategory': {
                        'name': common_cat.name,
                        'url': common_cat_url,
                        'urlTouch': common_cat_touch_url,
                    },
                    'business': {
                        'grades_count': 0,
                        'id': offer.business_id(),
                        'name': offer_shop.name,
                        'logo': {
                            'extension': 'PNG',
                            'width': 28,
                            'height': 28,
                            'retina_url': offer_shop._Shop__shop_logo_retina_url,
                            'url': offer_shop._Shop__shop_logo_url,
                        },
                        "rating": "3.2",
                    },
                    "price": {"currency": "RUR", "priceMin": str(offer.price), "type": "min"},
                    "discount": {
                        "currency": "",
                        "oldprice": str(int(offer.price_old)),
                        "percent": str(offer._Offer__discount),
                    },
                    "relevance": "{}".format(relevance),
                    "delivery": {},
                    "title": {
                        "text": {"__hl": {"raw": True, "text": offer.title}},
                        "offercardUrl": "",
                        "urlForCounter": "",
                        "url": '//' + item_url,
                        "urlTouch": '//m.' + item_url,
                    },
                    "wareMd5": offer.waremd5,
                }
                if offer.hyperid != 'None' and offer.hyperid != str(MODEL_GROUP_HYPERID):
                    item['modelId'] = str(offer.hyperid)
                    item['pictures'] = [
                        '//mdata.yandex.net/2hq',
                        '//mdata.yandex.net/2hq',
                    ]
                expected_showcase_items.append(item)

            self.assertFragmentIn(
                response,
                {
                    'market_sins_offers_wizard': {
                        'showcase': {
                            'items': expected_showcase_items,
                        }
                    }
                },
                allow_different_len=False,
            )

    def test_title_shortening(self):
        """Check title shorteting to MAX_TITLE_LENGTH or less"""
        # TODO: добавить тесты на различные варианты строк, включая строки без пробелов
        request = (
            'place=parallel'
            '&text=very long name'
            # Experiment is turned on globally in report.py
            # '&rearr-factors=market_enable_sins_offers_wizard=1;'
            '&wprid=some-wprid-value'
        )
        response = self.report.request_bs_pb(request)
        expected_offers = OFFERS_BY_SHOP['long_name_shop']
        _ = EXPECTED_COMMON_CATEGORY_BY_SHOP[expected_offers[0].business_id()]
        wizards = response.extract_wizards()
        showcase_items = wizards[0]["market_sins_offers_wizard"][u"showcase"][u"items"]

        for item in showcase_items:
            common_category_title = item[u'offersCommonCategory'][u'name']
            self.assertTrue(len(common_category_title) <= MAX_TITLE_LENGTH)
            offer_title = item[u'title'][u'text'][u'__hl'][u'text']
            self.assertTrue(len(offer_title) <= MAX_TITLE_LENGTH)

    def test_relevance_threshold(self):
        _ = (RELEVANCE_LESS + RELEVANCE_MORE) / 2

        f_request = (
            'place=parallel'
            '&text=iphone'
            # '&debug=1&trace_wizard=2'
            '&rearr-factors=market_sins_relevance_threshold%3D{};'
            '&wprid=some-wprid-value'
        )
        request = f_request.format(int((RELEVANCE_LESS + RELEVANCE_MORE) / 2 * 100))
        response = self.report.request_bs_pb(request)
        # print(response.get_trace_wizard())
        wizards = response.extract_wizards()
        showcase_items = wizards[0]["market_sins_offers_wizard"][u"showcase"][u"items"]
        item_count_more = len(showcase_items)

        request = f_request.format(int(RELEVANCE_LESS / 2 * 100))
        response = self.report.request_bs_pb(request)
        wizards = response.extract_wizards()
        showcase_items = wizards[0]["market_sins_offers_wizard"][u"showcase"][u"items"]
        item_count_less = len(showcase_items)

        self.assertTrue(item_count_more < item_count_less)


if __name__ == '__main__':
    main()
