#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from unittest import skip
from core.matcher import EmptyList, NotEmptyList, NotEmpty, NoKey, Absent, Contains, Regex
from core.types import (
    BlueOffer,
    ConsequentParam,
    CreditTemplate,
    DependentParamValue,
    FormalizedParam,
    GLParam,
    GLType,
    GLValue,
    GradeDispersionItem,
    HyperCategory,
    HyperCategoryType,
    MainParamValue,
    MarketSku,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Region,
    ReportState,
    ReqwExtMarkupMarketCategory,
    ReqwExtMarkupMarketShop,
    ReqwExtMarkupMarketTiresKeyword,
    ReqwExtMarkupMarketTiresMark,
    ReqwExtMarkupToken,
    ReqwExtMarkupTokenChar,
    ReviewDataItem,
    Shop,
    VCluster,
    ResaleCondition,
    ResaleReason,
)
from core.types.autogen import Const
from core.types.fashion_parameters import FashionCategory
from core.testcase import TestCase, main


class T(TestCase):
    def get_base_search_text_debug_output(self, text):
        """
        Дебаг выдача с запросом на базовые.
        "мобильный телефон" -> "reqwizardText": "мобильный::4764 &/(-2 4) телефон::3623 ..."
        """
        words = [word + "::" for word in text.split()]
        return {"reqwizardText": Contains(*words)}

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        # hid: [1, 36]
        # gltypes: [1, 101]
        # nid: [1, 10]

        cls.index.hypertree += [
            HyperCategory(hid=1),
            HyperCategory(hid=2),
            HyperCategory(hid=3),
            HyperCategory(hid=4, visual=True),
            HyperCategory(hid=5, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=6),
            HyperCategory(hid=7),
            HyperCategory(hid=8),
            HyperCategory(hid=9),
            HyperCategory(hid=10),
            HyperCategory(hid=20),
            HyperCategory(hid=22),
            HyperCategory(hid=60, children=[HyperCategory(hid=61)]),
        ]

        cls.index.fashion_categories += [
            FashionCategory("CATEGORY_COMMON", 60),
            FashionCategory("CATEGORY_", 200),
            FashionCategory("CATEGORY_", 11),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=198118,
                children=[  # нужно, чтобы шинный работал совместно с кредитным
                    HyperCategory(hid=Const.TIRES_HID),
                ],
            )
        ]

        cls.index.navtree += [
            NavCategory(nid=1, hid=4),
            NavCategory(nid=2, hid=8),
        ]

        cls.index.vclusters += [
            VCluster(hid=4, vclusterid=1000000001),
            VCluster(hid=4, vclusterid=1000000002),
        ]

        cls.index.gltypes += [
            GLType(param_id=1, hid=1, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=2, hid=1, gltype=GLType.NUMERIC),
            GLType(param_id=3, hid=1, gltype=GLType.NUMERIC),
            GLType(param_id=5, hid=1, gltype=GLType.ENUM, values=[5, 6, 7, 8]),
            GLType(param_id=6, hid=1, gltype=GLType.ENUM, values=[1, 2, 3]),
            GLType(param_id=7, hid=4, gltype=GLType.ENUM, values=[1, 2, 3, 6]),
            GLType(param_id=8, hid=4, gltype=GLType.NUMERIC),
            # 1 kind params
            GLType(param_id=9, hid=5, gltype=GLType.ENUM, values=[1, 3], cluster_filter=False),
            GLType(param_id=10, hid=5, gltype=GLType.NUMERIC, cluster_filter=False),
            # 2 kind params
            GLType(param_id=91, hid=5, gltype=GLType.ENUM, values=[1, 3], cluster_filter=True),
            GLType(param_id=101, hid=5, gltype=GLType.NUMERIC, cluster_filter=True),
            GLType(param_id=12, hid=9, gltype=GLType.BOOL),
            GLType(param_id=13, hid=9, gltype=GLType.BOOL, hasboolno=True),
            GLType(param_id=16, hid=10, gltype=GLType.NUMERIC),
            GLType(param_id=30, hid=20, gltype=GLType.ENUM, values=[1, 2, 3]),
            GLType(param_id=31, hid=20, gltype=GLType.NUMERIC),
            GLType(param_id=32, hid=20, gltype=GLType.BOOL),
        ]

        cls.index.offers += [
            Offer(
                title="red iphone 5 16 GB pretty",
                hid=1,
                glparams=[GLParam(param_id=1, value=2), GLParam(param_id=2, value=5), GLParam(param_id=3, value=16)],
            ),
            Offer(title="romb", hid=2),
            Offer(title="table", hid=3),
            Offer(
                title="blue jacket size 60",
                hid=4,
                vclusterid=1000000001,
                fesh=123456,
                glparams=[
                    GLParam(param_id=7, value=6),
                    GLParam(param_id=8, value=4.5),
                    GLParam(param_id=101, value=30),
                ],
            ),
            Offer(title="red plate 20 cm", hid=5),
            Offer(
                title="big plate 30 mm",
                hid=5,
                glparams=[GLParam(param_id=91, value=1), GLParam(param_id=9, value=1), GLParam(param_id=101, value=30)],
            ),
            Offer(title="dark pink plate 30 mm", hid=5, glparams=[GLParam(param_id=101, value=30)]),
            Offer(title="hat", hid=6),
            # vcluster=1000000002 should not be empty
            Offer(title="blue jacket size 60", hid=4, vclusterid=1000000002),
            Offer(title="blue jacket size 60", hid=4),
            Offer(title="blue jacket size 60", hid=4),
            Offer(title="refrigerator", hid=8),
            Offer(title="wire bra", hid=9, glparams=[GLParam(param_id=12, value=1)]),
            Offer(title="wireless bra", hid=9, glparams=[GLParam(param_id=12, value=0)]),
            Offer(title="lacy bra", hid=9, glparams=[GLParam(param_id=13, value=1)]),
            Offer(title="not lacy bra", hid=9, glparams=[GLParam(param_id=13, value=0)]),
            Offer(title="fireworks 30 fires", hid=10, glparams=[GLParam(param_id=16, value=30)]),
            Offer(
                title="mouse keyboard and monitor",
                hid=20,
                glparams=[GLParam(param_id=30, value=1), GLParam(param_id=32, value=1)],
            ),
            Offer(
                title="monitor 15'' 16'' 19''",
                hid=20,
                glparams=[GLParam(param_id=30, value=3), GLParam(param_id=31, value=15), GLParam(param_id=32, value=1)],
            ),
            Offer(title="mouse wire or wireless", hid=20),
            # for test_add_1_kind_params_to_ex_guru_categ_redir
            Offer(
                title="red cute plate",
                hid=5,
                glparams=[
                    GLParam(param_id=9, value=1),
                    GLParam(param_id=10, value=20.0),
                ],
            ),
        ]

        # for test_add_1_kind_params_to_ex_guru_categ_redir
        cls.index.models += [
            Model(
                hyperid=1,
                title="red wonderful plate",
                hid=5,
                glparams=[
                    GLParam(param_id=9, value=1),
                    GLParam(param_id=10, value=20.0),
                ],
            )
        ]

        cls.formalizer.on_request(hid=1, query="red iphone 5 16 GB pretty").respond(
            formalized_params=[
                FormalizedParam(param_id=1, value=2, is_numeric=False, value_positions=(0, 3)),
                FormalizedParam(param_id=2, value=5, is_numeric=True, value_positions=(11, 12)),
                FormalizedParam(
                    param_id=3, value=16, is_numeric=True, value_positions=(13, 15), unit_positions=(16, 18)
                ),
                FormalizedParam(param_id=4, value=2, is_numeric=False),
                FormalizedParam(param_id=5, value=1, is_numeric=False),
                FormalizedParam(param_id=6, value=2, is_numeric=False, value_positions=(19, 25), rule_id=1),
            ]
        )

        cls.formalizer.on_request(hid=1, query="red iphone").respond(
            formalized_params=[FormalizedParam(param_id=1, value=2, is_numeric=False, param_xsl_name='')]
        )

        cls.formalizer.on_request(hid=4, query="blue jacket size 60").respond(
            formalized_params=[
                FormalizedParam(param_id=7, value=6, is_numeric=False, value_positions=(0, 4)),
                FormalizedParam(
                    param_id=8, value=4.5, is_numeric=True, value_positions=(17, 19), param_positions=(12, 16)
                ),
            ]
        )

        cls.reqwizard.on_request('blue jacket size 60').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=4),
                ReqwExtMarkupTokenChar(begin_char=4, end_char=19),
            ],
            found_shop_positions=[
                ReqwExtMarkupToken(
                    begin=0,
                    end=1,
                    data=ReqwExtMarkupMarketShop(shop_id=123456, alias_type='NAME', is_good_for_matching=True),
                ),
            ],
        )

        cls.formalizer.on_request(hid=4, query="blue jacket size 30").respond(
            formalized_params=[
                FormalizedParam(param_id=7, value=6, is_numeric=False, value_positions=(0, 4)),
                FormalizedParam(
                    param_id=8, value=2.25, is_numeric=True, value_positions=(17, 19), param_positions=(12, 16)
                ),
            ]
        )

        cls.formalizer.on_request(hid=5, query="red plates 20 cm").respond(
            formalized_params=[
                FormalizedParam(param_id=9, value=1, is_numeric=False, value_positions=(0, 3)),
                FormalizedParam(
                    param_id=10, value=20.0, is_numeric=True, value_positions=(11, 13), unit_positions=(14, 16)
                ),
            ]
        )

        cls.formalizer.on_request(hid=5, query="big plates 30 mm").respond(
            formalized_params=[
                FormalizedParam(param_id=91, value=1, is_numeric=False, value_positions=(0, 3)),
                FormalizedParam(
                    param_id=101, value=30.0, is_numeric=True, value_positions=(11, 13), unit_positions=(14, 16)
                ),
            ]
        )

        cls.formalizer.on_request(hid=5, query="red plates 30 mm").respond(
            formalized_params=[
                FormalizedParam(param_id=9, value=1, is_numeric=False, value_positions=(0, 3)),
                FormalizedParam(
                    param_id=101, value=30.0, is_numeric=True, value_positions=(11, 13), unit_positions=(14, 16)
                ),
            ]
        )

        cls.formalizer.on_request(hid=2, query="rombs").return_code(500)
        cls.formalizer.on_request(hid=3, query="tables").return_code(503)
        cls.formalizer.on_request(hid=6, query="hat").respond(known_category=False)
        cls.formalizer.on_request(hid=8, query="refrigerator").respond(formalized_params=[])

        cls.formalizer.on_request(hid=9, query="wire bra").respond(
            formalized_params=[FormalizedParam(param_id=12, value=1, is_numeric=False, param_positions=(0, 4))]
        )

        cls.formalizer.on_request(hid=9, query="wireless bra").respond(
            formalized_params=[FormalizedParam(param_id=12, value=0, is_numeric=False, param_positions=(0, 8))]
        )

        cls.formalizer.on_request(hid=9, query="lacy bra").respond(
            formalized_params=[FormalizedParam(param_id=13, value=1, is_numeric=False, param_positions=(0, 4))]
        )

        cls.formalizer.on_request(hid=9, query="not lacy bra").respond(
            formalized_params=[FormalizedParam(param_id=13, value=0, is_numeric=False, param_positions=(0, 8))]
        )

        cls.formalizer.on_request(hid=10, query="fireworks 30 fires").respond(
            formalized_params=[
                FormalizedParam(
                    param_id=16, value=30, is_numeric=True, param_positions=(10, 12), unit_positions=(13, 18)
                )
            ]
        )

        # match multiple values for params

        cls.formalizer.on_request(hid=20, query="mouse keyboard and monitor").respond(
            formalized_params=[
                FormalizedParam(param_id=30, value=1, is_numeric=False, value_positions=(0, 5)),
                FormalizedParam(param_id=30, value=2, is_numeric=False, value_positions=(6, 14)),
                FormalizedParam(param_id=30, value=3, is_numeric=False, value_positions=(19, 26)),
            ]
        )

        cls.formalizer.on_request(hid=20, query="monitor 15'' 16'' 19''").respond(
            formalized_params=[
                FormalizedParam(param_id=30, value=3, is_numeric=False, value_positions=(0, 7)),
                FormalizedParam(param_id=31, value=15.0, is_numeric=True, value_positions=(8, 12)),
                FormalizedParam(param_id=31, value=16.0, is_numeric=True, value_positions=(13, 17)),
                FormalizedParam(param_id=31, value=19.0, is_numeric=True, value_positions=(18, 22)),
            ]
        )

        cls.formalizer.on_request(hid=20, query="mouse wire or wireless").respond(
            formalized_params=[
                FormalizedParam(param_id=30, value=1, is_numeric=False, value_positions=(0, 5)),
                FormalizedParam(param_id=32, value=11, is_numeric=False, param_positions=(6, 10)),
                FormalizedParam(param_id=32, value=11, is_numeric=False, param_positions=(14, 22)),
            ]
        )

        cls.reqwizard.on_default_request().respond()
        cls.speller.on_default_request().respond()
        cls.formalizer.on_default_request().respond()

        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

    def test_add_params_to_ex_nonvis_categ_redir(self):
        text = 'red iphone 5 16 GB pretty'
        # TODO: MSSUP-763 - переход на новые факторы приводит к другому поведению редиректов, поэтому оставляем старое поведение
        rearr = '&rearr-factors=market_categ_dssm_factor_fast_calc=0;market_skip_broken_category_factors=0'

        response = self.report.request_json('place=prime&text={}&cvredirect=1'.format(text) + rearr)

        glfilters = ["1:2", "2:5,5", "3:16,16"]
        hid = "1"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}'.format(text, hid, rs, glfilters_query) + rearr
        )

        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "filters": [
                    {"id": "1", "isParametric": True},
                    {"id": "2", "isParametric": True},
                    {"id": "3", "isParametric": True},
                ],
            },
        )
        self.assertFragmentNotIn(response, {"filters": [{"id": "5"}]})
        self.assertFragmentNotIn(response, {"filters": [{"id": "6"}]})

        self.assertFragmentIn(
            response,
            {
                "query": {
                    "highlighted": [
                        {"value": "red", "highlight": True},
                        {"value": " iphone ", "highlight": NoKey("highlight")},
                        {"value": "5", "highlight": True},
                        {"value": " ", "highlight": NoKey("highlight")},
                        {"value": "16", "highlight": True},
                        {"value": " ", "highlight": NoKey("highlight")},
                        {"value": "GB", "highlight": True},
                        {"value": " pretty", "highlight": NoKey("highlight")},
                    ]
                }
            },
            preserve_order=True,
        )

    def test_add_params_to_ex_visual_categ_redir(self):
        text = 'blue jacket size 60'

        response = self.report.request_json('place=prime&text={}&cvredirect=1'.format(text))

        glfilters = ["7:6", "8:4.5,4.5"]
        hid = "4"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "nid": ["1"],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}'.format(text, hid, rs, glfilters_query)
        )

        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "filters": [{"id": "8", "isParametric": True}, {"id": "7", "isParametric": True}],
            },
        )

        self.assertFragmentIn(
            response,
            {
                "query": {
                    "highlighted": [
                        {"value": "blue", "highlight": True},
                        {"value": " jacket ", "highlight": NoKey("highlight")},
                        {"value": "size", "highlight": True},
                        {"value": " ", "highlight": NoKey("highlight")},
                        {"value": "60", "highlight": True},
                    ]
                }
            },
            preserve_order=True,
        )

    def test_add_1_kind_params_to_ex_guru_categ_redir(self):
        text = 'red plates 20 cm'
        response = self.report.request_json('place=prime&text={}&cvredirect=1&'.format(text))

        glfilters = ["9:1", "10:20,20"]
        hid = "5"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}'.format(text, hid, rs, glfilters_query)
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "isFuzzySearch": Absent(),
                    "isParametricSearch": True,
                    "results": [
                        {"entity": "product", "titles": {"raw": "red wonderful plate"}},
                        {"entity": "offer", "titles": {"raw": "red cute plate"}},
                    ],
                },
                "filters": [
                    {"id": "9", "isParametric": True},
                    {"id": "10", "isParametric": True},
                ],
                "query": {
                    "highlighted": [
                        {"value": "red", "highlight": True},
                        {"value": " plates ", "highlight": NoKey("highlight")},
                        {"value": "20", "highlight": True},
                        {"value": " ", "highlight": NoKey("highlight")},
                        {"value": "cm", "highlight": True},
                    ]
                },
            },
        )
        self.assertFragmentNotIn(response, {"filters": {"id": "91"}})

    def test_add_2_kind_params_to_guru_categ_redir(self):
        text = 'big plates 30 mm'

        response = self.report.request_json('place=prime&text={}&cvredirect=1&pp=18'.format(text))

        glfilters = ["91:1", "101:30,30"]
        hid = "5"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
            preserve_order=False,
        )

        rs = response.root['redirect']['params']['rs'][0]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}'.format(text, hid, rs, glfilters_query)
        )

        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "filters": [
                    {"id": "9", "isParametric": NoKey("isParametric")},
                    {"id": "91", "isParametric": True},
                    {"id": "101", "isParametric": True},
                ],
            },
            preserve_order=False,
        )

        self.assertFragmentIn(
            response,
            {
                "query": {
                    "highlighted": [
                        {"value": "big", "highlight": True},
                        {"value": " plates ", "highlight": NoKey("highlight")},
                        {"value": "30", "highlight": True},
                        {"value": " ", "highlight": NoKey("highlight")},
                        {"value": "mm", "highlight": True},
                    ]
                }
            },
            preserve_order=True,
        )

    def test_add_1_kind_and_2_kind_params_to_guru_categ_redir(self):
        text = 'red plates 30 mm'

        response = self.report.request_json('place=prime&text={}&cvredirect=1&pp=18'.format(text))

        glfilters = ["101:30,30", "9:1"]
        hid = "5"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {"was_redir": ["1"], "hid": [hid], "text": [text], "glfilter": glfilters},
                }
            },
            preserve_order=False,
        )

        rs = response.root['redirect']['params']['rs'][0]

        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}'.format(text, hid, rs, glfilters_query)
        )

        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "filters": [
                    {"id": "9", "isParametric": True},
                    {"id": "91", "isParametric": NoKey("isParametric")},
                    {"id": "101", "isParametric": True},
                ],
            },
            preserve_order=False,
        )

        self.assertFragmentIn(
            response,
            {
                "query": {
                    "highlighted": [
                        {"value": "red", "highlight": True},
                        {"value": " plates ", "highlight": NoKey("highlight")},
                        {"value": "30", "highlight": True},
                        {"value": " ", "highlight": NoKey("highlight")},
                        {"value": "mm", "highlight": True},
                    ]
                }
            },
            preserve_order=True,
        )

    def test_formalizer_internal_server_error(self):
        response = self.report.request_json('place=prime&text=rombs&cvredirect=1&debug=da')

        self.error_log.not_expect('HTTP/1.1 500 Internal Server Error')
        self.external_services_log.expect(service='formalizer', http_code=500)

        # still a correct redirect
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {"was_redir": ["1"], "srnum": ["1"], "hid": ["2"], "text": ["rombs"]},
                }
            },
        )

        self.assertFragmentNotIn(response, {"glfilter": NotEmpty()})

        self.assertFragmentNotIn(response, {"query": NotEmpty()})

        self.assertFragmentIn(response, {"rs": NotEmpty()})

        self.assertFragmentNotIn(response, Const.DEBUG_CANCELED_PARAM_MSG)

    def test_formalizer_unavailable(self):
        response = self.report.request_json('place=prime&text=tables&cvredirect=1&debug=da')

        self.error_log.not_expect('HTTP/1.1 503 Service Unavailable')
        self.external_services_log.expect(service='formalizer', http_code=503)

        # still a correct redirect
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {"was_redir": ["1"], "srnum": ["1"], "hid": ["3"], "text": ["tables"]},
                }
            },
        )

        self.assertFragmentNotIn(response, {"glfilter": NotEmpty()})

        self.assertFragmentNotIn(response, {"query": NotEmpty()})

        self.assertFragmentIn(response, {"rs": NotEmpty()})

        self.assertFragmentNotIn(response, Const.DEBUG_CANCELED_PARAM_MSG)

    def test_unknown_category(self):
        response = self.report.request_json('place=prime&text=hat&cvredirect=1&debug=da')

        # still a correct redirect
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {"was_redir": ["1"], "srnum": ["1"], "hid": ["6"], "text": ["hat"]},
                }
            },
        )

        self.assertFragmentNotIn(response, {"glfilter": NotEmpty()})

        self.assertFragmentNotIn(response, {"query": NotEmpty()})

        self.assertFragmentIn(response, {"rs": NotEmpty()})

        self.assertFragmentNotIn(response, Const.DEBUG_CANCELED_PARAM_MSG)

    def test_no_params_were_found(self):
        response = self.report.request_json('place=prime&text=refrigerator&cvredirect=1&debug=da')

        # still a correct redirect
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "srnum": ["1"],
                        "hid": ["8"],
                        "nid": ["2"],
                        "text": ["refrigerator"],
                    },
                }
            },
        )

        self.assertFragmentNotIn(response, {"glfilter": NotEmpty()})

        self.assertFragmentNotIn(response, {"query": NotEmpty()})

        self.assertFragmentIn(response, {"rs": NotEmpty()})

        self.assertFragmentIn(response, 'No parameters were found by formalizer')
        self.assertFragmentNotIn(response, Const.DEBUG_CANCELED_PARAM_MSG)

    def test_simple_boolean_param(self):
        text = 'wire bra'
        response = self.report.request_json('place=prime&text={}&cvredirect=1' ''.format(text))

        glfilters = ["12:1"]
        hid = "9"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}'.format(text, hid, rs, glfilters_query)
        )

        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "filters": [{"id": "12", "isParametric": True}, {"id": "13", "isParametric": NoKey("isParametric")}],
            },
        )

        self.assertFragmentIn(
            response,
            {
                "query": {
                    "highlighted": [
                        {"value": "wire", "highlight": True},
                        {"value": " bra", "highlight": NoKey("highlight")},
                    ]
                },
            },
            preserve_order=True,
        )

        # false params without hasboolno should be skipped
        response = self.report.request_json('place=prime&text=wireless+bra' '&cvredirect=1&debug=da')

    def test_boolean_param_with_hasboolno(self):
        text = 'lacy bra'
        response = self.report.request_json('place=prime&text={}&cvredirect=1' ''.format(text))

        glfilters = ["13:1"]
        hid = "9"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}'.format(text, hid, rs, glfilters_query)
        )

        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "filters": [{"id": "12", "isParametric": NoKey("isParametric")}, {"id": "13", "isParametric": True}],
            },
        )

        self.assertFragmentIn(
            response,
            {
                "query": {
                    "highlighted": [
                        {"value": "lacy", "highlight": True},
                        {"value": " bra", "highlight": NoKey("highlight")},
                    ]
                },
            },
            preserve_order=True,
        )

        text = 'not lacy bra'
        response = self.report.request_json('place=prime&text={}&cvredirect=1' ''.format(text))

        glfilters = ["13:0"]
        hid = "9"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}'.format(text, hid, rs, glfilters_query)
        )

        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "filters": [{"id": "12", "isParametric": NoKey("isParametric")}, {"id": "13", "isParametric": True}],
            },
        )

        self.assertFragmentIn(
            response,
            {
                "query": {
                    "highlighted": [
                        {"value": "not lacy", "highlight": True},
                        {"value": " bra", "highlight": NoKey("highlight")},
                    ]
                },
            },
            preserve_order=True,
        )

    def test_numeric_params_in_tail(self):
        text = 'fireworks 30 fires'
        # TODO: MSSUP-763 - переход на новые факторы приводит к другому поведению редиректов, поэтому оставляем старое поведение
        rearr = '&rearr-factors=market_skip_broken_category_factors=0;market_categ_dssm_factor_fast_calc=0'
        response = self.report.request_json('place=prime&text={}&cvredirect=1' ''.format(text) + rearr)

        glfilters = ["16:30,30"]
        hid = "10"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}'.format(text, hid, rs, glfilters_query) + rearr
        )

        self.assertFragmentIn(
            response, {"search": {"isParametricSearch": True}, "filters": [{"id": "16", "isParametric": True}]}
        )

        self.assertFragmentIn(
            response,
            {
                "query": {
                    "highlighted": [
                        {"value": "fireworks ", "highlight": NoKey("highlight")},
                        {"value": "30", "highlight": True},
                        {"value": " ", "highlight": NoKey("highlight")},
                        {"value": "fires", "highlight": True},
                    ]
                }
            },
            preserve_order=True,
        )

    def test_parametric_search_multiple_values(self):
        # multiple values in enum params

        text = 'mouse keyboard and monitor'
        # TODO: MSSUP-763 - переход на новые факторы приводит к другому поведению редиректов, поэтому оставляем старое поведение
        rearr = '&rearr-factors=market_skip_broken_category_factors=0;market_categ_dssm_factor_fast_calc=0'
        response = self.report.request_json('place=prime&text={}' '&cvredirect=1'.format(text) + rearr)

        glfilters = ["30:1,2,3"]
        hid = "20"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}'.format(text, hid, rs, glfilters_query) + rearr
        )

        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "filters": [
                    {"id": "32", "isParametric": NoKey("isParametric")},
                    {"id": "30", "isParametric": True},
                ],
            },
        )

        self.assertFragmentIn(
            response,
            {
                "query": {
                    "highlighted": [
                        {"value": "mouse", "highlight": True},
                        {"value": " ", "highlight": NoKey("highlight")},
                        {"value": "keyboard", "highlight": True},
                        {"value": " and ", "highlight": NoKey("highlight")},
                        {"value": "monitor", "highlight": True},
                    ]
                }
            },
            preserve_order=True,
        )

        # multiple values in numeric params

        text = 'monitor 15\'\' 16\'\' 19\'\''
        response = self.report.request_json('place=prime&text={}' '&cvredirect=1'.format(text) + rearr)

        glfilters = ["30:3", "31:15,19"]
        hid = "20"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}'.format(text, hid, rs, glfilters_query) + rearr
        )

        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "filters": [
                    {"id": "32", "isParametric": NoKey("isParametric")},
                    {"id": "30", "isParametric": True},
                    {"id": "31", "isParametric": True},
                ],
            },
        )

        self.assertFragmentIn(
            response,
            {
                "query": {
                    "highlighted": [
                        {"value": "monitor", "highlight": True},
                        {"value": " ", "highlight": NoKey("highlight")},
                        {"value": "15''", "highlight": True},
                        {"value": " 16'' ", "highlight": NoKey("highlight")},
                        {"value": "19''", "highlight": True},
                    ]
                }
            },
            preserve_order=True,
        )

        # multiple values in boolean params

        text = 'mouse wire or wireless'
        response = self.report.request_json('place=prime&text={}' '&cvredirect=1'.format(text) + rearr)

        glfilters = ["30:1"]
        hid = "20"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        self.assertFragmentNotIn(response, {"glfilter": ["32:1"]})

        self.assertFragmentNotIn(response, {"glfilter": ["32:0"]})

    @classmethod
    def prepare_rerequests_with_cuttext(cls):
        cls.index.hypertree += [HyperCategory(hid=11)]

        cls.index.gltypes += [
            GLType(param_id=17, hid=11, gltype=GLType.NUMERIC),
            GLType(param_id=18, hid=11, gltype=GLType.BOOL),
            GLType(param_id=19, hid=11, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=20, hid=11, gltype=GLType.BOOL),
            GLType(param_id=21, hid=11, gltype=GLType.BOOL),
        ]

        cls.index.offers += [
            Offer(
                title="torn levi blue jeans size 36",
                hid=11,
                glparams=[
                    GLParam(param_id=17, value=36),
                    GLParam(param_id=18, value=1),
                    GLParam(param_id=19, value=1),
                    GLParam(param_id=20, value=1),
                ],
            ),
            Offer(
                title="blue jeans",
                hid=11,
                glparams=[
                    GLParam(param_id=17, value=36),
                    GLParam(param_id=18, value=1),
                    GLParam(param_id=19, value=1),
                    GLParam(param_id=20, value=1),
                ],
            ),
            Offer(
                title="blue beautiful jeans",
                hid=11,
                glparams=[
                    GLParam(param_id=17, value=36),
                    GLParam(param_id=18, value=1),
                    GLParam(param_id=19, value=1),
                    GLParam(param_id=20, value=1),
                ],
            ),
            Offer(
                title="big blue jeans",
                hid=11,
                glparams=[
                    GLParam(param_id=17, value=36),
                    GLParam(param_id=18, value=1),
                    GLParam(param_id=19, value=1),
                    GLParam(param_id=20, value=1),
                ],
            ),
            Offer(
                title="torn jeans",
                hid=11,
                glparams=[
                    GLParam(param_id=17, value=36),
                    GLParam(param_id=18, value=1),
                    GLParam(param_id=19, value=1),
                    GLParam(param_id=20, value=1),
                ],
            ),
            Offer(
                title="jeans",
                hid=11,
                glparams=[
                    GLParam(param_id=17, value=36),
                    GLParam(param_id=18, value=1),
                    GLParam(param_id=19, value=2),
                    GLParam(param_id=20, value=1),
                ],
            ),
            Offer(
                title="torn armani",
                hid=11,
                glparams=[
                    GLParam(param_id=18, value=1),
                    GLParam(param_id=19, value=2),
                    GLParam(param_id=20, value=1),
                    GLParam(param_id=21, value=1),
                ],
            ),
            Offer(
                title="some unusual chinese jeans",
                hid=11,
                glparams=[GLParam(param_id=18, value=1), GLParam(param_id=19, value=2), GLParam(param_id=20, value=1)],
            ),
            Offer(
                title="OMFG so jeans such wow",
                hid=11,
                glparams=[GLParam(param_id=18, value=1), GLParam(param_id=19, value=2), GLParam(param_id=20, value=1)],
            ),
            Offer(title="new jeans", hid=11, glparams=[GLParam(param_id=20, value=1)]),
        ]

        cls.formalizer.on_request(hid=11, query="torn levi blue jeans size 36").respond(
            formalized_params=[
                FormalizedParam(
                    param_id=17, value=36, is_numeric=True, param_positions=(21, 25), value_positions=(26, 28)
                ),
                FormalizedParam(param_id=18, value=True, param_positions=(0, 4)),
                FormalizedParam(param_id=19, value=1, param_positions=(5, 9)),
                FormalizedParam(param_id=20, value=1),
            ]
        )

        cls.formalizer.on_request(hid=11, query="torn armani").respond(
            formalized_params=[
                FormalizedParam(param_id=18, value=True, param_positions=(0, 4)),
                FormalizedParam(param_id=19, value=2, param_positions=(5, 11)),
                FormalizedParam(param_id=20, value=1),
            ]
        )

        cls.formalizer.on_request(hid=11, query="new jeans").respond(
            formalized_params=[
                FormalizedParam(param_id=20, value=1),
            ]
        )

    def test_rerequests_with_formalized_text(self):
        """
        Проверяем, что под флагом работают различные варианты вырезания текста в параметрическом --
        смотрим на урезанные тексты в дебажной выдаче + удостоверяемся, что на фильтрацию
        документов влияет. Проверяем, что отключается урезание в случаях, когда не весь
        запрос наматчился.
        """

        # 1. Текст частично формализован: [<torn> <levi> blue jeans <size 36>]
        # При этом текст не вырезается даже под флагом, т.к. наматчен не целиком

        text = 'torn levi blue jeans size 36'
        hid = "11"
        glfilters = ["17:36,36", "18:1", "19:1", "20:1"]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])

        rearr_factors = '&rearr-factors=disable_panther_quorum=0'
        rearr_factors += '&rearr-factors=market_enable_parametric_cut_text=1'
        response = self.report.request_json('place=prime&text={}&cvredirect=1'.format(text) + rearr_factors)

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]

        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}&debug=da'.format(text, hid, rs, glfilters_query) + rearr_factors
        )
        self.assertFragmentIn(response, self.get_base_search_text_debug_output(text))

        # Запрос подсвечивается
        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "query": {
                    "highlighted": [
                        {
                            "value": "torn",
                            "highlight": True,
                        },
                        {
                            "value": " ",
                            "highlight": NoKey("highlight"),
                        },
                        {
                            "value": "levi",
                            "highlight": True,
                        },
                        {
                            "value": " blue jeans ",
                            "highlight": NoKey("highlight"),
                        },
                        {
                            "value": "size",
                            "highlight": True,
                        },
                        {
                            "value": " ",
                            "highlight": NoKey("highlight"),
                        },
                        {
                            "value": "36",
                            "highlight": True,
                        },
                    ]
                },
            },
        )

        # Часть документов не находится -- не набирается кворум
        for title in ("torn levi blue jeans size 36",):
            self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": title}})
        for title in ("jeans", "blue jeans", "blue beautiful jeans", "big blue jeans", "torn jeans"):
            self.assertFragmentNotIn(response, {"entity": "offer", "titles": {"raw": title}})

        # 2. Текст полностью формализован: [<torn> <armani>]
        text = 'torn armani'
        hid = "11"
        glfilters = ["18:1", "19:2", "20:1"]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])

        response = self.report.request_json('place=prime&text={}&cvredirect=1'.format(text))

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]

        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}&debug=da'.format(text, hid, rs, glfilters_query)
        )
        self.assertFragmentIn(response, self.get_base_search_text_debug_output(text))
        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "query": {
                    "highlighted": [
                        {
                            "value": "torn",
                            "highlight": True,
                        },
                        {
                            "value": " ",
                            "highlight": NoKey("highlight"),
                        },
                        {
                            "value": "armani",
                            "highlight": True,
                        },
                    ]
                },
            },
        )

        for title in ("some unusual chinese jeans", "OMFG so jeans such wow"):
            self.assertFragmentNotIn(response, {"entity": "offer", "titles": {"raw": title}})

        # С флагом полностью наматченный запрос становится пустым, появляется suggest_text

        rearr_factors = '&rearr-factors=market_enable_parametric_cut_text=1'
        response = self.report.request_json('place=prime&text={}&cvredirect=1'.format(text) + rearr_factors)

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "suggest_text": [text],
                        "text": Absent(),
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]

        response = self.report.request_json(
            'place=prime&hid={}&rs={}&{}&debug=da'.format(hid, rs, glfilters_query + rearr_factors)
        )
        debug_cuttext_msg = "Reduced text to cutText without params: [{}] -> [{}]".format(text, '')
        self.assertFragmentIn(response, debug_cuttext_msg)

        # И всё равно остаётся подсвеченный запрос

        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "query": {
                    "highlighted": [
                        {
                            "value": "torn",
                            "highlight": True,
                        },
                        {
                            "value": " ",
                            "highlight": NoKey("highlight"),
                        },
                        {
                            "value": "armani",
                            "highlight": True,
                        },
                    ]
                },
            },
        )

        # 3. Ничего не формализуется: [new jeans]
        # Опять же текст не вырезается даже под флагом, т.к. наматчен не целиком

        text = 'new jeans'
        hid = "11"
        glfilters = ["20:1"]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])

        rearr_factors = '&rearr-factors=market_enable_parametric_cut_text=1'

        response = self.report.request_json('place=prime&text={}&cvredirect=1'.format(text) + rearr_factors)

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]

        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}&debug=da'.format(text, hid, rs, glfilters_query) + rearr_factors
        )

        self.assertFragmentIn(response, self.get_base_search_text_debug_output(text))

        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "query": {
                    "highlighted": [
                        {
                            "value": "new jeans",
                            "highlight": NoKey("highlight"),
                        }
                    ]
                },
            },
        )

        for title in ("new jeans", "torn jeans"):
            self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": title}})

    def test_rerequests_with_formalized_text_rs_version(self):
        """
        Другая версия ведения на бестекст в случае, если формализовался весь запрос:
        1) Отдаем редирект на текст, в рс запоминаем результат формализации
        2) При редиректе используем бестекст, если результат формализации совпадает с запросом полностью
        """

        # 1. Текст полностью формализован: [<torn> <armani>]
        text = 'torn armani'
        hid = "11"
        glfilters = ["18:1", "19:2", "20:1"]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])

        # С флагом полностью наматченный запрос остается редирект на текст

        rearr_factors = '&rearr-factors=market_enable_parametric_cut_text_to_rs=1'
        response = self.report.request_json('place=prime&text={}&cvredirect=1&debug=da'.format(text) + rearr_factors)

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["11"],
                        "rs": [NotEmpty()],
                        "hid": [hid],
                        "suggest_text": Absent(),
                        "text": [text],
                        "glfilter": glfilters,
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]

        response = self.report.request_json(
            'place=prime&hid={}&rs={}&{}&debug=da&text={}'.format(hid, rs, glfilters_query + rearr_factors, text)
        )
        debug_cuttext_msg = "Reduced text to cutText without params: [{}] -> [{}]".format(text, '')
        self.assertFragmentIn(response, debug_cuttext_msg)
        debug_textless_msg = "we can search textless"
        self.assertFragmentIn(response, debug_textless_msg)
        # в шоу логе пишется специальное магическое слово для бестекста
        self.show_log.expect(query_context='b956d5f0d5c4dopaliha666 11', title="torn armani")

        # И всё равно остаётся подсвеченный запрос
        self.assertFragmentIn(
            response,
            {
                "search": {"isParametricSearch": True},
                "query": {
                    "highlighted": [
                        {
                            "value": "torn",
                            "highlight": True,
                        },
                        {
                            "value": " ",
                            "highlight": NoKey("highlight"),
                        },
                        {
                            "value": "armani",
                            "highlight": True,
                        },
                    ]
                },
            },
        )

        # Убираем фильтр, поиск опять текстовый
        glfilters = ["18:1"]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&hid={}&rs={}&{}&debug=da&text={}'.format(hid, rs, glfilters_query + rearr_factors, text)
        )
        debug_textless_msg = "Parametric text search with text: {}".format(text)
        self.assertFragmentIn(response, debug_textless_msg)

        # все фильтры формализованы, но еще добавляется фильтр в запросе - все равно бестекст
        glfilters = ["18:1", "19:2", "20:1", "21:1"]
        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&hid={}&rs={}&{}&debug=da&text={}'.format(hid, rs, glfilters_query + rearr_factors, text)
        )
        debug_textless_msg = "we can search textless"
        self.assertFragmentIn(response, debug_textless_msg)

    @classmethod
    def prepare_too_short_cuttext_empty_under_reverse_flag(cls):
        cls.index.hypertree += [
            HyperCategory(hid=23),
        ]

        cls.index.offers += [
            Offer(
                title="телефон с 3g",
                hid=23,
                glparams=[
                    GLParam(param_id=38, value=1),
                    GLParam(param_id=39, value=1),
                ],
            ),
            Offer(
                title="huawei p9",
                hid=23,
                glparams=[
                    GLParam(param_id=40, value=1),
                ],
            ),
            Offer(
                title="huawei p10",
                hid=23,
                glparams=[
                    GLParam(param_id=40, value=1),
                ],
            ),
        ]

        cls.index.gltypes += [
            GLType(param_id=38, hid=23, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=39, hid=23, gltype=GLType.BOOL),
            GLType(param_id=40, hid=23, gltype=GLType.ENUM, values=[1, 2]),
        ]

        cls.formalizer.on_request(hid=23, query='телефон с 3g').respond(
            formalized_params=[
                FormalizedParam(param_id=38, value=1, value_positions=(0, 7)),
                FormalizedParam(param_id=39, value=True, value_positions=(10, 12)),
            ]
        )

        cls.formalizer.on_request(hid=23, query='huawei p9').respond(
            formalized_params=[
                FormalizedParam(param_id=40, value=1, value_positions=(0, 6)),
            ]
        )

    @classmethod
    def prepare_parametric_specification(cls):
        cls.index.gltypes += [
            GLType(param_id=35, hid=22, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=36, hid=22, gltype=GLType.BOOL),
            GLType(param_id=37, hid=22, gltype=GLType.NUMERIC),
            GLType(param_id=38, hid=22, gltype=GLType.BOOL),  # size: small - 0, big - 1
        ]

        cls.index.offers += [
            # the next 2 will be filtered out by &text=reusable black pencil,
            # but won't by &glfilter=35:2&glfilter=36:1
            Offer(
                title="big reusable black pencil",
                hid=22,
                glparams=[
                    GLParam(param_id=35, value=2),
                    GLParam(param_id=36, value=1),
                    GLParam(param_id=38, value=1),
                ],
            ),
            Offer(
                title="small reusable black pencil",
                hid=22,
                glparams=[
                    GLParam(param_id=35, value=2),
                    GLParam(param_id=36, value=1),
                    GLParam(param_id=38, value=0),
                ],
            ),
            # the next 2 will be filtered out by &glfilter=35:2,
            # but won't by &text=reusable black pencil
            Offer(
                title="small reusable brown pencil",
                hid=22,
                glparams=[
                    GLParam(param_id=35, value=1),
                    GLParam(param_id=36, value=1),
                    GLParam(param_id=37, value=2.0),
                    GLParam(param_id=38, value=0),
                ],
            ),
            Offer(
                title="small reusable pencil",
                hid=22,
                glparams=[
                    GLParam(param_id=37, value=2.0),
                    GLParam(param_id=38, value=0),
                ],
            ),
        ]

        cls.formalizer.on_request(hid=22, query="reusable black pencil").respond(
            formalized_params=[
                FormalizedParam(param_id=35, value=2, value_positions=(9, 14)),
                FormalizedParam(param_id=36, value=True, param_positions=(0, 8)),
            ]
        )

        cls.formalizer.on_request(hid=22, query="erusable blakc pencli").respond()

        cls.speller.on_request(text='erusable blakc pencli').respond(
            originalText='<fix>er</fix>usable bla<fix>kc</fix> penc<fix>li</fix>',
            fixedText='<fix>re</fix>usable bla<fix>ck</fix> penc<fix>il</fix>',
        )

    def check_parametric_specification_works(self, query):
        response = self.report.request_json(query)

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "isFuzzySearch": Absent(),
                    "isParametricSearch": True,
                    "results": [
                        {"entity": "offer", "titles": {"raw": "big reusable black pencil"}},
                        {"entity": "offer", "titles": {"raw": "small reusable black pencil"}},
                    ],
                },
                "filters": [
                    {
                        "id": "35",
                        "isParametric": True,
                        "values": [
                            {
                                "id": "1",
                                "checked": NoKey("checked"),
                            },
                            {
                                "id": "2",
                                "checked": True,
                            },
                        ],
                    },
                    {
                        "id": "36",
                        "isParametric": True,
                        "values": [
                            {
                                "id": "0",
                                "checked": NoKey("checked"),
                            },
                            {
                                "id": "1",
                                "checked": True,
                            },
                        ],
                    },
                ],
                "query": {
                    "highlighted": [
                        {
                            "value": "reusable",
                            "highlight": True,
                        },
                        {
                            "value": " ",
                            "highlight": NoKey("highlight"),
                        },
                        {
                            "value": "black",
                            "highlight": True,
                        },
                        {
                            "value": " pencil",
                            "highlight": NoKey("highlight"),
                        },
                    ]
                },
            },
        )

        self.assertFragmentNotIn(response, {"entity": "offer", "titles": {"reusable black pencil"}})

    def check_parametric_specification_doesnt_work(self, query):
        response = self.report.request_json(query)

        self.assertFragmentNotIn(response, {"isParametricSearch": True})

        self.assertFragmentNotIn(response, {"filters": [{"isParametric": True}]})

        self.assertFragmentNotIn(response, {"query": NotEmpty()})

        self.assertFragmentNotIn(response, {"entity": "offer", "titles": {"big pencil"}})

        self.assertFragmentNotIn(response, {"entity": "offer", "titles": {"small pencil"}})

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "small reusable brown pencil"}},
                    {"entity": "offer", "titles": {"raw": "small reusable pencil"}},
                ]
            },
        )

        self.assertFragmentNotIn(response, Const.DEBUG_CANCELED_PARAM_MSG)

    # see https://st.yandex-team.ru/MARKETOUT-8201
    def test_parametric_specification_works(self):
        '''Дополнительная спецификация параметров в тексте запроса при заданной категории и параметре cvredirect=3'''
        text = "reusable black pencil"
        hid = 22

        # по запросу reusable black pencil нахоятся два оффера, которые не имеют
        # нужного glfilter=35:2, параметризующегося из black при указании категории,
        # и два офера с этим параметром
        response = self.report.request_json('place=prime&text=reusable black pencil')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "small reusable brown pencil"}},
                    {"titles": {"raw": "small reusable pencil"}},
                    {"titles": {"raw": "small reusable black pencil"}},
                    {"titles": {"raw": "big reusable black pencil"}},
                ]
            },
            allow_different_len=False,
        )

        # по запросу с &glfilter=35:2 находятся два оффера имеющих данный цвет:black
        response = self.report.request_json('place=prime&hid={}&glfilter=35:2&glfilter=36:1'.format(hid))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "small reusable black pencil"}},
                    {"titles": {"raw": "big reusable black pencil"}},
                ]
            },
            allow_different_len=False,
        )

        # проверяем, что запрос reusable black pencil будет дополнительно специфицирован,
        # после чего слова reusable и black не будут вырезаны, т.к. неполный матчинг,
        # а офферы будут фильтроваться по glfilter=35:2 и glfilter:36:1, которые были параметризованы из запроса
        query = "place=prime&text={}&hid={}&cvredirect=3".format(text, hid)
        self.check_parametric_specification_works(query)

    # see https://st.yandex-team.ru/MARKETOUT-8201
    def test_parametric_specification_with_spell_err_works(self):
        '''Дополнительная спецификация параметров в тексте запроса при заданной категории и параметре cvredirect=3
        Если в запросе ошибка - то при пустой выдаче (или при ответе spellchecker-а c reliablility > 10000)
        ошибка будет исправлена а после этого будет применен параметрический'''
        text = "reusable black pencil"
        text_spell_err = "erusable blakc pencli"
        hid = 22

        query = "place=prime&text={}&hid={}&cvredirect=3".format(text_spell_err, hid)

        # проверяем, что запрос erusable blakc pencli будет исправлен в reusable black pencil,
        # после чего слова reusable и black не будут вырезаны, т.к. неполный матчинг,
        # а офферы будут фильтроваться по glfilter=35:2 и glfilter:36:1, которые были параметризованы из запроса
        self.check_parametric_specification_works(query)

        response = self.report.request_json(query)

        self.assertFragmentIn(
            response,
            {
                "spellchecker": {
                    "old": text_spell_err,
                    "new": {
                        "raw": text,
                        "highlighted": [
                            {"value": "re", "highlight": True},
                            {"value": "usable bla"},
                            {"value": "ck", "highlight": True},
                            {"value": " penc"},
                            {"value": "il", "highlight": True},
                        ],
                    },
                },
            },
        )

    # see https://st.yandex-team.ru/MARKETOUT-17237
    def test_parametric_specification_with_glfilters_and_alice_1_works(self):
        '''Параметры специфицируются если пользователем были заданы какие-либо gl-фильтры
        и alice=1'''
        text = "reusable black pencil"
        gl_filter = "38:0"  # ищем small
        hid = 22

        query = "place=prime&text={}&hid={}&cvredirect=3&alice=1&glfilter={}".format(text, hid, gl_filter)

        response = self.report.request_json(query)

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "isFuzzySearch": Absent(),
                    "isParametricSearch": True,
                    "results": [
                        {"entity": "offer", "titles": {"raw": "small reusable black pencil"}},
                    ],
                },
                "filters": [
                    {
                        "id": "35",
                        "isParametric": True,
                        "values": [
                            {
                                "id": "1",
                                "checked": NoKey("checked"),
                            },
                            {
                                "id": "2",
                                "checked": True,
                            },
                        ],
                    },
                    {
                        "id": "36",
                        "isParametric": True,
                        "values": [
                            {
                                "id": "0",
                                "checked": NoKey("checked"),
                            },
                            {
                                "id": "1",
                                "checked": True,
                            },
                        ],
                    },
                    {
                        "id": "38",
                        "isParametric": NoKey("isParametric"),
                        "values": [
                            {
                                "id": "0",
                                "checked": True,
                            },
                            {
                                "id": "1",
                                "checked": NoKey("checked"),
                            },
                        ],
                    },
                ],
                "query": {
                    "highlighted": [
                        {
                            "value": "reusable",
                            "highlight": True,
                        },
                        {
                            "value": " ",
                            "highlight": NoKey("highlight"),
                        },
                        {
                            "value": "black",
                            "highlight": True,
                        },
                        {
                            "value": " pencil",
                            "highlight": NoKey("highlight"),
                        },
                    ]
                },
            },
        )

    # see https://st.yandex-team.ru/MARKETOUT-8201
    def test_parametric_specification_without_cvredirect_3_doesnt_work(self):
        '''Если не выставлен cvredirect=3 то дополнительная спецификация параметров не запускается'''
        text = "reusable black pencil"
        hid = 22

        query = "place=prime&text={}&hid={}&debug=da".format(text, hid)

        self.check_parametric_specification_doesnt_work(query)

    def test_parametric_specification_with_glfilters_doesnt_work(self):
        '''Параметры не специфицируются если пользователем были заданы какие-либо gl-фильтры'''
        text = "reusable black pencil"
        hid = 22

        query = "place=prime&text={}&hid={}&cvredirect=3" "&glfilter=37:1,3&debug=da".format(text, hid)

        self.check_parametric_specification_doesnt_work(query)

    # see https://st.yandex-team.ru/MARKETOUT-8201
    def test_parametric_specification_without_hid_doesnt_work(self):
        '''Параметры не специфицируются если не задана категория'''
        text = "reusable black pencil"
        _ = 22

        query = "place=prime&text={}&cvredirect=3&debug=da".format(text)

        self.check_parametric_specification_doesnt_work(query)

    # see https://st.yandex-team.ru/MARKETOUT-8201
    def test_parametric_specification_without_text_doesnt_work(self):
        '''Параметры не специфицируются если в запросе отсутствует текст'''
        hid = 22

        query = "place=prime&hid={}&cvredirect=3&debug=da".format(hid)

        self.check_parametric_specification_doesnt_work(query)

    # see https://st.yandex-team.ru/MARKETOUT-8201
    def test_parametric_specification_with_cvredirect_1_doesnt_work(self):
        '''Дополнительная спецификация параметров не применяется если задан cvreidrect=1
        (работает только с cvredirect=3)
        '''
        text = "reusable black pencil"
        hid = 22

        query = "place=prime&text={}&hid={}&" "cvredirect=1&debug=da".format(text, hid)

        self.check_parametric_specification_doesnt_work(query)

    def test_add_report_state_on_parametric_specification(self):
        """
        Проверяем, что reportState в ответе проставляется, если сработала параметрическая
        спецификация
        """
        response = self.report.request_json(
            "place=prime&text=reusable+black+pencil&hid=22&cvredirect=3"
            "&rearr-factors=market_rs_on_parametric_specification=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "reportState": NotEmpty(),
                    "results": NotEmptyList(),
                },
            },
        )

        # Проверяем, что без флага reportState не добавляется
        response = self.report.request_json("place=prime&text=reusable+black+pencil&hid=22&cvredirect=3")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "reportState": NoKey("reportState"),
                    "results": NotEmptyList(),
                },
            },
        )

    def test_dont_add_report_state_without_parametric_specification(self):
        """
        Проверяем, что с отключенной парметрической спецификацией reportState
        в ответе не проставляется.
        """
        # Проверяем случай, когда праметрическая спецификация отключена впринципе
        # (не указан cvredirect=3)
        response = self.report.request_json(
            "place=prime&text=reusable+black+pencil&hid=22" "&rearr-factors=market_rs_on_parametric_specification=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "reportState": NoKey("reportState"),
                    "results": NotEmptyList(),
                },
            },
        )

        # Проверяем случай, когда праметрическая спецификация не сработала
        # (для text=reusable+pencil формализация не настроена)
        response = self.report.request_json(
            "place=prime&text=reusable+pencil&hid=22&cvredirect=3"
            "&rearr-factors=market_rs_on_parametric_specification=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "reportState": NoKey("reportState"),
                    "results": NotEmptyList(),
                },
            },
        )

    def test_dont_add_report_state_without_cvredirect_3_when_rs_specified(self):
        """
        Проверяем, что reportState в ответе не проставляется, если cvredirect != 3,
        и в rs заполнен parametric_search_state
        """
        # Получаю rs с parametric_search_state
        response = self.report.request_json("place=prime&text=reusable+black+pencil&cvredirect=1")
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "rs": [NotEmpty()],
                    },
                },
            },
        )
        rs = response["redirect"]["params"]["rs"][0]
        self.assertTrue(ReportState.parse(rs).search_state.HasField("parametric_search_state"))

        response = self.report.request_json(
            "place=prime&text=reusable+pencil&hid=22&rs="
            + rs
            + "&rearr-factors=market_rs_on_parametric_specification=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "reportState": NoKey("reportState"),
                    "results": NotEmptyList(),
                },
            },
        )

    @classmethod
    def prepare_add_parametric_specification(cls):
        cls.index.gltypes += [
            GLType(param_id=201, hid=200, gltype=GLType.ENUM, values=[1, 2]),  # litte
            GLType(param_id=202, hid=200, gltype=GLType.BOOL),  # pony
        ]
        cls.index.offers += [
            Offer(
                title="pretty little pony",
                hid=200,
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=201, value=3),
                    GLParam(param_id=202, value=1),
                ],
            ),
        ]
        cls.formalizer.on_request(hid=200, query="little pony").respond(
            formalized_params=[
                FormalizedParam(param_id=201, value=2, value_positions=(0, 6)),
                FormalizedParam(param_id=201, value=3, value_positions=(0, 6)),
                FormalizedParam(param_id=202, value=True, param_positions=(7, 11)),
            ]
        )
        cls.formalizer.on_request(hid=200, query="pretty pony").respond(
            formalized_params=[
                FormalizedParam(param_id=202, value=True, param_positions=(7, 11)),
            ]
        )

        cls.index.dssm.query_embedding.on(query='little pony').set(-0.1, -0.11, -0.12, -0.13)
        cls.index.dssm.query_embedding.on(query='pretty pony').set(-0.2, -0.21, -0.22, -0.23)
        cls.index.dssm.query_embedding.on(query='pony').set(-0.3, -0.31, -0.32, -0.33)
        cls.index.dssm.query_embedding.on(query='HID-200').set(-0.4, -0.41, -0.42, -0.43)
        cls.index.dssm.hard2_query_embedding.on(query='little pony').set(0.1, 0.11, 0.12, 0.13)
        cls.index.dssm.hard2_query_embedding.on(query='pretty pony').set(0.2, 0.21, 0.22, 0.23)
        cls.index.dssm.hard2_query_embedding.on(query='pony').set(0.3, 0.31, 0.32, 0.33)
        cls.index.dssm.hard2_query_embedding.on(query='HID-200').set(0.4, 0.41, 0.42, 0.43)

    def test_add_parametric_specification__full_formalizaton(self):
        response = self.report.request_json("place=prime&hid=200&cvredirect=3&text=little+pony&debug=da")
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 1},  # убеждаюсь, что specification находится в корне
                "filters": [  # убеждаюсь, что формализацию настроил правильно
                    {"id": "201", "values": [{"id": "2", "checked": True}, {"id": "3", "checked": True}]},
                    {"id": "202", "values": [{"id": "1", "checked": True}]},
                ],
                "specification": {
                    "parametric": {
                        "params": {
                            "glfilter": ["201:2,3", "202:1"],
                            "text": ["little%20pony"],
                        }
                    }
                },
            },
        )
        doc_factors = response["search"]["results"][0]["debug"]["factors"]

        # убеждаюсь, что факторы после уточнения такие же как при запросе
        # в категорию с параметрами из specification/parametric/params
        response = self.report.request_json(
            "place=prime&hid=200&text=little+pony&debug=da" "&glfilter=201:2&glfilter=202:1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"debug": {"factors": doc_factors}},
                    ]
                }
            },
        )

        # с флагом урезания текст становится пустым, вместо него отдаётся suggest_text
        response = self.report.request_json(
            "place=prime&hid=200&cvredirect=3&text=little+pony&debug=da&rearr-factors=market_enable_parametric_cut_text=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 1},  # убеждаюсь, что specification находится в корне
                "filters": [  # убеждаюсь, что формализацию настроил правильно
                    {"id": "201", "values": [{"id": "2", "checked": True}, {"id": "3", "checked": True}]},
                    {"id": "202", "values": [{"id": "1", "checked": True}]},
                ],
                "specification": {
                    "parametric": {
                        "params": {
                            "glfilter": ["201:2,3", "202:1"],
                            "text": [""],
                            "suggest_text": ["little%20pony"],
                        }
                    }
                },
            },
        )

    def test_add_parametric_specification_to_rs_full_formalizaton(self):
        # флаг, при котором не отдается редирект на бестекст, информация о формализации хранится в рс
        response = self.report.request_json(
            "place=prime&hid=200&cvredirect=3&text=little+pony&debug=da&rearr-factors=market_enable_parametric_cut_text_to_rs=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 1},  # убеждаюсь, что specification находится в корне
                "filters": [
                    {"id": "201", "values": [{"id": "2", "checked": True}, {"id": "3", "checked": True}]},
                    {"id": "202", "values": [{"id": "1", "checked": True}]},
                ],
                "specification": {
                    "parametric": {
                        "params": {
                            "glfilter": ["201:2,3", "202:1"],
                            "text": [
                                "little%20pony"
                            ],  # с этим флагом в параметрах редиректа остается текст, а информация о формализации в рс
                            "suggest_text": Absent(),
                        }
                    }
                },
            },
        )
        # но искали мы бестекстом:
        debug_textless_msg = "we can search textless"
        self.assertFragmentIn(response, debug_textless_msg)

        # теперь добавим глфильтров в запрос
        response = self.report.request_json(
            "place=prime&hid=200&cvredirect=3&text=little+pony&debug=da&rearr-factors=market_enable_parametric_cut_text_to_rs=1&glfilter=201:2,3"
        )
        # все равно идем в формализатор, ищем бестекстом:
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 1},  # убеждаюсь, что specification находится в корне
                "filters": [
                    {"id": "201", "values": [{"id": "2", "checked": True}, {"id": "3", "checked": True}]},
                    {"id": "202", "values": [{"id": "1", "checked": True}]},
                ],
                "specification": {
                    "parametric": {
                        "params": {
                            "glfilter": ["201:2,3", "202:1"],
                            "text": [
                                "little%20pony"
                            ],  # с этим флагом в параметрах редиректа остается текст, а информация о формализации в рс
                            "suggest_text": Absent(),
                        }
                    }
                },
            },
        )
        # искали мы бестекстом:
        debug_textless_msg = "we can search textless"
        self.assertFragmentIn(response, debug_textless_msg)

        # проверяем что есть рс в ответе
        self.assertFragmentIn(response, {"search": {"reportState": NotEmpty()}})
        rs = response["search"]["reportState"]

        # делаем следующий запрос (фронт отправит cvredirect=0) все фильтры зажаты из предыдущего запроса, поэтому будет бестекст
        response = self.report.request_json(
            "place=prime&hid=200&cvredirect=0&text=little+pony&debug=da"
            "&rearr-factors=market_enable_parametric_cut_text_to_rs=1&glfilter=201:3,2&glfilter=202:1&rs={}".format(rs)
        )
        # ищем бестекстом, ответ формализатора есть в рс:
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 1, "searchType": "Textless"},
            },
        )
        debug_textless_msg = "we can search textless"
        self.assertFragmentIn(response, debug_textless_msg)

        # делаем следующий запрос (фронт отправит cvredirect=0) один фильтр отжимаем, теперь опять текст
        response = self.report.request_json(
            "place=prime&hid=200&cvredirect=0&text=little+pony&debug=da"
            "&rearr-factors=market_enable_parametric_cut_text_to_rs=1&glfilter=202:1&rs={}".format(rs)
        )
        # ищем текстом, ответ формализатора есть в рс:
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 1, "searchType": "Text"},
            },
        )
        debug_textless_msg = "Parametric text search with text: {}".format("little pony")
        self.assertFragmentIn(response, debug_textless_msg)

    def test_true_text_search_by_show_log(self):
        # проверяем по шоулогу, что действительно искали текстом
        response = self.report.request_json(
            "place=prime&text=hali&cvredirect=1&rearr-factors=market_enable_parametric_cut_text_to_rs=1"
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "suggest_text": Absent(),
                        "text": ["hali"],
                        "glfilter": ["301:1"],
                        "hid": ["300"],
                        "rt": ["11"],  # RT_PARAMETRIC
                    }
                }
            },
            allow_different_len=False,
        )
        rs = response.root['redirect']['params']['rs'][0]
        # отжать фильтр
        response = self.report.request_json(
            "place=prime&text=hali&cvredirect=0&hid=300&rearr-factors=market_enable_parametric_cut_text_to_rs=1&rs={}".format(
                rs
            )
        )
        self.show_log.expect(query_context='hali')

    def test_add_parametric_specification__partial_formalizaton(self):
        response = self.report.request_json(
            "place=prime&hid=200&cvredirect=3&text=pretty+pony&debug=da&rearr-factors=market_enable_parametric_cut_text=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 1},  # убеждаюсь, что specification находится в корне
                "filters": [  # убеждаюсь, что формализацию настроил правильно
                    {"id": "201", "values": [{"id": "2", "checked": NoKey("checked")}]},
                    {"id": "202", "values": [{"id": "1", "checked": True}]},
                ],
                "specification": {
                    "parametric": {
                        "params": {
                            "glfilter": ["202:1"],
                            "text": ["pretty%20pony"],
                            "suggest_text": NoKey("suggest_text"),
                        }
                    }
                },
            },
        )
        doc_factors = response["search"]["results"][0]["debug"]["factors"]

        # убеждаюсь, что факторы после уточнения такие же как при запросе
        # в категорию с параметрами из specification/parametric/params
        response = self.report.request_json("place=prime&hid=200&text=pretty+pony&debug=da&glfilter=202:1")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"debug": {"factors": doc_factors}},
                    ]
                }
            },
        )

    def test_add_parametric_specification__empty_formalizaton(self):
        response = self.report.request_json(
            "place=prime&hid=200&cvredirect=3&text=pony&debug=da&rearr-factors=market_enable_parametric_cut_text=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 1},  # убеждаюсь, что specification находится в корне
                "filters": [  # убеждаюсь, что формализацию настроил правильно
                    {"id": "201", "values": [{"id": "2", "checked": NoKey("checked")}]},
                    {"id": "202", "values": [{"id": "1", "checked": NoKey("checked")}]},
                ],
                "specification": NoKey("specification"),
            },
        )
        doc_factors = response["search"]["results"][0]["debug"]["factors"]

        # убеждаюсь, что факторы после уточнения такие же как при запросе
        # в категорию с параметрами из specification/parametric/params
        response = self.report.request_json("place=prime&hid=200&text=pony&debug=da")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"debug": {"factors": doc_factors}},
                    ]
                }
            },
        )

    @classmethod
    def prepare_dont_cut_text_with_full_text_formalization(cls):
        cls.index.offers += [
            Offer(
                title="hali gali",
                hid=300,
                glparams=[
                    GLParam(param_id=301, value=1),
                ],
            ),
            Offer(
                title="not text search offer",
                hid=300,
                glparams=[
                    GLParam(param_id=301, value=1),
                ],
            ),
        ]
        cls.formalizer.on_request(hid=300, query="hali").respond(
            formalized_params=[
                FormalizedParam(param_id=301, value=1, param_positions=(0, 4)),
            ]
        )

    def test_dont_cut_text_with_full_text_formalization__redirect(self):
        response = self.report.request_json("place=prime&text=hali&cvredirect=1")
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": ["hali"],
                        "suggest_text": Absent(),
                        "glfilter": ["301:1"],
                        "hid": ["300"],
                        "rt": ["11"],  # RT_PARAMETRIC
                    }
                }
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=prime&text=hali&cvredirect=1&rearr-factors=market_enable_parametric_cut_text=1"
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": Absent(),
                        "suggest_text": ["hali"],
                        "glfilter": ["301:1"],
                        "hid": ["300"],
                        "rt": ["11"],  # RT_PARAMETRIC
                    }
                }
            },
            allow_different_len=False,
        )
        response = self.report.request_json(
            "place=prime&text=hali&cvredirect=1&rearr-factors=market_enable_parametric_cut_text_to_rs=1"
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "suggest_text": Absent(),
                        "text": ["hali"],
                        "glfilter": ["301:1"],
                        "hid": ["300"],
                        "rt": ["11"],  # RT_PARAMETRIC
                    }
                }
            },
            allow_different_len=False,
        )

    def test_dont_cut_text_with_full_text_formalization__cvredirect_3(self):
        response = self.report.request_json("place=prime&text=hali&hid=300&cvredirect=3")
        self.assertFragmentIn(
            response,
            {
                "search": {  # убеждаюсь, что поиск текстовый, и 2й оффер,
                    # не подходящий по тексту, не нашёлся
                    "total": 1,
                    "results": [{"titles": {"raw": "hali gali"}}],
                },
                "specification": {
                    "parametric": {
                        "params": {
                            "text": ["hali"],
                            "suggest_text": Absent(),
                            "glfilter": ["301:1"],
                        }
                    }
                },
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=prime&text=hali&hid=300&cvredirect=3&rearr-factors=market_enable_parametric_cut_text=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {  # оба оффера теперь находятся
                    "total": 2,
                },
                "specification": {
                    "parametric": {
                        "params": {
                            "text": [""],
                            "suggest_text": ["hali"],
                            "glfilter": ["301:1"],
                        }
                    }
                },
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_cancel_parametric_search_for_empty_serps_guru(cls):
        cls.index.hypertree += [HyperCategory(hid=24, output_type=HyperCategoryType.GURU)]

        cls.index.gltypes += [GLType(param_id=41, hid=24, gltype=GLType.ENUM, values=[1, 2])]

        cls.formalizer.on_request(hid=24, query='часы мужские').respond(
            formalized_params=[
                FormalizedParam(param_id=41, value=1, value_positions=(5, 12)),
            ]
        )

        # оффер найдется по запросу [часы мужские]
        # но не найдется по запросу и фильтру glfilter=41:1
        cls.index.offers += [
            # Оффер нужен, чтобы случился редирект в категорию
            # По параметрам он не найдётся, т.к. не выставлены glparams
            Offer(title="хронограф мужской кварцевый", hid=24, price=1000)
        ]

    # see https://st.yandex-team.ru/MARKETOUT-11263
    def test_cancel_parametric_search_for_empty_serps_guru(self):
        """
        Проверяем, что в гуру-категориях параметрический поиск отменится ещё до редиректа,
        если он может привести к пустому СЕРПу
        Проверяем также, что в случае контентного апи эта логика отключена для тяжелых запросов контентного апи (советник, виджеты)
        """

        # [часы мужские] - находится оффер "хронограф мужской кварцевый" и происходит редирект в категорию
        # запрос [часы мужские] формализуется и получается фильтр 41:1
        # по формализованному запросу с фильтром ничего не найдется
        # в результате параметрический будет отменен и случится редирект в категорию
        text = 'часы мужские'
        hid = "24"
        # TODO: MSSUP-763 - переход на новые факторы приводит к другому поведению редиректов, поэтому оставляем старое поведение
        rearr = '&rearr-factors=market_skip_broken_category_factors=0;market_categ_dssm_factor_fast_calc=0'
        request = 'place=prime&text={}&cvredirect=1&debug=da'.format(text) + rearr

        for query in [request, request + '&api=content', request + '&api=content&content-api-client=101']:
            response = self.report.request_json(query)

            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "target": "search",
                        "params": {
                            "was_redir": ["1"],
                            "hid": [hid],
                            "text": [text],
                            "glfilter": Absent(),  # параметрический был отменен
                        },
                    }
                },
            )
            self.assertFragmentIn(response, {"logicTrace": [Contains(Const.DEBUG_CANCELED_PARAM_MSG)]})

        for query in [request + '&api=content&client=sovetnik', request + '&api=content&client=widget']:
            response = self.report.request_json(query)

            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "target": "search",
                        "params": {
                            "was_redir": ["1"],
                            "hid": [hid],
                            "text": [text],
                            "glfilter": ["41:1"],
                            "rs": NotEmpty(),
                        },
                    }
                },
            )
            self.assertFragmentNotIn(response, {"logicTrace": [Contains(Const.DEBUG_CANCELED_PARAM_MSG)]})

    @classmethod
    def prepare_cancel_parametric_search_for_empty_serps_gurulight(cls):
        cls.index.hypertree += [HyperCategory(hid=25, output_type=HyperCategoryType.GURULIGHT)]

        cls.index.offers += [
            # Оффер нужен, чтобы случился редирект в категорию
            # По параметрам он не найдётся, т.к. не выставлены glparams
            Offer(title="киянка из резины", hid=25, price=1000)
        ]

        cls.index.gltypes += [GLType(param_id=42, hid=25, gltype=GLType.ENUM, values=[1, 2])]

        cls.formalizer.on_request(hid=25, query='киянка резиновая').respond(
            formalized_params=[
                FormalizedParam(param_id=42, value=1, value_positions=(7, 16)),
            ]
        )

        # https://st.yandex-team.ru/MARKETOUT-14238
        # "Надежно" исправляем "опечатку" так, что выдача по исправленному запросу становится пустой.
        # Должен произойти перезапрос без исправления, т.е. с исходным запросом.
        # В результате такое исправление опечатки должно никак не повлиять на выдачу.
        cls.speller.on_request(text='киянка резиновая').respond(
            originalText='киянка резиновая', fixedText='ки<fix>та</fix>янка резиновая', reliability=100500
        )

    # see https://st.yandex-team.ru/MARKETOUT-11263
    def test_cancel_parametric_search_for_empty_serps_gurulight(self):
        """
        Проверяем, что в гурулайт-категориях параметрический поиск отменится ещё до редиректа,
        если он может привести к пустому СЕРПу
        """
        text = 'киянка резиновая'
        response = self.report.request_json('place=prime&text={}&cvredirect=1&debug=da' ''.format(text))

        hid = "25"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {"was_redir": ["1"], "hid": [hid], "text": [text], "glfilter": Absent()},
                }
            },
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains(Const.DEBUG_CANCELED_PARAM_MSG)]})

    def test_cancel_parametric_specification_for_guru(self):
        """
        Проверяем, что репорт отменяет параметрическое уточнение в гуру-категориях,
        если по запросу с ним ничего не нашлось. Подробнее о параметрическом уточнении: MARKETOUT-8201
        """
        text = "часы мужские"
        hid = 24

        response = self.report.request_json("place=prime&text={}&hid={}&cvredirect=3&debug=da".format(text, hid))

        self.assertFragmentNotIn(response, {"isParametricSearch": True})

        self.assertFragmentNotIn(response, {"filters": [{"isParametric": True}]})

        self.assertFragmentNotIn(response, {"query": NotEmpty()})

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "хронограф мужской кварцевый"}},
                ]
            },
        )

    def test_cancel_parametric_specification_for_gurulight(self):
        """
        Проверяем, что репорт отменяет параметрическое уточнение в гурулайт-категориях,
        если по запросу с ним ничего не нашлось. Подробнее о параметрическом уточнении: MARKETOUT-8201
        """
        text = 'киянка резиновая'
        hid = 25

        response = self.report.request_json("place=prime&text={}&hid={}&cvredirect=3&debug=da".format(text, hid))

        self.assertFragmentNotIn(response, {"isParametricSearch": True})

        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "isParametric": True,
                    }
                ]
            },
        )

        self.assertFragmentNotIn(response, {"query": NotEmpty()})

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "киянка из резины"}},
                ]
            },
        )

    @classmethod
    def prepare_skip_1_symbol_params_guru(cls):
        cls.index.hypertree += [HyperCategory(hid=26, output_type=HyperCategoryType.GURU)]

        cls.index.offers += [
            Offer(
                title="велосипед изобретённый 9",
                hid=26,
                glparams=[
                    GLParam(param_id=43, value=1),
                    GLParam(param_id=44, value=6),
                ],
            ),
        ]

        cls.index.gltypes += [
            GLType(param_id=43, hid=26, gltype=GLType.BOOL),
            GLType(param_id=44, hid=26, gltype=GLType.NUMERIC),
        ]

        cls.formalizer.on_request(hid=26, query='велосипед изобретённый 9').respond(
            formalized_params=[
                FormalizedParam(param_id=43, value=1, value_positions=(10, 22)),
                FormalizedParam(param_id=44, value=9, is_numeric=True, value_positions=(23, 24)),
            ]
        )

    # see https://st.yandex-team.ru/MARKETOUT-11263
    def test_skip_1_symbol_params_guru(self):
        """
        Проверяем, что в параметрический поиск для гуру-категорий не попадают односимвольные параметры
        """
        text = 'велосипед изобретённый 9'
        # TODO: MSSUP-763 - переход на новые факторы приводит к другому поведению редиректов, поэтому оставляем старое поведение
        rearr = '&rearr-factors=market_skip_broken_category_factors=0;market_categ_dssm_factor_fast_calc=0'
        response = self.report.request_json(
            'place=prime&text={}&cvredirect=1'
            '&rearr-factors=market_guru_parametric_search_for_first_kind_filters=1'.format(text) + rearr
        )

        self.assertFragmentIn(response, {"redirect": NotEmpty()})

        self.assertFragmentIn(response, {"glfilter": ["43:1"]}, allow_different_len=False)

    @classmethod
    def prepare_dont_skip_1_symbol_params_gurulight(cls):
        cls.index.hypertree += [HyperCategory(hid=27, output_type=HyperCategoryType.GURULIGHT)]

        cls.index.offers += [
            Offer(
                title="костыль временный 6",
                hid=27,
                glparams=[
                    GLParam(param_id=45, value=1),
                    GLParam(param_id=46, value=6),
                ],
            ),
        ]

        cls.index.gltypes += [
            GLType(param_id=45, hid=27, gltype=GLType.BOOL),
            GLType(param_id=46, hid=27, gltype=GLType.NUMERIC),
        ]

        cls.formalizer.on_request(hid=27, query='костыль временный 6').respond(
            formalized_params=[
                FormalizedParam(param_id=45, value=1, value_positions=(8, 17)),
                FormalizedParam(param_id=46, value=6, is_numeric=True, param_positions=(18, 19)),
            ]
        )

    # see https://st.yandex-team.ru/MARKETOUT-11263
    def test_dont_skip_1_symbol_params_gurulight(self):
        """
        Проверяем, что в параметрический поиск для гурулайт-категорий попадают односимвольные параметры
        """
        text = 'костыль временный 6'
        response = self.report.request_json('place=prime&text={}&cvredirect=1&' ''.format(text))

        self.assertFragmentIn(response, {"redirect": NotEmpty()})

        self.assertFragmentIn(response, {"glfilter": ["45:1", "46:6,6"]}, allow_different_len=False)

    @classmethod
    def prepare_dont_formalize_filters_no_pos(cls):
        cls.index.hypertree += [HyperCategory(hid=28, output_type=HyperCategoryType.GURULIGHT)]

        cls.index.offers += [Offer(title="кисти", hid=28)]

        # positionless=True в LITE означает, что position=-1
        cls.index.gltypes += [GLType(param_id=51, hid=28, positionless=True, gltype=GLType.ENUM, values=[1, 2])]

        cls.formalizer.on_request(hid=28, query='кисти').respond(
            formalized_params=[
                FormalizedParam(param_id=51, value=1, is_numeric=False, param_positions=(0, 5)),
            ]
        )

    def test_dont_formalize_filters_no_pos(self):
        """
        Проверяем, что параметры с position=-1 не отдаются параметрическим поиском
        """
        response = self.report.request_json('place=prime&text=кисти&cvredirect=1')

        self.assertFragmentNotIn(response, {"glfilter": NotEmpty()})

        self.assertFragmentIn(response, {"rs": NotEmpty()})

    @classmethod
    def prepare_guru_category_not_found_by_name(cls):
        # Заводим гуру-категорию, которая найдётся по запросу
        cls.index.hypertree += [
            HyperCategory(hid=30, output_type=HyperCategoryType.GURU),
        ]

        # Заводим фильтры, учим формализатор их отдавать
        cls.index.gltypes += [GLType(param_id=56, hid=30, gltype=GLType.ENUM, values=[1, 2])]
        cls.formalizer.on_request(hid=30, query="мебель креслов").respond(
            formalized_params=[
                FormalizedParam(param_id=56, value=1, is_numeric=False, value_positions=(7, 14)),
            ]
        )

        # Учим реквизард отдавать информацию о найденной в запросе категории
        cls.reqwizard.on_request("мебель креслов").respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=6),
                ReqwExtMarkupTokenChar(begin_char=7, end_char=14),
            ],
            found_main_categories=[30],
            found_categories_positions=[ReqwExtMarkupToken(begin=1, end=2, data=ReqwExtMarkupMarketCategory(hid=30))],
        )

    def test_guru_category_not_found_by_name(self):
        """
        Проверяем, что для гуру-категорий НЕ работает параметрический поиск
        с определением категории по названию в запросе (даже с флагом
        market_guru_parametric_search_for_first_kind_filters)
        """
        text = 'мебель креслов'
        response = self.report.request_json(
            'place=prime&text={}&cvredirect=1&'
            'rearr-factors=market_guru_parametric_search_for_first_kind_filters=1'.format(text)
        )

        # Редирект не должен случиться в принципе, т.к. нет офферов, а редирект
        # по названию категории запрещён
        self.assertFragmentNotIn(response, {'redirect': NotEmpty()})

    @classmethod
    def prepare_dont_formalize_filters_no_filter_value(cls):
        cls.index.hypertree += [HyperCategory(hid=31)]

        # Заводим 2 значения одного фильтра с разными filter_value
        cls.index.gltypes += [
            GLType(
                param_id=57,
                hid=31,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='Феникс', filter_value=False),
                    GLValue(value_id=2, text='HEGGI', filter_value=True),
                ],
            )
        ]

        # Оффер приматчен к тому из них, у которого filter_value=True,
        # чтобы выдача не оказалась пустой
        cls.index.offers += [
            Offer(
                title="шкаф-купе HEGGI феникс",
                hid=31,
                glparams=[
                    GLParam(param_id=57, value=2),
                ],
            )
        ]

        # Учим формализатор их оба отдавать
        cls.formalizer.on_request(hid=31, query='шкаф-купе HEGGI феникс').respond(
            formalized_params=[
                FormalizedParam(param_id=57, value=1, is_numeric=False, param_positions=(10, 15)),
                FormalizedParam(param_id=57, value=2, is_numeric=False, param_positions=(16, 22)),
            ]
        )

    def test_dont_formalize_filters_no_filter_value(self):
        """
        Проверяем, что параметрическим поиском отдаются только параметры с filter_value=True
        """
        text = 'шкаф-купе HEGGI феникс'

        response = self.report.request_json("place=prime&text={}&cvredirect=1".format(text))

        glfilters = ["57:2"]
        hid = "31"

        self.assertFragmentIn(
            response, {"redirect": {"target": "search", "params": {"rs": [NotEmpty()], "hid": [hid], "text": [text]}}}
        )

        self.assertFragmentIn(response, {"glfilter": glfilters}, allow_different_len=False)

    @classmethod
    def prepare_only_bad_characters_left(cls):
        cls.index.hypertree += [HyperCategory(hid=32)]

        cls.index.gltypes += [
            GLType(param_id=58, hid=32, gltype=GLType.NUMERIC),
            GLType(param_id=59, hid=32, gltype=GLType.NUMERIC),
            GLType(param_id=60, hid=32, gltype=GLType.NUMERIC),
        ]

        cls.index.offers += [
            Offer(
                title='245/70/16',
                hid=32,
                glparams=[
                    GLParam(param_id=58, value=245),
                    GLParam(param_id=59, value=70),
                    GLParam(param_id=60, value=16),
                ],
            )
        ]

        # формализатор матчит параметры во всём запросе, кроме '//'
        cls.formalizer.on_request(hid=32, query='245/70/16').respond(
            formalized_params=[
                FormalizedParam(param_id=58, value=245, is_numeric=True, param_positions=(0, 3)),
                FormalizedParam(param_id=59, value=70, is_numeric=True, param_positions=(4, 6)),
                FormalizedParam(param_id=60, value=16, is_numeric=True, param_positions=(7, 9)),
            ]
        )

    def test_only_bad_characters_left(self):
        """
        Проверяем, что при вырезании из запроса всего, кроме небуквоцифр, поиск не будет отвечать
        сообщением "пустой запрос". Проверяем, что дозапрос за моделями при схлапывании т.ж. отработает
        без ошибок (добавляем &allow-collapsing=1)
        """
        text = '245/70/16'
        # TODO: MSSUP-763 - переход на новые факторы приводит к другому поведению редиректов, поэтому оставляем старое поведение
        rearr = '&rearr-factors=market_skip_broken_category_factors=0;market_categ_dssm_factor_fast_calc=0'

        response = self.report.request_json("place=prime&text={}&cvredirect=1".format(text) + rearr)

        glfilters = ["58:245,245", "59:70,70", "60:16,16"]
        hid = "32"

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {"rs": [NotEmpty()], "hid": [hid], "text": [text], "glfilter": glfilters},
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]

        glfilters_query = '&'.join(['glfilter=' + g for g in glfilters])
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}&allow-collapsing=1'.format(text, hid, rs, glfilters_query) + rearr
        )

        self.assertFragmentNotIn(
            response, {"error": {"code": "UNEXPECTED_EXCEPTION", "message": "Search request is empty. "}}
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "isParametricSearch": True,
                    "results": [{"entity": "offer", "titles": {"raw": "245/70/16"}}],
                },
                "filters": [
                    {"id": "58", "isParametric": True, "values": [{"min": "245", "max": "245", "checked": True}]},
                    {"id": "59", "isParametric": True, "values": [{"min": "70", "max": "70", "checked": True}]},
                    {"id": "60", "isParametric": True, "values": [{"min": "16", "max": "16", "checked": True}]},
                ],
                "query": {
                    "highlighted": [
                        {"value": "245", "highlight": True},
                        {"value": "/", "highlight": NoKey("highlight")},
                        {"value": "70", "highlight": True},
                        {"value": "/", "highlight": NoKey("highlight")},
                        {"value": "16", "highlight": True},
                    ]
                },
            },
        )

        self.assertEqual(response.code, 200)

    @classmethod
    def prepare_dont_remove_category_not_on_category_match(cls):
        cls.index.hypertree += [HyperCategory(hid=33)]

        cls.index.gltypes += [
            GLType(param_id=61, hid=33, gltype=GLType.ENUM, values=[GLValue(value_id=1), GLValue(value_id=2)])
        ]

        cls.index.offers += [
            Offer(title='Спиннер серебристый модный', hid=33, glparams=[GLParam(param_id=61, value=1)]),
            Offer(title='Зелёная крутящаяся шняга дешёвая', hid=33, glparams=[GLParam(param_id=61, value=2)]),
        ]

        cls.formalizer.on_request(hid=33, query='спиннер серебристый').respond(
            formalized_params=[FormalizedParam(param_id=61, value=1, is_numeric=False, param_positions=(8, 19))]
        )

        cls.formalizer.on_request(hid=33, query='зелёная крутящаяся шняга').respond(
            formalized_params=[FormalizedParam(param_id=61, value=2, is_numeric=False, param_positions=(0, 7))]
        )

        cls.reqwizard.on_request('спиннер серебристый').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=7),
                ReqwExtMarkupTokenChar(begin_char=8, end_char=19),
            ],
            found_main_categories=[33],
            found_categories_positions=[ReqwExtMarkupToken(begin=0, end=1, data=ReqwExtMarkupMarketCategory(hid=33))],
        )

        cls.reqwizard.on_request('зелёная крутящаяся шняга').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=7),
                ReqwExtMarkupTokenChar(begin_char=8, end_char=18),
                ReqwExtMarkupTokenChar(begin_char=19, end_char=24),
            ],
            found_extra_categories=[33],
            found_categories_positions=[
                ReqwExtMarkupToken(begin=1, end=3, data=ReqwExtMarkupMarketCategory(hid=33, is_main=False))
            ],
        )

    def check_remove_category_not_on_category_match(self, text, rearrs_texts, hid):
        for rearr, new_text in rearrs_texts:
            rearr_factors = '&rearr-factors={}'.format(rearr) if rearr else ''
            response = self.report.request_json(
                'place=prime&text={}&cvredirect=1&debug=da{}'.format(text, rearr_factors)
            )
            self.assertFragmentIn(response, self.get_base_search_text_debug_output(new_text))

            response = self.report.request_json(
                'place=prime&text={}&hid={}&cvredirect=3&debug=da{}'.format(text, hid, rearr_factors)
            )
            self.assertFragmentIn(response, {"isParametricSearch": True})
            self.assertFragmentIn(response, self.get_base_search_text_debug_output(new_text))

    def test_dont_remove_category_not_on_category_match(self):
        """
        Проверяем, что из запросов, по которым произошёл параметрический поиск
        (или параметрическое уточнение), имя категории НЕ вырезается
        """

        self.check_remove_category_not_on_category_match(
            text='спиннер серебристый',
            rearrs_texts=[
                # без флагов из запроса ничего не вырезается,
                # т.к. наматчился только параметр (не весь запрос)
                ('', 'спиннер серебристый'),
            ],
            hid=33,
        )

        self.check_remove_category_not_on_category_match(
            text='зелёная крутящаяся шняга',
            rearrs_texts=[
                # без флагов из запроса ничего не вырезается,
                # т.к. наматчился только параметр (не весь запрос)
                ('', 'зелёная крутящаяся шняга'),
            ],
            hid=33,
        )

    @classmethod
    def prepare_exp_parameters(cls):
        cls.index.hypertree += [HyperCategory(hid=34)]

        cls.index.gltypes += [
            GLType(param_id=62, hid=34, gltype=GLType.ENUM, values=[GLValue(value_id=1), GLValue(value_id=2)]),
            GLType(param_id=63, hid=34, gltype=GLType.ENUM, values=[GLValue(value_id=1), GLValue(value_id=2)]),
            GLType(param_id=64, hid=34, gltype=GLType.ENUM, values=[GLValue(value_id=1), GLValue(value_id=2)]),
        ]

        cls.index.offers += [
            Offer(
                title='беспроводные наушники bluetooth',
                hid=34,
                glparams=[
                    GLParam(param_id=62, value=1),
                    GLParam(param_id=63, value=1),
                    GLParam(param_id=64, value=1),
                ],
            )
        ]

        # параметр НЕ добавляется при отсутствии в запросе сингала об использовании тестовых параметров (пустой exp_tags)
        # по умолчанию в exp_tags лежит 'test'
        cls.formalizer.on_request(hid=34, query='беспроводные наушники bluetooth', exp_tags=[]).respond(
            formalized_params=[FormalizedParam(param_id=62, value=1, is_numeric=False, param_positions=(0, 12))]
        )

        cls.formalizer.on_request(hid=34, query='беспроводные наушники bluetooth').respond(
            formalized_params=[
                FormalizedParam(param_id=62, value=1, is_numeric=False, param_positions=(0, 12)),
                FormalizedParam(param_id=63, value=1, is_numeric=False, param_positions=(22, 31)),
            ]
        )

        cls.formalizer.on_request(hid=34, query='беспроводные наушники bluetooth', exp_tags=['test1']).respond(
            formalized_params=[
                FormalizedParam(param_id=64, value=1, is_numeric=False, param_positions=(13, 21)),
            ]
        )

        cls.formalizer.on_request(hid=34, query='беспроводные наушники bluetooth', exp_tags=['test2', 'test3']).respond(
            formalized_params=[
                FormalizedParam(param_id=62, value=1, is_numeric=False, param_positions=(0, 12)),
                FormalizedParam(param_id=63, value=1, is_numeric=False, param_positions=(22, 31)),
                FormalizedParam(param_id=64, value=1, is_numeric=False, param_positions=(13, 21)),
            ]
        )

    def test_exp_parameters(self):
        """
        Проверяем, что под обратным флагом репорт НЕ передаёт в формализатор exp_tag,
        по которому настоящий формализатор отдаёт новые тестовые параметры
        """

        text = 'беспроводные наушники bluetooth'
        hid = '34'
        request = 'place=prime&text={}&cvredirect=1'.format(text)
        empty_params = '&rearr-factors=market_exp_params_in_parametric_search='
        single_params = '&rearr-factors=market_exp_params_in_parametric_search=test1'
        multiple_params = '&rearr-factors=market_exp_params_in_parametric_search=test2,test3'

        requests_and_glfilters = [
            (request + empty_params, ["62:1"]),
            (request + single_params, ["64:1"]),
            (request + multiple_params, ["62:1", "63:1", "64:1"]),
            (request, ["62:1", "63:1"]),
        ]

        for request, glfilters in requests_and_glfilters:
            response = self.report.request_json(request)

            self.assertFragmentIn(
                response,
                {"redirect": {"target": "search", "params": {"rs": [NotEmpty()], "hid": [hid], "glfilter": glfilters}}},
                allow_different_len=False,
            )

    @classmethod
    def prepare_not_cut_query_in_parametric_search_with_short_result_request(cls):
        # данные для параметрического редиректа
        cls.index.hypertree += [HyperCategory(hid=162921)]

        cls.index.gltypes += [GLType(param_id=162922, hid=162921, gltype=GLType.ENUM, values=[GLValue(value_id=1)])]

        cls.index.offers += [
            Offer(title='oneplus 5', hid=162921, glparams=[GLParam(param_id=162922, value=1)]),
        ]

        cls.formalizer.on_request(hid=162921, query='oneplus 5').respond(
            formalized_params=[FormalizedParam(param_id=162922, value=1, is_numeric=False, param_positions=(0, 7))]
        )

        # данные для параметрического редиректа, в котором вырезаются все слова запроса
        cls.index.gltypes += [
            GLType(param_id=172922, hid=162921, gltype=GLType.ENUM, values=[GLValue(value_id=1)]),
            GLType(param_id=172923, hid=162921, gltype=GLType.ENUM, values=[GLValue(value_id=1)]),
        ]

        cls.index.offers += [
            Offer(
                title='surplus 5a',
                hid=162921,
                glparams=[GLParam(param_id=172922, value=1), GLParam(param_id=172923, value=1)],
            ),
        ]

        cls.formalizer.on_request(hid=162921, query='surplus 5a').respond(
            formalized_params=[
                FormalizedParam(param_id=172922, value=1, is_numeric=False, param_positions=(0, 7)),
                FormalizedParam(param_id=172923, value=1, is_numeric=False, param_positions=(8, 10)),
            ]
        )

        cls.reqwizard.on_request('shini+honda+x').respond(
            tires_mark='Honda',
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=5),
                ReqwExtMarkupTokenChar(begin_char=6, end_char=11),
                ReqwExtMarkupTokenChar(begin_char=12, end_char=13),
            ],
            found_tires_keyword_positions=[ReqwExtMarkupToken(begin=0, end=1, data=ReqwExtMarkupMarketTiresKeyword())],
            found_tires_mark_positions=[
                ReqwExtMarkupToken(begin=1, end=2, data=ReqwExtMarkupMarketTiresMark(mark_name='Honda'))
            ],
        )

        cls.index.offers += [
            Offer(title='shini honda x', hid=Const.TIRES_HID),
        ]

    def test_parametric_search_with_short_result_request(self):
        """
        Проверяем параметрический, когда запрос становится слишком коротким после формализации
        https://st.yandex-team.ru/MARKETOUT-16292
        https://st.yandex-team.ru/MARKETOUT-16556
        """
        # 1. Параметрический редирект
        # 1.1. Неформализванная часть [5],
        text = 'oneplus 5'
        response = self.report.request_json('place=prime&text={}&cvredirect=1&debug=da'.format(text))
        # Проверяем, что запрос не урезается в редиректе
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": [text],
                    }
                }
            },
        )
        # Проверяем, что мы ищем по полному запросу
        self.assertFragmentIn(response, self.get_base_search_text_debug_output(text))

        # Проверяем, что запрос не урезается при указании rs из редиректа
        rs = response.root['redirect']['params']['rs'][0]
        response = self.report.request_json(
            'place=prime&text={}&rs={}&hid=162921&glfilters=162922:1&debug=da'.format(text, rs)
        )
        self.assertFragmentIn(response, self.get_base_search_text_debug_output(text))

        # 1.2. Проверяем, что даже с флагом урезания слишком короткий запрос не ведёт к урезанию запроса
        rearr = '&rearr-factors=market_enable_parametric_cut_text=1'
        response = self.report.request_json('place=prime&text={}&cvredirect=1&debug=da'.format(text) + rearr)
        # Проверяем, что запрос не урезается в редиректе
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": [text],
                    }
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]
        response = self.report.request_json(
            'place=prime&text={}&rs={}&hid=162921&glfilters=162922:1&debug=da'.format(text, rs) + rearr
        )

        # Запрос остаётся неизменным
        debug_cuttext_msg = "Reduced text to cutText without params: [{}] -> [{}]"
        self.assertFragmentIn(response, debug_cuttext_msg.format(text, text))

        # 2. Проверяем paramertic-kind редиректы (в данном случае шинный)
        # 2.1. Без флага
        text = 'shini honda x'
        response = self.report.request_json('place=prime&text={}&cvredirect=1&debug=da'.format(text))
        # Проверяем, что запрос не урезается в редиректе
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": [text],
                    }
                }
            },
        )
        # Проверяем, что мы ищем по полному запросу
        self.assertFragmentIn(response, self.get_base_search_text_debug_output(text))

        # Проверяем, что запрос не урезается при указании rs из редиректа
        rs = response.root['redirect']['params']['rs'][0]
        response = self.report.request_json(
            'place=prime&text={}&rs={}&hid={}&debug=da'.format(text, rs, Const.TIRES_HID)
        )
        self.assertFragmentIn(response, self.get_base_search_text_debug_output(text))

        # 2.2. С флагом
        rearr = '&rearr-factors=market_enable_parametric_cut_text=1'
        response = self.report.request_json('place=prime&text={}&cvredirect=1&debug=da'.format(text) + rearr)
        # Проверяем, что запрос урезается в редиректе
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "suggest_text": [text],
                        "text": Absent(),
                    }
                }
            },
        )

        # Проверяем, что запрос урезается при указании rs из редиректа
        rs = response.root['redirect']['params']['rs'][0]
        response = self.report.request_json('place=prime&rs={}&hid={}&debug=da'.format(rs, Const.TIRES_HID) + rearr)
        self.assertFragmentIn(response, debug_cuttext_msg.format(text, ''))

    @classmethod
    def prepare_enable_colors(cls):
        cls.index.hypertree += [
            HyperCategory(hid=35),
        ]

        cls.index.gltypes += [
            GLType(param_id=65, hid=35, gltype=GLType.ENUM, values=[GLValue(value_id=1, text='чёрный')]),
            GLType(param_id=66, hid=35, gltype=GLType.ENUM, values=[GLValue(value_id=1, text='красивый')]),
        ]

        cls.index.models += [
            Model(hyperid=7, hid=35),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=7,
                title='чёрный красивый айфон',
                sku=1,
                blue_offers=[BlueOffer()],
                glparams=[GLParam(param_id=65, value=1), GLParam(param_id=66, value=1)],
            )
        ]

        cls.index.offers += [
            Offer(
                title='чёрный красивый айфон',
                hid=35,
                glparams=[GLParam(param_id=65, value=1), GLParam(param_id=66, value=1)],
            ),
        ]

        # Учим формализатор отвечать на запрос-категорию двумя параметрами:
        # оба матчатся правилом, у одного xsl_name 'color_glob' -- маркетный цвет --
        # этот параметр должен поматчиться даже с правилом, у другого -- любая другая
        # строка, он, соответственно, не должен
        for glued in (True, False):
            cls.formalizer.on_request(hid=35, query='чёрный красивый айфон', return_glued_params=glued).respond(
                formalized_params=[
                    FormalizedParam(
                        param_id=65, value=1, is_numeric=False, rule_id=100500, param_xsl_name='color_glob'
                    ),
                    FormalizedParam(
                        param_id=66,
                        value=1,
                        is_numeric=False,
                        param_positions=(7, 15),
                        rule_id=100501,
                        param_xsl_name='some_other_string',
                    ),
                    # парный consequent-параметр для param_id=65, из которого будут браться позиции
                    FormalizedParam(
                        param_id=67, value=234, is_numeric=False, param_positions=(0, 6), param_xsl_name='color_vendor'
                    ),
                ],
                consequent_params=[
                    ConsequentParam(
                        main_param_id=67,
                        main_values=[
                            MainParamValue(
                                main_param_value_id=234,
                                dependent_param_values=[DependentParamValue(param_id=65, value_ids=[1])],
                            )
                        ],
                    )
                ],
            )

    def test_enable_colors(self):
        """Учёт маркетных (xsl_name=color_glob) цветов в параметрическом.
        включено только в категориях красоты и фэшн
        """
        request = 'place=prime&text=чёрный+красивый+айфон&cvredirect=1&debug=da'

        # Без флага (market_enable_parametric_search_for_colors=0 по умолчанию) не поматчатся оба параметра, т.к. у обоих rule_id ненулевой
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {"params": {"glfilter": NotEmpty()}})

    @classmethod
    def prepare_enable_colors_in_beauty(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=90509,
                uniq_name="Товары для красоты",
                output_type=HyperCategoryType.GURU,
                children=[HyperCategory(hid=905091, uniq_name="Лак для ногтей", output_type=HyperCategoryType.GURU)],
            )
        ]

        cls.index.gltypes += [
            GLType(
                param_id=65,
                hid=905091,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='красный'), GLValue(value_id=2, text='синий')],
            ),
            GLType(param_id=66, hid=905091, gltype=GLType.ENUM, values=[GLValue(value_id=1, text='прекрасный')]),
        ]

        cls.index.offers += [
            Offer(
                title='прекрасный красный лак',
                hid=905091,
                glparams=[
                    GLParam(param_id=65, value=1),
                    GLParam(param_id=66, value=1),
                ],
            ),
            Offer(
                title='синий лак',
                hid=905091,
                glparams=[
                    GLParam(param_id=65, value=2),
                ],
            ),
        ]

        for glued in (True, False):
            cls.formalizer.on_request(hid=905091, query='прекрасный красный лак', return_glued_params=glued).respond(
                formalized_params=[
                    FormalizedParam(
                        param_id=65, value=1, is_numeric=False, rule_id=100500, param_xsl_name='color_glob'
                    ),
                    FormalizedParam(
                        param_id=66, value=1, is_numeric=False, param_positions=(0, 10), param_xsl_name='quality'
                    ),
                    # парный consequent-параметр для param_id=65, из которого будут браться позиции
                    FormalizedParam(
                        param_id=67, value=234, is_numeric=False, param_positions=(11, 7), param_xsl_name='color_vendor'
                    ),
                ],
                consequent_params=[
                    ConsequentParam(
                        main_param_id=67,
                        main_values=[
                            MainParamValue(
                                main_param_value_id=234,
                                dependent_param_values=[DependentParamValue(param_id=65, value_ids=[1])],
                            )
                        ],
                    )
                ],
            )

    def test_enable_colors_in_beauty(self):
        """В категории Товары для красоты (hid=90509) матчится цвет"""

        # TODO: MSSUP-763 - переход на новые факторы приводит к другому поведению редиректов, поэтому оставляем старое поведение
        rearr = '&rearr-factors=market_skip_broken_category_factors=0;market_categ_dssm_factor_fast_calc=0'
        request = 'place=prime&text=прекрасный+красный+лак&cvredirect=1&debug=da' + rearr

        # матчится цвет (красный) и качество (прекрасный)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {"params": {"glfilter": ["65:1", "66:1"], "hid": ["905091"]}},
            allow_different_len=False,
            preserve_order=False,
        )

        # Если категория не является дочерней категорией для Товары для красоты
        # то в ней параметр цвет не матчится
        request = 'place=prime&text=чёрный+красивый+айфон&cvredirect=1&debug=da' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"params": {"glfilter": NoKey("glfilter"), "hid": ["35"]}})

    @classmethod
    def prepare_enable_colors_in_fashion(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=7877999,
                uniq_name="Одежда, обувь и аксессуары",
                output_type=HyperCategoryType.GURU,
                children=[HyperCategory(hid=7811877, uniq_name="Мужская одежда", output_type=HyperCategoryType.GURU)],
            )
        ]

        cls.index.gltypes += [
            GLType(
                param_id=65,
                hid=7811877,
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='красный'), GLValue(value_id=2, text='синий')],
            ),
            GLType(param_id=66, hid=7811877, gltype=GLType.ENUM, values=[GLValue(value_id=1, text='свитер')]),
        ]

        cls.index.offers += [
            Offer(
                title='свитер красный мужской',
                hid=7811877,
                glparams=[
                    GLParam(param_id=65, value=1),
                    GLParam(param_id=66, value=1),
                ],
            ),
            Offer(
                title='кардиган синий мужской',
                hid=7811877,
                glparams=[
                    GLParam(param_id=65, value=2),
                ],
            ),
        ]

        for glued in (True, False):
            cls.formalizer.on_request(hid=7811877, query='свитер красный мужской', return_glued_params=glued).respond(
                formalized_params=[
                    FormalizedParam(
                        param_id=65, value=1, is_numeric=False, rule_id=100500, param_xsl_name='color_glob'
                    ),
                    FormalizedParam(
                        param_id=66, value=1, is_numeric=False, param_positions=(0, 6), param_xsl_name='type'
                    ),
                    # парный consequent-параметр для param_id=65, из которого будут браться позиции
                    FormalizedParam(
                        param_id=67, value=234, is_numeric=False, param_positions=(7, 7), param_xsl_name='color_vendor'
                    ),
                ],
                consequent_params=[
                    ConsequentParam(
                        main_param_id=67,
                        main_values=[
                            MainParamValue(
                                main_param_value_id=234,
                                dependent_param_values=[DependentParamValue(param_id=65, value_ids=[1])],
                            )
                        ],
                    )
                ],
            )

    def test_enable_colors_in_fashion(self):
        """В категории Одежда, обувь, аксессуары (hid=7877999) матчится цвет"""

        # TODO: MSSUP-763 - переход на новые факторы приводит к другому поведению редиректов, поэтому оставляем старое поведение
        formula_rearr = '&rearr-factors=market_skip_broken_category_factors=0;market_categ_dssm_factor_fast_calc=0'
        request = 'place=prime&text=свитер+красный+мужской&cvredirect=1&debug=da' + formula_rearr

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {"params": {"glfilter": ["65:1", "66:1"], "hid": ["7811877"]}},
            allow_different_len=False,
        )

        # Если категория не является дочерней категорией для Одежда, обувь, аксессуары,
        # то в ней параметр цвет не матчится
        request = 'place=prime&text=чёрный+красивый+айфон&cvredirect=1&debug=da' + formula_rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"params": {"glfilter": NoKey("glfilter"), "hid": ["35"]}})

    def test_disable_parametric_search(self):
        """Проверяем, что параметрический поиск
        отключается флагом market_disable_parametric_search.
        https://st.yandex-team.ru/MARKETOUT-17105
        """
        rearr = '&rearr-factors=market_disable_parametric_search=1'
        # параметрический поиск
        query = 'place=prime&text=blue+jacket+size+60&cvredirect=1'
        response = self.report.request_json(query)
        self.assertFragmentIn(response, {"redirect": {"params": {"glfilter": NotEmpty()}}})
        response = self.report.request_json(query + rearr)
        self.assertFragmentIn(response, {"redirect": {"params": {"glfilter": NoKey("glfilter")}}})

        # параметрическое уточнение
        text = "reusable black pencil"
        hid = 22
        query = "place=prime&text={}&hid={}&cvredirect=3".format(text, hid)
        self.check_parametric_specification_works(query)
        self.check_parametric_specification_doesnt_work(query + rearr)

    def test_disable_parametric_search_for_white(self):
        """
        https://st.yandex-team.ru/MARKETOUT-25652
        https://st.yandex-team.ru/MARKETOUT-27628
        https://st.yandex-team.ru/MARKETOUT-29254
        """

        query = 'place=prime&text=blue+jacket+size+60&cvredirect=1'

        # По умолчанию на белом параметрический поиск включён
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {"redirect": {"params": {"glfilter": NotEmpty()}}},
        )

        # market_disable_parametric_search=1 должен отключать параметрический поиск по гуру-лайт фильтрам
        # фильтр по магазину продолжает матчиться
        response = self.report.request_json(query + '&rearr-factors=market_disable_parametric_search=1')
        self.assertFragmentIn(
            response,
            {"redirect": {"params": {"glfilter": Absent(), "fesh": NotEmpty()}}},
        )

        # флаг market_disable_parametric_search=1 отключает параметрический поиск
        query = 'place=prime&text=reusable black pencil&hid=22&cvredirect=3'
        self.check_parametric_specification_doesnt_work(query + '&rearr-factors=market_disable_parametric_search=1')

    @classmethod
    def prepare_cancel_parametric_search_with_empty_text(cls):
        cls.index.hypertree += [HyperCategory(hid=36, output_type=HyperCategoryType.GURU)]

        cls.index.gltypes += [GLType(param_id=68, hid=36, gltype=GLType.ENUM, values=[1, 2])]

        cls.formalizer.on_request(hid=36, query='волынки').respond(
            formalized_params=[
                FormalizedParam(param_id=68, value=1, value_positions=(0, 7)),
            ]
        )

        cls.index.offers += [Offer(title="волынки", hid=36)]

    def test_cancel_parametric_search_with_empty_text(self):
        """
        Првоеряем, что в ситуации, когда параметрический урезал бы текст полностью, но привёл бы
        при этом к пустому СЕРПу, случается редирект без параметрического с исходным текстом
        """
        text = "волынки"
        query = 'place=prime&text={}&cvredirect=1&debug=da'.format(text)
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response, {"redirect": {"params": {"hid": NotEmpty(), "glfilter": Absent(), "text": [text]}}}
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains(Const.DEBUG_CANCELED_PARAM_MSG)]})

    @classmethod
    def prepare_cancel_parametric_search_with_negative_fesh(cls):
        cls.index.gltypes += [GLType(param_id=80, hid=50, gltype=GLType.ENUM, values=[1, 2])]

        cls.formalizer.on_request(hid=50, query='подстаканник').respond(
            formalized_params=[
                FormalizedParam(param_id=80, value=1, value_positions=(0, 12)),
            ]
        )

        # Ни один из офферов не найдётся параметрическим, т.к. ни у одного нет glparams
        # Параметрический на пустом СЕРПе отменится
        cls.index.offers += [
            Offer(fesh=118, title="подстаканник 1", hid=50),
            Offer(fesh=119, title="подстаканник 2", hid=50),
            Offer(fesh=120, title="подстаканник 2", hid=50),
        ]

    def test_cancel_parametric_search_with_negative_fesh(self):
        """
        Проверяем, что при отмена параметрического нормально взаимодействует
        с отрицательным fesh
        замечание:
        fesh=119&fesh=120 - выдаст 119 ИЛИ 120 магазин
        fesh=-119&fesh=-120 - выдаст НЕ (119 ИЛИ 120) магазин
        fesh=-119&fesh=120 выдаст только 120 магазин
        """

        for feshes_query in ['&fesh=-118&fesh=120', '&fesh=-118&fesh=-119']:
            text = "подстаканник"
            query = 'place=prime&text={}&cvredirect=1&{}&debug=da'.format(text, feshes_query)
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response, {"redirect": {"params": {"hid": NotEmpty(), "glfilter": Absent(), "text": [text]}}}
            )
            self.assertFragmentIn(response, {"logicTrace": [Contains(Const.DEBUG_CANCELED_PARAM_MSG)]})

            hid = response.root['redirect']['params']['hid'][0]

            response = self.report.request_json('place=prime&hid={}&debug=da&was_redir=1&{}'.format(hid, feshes_query))

            # В выдаче только 120 магазин
            self.assertFragmentIn(
                response,
                {"search": {"total": 1, "results": [{"entity": "offer", "shop": {"id": 120}}]}},
                allow_different_len=False,
                preserve_order=False,
            )

    @classmethod
    def prepare_together(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=198119,
                children=[
                    HyperCategory(hid=51),
                    HyperCategory(hid=52),
                ],
            )
        ]

        cls.index.regiontree += [Region(rid=213, name='Москва')]

        cls.index.credit_templates += [
            CreditTemplate(template_id=1, bank="Sber", url="sber.ru", term=24, rate=5.0),
        ]

        cls.index.shops += [
            Shop(fesh=123456, name="credit_shop_1", regions=[213, 75, 11], priority_region=213),
        ]

        cls.index.gltypes += [
            GLType(param_id=70, hid=51, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=71, hid=Const.TIRES_HID, gltype=GLType.ENUM, values=[1, 2]),
        ]

        for query in ['мандарины бу', 'апельсины в кредит бу', 'клементины в кредит бу']:
            cls.formalizer.on_request(hid=51, query=query).respond(
                formalized_params=[
                    FormalizedParam(param_id=70, value=1, value_positions=(0, 9)),
                ]
            )

        for query in ['мандарины бу', 'клементины в кредит бу', 'кумкваты в кредит бу', 'лаааааймы в кредит бу']:
            cls.reqwizard.on_request(query).respond(
                token_chars=[ReqwExtMarkupTokenChar(begin_char=0, end_char=9)],
                found_shop_positions=[
                    ReqwExtMarkupToken(
                        begin=0,
                        end=1,
                        data=ReqwExtMarkupMarketShop(shop_id=123456, alias_type='NAME', is_good_for_matching=True),
                    ),
                ],
            )

        for title in ['мандарины бу', 'апельсины бу', 'клементины бу', 'кумкваты бу']:
            for is_cutprice in (True, False):
                cls.index.offers += [
                    Offer(
                        title=title,
                        hid=51,
                        fesh=123456,
                        price=3000,
                        credit_template_id=1,
                        is_cutprice=is_cutprice,
                        glparams=[
                            GLParam(param_id=70, value=1),
                        ],
                    )
                ]

        for title in ['померанцы в кредит бу', 'лаааааймы в кредит бу']:
            for is_cutprice in (True, False):
                cls.index.offers += [
                    Offer(title=title, hid=51, fesh=123456, price=3000, credit_template_id=1, is_cutprice=is_cutprice),
                    Offer(title=title, hid=52, fesh=123456, price=3000, is_cutprice=is_cutprice),
                ]

        cls.formalizer.on_request(hid=Const.TIRES_HID, query='шины в кредит бу').respond(
            formalized_params=[
                FormalizedParam(param_id=71, value=1, value_positions=(0, 5)),
            ]
        )

        cls.reqwizard.on_request('шины в кредит бу').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=5, end_char=6),
                ReqwExtMarkupTokenChar(begin_char=7, end_char=13),
            ],
            found_shop_positions=[
                ReqwExtMarkupToken(
                    begin=0,
                    end=1,
                    data=ReqwExtMarkupMarketShop(shop_id=123456, alias_type='NAME', is_good_for_matching=True),
                ),
            ],
            tires_mark='Skoda',
            tires_model='Octavia',
            found_tires_keyword_positions=[ReqwExtMarkupToken(begin=0, end=1, data=ReqwExtMarkupMarketTiresKeyword())],
            found_tires_mark_positions=[
                ReqwExtMarkupToken(begin=1, end=2, data=ReqwExtMarkupMarketTiresMark(mark_name='Skoda'))
            ],
        )

        cls.index.offers += [
            Offer(
                title='шины бу',
                hid=Const.TIRES_HID,
                fesh=123456,
                price=3000,
                credit_template_id=1,
                is_cutprice=True,
                glparams=[
                    GLParam(param_id=70, value=1),
                ],
            )
        ]

    def check_has_entities(self, response, hid, glfilters, shops, credits, tires, cut_price):
        self.assertFragmentIn(response, {"redirect": NotEmpty()})

        hid_output = {"params": {"hid": NotEmpty()}}
        glfilters_output = {"params": {"glfilter": NotEmpty()}}
        shops_output = {"params": {"fesh": NotEmpty()}}
        credits_output = {"params": {"credit-type": ["credit"]}}
        tires_output = {"params": {"hid": [str(Const.TIRES_HID)], "mark": NotEmpty()}}
        cut_price_output = {"params": {"good-state": ["cutprice"]}}

        check = (
            lambda present, output: self.assertFragmentIn(response, output)
            if present
            else self.assertFragmentNotIn(response, output)
        )

        check(hid, hid_output)
        check(glfilters, glfilters_output)
        check(shops, shops_output)
        check(credits, credits_output)
        check(tires, tires_output)
        check(cut_price, cut_price_output)

    @skip('white credits will be deleted soon')
    def test_together(self):
        """
        Фиксируем совместную работу разных типов параметрического поиска
        """

        common_request = (
            'place=prime&cvredirect=1&rearr-factors=market_return_credits=1;'
            'market_calculate_credits=1&debug=da&rids=213'
        )

        # 1. Если в запросе одновременно матчатся категория, g(l)-фильтры, магазин и уценённые,
        # все попадают в редирект, редирект категорийный
        self.check_has_entities(
            self.report.request_json(common_request + "&text=мандарины+бу"),
            hid=True,
            glfilters=True,
            shops=True,
            credits=False,
            tires=False,
            cut_price=True,
        )

        # 2. Если одновременно матчатся категория, g(l)-фильтры, кредит и уценённые,
        # все попадают в редирект, редирект категорийный
        self.check_has_entities(
            self.report.request_json(common_request + "&text=апельсины+в+кредит+бу"),
            hid=True,
            glfilters=True,
            shops=False,
            credits=True,
            tires=False,
            cut_price=True,
        )

        # 3. Категория, g(l)-фильтры, магазин, кредит и уценённые могут сосуществовать в одном редиректе
        self.check_has_entities(
            self.report.request_json(common_request + "&text=клементины+в+кредит+бу"),
            hid=True,
            glfilters=True,
            shops=True,
            credits=True,
            tires=False,
            cut_price=True,
        )

        # 4. Если одновременно матчатся категория, магазин, кредит и уценённые, всё попадёт в редирект, редирект категорийный
        self.check_has_entities(
            self.report.request_json(common_request + "&text=кумкваты+в+кредит+бу"),
            hid=True,
            glfilters=False,
            shops=True,
            credits=True,
            tires=False,
            cut_price=True,
        )

        suppress_cat = '&rearr-factors=market_category_redirect_treshold=3'
        # 5. Если не матчится категория, но матчатся магазин, кредит и уценённые, кредит не попадёт, будет магазинный редирект
        # с уценёнными
        # Чисто кредит:
        self.check_has_entities(
            self.report.request_json(common_request + "&text=померанцы+в+кредит+бу" + suppress_cat),
            hid=False,
            glfilters=False,
            shops=False,
            credits=True,
            tires=False,
            cut_price=True,
        )
        # Магазин + кредит + уценённые -> магазин + уценённые:
        self.check_has_entities(
            self.report.request_json(common_request + "&text=лаааааймы+в+кредит+бу" + suppress_cat),
            hid=False,
            glfilters=False,
            shops=True,
            credits=False,
            tires=False,
            cut_price=True,
        )

        # 6. Если одновременно матчатся фильтры, шины, магазин, кредит и уценённые,
        # всё, кроме фильтров, попадёт в редирект, редирект шинный
        self.check_has_entities(
            self.report.request_json(common_request + "&text=шины+в+кредит+бу"),
            hid=True,
            glfilters=False,
            shops=True,
            credits=True,
            tires=True,
            cut_price=True,
        )

    @skip('white credits will be deleted soon')
    def test_parametric_kind_specification(self):
        """
        Фиксируем поведение параметрического уточнения для нестандартных типов
        параметрического поиска: магазинов, кредитов, шин, уценённых
        """

        common_request_tmpl = (
            'cvredirect=3&&hid={}&place=prime&rids=213&rearr-factors=market_return_credits=1;'
            'market_calculate_credits=1&show-cutprice=1'
        )
        common_request = common_request_tmpl.format('51')

        # 1. При наличии gl-фильтров и магазинов/кредитов/уценённых
        # выдача фильтруется и по gl-фильтрам, и по магазинам/кредитам/уценённым
        # и все наматченные фильтры помечаются как "параметрические"
        response = self.report.request_json(common_request + '&text=клементины+в+кредит+бу')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "70", "isParametric": True, "values": [{"id": "1", "checked": True}]},
                    {"id": "fesh", "isParametric": True, "values": [{"id": "123456", "checked": True}]},
                    {"id": "credit-type", "isParametric": True, "values": [{"value": "credit", "checked": True}]},
                    {"id": "good-state", "isParametric": True, "values": [{"value": "cutprice", "checked": True}]},
                ]
            },
        )

        # 2. Спецификация также происходит при матчинге чисто магазинов/кредитов/уценённых
        response = self.report.request_json(common_request + '&text=кумкваты+в+кредит+бу')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "fesh", "isParametric": True, "values": [{"id": "123456", "checked": True}]},
                    {"id": "credit-type", "isParametric": True, "values": [{"value": "credit", "checked": True}]},
                    {"id": "good-state", "isParametric": True, "values": [{"value": "cutprice", "checked": True}]},
                ]
            },
        )

        tire_request = common_request_tmpl.format(str(Const.TIRES_HID))

        # 3. Для шин спецификация не работает
        response = self.report.request_json(tire_request + '&text=шины+в+кредит+бу')
        self.assertFragmentNotIn(response, {"filters": [{"id": "mark"}]})
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "70", "isParametric": Absent(), "values": [{"id": "1", "checked": Absent()}]}]},
        )

    @classmethod
    def prepare_dont_formalize_filters_not_published_formalization(cls):
        cls.index.hypertree += [HyperCategory(hid=37, output_type=HyperCategoryType.GURULIGHT)]

        cls.index.offers += [Offer(title="щотка", hid=37, glparams=[GLParam(param_id=73, value=1)])]
        cls.index.offers += [Offer(title="нелистовая кепка", hid=60, glparams=[GLParam(param_id=174, value=1)])]

        cls.index.gltypes += [
            GLType(param_id=72, hid=37, use_formalization=False, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=73, hid=37, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=174, hid=60, use_formalization=False, gltype=GLType.ENUM, values=[1, 2]),
        ]

        cls.formalizer.on_request(hid=37, query='щотка').respond(
            formalized_params=[
                FormalizedParam(param_id=72, value=1, is_numeric=False, param_positions=(0, 5)),
                FormalizedParam(param_id=73, value=1, is_numeric=False, param_positions=(0, 5)),
            ]
        )
        cls.formalizer.on_request(hid=60, query='нелистовая кепка').respond(
            formalized_params=[
                FormalizedParam(param_id=174, value=1, is_numeric=False, param_positions=(10, 15)),
            ]
        )

    def test_dont_formalize_filters_not_published_formalization(self):
        """
        Проверяем, что параметры с use_formalization=False не отдаются параметрическим поиском
        """
        response = self.report.request_json('place=prime&text=щотка&cvredirect=1')

        self.assertFragmentIn(
            response,
            {"redirect": {"target": "search", "params": {"rs": [NotEmpty()], "glfilter": ["73:1"]}}},
            allow_different_len=False,
        )

        # для нелистовой категории флаг use_formalization не учитывается
        response = self.report.request_json(
            'place=prime&text=нелистовая кепка&cvredirect=1&rearr-factors=market_formalizer_non_leafs_categories=1'
        )

        self.assertFragmentIn(
            response,
            {"redirect": {"target": "search", "params": {"rs": [NotEmpty()], "glfilter": ["174:1"]}}},
            allow_different_len=False,
        )

    @classmethod
    def prepare_onstock_in_empty_serp_check(cls):
        cls.index.hypertree += [HyperCategory(hid=38, output_type=HyperCategoryType.GURU)]

        cls.index.gltypes += [
            GLType(param_id=74, hid=38, gltype=GLType.ENUM, values=[1, 2]),
        ]

        cls.index.models += [
            Model(
                hyperid=2,
                title="матрас без оффера",
                hid=38,
                glparams=[
                    GLParam(param_id=74, value=1),
                ],
            ),
            Model(
                hyperid=3,
                title="матрас с оффером",
                hid=38,
                glparams=[
                    GLParam(param_id=74, value=2),
                ],
            ),
            Model(
                hyperid=4,
                title="матрас с оффером 2",
                hid=38,
                glparams=[
                    GLParam(param_id=74, value=2),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=3),
            Offer(hyperid=4),
        ]

        cls.formalizer.on_request(hid=38, query='матрас').respond(
            formalized_params=[
                FormalizedParam(param_id=74, value=1, is_numeric=False, param_positions=(0, 6)),
            ]
        )

        cls.formalizer.on_request(hid=38, query='матрасик с оффером').respond(
            formalized_params=[
                FormalizedParam(param_id=74, value=2, is_numeric=False, param_positions=(11, 18)),
            ]
        )

    def test_onstock_in_empty_serp_check(self):
        """
        Проверяем, что при проверке на пустой СЕРП исключаются модели не в продаже,
        если параметр onstock=0 не задан явно
        """

        for onstock in ('&onstock=1', '', '&onstock='):
            response = self.report.request_json('place=prime&text=матрас&cvredirect=1' + onstock)

            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "target": "search",
                        "params": {
                            "hid": ["38"],
                            "glfilter": Absent(),
                        },
                    }
                },
                allow_different_len=False,
            )

        response = self.report.request_json('place=prime&text=матрас&cvredirect=1&onstock=0')

        self.assertFragmentIn(
            response,
            {"redirect": {"target": "search", "params": {"hid": ["38"], "rs": [NotEmpty()], "glfilter": ["74:1"]}}},
            allow_different_len=False,
        )

    # @see https://st.yandex-team.ru/MARKETOUT-31624
    def test_onstock_with_parametric_specification(self):
        onstock_disabled = {
            "filters": [
                {
                    "id": "onstock",
                    "values": [{"value": "0", "checked": True}, {"value": "1", "checked": NoKey("checked")}],
                }
            ]
        }

        onstock_enabled = {
            "filters": [
                {
                    "id": "onstock",
                    "values": [{"value": "0", "checked": NoKey("checked")}, {"value": "1", "checked": True}],
                }
            ]
        }

        # фильтр выключен если onstock не задан или задан onstock=0
        # [оффер] параметрический не срабатывает, [матрасик с оффером] - срабатывает (в этом случае у нас раньше была проблема)
        for text in ['оффер', 'матрасик с оффером']:
            for onstock in ['&onstock=0', '&onstock=', '']:
                response = self.report.request_json('place=prime&text={}&hid=38&cvredirect=3'.format(text) + onstock)
                self.assertFragmentIn(response, onstock_disabled, preserve_order=False, allow_different_len=True)

        for text in ['оффер', 'матрасик с оффером']:
            for onstock in ['&onstock=0', '&onstock=', '']:
                response = self.report.request_json('place=prime&text={}&hid=38&cvredirect=3'.format(text) + onstock)
                self.assertFragmentIn(response, onstock_disabled, preserve_order=False, allow_different_len=True)

            # только явное включение onstock=1 дает включенный фильтр
            response = self.report.request_json('place=prime&text={}&hid=38&onstock=1&cvredirect=3'.format(text))
            self.assertFragmentIn(response, onstock_enabled, preserve_order=False, allow_different_len=True)

    @classmethod
    def prepare_dont_formalize_suspicious_under_flag(cls):
        cls.index.hypertree += [HyperCategory(hid=39, output_type=HyperCategoryType.GURULIGHT)]

        cls.index.offers += [
            Offer(title="трезубец", hid=39, glparams=[GLParam(param_id=75, value=1), GLParam(param_id=76, value=1)])
        ]

        cls.index.models += [
            Model(hyperid=9, hid=39),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=9,
                title='трезубец',
                sku=3,
                blue_offers=[BlueOffer()],
                glparams=[GLParam(param_id=75, value=1), GLParam(param_id=76, value=1)],
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=75, hid=39, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=76, hid=39, gltype=GLType.ENUM, values=[1, 2]),
        ]

        for glued in (True, False):
            cls.formalizer.on_request(hid=39, query='трезубец', return_glued_params=glued).respond(
                formalized_params=[
                    FormalizedParam(param_id=75, value=1, is_numeric=False, param_positions=(0, 5), suspicious=True),
                    FormalizedParam(param_id=76, value=1, is_numeric=False, param_positions=(0, 5)),
                ]
            )

    def test_dont_formalize_suspicious(self):
        """
        Проверяем, что параметры со свойством suspicious не отдаются
        """
        query = 'place=prime&text=трезубец&cvredirect=1'
        response = self.report.request_json(query)

        self.assertFragmentIn(
            response,
            {"redirect": {"target": "search", "params": {"rs": [NotEmpty()], "glfilter": ["76:1"]}}},
            allow_different_len=False,
        )

    @classmethod
    def prepare_dont_formalize_glued_under_flag(cls):
        cls.index.hypertree += [HyperCategory(hid=40, output_type=HyperCategoryType.GURULIGHT)]

        cls.index.offers += [
            Offer(title="манишка", hid=40, glparams=[GLParam(param_id=77, value=12), GLParam(param_id=78, value=13)])
        ]

        cls.index.models += [
            Model(hyperid=8, hid=40),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=8,
                title='манишка',
                sku=2,
                blue_offers=[BlueOffer()],
                glparams=[GLParam(param_id=77, value=12), GLParam(param_id=78, value=13)],
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=77, hid=40, gltype=GLType.NUMERIC),
            GLType(param_id=78, hid=40, gltype=GLType.NUMERIC),
        ]

        cls.formalizer.on_request(hid=40, query='манишка абв12гдеф 13см', return_glued_params=True).respond(
            formalized_params=[
                FormalizedParam(param_id=77, value=12, is_numeric=True, param_positions=(11, 14)),
                FormalizedParam(param_id=78, value=13, is_numeric=True, param_positions=(18, 22)),
            ]
        )

        cls.formalizer.on_request(hid=40, query='манишка абв12гдеф 13см', return_glued_params=False).respond(
            formalized_params=[
                FormalizedParam(param_id=78, value=13, is_numeric=True, param_positions=(18, 22)),
            ]
        )

    def test_dont_formalize_glued(self):
        """
        Проверяем, что glued-параметры не формализуются
        """

        query = 'place=prime&text=манишка+абв12гдеф+13см&cvredirect=1'
        response = self.report.request_json(query)

        self.assertFragmentIn(
            response,
            {"redirect": {"target": "search", "params": {"rs": [NotEmpty()], "glfilter": ["78:13,13"]}}},
            allow_different_len=False,
        )

    @classmethod
    def prepare_reviews_in_empty_serp_check(cls):
        cls.index.hypertree += [HyperCategory(hid=41, output_type=HyperCategoryType.GURU)]

        cls.index.gltypes += [
            GLType(param_id=79, hid=41, gltype=GLType.ENUM, values=[1, 2]),
        ]

        cls.index.models += [
            Model(hyperid=5, title="ползунок с отзывами, но без параметра", hid=41),
            Model(
                hyperid=6,
                title="ползунок без отзывов, но с параметрами",
                hid=41,
                glparams=[
                    GLParam(param_id=79, value=1),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=5),
            Offer(hyperid=6),
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(
                review_id=1,
                model_id=5,
                grade_value=4,
            )
        ]

        cls.index.model_grade_dispersion_data += [GradeDispersionItem(model_id=5)]

        for query in ('ползунок', 'ползунок отзывы'):
            cls.formalizer.on_request(hid=41, query=query).respond(
                formalized_params=[
                    FormalizedParam(param_id=79, value=1, is_numeric=False, param_positions=(0, 8)),
                ]
            )

    def test_reviews_in_empty_serp_check(self):
        """
        Проверяем, что при проверке на пустой СЕРП исключаются модели без отзывов,
        если может произойти редирект в хаб отзывов
        """

        request = 'place=prime&text=ползунок+отзывы&cvredirect=0&hid=41'
        reviews = '&show-reviews=1'
        filters = '&glfilter=79:1'

        # С отзывами И параметрами выдача пустая
        response = self.report.request_json(request + reviews + filters)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                    "results": EmptyList(),
                }
            },
        )

        # Если бы было что-то одно, выдача не была бы пустая
        for r in (request + filters, request + reviews):
            response = self.report.request_json(request + filters)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 1,
                        "results": NotEmptyList(),
                    }
                },
            )

        response = self.report.request_json('place=prime&text=ползунок+отзывы&cvredirect=1')

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "glfilter": Absent(),
                        "show-reviews": ["1"],
                        "rt": ["9"],
                    },
                }
            },
        )

        response = self.report.request_json('place=prime&text=ползунок&cvredirect=1')

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "glfilter": NotEmptyList(),
                        "show-reviews": Absent(),
                        "rt": ["11"],
                    },
                }
            },
        )

    @classmethod
    def prepare_cut_price(cls):
        cls.index.hypertree += [
            HyperCategory(hid=42, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=43),
            HyperCategory(hid=44),
            HyperCategory(hid=45),
        ]

        cls.index.offers += [
            Offer(title='стратосфера уценочка', hid=42, is_cutprice=True),
            Offer(title='стратосфера НЕ уценочка', hid=42, is_cutprice=False),
            Offer(title='ноосфера НЕ уценочка', is_cutprice=False, waremd5='8GaN0stIZ5AJ4Oe_0SK3qQ'),
            Offer(title='плазмосфера б.уп', hid=43, is_cutprice=True),
            Offer(title='плазмосфера б.уп', hid=44, is_cutprice=True),
            Offer(title='плазмосфера НЕ б.уп', hid=45, is_cutprice=False),
            Offer(title='плазмосфера НЕ б.уп', hid=46, is_cutprice=False),
            Offer(title='атмосфера НЕ б.уп', hid=47, is_cutprice=False),
        ]

    def test_cut_price(self):
        """
        Проверяем, что работает редирект по уценённым товарам
        """
        rev_flag = '&rearr-factors=market_enable_cut_price_redirects=0'

        redir = {
            "redirect": {
                "params": {
                    "good-state": ["cutprice"],
                }
            }
        }

        redir_with_hid = {
            "redirect": {
                "params": {
                    "good-state": ["cutprice"],
                    "hid": ["42"],
                }
            }
        }

        redir_no_hid = {
            "redirect": {
                "params": {
                    "good-state": ["cutprice"],
                    "hid": Absent(),
                }
            }
        }

        common = 'place=prime&cvredirect=1&show-cutprice=1'

        # с обратным флагом не работает
        response = self.report.request_json(common + '&text=стратосфера+уценочка' + rev_flag)
        self.assertFragmentNotIn(response, redir)

        # с cpa=real не работает
        response = self.report.request_json(common + '&text=стратосфера+уценочка&cpa=real&debug=da')
        self.assertFragmentNotIn(response, redir)
        self.assertFragmentIn(response, {'logicTrace': [Contains('Cut price parametric search is disabled')]})

        # с оффером работает
        response = self.report.request_json(common + '&text=стратосфера+уценочка')
        self.assertFragmentIn(response, redir_with_hid)

        # работает и на бескатегорийных редиректах
        suppress_cat = '&rearr-factors=market_category_redirect_treshold=3'
        response = self.report.request_json(common + '&text=плазмосфера+б.уп' + suppress_cat)
        self.assertFragmentIn(response, redir_no_hid)

        # не работает (отменяется) на потенциально пустых выдачах
        response = self.report.request_json(common + '&text=ноосфера+бушная')
        self.assertFragmentNotIn(response, redir)

        # отменяется на пустых выдачах, даже если есть nailed-офферы

        state = ReportState.create()
        nailed_doc = state.search_state.nailed_docs.add()
        nailed_doc.ware_id = '8GaN0stIZ5AJ4Oe_0SK3qQ'
        nailed_doc.shop_click_price = 10
        nailed_doc.vendor_click_price = 10
        nailed_doc.fee = 5
        rs = ReportState.serialize(state)

        response = self.report.request_json(common + '&text=ноосфера+бушная' + '&rs=' + rs)
        self.assertFragmentNotIn(response, redir)

    @classmethod
    def prepare_empty_serp_offer_no_model(cls):
        cls.index.hypertree += [
            HyperCategory(hid=48, output_type=HyperCategoryType.GURU, show_offers=False),
            HyperCategory(hid=49, output_type=HyperCategoryType.GURULIGHT),
        ]

        cls.index.gltypes += [
            GLType(hid=48, param_id=81, values=[1, 2]),
        ]

        cls.index.offers += [
            Offer(hid=48, title='зубная паста', glparams=[GLParam(param_id=81, value=1)]),
            Offer(hid=49, title='зубная паста', glparams=[GLParam(param_id=81, value=2)]),
        ]

        cls.formalizer.on_request(hid=48, query='зубная паста').respond(
            formalized_params=[
                FormalizedParam(param_id=81, value=1, is_numeric=False, param_positions=(0, 12)),
            ]
        )

    @classmethod
    def prepare_no_type_parameters_for_beauty(cls):
        cls.index.hypertree += [
            HyperCategory(hid=53),
        ]

        # 53 -- не подкатегория красоты
        # 905091 -- подкатегория красоты (см. prepare_enable_colors_in_beauty)

        cls.index.gltypes += [
            GLType(hid=53, param_id=82, values=[1, 2]),
            GLType(hid=905091, param_id=83, values=[1, 2]),
            GLType(hid=905091, param_id=84, values=[1, 2]),
        ]

        cls.index.offers += [
            Offer(hid=53, title='гуашь', glparams=[GLParam(param_id=82, value=1)]),
            Offer(hid=905091, title='тушь', glparams=[GLParam(param_id=83, value=1)]),
            Offer(hid=905091, title='помада наотмашь', glparams=[GLParam(param_id=84, value=1)]),
        ]

        cls.formalizer.on_request(hid=53, query='гуашь').respond(
            formalized_params=[
                FormalizedParam(param_id=82, value=1, is_numeric=False, param_positions=(0, 5), param_xsl_name='type'),
            ]
        )

        cls.formalizer.on_request(hid=905091, query='тушь').respond(
            formalized_params=[
                FormalizedParam(param_id=83, value=1, is_numeric=False, param_positions=(0, 4), param_xsl_name='type'),
            ]
        )

        cls.formalizer.on_request(hid=905091, query='помада наотмашь').respond(
            formalized_params=[
                FormalizedParam(param_id=84, value=1, is_numeric=False, param_positions=(7, 15), param_xsl_name='pyte'),
            ]
        )

    def test_no_type_parameters_for_beauty(self):
        """
        Проверяем, что в подкатегориях красоты не матчится параметр "Тип"
        """
        request = 'place=prime&cvredirect=1&text={}'

        # На НЕкрасоте тип матчится

        response = self.report.request_json(request.format('гуашь'))

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "glfilter": ["82:1"],
                        "rt": ["11"],
                    },
                }
            },
            allow_different_len=False,
        )

        # На красоте параметр "тип" перестаёт матчиться
        response = self.report.request_json(request.format('тушь'))

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "glfilter": Absent(),
                        "rt": ["9"],
                    },
                }
            },
            allow_different_len=False,
        )

        # На красоте параметры кроме "тип" матчатся
        response = self.report.request_json(request.format('помада наотмашь'))

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "glfilter": ["84:1"],
                        "rt": ["11"],
                    },
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_through_filters_redirect(cls):

        cls.index.hypertree += [
            HyperCategory(hid=738304, name="Средства против пандемии", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=383772, name="Биологическое оружие", output_type=HyperCategoryType.GURU),
        ]

        for hid in [90401, 738304, 383772]:
            cls.index.gltypes += [
                # вендор - глобальный пробрасываемый gl-параметр
                GLType(
                    hid=hid,
                    gltype=GLType.ENUM,
                    param_id=7893318,
                    values=[1, 2, 3],
                    name="Производитель",
                    vendor=True,
                    through=True,
                ),
                # тип - глобальный пробрасываемый gl-параметр
                GLType(hid=hid, gltype=GLType.ENUM, param_id=17354681, values=[1, 2, 3], name="Тип", through=True),
                # глобальный не пробрасываемый gl-параметр
                GLType(hid=hid, gltype=GLType.ENUM, param_id=45678098, values=[1, 2, 3], name="Эффективность"),
            ]
        for hid in [738304, 383772]:
            cls.index.gltypes += [
                # не глобальный пробрасываемый gl-параметр
                GLType(
                    hid=hid,
                    gltype=GLType.ENUM,
                    param_id=17354854,
                    values=[1, 2, 3],
                    name="Побочные эффекты",
                    through=True,
                ),
            ]

        cls.index.offers += [
            Offer(
                title="Марлевая повязка",
                hid=738304,
                descr="Производитель:Wuhan Тип:воздушно-капельный Эффективность:низкая Побочные эффекты:незначительные",
                glparams=[GLParam(7893318, 1), GLParam(17354681, 1), GLParam(45678098, 1), GLParam(17354854, 1)],
            ),
            Offer(
                title="Закрытие границ",
                hid=738304,
                descr="Производитель:USSR Тип:контактный Эффективность:средняя Побочные эффекты:заметные",
                glparams=[GLParam(7893318, 2), GLParam(17354681, 2), GLParam(45678098, 2), GLParam(17354854, 2)],
            ),
            Offer(
                title="Сжигай их всех",
                hid=738304,
                descr="Производитель:Средневековая европа Тип:бесконтактный Эффективность:высокая Побочные эффекты:критические",
                glparams=[GLParam(7893318, 3), GLParam(17354681, 3), GLParam(45678098, 3), GLParam(17354854, 3)],
            ),
            Offer(
                title="COVID-19",
                hid=383772,
                descr="Производитель:Wuhan Тип:воздушно-капельный Эффективность:низкая Побочные эффекты:незначительные",
                glparams=[GLParam(7893318, 1), GLParam(17354681, 1), GLParam(45678098, 1), GLParam(17354854, 1)],
            ),
            Offer(
                title="ВИЧ/СПИД",
                hid=383772,
                descr="Производитель:USSR Тип:контактный Эффективность:средняя Побочные эффекты:заметные",
                glparams=[GLParam(7893318, 2), GLParam(17354681, 2), GLParam(45678098, 2), GLParam(17354854, 2)],
            ),
            Offer(
                title="Чума",
                hid=383772,
                descr="Производитель:Средневековая европа Тип:бесконтактный Эффективность:высокая Побочные эффекты:критические",
                glparams=[GLParam(7893318, 3), GLParam(17354681, 3), GLParam(45678098, 3), GLParam(17354854, 3)],
            ),
        ]

        cls.formalizer.on_request(hid=90401, query='Производитель:USSR').respond(
            formalized_params=[FormalizedParam(param_id=7893318, value=2, is_numeric=False, param_positions=(0, 18))]
        )

        cls.formalizer.on_request(
            hid=90401, query='Производитель:Wuhan Тип:воздушно-капельный Эффективность:низкая минимум побочных эффектов'
        ).respond(
            formalized_params=[
                FormalizedParam(param_id=7893318, value=1, is_numeric=False, param_positions=(0, 19)),
                FormalizedParam(param_id=17354681, value=1, is_numeric=False, param_positions=(20, 42)),
                FormalizedParam(param_id=45678098, value=1, is_numeric=False, param_positions=(43, 63)),
                FormalizedParam(param_id=45678098, value=1, is_numeric=False, param_positions=(64, 89)),
            ]
        )

        # forbid category redirect
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 738304).respond(-4)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 383772).respond(-4)

    def test_vendor_parametric_search_without_category(self):
        """В запросе без категорийного редиректа может формализоваться параметр Производитель
        Даже при полном совпадении текста запроса с параметром - текст не вырезается
        """

        response = self.report.request_json(
            'place=prime&cvredirect=1&text=Производитель:Wuhan Тип:воздушно-капельный Эффективность:низкая минимум побочных эффектов'
            '&rearr-factors=market_through_gl_filters_on_search=1;market_through_gl_filters_redirect=1'
        )

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "rs": [NotEmpty()],
                        "rt": ["11"],
                        "text": [
                            "Производитель:Wuhan Тип:воздушно-капельный Эффективность:низкая минимум побочных эффектов"
                        ],
                        "glfilter": ["7893318:1"],
                        "hid": NoKey("hid"),
                        "nid": NoKey("nid"),
                    },
                    "target": "search",
                }
            },
            allow_different_len=False,
        )

        rs = response.root['redirect']['params']['rs'][0]
        response = self.report.request_json(
            'place=prime&text=Производитель:Wuhan Тип:воздушно-капельный Эффективность:низкая минимум побочных эффектов&debug=da&rs={}'.format(
                rs
            )
            + '&glfilter=7893318:1&was_redir=1&rearr-factors=market_through_gl_filters_on_search=1;market_through_gl_filters_redirect=1&enable-hard-filters=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'isParametricSearch': True,
                    'results': [
                        {'titles': {'raw': 'Марлевая повязка'}},
                        {'titles': {'raw': 'COVID-19'}},
                    ],
                },
                # только through фильтры
                'filters': [
                    {
                        'id': '7893318',
                        'isParametric': True,
                        'values': [{'id': "1", 'checked': True}, {"id": "2"}, {"id": "3"}],
                    },
                    {'id': '17354681', 'isParametric': NoKey('isParametric')},
                    {'id': '17354854', 'isParametric': NoKey('isParametric')},
                    {'id': 'glprice'},
                    {'id': 'at-beru-warehouse'},
                    {'id': 'with-yandex-delivery'},
                    {'id': 'manufacturer_warranty'},
                    {'id': 'onstock'},
                    {'id': 'qrfrom'},
                    {'id': 'offer-shipping'},
                    {'id': 'fesh'},
                ],
                'query': {
                    "highlighted": [
                        {"value": "Производитель:Wuhan", "highlight": True},
                        {
                            "value": " Тип:воздушно-капельный Эффективность:низкая минимум побочных эффектов",
                            "highlight": NoKey("highlight"),
                        },
                    ]
                },
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, self.get_base_search_text_debug_output("Производитель Wuhan"))

        # запрос Производитель:USSR полностью совпадает с распашненными параметрами
        # параметры подсвечиваются, но не вырезаются из запроса

        response = self.report.request_json(
            'place=prime&cvredirect=1&text=Производитель:USSR'
            '&rearr-factors=market_through_gl_filters_on_search=1;market_through_gl_filters_redirect=1'
        )

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "rs": [NotEmpty()],
                        "rt": ["11"],
                        "text": ["Производитель:USSR"],
                        "glfilter": ["7893318:2"],
                        "hid": NoKey("hid"),
                        "nid": NoKey("nid"),
                    },
                    "target": "search",
                }
            },
            allow_different_len=False,
        )

        rs = response.root['redirect']['params']['rs'][0]
        response = self.report.request_json(
            'place=prime&text=Производитель:USSR&debug=da&rs={}'.format(rs)
            + '&glfilter=7893318:2&was_redir=1&rearr-factors=market_through_gl_filters_on_search=1;market_through_gl_filters_redirect=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'isParametricSearch': True,
                    'results': [
                        {'titles': {'raw': 'Закрытие границ'}},
                        {'titles': {'raw': 'ВИЧ/СПИД'}},
                    ],
                },
                'query': {"highlighted": [{"value": "Производитель:USSR", "highlight": True}]},
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, self.get_base_search_text_debug_output("Производитель USSR"))

        # с флагом market_enable_parametric_cut_text_to_rs редирект на текст, а поиск бестекстовый при полном совпадении
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=Производитель:USSR'
            '&rearr-factors=market_through_gl_filters_on_search=1;market_through_gl_filters_redirect=1;market_enable_parametric_cut_text_to_rs=1'
        )

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "rs": [NotEmpty()],
                        "rt": ["11"],
                        "text": ["Производитель:USSR"],
                        "glfilter": ["7893318:2"],
                        "hid": NoKey("hid"),
                        "nid": NoKey("nid"),
                    },
                    "target": "search",
                }
            },
            allow_different_len=False,
        )

        rs = response.root['redirect']['params']['rs'][0]
        response = self.report.request_json(
            'place=prime&text=Производитель:USSR&debug=da&rs={}'.format(rs)
            + '&glfilter=7893318:2&was_redir=1&rearr-factors=market_through_gl_filters_on_search=1;market_through_gl_filters_redirect=1;market_enable_parametric_cut_text_to_rs=1'
        )
        debug_textless_msg = "we can search textless"
        self.assertFragmentIn(response, debug_textless_msg)

        # если нет одного из флагов
        # формализации вендора не случится
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=Производитель:Wuhan Тип:воздушно-капельный Эффективность:низкая минимум побочных эффектов'
            '&rearr-factors=market_through_gl_filters_on_search=0;market_through_gl_filters_redirect=1'
        )
        self.assertFragmentIn(response, {'search': {'results': NotEmpty()}})

        response = self.report.request_json(
            'place=prime&cvredirect=1&text=Производитель:Wuhan Тип:воздушно-капельный Эффективность:низкая минимум побочных эффектов'
            '&rearr-factors=market_through_gl_filters_on_search=1;market_through_gl_filters_redirect=0'
        )
        self.assertFragmentIn(response, {'search': {'results': NotEmpty()}})

    @classmethod
    def prepare_non_leaf_category(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=541,
                uniq_name="Модные горячие няпитки",
                children=[
                    HyperCategory(hid=5410),
                ],
            ),
            HyperCategory(
                hid=542,
                uniq_name="Холодные чаи",
                children=[
                    HyperCategory(hid=5420),
                ],
            ),
        ]
        cls.index.fashion_categories += [
            FashionCategory("CATEGORY_COMMON", 541),
            FashionCategory("CATEGORY_COMMON", 5410),
        ]

        cls.index.gltypes += [
            GLType(hid=541, param_id=104, values=[1, 2]),
            GLType(hid=5410, param_id=105, values=[1, 2]),
            GLType(hid=5410, param_id=106, values=[1, 2]),
            GLType(hid=542, param_id=107, values=[1, 2]),
            GLType(hid=5420, param_id=108, values=[1, 2]),
        ]

        cls.index.offers += [
            Offer(
                hid=5410,
                title='индийский чай',
                glparams=[
                    GLParam(param_id=104, value=1),
                    GLParam(param_id=105, value=1),
                    GLParam(param_id=106, value=2),
                ],
            ),
            Offer(
                hid=5420,
                title='татарский лимонад',
                glparams=[
                    GLParam(param_id=107, value=1),
                    GLParam(param_id=108, value=1),
                ],
            ),
        ]

        cls.formalizer.on_request(hid=541, query='чай').respond(
            formalized_params=[
                FormalizedParam(param_id=104, value=1, is_numeric=False, param_positions=(0, 3), param_xsl_name='type'),
            ]
        )
        cls.formalizer.on_request(hid=541, query='чай с лимоном').respond(
            formalized_params=[
                FormalizedParam(param_id=104, value=1, is_numeric=False, param_positions=(0, 3), param_xsl_name='type'),
                FormalizedParam(param_id=105, value=1, is_numeric=False, param_positions=(0, 3), param_xsl_name='type'),
            ]
        )
        cls.formalizer.on_request(hid=5410, query='чай').respond(
            formalized_params=[
                FormalizedParam(param_id=105, value=1, is_numeric=False, param_positions=(0, 3), param_xsl_name='type'),
            ]
        )
        cls.formalizer.on_request(hid=5410, query='горячий чай', use_new_handler=True).respond(
            formalized_params=[
                FormalizedParam(param_id=106, value=2, is_numeric=False, param_positions=(0, 3), param_xsl_name='type'),
            ],
        )
        cls.formalizer.on_request(hid=542, query='лимонад').respond(
            formalized_params=[
                FormalizedParam(param_id=107, value=2, is_numeric=False, param_positions=(0, 3), param_xsl_name='type'),
            ],
        )
        cls.formalizer.on_request(hid=5420, query='лимонад').respond(
            formalized_params=[
                FormalizedParam(param_id=108, value=2, is_numeric=False, param_positions=(0, 3), param_xsl_name='type'),
            ],
        )

        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 541).respond(0.8)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 5410).respond(-2)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 542).respond(0.8)
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 5420).respond(-2)

    def test_not_formalize_non_leaf_category(self):
        """В запросе с нелистовой категорией репорт не должен ходить в формализатор"""

        # нелистовая не формализуется
        response = self.report.request_json('place=prime&cvredirect=3&text=чай&hid=541&debug=da')
        self.assertFragmentNotIn(response, {"logicTrace": [Contains("Making POST-request to formalizer")]})

        # но с флагом ходит в формализатор (если это фешн категория)
        response = self.report.request_json(
            'place=prime&cvredirect=3&text=чай&hid=541&debug=da&rearr-factors=market_formalizer_non_leafs_categories=1'
        )
        self.assertFragmentIn(response, {"logicTrace": [Contains("Making POST-request to formalizer")]})

        # в листовой уже формализуется, если это фешн категория
        response = self.report.request_json('place=prime&cvredirect=3&text=чай&debug=da&hid=5410')
        self.assertFragmentIn(response, {"logicTrace": [Contains("Making POST-request to formalizer")]})

        # для нелистовой и не фешн с флагом не ходит в формализатор
        response = self.report.request_json(
            'place=prime&cvredirect=3&text=чай&debug=da&hid=542&rearr-factors=market_formalizer_non_leafs_categories=1'
        )
        self.assertFragmentNotIn(response, {"logicTrace": [Contains("Making POST-request to formalizer")]})

        # для листовой и не фешн не ходит в формализатор
        response = self.report.request_json(
            'place=prime&cvredirect=3&text=чай&debug=da&hid=542&rearr-factors=market_formalizer_non_leafs_categories=1'
        )
        self.assertFragmentNotIn(response, {"logicTrace": [Contains("Making POST-request to formalizer")]})

    def test_not_formalize_parametric_redirect_in_non_leaf(self):
        """при параметрическом редиректе в нелистовую категорию формализация должна быть только под флагом"""
        # с флагом в редиректе будут глфильтры
        response = self.report.request_json(
            'place=prime&cvredirect=1&text=чай с лимоном&debug=da&rearr-factors=market_formalizer_non_leafs_categories=1'
        )
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rs": [NotEmpty()],
                        "hid": ["541"],
                        "glfilter": ["104:1"],
                    },
                }
            },
        )

        # по умолчанию в нелистовых не ходим в формализатор, поэтому глфльтров не будет
        response = self.report.request_json('place=prime&cvredirect=1&text=чай с лимоном&debug=da')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "rs": [NotEmpty()],
                        "hid": ["541"],
                        "glfilter": NoKey("glfilter"),
                    },
                }
            },
        )

    def test_formalize_for_search_handles(self):
        """Проверяем что под флагом и для фешн ходим в новую ручку формализатора: /FormalizeForSearch"""
        response = self.report.request_json(
            'place=prime&cvredirect=3&hid=5410&text=горячий чай&rearr-factors=market_use_formalizer_for_search=1&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                "specification": {
                    "parametric": {
                        "params": {
                            "glfilter": ["106:2"],
                        },
                    }
                },
            },
        )
        self.assertFragmentIn(
            response, {"logicTrace": [Regex(r".*Get\(\): Trying to get http://[^\:]+:[0-9]+/FormalizeForSearch.*")]}
        )

        """ без флага в старую, параметр другой """
        response = self.report.request_json('place=prime&cvredirect=3&hid=5410&text=горячий чай&debug=da')
        self.assertFragmentIn(response, {"specification": NoKey("specification")})
        self.assertFragmentIn(
            response, {"logicTrace": [Regex(r".*Get\(\): Trying to get http://[^\:]+:[0-9]+/formalize.*")]}
        )

        """ с флагом, для не fashion ходим в старую, параметр другой """
        response = self.report.request_json(
            'place=prime&cvredirect=3&hid=5420&text=лимонад&debug=da&rearr-factors=market_use_formalizer_for_search=1'
        )
        self.assertFragmentIn(response, {"specification": NoKey("specification")})
        self.assertFragmentIn(
            response, {"logicTrace": [Regex(r".*Get\(\): Trying to get http://[^\:]+:[0-9]+/formalize.*")]}
        )

    @classmethod
    def prepare_resale(cls):
        cls.index.hypertree += [
            HyperCategory(hid=100),
        ]

        cls.index.offers += [
            Offer(
                title='подержанный автомобиль',
                hid=100,
                resale_condition=ResaleCondition.PERFECT,
                resale_reason=ResaleReason.USED,
                resale_description="perfect item",
            ),
        ]

    def test_resale(self):
        """
        Проверяем редирект б/у
        """

        redir = {
            "redirect": {
                "params": {
                    "resale_goods": ["resale_resale"],
                }
            }
        }

        no_redir = {
            "redirect": {
                "params": {
                    "resale_goods": Absent(),
                }
            }
        }

        common = 'place=prime&cvredirect=1&enable-resale-goods=1'

        response = self.report.request_json(common + '&text=подержанный+автомобиль')
        self.assertFragmentIn(response, redir)

        response = self.report.request_json(common + '&text=автомобиль')
        self.assertFragmentIn(response, no_redir)

    @classmethod
    def prepare_gender_expand(cls):

        cls.index.gltypes += [
            GLType(
                param_id=14805991,
                hid=11,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=14805992, text='male', filter_value=True),
                    GLValue(value_id=25295150, text='boys', filter_value=True),
                    GLValue(value_id=14805994, text='unisex', filter_value=True),
                    GLValue(value_id=28574128, text='boys,girls', filter_value=True),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="coat",
                hid=11,
                glparams=[
                    GLParam(param_id=14805991, value=14805992),
                    GLParam(param_id=14805991, value=25295150),
                ],
            ),
        ]

        cls.formalizer.on_request(hid=11, query="coat").respond(
            formalized_params=[
                FormalizedParam(param_id=14805991, value=14805992, is_numeric=False, param_xsl_name='pol'),
                FormalizedParam(param_id=14805991, value=25295150, is_numeric=False, param_xsl_name='pol'),
            ]
        )

    def test_gender_expand(self):
        """
        https://st.yandex-team.ru/MARKETOUT-47781
        Проверяем, что при параметризации параметра pol добавляем unisex(14805994) и boy-girls(28574128)
        """

        text = 'coat'
        response = self.report.request_json('place=prime&text={}&cvredirect=1'.format(text))

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "hid": ["11"],
                        "text": [text],
                        "glfilter": ["14805991:14805994,14805992,28574128,25295150"],
                    },
                }
            },
        )

        response = self.report.request_json(
            'place=prime&text={}&cvredirect=1&rearr-factors=market_expand_gender_parametrization=0'.format(text)
        )

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "hid": ["11"],
                        "text": [text],
                        "glfilter": ["14805991:14805992,25295150"],
                    },
                }
            },
        )

    def test_parametric_restrict_gl_filters(self):
        """
        https://st.yandex-team.ru/MARKETOUT-48053
        Не проставляем фильтры, в случае если не ведем на бестекст под флагом market_parametric_restrict_gl_filters
        """

        rearr = "&rearr-factors=market_enable_parametric_cut_text_to_rs=1"
        response = self.report.request_json('place=prime&text=torn levi blue jeans size 36&cvredirect=1' + rearr)

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "glfilter": NotEmpty(),
                    },
                }
            },
        )

        rearr = "&rearr-factors=market_enable_parametric_cut_text_to_rs=1;market_parametric_restrict_gl_filters=1"
        response = self.report.request_json('place=prime&text=torn levi blue jeans size 36&cvredirect=1' + rearr)

        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "glfilter": NoKey("glfilter"),
                    },
                }
            },
        )


if __name__ == '__main__':
    main()
