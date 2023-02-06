#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    BookingAvailability,
    BusinessCalendar,
    ClickType,
    CpaCategory,
    CpaCategoryType,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    DynamicPriceControlData,
    ExchangeRate,
    GLParam,
    GLType,
    GLValue,
    MarketSku,
    Model,
    NavCategory,
    NewShopRating,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Picture,
    Region,
    Shop,
    ShopOperationalRating,
    TopQueries,
    Vendor,
    VendorLogo,
)
from core.types.currency import Currencies
from core.matcher import Absent, Contains, NoKey
from core.types.taxes import Tax
from core.types.parallel_import_warranty import ParallelImportWarranty
from core.types.picture import thumbnails_config
from core.cpc import Cpc

import re
import base64

from core.report import DefaultFlags


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.gltypes += [
            GLType(param_id=1, hid=3, gltype=GLType.ENUM, values=[1, 2], unit_name="Length", cluster_filter=True),
            GLType(param_id=2, hid=3, gltype=GLType.ENUM, xslname="vendor"),
        ]
        cls.index.outlets += [
            Outlet(fesh=1, region=213, point_type=Outlet.FOR_PICKUP, point_id=1001),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[99],
                options=[PickupOption(outlet_id=1001)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]
        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                name='test_shop_1',
                currency=Currency.RUR,
                new_shop_rating=NewShopRating(new_rating=3.7, rec_and_nonrec_pub_count=123),
                pickup_buckets=[5001],
            ),
            Shop(fesh=2, priority_region=213, name='test_shop_2', currency=Currency.RUR),
        ]
        # Виртуальный магазин для синих оферов
        cls.index.shops += [
            Shop(
                fesh=101,
                datafeed_id=101,
                priority_region=213,
                name='virtual_blue_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            )
        ]
        # Виртуальный магазин для красных оферов
        cls.index.shops += [
            Shop(
                fesh=102,
                datafeed_id=102,
                priority_region=213,
                name='virtual_red_shop',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_RED,
            )
        ]
        cls.index.business_calendars += [
            BusinessCalendar(region=225, holidays=[]),
        ]
        cls.index.navtree += [
            NavCategory(nid=4, hid=3),
        ]
        cls.index.offers += [
            Offer(
                adult=True,
                delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2)],
                descr='test_description',
                fesh=1,
                hid=3,
                manufacturer_warranty=True,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                price=100,
                title='test_title',
                vendor_id=2,
                waremd5='2b0-iAnHLZST2Ekoq4xElr',
                glparams=[
                    GLParam(param_id=1, value=1),
                    GLParam(param_id=2, value=16644882),
                ],
                feedid=11000,
                offerid="OfFeR1",
                feed_category_id="FeedCategorY",
            ),
            Offer(
                adult=True,
                delivery_options=[DeliveryOption(price=100)],
                descr='test_description',
                fesh=1,
                hid=3,
                manufacturer_warranty=True,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                price=100,
                title='test_title',
                vendor_id=2,
                waremd5='09lEaAKkQll1XTjm0WPoIA',
                glparams=[
                    GLParam(param_id=1, value=1),
                ],
            ),
            Offer(
                adult=True,
                delivery_options=[DeliveryOption(price=100)],
                descr='test_description',
                fesh=2,
                hid=3,
                manufacturer_warranty=True,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                price=100,
                title='test_title',
                vendor_id=2,
                waremd5='RcSMzi4tf73qGvxRx8atJg',
                feedid=100501,
                offerid=19,
                glparams=[
                    GLParam(param_id=1, value=1),
                ],
            ),
            Offer(
                adult=True,
                delivery_options=[DeliveryOption(price=100)],
                descr='test_description',
                fesh=2,
                hid=3,
                manufacturer_warranty=True,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                price=100,
                title='test_title',
                vendor_id=2,
                waremd5='ZRK9Q9nKpuAsmQsKgmUtyg',
                feedid=100500,
                offerid=17,
                glparams=[
                    GLParam(param_id=1, value=1),
                ],
            ),
            Offer(
                descr='test_description',
                fesh=2,
                hid=3,
                seller_warranty='P1Y2M10DT2H30M',
                price=150,
                title='test_title',
                vendor_id=2,
                waremd5='ZRK1Q_valid_mQsKgmUtyg',
                feedid=100500,
                offerid=25,
            ),
            Offer(
                descr='test_description',
                fesh=2,
                hid=3,
                seller_warranty='true',
                price=150,
                title='test_title',
                vendor_id=2,
                waremd5='ZRK2Q_invalid_sKgmUtyg',
                feedid=100500,
                offerid=35,
            ),
            Offer(
                descr='test_description',
                fesh=2,
                hid=3,
                seller_warranty='',
                price=150,
                title='test_title',
                vendor_id=2,
                waremd5='ZRK3Q_empty_mQsKgmUtyg',
                feedid=100500,
                offerid=45,
            ),
            Offer(
                descr='test_description',
                fesh=2,
                hid=3,
                parallel_imported=True,
                price=150,
                title='test_title',
                vendor_id=2,
                waremd5='ZRK1Q_parallel_KgmUtyg',
                feedid=100500,
                offerid=50,
            ),
            Offer(
                descr='test_description',
                fesh=2,
                hid=1111111,
                parallel_imported=True,
                price=150,
                title='test_title',
                vendor_id=2222222,
                waremd5='ZR_parallel_warranty_g',
                feedid=100500,
                offerid=55,
            ),
            Offer(
                descr='test_description',
                fesh=2,
                hid=3,
                parallel_imported=False,
                price=150,
                title='test_title',
                vendor_id=2,
                waremd5='ZRK1Q_not_parallel_tyg',
                feedid=100500,
                offerid=65,
            ),
        ]
        cls.index.vendors += [
            Vendor(
                vendor_id=2,
                name='VendorName',
                webpage_recommended_shops='http://www.beko.ru/recommended-online-stores.html',
                description='VendorDescription',
                logos=[VendorLogo(url='//mdata.yandex.net/i?path=b0726220734_img_id5949902134120952009.png')],
                website="http://www.beko.ru/",
            )
        ]

    @classmethod
    def prepare_api_offer_info(cls):
        cls.index.shops += [Shop(fesh=10, priority_region=213)]

        cls.index.cpa_categories += [
            CpaCategory(hid=777, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION)
        ]

        cls.index.offers += [
            Offer(
                title='The Offer',
                fesh=10,
                hid=777,
                hyperid=100500,
                waremd5='OfferToCheckApiInfo123',
                price=20000,
                bid=800,
                popular_queries_all=[
                    TopQueries('Киянка резиновая', 10, 1, 1.5),
                    TopQueries('Молоток резиновый', 10, 1, 1.5),
                ],
                popular_queries_offer=[
                    TopQueries('Удобный киянка', 10, 1, 1.5),
                    TopQueries('Киянка купить дешево', 10, 1, 1.5),
                ],
            )
        ]

    def check_offerinfo_format(self, rs):
        response = self.report.request_json(
            'place=offerinfo'
            '&offerid=2b0-iAnHLZST2Ekoq4xElr'
            '&rids=213'
            '&cpc=2oXU--l8g29drjlI6MsbNV4dt7Jx--fqnTMvTee5T_RRQL_Lkid4pEdfE4SeNG-DNvoI_C52LmU,'
            '&rs={}'
            '&hid=3'
            '&nid=4'
            '&show-urls=cpa,external,phone,showPhone'
            '&regset=1'
            '&pp=42'
            '&test-buckets=4,5,6'
            '&subreqid=ignored_subreqid'.format(rs)
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "adult": True,
                    "restrictionAge18": True,
                    "salesDetected": False,
                    "shops": 1,
                    "isFuzzySearch": Absent(),
                    "isParametricSearch": False,
                    "results": [
                        {
                            "entity": "offer",
                            "vendor": {
                                "entity": "vendor",
                                "id": 2,
                                "name": "VendorName",
                                "description": "VendorDescription",
                                "logo": {
                                    "entity": "picture",
                                    "url": "//mdata.yandex.net/i?path=b0726220734_img_id5949902134120952009.png",
                                },
                                "webpageRecommendedShops": "http://www.beko.ru/recommended-online-stores.html",
                                "website": "http://www.beko.ru/",
                            },
                            "titles": {"raw": "test_title", "highlighted": [{"value": "test_title"}]},
                            "description": "test_description",
                            "eligibleForBookingInUserRegion": False,
                            "categories": [
                                {
                                    "entity": "category",
                                    "id": 3,
                                }
                            ],
                            "navnodes": [
                                {
                                    "entity": "navnode",
                                    "id": 4,
                                }
                            ],
                            "filters": [
                                {
                                    "id": "1",
                                    "type": "enum",
                                    "subType": "",
                                    "kind": 2,
                                    "unit": "Length",
                                    "position": 1,
                                    "noffers": 1,
                                    "values": [{"initialFound": 1, "found": 1, "id": "1"}],
                                }
                            ],
                            "pictures": [
                                {
                                    "entity": "picture",
                                    "thumbnails": [
                                        {
                                            "containerWidth": 200,
                                            "containerHeight": 200,
                                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/200x200",
                                            "width": 200,
                                            "height": 200,
                                        }
                                    ],
                                }
                            ],
                            "delivery": {
                                "shopPriorityRegion": {
                                    "entity": "region",
                                    "id": 213,
                                },
                                "isPriorityRegion": True,
                                "isCountrywide": True,
                                "isAvailable": True,
                                "hasPickup": True,
                            },
                            "shop": {
                                "entity": "shop",
                                "id": 1,
                                "name": "test_shop_1",
                                "outletsCount": 1,
                                "feed": {"id": "11000", "offerId": "OfFeR1", "categoryId": "FeedCategorY"},
                                "ratingToShow": 3.7,
                                "overallGradesCount": 123,
                            },
                            "wareId": "2b0-iAnHLZST2Ekoq4xElg",
                            "offerColor": "white",
                            "prices": {"currency": "RUR", "value": "100", "rawValue": "100"},
                            "manufacturer": {"entity": "manufacturer", "warranty": True},
                        }
                    ],
                },
                "intents": [],
                "sorts": [],
            },
            preserve_order=True,
        )

    def test_hide_invalid_vendor(self):
        response = self.report.request_json('place=offerinfo' '&offerid=2b0-iAnHLZST2Ekoq4xElr' '&rids=213' '&regset=1')
        self.assertFragmentNotIn(response, {"filters": [{"id": "2"}]})
        self.assertFragmentIn(response, {"filters": [{"id": "1"}]})

    def test_place(self):
        # place=offerinfo&offerid=2b0-iAnHLZST2Ekoq4xElr&rids=213&cpc=2oXU--l8g29drjlI6MsbNV4dt7Jx--fqnTMvTee5T_RRQL_Lkid4pEdfE4SeNG-DNvoI_C52LmU,&pp=18&hid=3&nid=4&show-urls=cpa,external&regset=1&pp=42&test-buckets=1,2,3&subreqid=test_subreqid
        # The hardcoded rs value represents the following:
        # python -c 'import base64, sys, zlib; sys.stdout.write(zlib.decompress(base64.urlsafe_b64decode("eJzjEBJiNdQx0jGWYFTiLUktLokvLk0qSi3MTAEAQp0G5g==")))' | protoc --decode_raw
        # 1: 18
        # 2: "1,2,3"
        # 3: 1
        # 4: "test_subreqid"
        # MarketSearch::TOfferCardReportState
        reportState = 'eJyzkuIQEmI11DHSMZZgVOItSS0uiS8uTSpKLcxMAQBLtQc6'
        self.check_offerinfo_format(reportState)
        self.show_log.expect(
            ware_md5='2b0-iAnHLZST2Ekoq4xElg',
            pp=18,
            pp_oi=1,
            position=1,
            test_buckets='1,2,3',
            subrequest_id='test_subreqid',
            click_price=15,
            bid=20,
            min_bid=10,
        ).times(1)
        self.show_log.expect(
            ware_md5='2b0-iAnHLZST2Ekoq4xElg',
            pp=18,
            pp_oi=6,
            position=1,
            test_buckets='1,2,3',
            subrequest_id='test_subreqid',
        ).times(2)
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            ware_md5='2b0-iAnHLZST2Ekoq4xElg',
            pp=18,
            pp_oi=1,
            position=1,
            test_buckets='1,2,3',
            sub_request_id='test_subreqid',
            cp=15,
            cb=20,
            min_bid=10,
        ).times(1)
        self.click_log.expect(
            clicktype=ClickType.PHONE,
            ware_md5='2b0-iAnHLZST2Ekoq4xElg',
            pp=18,
            pp_oi=6,
            position=1,
            test_buckets='1,2,3',
            sub_request_id='test_subreqid',
            cp=15,
            cb=20,
            min_bid=10,
        ).times(1)
        self.click_log.expect(
            clicktype=ClickType.SHOW_PHONE,
            ware_md5='2b0-iAnHLZST2Ekoq4xElg',
            pp=18,
            pp_oi=6,
            position=1,
            test_buckets='1,2,3',
            sub_request_id='test_subreqid',
            cp=15,
            cb=20,
            min_bid=10,
        ).times(1)

    @classmethod
    def prepare_picture_limit(cls):
        cls.index.offers += [
            Offer(
                hid=4, fesh=2, title="boots", waremd5='ZRK9Q9nKpuAsmQsKgmUtyf', pictures=[Picture() for _ in range(25)]
            )
        ]

    def test_picture_limit(self):
        url = 'place=offerinfo&offerid=ZRK9Q9nKpuAsmQsKgmUtyf&rids=213&show-urls=cpa,external&regset=1'
        # test without given limit, offer has 25 pictures, expect use of internal limit (20)
        response = self.report.request_json(url)
        self.assertEqual(response.count({'entity': 'picture'}), 20)

        # test with given limit, expect cutting picture count to limit
        response = self.report.request_json(url + '&max_pictures=10')
        self.assertEqual(response.count({'entity': 'picture'}), 10)

    def test_empty_request(self):
        # place=offerinfo&offerid=2b0-xxxxxxxxxxxxxxxxxx&rids=213&cpc=2oXU--l8g29drjlI6MsbNV4dt7Jx--fqnTMvTee5T_RRQL_Lkid4pEdfE4SeNG-DNvoI_C52LmU,&pp=18&hid=3&nid=4&show-urls=cpa,external&regset=1
        response = self.report.request_json(
            'place=offerinfo'
            '&offerid=2b0-xxxxxxxxxxxxxxxxxx'
            '&rids=213'
            '&cpc=2oXU--l8g29drjlI6MsbNV4dt7Jx--fqnTMvTee5T_RRQL_Lkid4pEdfE4SeNG-DNvoI_C52LmU,'
            '&rs=eJyzkuIQEmI11DHSMZZgVOItSS0uiS8uTSpKLcxMAQBLtQc6'
            '&hid=3'
            '&nid=4'
            '&show-urls=cpa,external'
            '&regset=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                }
            },
            preserve_order=True,
        )

    def test_bad_request(self):
        # place=offerinfo&offerid=2b0-xxxxxxxxxxxxxxxxxx&rids=213&cpc=2oXU--l8g29drjlI6MsbNV4dt7Jx--fqnTMvTee5T_RRQL_Lkid4pEdfE4SeNG-DNvoI_C52LmU,&pp=18&hid=3&nid=4&show-urls=cpa,external&regset=1
        response = self.report.request_json('place=offerinfo')
        self.assertFragmentIn(
            response,
            {
                "error": {
                    "code": "INVALID_USER_CGI",
                    "message": "show-urls, rids, regset and (feed_shoffer_id, feed_shoffer_id_base64, offerid or market-sku) params are required",
                }
            },
        )
        self.error_log.expect(code=3043)

    def test_missing_pp(self):
        response = self.report.request_json(
            'place=offerinfo'
            '&offerid=2b0-iAnHLZST2Ekoq4xElr'
            '&rids=213'
            '&cpc=IGY_WwXibOnyvZqmqHCOr50wrmHJ4r9BmbL9r-5iSyQpbvr8nEwjZRF9xDyvlytE'
            '&hid=3'
            '&nid=4'
            '&show-urls=cpa,external,phone,showPhone'
            '&regset=1'
            '&ip=127.0.0.1',
            strict=False,
            add_defaults=DefaultFlags.BS_FORMAT,
        )
        self.error_log.expect('Some client has not set PP value. Find and punish him violently').once()
        self.assertEqual(500, response.code)

    def test_bad_rs(self):
        # non base64 rs
        response = self.report.request_json(
            'place=offerinfo'
            '&offerid=ZRK9Q9nKpuAsmQsKgmUtyg'
            '&rids=213'
            '&cpc=2oXU--l8g29drjlI6MsbNV4dt7Jx--fqnTMvTee5T_RRQL_Lkid4pEdfE4SeNG-DNvoI_C52LmU,'
            '&rs=B@D_RS'
            '&hid=3'
            '&nid=4'
            '&show-urls=cpa,external,phone,showPhone'
            '&regset=1'
        )
        self.assertFragmentIn(response, {"entity": "offer"})

        # bad zlib inside of base64 (incorrect header check)
        response = self.report.request_json(
            'place=offerinfo'
            '&offerid=ZRK9Q9nKpuAsmQsKgmUtyg'
            '&rids=213'
            '&cpc=2oXU--l8g29drjlI6MsbNV4dt7Jx--fqnTMvTee5T_RRQL_Lkid4pEdfE4SeNG-DNvoI_C52LmU,'
            '&rs=ejzjybcsmty0mjftmdaxtzy2mdawbllmdcqylrgnaum-bii%2c'
            '&hid=3'
            '&nid=4'
            '&show-urls=cpa,external,phone,showPhone'
            '&regset=1'
        )
        self.assertFragmentIn(response, {"entity": "offer"})
        self.error_log.expect(code=3630, message=Contains('can not decode report state from')).times(4)

    def test_offers_filter(self):
        """
        Регрессионный тест для MARKETOUT-11087 с фильтром по идентификаторам оферов
        Должны поддерживаться оба формата: перечисление через запятую и повторение параметра запроса
        """

        unknown_offer_id = '8Y1FUFggY25_YardWN-N4g'

        # через запятую
        response = self.report.request_json(
            'place=offerinfo&offerid=ZRK9Q9nKpuAsmQsKgmUtyg,{unknown_offer_id}&rids=213&show-urls=cpa,external,phone,showPhone&regset=1'.format(
                unknown_offer_id=unknown_offer_id
            )
        )
        self.assertFragmentIn(response, {"search": {"total": 1}})
        response = self.report.request_json(
            'place=offerinfo&offerid={unknown_offer_id},ZRK9Q9nKpuAsmQsKgmUtyg&rids=213&show-urls=cpa,external,phone,showPhone&regset=1'.format(
                unknown_offer_id=unknown_offer_id
            )
        )
        self.assertFragmentIn(response, {"search": {"total": 1}})

        # повторение парамметра
        response = self.report.request_json(
            'place=offerinfo&offerid=ZRK9Q9nKpuAsmQsKgmUtyg&offerid={unknown_offer_id}&rids=213&show-urls=cpa,external,phone,showPhone&regset=1'.format(
                unknown_offer_id=unknown_offer_id
            )
        )
        self.assertFragmentIn(response, {"search": {"total": 1}})

        response = self.report.request_json(
            'place=offerinfo&offerid={unknown_offer_id}&offerid=ZRK9Q9nKpuAsmQsKgmUtyg&rids=213&show-urls=cpa,external,phone,showPhone&regset=1'.format(
                unknown_offer_id=unknown_offer_id
            )
        )
        self.assertFragmentIn(response, {"search": {"total": 1}})

        # исключающий фильтр в формате с повторением
        response = self.report.request_json(
            'place=offerinfo&offerid={unknown_offer_id}&offerid=-ZRK9Q9nKpuAsmQsKgmUtyg&rids=213&show-urls=cpa,external,phone,showPhone&regset=1'.format(
                unknown_offer_id=unknown_offer_id
            )
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})

        # исключающий фильтр в формате перечисления через запятую
        response = self.report.request_json(
            'place=offerinfo&offerid={unknown_offer_id},-ZRK9Q9nKpuAsmQsKgmUtyg&rids=213&show-urls=cpa,external,phone,showPhone&regset=1'.format(
                unknown_offer_id=unknown_offer_id
            )
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})

        # Пустая выдача, если не было ни одного валидного офера
        response = self.report.request_json(
            'place=offerinfo&offerid={unknown_offer_id}&rids=213&show-urls=cpa,external,phone,showPhone&regset=1'.format(
                unknown_offer_id='too_short'
            )
        )
        self.assertFragmentIn(response, {"search": {"total": 0}})

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='1').times(4)
        self.access_log.expect(total_renderable='0').times(2)

    def test_api_offerinfo_output(self):
        """
        Тест проверяет наличие в выдаче данных по предложению: текущие и минимальные ставки, model-id
        """
        response = self.report.request_xml('place=api_offerinfo&offerid=OfferToCheckApiInfo123&rids=213')
        self.assertFragmentIn(
            response,
            '''<offer>
                <model-id>100500</model-id>
                <title>The Offer</title>
                <minimal-bids bid="26"/>
                <bids bid="800"/>
                <top-queries-requests>
                    <all-queries>
                       <query-item>Киянка резиновая</query-item>
                       <query-item>Молоток резиновый</query-item>
                    </all-queries>
                    <offer-queries>
                       <query-item>Удобный киянка</query-item>
                       <query-item>Киянка купить дешево</query-item>
                    </offer-queries>
                </top-queries-requests>
               </offer>''',
        )

    def test_api_offerinfo_title_search(self):
        """
        Тест проверяет наличие в выдаче данных по предложению: текущие и минимальные ставки
        """
        for title_param in ('text2=yx_title="The Offer"', 'title=The Offer'):
            response = self.report.request_xml(
                'place=api_offerinfo&{title_param}&rids=213&fesh=10&debug=1'.format(title_param=title_param)
            )
            self.assertFragmentIn(
                response,
                '''
                <offer>
                    <minimal-bids bid="26"/>
                </offer>
                ''',
            )

            self.assertFragmentIn(
                response,
                r'''
                <param name="text">
                    <value>yx_hashtitle:"24538de17db77b7e978fad957988ca27"(::1124746168)? is_b2c:"1" (..) \(yx_ds_id:"10" \| bsid:"10"\)</value>
                </param>
                ''',
                use_regex=True,
            )

    def test_bulk_offers(self):
        '''
        MARKETOUT-11400 Множественный запрос offer_id
        Проверяется, что на выдаче place=offerinfo будут выведены все запрошеныt offerid, а не первый релевантный.
        Запрашиваются два офера с offerid (waremd5) = 09lEaAKkQll1XTjm0WPoIA и ZRK9Q9nKpuAsmQsKgmUtyg.
        Запросы проходят как через запятую, так и в разных параметрах offerid.
        На выходе ожидается два документа с заданными offerid (wareId)
        '''

        responseJson = {
            "search": {
                "total": 2,
                "results": [
                    {"entity": "offer", "wareId": "09lEaAKkQll1XTjm0WPoIA"},
                    {"entity": "offer", "wareId": "ZRK9Q9nKpuAsmQsKgmUtyg"},
                ],
            }
        }

        # через запятую
        response = self.report.request_json(
            'place=offerinfo&offerid=ZRK9Q9nKpuAsmQsKgmUtyg,09lEaAKkQll1XTjm0WPoIA&rids=213&show-urls=cpa,external,phone,showPhone&regset=1'
        )
        self.assertFragmentIn(response, responseJson)

        # повторение парамметра
        response = self.report.request_json(
            'place=offerinfo&offerid=09lEaAKkQll1XTjm0WPoIA&offerid=ZRK9Q9nKpuAsmQsKgmUtyg&rids=213&show-urls=cpa,external,phone,showPhone&regset=1'
        )
        self.assertFragmentIn(response, responseJson)

    def test_offers_by_feed_shoffer_id(self):
        '''
        MARKETOUT-11400 Множественный запрос offer_id
        Проверяется, что на выдаче place=offerinfo будут выведены все запрошенные пары feed_id + shop_offer_id (feed_shoffer_id)
        Запрашивается два feed_shoffer_id (только с дублированем параметров) 100500-17 (waremd5=ZRK9Q9nKpuAsmQsKgmUtyg) и 100501-19 (waremd5=RcSMzi4tf73qGvxRx8atJg)
        На выводе проверяются их wareId (waremd5)
        '''

        responseJson = {
            "search": {
                "total": 2,
                "results": [
                    {"entity": "offer", "wareId": "ZRK9Q9nKpuAsmQsKgmUtyg"},
                    {"entity": "offer", "wareId": "RcSMzi4tf73qGvxRx8atJg"},
                ],
            }
        }

        for rgb in [None, 'green', 'white', 'whiTE']:
            request = 'place=offerinfo&feed_shoffer_id=100500-17&feed_shoffer_id=100501-19&rids=213&show-urls=cpa,external,phone,showPhone&regset=1'
            if rgb is not None:
                request += '&rgb={}'.format(rgb)
            response = self.report.request_json(request)
            self.assertFragmentIn(response, responseJson)

    russian_offer_id = "Русские буквы"
    comma_offer_id = "Запятые,?:^%№\"@!/&()+-_."

    @classmethod
    def prepare_offer_by_feed_shoffer_id_base64(cls):
        cls.index.offers += [
            Offer(feedid=100511, fesh=100511, offerid=T.russian_offer_id),
            Offer(feedid=100511, fesh=100511, offerid=T.comma_offer_id),
        ]

    def test_offers_by_feed_shoffer_id_base64(self):
        '''
        Множественный запрос offer_id, закодированных в base64
        Проверяется, что на выдаче place=offerinfo будут выведены все запрошенные пары feed_id + shop_offer_id (feed_shoffer_id_base64)
        feed_shoffer_id_base64 содержит закодированные строки, т.к. shop_offer_id может быть любой строкой на синем маркете
        feed_shoffer_id_base64 перетирает feed_shoffer_id
        Функционал доступен под флагом (чтобы не сломать текущее поведение)
        '''

        def encode(feed_shoffer):
            return base64.b64encode(feed_shoffer)

        for rgb in [None, 'green', 'white', 'wHIte']:
            request = 'place=offerinfo&feed_shoffer_id_base64={},{}&feed_shoffer_id=100500-17&rids=213&show-urls=external&regset=1'.format(
                encode(b'100511-' + T.russian_offer_id), encode(b'100511-' + T.comma_offer_id)
            )
            if rgb is not None:
                request += '&rgb={}'.format(rgb)

            # Кодированные запросы разрешены
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,  # Оферы из feed_shoffer_id не пришли
                        "results": [
                            {"shop": {"feed": {"id": "100511", "offerId": T.russian_offer_id}}},
                            {"shop": {"feed": {"id": "100511", "offerId": T.comma_offer_id}}},
                        ],
                    }
                },
                allow_different_len=False,
            )

    def test_offers_by_feed_shoffer_id_and_offerid(self):
        '''
        MARKETOUT-11400 Множественный запрос offer_id
        В случае запроса feed_shoffer_id и offerid на выдаче place=offerinfo будет ошибка "Dont use feed_shoffer_id and offerid in same request"
        '''

        responseJson = {
            "error": {"code": "INVALID_USER_CGI", "message": "Don't use feed_shoffer_id and offerid in same request"}
        }
        self.error_log.expect(code=3043)

        response = self.report.request_json(
            'place=offerinfo&feed_shoffer_id=100500-17&offerid=RcSMzi4tf73qGvxRx8atJg&rids=213&show-urls=cpa,external,phone,showPhone&regset=1'
        )
        self.assertFragmentIn(response, responseJson)

    def test_legacy_offer_card_report_state(self):
        """Что тестируем: поддержку старого формата параметра rs для place=offerinfo

        Задаем запрос с rs в старом формате, проверяем, что выдача такая же, как в test_place
        """
        reportState = 'eJzjEBJiNdQx0jGWYFTiLUktLokvLk0qSi3MTAEAQp0G5g%2C%2C'
        self.check_offerinfo_format(reportState)

    @classmethod
    def prepare_booking_outlets(cls):
        cls.index.shops += [
            Shop(fesh=3, priority_region=213, name='test_shop_3', currency=Currency.RUR, pickup_buckets=[5002]),
        ]
        cls.index.outlets += [Outlet(point_id=1201, fesh=3, region=213, point_type=Outlet.FOR_STORE)]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5002,
                fesh=3,
                carriers=[99],
                options=[PickupOption(outlet_id=1201)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )
        ]

        cls.index.offers += [
            Offer(
                hyperid=9880111,
                fesh=3,
                waremd5='XGllCCxwOP3lZ46vu3kcCg',
                pickup=True,
                store=True,
                post_term_delivery=True,
                booking_availabilities=[BookingAvailability(1201, 213, 5)],
            ),
        ]

    def test_booking_outlets(self):
        """
        Что тестируем: вывод списка точек выдачи с количеством товара, если задан параметр show-booking-outlets
        """
        response = self.report.request_json(
            'place=offerinfo&offerid=XGllCCxwOP3lZ46vu3kcCg&rids=213&show-urls=cpa&regset=1&show-booking-outlets=1'
        )
        self.assertFragmentIn(response, {'bookingOutlets': [{"entity": "bookingOutlet", "id": 1201, "amount": 5}]})

        """ При значении show-booking-outlets=0 список не выводится """
        response = self.report.request_json(
            'place=offerinfo&offerid=XGllCCxwOP3lZ46vu3kcCg&rids=213&show-urls=cpa&regset=1&show-booking-outlets=0'
        )
        self.assertFragmentNotIn(response, {'bookingOutlets': []})

        """ При отсутствии параметра show-booking-outlets список не выводится """
        response = self.report.request_json(
            'place=offerinfo&offerid=XGllCCxwOP3lZ46vu3kcCg&rids=213&show-urls=cpa&regset=1'
        )
        self.assertFragmentNotIn(response, {'bookingOutlets': []})

    @classmethod
    def prepare_currency_convert(cls):
        """
        Инициализация валют для проверки SellerPrice. Для простоты вычисления примем такие курсы из расчета за 1000р:
        1. UAH = 2000
        """

        cls.index.currencies += [
            Currency(
                name=Currency.UAH,
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=2),
                ],
            ),
            Currency(
                name=Currency.RUR,
                exchange_rates=[
                    ExchangeRate(to=Currency.UAH, rate=2),
                ],
            ),
        ]

    @classmethod
    def prepare_seller_price(cls):
        cls.index.shops += [
            Shop(fesh=4, priority_region=213, name='test_shop_4', currency=Currency.UAH),
            Shop(fesh=5, priority_region=213, name='test_shop_5', currency=Currency.RUR),
        ]
        cls.index.offers += [
            Offer(
                fesh=4,
                price=2000,
                title='offer_in_uah',
                waremd5='AfFQXGZRcq-zCQLA1DpAtg',
            ),
            Offer(
                fesh=5,
                price=5000,
                title='offer_in_rur',
                waremd5='d_cvMFtlIDImYfwkDbpbWA',
            ),
        ]

    def __response_seller_price(self, shop_price, shop_currency, user_currency):
        currencies = Currencies(self.index.currencies)
        exchange_rate = currencies.get_exchange_rate(shop_currency, user_currency)
        user_price = shop_price * exchange_rate

        return {
            "seller": {
                "price": '{0:.0f}'.format(shop_price),
                "currency": shop_currency,
                "sellerToUserExchangeRate": exchange_rate if exchange_rate != 1.0 else 1,
            },
            "prices": {"currency": user_currency, "rawValue": '{0:.0f}'.format(user_price)},
        }

    def test_seller_price(self):
        """
        Что проверяем: правильность отображения цены и валюты магазина, а так же конвертации, в зависимости от валюты покупателя
        """

        response = self.report.request_json(
            'place=offerinfo&offerid=AfFQXGZRcq-zCQLA1DpAtg&rids=213&show-urls=external&regset=1'
        )
        self.assertFragmentIn(response, self.__response_seller_price(4000, 'UAH', 'RUR'))

        response = self.report.request_json(
            'place=offerinfo&offerid=AfFQXGZRcq-zCQLA1DpAtg&rids=213&show-urls=external&regset=1&currency=UAH'
        )
        self.assertFragmentIn(response, self.__response_seller_price(4000, 'UAH', 'UAH'))

        response = self.report.request_json(
            'place=offerinfo&offerid=d_cvMFtlIDImYfwkDbpbWA&rids=213&show-urls=external&regset=1'
        )
        self.assertFragmentIn(response, self.__response_seller_price(5000, 'RUR', 'RUR'))

        response = self.report.request_json(
            'place=offerinfo&offerid=d_cvMFtlIDImYfwkDbpbWA&rids=213&show-urls=external&regset=1&currency=UAH'
        )
        self.assertFragmentIn(response, self.__response_seller_price(5000, 'RUR', 'UAH'))

    @classmethod
    def prepare_is_shop_unit(cls):
        cls.index.gltypes += [
            GLType(
                param_id=210,
                hid=6,
                gltype=GLType.ENUM,
                subtype='size',
                unit_param_id=211,
                name="Size",
                values=[
                    GLValue(value_id=1, text='32', unit_value_id=11),
                    GLValue(value_id=2, text='34', unit_value_id=11),
                    GLValue(value_id=3, text='XL', unit_value_id=12),
                ],
                cluster_filter=True,
            ),
            GLType(
                param_id=211,
                hid=6,
                gltype=GLType.ENUM,
                name='size_units',
                position=None,
                values=[
                    GLValue(value_id=11, text='RU', default=True),
                    GLValue(value_id=12, text='INT'),
                ],
            ),
        ]
        cls.index.offers += [
            Offer(
                fesh=6,
                hid=6,
                waremd5='sgqPJFAFWJ-L9KeV8PRaLQ',
                glparams=[
                    GLParam(param_id=210, value=1),
                    GLParam(param_id=210, value=2),
                    GLParam(param_id=210, value=3),
                ],
                original_glparams=[
                    GLParam(param_id=210, value=1),
                    GLParam(param_id=210, value=3),
                ],
            ),
        ]

    def test_is_shop_unit(self):
        """
        Что тестируем: флаг isShopValue в фильтрах офера. Этот флаг указывает выставлял ли магазин этот параметр.
        Магазин указал размеры в интернациональном размере (INT) и руссколм (RU). Остальные размеры были сконвертированы в маркете.
        На выдаче флаг isShopValue=true будет указан для данных значений. Значения false не выводятся.
        """
        response = self.report.request_json(
            'place=offerinfo&offerid=sgqPJFAFWJ-L9KeV8PRaLQ&rids=213&show-urls=cpa&regset=1'
        )
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        "id": "210",
                        "units": [
                            {"unitId": "INT", "id": "12", "values": [{"id": "3", "isShopValue": True}]},
                            {
                                "unitId": "RU",
                                "id": "11",
                                "values": [
                                    {"id": "1", "isShopValue": True},
                                    {"id": "2", "isShopValue": NoKey("isShopValue")},
                                ],
                            },
                        ],
                    }
                ]
            },
        )

    def get_showuid(self, response, offer_index):
        url = response.root['search']['results'][offer_index]['urls']['encrypted']
        return re.search(r'\/uid=(\d+)/?', url).group(1)

    def test_showuid_uniqueness(self):
        """Используем существующие офферы
        Что тестируем: значение show-uid уникально для каждого оффера
        """
        # Делаем запрос с включенным random (no-random=0)
        response = self.report.request_json(
            'place=offerinfo&no-random=0&rids=213&show-urls=external&regset=1&offerid=09lEaAKkQll1XTjm0WPoIA&offerid=ZRK9Q9nKpuAsmQsKgmUtyg&offerid=OfferToCheckApiInfo123'
        )

        # Получаем список show-uid-ов офферов
        show_uids = list()
        show_uids.append(self.get_showuid(response, 0))
        show_uids.append(self.get_showuid(response, 1))
        show_uids.append(self.get_showuid(response, 2))

        # Проверяем, что show-uid не повторяются
        self.assertEqual(len(show_uids), len(set(show_uids)))

    @classmethod
    def prepare_feed_request(cls):
        cls.index.offers += [
            Offer(
                fesh=1,
                feedid=1,
                title='feed1_offer{}'.format(i),
                offerid="offer{}".format(i),
                randx=i,
            )
            for i in range(1, 21)
        ]

    def test_feed_request_default_page_size(self):
        """
        Что тестируем: при наличии запроса только по feed_id выдача разбивается на страницы
        """
        response = self.report.request_json('place=offerinfo&rids=213&show-urls=external&regset=1&feed_shoffer_id=1-*')
        self.assertFragmentIn(
            response,
            {
                "total": 10,  # По-умолчанию страница 10
                "totalOffers": 20,
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "feed1_offer{}".format(i)},
                    }
                    for i in range(20, 10, -1)
                ],
            },
            allow_different_len=False,
        )

    def test_feed_request_default_page_size_page_2(self):
        """
        Что тестируем: при наличии запроса только по feed_id выдача разбивается на страницы
        Во второй странице нет оферов с первой
        """
        response = self.report.request_json(
            'place=offerinfo&rids=213&show-urls=external&regset=1&feed_shoffer_id=1-*&page=2'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 10,  # По-умолчанию страница 10
                "totalOffers": 20,
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "feed1_offer{}".format(i)},
                    }
                    for i in range(10, 0, -1)
                ],
            },
            allow_different_len=False,
        )

    def test_feed_request_with_page_size(self):
        """
        Что тестируем: при наличии запроса только по feed_id выдача разбивается на страницы. Выдача разбивается согласно заданному размеру
        """
        for i in range(1, 11):
            response = self.report.request_json(
                'place=offerinfo&rids=213&show-urls=external&regset=1&feed_shoffer_id=1-*&numdoc=2&page={}'.format(i)
            )
            self.assertFragmentIn(
                response,
                {
                    "total": 2,
                    "totalOffers": 20,
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "feed1_offer{}".format(i)},
                        }
                        for i in range(22 - i * 2, 20 - i * 2, -1)
                    ],
                },
                allow_different_len=False,
            )

        response = self.report.request_json(
            'place=offerinfo&rids=213&show-urls=external&regset=1&feed_shoffer_id=1-*&numdoc=30'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 20,
                "totalOffers": 20,
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "feed1_offer{}".format(i)},
                    }
                    for i in range(20, 0, -1)
                ],
            },
            allow_different_len=False,
        )

    def test_feed_offer_request_paging(self):
        """
        Что тестируем: если в запросе только конкретные оферы (без записей виду 1-*), то дефолтный пэйджинг будет задан для выдачи всех оферов
        """
        offers_request = ''
        for i in range(1, 21):
            offers_request += '&feed_shoffer_id=1-offer{}'.format(i)
        response = self.report.request_json('place=offerinfo&rids=213&show-urls=external&regset=1' + offers_request)
        self.assertFragmentIn(
            response,
            {
                "total": 20,
                "totalOffers": 20,
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "feed1_offer{}".format(i)},
                    }
                    for i in range(20, 0, -1)
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_sku_literal(cls):
        cls.index.offers += [
            Offer(feedid=2018, waremd5='1527iAnHLZST2Ekoq4xEgg', sku=2323),
            Offer(feedid=2019, waremd5='1528iAnHLZST2Ekoq4xEgg', sku=2323),
            Offer(feedid=2020, waremd5='1529iAnHLZST2Ekoq4xEgg', sku=232323),
        ]

    def test_sku_literal(self):
        """
        Что проверяем: работу поискового литерала по маркетному СКУ в offerinfo
        Запрашиваем все оферы только по маркетному СКУ
        """
        response = self.report.request_json('place=offerinfo&rids=213&show-urls=external&regset=1&market-sku=2323')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "totalOffers": 2,
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "1527iAnHLZST2Ekoq4xEgg",
                    },
                    {
                        "entity": "offer",
                        "wareId": "1528iAnHLZST2Ekoq4xEgg",
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_sku_literal_with_feed(self):
        """
        Что проверяем: работу поискового литерала по маркетному СКУ в offerinfo
        Запрашиваем все оферы по маркетному СКУ и фиду
        """
        response = self.report.request_json(
            'place=offerinfo&rids=213&show-urls=external&regset=1&market-sku=2323&feed_shoffer_id=2018-*'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "totalOffers": 1,
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "1527iAnHLZST2Ekoq4xEgg",
                    }
                ],
            },
            allow_different_len=False,
        )

    def test_show_urls_is_not_required(self):
        '''
        Проверяем, что параметр &show-urls не требуется
        '''
        response = self.report.request_json('place=offerinfo&rids=213&regset=1&market-sku=2323&feed_shoffer_id=2018-*')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "totalOffers": 1,
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "1527iAnHLZST2Ekoq4xEgg",
                    }
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_supplier_description(cls):
        cls.index.regiontree += [
            Region(rid=1),
            Region(rid=2),
        ]
        cls.index.mskus += [
            MarketSku(
                sku=1,
                hyperid=777,
                title="supplier description test",
                pickup_buckets=[5001],
                blue_offers=[
                    BlueOffer(price=1, offerid="without_description", supplier_description=None),
                    BlueOffer(
                        price=1000,
                        offerid="description_and_country",
                        supplier_description="supplier_description",
                        manufacturer_country_ids=[1, 2],
                    ),
                    BlueOffer(price=10, offerid="description", supplier_description="supplier_description"),
                    BlueOffer(price=10, offerid="manufacturer_country", manufacturer_country_ids=[2]),
                ],
            ),
        ]

    def test_supplier_description(self):
        '''
        Проверяем вывод описания от поставщика и стран производителей
        '''
        response = self.report.request_json("place=offerinfo&rids=213&regset=2&market-sku=1")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'shopSku': 'without_description',
                        'supplierDescription': Absent(),
                        'manufacturer': {
                            'countries': Absent(),
                        },
                    },
                    {
                        'shopSku': 'description_and_country',
                        'supplierDescription': 'supplier_description',
                        'manufacturer': {
                            'countries': [
                                {
                                    'entity': 'region',
                                    'id': 1,
                                },
                                {
                                    'entity': 'region',
                                    'id': 2,
                                },
                            ],
                        },
                    },
                    {
                        'shopSku': 'description',
                        'supplierDescription': 'supplier_description',
                        'manufacturer': {
                            'countries': Absent(),
                        },
                    },
                    {
                        'shopSku': 'manufacturer_country',
                        'supplierDescription': Absent(),
                        'manufacturer': {
                            'countries': [
                                {
                                    'entity': 'region',
                                    'id': 2,
                                }
                            ],
                        },
                    },
                ]
            },
        )

    @classmethod
    def prepare_domain_url(cls):
        cls.index.shops += [
            Shop(fesh=73733, name='shop_with_domain', domain='ozon.ru', regions=[213]),
            Shop(fesh=73734, name='shop_without_domain', regions=[213]),
        ]

        cls.index.models += [
            Model(hid=2222, hyperid=123, title='Model test'),
        ]

        cls.index.offers += [
            Offer(
                fesh=73733,
                price=100,
                title='offer_with_domain',
                hyperid=123,
            ),
            Offer(
                fesh=73734,
                price=200,
                title='offer_without_domain',
                hyperid=123,
            ),
        ]

    def test_domain_url(self):
        response = self.report.request_json("place=productoffers&hyperid=123&rids=213")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "offer_with_domain"}, "shop": {"domainUrl": "ozon.ru"}},
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer_without_domain"},
                        "shop": {"domainUrl": NoKey("domain")},
                    },
                ]
            },
        )

    def test_market_delivery_flag_on_white(self):
        '''
        Проверка работы флага deliveryPartnerTypes на белом маркете - ожидаем пустой список,
        т.к. наличие доставки проверяется через связь склада и службы доставки, а к белым магазин склад не привязывается
        '''
        response = self.report.request_json(
            'place=offerinfo&offerid=XGllCCxwOP3lZ46vu3kcCg&rids=213&show-urls=cpa&regset=1'
        )
        self.assertFragmentIn(response, {"results": [{"entity": "offer", "delivery": {"deliveryPartnerTypes": []}}]})

    @classmethod
    def prepare_third_party_dco_fields(cls):
        cls.index.shops += [
            Shop(
                fesh=7777,
                datafeed_id=7777,
                priority_region=213,
                name='OZON',
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=8888,
                datafeed_id=8888,
                priority_region=213,
                name='Amazon',
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=99999,
                datafeed_id=99999,
                priority_region=213,
                name='Goods',
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(fesh=666666, datafeed_id=666666, priority_region=213, name='pleer', cpa=Shop.CPA_REAL),
        ]
        cls.index.mskus += [
            MarketSku(
                sku=1001,
                hyperid=666,
                title="MSKU DCO 3P",
                ref_min_price=100,
                is_golden_matrix=True,
                blue_offers=[
                    BlueOffer(
                        price=102, feedid=7777, waremd5="BOffer-BuyBox-Strategy", offerid="G-3-SA-97827009-русский"
                    ),
                    BlueOffer(price=103, feedid=8888, waremd5="BOffer-None-Strategy01"),
                    BlueOffer(price=104, feedid=99999, waremd5="BOffer-MinRef-Strategy"),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=666666,
                title='dsbs offer',
                waremd5="BOffer-Dsbs-Strategy01",
                hyperid=666,
                sku=1001,
                price=105,
                cpa=Offer.CPA_REAL,
            ),
        ]

        cls.dynamic.market_dynamic.dynamic_price_control += [
            DynamicPriceControlData(7777, 1, 0),
            DynamicPriceControlData(99999, 5, 1),
        ]
        cls.index.shop_operational_rating += [
            ShopOperationalRating(
                calc_time=1589936458409,
                shop_id=7777,
                late_ship_rate=5.9,
                cancellation_rate=1.93,
                return_rate=0.14,
                total=99.8,
            ),
        ]

        cls.index.shop_operational_rating += [
            ShopOperationalRating(
                calc_time=1589936458409,
                shop_id=666666,
                late_ship_rate=9.9,
                cancellation_rate=1.93,
                return_rate=0.14,
                total=99.8,
                dsbs_return_rate=0.5,
            ),
        ]

    def test_third_party_dco_fields(self):
        response = self.report.request_json("place=offerinfo&rids=213&regset=2&market-sku=1001")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "marketSku": "1001",
                            "offerColor": "blue",
                            "supplier": {"id": 7777},
                            "refMinPrice": {"currency": "RUR", "value": "100"},
                            "isGoldenMatrix": True,
                            "dynamicPriceStrategy": 1,
                            "maxAllowedDiscount": 1,
                            "priceBeforeDynamicStrategy": {"currency": "RUR", "value": "102"},
                        },
                        {
                            "marketSku": "1001",
                            "offerColor": "blue",
                            "supplier": {"id": 8888},
                            "refMinPrice": {"currency": "RUR", "value": "100"},
                            "isGoldenMatrix": True,
                        },
                        {
                            "marketSku": "1001",
                            "offerColor": "blue",
                            "supplier": {"id": 99999},
                            "refMinPrice": {"currency": "RUR", "value": "100"},
                            "isGoldenMatrix": True,
                            "dynamicPriceStrategy": 2,
                            "maxAllowedDiscount": 5,
                            "priceBeforeDynamicStrategy": {"currency": "RUR", "value": "104"},
                        },
                    ]
                }
            },
        )

        self.assertFragmentNotIn(
            response, {"search": {"results": [{"priceBeforeDynamicStrategy": {"currency": "RUR", "value": "103"}}]}}
        )

        self.show_log.expect(
            price_before_dynamic_strategy='104',
            dynamic_strategy_type=1,
        ).times(2)
        self.show_log.expect(
            price_before_dynamic_strategy='102',
            dynamic_strategy_type=0,
        ).times(2)

    def test_show_supplier_operational_rating(self):
        """
        Проверяем, что при переданном флаге market_opeational_rating происходит добавление в выдачу
        операционного рейтинга, если таковой имеется для данного магазина.
        """
        OPERATIONAL_RATING_FLAGS = (
            ('', True),
            ('&rearr-factors=market_operational_rating=1;market_operational_rating_everywhere=1', True),
            ('&rearr-factors=market_operational_rating=0;market_operational_rating_everywhere=0', False),
        )

        for rearr_flag, has_rating in OPERATIONAL_RATING_FLAGS:
            response = self.report.request_json(
                "place=offerinfo&rids=213&regset=2&offerid=BOffer-BuyBox-Strategw&rgb=blue{}".format(rearr_flag)
            )
            if has_rating:
                self.assertFragmentIn(
                    response,
                    {
                        "wareId": "BOffer-BuyBox-Strategw",
                        "supplier": {
                            "id": 7777,
                            "operationalRating": {
                                "calcTime": 1589936458409,
                                "lateShipRate": 5.9,
                                "cancellationRate": 1.93,
                                "returnRate": 0.14,
                                "total": 99.8,
                            },
                        },
                    },
                )
            else:
                self.assertFragmentIn(
                    response,
                    {"wareId": "BOffer-BuyBox-Strategw", "supplier": {"id": 7777, "operationalRating": Absent()}},
                )

        response = self.report.request_json(
            "place=offerinfo&rids=213&regset=2&offerid=BOffer-Dsbs-Strategy01&rearr-factors=market_operational_rating=1;market_operational_rating_everywhere=0"
        )
        self.assertFragmentNotIn(
            response,
            {
                "wareId": "BOffer-Dsbs-Strategy0w",
                "shop": {
                    "id": 666666,
                    "operationalRating": {
                        "calcTime": 1589936458409,
                        "lateShipRate": 9.9,
                        "cancellationRate": 1.93,
                        "returnRate": 0.14,
                        "total": 99.8,
                    },
                },
            },
        )

        response = self.report.request_json(
            "place=offerinfo&rids=213&regset=2&offerid=BOffer-Dsbs-Strategy01&rearr-factors=market_operational_rating=1;market_operational_rating_dropship_only=0"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "BOffer-Dsbs-Strategy0w",
                "shop": {
                    "id": 666666,
                    "operationalRating": {
                        "calcTime": 1589936458409,
                        "lateShipRate": 9.9,
                        "cancellationRate": 1.93,
                        "returnRate": 0.14,
                        "total": 99.8,
                        "dsbsReturnRate": 0.5,
                        "ffPlanFactRate": 0,
                        "ffReturnRate": 0,
                        "ffLateShipRate": 0,
                        "dsbsLateDeliveryRate": 0,
                        "dsbsCancellationRate": 0,
                    },
                },
            },
        )

    def test_consistent_feedid_virtualization(self):
        """Проверяем, что при передаче флага consistent_feedid_virtualization feedid на выдаче сохраняет значение из запроса"""

        # виртуализация не происходит
        response = self.report.request_json(
            "place=offerinfo&feed_shoffer_id=7777-G-3-SA-97827009-русский&show-urls=cpa&rids=213&regset=1"
        )
        self.assertFragmentIn(
            response,
            {
                "feed": {
                    "id": "7777",
                    "offerId": "G-3-SA-97827009-русский",
                }
            },
        )

        # даже с wildcard-offer-ом
        response = self.report.request_json("place=offerinfo&feed_shoffer_id=7777-*&show-urls=cpa&rids=213&regset=1")
        self.assertFragmentIn(
            response,
            {
                "feed": {
                    "id": "7777",
                    "offerId": "G-3-SA-97827009-русский",
                }
            },
        )

        # даже с ппц укуренным wildcard-offer-ом
        response = self.report.request_json(
            "place=offerinfo&feed_shoffer_id=7777-G-3-SA-97827009-*&show-urls=cpa&rids=213&regset=1"
        )
        self.assertFragmentIn(
            response,
            {
                "feed": {
                    "id": "7777",
                    "offerId": "G-3-SA-97827009-русский",
                }
            },
        )

        # при передаче виртуализированной формы -- она сохраняется
        response = self.report.request_json(
            "place=offerinfo&feed_shoffer_id=101-7777.G-3-SA-97827009-русский&show-urls=cpa&rids=213&regset=1"
        )
        self.assertFragmentIn(
            response,
            {
                "feed": {
                    "id": "101",
                    "offerId": "7777.G-3-SA-97827009-русский",
                }
            },
        )

    @classmethod
    def prepare_large_size(cls):
        cls.index.offers += [
            Offer(
                fesh=666666,
                title='dsbs offer',
                waremd5="Large__Dsbs_Offer____g",
                hyperid=666,
                sku=1001,
                price=105,
                weight=100,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                fesh=666666,
                title='dsbs offer',
                waremd5="Small__Dsbs_Offer____g",
                hyperid=666,
                sku=1001,
                price=105,
                weight=1,
                cargo_types=[300],
                cpa=Offer.CPA_REAL,
            ),
        ]

    def test_large_size_field(self):
        is_kgt_offers = (("Large__Dsbs_Offer____g", True), ("Small__Dsbs_Offer____g", False))

        for offer, is_kgt in is_kgt_offers:
            response = self.report.request_json("place=offerinfo&rids=213&regset=2&offerid={}".format(offer))
            self.assertFragmentIn(response, {"largeSize": is_kgt})

    @classmethod
    def prepare_session_context_timeout(cls):
        cls.index.shops += [
            Shop(fesh=1331, priority_region=213, regions=[255], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.offers += [
            Offer(
                fesh=1331,
                price=15000,
                hyperid=1351,
                cpa=Offer.CPA_REAL,
                waremd5='RcSMzi4txxxqGvxRx8atJg',
                bid=213,
                fee=1000,
            ),
        ]

    def test_session_context_timeout(self):
        """
        @see MARKETOUT-40095
        проверяем, как заполняется fee из SessionContext
        """
        default_session_context_timeout = 3 * 60 * 60  # from market/report/library/internals/internals.h
        cpc1 = Cpc.create_for_offer(
            click_price=1,
            offer_id='RcSMzi4txxxqGvxRx8atJg',
            shop_id=1331,
            bid=1,
            shop_fee=800,
            fee=650,
            minimal_fee=100,
        )
        cpc2 = Cpc.create_for_offer(
            click_price=1,
            offer_id='RcSMzi4txxxqGvxRx8atJg',
            shop_id=1331,
            bid=1,
            shop_fee=800,
            fee=650,
            minimal_fee=100,
            time_after_generation=default_session_context_timeout / 2,
        )

        # generation_time = now
        response = self.report.request_json(
            'place=offerinfo&hyperid=1351&cpc={}&show-urls=external,cpa&rids=213&offerid=RcSMzi4txxxqGvxRx8atJg&regset=2&debug=1'.format(
                cpc1
            )
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "debug": {
                                "sale": {
                                    "shopFee": 800,
                                    "brokeredFee": 650,
                                }
                            }
                        }
                    ]
                }
            },
        )

        # generation_time = now - default_session_context_timeout / 2
        response = self.report.request_json(
            'place=offerinfo&hyperid=1351&cpc={}&show-urls=external,cpa&rids=213&offerid=RcSMzi4txxxqGvxRx8atJg&regset=2&debug=1'.format(
                cpc2
            )
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "debug": {
                                "sale": {
                                    "shopFee": 800,
                                    "brokeredFee": 650,
                                }
                            }
                        }
                    ]
                },
                "debug": {"report": {"logicTrace": [Contains("sale.cpp", "overrided by session context")]}},
            },
        )

        # generation_time = now - default_session_context_timeout / 2
        # с уменьшенным вчетверо таймаутом (просроченный)
        response = self.report.request_json(
            'place=offerinfo&hyperid=1351&cpc={}&show-urls=external,cpa&rids=213&offerid=RcSMzi4txxxqGvxRx8atJg&regset=2&debug=1'
            '&rearr-factors=market_session_context_timeout={}'.format(cpc2, default_session_context_timeout / 4)
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "debug": {
                                "sale": {
                                    "shopFee": 0,
                                    "brokeredFee": 0,
                                }
                            }
                        }
                    ]
                },
                "debug": {
                    "report": {
                        "logicTrace": [Contains("sale.cpp", "was not overrided by invalid or overdue session context")]
                    }
                },
            },
        )

        # generation_time = now - default_session_context_timeout / 2
        # с отключенным таймаутом
        response = self.report.request_json(
            'place=offerinfo&hyperid=1351&cpc={}&show-urls=external,cpa&rids=213&offerid=RcSMzi4txxxqGvxRx8atJg&regset=2&debug=1'
            '&rearr-factors=market_session_context_timeout=0'.format(cpc2)
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "debug": {
                                "sale": {
                                    "shopFee": 800,
                                    "brokeredFee": 650,
                                }
                            }
                        }
                    ]
                },
                "debug": {"report": {"logicTrace": [Contains("sale.cpp", "overrided by session context")]}},
            },
        )

    @classmethod
    def prepare_medicine_booking(cls):
        cls.index.shops += [
            Shop(fesh=1024, priority_region=213, regions=[255], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.offers += [
            Offer(
                fesh=1024,
                price=1536,
                hyperid=2048,
                cpa=Offer.CPA_REAL,
                waremd5='med_white_booking____g',
                is_medicine=True,
                is_medical_booking=True,
            ),
        ]

    def test_medicine_booking_offer(self):
        request = 'place=offerinfo&offerid=med_white_booking____g&rids=213&show-urls=cpa,external&regset=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "hasBooking": True,
                        },
                        "model": {
                            "id": 2048,
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_jumptable_params_for_offer_filters(cls):
        cls.index.gltypes += [
            GLType(
                hid=46114,
                param_id=1,
                name="Size",
                xslname="size",
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[30, 32],
                model_filter_index=1,
                position=-1,
            ),
            GLType(
                hid=46114,
                param_id=2,
                name="Height",
                xslname="height",
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[170, 175],
                model_filter_index=2,
                position=-1,
            ),
        ]

        cls.index.models += [
            Model(
                hid=46114,
                title="Model",
                hyperid=46114,
                glparams=[
                    GLParam(param_id=3, value=50),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=46114,
                hyperid=46114,
                sku=4611411,
                title="Concrete MSKU 1",
                blue_offers=[
                    BlueOffer(ts=1, price=200, waremd5='Jump_Table_Offer_____1'),
                ],
                glparams=[
                    GLParam(param_id=1, value=30),
                    GLParam(param_id=2, value=170),
                ],
            ),
        ]

    def test_jumptable_params_in_offer_filters(self):
        # Проверяем, что без fill-offer-filters-with-jumptable
        response = self.report.request_json(
            'place=offerinfo&offerid=Jump_Table_Offer_____1&rids=0&regset=2&show-urls=external'
        )

        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "3",
                    },
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "1",
                    },
                    {
                        "id": "2",
                    },
                ]
            },
        )

        # Проверяем, что c fill-offer-filters-with-jumptable в фильтрах так же приходит карта переходов
        response = self.report.request_json(
            'place=offerinfo&offerid=Jump_Table_Offer_____1&rids=0&regset=2&show-urls=external&fill-offer-filters-with-jumptable=1'
        )

        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "1",
                        "valuesCount": 1,
                        "values": [{"id": "30"}],
                        "marks": {"specifiedForOffer": True},
                    },
                    {
                        "id": "2",
                        "valuesCount": 1,
                        "values": [{"id": "170"}],
                        "marks": {"specifiedForOffer": True},
                    },
                    {
                        "id": "3",
                    },
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "3",
                        "marks": {"specifiedForOffer": True},
                    },
                ]
            },
        )

    def test_valid_warranty(self):
        # Проверяем, что если параметр seller_warranty указан и содержит срок гарантии, поле warrantyPeriod появляется в выдаче
        response = self.report.request_json(
            'place=offerinfo&offerid=ZRK1Q_valid_mQsKgmUtyg&rids=0&regset=2&show-urls=external'
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "ZRK1Q_valid_mQsKgmUtyg",
                "seller": {
                    "warrantyPeriod": "P1Y2M10DT2H30M",
                },
            },
        )

    def test_invalid_warranty(self):
        # Проверяем, что если параметр seller_warranty не указан либо указан но не содержит срок гарантии, поля warrantyPeriod нет в выдаче
        for offerid in ['ZRK9Q9nKpuAsmQsKgmUtyg', 'ZRK2Q_invalid_sKgmUtyg', 'ZRK3Q_empty_mQsKgmUtyg']:
            response = self.report.request_json(
                'place=offerinfo&offerid={}&rids=0&regset=2&show-urls=external'.format(offerid)
            )

            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "wareId": offerid,
                    "seller": {
                        "warrantyPeriod": Absent(),
                    },
                },
            )

    @classmethod
    def prepare_parallel_import(cls):
        cls.index.parallel_import_warranty += [
            ParallelImportWarranty(category=1111111, brand=2222222),
        ]

    def test_parallel_import(self):
        # Проверяем, что параметр parallel_imported пробрасывается,
        # parallelImportWarrantyAction заполняется только если parallel_imported=True,
        # если hid+vendor_id есть в svn-data (parallel_import_warranty.json)
        # parallelImportWarrantyAction='WARRANTY' иначе 'CHARGE_BACK'
        for offerid, is_parallel_imported, warranty_action in [
            ('ZRK1Q_parallel_KgmUtyg', True, 'CHARGE_BACK'),
            ('ZR_parallel_warranty_g', True, 'WARRANTY'),
            ('ZRK1Q_not_parallel_tyg', False, None),
            ('ZRK9Q9nKpuAsmQsKgmUtyg', False, None),
        ]:
            response = self.report.request_json(
                'place=offerinfo&offerid={}&rids=0&regset=2&show-urls=external'.format(offerid)
            )

            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "wareId": offerid,
                    "parallelImport": is_parallel_imported,
                    "parallelImportWarrantyAction": warranty_action if is_parallel_imported else Absent(),
                },
            )


if __name__ == '__main__':
    main()
