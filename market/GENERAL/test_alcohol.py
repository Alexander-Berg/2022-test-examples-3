#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CategoryRestriction,
    ClickType,
    Currency,
    DeliveryBucket,
    DynamicWarehousesPriorityInRegion,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    GLParam,
    HyperCategory,
    MarketSku,
    Model,
    Offer,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    OutletLegalInfo,
    OutletLicense,
    PickupBucket,
    PickupOption,
    Region,
    RegionalRestriction,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main
from core.matcher import ElementCount, EmptyList, Greater, NoKey
from core.types.hypercategory import ALCOHOL_VINE_CATEG_ID

import urllib


class _Outlets(object):
    alcodata_outlet = Outlet(
        point_id=48705729,
        fesh=549083,
        region=213,
        point_type=Outlet.FOR_PICKUP,
        delivery_option=OutletDeliveryOption(
            day_from=3,
            day_to=5,
            order_before=6,
            work_in_holiday=False,
            price=500,
            price_to=1000,
            shipper_readable_id="self shipper",
        ),
        working_days=[i for i in range(10)],
    )

    mbi_outlet = Outlet(
        point_id=67137,
        fesh=549083,
        region=213,
        point_type=Outlet.FOR_PICKUP,
        working_days=[i for i in range(10)],
        legal_info=OutletLegalInfo(
            number='2222', type=u'ААА', name=u'Точка', legal_address=u'Москва', actual_address=u'СпБ'
        ),
        licenses=[OutletLicense(number='1', issue_date='2019-01-01', expiry_date='2021-01-01', status='SUCCESS')],
    )

    alcodata_and_mbi_outlet = Outlet(
        point_id=48705836,
        fesh=549083,
        region=213,
        point_type=Outlet.FOR_PICKUP,
        working_days=[i for i in range(10)],
        legal_info=OutletLegalInfo(number='333', type=u'БББ', name=u'Точка2', legal_address=None, actual_address=None),
        licenses=[OutletLicense(number='2', issue_date='2019-02-02', expiry_date='2021-02-02', status='SUCCESS')],
    )

    outlet_without_legal_info = Outlet(
        point_id=400, fesh=549083, region=213, point_type=Outlet.FOR_PICKUP, working_days=[i for i in range(10)]
    )

    outlet_no_license_in_mbi = Outlet(
        point_id=48705769,
        fesh=549083,
        region=213,
        point_type=Outlet.FOR_PICKUP,
        working_days=[i for i in range(10)],
        legal_info=OutletLegalInfo(number='444', type=u'ВВВ', name=u'Запятая', legal_address=None, actual_address=None),
    )

    blue_outlet = Outlet(
        point_id=66843529, fesh=431782, region=213, point_type=Outlet.FOR_PICKUP, working_days=[i for i in range(7)]
    )

    outlet_non_alcohol_license = Outlet(
        point_id=987987123,
        fesh=549083,
        region=213,
        point_type=Outlet.FOR_PICKUP,
        working_days=[i for i in range(10)],
        licenses=[OutletLicense(type='UNKNOWN', number='3', issue_date='2019-03-03', expiry_date='2021-04-04')],
    )

    outlet_alcohol_license_new = Outlet(
        point_id=987123,
        fesh=549083,
        region=213,
        point_type=Outlet.FOR_PICKUP,
        working_days=[i for i in range(10)],
        licenses=[OutletLicense(number='3', status='NEW', issue_date='2019-03-03', expiry_date='2021-04-04')],
    )

    outlet_alcohol_license_unknown = Outlet(
        point_id=9823,
        fesh=549083,
        region=213,
        point_type=Outlet.FOR_PICKUP,
        working_days=[i for i in range(7)],
        licenses=[OutletLicense(number='5', status='UNKNOWN', issue_date='2019-04-04', expiry_date='2021-05-05')],
    )

    all_outlets = (
        alcodata_outlet,
        mbi_outlet,
        alcodata_and_mbi_outlet,
        outlet_without_legal_info,
        outlet_no_license_in_mbi,
        blue_outlet,
        outlet_non_alcohol_license,
        outlet_alcohol_license_new,
        outlet_alcohol_license_unknown,
    )


def get_licence(outlet):
    licence = next(iter(outlet.licenses), None)
    if licence is None:
        return None
    if licence.type != "ALCOHOL":
        return None
    if licence.status != "SUCCESS":
        return None  # status NEW is not allowed in production environment

    return licence


class _AlcoData(object):
    legal_info = OutletLegalInfo(
        name=u'Арома Маркет',
        legal_address=u'121087, г.Москва, пр-д Береговой, д.5, стр.1',
        actual_address=u'121087, г.Москва, пр-д Береговой, д.5, стр.1',
        number="1027739146412",
    )

    outlets = [
        Outlet(
            point_id=48705729,
            legal_info=legal_info,
            licenses=[OutletLicense(number=u"77РПА0010392", issue_date="2014-12-15", expiry_date="2019-12-15")],
        ),
        Outlet(
            point_id=48705836,
            legal_info=legal_info,
            licenses=[OutletLicense(number=u"77РПА0012222", issue_date="2016-05-26", expiry_date="2021-05-26")],
        ),
        Outlet(
            point_id=48705769,
            legal_info=legal_info,
            licenses=[OutletLicense(number=u"77РПА0009854", issue_date="2014-06-27", expiry_date="2019-06-27")],
        ),
        Outlet(
            point_id=66843529,
            legal_info=OutletLegalInfo(
                type=u'МММ',
                name=u"Звонкие пузыри",
                number="1092837465473",
                legal_address=u"101102, г. Москва, ул. Льва Толстого, д. 3",
                actual_address=u"101102, г. Москва, ул. Льва Толстого, д. 3",
            ),
            licenses=[OutletLicense(number=u"11ЛИЦ3434901", issue_date="2019-05-01", expiry_date="2019-06-30")],
        ),
    ]

    @classmethod
    def legal_info(cls, point_id):
        for o in cls.outlets:
            if o.point_id == point_id:
                return o.legal_info
        return None

    @classmethod
    def license(cls, point_id):
        for o in cls.outlets:
            if o.point_id == point_id:
                return o.licenses[0]
        return None


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.shops += [
            Shop(fesh=549083, priority_region=213, phone="+7222998989", phone_display_options='*'),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=ALCOHOL_VINE_CATEG_ID),
            HyperCategory(hid=101),
        ]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='ask_18', hids=[ALCOHOL_VINE_CATEG_ID], regional_restrictions=[RegionalRestriction()]
            )
        ]

        cls.index.outlets += [o for o in _Outlets.all_outlets]

        #        FIXME: places (except place=parallel) tests fail if uncomment (elenita@)
        #        cls.index.category_restrictions += [
        #            CategoryRestriction(
        #                name="alco",
        #                hids=[ALCOHOL_VINE_CATEG_ID],
        #                regional_restrictions=[
        #                    RegionalRestriction(show_offers=False)
        #                ]
        #            )
        #        ]

        cls.index.models += [
            Model(hyperid=1, hid=ALCOHOL_VINE_CATEG_ID, title="Blue wine model"),
            Model(hyperid=900, hid=ALCOHOL_VINE_CATEG_ID, title="Super vine"),
            Model(hyperid=1900, hid=ALCOHOL_VINE_CATEG_ID, title="Super vine"),
            Model(hyperid=800, hid=101, title="mobile phone"),
        ]
        cls.dynamic.lms += [
            DynamicWarehousesPriorityInRegion(region=213, warehouses=[166, 145]),
            DynamicWarehouseInfo(id=166, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=166, warehouse_to=166),
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        ]

        cls.index.offers += [
            Offer(fesh=549083, hid=ALCOHOL_VINE_CATEG_ID, alcohol=True, title="Vine 1", offerid="vine"),
            Offer(
                fesh=549083,
                hid=ALCOHOL_VINE_CATEG_ID,
                alcohol=True,
                title="Vine 2",
                hyperid=900,
                offerid="vinemodel",
                pickup_buckets=[53],
                has_delivery_options=False,
                waremd5="LicensedWhiteWine____g",
            ),
            Offer(
                fesh=549083,
                hid=ALCOHOL_VINE_CATEG_ID,
                alcohol=True,
                title="Unlicensed white wine",
                hyperid=1900,
                has_delivery_options=False,
                waremd5='UnlicensedWhiteWine__g',
                pickup_buckets=[54],
            ),
            Offer(
                fesh=549083,
                hid=ALCOHOL_VINE_CATEG_ID,
                alcohol=True,
                title="White wine available in 1 outlet with license",
                hyperid=1900,
                has_delivery_options=False,
                waremd5='MixedLicenseWhiteWineg',
                pickup_buckets=[54, 55],
            ),
            Offer(fesh=549083, hid=101, title="Mobile Phone", hyperid=800, offerid="phone", pickup_buckets=[53]),
        ]

    def test_alcohol_prime(self):
        """
        Проверяем, что в запросе без флага adult, в ответе нет алкоголя
        """
        for is_blue, text, total_count, color in ((True, "wine", 1, "&rgb=blue"), (False, "vine", 0, "")):
            response = self.report.request_json(
                'place=prime&hid={}&text={}{}'.format(ALCOHOL_VINE_CATEG_ID, text, color)
            )
            self.assertFragmentIn(response, {"results": ElementCount(0)})

            """
            И в ответе есть флаг adult: True
            """
            self.assertFragmentIn(response, {"search": {"total": 0, "adult": True, "restrictionAge18": True}})

            """
            но с флагом adult оффер и модель в ответе есть
            """
            response = self.report.request_json(
                'place=prime&adult=1&hid={}&text={}{}'.format(ALCOHOL_VINE_CATEG_ID, text, color)
            )
            if is_blue:
                self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "Blue wine"}})
                self.assertFragmentIn(response, {"entity": "product", "titles": {"raw": "Blue wine model"}})
            else:
                self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "Vine 1"}})
                self.assertFragmentIn(response, {"entity": "product", "titles": {"raw": "Super vine"}})

            self.assertFragmentIn(
                response, {"search": {"total": 1 if is_blue else 4, "adult": True, "restrictionAge18": True}}
            )

    def test_alcohol_images(self):
        """На place=images не должно быть алкоголя"""

        offers = self.report.request_images('place=images&text=vine&adult=1')
        self.assertEqual(0, len(offers))

    def test_alcohol_productoffers(self):
        """
        Проверяем флажок adult в плейсе productoffers
        """
        for is_blue, hyperid in ((True, 1), (False, 900)):
            rgb = '&rgb=blue' if is_blue else ''
            response = self.report.request_json('place=productoffers&hyperid={}{}'.format(hyperid, rgb))
            self.assertFragmentIn(response, {"search": {"total": 0, "adult": True, "restrictionAge18": True}})

    def test_alcohol_modelinfo(self):
        """
        Проверяем флажок adult в плейсе modelinfo и модель тоже есть.
        """
        for rgb, hyperid, is_blue in (('', 900, False), ('&rgb=blue', 1, True)):
            response = self.report.request_json('place=modelinfo&hyperid={}&rids=0{}'.format(hyperid, rgb))
            self.assertFragmentIn(response, {"search": {"total": 1, "adult": True, "restrictionAge18": True}})
            self.assertFragmentIn(
                response, {"entity": "product", "titles": {"raw": "Blue wine model" if is_blue else "Super vine"}}
            )

    def test_alco_format(self):
        """
        Проверяем алко-флажок в выдача
        """
        response = self.report.request_json('place=prime&adult=1&hid=%d&numdoc=30' % ALCOHOL_VINE_CATEG_ID)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Vine 1"},
                "categories": [{"id": ALCOHOL_VINE_CATEG_ID, "kinds": ["alco"]}],
            },
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "titles": {"raw": "Super vine"},
                "categories": [{"id": ALCOHOL_VINE_CATEG_ID, "kinds": ["alco"]}],
            },
        )

    def test_alco_outlet(self):
        """
        проверяем наличие информации о лицензии, если оффер алкогольный
        """
        outlet_id = str(_Outlets.alcodata_and_mbi_outlet.point_id)
        response = self.report.request_json('place=geo&adult=1&rids=213&hyperid=900')
        self.assertFragmentIn(
            response,
            {
                "outlet": {
                    "id": outlet_id,
                    "legalInfo": {
                        "licence": {},
                    },
                }
            },
        )

        """
            проверяем отсутствие информации о лицензии, если оффер не алкогольный
        """
        response = self.report.request_json('place=geo&adult=1&rids=213&hyperid=800')
        self.assertFragmentIn(
            response,
            {"outlet": {"id": outlet_id, "legalInfo": {"licence": NoKey("licence")}}},
        )

    def test_sovetnik(self):
        """Проверяем, что в контентном апи в целом и в советнике в частности
        нет алкоголя ни с флагом adult=1 ни без него
        """

        base_request = 'api=content&place=prime&hid=%d' % ALCOHOL_VINE_CATEG_ID
        for request in [
            base_request,
            base_request + '&adult=1',
            base_request + '&client=sovetnik',
            base_request + '&client=sovetnik&adult=1',
        ]:
            response = self.report.request_json(request)
            self.assertFragmentNotIn(response, {"entity": "offer"})
            self.assertFragmentNotIn(response, {"entity": "product"})

    def test_content_api(self):
        """
        Проверяем, что в api=content нет алкоголя ни с флагом adult=1 ни без него,
        а также алкоголь фильтруется на мобильных устройствах под флагом market_disable_alcohol_for_apps=1
        """
        for request in [
            'api=content&place=prime&adult=1&hid=%d' % ALCOHOL_VINE_CATEG_ID,
            'api=content&place=prime&hid=%d' % ALCOHOL_VINE_CATEG_ID,
            'client=ANDROID&place=prime&adult=1&hid=%d&rearr-factors=market_disable_alcohol_for_apps=1'
            % ALCOHOL_VINE_CATEG_ID,
            'client=IOS&place=prime&adult=1&hid=%d&rearr-factors=market_disable_alcohol_for_apps=1'
            % ALCOHOL_VINE_CATEG_ID,
        ]:
            response = self.report.request_json(request)
            self.assertFragmentNotIn(response, {"entity": "offer"})
            self.assertFragmentNotIn(response, {"entity": "product"})

    def test_alco_filters(self):
        """
        Проверяем что у алкоголя нет фильтров про доставку
        """
        for is_blue_market in (False, True):

            for request in [
                'place=prime&adult=1&hid=%d' % ALCOHOL_VINE_CATEG_ID,
                'adult=1&place=productoffers&hyperid=1&hid=%d' % ALCOHOL_VINE_CATEG_ID,
            ]:
                request += '&rgb={}'.format('blue' if is_blue_market else 'green')

                expected_filters = [
                    "glprice",
                ]

                unexpected_filters = ["promo-type", "delivery-interval", "free-delivery", "offer-shipping"]

                response = self.report.request_json(request)
                self.assertFragmentIn(response, {'filters': [{'id': f} for f in expected_filters]})
                for f in unexpected_filters:
                    self.assertFragmentNotIn(response, {'filters': [{'id': f}]})

    @classmethod
    def prepare_offer_without_url(cls):
        cls.index.shops += [
            Shop(
                fesh=100500,
                priority_region=213,
                phone="+7222998989",
                phone_display_options='*',
                alcohol_status=Shop.ALCOHOL_REAL,
            ),
        ]

        cls.index.offers += [
            Offer(fesh=100500, hid=16155651, alcohol=True, title="Vodka", hyperid=100, pickup=False, has_url=False),
        ]

    def test_offer_without_url(self):
        """
        Проверяем, что оффер без урла возвращается со ссылкой на телефон
        """
        response = self.report.request_json("place=productoffers&hid=16155651&hyperid=100&adult=1&pp=21&rids=0")
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Vodka"},
                "shop": {"phones": {"raw": "+7222998989", "sanitized": "+7222998989"}},
            },
        )

    @classmethod
    def prepare_offer_without_phone(cls):
        cls.index.offers += [
            Offer(
                title="Beauty vine",
                hid=16155651,
                fesh=10299345,
                alcohol=True,
                price=500,
                hyperid=200,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=False,
            )
        ]
        cls.index.shops += [Shop(fesh=10299345, phone=None, phone_display_options='*')]

    def test_offer_without_phone(self):
        """
        Проверяем, что оффер без телефона работает
        """
        response = self.report.request_json("place=productoffers&hid=16155651&hyperid=200&adult=1&pp=21")
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Beauty vine"},
            },
        )

    def test_alcohol_not_zero_click_price(self):
        """
        Проверяем что клики по алкоголю биллятся
        """
        _ = self.report.request_json('place=productoffers&hyperid=900&adult=1&show-urls=showPhone')
        self.click_log.expect(ClickType.SHOW_PHONE, cb=Greater(0), cp=Greater(0))

    @classmethod
    def prepare_parallel_offers_wizard_data(cls):
        """Создаем алкогольные и неалкогольные оферы и модели
        для тестирования оферного колдунщика при наличии алкоголя.
        https://st.yandex-team.ru/MARKETPROJECT-1187
        """
        cls.index.shops += [
            Shop(fesh=10900 + i, priority_region=213, phone="+7222998989", phone_display_options='*') for i in range(10)
        ]

        cls.index.models += [
            Model(hyperid=901, hid=ALCOHOL_VINE_CATEG_ID, title="liquor Jagermeister model"),
            Model(hyperid=902, title="liquor Jagermeister shot model 0"),
            Model(hyperid=903, title="liquor Jagermeister shot model 1"),
        ]
        cls.index.offers += [
            Offer(
                hid=ALCOHOL_VINE_CATEG_ID,
                alcohol=True,
                title="liquor Jagermeister offer 0",
                hyperid=901,
                fesh=10900 + 0,
            ),
            Offer(
                hid=ALCOHOL_VINE_CATEG_ID,
                alcohol=True,
                title="liquor Jagermeister offer 1",
                hyperid=901,
                fesh=10900 + 1,
            ),
            Offer(
                hid=ALCOHOL_VINE_CATEG_ID,
                alcohol=True,
                title="liquor Jagermeister offer 2",
                hyperid=901,
                fesh=10900 + 2,
            ),
        ]
        cls.index.offers += [
            Offer(title="liquor Jagermeister shot offer {}".format(i), hyperid=902, fesh=10900 + 3 + i)
            for i in range(3)
        ]
        cls.index.offers += [
            Offer(title="liquor Jagermeister shot offer {}".format(i), hyperid=903, fesh=10900 + 3 + i)
            for i in range(3, 6)
        ]

        cls.index.offers += [
            Offer(title='Книга про приготовление домашнего вина', fesh=10900),
            Offer(title='Домашнее вино. Все секреты приготовления', fesh=10901),
            Offer(title='Как приготовить настоящее вино в домашних условиях', fesh=10902),
        ]

        cls.index.models += [
            Model(hyperid=910, hid=ALCOHOL_VINE_CATEG_ID, title="Cointreau model"),
            Model(hyperid=911, title="Cointreau shot model"),
        ]
        cls.index.offers += [
            Offer(hid=ALCOHOL_VINE_CATEG_ID, alcohol=True, title="Cointreau offer 0", hyperid=910),
            Offer(hid=ALCOHOL_VINE_CATEG_ID, alcohol=True, title="Cointreau offer 1", hyperid=910),
            Offer(hid=ALCOHOL_VINE_CATEG_ID, alcohol=True, title="Cointreau offer 2", hyperid=910),
        ]

    def test_parallel_offers_wizard(self):
        """Проверяем, что в оферном врезка не показывается, если доля алкоголя
        превышает заданную в market_offers_incut_alcohol_proportion.
        https://st.yandex-team.ru/MARKETPROJECT-1187
        """

        class TraceCode:
            SUCCESS = 2
            ERROR = 4

        request = 'place=parallel&text=Jagermeister&trace_wizard=1'
        # запрос считается алкогольным - врезки нет
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": EmptyList(),
                    },
                    "text": [{"__hl": {"text": "6 магазинов. Выбор по параметрам.", "raw": True}}],
                    "button": [{"text": "Еще 6 предложений"}],
                    "offer_count": 6,
                }
            },
        )
        self.assertIn('{} Алкоголь: 3<=0.3*9'.format(TraceCode.ERROR), response.get_trace_wizard())

        request = 'place=parallel&text=Jagermeister&trace_wizard=1'
        # запрос не считается алкогольным - врезка есть (но без алкоголя)
        response = self.report.request_bs_pb(request + '&rearr-factors=market_offers_incut_alcohol_proportion=0.4')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {
                                        "__hl": {"text": "liquor Jagermeister shot offer {}".format(i), "raw": True}
                                    }
                                }
                            }
                            for i in range(6)
                        ],
                    },
                    "text": [{"__hl": {"text": "6 магазинов. Выбор по параметрам.", "raw": True}}],
                    "button": [{"text": "Еще 6 предложений"}],
                    "offer_count": 6,
                }
            },
            preserve_order=False,
            allow_different_len=False,
        )
        self.assertIn('{} Алкоголь: 3<=0.4*9'.format(TraceCode.SUCCESS), response.get_trace_wizard())

        request = 'place=parallel&text=Cointreau&trace_wizard=1'
        response = self.report.request_bs_pb(request)
        # Есть только алкогольные оферы - показывается их количество, не показывается количество магазинов.
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": EmptyList(),
                    },
                    "text": [{"__hl": {"text": "Выбор по параметрам.", "raw": True}}],
                    "button": [{"text": "Еще 3 предложения"}],
                    "offer_count": 3,
                }
            },
        )
        self.assertIn('Оферы не найдены, но есть алкоголь', response.get_trace_wizard())

        # Проверяем, что при наличии только алкоголя пороги не проверяются
        response = self.report.request_bs_pb(request + '&rearr-factors=market_offers_wiz_top_offers_threshold=2.4;')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": EmptyList(),
                    },
                    "text": [{"__hl": {"text": "Выбор по параметрам.", "raw": True}}],
                    "button": [{"text": "Еще 3 предложения"}],
                    "offer_count": 3,
                }
            },
        )
        self.assertIn('Пропускаем фильтрующие формулы - найден только алкоголь', response.get_trace_wizard())

    @classmethod
    def prepare_parallel_no_buy_words(cls):
        # имя региона
        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        cls.index.alcohol_queries += ['вино домашнее']

    def test_parallel_no_buy_words(self):
        """Проверяем, что при наличии алкоголя в результатах поиска или
        или если запрос из алкогольного списка
        из запроса вырезаются слова,
        связанные с покупкой, и не пишем про доставку в сниппете.
        https://st.yandex-team.ru/MARKETPROJECT-1187
        """
        request_wizard_rules = (
            '{"Market": {"MaxWeight": "316392846", "MinWeight": "10910098", "NotFoundRate": "0.50", "AvgWeight": "163651472.00", "MarketIntent": "0.40", '
            '"qtree4market": "cHicxZZNaBNBFMffm8224zRqSK2GATUEoaughOKhCH4gEYqIlJ40INjQ0FRqxa1i6cVaKbYV1Cp6kB5E7ZdgWxsKVesHiKCSwwYUD2K9eROvXgRndjfpZrM1r'
            'UgaCDO7895_3_u9P5uww8xPIQAhCBONRCEIHCKwA-pgT-4-aBCFA2qD2gjH4CSk8AbCXYT7CDMILxDE5x2CgS38GmHHbTkq0qRcRXvb2fNndI7hCIZt2QqHLDSAlE09_J5mOV07x6U'
            'ehXo8OIoUA8DtiAhoGMXGW8SqS9_P7IMQSvF6aML-OBmZTIgnctCqE1SspLZTUxKVAcLR2gDHWk1JQRfRY8UCA3FyLx3HJ4UayRaRukZoKG0draaKYspJlW5CSQ-C6IZ_Q5Z0AfGfa'
            'm5N6qeTbZ3nkrpgnafi96AyfGfhMclhKcj0gtNusikIcxM6xAqOQ_J4hZzQ2WFkLb0Y6MEQCSuaGpXIMP6ZUFrTu2FXjL8lrNnVPjOeG3PGM2M228NxewQ1u3vVo_tf6St5TzjyvFr'
            '_ZPnCEWV3_iDX-QAyx2kI7Pkq_a8_xMn4ZHH7xrTo2trN53dz5o5wku3L7YxZsWMB5D7jpTFj3sV8joy8nLuXvWoaBPVtzL9YijGdnwKIClBWkKJdqoCMNmSfRKhBfEGSPfF7d4xnC'
            'Ot2kd1ozBppU3Be7J5mL2V7iyhXlfLYEiJeyN9YyJfIcOMfQrZEpHMU78s5hgLAJEAE4J-qADzStyXGv6rFgLPX8_VP_zNgbxEvwB99FmDvDDfg2wKwd6R4l-HqeB2F12-WrsvXP1z'
            'GycuihpYFq7w1JZbD6ZWo6dGU-Iohjk9ZwxwT65hYR8U6KtYJsU7IIU8565fP8HD8D-l4gAsx_kVlXX95pTgdv_gPYUUvlGX63TvDy-_ekavv9xJ1rY7fS8Mqu99Lc_rPfveJq0Hcy'
            'swf1OAmiryaKjWZzJG9m-s61u8Lw07pyUGsNiNIsIrSpkqKQeVoY3IQ_Xaej67TwboUotaleMYfoERDLg,,", "NonBuyStopwordsQuery": "liquor jagermeister", "NonR'
            'egionQuery": "купить liquor jagermeister", "WordNotFound": "1", "MatchedFlags": ["stop_word", "buy_stop_word"]}}'
        )
        response = self.report.request_bs_pb(
            'place=parallel&text=купить+liquor+Jagermeister&rids=213&wizard-rules={}&rearr-factors=market_parallel_wizard=1'.format(
                urllib.quote(request_wizard_rules)
            )
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "\7[Liquor jagermeister\7] Москва",
                    "text": [
                        {
                            "__hl": {
                                "text": "6 магазинов. Выбор по параметрам. Выбор в магазинах Москва и других регионов.",
                                "raw": True,
                            }
                        }
                    ],
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "title": "\7[Liquor jagermeister\7] Москва",
                }
            },
        )

        # в [купить домашнее вино] подходит под стоп-запрос [вино домашнее]
        request_wizard_rules = (
            '{"Market": {"MaxWeight": "848238", "FoundMainCategories": ["486680", "16155466" ], "MinWeight": "652356", "NotFoundRate": "0.00", "AvgWeight": "750297.00", "FoundExtraCategories": ["486680", "16155466"], "MarketIntent": "0.03", '  # noqa
            '"qtree4market": "cHicjVRNaBNBFH5vdtNOxyBL0mBcCIYgdBHU6KkU2mrxUES0FA-yitrQ0gjaQgql2EtEhFCxCoIH8aQUCmqo1UILzWoFQUsOk4M3KYgHb97FQ52d_Us3W20Oycx73_fem--bCTvL4jSmtaUhiwbJQwJ0yMEROAk9cQoaiDgYkIdTscHYEFyC61DERwhPEZ4jLCPUEMTnMwLHUX1LYTeZQ6'  # noqa
            'OCZpfT-Dtu8XW-1KjwGl_hKzpmc5h1G9CmBjAIdoPit0_jXoMWcqhhHrpx4AehqIHegs2BgXkcekGcoUsGa4GkbQh0wzAU2kQJMJJFOgOlByQEXeVraTmkgGLZVBerqxumuiC-C1QjOuErhlJgooLK33JLrL1oUqxQrNZkzF6tyxXopHHXwzXmfcaql5UVd-W6OJt7z8s2HsoZiK425oOKbjQ0mYzqGtU7-Xtxz'  # noqa
            'JAqzZ2LMIO3hcBlBGGK_oqwyyGDKV8W-tS4Je6N76sa4WulccHz1edE-fkRpZ8-JuxjjnmppSb_nIkt6WCsNOtjLN84pbKxaZLFqkkWqsJuFHZ7ci75q90lJjtMsaS1AYM4hu6UKxenikbTJKsYsbw9BJo_VZpNwYfpM_pXlQ0y74mRvTy9fzy8l4RdC_nSMTp5a2SqODE2Zr84r2xHhDHPnmy9Jl7pgBblTc3x'  # noqa
            'JgC55jz2zDnOglwaXXdIZcHEqvfECu3iF6eEWDhDSgNRBKy4JjkEKXSXYAgqkVSnRpdb43BkjbIZvGqBErYQ9xZbyC6G1FKnb0xM_u8Gb9bu-DpJQpREV6RCMh0Wp5fJcPBXsvdjQvMBchrd3la0clkVd0s1FOduuTlVnIsYqtjN4SFm7yBxgKKepEpqtvNYXyb1p9ifhaP22GHEwVS9fq438-t3C4K4CC31pV7'  # noqa
            'vy7yxru5EkCbE6e89_Zn7J8YDRNKtsY_S4XaKCeX80Mgcxt3eKt1fAmdrl5FbcZK_SL-Gsw,,", "NonBuyStopwordsQuery": "домашнее вино", "NonRegionQuery": "купить домашнее вино", "WordNotFound": "0", "MatchedFlags": ["stop_word", "buy_stop_word"]}}'  # noqa
        )
        response = self.report.request_bs_pb(
            'place=parallel&text=купить+домашнее+вино&rids=213&wizard-rules={}&rearr-factors=market_parallel_wizard=1'.format(
                urllib.quote(request_wizard_rules)
            )
        )

        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "title": "\7[Домашнее вино\7] Москва",
                    "text": [
                        {
                            "__hl": {
                                "text": "3 магазина. Выбор по параметрам. Выбор в магазинах Москва и других регионов.",
                                "raw": True,
                            }
                        }
                    ],
                    "showcase": {"items": EmptyList()},  # врезка по алкогольным запросам не показывается
                }
            },
        )

    @classmethod
    def prepare_alco_license_outlet(cls):
        cls.index.shops += [
            Shop(
                fesh=552414,
                alcohol_status=Shop.ALCOHOL_SANDBOX,
                datafeed_id=3,
                priority_region=213,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=166,
                phone=None,
                fulfillment_program=False,
            ),
            Shop(
                fesh=431782,
                alcohol_status=Shop.ALCOHOL_REAL,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                warehouse_id=145,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=1,
                hid=ALCOHOL_VINE_CATEG_ID,
                feedid=3,
                is_fulfillment=False,
                has_delivery_options=False,
                weight=1,
                dimensions=OfferDimensions(length=8, width=8, height=60),
                blue_offers=[
                    BlueOffer(
                        price=10,
                        price_old=13,
                        vat=Vat.VAT_18,
                        offerid='Vindigo',
                        title="Blue wine",
                        waremd5='BlueAlcoholOffer_____g',
                        pickup_buckets=[52],
                    ),
                    BlueOffer(
                        price=5,
                        price_old=7,
                        vat=Vat.VAT_18,
                        offerid='Vindigo1',
                        waremd5='NoLicensedDelivery___g',
                        pickup_buckets=[541],
                    ),
                    BlueOffer(
                        price=7,
                        price_old=10,
                        vat=Vat.VAT_18,
                        offerid='Vindigo2',
                        waremd5='MixedOutletsBlueOfferg',
                        pickup_buckets=[541, 551],
                    ),
                ],
            ),
            MarketSku(
                hyperid=2,
                sku=2,
                hid=1,
                blue_offers=[
                    BlueOffer(
                        price=2,
                        vat=Vat.VAT_18,
                        offerid='Not alcohol',
                        feedid=3,
                        is_fulfillment=False,
                        waremd5='BlueNonAlcoholOffer__g',
                        pickup_buckets=[52],
                    )
                ],
            ),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=52,
                fesh=552414,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                options=[PickupOption(outlet_id=66843529, day_from=2)],
            ),
            PickupBucket(
                bucket_id=53,
                fesh=549083,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                options=[PickupOption(outlet_id=outlet.point_id) for outlet in _Outlets.all_outlets],
            ),
            # outlets without a valid license
            PickupBucket(
                bucket_id=54,
                fesh=549083,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                options=[
                    PickupOption(outlet_id=o.point_id, day_from=2)
                    for o in (_Outlets.outlet_alcohol_license_unknown, _Outlets.outlet_alcohol_license_new)
                ],
            ),
            PickupBucket(
                bucket_id=541,
                fesh=552414,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                options=[
                    PickupOption(outlet_id=o.point_id, day_from=2)
                    for o in (_Outlets.outlet_alcohol_license_unknown, _Outlets.outlet_alcohol_license_new)
                ],
            ),
            # outlet with valid license
            PickupBucket(
                bucket_id=55,
                fesh=549083,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                options=[PickupOption(outlet_id=_Outlets.alcodata_and_mbi_outlet.point_id, day_from=2)],
            ),
            PickupBucket(
                bucket_id=551,
                fesh=552414,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                options=[PickupOption(outlet_id=_Outlets.alcodata_and_mbi_outlet.point_id, day_from=2)],
            ),
        ]

    def test_alco_discount_filter(self):
        """Тест на выдачу фильров на скидку"""
        offer_request = 'place=prime&rgb=blue&pp=18&offerid=BlueAlcoholOffer_____g&adult=1'
        self.assertFragmentNotIn(self.report.request_json(offer_request), {'filters': [{"id": "filter-discount-only"}]})

    def test_alco_license_is_shown_on_blue_market(self):
        """Лицензия на торговлю алкоголем должна отображаться для аутлетов всегда"""
        outlets_request = 'place=outlets&outlets={}&bsformat=2'.format(_Outlets.blue_outlet.point_id)
        self.assertFragmentIn(
            self.report.request_json(outlets_request),
            {
                "entity": "outlet",
                "id": str(_Outlets.blue_outlet.point_id),
                "legalInfo": {"licence": {"number": u"11ЛИЦ3434901"}},
            },
        )

        legal_info = _AlcoData.legal_info(_Outlets.blue_outlet.point_id)

        for color in ['&rgb=blue', '&rgb=green']:
            for offer, is_alcohol in [
                # на синем маркете отображаем legalInfo всегда
                ('BlueNonAlcoholOffer__g', False),
                ('BlueAlcoholOffer_____g', True),
            ]:
                expected_legal_info = {
                    'organizationType': legal_info.type or "",
                    'organizationName': legal_info.name or "",
                    'juridicalAddress': legal_info.legal_address or "",
                    'factAddress': legal_info.actual_address or "",
                    'registrationNumber': legal_info.number or "",
                    "licence": {"number": u"11ЛИЦ3434901"} if is_alcohol else NoKey("licence"),
                }
                self.assertFragmentIn(
                    self.report.request_json(
                        "place=offerinfo&offerid={}&regset=2&rids=213&show-urls=direct{}&adult=1".format(offer, color)
                    ),
                    {
                        "entity": "outlet",
                        "id": str(_Outlets.blue_outlet.point_id),
                        "legalInfo": expected_legal_info,
                    },
                )

    def test_blue_alco_offers_are_shown_on_white_market(self):
        """От юристов получили ок на показ синих офферов на белом маркете"""
        request = 'place=prime&adult=1&offerid=BlueAlcoholOffer_____g&debug=1'

        for rgb in ['', '&rgb=blue']:
            # и на синем и на белом офферы отображается нормально
            response = self.report.request_json(request + rgb)
            self.assertFragmentIn(response, {"entity": "offer", "wareId": "BlueAlcoholOffer_____g"})

    def test_license_fallback(self):
        """Основной источник юринфо и лицензий для ПВЗ - выгрузка shopsOutlet от MBI
        В качестве временного костыля использовали хардкод этих данных в репорте (alcodata)
        Здесь проверяем, что в первую очередь смотрим на выгрузку MBI, затем - в alcodata
        """

        def convert_date(date_str):
            months = (
                u'',
                u'января',
                u'февраля',
                u'марта',
                u'апреля',
                u'мая',
                u'июня',
                u'июля',
                u'августа',
                u'сентября',
                u'октября',
                u'ноября',
                u'декабря',
            )
            year, month, day = date_str.split('-')
            return ' '.join((day, months[int(month)], year, u'г.'))

        for outlet in _Outlets.all_outlets:
            outlets_request = 'place=outlets&outlets={}&bsformat=2&rgb=blue'.format(outlet.point_id)
            legal_info = outlet.legal_info or _AlcoData.legal_info(outlet.point_id)
            license = get_licence(outlet) or _AlcoData.license(outlet.point_id)

            self.assertFragmentIn(
                self.report.request_json(outlets_request),
                {
                    "entity": "outlet",
                    "id": str(outlet.point_id),
                    "legalInfo": NoKey('legalInfo')
                    if legal_info is None
                    else {
                        'organizationType': legal_info.type or "",
                        'organizationName': legal_info.name or "",
                        'juridicalAddress': legal_info.legal_address or "",
                        'factAddress': legal_info.actual_address or "",
                        'registrationNumber': legal_info.number or "",
                        "licence": NoKey('licence')
                        if license is None
                        else {
                            "number": license.number,
                            "startDate": convert_date(license.issue_date),
                            "endDate": convert_date(license.expiry_date),
                            "type": license.type,
                            "status": license.status,
                        },
                    },
                },
            )

    def test_filter_outlets_without_valid_license(self):
        """Проверяем, что точки ПВЗ без действующей лицензии на продажу алкоголя не попадают на выдачу"""

        def blue(request):
            return request + "&rgb=blue"

        def response_with_offer(request, offer):
            return self.report.request_json(request + "&offerid=" + offer)

        def check_no_offer_in_response(request, offer):
            self.assertFragmentNotIn(response_with_offer(request, offer), {"entity": "offer", "wareId": offer})

        def check_offer_in_response(request, offer):
            self.assertFragmentIn(response_with_offer(request, offer), {"entity": "offer", "wareId": offer})

        def check_outlet_in_response(request, offer, outlet):
            self.assertFragmentIn(
                response_with_offer(request, offer),
                {"entity": "offer", "wareId": offer, "outlet": {"id": str(outlet.point_id)}},
            )

        def check_place(request_template):
            request = request_template + '&rids=213&adult=1'
            # Отбрасывается, т.к. все точки не имеют лицензии
            check_no_offer_in_response(request, "UnlicensedWhiteWine__g")
            check_no_offer_in_response(blue(request), "NoLicensedDelivery___g")
            # Есть множество точек с лицензией
            check_offer_in_response(request, "LicensedWhiteWine____g")
            check_offer_in_response(blue(request), "BlueAlcoholOffer_____g")
            # Здесь в качестве точки может выбраться только одна, с лицензией (хотя в бакетах их 3)
            check_outlet_in_response(request, "MixedLicenseWhiteWineg", _Outlets.alcodata_and_mbi_outlet)
            check_outlet_in_response(blue(request), "MixedOutletsBlueOfferg", _Outlets.alcodata_and_mbi_outlet)

        check_place("place=offerinfo&regset=2&show-urls=direct")
        check_place("place=prime&regset=2&show-urls=direct")
        check_place("place=geo&show-outlet=offers")

        # actual_delivery
        delivery_request = "place=actual_delivery&rgb=blue&adult=1&offers-list={}:1&rids=213"
        check_no_offer_in_response(delivery_request.format("NoLicensedDelivery___g"), "NoLicensedDelivery___g")
        check_offer_in_response(delivery_request.format("BlueAlcoholOffer_____g"), "BlueAlcoholOffer_____g")
        check_outlet_in_response(
            delivery_request.format("MixedOutletsBlueOfferg"),
            "MixedOutletsBlueOfferg",
            _Outlets.alcodata_and_mbi_outlet,
        )

        # sku_offers
        sku_offers_request = "place=sku_offers&rgb=blue&adult=1&market-sku=1&rids=213"
        check_no_offer_in_response(sku_offers_request, "NoLicensedDelivery___g")
        check_offer_in_response(sku_offers_request, "BlueAlcoholOffer_____g")
        check_outlet_in_response(sku_offers_request, "MixedOutletsBlueOfferg", _Outlets.alcodata_and_mbi_outlet)


if __name__ == '__main__':
    main()
