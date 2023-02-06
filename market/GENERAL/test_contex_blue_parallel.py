#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, Contex, MarketSku, Model
from core.testcase import TestCase, main
from core.types.contex import create_experiment_mskus


class Offers(object):
    sku8_offer1 = BlueOffer(feedid=20, waremd5='Sku8Offer1-IiLVm1goleg', offerid=200034)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.use_external_snippets = True
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']

    @classmethod
    def prepare_place_parallel(cls):
        cls.index.models += [
            Model(
                title='original model',
                hyperid=500,
                hid=6,
                has_blue_offers=True,
                picinfo='//avatars.mds.yandex.net/get-mpic/100500/img_bas_model_pic_id/orig#1000#1000',
            ),
            Model(
                title='experimental model',
                hyperid=501,
                hid=6,
                has_blue_offers=True,
                contex=Contex(parent_id=500, exp_name="contex-exp-2"),
                picinfo='//avatars.mds.yandex.net/get-mpic/200500/img_bas_model_pic_id/orig#1000#1000',
            ),
        ]

        msku_no_exp, msku_exp = create_experiment_mskus(
            base_class=MarketSku,
            offer_kwargs={
                'title': 'exceptionally original of highest quality',
                'hyperid': 500,
                'sku': 8,
                'feedid': 30,
                'offerid': 400034,
                'blue_offers': [Offers.sku8_offer1],
                'randx': 2,
            },
            offer_exp_kwargs={
                'hyperid': 501,
                'sku': 9,
                'title': 'exceptionally experimental of highest quality',
            },
            exp_name='contex-exp-2',
        )

        cls.index.mskus += [msku_no_exp, msku_exp]

    def test_place_parallel(self):
        """
        Проверяем, что тестовые офферы не появляются на выдаче плейса parallel, какие бы флаги мы ни указывали.
        """

        expected_output = {
            'market_model': [
                {
                    'picture': '//avatars.mds.yandex.net/get-mpic/100500/img_bas_model_pic_id/2hq',
                    'title': {"__hl": {"text": "Original model", "raw": True}},
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {
                                        "__hl": {"text": "exceptionally original of highest quality", "raw": True}
                                    },
                                }
                            }
                        ]
                    },
                }
            ],
        }

        expected_output_exp = {
            'market_model': [
                {
                    'picture': '//avatars.mds.yandex.net/get-mpic/200500/img_bas_model_pic_id/2hq',
                    'title': {"__hl": {"text": "Experimental model", "raw": True}},
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {
                                        "__hl": {"text": "exceptionally experimental of highest quality", "raw": True}
                                    },
                                }
                            }
                        ]
                    },
                }
            ],
        }

        # вне эксперимента и без contex
        # response = self.report.request_bs('place=parallel&text=exceptionally+highest+quality&ignore-mn=1')
        # self.assertFragmentIn(response, expected_output, allow_different_len=False)

        # вне эксперимента
        response = self.report.request_bs(
            'place=parallel&text=exceptionally+highest+quality&ignore-mn=1&rearr-factors=contex=1'
        )
        self.assertFragmentIn(response, expected_output, allow_different_len=False)

        # в эксперименте
        response = self.report.request_bs(
            'place=parallel&text=exceptionally+highest+quality&ignore-mn=1&rearr-factors=contex=1;contex-exp-2=1&debug=da'
        )
        # print response
        self.assertFragmentIn(response, expected_output_exp, allow_different_len=False)


if __name__ == '__main__':
    main()
