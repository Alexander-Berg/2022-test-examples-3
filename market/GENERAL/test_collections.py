#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, YamarecFeaturePartition, YamarecPlace
from core.testcase import TestCase, main

from core.matrixnet import MatrixnetFeature
from core.crypta import CryptaName, CryptaFeature


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1),
            HyperCategory(hid=2, children=[HyperCategory(hid=3)]),
        ]

        cls.crypta.on_default_request().respond(features=[])

        keys = ['collection_id', 'category_id']
        names = ['collection_id', 'category_id', 'goodness']
        features = [
            [100, 1, 1],
            [101, 1, 2],
            [102, 2, 3],
            [103, 2, 4],
            [104, 3, 5],
            [105, 3, 6],
        ]

        # sort by goodness
        goodness_formula = 700
        for record in features:
            goodness = record[2]
            cls.matrixnet.on_call(
                formula_id=goodness_formula, features=[MatrixnetFeature('goodness', goodness)]
            ).respond(goodness)

        # sort by gender: for male -- badness, for not male (female?) -- odds first, evens last, sort by goodness
        gender_formula = 800
        for idx, record in enumerate(features):
            goodness = record[2]
            badness = 100 - goodness
            cls.matrixnet.on_call(
                formula_id=gender_formula,
                features=[MatrixnetFeature('goodness', goodness), MatrixnetFeature(CryptaName.GENDER_MALE, 100)],
            ).respond(badness)

            cls.matrixnet.on_call(
                formula_id=gender_formula,
                features=[MatrixnetFeature('goodness', goodness), MatrixnetFeature(CryptaName.GENDER_MALE, 1)],
            ).respond(100500 + goodness if idx % 2 else goodness)

        cls.crypta.on_request_profile(yandexuid=2).respond(
            features=[
                CryptaFeature(name=CryptaName.GENDER_MALE, value=100),
            ]
        )

        cls.crypta.on_request_profile(yandexuid=3).respond(
            features=[
                CryptaFeature(name=CryptaName.GENDER_MALE, value=1),
            ]
        )

        # sort by category: 2 - first, 3 - second, 1 - last, inside by goodness
        category_formula = 900
        category_rank = {2: 500, 3: 400, 1: 300}

        for record in features:
            goodness = record[2]
            category = record[1]
            rank = category_rank[category]
            cls.matrixnet.on_call(
                formula_id=category_formula,
                features=[
                    MatrixnetFeature('goodness', goodness),
                ],
            ).respond(rank + goodness)

        # default formula: for absent-yandexuid -- male div3 -> by badness, else -> by goodness
        cls.crypta.on_request_profile(yandexuid=None).respond(features=[])

        category_rank = {1: 30, 2: 20, 3: 10}
        anonymous_formula = 1000
        for idx, record in enumerate(features):
            category = record[1]
            goodness = record[2]
            rank = category_rank[category]
            weight = rank + (goodness if idx % 2 else -goodness)
            cls.matrixnet.on_call(
                formula_id=anonymous_formula,
                features=[
                    MatrixnetFeature('goodness', goodness),
                    MatrixnetFeature(CryptaName.GENDER_MALE, 621947),  # hardcoded value if crypta returns empty
                ],
            ).respond(weight)

        # place configuration
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COLLECTION_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        formula_id=goodness_formula,
                        splits=['1'],
                        feature_keys=keys,
                        feature_names=names,
                        features=features,
                    ),
                    YamarecFeaturePartition(
                        formula_id=gender_formula,
                        splits=['2', '3'],
                        feature_keys=keys,
                        feature_names=names,
                        features=features,
                    ),
                    YamarecFeaturePartition(
                        formula_id=category_formula,
                        splits=['4', '5'],
                        feature_keys=keys,
                        feature_names=names,
                        features=features,
                    ),
                    YamarecFeaturePartition(
                        formula_id=anonymous_formula,
                        splits=['*'],
                        feature_keys=keys,
                        feature_names=names,
                        features=features,
                    ),
                ],
            )
        ]

    def test_builtin_features(self):
        response = self.report.request_xml('place=collections&yandexuid=1')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <collection id="105"/>
            <collection id="104"/>
            <collection id="103"/>
            <collection id="102"/>
            <collection id="101"/>
            <collection id="100"/>
        </search_results>
        ''',
            preserve_order=True,
        )

    def test_crypta_features(self):
        # Request of strong male (yandexuid=2, crypta respond 100)
        # Expect sorting by badness (reversed goodness)
        response = self.report.request_xml('place=collections&yandexuid=2')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <collection id="100"/>
            <collection id="101"/>
            <collection id="102"/>
            <collection id="103"/>
            <collection id="104"/>
            <collection id="105"/>
        </search_results>
        ''',
            preserve_order=True,
        )

        # Request of weak male (yandexuid=3, crypta respond 1)
        # Expect sorting by goodness: odd first, even last
        response = self.report.request_xml('place=collections&yandexuid=3')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <collection id="105"/>
            <collection id="103"/>
            <collection id="101"/>
            <collection id="104"/>
            <collection id="102"/>
            <collection id="100"/>
        </search_results>
        ''',
            preserve_order=True,
        )

    def test_ichwill_features(self):
        # Request split 4 with ichwill category dependent formula (yandexuid=4)
        # Expect sorting by category: 2, 3, 1, inside -- goodness
        response = self.report.request_xml('place=collections&yandexuid=4')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <collection id="103"/>
            <collection id="102"/>
            <collection id="105"/>
            <collection id="104"/>
            <collection id="101"/>
            <collection id="100"/>
        </search_results>
        ''',
            preserve_order=True,
        )

        response = self.report.request_xml('place=collections&yandexuid=5&puid=8')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <collection id="103"/>
            <collection id="102"/>
            <collection id="105"/>
            <collection id="104"/>
            <collection id="101"/>
            <collection id="100"/>
        </search_results>
        ''',
            preserve_order=True,
        )

    def test_category_filter(self):
        response = self.report.request_xml('place=collections&yandexuid=1&hid=2')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <collection id="105"/>
            <collection id="104"/>
            <collection id="103"/>
            <collection id="102"/>
        </search_results>
        ''',
            preserve_order=True,
        )

    def test_paging(self):
        response = self.report.request_xml('place=collections&yandexuid=1&numdoc=2')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <collection id="105"/>
            <collection id="104"/>
        </search_results>
        ''',
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_xml('place=collections&yandexuid=1&numdoc=2&page=2')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <collection id="103"/>
            <collection id="102"/>
        </search_results>
        ''',
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_xml('place=collections&yandexuid=1&numdoc=7')
        self.assertEqual(6, response.count('<collection/>'))

        response = self.report.request_xml('place=collections&yandexuid=1&numdoc=2&page=4')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <collection id="101"/>
            <collection id="100"/>
        </search_results>
        ''',
            preserve_order=True,
            allow_different_len=False,
        )

        """Проверяется общее количество для показа"""
        self.access_log.expect(total_renderable='6').times(4)

    def test_anonymous_users(self):
        # Request of anonymous (male = 621947, hardcoded as default profile)
        # Expect sorting by formula: weight = category_rank + (goodness if idx % 2 else -goodness)

        response = self.report.request_xml('place=collections')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <collection id="101"/>
            <collection id="100"/>
            <collection id="103"/>
            <collection id="102"/>
            <collection id="105"/>
            <collection id="104"/>
        </search_results>
        ''',
            preserve_order=True,
        )

    def test_missing_pp(self):
        self.report.request_xml('place=collections&yandexuid=1&ip=127.0.0.1', add_defaults=False)


if __name__ == '__main__':
    main()
