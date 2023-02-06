#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import GLParam, GLType, HyperCategory, MnPlace, Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=95, visual=True),
            HyperCategory(
                hid=7877999,
                visual=True,
                children=[
                    HyperCategory(hid=7812186, visual=True),
                ],
            ),
        ]

        cls.index.gltypes += [
            GLType(param_id=1202, hid=7812186, gltype=GLType.ENUM, cluster_filter=True),
            GLType(param_id=1203, hid=7812186, gltype=GLType.ENUM, cluster_filter=True),
            GLType(param_id=1204, hid=95, gltype=GLType.ENUM, cluster_filter=True),
        ]

        cls.index.offers += [
            Offer(
                vclusterid=1000001101,
                hid=7812186,
                glparams=[
                    GLParam(param_id=1202, value=1),
                    GLParam(param_id=1203, value=1),
                ],
                ts=1,
            ),
            Offer(
                vclusterid=1000001102,
                hid=7812186,
                glparams=[
                    GLParam(param_id=1202, value=5),
                    GLParam(param_id=1203, value=1),
                ],
                ts=2,
            ),
            Offer(
                vclusterid=1000001104,
                hid=7812186,
                glparams=[
                    GLParam(param_id=1202, value=4),
                    GLParam(param_id=1203, value=1),
                ],
                ts=3,
            ),
            Offer(
                vclusterid=1000001103,
                hid=95,
                glparams=[
                    GLParam(param_id=1204, value=1),
                ],
                ts=4,
            ),
        ]
        for ts in range(1, 5):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.5 + 0.001 * ts)

    def test_clothes_filters(self):
        response = self.report.request_json(
            'place=prime&hid=7812186&glfilter=1202:1,4&glfilter=1203:1&debug=1&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            [{'entity': 'product', 'id': 1000001104}, {'entity': 'product', 'id': 1000001101}],
            allow_different_len=False,
        )

    def test_non_clothes_filters(self):
        response = self.report.request_json('place=prime&hid=95&glfilter=1204:1&allow-collapsing=1&debug=1')
        self.assertFragmentIn(response, {'entity': 'product', 'id': 1000001103}, allow_different_len=False)


if __name__ == '__main__':
    main()
