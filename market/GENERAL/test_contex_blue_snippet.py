#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Contex,
    GLParam,
    GLType,
    GLValue,
    MarketSku,
    Model,
    ModelDescriptionTemplates,
    RegionalModel,
)
from core.testcase import TestCase, main
from core.types.contex import create_experiment_mskus


class T(TestCase):
    @classmethod
    def prepare(cls):
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
            GLType(
                param_id=501,
                hid=2,
                gltype=GLType.ENUM,
                xslname="SomeParamOne_",
                values=[GLValue(value_id=1, text='First')],
            ),
            GLType(
                param_id=502,
                hid=2,
                gltype=GLType.ENUM,
                xslname="SomeParamTwo_",
                values=[GLValue(value_id=1, text='Second')],
            ),
        ]

        cls.index.models += [
            Model(title='red phone', hyperid=122, hid=2, glparams=[GLParam(param_id=401, value=1)]),
            Model(title='green phone', hyperid=123, hid=2, glparams=[GLParam(param_id=401, value=1)], model_clicks=2),
            Model(
                title='blue phone',
                hyperid=124,
                hid=2,
                contex=Contex(parent_id=123, exp_name='new-title'),
                glparams=[GLParam(param_id=402, value=1)],
            ),
        ]

        sku1_offer1 = BlueOffer(offerid='Shop1_sku1', feedid=1)
        sku1_offer2 = BlueOffer(offerid='Shop2_sku1', feedid=2)

        sku2_offer1 = BlueOffer(offerid='Shop1_sku2', feedid=3)
        sku2_offer2 = BlueOffer(offerid='Shop2_sku2', feedid=4)

        msku_red = MarketSku(
            title='offer sku1 red phone',
            feedid=100,
            offerid=100,
            hyperid=122,
            sku=1,
            waremd5='Sku1-wdDXWsIiLVm1goleg',
            blue_offers=[sku1_offer1, sku1_offer2],
            glparams=[
                GLParam(param_id=501, value=1),
            ],
            # pickup_buckets=[5001],
            # post_buckets=[5004],
            randx=1,
        )

        msku_green, msku_blue = create_experiment_mskus(
            base_class=MarketSku,
            offer_kwargs={
                'title': 'offer sku2 green phone',
                'hyperid': 123,
                'sku': 2,
                'feedid': 200,
                'offerid': 200,
                'waremd5': 'Sku2-wdDXWsIiLVm1goleg',
                'blue_offers': [sku2_offer1, sku2_offer2],
                'glparams': [
                    GLParam(param_id=501, value=1),
                ],
                # delivery_buckets=[801],
                # pickup_buckets=[5001],
                'randx': 2,
            },
            offer_exp_kwargs={
                'hyperid': 124,
                'title': 'offer sku2 blue phone',
                'sku': 3,
            },
            exp_name='new-title',
        )

        cls.index.mskus += [msku_red, msku_green, msku_blue]

        cls.settings.use_external_snippets = True

    def test_sku_offers(self):
        # msku без экспериментальной
        for exp in ['', ';new-title=1']:
            response = self.report.request_json(
                'place=sku_offers&market-sku=1' '&rearr-factors=contex=1{}&rgb=blue&debug=da'.format(exp)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'id': '1',
                                'entity': 'sku',
                                'titles': {'raw': 'offer sku1 red phone'},
                                'offers': {
                                    'items': [
                                        {'titles': {'raw': 'offer sku1 red phone'}},
                                    ]
                                },
                                'product': {'id': 122},
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # вне эксперимента
        for exp in ['', ';new-title=0']:
            response = self.report.request_json(
                'place=sku_offers&market-sku=2' '&rearr-factors=contex=1{}&rgb=blue&debug=da'.format(exp)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'id': '2',
                                'entity': 'sku',
                                'titles': {'raw': 'offer sku2 green phone'},
                                'offers': {
                                    'items': [
                                        {'titles': {'raw': 'offer sku2 green phone'}},
                                    ]
                                },
                                'product': {'id': 123},
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # в эксперименте
        response = self.report.request_json(
            'place=sku_offers&market-sku=2' '&rearr-factors=contex=1;new-title=1&rgb=blue&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'id': '2',
                            'entity': 'sku',
                            'titles': {'raw': 'offer sku2 blue phone'},
                            'offers': {
                                'items': [
                                    {'titles': {'raw': 'offer sku2 blue phone'}},
                                ]
                            },
                            'product': {'id': 123},
                        }
                    ]
                }
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
