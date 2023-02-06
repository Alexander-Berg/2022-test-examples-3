#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    NavCategory,
)
from core.types.autogen import Const

MODELS_LIST = [1001, 1002, 2001, 2002, 3001, 3002]

FULL_MODEL_ANSWER = [{"entity": "product", "id": id} for id in MODELS_LIST]


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    @classmethod
    def prepare_data_for_glfilters(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=198118,
                output_type=HyperCategoryType.GURU,
                name="Бытовая техника",
                children=[
                    HyperCategory(
                        hid=54470,
                        output_type=HyperCategoryType.GURU,
                        name="Техника для дома",
                        children=[
                            HyperCategory(
                                hid=16126061,
                                output_type=HyperCategoryType.GURU,
                                name="Техника для уборки",
                                children=[
                                    HyperCategory(hid=16302535, output_type=HyperCategoryType.GURU, name="Пылесосы"),
                                    HyperCategory(
                                        hid=16302536, output_type=HyperCategoryType.GURU, name="Роботы-пылесосы"
                                    ),
                                    HyperCategory(
                                        hid=16302537, output_type=HyperCategoryType.GURU, name="Вертикальные пылесосы"
                                    ),
                                    HyperCategory(
                                        hid=16147796, output_type=HyperCategoryType.GURU, name="Пароочистители"
                                    ),
                                    HyperCategory(
                                        hid=278341, output_type=HyperCategoryType.GURU, name="Аксессуары для пылесосов"
                                    ),
                                    HyperCategory(
                                        hid=90564,
                                        output_type=HyperCategoryType.GURU,
                                        name="Электровеники и электрошвабры",
                                    ),
                                    HyperCategory(
                                        hid=12802914,
                                        output_type=HyperCategoryType.GURU,
                                        name="Электрические стеклоочистители",
                                    ),
                                ],
                            ),
                        ],
                    ),
                ],
            ),
        ]

        for h in [16126061, 16302535, 16302536, 16302537, 16147796, 278341, 90564, 12802914]:
            cls.index.gltypes += [
                GLType(
                    param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID,
                    hid=h,
                    gltype=GLType.ENUM,
                    unit_name="Производитель",
                    values=[
                        GLValue(value_id=1, text="Пр1"),
                        GLValue(value_id=2, text="Пр2"),
                    ],
                    cluster_filter=False,
                    through=True,
                ),
                GLType(
                    param_id=17354840,
                    hid=h,
                    gltype=GLType.ENUM,
                    unit_name="Тип уборки",
                    values=[
                        GLValue(value_id=1, text="влажная"),
                        GLValue(value_id=2, text="сухая"),
                        GLValue(value_id=3, text="сухая и влажная"),
                    ],
                    cluster_filter=False,
                    through=True,
                ),
            ]
        entities = [
            dict(
                hyperid=1001,
                hid=16302535,
                title='pylesos 1001 common',
                glparams=[
                    GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=1),  # Пр1
                    GLParam(param_id=17354840, value=1),  # влажная уборка
                ],
            ),
            dict(
                hyperid=1002,
                hid=16302535,
                title='pylesos 1002 common',
                glparams=[
                    GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=2),  # Пр2
                    GLParam(param_id=17354840, value=2),  # сухая уборка
                ],
            ),
            dict(
                hyperid=2001,
                hid=16302536,
                title='pylesos robot 2001 common',
                glparams=[
                    GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=1),  # Пр1
                    GLParam(param_id=17354840, value=2),  # сухая уборка
                ],
            ),
            dict(
                hyperid=2002,
                hid=16302536,
                title='pylesos robot 2002 common',
                glparams=[
                    GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=2),  # Пр2
                    GLParam(param_id=17354840, value=1),  # влажная уборка
                ],
            ),
            dict(
                hyperid=3001,
                hid=16302537,
                title='pylesos vertical 3001 common',
                glparams=[
                    GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=1),  # Пр1
                    GLParam(param_id=17354840, value=1),  # влажная уборка
                ],
            ),
            dict(
                hyperid=3002,
                hid=16302537,
                title='pylesos vertical 3002 common',
                glparams=[
                    GLParam(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, value=2),  # Пр2
                    GLParam(param_id=17354840, value=2),  # сухая уборка
                ],
            ),
        ]
        cls.index.models += [Model(**entity) for entity in entities]

        cls.index.mskus += [
            MarketSku(
                title=entity["title"] + "sku",
                sku=entity["hyperid"] * 10 + 1,
                hyperid=entity["hyperid"],
                blue_offers=[BlueOffer(offerid=entity["title"].replace(" ", "_"), glparams=entity["glparams"])],
                glparams=entity["glparams"],
            )
            for entity in entities
        ]

    def test_all_found_by_text(self):
        """
        просто поиск находит все
        """
        response = self.report.request_json('place=prime&text=pylesos')
        self.assertFragmentIn(response, {'results': FULL_MODEL_ANSWER})

    def test_by_text_and_filter(self):
        '''
        Проверяем, что фильтрация по сквозному фильтру происходит и без указания категории
        '''
        response = self.report.request_json(
            'place=prime&text=pylesos&glfilter={}:1&allow-collapsing=1&rearr-factors=market_through_gl_filters_on_search=1;market_metadoc_search=no'.format(
                Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID
            )
        )
        self.assertFragmentIn(
            response, {'results': self.get_expected_entities([1001, 2001, 3001])}, allow_different_len=False
        )

        response = self.report.request_json(
            'place=prime&text=pylesos&glfilter={}:1&allow-collapsing=1&rearr-factors=market_through_gl_filters_on_search=1;market_metadoc_search=no'.format(
                17354840
            )
        )
        self.assertFragmentIn(
            response, {'results': self.get_expected_entities([1001, 2002, 3001])}, allow_different_len=False
        )

    @staticmethod
    def get_expected_entities(models):
        '''
        при запросе на белый синие офферы попадают в результат вместе с моделями -> для белых запросов расширяем
        ожидаемый ответ
        '''
        return [{"entity": "product", "id": id} for id in models]

    def get_request(self, color='', rearr_flag='', client=''):
        req = (
            'place=prime&numdoc=25&text=pylesos'
            '&glfilter={filter}:1&rearr-factors={flag}&rgb={color}&allow-collapsing=1&allow-ungrouping=0&client={client}'
        ).format(
            filter=17354840,
            flag=rearr_flag,
            color=color,
            client=client,
        )
        return req

    def check_through_filters_disabled(self, color='', rearr_flag='', client=''):
        '''
        Проверяем, что фильтрация по сквозным фильтрам не работает
        при этом в error.log должна быть записана ошибка
        '''
        request = (
            self.get_request(color=color, rearr_flag=rearr_flag, client=client)
            + '&rearr-factors=market_metadoc_search=no'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'results': self.get_expected_entities(MODELS_LIST)}, allow_different_len=False)

    def check_through_filters_enabled(self, color='', rearr_flag='', client=''):
        '''Проверяем что фильтры в запросе отработали корректно и документы отфильтровались'''

        request = (
            self.get_request(color=color, rearr_flag=rearr_flag, client=client)
            + '&rearr-factors=market_metadoc_search=no'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response, {'results': self.get_expected_entities([1001, 2002, 3001])}, allow_different_len=False
        )

    def test_market_through_gl_filters_on_search(self):
        '''
        использование флага market_through_gl_filters_on_search должно включить фильтрацию
        по умолчанию она выключена, кроме приложения
        '''
        for color in ['green', 'blue']:

            self.check_through_filters_enabled(color=color, rearr_flag='market_through_gl_filters_on_search=1')
            self.check_through_filters_disabled(color=color, rearr_flag='market_through_gl_filters_on_search=0')
            self.check_through_filters_disabled(color=color)
            # на приложениях по умолчанию  включено
            self.check_through_filters_enabled(color=color, client='IOS')
            self.check_through_filters_enabled(color=color, client='ANDROID')
            # но можно выключить флагом
            self.check_through_filters_disabled(
                color=color, rearr_flag='market_through_gl_filters_on_search=0', client='IOS'
            )
            self.check_through_filters_disabled(
                color=color, rearr_flag='market_through_gl_filters_on_search=0', client='ANDROID'
            )

        self.error_log.expect(
            code=3036,
            message="GlFactory returned null (categoryId is root market category id), glfilters: 17354840:1, offending filter: 17354840:1, category id: 90401",
        ).times(8)

    @classmethod
    def prepare_data_for_nid_requests(cls):
        msku_1 = MarketSku(
            hyperid=1,
            hid=1,
            sku=11,
            blue_offers=[BlueOffer()],
            glparams=[
                GLParam(param_id=202, value=3),
            ],
        )

        cls.index.gltypes += [
            GLType(param_id=202, hid=1, gltype=GLType.ENUM, hidden=False, values=[3, 4]),
        ]

        cls.index.mskus += [msku_1]

        cls.index.navtree += [
            NavCategory(
                nid=6001,
                name="Parent",
                is_blue=True,
                children=[NavCategory(nid=6000, hid=1, name="Test", is_blue=True)],
            )
        ]

    def test_nid_request_with_convert_to_hid(self):
        '''
        Проверяем, что если не передан hid, но его можно получить из nid, то отдаются все фильтры, соответстующие этому hid-у.
        '''
        response = self.report.request_json('rgb=blue&pp=18&place=prime&nid=6000')
        self.assertFragmentIn(response, {"search": {}, "filters": [{"id": "202"}]})


if __name__ == '__main__':
    main()
