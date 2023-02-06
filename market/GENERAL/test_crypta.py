#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.logs import ErrorCodes
from core.types import HyperCategory, HyperCategoryType, Model, Offer
from core.testcase import TestCase, main
from core.matcher import Regex
from core.types import YamarecPlace, YamarecFeaturePartition


class T(TestCase):

    DEFAULT_PROFILE = (
        '{"weight":"621947","value":"0","id":"174"},'
        '{"weight":"375515","value":"1","id":"174"},'
        '{"weight":"55922","value":"0","id":"175"},'
        '{"weight":"136098","value":"1","id":"175"},'
        '{"weight":"339698","value":"2","id":"175"},'
        '{"weight":"228599","value":"3","id":"175"},'
        '{"weight":"236615","value":"4","id":"175"},'
        '{"weight":"54081","value":"0","id":"176"},'
        '{"weight":"433531","value":"1","id":"176"},'
        '{"weight":"507399","value":"2","id":"176"}'
    )

    @classmethod
    def prepare(cls):

        dummy_user = 1
        cls.crypta.on_request_profile(yandexuid=dummy_user).respond(features=[])
        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU),
        ]
        cls.index.models += [
            Model(hyperid=1, hid=100),
        ]
        cls.index.offers += [
            Offer(hyperid=1, discount=10, price_old=1000),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[[100, 1]],
                        splits=['*'],
                    )
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_DISCOUNT,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[[100, 1]],
                        splits=['*'],
                    )
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.ARTICLE_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['article_id', 'category_id', 'goodness'],
                        feature_keys=['article_id', 'category_id'],
                        features=[[100, 1, 1]],
                        splits=['*'],
                    )
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.COLLECTION_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['collection_id', 'category_id', 'goodness'],
                        feature_keys=['collection_id', 'category_id'],
                        features=[[100, 1, 1]],
                        splits=['*'],
                    )
                ],
            ),
        ]
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:').respond({'models': ['1']})

    def test_correct_yandexuid(self):
        """
        Передается корректный yandexuid, поэтому не должно выбрасываться исключение
        """
        self.report.request_json(
            'place=recom_universal&yandexuid=1&rearr-factors=market_cryptaprofile_use_shiny=true&text=iphone',
            headers={'X-Market-Req-ID': "market-request-id/42"},
        )

    def test_incorrect_yandexuid(self):
        """
        Передается некорректный yandexuid.
        Ожидается ошибка
        """
        self.report.request_json(
            'place=recom_universal&yandexuid=2&rearr-factors=market_cryptaprofile_use_shiny=true&text=iphone',
            headers={'X-Market-Req-ID': "market-request-id/42"},
        )
        self.error_log.expect(code=ErrorCodes.EXTREQUEST_WRONG_CRYPTA_PROFILE).times(1)

    def test_default_profile(self):
        """
        Тест работы крипты для пустого yandexuid
          Ожидается профиль по умолчанию
        """
        places = ['bestdeals', 'personalcategorymodels', 'articles', 'collections']
        self.error_log.ignore('Cannot find suitable personal categories. Fallback to all categories')

        for place in places:
            self.report.request_xml(
                'place={place}'.format(place=place), headers={'X-Market-Req-ID': "market-request-id/42"}
            )
            self.external_services_trace_log.expect(
                request_id=(Regex("market-request-id/42/[0-9]/*0*")),
            )


if __name__ == '__main__':
    main()
