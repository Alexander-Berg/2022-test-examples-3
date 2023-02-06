#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import GLParam, GLType, Model, Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.gltypes = [
            GLType(param_id=201, hid=1, gltype=GLType.ENUM),
            GLType(param_id=202, hid=1, gltype=GLType.BOOL),
            GLType(param_id=203, hid=1, gltype=GLType.NUMERIC),
            GLType(param_id=204, hid=1, gltype=GLType.ENUM),
            GLType(param_id=205, hid=1, gltype=GLType.BOOL),
            GLType(param_id=206, hid=1, gltype=GLType.NUMERIC),
            GLType(param_id=207, hid=1, gltype=GLType.BOOL),
        ]

        # Params are in random order so that we can check that they are sorted by id in the XML output.
        cls.index.models = [
            Model(
                title='only_model_params (onlymodel)',
                hid=1,
                glparams=[
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=201, value=301),
                    GLParam(param_id=203, value=0.5),
                ],
            ),
            Model(
                title='model_and_offer',
                hyperid=500,
                hid=1,
                glparams=[
                    # Exactly the same as only_model_params (onlymodel).
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=201, value=301),
                    GLParam(param_id=203, value=0.5),
                    GLParam(param_id=205, value=1),
                ],
            ),
            Model(
                title='empty offer',
                hyperid=501,
                hid=1,
                glparams=[
                    GLParam(param_id=201, value=302),
                ],
            ),
        ]

        cls.index.offers = [
            Offer(
                title='only_offer_params (onlyoffer)',
                hid=1,
                glparams=[
                    # Exactly the same as for only_model_params (onlymodel) model, except for the order.
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=203, value=0.5),
                    GLParam(param_id=201, value=301),
                ],
            ),
            Offer(
                title='model_and_offer',
                hid=1,
                hyperid=500,
                glparams=[
                    # Params #201...203 are overriden by their corresponding model values,
                    # #204...206 are not.
                    GLParam(param_id=202, value=0),
                    GLParam(param_id=204, value=303),
                    GLParam(param_id=206, value=0.8),
                    GLParam(param_id=205, value=1),
                    GLParam(param_id=201, value=302),
                    GLParam(param_id=203, value=0.7),
                ],
            ),
            Offer(title='empty_offer', hyperid=501, hid=1),
        ]

    gl_filters_params_for_only_model_or_offer = {
        "filters": [
            {"id": "201", "values": [{"initialFound": 1, "id": "301"}]},
            {"id": "202", "values": [{"initialFound": 0, "id": "0"}, {"initialFound": 1, "id": "1"}]},
            {"id": "203", "values": [{"initialMax": "0.5", "initialMin": "0.5"}]},
        ]
    }

    def test_only_model_params(self):
        response = self.report.request_json('place=prime&text=onlymodel&hid=1')
        self.assertFragmentIn(response, self.gl_filters_params_for_only_model_or_offer)
        self.assertFragmentIn(response, {"results": [{"entity": "product"}]}, allow_different_len=False)

        response = self.report.request_json('place=prime&text=onlymodel&hid=1&glfilter=201:301')
        self.assertFragmentIn(response, self.gl_filters_params_for_only_model_or_offer)
        self.assertFragmentIn(response, {"results": [{"entity": "product"}]}, allow_different_len=False)

        response = self.report.request_json('place=prime&text=onlymodel&hid=1&glfilter=201:301&glfilter=202:0')
        self.assertFragmentIn(response, self.gl_filters_params_for_only_model_or_offer)
        self.assertFragmentNotIn(response, {"entity": "product"})

    def test_only_offer_params(self):
        response = self.report.request_json('place=prime&text=onlyoffer&hid=1')
        self.assertFragmentIn(response, self.gl_filters_params_for_only_model_or_offer)
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}]}, allow_different_len=False)

        response = self.report.request_json('place=prime&text=onlyoffer&hid=1&glfilter=201:301')
        self.assertFragmentIn(response, self.gl_filters_params_for_only_model_or_offer)
        self.assertFragmentIn(response, {"results": [{"entity": "offer"}]}, allow_different_len=False)

        response = self.report.request_json('place=prime&text=onlyoffer&hid=1&glfilter=201:301&glfilter=202:0')
        self.assertFragmentIn(response, self.gl_filters_params_for_only_model_or_offer)
        self.assertFragmentNotIn(response, {"entity": "offer"})

    def test_model_and_offer(self):
        _ = {
            "filters": [
                {"id": "201", "values": [{"initialFound": 2, "id": "301"}]},
                {"id": "202", "values": [{"initialFound": 0, "id": "0"}, {"initialFound": 2, "id": "1"}]},
                {"id": "203", "values": [{"initialMax": "0.5", "initialMin": "0.5"}]},
                {"id": "204", "values": [{"initialFound": 1, "id": "303"}]},
                {"id": "205", "values": [{"initialFound": 0, "id": "0"}, {"initialFound": 2, "id": "1"}]},
                {"id": "206", "values": [{"initialMax": "0.8", "initialMin": "0.8"}]},
            ]
        }

        r = '&rearr-factors=disable_panther_quorum=0;market_early_pre_early_gl_filtering=1'

        response = self.report.request_json('place=prime&text=model_and_offer&hid=1' + r)
        self.assertFragmentIn(response, {"entity": "offer"})
        self.assertFragmentIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "201", "values": [{"initialFound": 2, "id": "301"}]}]},
            allow_different_len=True,
        )

        # 301 is the value of param #201 from the model (should be used).
        # оба документа были учтены дважды в initialFound т.к. был дозапрос за фильтрами
        response = self.report.request_json('place=prime&text=model_and_offer&hid=1&glfilter=201:301' + r)
        self.assertFragmentIn(response, {"entity": "offer"})
        self.assertFragmentIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "201", "values": [{"initialFound": 4, "id": "301"}]}]},
            allow_different_len=True,
        )

        # 302 is the value of param #201 from the offer (should NOT be used).
        # документы были учтены в initialFound только один раз (только в дозапросе за фильтрами, в основном они не нашлись)
        response = self.report.request_json('place=prime&text=model_and_offer&hid=1&glfilter=201:302' + r)
        self.assertFragmentNotIn(response, {"entity": "offer"})
        self.assertFragmentNotIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "201", "values": [{"initialFound": 2, "id": "301"}, {"initialFound": 0, "id": "302"}]}
                ]
            },
            allow_different_len=True,
        )

        # 1 (true) is the value of param #202 from the model (should be used).
        response = self.report.request_json('place=prime&text=model_and_offer&hid=1&glfilter=202:1' + r)
        self.assertFragmentIn(response, {"entity": "offer"})
        self.assertFragmentIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "202",
                        "values": [{"initialFound": 4, "id": "1"}, {"initialFound": 0, "id": "0"}],
                    }  # 2 really
                ]
            },
            allow_different_len=True,
        )

        # 0 (false) is the value of param #202 from the offer (should NOT be used).
        response = self.report.request_json('place=prime&text=model_and_offer&hid=1&glfilter=202:0' + r)
        self.assertFragmentNotIn(response, {"entity": "offer"})
        self.assertFragmentNotIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "202", "values": [{"initialFound": 2, "id": "1"}, {"initialFound": 0, "id": "0"}]}]},
            allow_different_len=True,
        )

        # 0.5 is the value of param #203 from the model (should be used).
        response = self.report.request_json('place=prime&text=model_and_offer&hid=1&glfilter=203:0.1,0.6' + r)
        self.assertFragmentIn(response, {"entity": "offer"})
        self.assertFragmentIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "203", "values": [{"initialMax": "0.5", "initialMin": "0.5"}]}]},
            allow_different_len=True,
        )

        # Same as above but with only an upper bound in request (no lower bound at all).
        response = self.report.request_json('place=prime&text=model_and_offer&hid=1&glfilter=203:,0.6' + r)
        self.assertFragmentIn(response, {"entity": "offer"})
        self.assertFragmentIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "203", "values": [{"initialMax": "0.5", "initialMin": "0.5"}]}]},
            allow_different_len=True,
        )

        # 0.7 is the value of param #203 from the offer (should NOT be used).
        response = self.report.request_json('place=prime&text=model_and_offer&hid=1&glfilter=203:0.65,0.8' + r)
        self.assertFragmentNotIn(response, {"entity": "offer"})
        self.assertFragmentNotIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "203", "values": [{"initialMax": "0.5", "initialMin": "0.5"}]}]},
            allow_different_len=True,
        )

        # Param #204 is not in the model, but is in the offer, with value = 303...
        response = self.report.request_json(
            'place=prime&text=model_and_offer&hid=1&glfilter=201:301&glfilter=204:303' + r
        )
        self.assertFragmentIn(response, {"entity": "offer"})
        self.assertFragmentNotIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {"id": "204", "values": [{"initialFound": 2, "found": 2, "id": "303"}]},  # really 1, counted twice
                ]
            },
            allow_different_len=True,
        )

        # ...and not some other random value.
        response = self.report.request_json(
            'place=prime&text=model_and_offer&hid=1&glfilter=201:301&glfilter=204:304' + r
        )
        self.assertFragmentNotIn(response, {"entity": "offer"})
        self.assertFragmentNotIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {"id": "204", "values": [{"initialFound": 1, "id": "303"}]},
                ]
            },
            allow_different_len=True,
        )

        # Param #205 is not in the model, but is in the offer, with value = 1 (true)...
        response = self.report.request_json(
            'place=prime&text=model_and_offer&hid=1&glfilter=201:301&glfilter=205:1' + r
        )
        self.assertFragmentIn(response, {"entity": "offer"})

        self.assertFragmentIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {"id": "205", "values": [{"initialFound": 4, "id": "1"}, {"initialFound": 0, "id": "0"}]},
                ]
            },
            allow_different_len=True,
        )

        # Param #207 is neither in the offer nor the model.
        # Turns out that "a boolean param is absent" and "a false boolean param" is the same thing to report.
        # Still, the report treats this as if the param was set to false (see the preceding test).
        response = self.report.request_json(
            'place=prime&text=model_and_offer&hid=1&glfilter=201:301&glfilter=207:0' + r
        )
        self.assertFragmentIn(response, {"entity": "offer"})
        self.assertFragmentIn(response, {"entity": "product"})
        self.assertFragmentNotIn(response, {'filters': [{"id": "207"}]})

        # ...and not 1 (true) but here we show filter with initialFound=0
        response = self.report.request_json(
            'place=prime&text=model_and_offer&hid=1&glfilter=201:301&glfilter=207:1' + r
        )
        self.assertFragmentNotIn(response, {"entity": "offer"})
        self.assertFragmentNotIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {'filters': [{"id": "207", "values": [{"initialFound": 0, "id": "1"}, {"initialFound": 0, "id": "0"}]}]},
        )

        # Param #206 is not in the model, but is in the offer, with value = 0.8...
        response = self.report.request_json(
            'place=prime&text=model_and_offer&hid=1&glfilter=201:301&glfilter=206:0.75,1.0' + r
        )
        self.assertFragmentIn(response, {"entity": "offer"})
        self.assertFragmentNotIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "206", "values": [{"initialMax": "0.8", "initialMin": "0.8"}]}]},
            allow_different_len=True,
        )

        # ...and not some other random value.
        response = self.report.request_json(
            'place=prime&text=model_and_offer&hid=1&glfilter=201:301&glfilter=206:0.1,0.3' + r
        )
        self.assertFragmentNotIn(response, {"entity": "offer"})
        self.assertFragmentNotIn(response, {"entity": "product"})
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "206", "values": [{"initialMax": "0.8", "initialMin": "0.8"}]}]},
            allow_different_len=True,
        )

    def test_empty_offer(self):
        # Even though the offer has no params, it should still be connected to a
        # model and inherit its params.

        response = self.report.request_json(
            'place=prime&text=empty_offer&hid=1&glfilter=201:302' '&rearr-factors=market_early_pre_early_gl_filtering=1'
        )
        self.assertFragmentIn(
            response, {"filters": [{"id": "201", "values": [{"initialFound": 4, "id": "302"}]}]}  # 2 в действительности
        )
        self.assertFragmentIn(response, {"entity": "product"})
        self.assertFragmentIn(response, {"entity": "offer"})

    def test_glfilter_syntax_validation(self):
        # Test null filter string (should be ok)
        response = self.report.request_json('place=prime&text=model_and_offer&hid=1&glfilter=201:301;')
        self.assertFragmentIn(response, {"entity": "product"})

        # Test invalid filter string (should be ok with error in common log)
        response = self.report.request_json('place=prime&text=model_and_offer&hid=1&glfilter=201:301;20')
        self.assertFragmentIn(response, {"entity": "product"})
        self.error_log.expect("Parameter name or value is empty, glfilters: 201:301;20, offending filter: 20")

    @classmethod
    def prepare_test_model_and_offer_level_filters(cls):
        '''
        Создаем два фильтра: один относится только к моделям (cluster_filter=False), другой только к офферам (cluster_filter=True)
        Создадим по модели и офферу с этими параметрами, чтобы было что искать
        '''
        cls.index.gltypes += [
            GLType(param_id=401, hid=40, gltype=GLType.BOOL, cluster_filter=True),
            GLType(param_id=402, hid=40, gltype=GLType.BOOL, cluster_filter=False),
        ]

        cls.index.offers += [Offer(hid=40, glparams=[GLParam(param_id=401, value=1)])]

        cls.index.models += [Model(hid=40, glparams=[GLParam(param_id=402, value=1)])]

    def test_model_and_offer_level_filters(self):
        '''
        На плейсе prime должны показываться оба фильтра (и модельный и офферный)
        '''
        response = self.report.request_json('place=prime&hid=40')
        self.assertFragmentIn(response, {"filters": [{"id": "401"}, {"id": "402"}]})


if __name__ == '__main__':
    main()
