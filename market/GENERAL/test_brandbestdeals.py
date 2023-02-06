#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, HyperCategoryType, Model, Offer, YamarecFeaturePartition, YamarecPlace
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        '''Создаем модели со скидочными оферами для разных вендоров,
        одну модель без скидочных оферов'''
        cls.index.models += [
            Model(title='hyper_100', hyperid=100, hid=200, vendor_id=1),
            Model(title='hyper_101', hyperid=101, hid=201, vendor_id=1),
            Model(title='hyper_102', hyperid=102, hid=201, vendor_id=1),
            Model(hyperid=103, hid=202, vendor_id=2),
            Model(hyperid=104, hid=203, vendor_id=1),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=200, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=201, output_type=HyperCategoryType.GURU),
        ]

        cls.index.offers += [
            Offer(hyperid=100, hid=200, vendor_id=1, discount=50),
            Offer(hyperid=101, hid=201, vendor_id=1, discount=50),
            Offer(hyperid=102, hid=201, vendor_id=1, discount=50),
            Offer(hyperid=103, hid=202, vendor_id=2, discount=50),
            Offer(hyperid=104, hid=203, vendor_id=1),
        ]

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

    def test_filter_vendor_id(self):
        '''Проверяем, что на выдаче только модели запрошенного вендора,
        нет моделей без скидочных оферов.
        Проверяем запись в show.log'''
        response = self.report.request_xml('place=brandbestdeals&yandexuid=555&vendor_id=1')
        self.assertFragmentIn(
            response,
            '''<offers>
            <model id="100" />
            <model id="101" />
            <model id="102" />
        </offers>''',
            allow_different_len=False,
        )
        self.assertFragmentNotIn(response, '<model id="103" />')
        self.assertFragmentNotIn(response, '<model id="104" />')
        self.error_log.ignore('Personal category config is not available')

        self.show_log_tskv.expect(title='hyper_100', position=1)
        self.show_log_tskv.expect(title='hyper_101', position=2)
        self.show_log_tskv.expect(title='hyper_102', position=3)

    @classmethod
    def prepare_top_categories_bestdeals_chain(cls):
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:555", item_count=1000).respond(
            {"models": ['100', '101', '102']}
        )

    def test_top_categories_bestdeals_chain(self):
        '''Проверяем, что плейс эквивалентен запуску
        bestdeals с результатами top_categories'''
        response = self.report.request_json('place=top_categories&vendor_id=1&numdoc=120')
        self.assertFragmentIn(
            response,
            {
                'link': {'params': {'hid': '201'}},
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=bestdeals&yandexuid=555&vendor_id=1&hid=200,201')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'product', 'id': 100},
                    {'entity': 'product', 'id': 101},
                    {'entity': 'product', 'id': 102},
                ]
            },
            allow_different_len=False,
        )
        self.error_log.ignore('Personal category config is not available')

        response = self.report.request_xml('place=brandbestdeals&yandexuid=555&vendor_id=1')
        self.assertFragmentIn(
            response,
            '''<offers>
            <model id="100" />
            <model id="101" />
            <model id="102" />
        </offers>''',
            allow_different_len=False,
        )
        self.error_log.ignore('Personal category config is not available')

    def test_missing_pp(self):
        '''Проверяем обязательность pp'''
        response = self.report.request_xml(
            'place=brandbestdeals&yandexuid=555&vendor_id=1', strict=False, add_defaults=False
        )
        self.error_log.ignore('Personal category config is not available')
        self.error_log.expect('Some client has not set PP value. Find and punish him violently').once()
        self.assertEqual(500, response.code)

    def test_missing_vendor_id(self):
        '''Проверяем обязательность vendor_id'''
        response = self.report.request_xml('place=brandbestdeals')
        self.assertFragmentIn(response, '<search_results message="One and only one vendor ID should be specified" />')
        self.error_log.expect(code=3043)

    @classmethod
    def prepare_limits(cls):
        '''Создаем более 120 категорий с двумя моделями со скидками'''
        models_in_hid = 2
        models_for_personal_history = []
        for i in range(121):
            hid = 210 + i
            cls.index.hypertree += [HyperCategory(hid=hid, output_type=HyperCategoryType.GURU)]
            for j in range(models_in_hid):
                hyperid = 110 + models_in_hid * i + j
                amodel = hyperid
                cls.index.models += [Model(hyperid=hyperid, hid=hid, vendor_id=3, randx=hyperid)]
                cls.index.offers += [Offer(hyperid=hyperid, hid=hid, vendor_id=3, discount=50)]
            models_for_personal_history.append(amodel)
        cls.recommender.on_request_models_of_interest(user_id="yandexuid:", item_count=1000).respond(
            {"models": map(str, list(reversed(models_for_personal_history)))}
        )

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
            )
        ]

    def test_limits(self):
        '''Проверяем, что при запросе более 120 документов
        отдаются модели только из 120 категорий (сколько отдает top_categories),
        причем с наибольшими randx, т.к. остальные показатели
        ранжирования top_categories одинаковые'''
        response = self.report.request_json('place=brandbestdeals&vendor_id=3&numdoc=130')
        for i in range(120):
            self.assertFragmentIn(response, {'entity': 'category', 'id': 211 + i})
        self.assertFragmentNotIn(response, {'entity': 'category', 'id': 210})
        self.error_log.ignore('Personal category config is not available')

    def test__total_renderable(self):
        request = 'place=brandbestdeals&yandexuid=555&vendor_id=1'
        response_with_total = '<search_results adult="*" book-now-detected="*" sales-detected="*" shop-outlets="*" shops="*" total="3"></search_results>'

        response = self.report.request_xml(request)
        self.error_log.ignore('Personal category config is not available')
        self.assertFragmentIn(response, response_with_total)
        self.assertEqual(3, response.count("model id"))

        response = self.report.request_xml(request + '&numdoc=2')
        self.assertFragmentIn(response, response_with_total)
        self.assertEqual(2, response.count("model id"))

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='3').times(2)


if __name__ == '__main__':
    main()
