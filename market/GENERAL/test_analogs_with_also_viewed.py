#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, YamarecPlace, YamarecSettingPartition
from core.testcase import TestCase, main


class T(TestCase):
    """
    Набор тестов для эксперимента аналоги + видевшие
    MARKETOUT-18917
    """

    @classmethod
    def prepare(cls):
        """
        Данные для also_viewed, product_analogs
        """

        model_ids = list(range(1, 11))
        cls.index.models += [Model(hyperid=hyperid, hid=101) for hyperid in model_ids[:-1]]
        cls.index.models.append(Model(hyperid=10, hid=102))
        cls.index.offers += [Offer(hyperid=hyperid) for hyperid in model_ids]
        """also_viewed and new analogs"""
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': 'COVIEWS1'}, splits=[{}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(model_id=1, item_count=1000, version='COVIEWS1').respond(
            {'models': ['2', '3']}
        )
        cls.recommender.on_request_accessory_models(model_id=10, item_count=1000, version='COVIEWS1').respond(
            {'models': ['8', '9']}
        )

    def test_exp(self):
        """
        Без rearr-флага или с некорретным значением работает контрольный сплит - also_viewed
        """
        response = self.report.request_json('place=also_viewed&yandexuid=001&hyperid=1&numdoc=2')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {'entity': 'product', 'id': 2, 'meta': {'place': 'also_viewed'}},
                        {'entity': 'product', 'id': 3, 'meta': {'place': 'also_viewed'}},
                    ],
                }
            },
            preserve_order=False,
        )


if __name__ == '__main__':
    main()
