#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Contex,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    HyperCategoryType,
    Model,
    ModelDescriptionTemplates,
    Offer,
    Region,
    RegionalModel,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regional_models += [
            RegionalModel(hyperid=123, offers=1, price_min=1, price_max=2, rids=[0, 213]),
            RegionalModel(hyperid=124, offers=2, price_min=1, price_max=2, rids=[213]),
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=2,
                model=[
                    (
                        "Технические характеристики",
                        {
                            "Какой-то параметр1": "{SomeParamOne}",
                            "Какой-то параметр2": "{SomeParamTwo}",
                        },
                    )
                ],
            )
        ]

        cls.index.gltypes += [
            GLType(
                param_id=401,
                hid=2,
                gltype=GLType.ENUM,
                xslname="SomeParamOne",
                values=[GLValue(value_id=1, text='First')],
            ),
            GLType(
                param_id=402,
                hid=2,
                gltype=GLType.ENUM,
                xslname="SomeParamTwo",
                values=[GLValue(value_id=1, text='Second')],
            ),
        ]

        cls.index.models += [
            Model(title='red phone', hid=2, glparams=[GLParam(param_id=401, value=1)]),
            Model(title='green phone', hyperid=123, hid=2, glparams=[GLParam(param_id=401, value=1)], model_clicks=2),
            Model(
                title='blue phone',
                hid=2,
                contex=Contex(parent_id=123, exp_name='new-title'),
                glparams=[GLParam(param_id=402, value=1)],
            ),
        ]

        cls.index.hypertree += [HyperCategory(hid=2, name='phones', output_type=HyperCategoryType.GURU)]

    def test_wide_query(self):
        """
        Ищем по тексту phone:
        - не в эксперименте -- находим модели red & green
        - в эксперименте -- находим модели red & blue

        У моделей green & blue на выдаче hyperid = 123 (т.е. базовой модели)
        """

        # вне эксперимента
        for glfilter in ("", "&glfilter=401:1"):
            response = self.report.request_json('place=prime&text=phone&hid=2&rearr-factors=contex=1' + glfilter)
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'totalModels': 2,
                        'results': [
                            {'titles': {'raw': 'red phone'}},
                            {'titles': {'raw': 'green phone'}, 'id': 123, 'filters': [{'id': "401"}]},
                        ],
                    }
                },
            )

        # в выключенном эксперименте
        for glfilter in ("", "&glfilter=401:1"):
            response = self.report.request_json(
                'place=prime&text=phone&hid=2&rearr-factors=new-title=0;contex=1' + glfilter
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'totalModels': 2,
                        'results': [
                            {'titles': {'raw': 'red phone'}},
                            {'titles': {'raw': 'green phone'}, 'id': 123, 'filters': [{'id': "401"}]},
                        ],
                    }
                },
            )

        response = self.report.request_json('place=prime&text=phone&hid=2&rearr-factors=new-title=1;contex=1&debug=da')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'totalModels': 2,
                    'results': [
                        {'titles': {'raw': 'red phone'}},
                        {'titles': {'raw': 'blue phone'}, 'id': 123, 'filters': [{'id': "402"}]},
                    ],
                }
            },
        )

        # with glfilter
        response = self.report.request_json(
            'place=prime&text=phone&hid=2&rearr-factors=new-title=1;contex=1&debug=da&glfilter=402:1'
        )
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'titles': {'raw': 'blue phone'}, 'id': 123, 'filters': [{'id': "402"}]}]}},
        )

    def test_narrow_query(self):
        """
        Ищем по hyperid базовой модели, ожидаем увидеть:
        - в эксперименте -- экспериментальную модель
        - вне эксперимента -- базовую
        """

        # вне эксперимента
        response = self.report.request_json('place=modelinfo&hyperid=123&rids=0&bsformat=2&rearr-factors=contex=1')
        self.assertFragmentIn(
            response,
            {'total': 1, 'results': [{'titles': {'raw': "green phone"}, 'id': 123, 'filters': [{'id': "401"}]}]},
        )

        # в эксперименте
        response = self.report.request_json(
            'place=modelinfo&hyperid=123&rearr-factors=new-title=1;contex=1&rids=0&bsformat=2'
        )
        self.assertFragmentIn(
            response,
            {'total': 1, 'results': [{'titles': {'raw': "blue phone"}, 'id': 123, 'filters': [{'id': "402"}]}]},
        )

        response = self.report.request_json(
            'place=modelinfo&hyperid=123&rearr-factors=new-title=1;contex=1&rids=0&bsformat=2&show-models-specs=full'
        )
        self.assertFragmentIn(
            response,
            {
                'total': 1,
                'results': [
                    {
                        'titles': {'raw': "blue phone"},
                        'id': 123,
                        'filters': [{'id': "402"}],
                        'specs': {"full": [{"groupSpecs": [{"usedParams": [{"id": 402}]}]}]},
                    }
                ],
            },
        )

    @classmethod
    def prepare_model_wizard(cls):
        cls.index.regiontree += [Region(rid=213)]

        cls.index.models += [
            Model(
                hyperid=106,
                title='pepelac 2000',
                picinfo='//avatars.mds.yandex.net/get-mpic/1235/img_bas_model_pic_id/orig#1000#1000',
            ),
            Model(
                picinfo='//avatars.mds.yandex.net/get-mpic/1236/img_bas_model_pic_id/orig#1000#1000',
                title='pepelac 2000',
                contex=Contex(parent_id=106, exp_name='sp1'),
            ),
        ]

    def test_model_wizard(self):
        # вне эксперимента
        response = self.report.request_bs(
            'place=parallel&text=pepelac+2000&ignore-mn=1&rids=213&rearr-factors=contex=1'
        )
        self.assertFragmentIn(
            response,
            {'market_model': [{'picture': '//avatars.mds.yandex.net/get-mpic/1235/img_bas_model_pic_id/2hq'}]},
            allow_different_len=False,
        )

        # в эксперименте
        response = self.report.request_bs(
            'place=parallel&text=pepelac+2000&ignore-mn=1&rids=213&rearr-factors=sp1=1;contex=1'
        )
        self.assertFragmentIn(
            response,
            {'market_model': [{'picture': '//avatars.mds.yandex.net/get-mpic/1236/img_bas_model_pic_id/2hq'}]},
            allow_different_len=False,
        )

    def test_model_clicks(self):
        """
        Проверяем, что клики по моделям берутся откуда надо
        MARKETOUT-15821
        """

        # вне эксперимента
        response = self.report.request_json('place=prime&hid=2&rids=213&glfilter=401:1&debug=da')
        self.assertFragmentIn(response, {'titles': {'raw': "green phone"}})
        self.assertFragmentIn(response, {"CLICK_COUNT": "2"})

        # в эксперименте
        response = self.report.request_json(
            'place=prime&hid=2&rearr-factors=new-title=1&rids=213&glfilter=402:1&debug=da'
        )
        self.assertFragmentIn(response, {'titles': {'raw': "blue phone"}})
        self.assertFragmentIn(response, {"CLICK_COUNT": "2"})

    @classmethod
    def prepare_collapsing(cls):
        cls.index.models += [
            Model(title='red tv', hid=3, hyperid=128),
            Model(title='green tv', hid=3, hyperid=129),
            Model(title='blue tv', hid=3, hyperid=130),
            Model(title='red tv exp', hid=3, hyperid=228, contex=Contex(parent_id=128, exp_name="tv")),
            Model(title='green tv exp', hid=3, hyperid=229, contex=Contex(parent_id=129, exp_name="tv")),
            Model(title='blue tv exp', hid=3, hyperid=230, contex=Contex(parent_id=130, exp_name="tv")),
        ]

        cls.index.offers += [
            Offer(title="super red tv", hid=3, hyperid=128),
            Offer(title="super green tv", hid=3, hyperid=129),
            Offer(title="super blue tv", hid=3, hyperid=130),
        ]

    def test_collapsing(self):
        response = self.report.request_json(
            'place=prime&text=super&allow-collapsing=1&rearr-factors=tv=1;contex=1&rids=0&debug=da'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': "red tv exp"}},
                    {'titles': {'raw': "green tv exp"}},
                    {'titles': {'raw': "blue tv exp"}},
                ]
            },
        )


if __name__ == '__main__':
    main()
