#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    HyperCategory,
    MnPlace,
    Model,
    NewShopRating,
    Offer,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare_glfilters_delimiter_offers(cls):
        cls.disable_randx_randomize()
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.gltypes += [
            GLType(param_id=3001, hid=90900, gltype=GLType.ENUM, cluster_filter=True, values=[1, 2, 3]),
            GLType(param_id=3002, hid=90900, gltype=GLType.ENUM, cluster_filter=True, values=[1, 2, 3]),
        ]

        cls.index.shops += [
            Shop(fesh=30011, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=3.0)),
            Shop(fesh=30012, priority_region=9, regions=[213], new_shop_rating=NewShopRating(new_rating_total=4.0)),
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1001,
                fesh=30012,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)])],
            )
        ]

        cls.index.offers += [
            Offer(hyperid=200101, fesh=30011, price=900, title='offer10', glparams=[GLParam(param_id=3001, value=1)]),
            Offer(hyperid=200101, fesh=30011, price=700, title='offer10a', glparams=[GLParam(param_id=3001, value=1)]),
            Offer(hyperid=200101, fesh=30011, price=2700, title='offer01', glparams=[GLParam(param_id=3002, value=1)]),
            Offer(hyperid=200101, fesh=30011, price=800, title='offer20', glparams=[GLParam(param_id=3001, value=2)]),
            Offer(
                hyperid=200101,
                fesh=30011,
                price=1700,
                title='offer-discount01',
                glparams=[GLParam(param_id=3002, value=1)],
                discount=12,
            ),
            Offer(
                hyperid=200101,
                fesh=30012,
                price=950,
                title='regional-offer10',
                glparams=[GLParam(param_id=3001, value=1)],
                delivery_buckets=[1001],
            ),
            Offer(
                hyperid=200101,
                fesh=30012,
                price=850,
                title='regional-offer02',
                glparams=[GLParam(param_id=3002, value=2)],
                delivery_buckets=[1001],
            ),
            Offer(
                hyperid=200101,
                fesh=30012,
                price=750,
                title='regional-offer-discount22',
                glparams=[GLParam(param_id=3001, value=2), GLParam(param_id=3002, value=2)],
                delivery_buckets=[1001],
                discount=10,
            ),
            Offer(
                hyperid=200101,
                fesh=30012,
                price=1250,
                title='regional-offer-discount-10',
                glparams=[GLParam(param_id=3001, value=1)],
                delivery_buckets=[1001],
                discount=8,
            ),
        ]

    def test_glfilters_delimiter_productoffers(self):
        """
        проверяем базовые кейсы наличия/отсутсвия черты про фильтрацию в зависимости от флагов
        """
        req = "place=productoffers&offer-set=all&hyperid=200101&rids=213&local-offers-first=0&hid=90900"

        # has offer with both filter
        response = self.report.request_json(req + "&glfilter=3001:2;3002:2")
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "totalPassedAllGlFilters": 1,
                "results": [
                    {"titles": {"raw": "regional-offer-discount22"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # CGI esqaped both filter MARKETOUT-27050
        response = self.report.request_json(req + "&glfilter=3001%253A2%253B3002%253A2")
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "totalPassedAllGlFilters": 1,
                "results": [
                    {"titles": {"raw": "regional-offer-discount22"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            req + "&glfilter=3001:2;3002:2" + "&rearr-factors=market_glfilter_delimiter=1"
        )
        self.assertFragmentIn(
            response,
            {
                "total": 3,
                "totalPassedAllGlFilters": 1,
                "results": [
                    {"titles": {"raw": "regional-offer-discount22"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "regional-offer02"}, "hasRelaxedGlFilter": True},
                    {"titles": {"raw": "offer20"}, "hasRelaxedGlFilter": True},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # has offer with one filter only
        response = self.report.request_json(req + "&glfilter=3001:3;3002:2")
        self.assertFragmentNotIn(response, {"entity": "offer"})

        response = self.report.request_json(
            req + "&glfilter=3001:3;3002:2" + "&rearr-factors=market_glfilter_delimiter=1"
        )
        # whould be like this, but we prohibit empty primary result (MARKETOUT-27835)
        # self.assertFragmentIn(response, {
        #    "results": [
        #        {"entity": "passFiltersDelimiter"},
        #        {"titles": {"raw": "regional-offer02"}, "hasRelaxedGlFilter": True},
        #    ]
        # }, preserve_order=True, allow_different_len=False)
        self.assertFragmentNotIn(response, {"entity": "offer"})
        self.assertFragmentIn(
            response,
            {
                "total": 0,
                "totalPassedAllGlFilters": 0,
            },
        )

        # has no offers with any filter
        response = self.report.request_json(
            req + "&rearr-factors=market_glfilter_delimiter=1" + "&glfilter=3001:3;3002:3"
        )
        self.assertFragmentNotIn(response, {"entity": "offer"})

    def test_glfilters_delimiter_productoffers_mix_regional_sorts(self):
        """
        Проверяем наличие черты при фильтрации по гл фильтрам для сортировок в разных регионах:
         - по умолчанию
         - по цене по возрастанию
         - по цене по убыванию
         - по рейтингу и цене
         - по размеру скидки
        """
        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&local-offers-first=0&hid=90900"  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"titles": {"raw": "regional-offer-discount-10"}},
                    {"titles": {"raw": "regional-offer10"}},
                    {"titles": {"raw": "offer10a"}},
                    {"titles": {"raw": "offer10"}},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "regional-offer02"}},
                    {"titles": {"raw": "offer-discount01"}},
                    {"titles": {"raw": "offer01"}},
                ],
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&hid=90900&local-offers-first=0&how=aprice"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"titles": {"raw": "offer10a"}},
                    {"titles": {"raw": "offer10"}},
                    {"titles": {"raw": "regional-offer10"}},
                    {"titles": {"raw": "regional-offer-discount-10"}},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "regional-offer02"}},
                    {"titles": {"raw": "offer-discount01"}},
                    {"titles": {"raw": "offer01"}},
                ],
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&hid=90900&local-offers-first=0&how=dprice"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"titles": {"raw": "regional-offer-discount-10"}},
                    {"titles": {"raw": "regional-offer10"}},
                    {"titles": {"raw": "offer10"}},
                    {"titles": {"raw": "offer10a"}},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "offer01"}},
                    {"titles": {"raw": "offer-discount01"}},
                    {"titles": {"raw": "regional-offer02"}},
                ],
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&hid=90900&local-offers-first=0&how=rorp"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"titles": {"raw": "offer10a"}},
                    {"titles": {"raw": "offer10"}},
                    {"titles": {"raw": "regional-offer10"}},
                    {"titles": {"raw": "regional-offer-discount-10"}},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "regional-offer02"}},
                    {"titles": {"raw": "offer-discount01"}},
                    {"titles": {"raw": "offer01"}},
                ],
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&hid=90900&local-offers-first=0&how=discount_p"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"titles": {"raw": "regional-offer-discount-10"}},
                    {"titles": {"raw": "regional-offer10"}},
                    {"titles": {"raw": "offer10a"}},
                    {"titles": {"raw": "offer10"}},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "offer-discount01"}},
                    {"titles": {"raw": "regional-offer02"}},
                    {"titles": {"raw": "offer01"}},
                ],
            },
            preserve_order=True,
        )

    def test_glfilters_delimiter_productoffers_mix_regional_paging(self):
        """
        Проверяем, что черта повторяется 1 раз при постраничной навигации
        """
        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&hid=90900&numdoc=2&local-offers-first=0&page=1"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"titles": {"raw": "regional-offer-discount-10"}},
                    {"titles": {"raw": "regional-offer10"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&hid=90900&numdoc=2&local-offers-first=0&page=2"  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"titles": {"raw": "offer10a"}},
                    {"titles": {"raw": "offer10"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&hid=90900&numdoc=2&local-offers-first=0&page=3"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "regional-offer02"}},
                    {"titles": {"raw": "offer-discount01"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&hid=90900&numdoc=2&local-offers-first=0&page=4"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"titles": {"raw": "offer01"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_glfilters_delimiter_productoffers_regional_regionaldelimiter(self):
        """
        1. Проверяем, что при наличии региональной черты - сначала видим региональную черту, потом черту для фильтров
        2. Проверяем, что при постраничной навигации порядок сохраняется
        """
        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&local-offers-first=1&hid=90900"  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "total": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"titles": {"raw": "offer10a"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                    {"titles": {"raw": "offer10"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                    {"entity": "regionalDelimiter"},
                    {
                        "titles": {"raw": "regional-offer-discount-10"},
                        "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter"),
                    },
                    {"titles": {"raw": "regional-offer10"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "offer-discount01"}, "hasRelaxedGlFilter": True},
                    {"titles": {"raw": "offer01"}, "hasRelaxedGlFilter": True},
                    {"titles": {"raw": "regional-offer02"}, "hasRelaxedGlFilter": True},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&local-offers-first=1&hid=90900&numdoc=2&page=1"  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "total": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"titles": {"raw": "offer10a"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                    {"titles": {"raw": "offer10"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # региональная черта не переносится
        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&local-offers-first=1&hid=90900&numdoc=2&page=2"  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "total": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"entity": "regionalDelimiter"},
                    {
                        "titles": {"raw": "regional-offer-discount-10"},
                        "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter"),
                    },
                    {"titles": {"raw": "regional-offer10"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # черта для фильтров отображается даже первой на странице
        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&local-offers-first=1&hid=90900&numdoc=2&page=3"  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "total": 7,
                "totalPassedAllGlFilters": 4,
                "results": [
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "offer-discount01"}, "hasRelaxedGlFilter": True},
                    {"titles": {"raw": "offer01"}, "hasRelaxedGlFilter": True},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # черта для фильтров не повторяется
        response = self.report.request_json(
            "place=productoffers&offer-set=all&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0;market_glfilter_delimiter=1&glfilter=3001:1&hyperid=200101&rids=213&local-offers-first=1&hid=90900&numdoc=2&page=4"  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "total": 7,
                "totalPassedAllGlFilters": 4,
                "results": [{"titles": {"raw": "regional-offer02"}, "hasRelaxedGlFilter": True}],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_glfilters_delimiter_models(cls):

        cls.index.hypertree += [
            HyperCategory(hid=34567),
        ]

        cls.index.gltypes += [
            GLType(param_id=3001, hid=34567, gltype=GLType.ENUM, values=[1, 2, 3]),
            GLType(param_id=3002, hid=34567, gltype=GLType.ENUM, values=[1, 2, 3]),
        ]

        cls.index.models += [
            Model(hid=34567, title='super model 102', hyperid=102, glparams=[GLParam(param_id=3002, value=2)], ts=102),
            Model(hid=34567, title='super model 110', hyperid=110, glparams=[GLParam(param_id=3001, value=1)], ts=110),
            Model(hid=34567, title='super model 120', hyperid=120, glparams=[GLParam(param_id=3001, value=2)], ts=120),
            Model(hid=34567, title='super model 101', hyperid=101, glparams=[GLParam(param_id=3002, value=1)], ts=101),
            Model(
                hid=34567,
                title='super model 122',
                hyperid=122,
                glparams=[GLParam(param_id=3001, value=2), GLParam(param_id=3002, value=2)],
                ts=122,
            ),
            Model(
                hid=34567,
                title='super model 1220',
                hyperid=1220,
                glparams=[GLParam(param_id=3001, value=2), GLParam(param_id=3002, value=2)],
                ts=1220,
            ),  # no offers
        ]

        cls.index.offers += [
            Offer(
                hyperid=110,
                hid=34567,
                fesh=30011,
                price=500,
                title='model offer10',
                ts=210,
                glparams=[GLParam(param_id=3001, value=1)],
            ),
            Offer(
                hyperid=120,
                hid=34567,
                fesh=30011,
                price=600,
                title='model offer20',
                ts=220,
                glparams=[GLParam(param_id=3001, value=2)],
                waremd5="RPaDqEFjs1I6_lfC4Ai8jA",
            ),
            Offer(
                hyperid=101,
                hid=34567,
                fesh=30011,
                price=700,
                title='model offer01',
                ts=201,
                glparams=[GLParam(param_id=3002, value=1)],
            ),
            Offer(
                hyperid=102,
                hid=34567,
                fesh=30011,
                price=800,
                title='model offer12-102',
                ts=212,
                glparams=[GLParam(param_id=3001, value=1), GLParam(param_id=3002, value=2)],
            ),
            Offer(
                hyperid=122,
                hid=34567,
                fesh=30011,
                price=900,
                title='model offer22',
                ts=222,
                glparams=[GLParam(param_id=3001, value=2), GLParam(param_id=3002, value=2)],
                discount=10,
                waremd5='qtZDmKlp7DGGgA1BL6erMQ',
            ),
            Offer(
                hyperid=101,
                hid=34567,
                fesh=30011,
                price=1000,
                title='model offer20-101',
                ts=2201,
                glparams=[GLParam(param_id=3001, value=2)],
            ),
            Offer(
                hyperid=110,
                hid=34567,
                fesh=30012,
                price=1100,
                title='regional-model-offer10',
                ts=310,
                glparams=[GLParam(param_id=3001, value=1)],
                delivery_buckets=[1001],
            ),
            Offer(
                hyperid=120,
                hid=34567,
                fesh=30012,
                price=1200,
                title='regional-model-offer20',
                ts=320,
                glparams=[GLParam(param_id=3001, value=2)],
                discount=12,
                delivery_buckets=[1001],
            ),
            Offer(
                hyperid=122,
                hid=34567,
                fesh=30012,
                price=1300,
                title='regional-model-offer22',
                ts=322,
                glparams=[GLParam(param_id=3001, value=2), GLParam(param_id=3002, value=2)],
                delivery_buckets=[1001],
            ),
            Offer(
                hid=34567,
                fesh=30012,
                price=1400,
                title='no-model-offer22',
                ts=1,
                glparams=[GLParam(param_id=3001, value=2), GLParam(param_id=3002, value=2)],
                delivery_buckets=[1001],
            ),
        ]

        tss = [1, 102, 110, 120, 101, 122, 210, 220, 201, 212, 222, 2201, 310, 320, 322, 1220]
        for ts in tss:
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(ts / 10000.0)

    def test_glfilters_delimiter_prime(self):
        req = 'place=prime&hid=34567'
        """
            "обычный" вариант: только то что подходит под оба фильтра
        """
        req += "&glfilter=3001:2;3002:2"
        expected = {
            "total": 5,
            "totalPassedAllGlFilters": 5,
            "results": [
                {"titles": {"raw": "super model 1220"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"titles": {"raw": "super model 122"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"titles": {"raw": "regional-model-offer22"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"titles": {"raw": "model offer22"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"titles": {"raw": "no-model-offer22"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
            ],
        }
        response = self.report.request_json(req)
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)
        response = self.report.request_json(req + "&local-offers-first=0")
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)

        """
            добавляется то что подходит под один фильтр, при отсутствии второго
        """
        req += "&rearr-factors=market_glfilter_delimiter=1"
        expected = {
            "total": 9,
            "totalPassedAllGlFilters": 5,
            "results": [
                {"titles": {"raw": "super model 1220"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"titles": {"raw": "super model 122"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"titles": {"raw": "regional-model-offer22"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"titles": {"raw": "model offer22"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"titles": {"raw": "no-model-offer22"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"entity": "passFiltersDelimiter"},
                {"titles": {"raw": "super model 120"}, "hasRelaxedGlFilter": True},
                {"titles": {"raw": "super model 102"}, "hasRelaxedGlFilter": True},
                {"titles": {"raw": "regional-model-offer20"}, "hasRelaxedGlFilter": True},
                {"titles": {"raw": "model offer20"}, "hasRelaxedGlFilter": True},
            ],
        }
        response = self.report.request_json(req)
        # + "&debug=1" + "&rearr-factors=market_documents_search_trace%3DRPaDqEFjs1I6_lfC4Ai8jA,qtZDmKlp7DGGgA1BL6erMQ")
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)
        response = self.report.request_json(req + "&local-offers-first=0")
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)

        """
            +onstock
            оффер от модели 102 не проходит фильтрацию, модель пропадает
            у модели 1220 нет офферов
        """
        req += "&onstock=1"
        expected = {
            "total": 7,
            "totalPassedAllGlFilters": 4,
            "results": [
                {"titles": {"raw": "super model 122"}},
                {"titles": {"raw": "regional-model-offer22"}},
                {"titles": {"raw": "model offer22"}},
                {"titles": {"raw": "no-model-offer22"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"entity": "passFiltersDelimiter"},
                {"titles": {"raw": "super model 120"}},
                {"titles": {"raw": "regional-model-offer20"}},
                {"titles": {"raw": "model offer20"}},
            ],
        }
        response = self.report.request_json(req)
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)
        response = self.report.request_json(req + "&local-offers-first=0")
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)

        response = self.report.request_json(
            "place=prime&hid=34567" + "&glfilter=3001:3;3002:2" + "&rearr-factors=market_glfilter_delimiter=1"
        )
        # whould be like this, but we prohibit empty primary result (MARKETOUT-27835)
        # self.assertFragmentIn(response, {
        #    "results": [
        #        {"entity": "passFiltersDelimiter"},
        #        {"titles": {"raw": "super model 102"}, "hasRelaxedGlFilter": True},
        #    ]
        # }, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(
            response,
            {
                "total": 0,
                "totalPassedAllGlFilters": 0,
            },
        )
        self.assertFragmentNotIn(response, {"entity": "offer"})
        self.assertFragmentNotIn(response, {"entity": "model"})

    def test_glfilters_delimiter_prime_collapsing(self):
        """
        проверяем, что получается при поиске
        """

        req = 'place=prime&hid=34567&text=model&use-default-offers=1&allow-collapsing=1'

        req += "&glfilter=3001:2;3002:2"
        expected = {
            "total": 3,
            "totalPassedAllGlFilters": 3,
            "results": [
                {"titles": {"raw": "super model 1220"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"titles": {"raw": "super model 122"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"titles": {"raw": "no-model-offer22"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
            ],
        }
        response = self.report.request_json(req)
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)
        response = self.report.request_json(req + "&local-offers-first=0")
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)

        req += "&rearr-factors=market_glfilter_delimiter=1"
        expected = {
            "total": 5,
            "totalPassedAllGlFilters": 3,
            "results": [
                {"titles": {"raw": "super model 1220"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"titles": {"raw": "super model 122"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"titles": {"raw": "no-model-offer22"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"entity": "passFiltersDelimiter"},
                {"titles": {"raw": "super model 120"}, "hasRelaxedGlFilter": True},
                {"titles": {"raw": "super model 102"}, "hasRelaxedGlFilter": True},
            ],
        }
        response = self.report.request_json(req)
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)
        response = self.report.request_json(req + "&local-offers-first=0")
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)

        req += "&onstock=1"
        expected = {
            "total": 3,
            "totalPassedAllGlFilters": 2,
            "results": [
                {"titles": {"raw": "super model 122"}},
                {"titles": {"raw": "no-model-offer22"}, "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter")},
                {"entity": "passFiltersDelimiter"},
                {"titles": {"raw": "super model 120"}},
            ],
        }
        response = self.report.request_json(req)
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)
        response = self.report.request_json(req + "&local-offers-first=0")
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)

    def test_glfilters_regional_delimiter_prime(self):
        """
        проверяем, что сначала идёт региональный разделитель, а уже потом - фильтровый
        """
        req = 'place=prime&hid=34567' + "&rids=213"
        """
            "обычный" вариант: только то что подходит под оба фильтра
        """
        req += "&glfilter=3001:2;3002:2"
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                "total": 5,
                "totalPassedAllGlFilters": 5,
                "results": [
                    {"titles": {"raw": "super model 122"}},
                    {"titles": {"raw": "model offer22"}},
                    {"entity": "regionalDelimiter"},
                    {"titles": {"raw": "regional-model-offer22"}},
                    {"titles": {"raw": "no-model-offer22"}},
                    {"titles": {"raw": "super model 1220"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        """
            добавляется то что подходит под один фильтр, при отсутствии второго
        """
        req += "&rearr-factors=market_glfilter_delimiter=1"
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                "total": 9,
                "totalPassedAllGlFilters": 5,
                "results": [
                    {"titles": {"raw": "super model 122"}},
                    {"titles": {"raw": "model offer22"}},
                    {"entity": "regionalDelimiter"},
                    {"titles": {"raw": "regional-model-offer22"}},
                    {"titles": {"raw": "no-model-offer22"}},
                    {"titles": {"raw": "super model 1220"}},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "super model 120"}},
                    {"titles": {"raw": "super model 102"}},
                    {"titles": {"raw": "model offer20"}},
                    {"titles": {"raw": "regional-model-offer20"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        """
            проверяем, что при листании порядок сохраняется
        """
        response = self.report.request_json(req + "&numdoc=2&page=2")
        self.assertFragmentIn(
            response,
            {
                "total": 9,
                "totalPassedAllGlFilters": 5,
                "results": [
                    {"entity": "regionalDelimiter"},
                    {"titles": {"raw": "regional-model-offer22"}},
                    {"titles": {"raw": "no-model-offer22"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(req + "&numdoc=2&page=3")
        self.assertFragmentIn(
            response,
            {
                "total": 9,
                "totalPassedAllGlFilters": 5,
                "results": [
                    {"titles": {"raw": "super model 1220"}},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "super model 120"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_glfilters_delimiter_prime_sort(self):
        """
        Проверяем наличие черты при фильтрации по гл фильтрам для сортировок:
         - по цене по возрастанию
         - по цене по убыванию
         - по рейтингу и цене
        """
        req = 'place=prime&hid=34567' + "&glfilter=3001:2;3002:2" + "&rearr-factors=market_glfilter_delimiter=1"

        response = self.report.request_json(req + "&how=aprice")
        self.assertFragmentIn(
            response,
            {
                "total": 9,
                "totalPassedAllGlFilters": 5,
                "results": [
                    {"titles": {"raw": "super model 122"}},
                    {"titles": {"raw": "model offer22"}},
                    {"titles": {"raw": "regional-model-offer22"}},
                    {"titles": {"raw": "no-model-offer22"}},
                    {"titles": {"raw": "super model 1220"}},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "super model 120"}},
                    {"titles": {"raw": "model offer20"}},
                    {"titles": {"raw": "regional-model-offer20"}},
                    {
                        "titles": {"raw": "super model 102"}
                    },  # по обновленным статистикам у нее нет офферов подходящих под параметры
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(req + "&how=dprice")
        self.assertFragmentIn(
            response,
            {
                "total": 9,
                "totalPassedAllGlFilters": 5,
                "results": [
                    {"titles": {"raw": "super model 122"}},
                    {"titles": {"raw": "no-model-offer22"}},
                    {"titles": {"raw": "regional-model-offer22"}},
                    {"titles": {"raw": "model offer22"}},
                    {"titles": {"raw": "super model 1220"}},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "super model 120"}},
                    {"titles": {"raw": "regional-model-offer20"}},
                    {"titles": {"raw": "model offer20"}},
                    {"titles": {"raw": "super model 102"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(req + "&how=rorp")
        self.assertFragmentIn(
            response,
            {
                "total": 9,
                "totalPassedAllGlFilters": 5,
                "results": [
                    {"titles": {"raw": "super model 122"}},
                    {"titles": {"raw": "model offer22"}},
                    {"titles": {"raw": "regional-model-offer22"}},
                    {"titles": {"raw": "no-model-offer22"}},
                    {"titles": {"raw": "super model 1220"}},
                    {"entity": "passFiltersDelimiter"},
                    {"titles": {"raw": "super model 120"}},
                    {"titles": {"raw": "super model 102"}},
                    {"titles": {"raw": "model offer20"}},
                    {"titles": {"raw": "regional-model-offer20"}},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_nofilter(self):
        req = 'place=prime&hid=34567&local-offers-first=1'
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                "total": 16,
                "totalPassedAllGlFilters": 16,
            },
        )

        req = 'place=prime&hid=34567&local-offers-first=0'
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                "total": 16,
                "totalPassedAllGlFilters": 16,
            },
        )

        response = self.report.request_json("place=productoffers&hyperid=200101&local-offers-first=1")
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 9,
                "totalPassedAllGlFilters": 9,
            },
        )

        response = self.report.request_json("place=productoffers&hyperid=200101&local-offers-first=0")
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 9,
                "totalPassedAllGlFilters": 9,
            },
        )

    @classmethod
    def prepare_glfilters_delimiter_bad_models(cls):

        cls.index.hypertree += [
            HyperCategory(hid=1243),
        ]

        cls.index.gltypes += [
            GLType(param_id=3001, hid=1243, gltype=GLType.ENUM, cluster_filter=True, values=[1, 2, 3]),
            GLType(param_id=3002, hid=1243, gltype=GLType.ENUM, cluster_filter=True, values=[1, 2, 3]),
        ]

        cls.index.models += [
            Model(
                hid=1243,
                title='super model 532',
                hyperid=532,
                glparams=[GLParam(param_id=3001, value=3), GLParam(param_id=3002, value=2)],
                ts=532,
            ),
            Model(hid=1243, title='super model 533', hyperid=533, glparams=[GLParam(param_id=3001, value=3)], ts=533),
        ]

        cls.index.offers += [
            Offer(
                hyperid=520,
                hid=1243,
                fesh=30011,
                price=600,
                title='model offer 520',
                ts=620,
                glparams=[GLParam(param_id=3001, value=2)],
            ),
            Offer(
                hyperid=532,
                hid=1243,
                fesh=30011,
                price=1500,
                title='model offer 532',
                ts=632,
                glparams=[GLParam(param_id=3001, value=3), GLParam(param_id=3002, value=2)],
            ),
            Offer(
                hyperid=533,
                hid=1243,
                fesh=30011,
                price=1600,
                title='bad model offer 530',
                ts=630,
                glparams=[GLParam(param_id=3001, value=3)],
            ),
        ]

    def test_glfilters_delimiter_prime_collapsing_bad_models(self):
        req = (
            'place=prime&hid=1243&text=offer' + '&glfilter=3001:3;3002:2' + '&rearr-factors=market_glfilter_delimiter=1'
        )

        expected_offers = {
            "total": 2,
            "totalPassedAllGlFilters": 1,
            "results": [
                {
                    "entity": "offer",
                    "titles": {"raw": "model offer 532"},
                    "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter"),
                },
                {"entity": "passFiltersDelimiter"},
                {
                    "entity": "offer",
                    "titles": {"raw": "bad model offer 530"},
                    "hasRelaxedGlFilter": True,
                },
            ],
        }
        response = self.report.request_json(req)
        self.assertFragmentIn(response, expected_offers, preserve_order=True, allow_different_len=False)

        """
            Первая модель восстанавливается из оффера без hasRelaxedGlFilter
            Вторая восстанавливается из оффера с этим флагом.
        """
        expected = {
            "total": 2,
            "totalPassedAllGlFilters": 1,
            "results": [
                {
                    "entity": "product",
                    "titles": {"raw": "super model 532"},
                    "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter"),
                    "offers": {
                        "items": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "model offer 532"},
                                "hasRelaxedGlFilter": NoKey("hasRelaxedGlFilter"),
                            }
                        ],
                    },
                },
                {"entity": "passFiltersDelimiter"},
                {
                    "entity": "product",
                    "titles": {"raw": "super model 533"},
                    "hasRelaxedGlFilter": True,
                    "offers": {
                        "items": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "bad model offer 530"},
                                "hasRelaxedGlFilter": True,
                            }
                        ],
                    },
                },
            ],
        }
        response = self.report.request_json(req + '&allow-collapsing=1' + '&use-default-offers=1')
        self.assertFragmentIn(response, expected, preserve_order=True, allow_different_len=False)


if __name__ == '__main__':
    main()
