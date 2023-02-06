#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Model,
    Offer,
    Shop,
    MnPlace,
)
from core.testcase import TestCase, main
from core.blender_bundles import get_supported_incuts_cgi, create_blender_bundles


class T(TestCase):
    @staticmethod
    def get_request(params, rearr):
        def dict_to_str(data, separator):
            return str(separator).join("{}={}".format(str(k), str(v)) for (k, v) in data.iteritems())

        return "{}&rearr-factors={}".format(dict_to_str(params, '&'), dict_to_str(rearr, ';'))

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.settings.formulas_path = create_blender_bundles(cls.meta_paths.testroot, None, {})

    @classmethod
    def prepare_cpa_incut_default(cls):
        titles = list(range(1, 22))
        cls.index.models += [
            Model(hid=66, hyperid=66 + i, title="Модель {}".format(titles[i]), ts=100020 + i) for i in range(1, 21)
        ]

        cls.index.shops += [
            Shop(
                fesh=66 + i, priority_region=213, shop_fee=100, cpa=Shop.CPA_REAL, name='CPA Shop {}'.format(titles[i])
            )
            for i in range(1, 21)
        ]

        cls.index.offers += [
            Offer(
                fesh=66 + i,
                hyperid=66 + i,
                hid=66,
                fee=90 + i,
                ts=100020 + i,
                price=100,
                cpa=Offer.CPA_REAL,
                title="Маркс {}".format(titles[i]),
            )
            for i in range(1, 10)
        ]

        cls.index.offers += [
            Offer(
                fesh=76,
                hyperid=76,
                hid=66,
                fee=100,
                ts=100030,
                price=100,
                cpa=Offer.CPA_REAL,
                title="Engels 1",
            )
        ]

        cls.index.offers += [
            Offer(
                fesh=76 + i,
                hyperid=76 + i,
                hid=66,
                fee=0,
                ts=100030 + i,
                price=100,
                cpa=Offer.CPA_REAL,
                title="Engels {}".format(titles[i]),
            )
            for i in range(2, 10)
        ]

        for i in range(1, 21):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100020 + i).respond(0.04)

    def test_cpa_incut_default(self):
        params = {
            "place": "blender",
            "text": "маркс",
            "additional_entities": "articles",
            "touch": "1",
            "client": "frontend",
            "platform": "touch",
            'debug': "da",
            'supported-incuts': get_supported_incuts_cgi(),
        }

        rearr_flags = {"market_blender_cpa_shop_incut_enabled": 1, "market_blender_use_bundles_config": 1}

        response = self.report.request_json(T.get_request(params, rearr_flags))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'inClid': 2,
                        },
                    ]
                }
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
