#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.blender_bundles import create_blender_bundles
from core.types import (
    BlueOffer,
    BoosterConfigRecord,
    Const,
    GLParam,
    GLType,
    MarketSku,
    MnPlace,
    Model,
    Picture,
    QueryIntList,
    Region,
    Shop,
)
from core.matcher import Absent, Round


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        bundle_const_search_position = '''
        {
            "incut_places": ["Search"],
            "incut_positions": [1],
            "incut_viewtypes": ["SimpleGallery"],
            "incut_ids": ["default"],
            "result_scores": [
                {
                    "incut_place": "Search",
                    "row_position": 1,
                    "incut_viewtype": "SimpleGallery",
                    "incut_id": "default",
                    "score": 1.0
                }
            ],
            "calculator_type": "ConstPosition"
        }
        '''

        bundles_config = '{}'
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            bundles_config,
            {
                'super_hype.json': bundle_const_search_position,
            },
        )

        cls.disable_randx_randomize()

        cls.reqwizard.on_default_request().respond()

    @classmethod
    def prepare_super_hype_goods(cls):
        GLType(param_id=Const.SUPER_HYPE_GL_PARAM_ID, hid=30, gltype=GLType.NUMERIC),
        GLType(param_id=Const.SUPER_HYPE_GL_PARAM_ID, hid=40, gltype=GLType.NUMERIC),

        cls.index.shops += [
            Shop(
                fesh=513,
                name='blue_shop_1',
                priority_region=213,
                supplier_type=Shop.FIRST_PARTY,
                datafeed_id=3,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                business_fesh=4,
            ),
        ]

        cls.index.models += [
            Model(hyperid=30004, hid=30),
        ]

        cls.index.mskus += [
            MarketSku(
                ts=30001,
                sku=30001,
                title='Super hype goods sku without offers',
                hid=30,
                picture=Picture(width=100, height=100, group_id=30001),
                glparams=[GLParam(param_id=Const.SUPER_HYPE_GL_PARAM_ID, value=1)],
            ),
            MarketSku(
                ts=30004,
                sku=30004,
                title='Super hype goods sku with model',
                hid=30,
                hyperid=30004,
                picture=Picture(width=100, height=100, group_id=30004),
                glparams=[GLParam(param_id=Const.SUPER_HYPE_GL_PARAM_ID, value=1)],
            ),
            MarketSku(
                sku=30002,
                title='Super hype goods sku',
                hid=30,
                picture=Picture(width=100, height=100, group_id=30002),
                glparams=[GLParam(param_id=Const.SUPER_HYPE_GL_PARAM_ID, value=1)],
                blue_offers=[
                    BlueOffer(ts=30002, title='Super hype goods offer'),
                ],
            ),
            MarketSku(
                sku=30003,
                title='Not super hype goods sku',
                hid=30,
                picture=Picture(width=100, height=100, group_id=30003),
                blue_offers=[
                    BlueOffer(ts=30003, title='Not super hype goods offer', hid=30),
                ],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30003).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30002).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30001).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30004).respond(0.5)

        for i in range(30200, 30220):
            hid = 50 if (i < 30202) else 40
            cls.index.models.append(Model(hyperid=i, hid=hid))
            cls.index.mskus.append(
                MarketSku(
                    sku=i,
                    title=('Super hype goods sku with model %d' % i),
                    hid=hid,
                    hyperid=i,
                    picture=Picture(width=100, height=100, group_id=i),
                    glparams=[GLParam(param_id=Const.SUPER_HYPE_GL_PARAM_ID, value=1)],
                )
            )
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, i).respond(0.5 - 0.001 * (i - 30200))

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                    Region(rid=2, name="Санкт-Петербург"),
                    Region(rid=193, name='Воронеж', preposition='в', locative='Воронеже'),
                    Region(rid=56, name='Челябинск', preposition='в', locative='Челябинске'),
                    Region(rid=35, name='Краснодар', preposition='в', locative='Краснодаре'),
                ],
            )
        ]

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='super_hype_goods_boost',
                type_name='super_hype_goods_boost',
                args={'@type': 'type.googleapis.com/MarketSearch.Booster.TNoArgs'},
                base_coeffs={
                    'text': 2,
                    'textless': 2,
                },
                hids=[],
            )
        ]

        cls.index.nailed_docs_white_list += [
            QueryIntList(
                query='Громкая новинка',
                integer_ids=[
                    '30001',
                    '30004',
                ],
            ),
        ]

    def test_super_hype_goods_boosting(self):
        """Проверяем, что флаг market_super_hype_goods_boost_coef задает коэффициент бустинга
        для SKU с признаком "Громкая новинка" без офферов
        https://st.yandex-team.ru/MARKETOUT-45429
        https://st.yandex-team.ru/MARKETOUT-45718
        """
        request = 'place=prime&text=Super+hype+goods&rids=213&local-offers-first=0&cpa=real&use-defaul-offers=1&onstock=0&debug=1&hid=30'
        for boost_flag in [
            '&rearr-factors=market_enable_new_booster=0;market_super_hype_goods_boost_coef=2',
            '&rearr-factors=market_enable_new_booster=1',
        ]:
            response = self.report.request_json(request + boost_flag)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "30001",
                            "titles": {"raw": "Super hype goods sku without offers"},
                            "isSuperHypeGoods": True,
                            "debug": {
                                "properties": {"BOOST_MULTIPLIER": Round(2)},
                                "metaProperties": {"BOOST_MULTIPLIER": Round(2)},
                            },
                        },
                        {
                            "entity": "sku",
                            "id": "30004",
                            "titles": {"raw": "Super hype goods sku with model"},
                            "isSuperHypeGoods": True,
                            "debug": {
                                "properties": {"BOOST_MULTIPLIER": Round(2)},
                                "metaProperties": {"BOOST_MULTIPLIER": Round(2)},
                            },
                        },
                        {
                            "entity": "offer",
                            "sku": "30003",
                            "titles": {"raw": "Not super hype goods offer"},
                            "isSuperHypeGoods": Absent(),
                            "debug": {
                                "properties": {"BOOST_MULTIPLIER": Round(1)},
                                "metaProperties": {"BOOST_MULTIPLIER": Round(1)},
                            },
                        },
                        {
                            "entity": "offer",
                            "sku": "30002",
                            "titles": {"raw": "Super hype goods offer"},
                            "isSuperHypeGoods": Absent(),
                            "debug": {
                                "properties": {"BOOST_MULTIPLIER": Round(1)},
                                "metaProperties": {"BOOST_MULTIPLIER": Round(1)},
                            },
                        },
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )
            self.assertFragmentNotIn(response, {"entity": "regionalDelimiter"})

    def test_disable_super_hype_goods_by_offer_param(self):
        """Проверяем, что громкие новинки не добавляются в выдачу если в запросе есть офферные фильтры
        https://st.yandex-team.ru/MARKETOUT-46199
        https://st.yandex-team.ru/MARKETOUT-46697
        """
        request = 'place=prime&text=Super+hype+goods&allow-collapsing=1&local-offers-first=0&hid=30'
        offer_specific_params = [
            '&mcpricefrom=1',
            '&mcpriceto=1000',
            '&at-beru-warehouse=1',
            '&with-yandex-delivery=1',
            '&filter-express-delivery=1',
            '&filter-express-delivery-today=1',
            '&offer-shipping=1',
            '&delivery_interval=0',
            '&payments=delivery_card',
        ]
        for param in offer_specific_params:
            response = self.report.request_json(request + param)
            self.assertFragmentNotIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "30001",
                            "titles": {"raw": "Super hype goods sku without offers"},
                            "isSuperHypeGoods": True,
                        },
                        {
                            "entity": "sku",
                            "id": "30004",
                            "titles": {"raw": "Super hype goods sku with model"},
                            "isSuperHypeGoods": True,
                        },
                    ]
                },
            )

    def test_super_hype_goods_model_info(self):
        """Проверяем, что в выдачу для карточки модели добавляется признак "Громкая новинка"
        https://st.yandex-team.ru/MARKETOUT-46369
        """
        response = self.report.request_json('place=modelinfo&rids=213&market-sku=30001')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "30001",
                        "titles": {"raw": "Super hype goods sku without offers"},
                        "isSuperHypeGoods": True,
                    },
                ]
            },
        )

        response = self.report.request_json('place=modelinfo&rids=213&market-sku=30003')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "30003",
                        "titles": {"raw": "Not super hype goods sku"},
                        "isSuperHypeGoods": Absent(),
                    },
                ]
            },
        )

    def test_super_hype_incut(self):
        """
        Проверяем блендерную врезку.
        SKU не в наличии переезжают во врезку INCLID_SUPER_HYPE=27 и не показываются в основной выдаче
        """
        request = (
            'place=prime&blender=1'
            '&text=Super+hype+goods'
            '&cpa=real&onstock=0&allow-collapsing=1&rids=213&local-offers-first=0&use-default-offers=1'
            '&rearr-factors=market_enable_super_hype_incut=1;market_blender_bundles_for_inclid=27:super_hype.json;market_new_cpm_iterator=4'
            '&platform=desktop&client=frontend&debug=lite&pp=18&viewtype=list'
            '&supported-incuts={%221%22%3A+[%221%22%2C+%222%22%2C+%223%22%2C+%224%22%2C+%225%22%2C+%2215%22]%2C+%222%22%3A+[%221%22%2C+%222%22%2C+%223%22%2C+%224%22%2C+%225%22%2C+%2215%22]}'
        )
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "slug": "not-super-hype-goods-offer",
                    },
                    {
                        "slug": "super-hype-goods-offer",
                    },
                ]
            },
        )
        incuts = response.root.get('incuts', {}).get('results', [])
        assert len(incuts) == 1
        incut = incuts[0]
        assert incut['inClid'] == 27
        assert len(incut['items']) == 20
        for item in incut['items']:
            assert item.get('isSuperHypeGoods')
            assert not item.get('offers', {})
            assert item.get('entity') == 'sku'
        self.show_log_tskv.expect(
            inclid=27,
            position=3,
            shop_name="Yandex.Market.Childless",
            hyper_id=30219,
            msku=30219,
            url_type=16,
            record_type=1,
            show_uid=incut['items'][2]['showUid'],
            show_block_id=incut['showUid'][:-5],
            super_uid=incut['showUid'],
        ).once()
        self.show_log_tskv.expect(inclid=0, shop_name="Yandex.Market.Childless").never()

    def test_not_enough_for_incut(self):
        """
        Проверяем, что врезка не строится, а товары попадают в обычную выдачу, если их нашлось меньше минимально необходимого
        для врезки количества (3 по умолчанию)
        """
        request = (
            'place=prime&blender=1'
            '&text=Super+hype+goods&hid=50'
            '&cpa=real&onstock=0&allow-collapsing=1&rids=213&local-offers-first=0&use-default-offers=1'
            '&rearr-factors=market_enable_super_hype_incut=1;market_blender_bundles_for_inclid=27:super_hype.json;market_new_cpm_iterator=4'
            '&platform=desktop&client=frontend&debug=lite&pp=18&viewtype=list'
            '&supported-incuts={%221%22%3A+[%221%22%2C+%222%22%2C+%223%22%2C+%224%22%2C+%225%22%2C+%2215%22]%2C+%222%22%3A+[%221%22%2C+%222%22%2C+%223%22%2C+%224%22%2C+%225%22%2C+%2215%22]}'
        )
        response = self.report.request_json(request)
        incuts = response.root.get('incuts', {}).get('results', [])
        assert len(incuts) == 0
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "slug": "super-hype-goods-sku-with-model-30200",
                    },
                    {
                        "slug": "super-hype-goods-sku-with-model-30201",
                    },
                ]
            },
        )
        self.show_log_tskv.expect(
            inclid=0, shop_name="Yandex.Market.Childless", hyper_id=30200, msku=30200, url_type=16, record_type=1
        ).once()

    def test_super_hype_incut_fallback(self):
        """
        Если нарушаются условия экспа (например, market_new_cpm_iterator=0), то superhype остаются в основной выдаче, как раньше.
        Блендерный конфиг тут не нужен, документы нагребаются до блендера.
        """
        request = (
            'place=prime&blender=1'
            '&text=Super+hype+goods&hid=40'
            '&cpa=real&onstock=0&allow-collapsing=1&rids=213&local-offers-first=0&use-default-offers=1'
            '&rearr-factors=market_enable_super_hype_incut=1;market_new_cpm_iterator=0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "slug": "super-hype-goods-sku-with-model-30219",
                    }
                ]
            },
        )
        self.show_log_tskv.expect(
            position=1,
            inclid=0,
            shop_name="Yandex.Market.Childless",
            hyper_id=30219,
            msku=30219,
            url_type=16,
            record_type=1,
        ).once()

    def test_nailed_super_hype_goods(self):
        """Проверяем что в выдачу прибиваются СКУ с признаком "Громкая новинка"
        без офферов из файла nailed-docs-white-list.db
        https://st.yandex-team.ru/MARKETOUT-45651
        """
        response = self.report.request_json('place=prime&text=Громкая+новинка')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "30001",
                        "titles": {"raw": "Super hype goods sku without offers"},
                        "isSuperHypeGoods": True,
                    },
                    {
                        "entity": "sku",
                        "id": "30004",
                        "titles": {"raw": "Super hype goods sku with model"},
                        "isSuperHypeGoods": True,
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
