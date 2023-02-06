#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Book,
    DeliveryOption,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    Model,
    NavCategory,
    NewShopRating,
    Offer,
    Picture,
    Region,
    RegionalModel,
    Shop,
    VCluster,
)
from core.testcase import TestCase, main
from core.matcher import LikeUrl, Absent, NoKey
from core.types.picture import thumbnails_config


class T(TestCase):
    @classmethod
    def prepare_vcluster(cls):
        cls.index.hypertree += [
            HyperCategory(hid=10, name='Coats', visual=True),
        ]

        cls.index.navtree += [
            NavCategory(nid=15, hid=10),
        ]

        cls.index.regiontree += [Region(rid=213, name='Москва', genitive='Москвы', locative='Москве', preposition='в')]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
        ]

        cls.index.gltypes += [
            GLType(param_id=600100, hid=10, position=1, cluster_filter=1, gltype=GLType.BOOL, name=u'Капюшон'),
            GLType(
                param_id=600101,
                hid=10,
                position=2,
                cluster_filter=1,
                gltype=GLType.ENUM,
                name=u'Сезон',
                values=[GLValue(value_id=1010, text='весна')],
            ),
            GLType(
                param_id=600102,
                hid=10,
                position=3,
                cluster_filter=1,
                gltype=GLType.ENUM,
                name=u'Цвет',
                subtype='color',
                values=[
                    GLValue(2010, code='#FF0000', tag='red', text='красный'),
                    GLValue(2011, code='#00FF00', tag='green', text='зелёный'),
                ],
            ),
            GLType(
                param_id=600103,
                hid=10,
                position=4,
                cluster_filter=1,
                gltype=GLType.ENUM,
                subtype='size',
                name=u'Размер',
                unit_param_id=600104,
                values=[
                    GLValue(value_id=1, text='42', unit_value_id=1),
                    GLValue(value_id=2, text='44', unit_value_id=1),
                    GLValue(value_id=3, text='46', unit_value_id=1),
                    GLValue(value_id=4, text='36', unit_value_id=2),
                    GLValue(value_id=5, text='38', unit_value_id=2),
                    GLValue(value_id=6, text='40', unit_value_id=2),
                ],
            ),
            GLType(
                param_id=600104,
                hid=10,
                position=None,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='RU', default=True), GLValue(value_id=2, text='EU')],
            ),
        ]

        cls.index.vclusters += [
            VCluster(
                vclusterid=1000000001,
                title='Coat Quelle',
                hid=10,
                pictures=[
                    Picture(
                        picture_id='tiSs0dxrqgAIXqUICMZUOQ',
                        width=180,
                        height=240,
                        thumb_mask=thumbnails_config.get_mask_by_names(
                            ['100x100', '90x120', '55x70', '180x240', '120x160']
                        ),
                        group_id=1234,
                    )
                ],
                glparams=[
                    GLParam(param_id=600100, value=1),  # с капюшоном
                    GLParam(param_id=600101, value=1010),  # сезон: весна
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=1,
                title='Coat Quelle',
                vclusterid=1000000001,
                price=10500,
                glparams=[GLParam(param_id=600102, value=2010), GLParam(param_id=600103, value=1)],  # red  # размер 42
            ),
            Offer(
                fesh=1,
                title='Coat Quelle',
                vclusterid=1000000001,
                price=11000,
                glparams=[
                    GLParam(param_id=600102, value=2011),  # green
                    GLParam(param_id=600103, value=3),  # размер 46
                ],
            ),
        ]

    def test_vcluster(self):
        """Проверяем корректность выдачи для кластера"""
        response = self.report.request_bs_pb('place=model_wizard_by_id&hyperid=1000000001&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "categoryId": 10,
                    "category_name": "Coats",
                    "url": LikeUrl.of("//market.yandex.ru/product--coat-quelle/1000000001?clid=502&hid=10&nid=15"),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--coat-quelle/1000000001?clid=704&hid=10&nid=15"
                    ),
                    "title": {"__hl": {"text": "Coat Quelle", "raw": True}},
                    "text": [{"__hl": {"text": "Капюшон: да; Сезон: весна; Размер: 42, 46 (RU)", "raw": True}}],
                    "offersUrl": LikeUrl.of(
                        "//market.yandex.ru/product--coat-quelle/1000000001/offers?clid=502&grhow=shop&hid=10&hyperid=1000000001&nid=15"
                    ),
                    "offersUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--coat-quelle/1000000001?clid=704&grhow=shop&hid=10&nid=15"
                    ),
                    "picture": LikeUrl.of(
                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_tiSs0dxrqgAIXqUICMZUOQ/100x100"
                    ),
                    "pictureTouch": LikeUrl.of(
                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_tiSs0dxrqgAIXqUICMZUOQ/90x120"
                    ),
                    "pictureTouchHd": LikeUrl.of(
                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_tiSs0dxrqgAIXqUICMZUOQ/180x240"
                    ),
                    "price": {"priceMin": "10500", "priceMax": "11000", "currency": "RUR"},
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru?clid=502"),
                        }
                    ],
                    "sitelinks": [
                        {
                            "text": "prices",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/product--coat-quelle/1000000001/offers?clid=502&grhow=shop&hid=10&hyperid=1000000001&nid=15"
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--coat-quelle/1000000001?clid=704&grhow=shop&hid=10&nid=15"
                            ),
                        },
                    ],
                }
            },
        )

    @classmethod
    def prepare_model(cls):
        cls.index.hypertree += [
            HyperCategory(hid=11, name='Shoes'),
        ]

        cls.index.navtree += [
            NavCategory(nid=16, hid=11),
        ]

        cls.index.models += [
            Model(
                hyperid=1,
                title='Shoes Dolomite',
                hid=11,
                description='Cool Shoes Dolomite',
                picinfo='//avatars.mds.yandex.net/get-marketpic/218271/market_ZAPrHu7Q2KBotHamg7JoHQ/orig#100#200',
                add_picinfo='//avatars.mds.yandex.net/get-marketpic/218271/market_ZAPrHu7Q2KBotHamg7JoAA/orig#50#50',
            ),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=1, rids=[213], offers=100, geo_offers=20),
        ]

        cls.index.offers += [
            Offer(fesh=1, title='Shoes Dolomite', hyperid=1, price=10500),
            Offer(fesh=1, title='Shoes Dolomite', hyperid=1, price=11000),
        ]

    def test_model(self):
        """Проверяем корректность выдачи для модели"""
        response = self.report.request_bs_pb('place=model_wizard_by_id&hyperid=1&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "categoryId": 11,
                    "category_name": "Shoes",
                    "url": LikeUrl.of("//market.yandex.ru/product--shoes-dolomite/1?clid=502&hid=11&nid=16"),
                    "urlTouch": LikeUrl.of("//m.market.yandex.ru/product--shoes-dolomite/1?clid=704&hid=11&nid=16"),
                    "title": {"__hl": {"text": "Shoes Dolomite", "raw": True}},
                    "text": [{"__hl": {"text": "Cool Shoes Dolomite", "raw": True}}],
                    "offersUrl": LikeUrl.of(
                        "//market.yandex.ru/product--shoes-dolomite/1/offers?clid=502&grhow=shop&hid=11&hyperid=1&nid=16"
                    ),
                    "offersUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--shoes-dolomite/1?clid=704&grhow=shop&hid=11&nid=16"
                    ),
                    "picture": LikeUrl.of(
                        "//avatars.mds.yandex.net/get-marketpic/218271/market_ZAPrHu7Q2KBotHamg7JoHQ/2hq"
                    ),
                    "pictureTouch": LikeUrl.of(
                        "//avatars.mds.yandex.net/get-marketpic/218271/market_ZAPrHu7Q2KBotHamg7JoHQ/7hq"
                    ),
                    "pictureTouchHd": LikeUrl.of(
                        "//avatars.mds.yandex.net/get-marketpic/218271/market_ZAPrHu7Q2KBotHamg7JoHQ/8hq"
                    ),
                    "price": {"priceMin": "10500", "priceMax": "11000", "currency": "RUR"},
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru?clid=502"),
                        }
                    ],
                    "sitelinks": [
                        {
                            "text": "prices",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/product--shoes-dolomite/1/offers?clid=502&grhow=shop&hid=11&hyperid=1&nid=16"
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--shoes-dolomite/1?clid=704&grhow=shop&hid=11&nid=16"
                            ),
                        },
                    ],
                }
            },
        )

    @classmethod
    def prepare_book(cls):
        cls.index.hypertree += [
            HyperCategory(hid=12, name='Books'),
        ]

        cls.index.navtree += [
            NavCategory(nid=17, hid=12),
        ]

        cls.index.books += [
            Book(
                hyperid=101,
                hid=12,
                author='Bruce G. Knuth',
                publisher='Dedal Press',
                title='Jewelry Book',
                isbn='978-5-902719-15-1',
                description='Cool Jewelry Book',
                picinfo='//avatars.mds.yandex.net/get-marketpic/223477/market_Y0YhSA3Zp541vFqR9HQQzQ/orig',
                picture=Picture(
                    picture_id='Y0YhSA3Zp541vFqR9HQQzQ',
                    width=500,
                    height=500,
                    thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100', '200x200', '300x300']),
                    group_id=1234,
                ),
            ),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=101, rids=[213], offers=100, geo_offers=20),
        ]

        cls.index.offers += [
            Offer(fesh=1, title='Jewelry Book', hyperid=101, price=10500),
            Offer(fesh=1, title='Jewelry Book', hyperid=101, price=11000),
        ]

    def test_book(self):
        """Проверяем корректность выдачи для книги"""
        response = self.report.request_bs_pb('place=model_wizard_by_id&hyperid=101&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "categoryId": 12,
                    "category_name": "Books",
                    "url": LikeUrl.of(
                        "//market.yandex.ru/product--bruce-g-knuth-jewelry-book/101?clid=502&hid=12&nid=17"
                    ),
                    "urlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--bruce-g-knuth-jewelry-book/101?clid=704&hid=12&nid=17"
                    ),
                    "title": {"__hl": {"text": "Bruce G. Knuth \"Jewelry Book\"", "raw": True}},
                    "text": [{"__hl": {"text": "Cool Jewelry Book", "raw": True}}],
                    "offersUrl": LikeUrl.of(
                        "//market.yandex.ru/product--bruce-g-knuth-jewelry-book/101/offers?clid=502&grhow=shop&hid=12&hyperid=101&nid=17"
                    ),
                    "offersUrlTouch": LikeUrl.of(
                        "//m.market.yandex.ru/product--bruce-g-knuth-jewelry-book/101?clid=704&grhow=shop&hid=12&nid=17"
                    ),
                    "picture": LikeUrl.of(
                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_Y0YhSA3Zp541vFqR9HQQzQ/100x100"
                    ),
                    "pictureTouch": LikeUrl.of(
                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_Y0YhSA3Zp541vFqR9HQQzQ/100x100"
                    ),
                    "pictureTouchHd": LikeUrl.of(
                        "//avatars.mdst.yandex.net/get-marketpic/1234/market_Y0YhSA3Zp541vFqR9HQQzQ/200x200"
                    ),
                    "price": {"priceMin": "10500", "priceMax": "11000", "currency": "RUR"},
                    "greenUrl": [
                        {
                            "url": LikeUrl.of("//market.yandex.ru?clid=502"),
                        }
                    ],
                    "categoryUrlTouch": LikeUrl.of("//m.market.yandex.ru/catalog?clid=704&hid=12&nid=17"),
                    "sitelinks": [
                        {
                            "text": "prices",
                            "url": LikeUrl.of(
                                "//market.yandex.ru/product--bruce-g-knuth-jewelry-book/101/offers?clid=502&grhow=shop&hid=12&hyperid=101&nid=17"
                            ),
                            "urlTouch": LikeUrl.of(
                                "//m.market.yandex.ru/product--bruce-g-knuth-jewelry-book/101?clid=704&grhow=shop&hid=12&nid=17"
                            ),
                        },
                    ],
                }
            },
        )

    @classmethod
    def prepare_empty_thumb_data(cls):
        cls.index.models += [
            Model(hyperid=2, picinfo='', add_picinfo=''),
        ]

    def test_empty_thumb(self):
        """Проверяем, что для модели с пустой картинкой
        репорт не падает"""
        response = self.report.request_bs_pb('place=model_wizard_by_id&hyperid=2&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "for_wiz": Absent(),
                    "pic_src": Absent(),
                    "pic_src-touch": Absent(),
                    "pic_src-hd-touch": Absent(),
                    "big_pic": Absent(),
                }
            },
        )

    @classmethod
    def prepare_model_without_offers(cls):
        cls.index.models += [
            Model(hyperid=3),
        ]

    def test_model_without_offers(self):
        """Проверяем корректность при отсутствии цены"""
        response = self.report.request_bs_pb('place=model_wizard_by_id&hyperid=3&rids=213')
        self.assertFragmentIn(response, {"market_model": {"price": NoKey("price")}})

    @classmethod
    def prepare_offer_info(cls):
        cls.index.models += [
            Model(hyperid=4, hid=201, title="Самокат Молния"),
        ]
        cls.index.shops += [
            Shop(
                fesh=2,
                name='Самокат',
                shop_grades_count=15,
                priority_region=213,
                url='samokat-molniya.ru',
                regions=[225],
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=4.0, rec_and_nonrec_pub_count=123),
            ),
        ]
        cls.index.offers += [
            Offer(
                title='Супер самокат Молния',
                hid=201,
                hyperid=4,
                fesh=2,
                cpa=Offer.CPA_REAL,
                fee=200,
                price=8000,
                delivery_options=[DeliveryOption(price=200, day_from=0, day_to=2, order_before=23)],
                url='https://www.samokat-molniya.ru/molniya_1',
                waremd5='wf9fCDXkniqrpGHVkgj-6w',
            ),
        ]

    def test_offer_info(self):
        """Проверяем корректность оферной выдачи внутри модельного"""
        response = self.report.request_bs_pb('place=model_wizard_by_id&hyperid=4&rids=213')
        self.assertFragmentIn(
            response,
            {
                "market_model": {
                    "showcase": {
                        "items": [
                            {
                                "thumb": {
                                    "source": "//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                                    "width": "100",
                                    "height": "100",
                                },
                                "price": {
                                    "currency": "RUR",
                                },
                                "shopRating": {
                                    "value": "4",
                                },
                                "greenUrl": {
                                    "text": "Самокат",
                                    "url": LikeUrl.of(
                                        "//market.yandex.ru/shop--samokat/2/reviews?cmid=wf9fCDXkniqrpGHVkgj-6w&clid=502&lr=213"
                                    ),
                                },
                                "gradeCount": 123,
                                "title": {"url": "https://www.samokat-molniya.ru/molniya_1"},
                                "offerId": "wf9fCDXkniqrpGHVkgj-6w",
                            }
                        ]
                    }
                }
            },
        )

    def test_no_model(self):
        """Проверяем корретную работу при отсутствии модели в индексе.
        https://st.yandex-team.ru/MARKETOUT-18247
        """
        response = self.report.request_bs_pb('place=model_wizard_by_id&hyperid=999999999&rids=213')
        self.assertFragmentNotIn(response, {"market_model": {}})


if __name__ == '__main__':
    main()
