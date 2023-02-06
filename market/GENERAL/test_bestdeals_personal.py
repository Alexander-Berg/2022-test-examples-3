#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    RegionalModel,
    YamarecFeaturePartition,
    YamarecPlace,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.models += [
            Model(hyperid=1, title='title 1', hid=100, model_clicks=100),
            Model(hyperid=2, title='title 2', hid=100, model_clicks=200),
            Model(hyperid=3, title='title 3', hid=100, model_clicks=300),
            Model(hyperid=4, title='title 4', hid=101, model_clicks=400),
            Model(hyperid=5, title='title 5', hid=101, model_clicks=500),
            Model(hyperid=6, title='title 6', hid=102, model_clicks=600),
        ]

        cls.index.offers += [
            Offer(title='title N1', hyperid=1, discount=35),
            Offer(title='title N2', hyperid=2, discount=36),
            Offer(title='title N3', hyperid=3, discount=37),
            Offer(title='title N4', hyperid=4, discount=38),
            Offer(title='title N5', hyperid=5, discount=39),
            Offer(title='title N6', hyperid=6, discount=40),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=1, offers=100, price_min=1100, price_old_min=1000),
            RegionalModel(hyperid=2, offers=100, price_min=1100, price_old_min=1000),
            RegionalModel(hyperid=3, offers=100, price_min=1100, price_old_min=1000),
            RegionalModel(hyperid=4, offers=100, price_min=1100, price_old_min=1000),
            RegionalModel(hyperid=5, offers=100, price_min=1100, price_old_min=1000),
            RegionalModel(hyperid=6, offers=100, price_min=1100, price_old_min=1000),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=101, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=102, output_type=HyperCategoryType.GURU),
        ]

        # 1. sort personal categories
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:11", item_count=1000).respond(
            {"models": ["1", "4", "6"]}
        )

        # place configuration
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_DISCOUNT,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        formula_id=152888,
                        splits=['*'],
                        feature_keys=['category_id'],
                        feature_names=['category_id', 'position'],
                        features=[],
                    )
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        splits=['*'],
                        feature_keys=['category_id'],
                        feature_names=['category_id', 'position'],
                        features=[],
                    )
                ],
            ),
        ]

    def test_pager(self):
        # all expected models for bestdeals request
        expected_model_order = [1, 4, 6, 2, 5, 3]

        # check default pager params: return all expected models
        response = self.report.request_json('place=bestdeals&yandexuid=11')
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"entity": "product", "id": hyperid} for hyperid in expected_model_order]}},
            preserve_order=True,
        )

        # check 3 documents on page and first page by default: return models 0,1,2
        response = self.report.request_json('place=bestdeals&yandexuid=11&numdoc=3')
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"entity": "product", "id": hyperid} for hyperid in expected_model_order[0:3]]}},
            preserve_order=True,
        )

        # check 3 documents on page and first page: return models 0,1,2
        response = self.report.request_json('place=bestdeals&yandexuid=11&numdoc=3&page=1')
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"entity": "product", "id": hyperid} for hyperid in expected_model_order[0:3]]}},
            preserve_order=True,
        )

        # check 3 documents on page and second page: return models 3,4,5
        response = self.report.request_json('place=bestdeals&yandexuid=11&numdoc=3&page=2')
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"entity": "product", "id": hyperid} for hyperid in expected_model_order[3:6]]}},
            preserve_order=True,
        )

        # check 3 documents on page and third page: return models 3,4,5
        response = self.report.request_json('place=bestdeals&yandexuid=11&numdoc=3&page=3')
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"entity": "product", "id": hyperid} for hyperid in expected_model_order[3:6]]}},
            preserve_order=True,
        )

        # check 3 documents on page, first page and skip 2 document: return models 2,3,4
        response = self.report.request_json('place=bestdeals&yandexuid=11&numdoc=3&page=1&skip=2')
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"entity": "product", "id": hyperid} for hyperid in expected_model_order[2:5]]}},
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
