#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    OverallModel,
    Picture,
    RegionalModel,
    Shop,
    VCluster,
)
from core.testcase import TestCase, main
from core.types.autogen import Const
from core.matcher import NotEmpty, NoKey, Absent
from core.types.hypercategory import ADULT_CATEG_ID
from core.types.picture import thumbnails_config


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=2, output_type=HyperCategoryType.CLUSTERS),
            HyperCategory(hid=3, output_type=HyperCategoryType.GURULIGHT),
            HyperCategory(hid=4, output_type=HyperCategoryType.SIMPLE),
        ]

        cls.index.models += [
            Model(
                hyperid=401,
                hid=1,
                title='iphone',
                vendor_id=101,
                picinfo='//mdata.yandex.net/i?path=iphone.jpg',
                add_picinfo='',
                licensor=111,
                hero_global=121,
                pers_model=131,
            ),
            Model(hyperid=402, hid=2, title='ipad', vendor_id=101, licensor=111, hero_global=121, pers_model=131),
        ]

        cls.index.navtree += [
            NavCategory(hid=1, nid=201),
            NavCategory(hid=2, nid=202),
            NavCategory(hid=3, nid=203),
            NavCategory(
                nid=250,
                name='RootNavNode',
                uniq_name='Root navigation node',
                children=[
                    NavCategory(
                        nid=251,
                        children=[
                            NavCategory(hid=4, nid=204, name='Bags', uniq_name='Bags for cool things'),
                        ],
                    )
                ],
            ),
            NavCategory(hid=5, nid=205),
        ]

        cls.index.offers += [
            Offer(
                hid=4,
                vendor_id=101,
                licensor=111,
                hero_global=121,
                pers_model=131,
                picture=Picture(
                    picture_id='AQJnjRkieZz5Eis_EDl18Q',
                    width=900,
                    height=1200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['900x1200']),
                    group_id=1234,
                ),
                glparams=[GLParam(param_id=301, value=101, vendor=True, cluster_filter=True)],
            )
        ]
        cls.index.offers += [
            Offer(hid=1, vendor_id=101, hyperid=401, licensor=111, hero_global=121, pers_model=131),  # Total 2 docs
            Offer(hid=2, vendor_id=101, hyperid=402, licensor=111, hero_global=121, pers_model=131),
            Offer(hid=2, vendor_id=101, hyperid=402, licensor=111, hero_global=121, pers_model=131),  # Total 3 docs
            Offer(hid=3, vendor_id=101, licensor=111, hero_global=121, pers_model=131),
            Offer(hid=3, vendor_id=101, licensor=111, hero_global=121, pers_model=131),
            Offer(hid=3, vendor_id=101, licensor=111, hero_global=121, pers_model=131),
            Offer(hid=3, vendor_id=101, licensor=111, hero_global=121, pers_model=131),  # Total 4 docs
            Offer(hid=4, vendor_id=101, licensor=111, hero_global=121, pers_model=131, no_picture=True),
            Offer(hid=4, vendor_id=101, licensor=111, hero_global=121, pers_model=131, no_picture=True),
            Offer(hid=4, vendor_id=101, licensor=111, hero_global=121, pers_model=131, no_picture=True),
            Offer(hid=4, vendor_id=101, licensor=111, hero_global=121, pers_model=131, no_picture=True),  # Total 5 docs
        ]
        cls.index.offers += [Offer(hid=5)]

    def _test_order(self, filter):
        '''Проверяем, что сначала идут GURU и CLUSTERS категории,
        затем GURULIGHT и SIMPLE.
        Внутри каждой из двух групп сортировка по количеству документов.
        '''
        response = self.report.request_json('place=top_categories&numdoc=10&' + filter)
        self.assertFragmentIn(
            response,
            [
                {'id': 202, "link": {"params": {"hid": "2"}}},
                {'id': 201, "link": {"params": {"hid": "1"}}},
                {'id': 204, "link": {"params": {"hid": "4"}}},
                {'id': 203, "link": {"params": {"hid": "3"}}},
            ],
            preserve_order=True,
        )

    def test_order(self):
        self._test_order('vendor_id=101')
        self._test_order('licensor=111')
        self._test_order('franchise=121')
        self._test_order('character=131')

    def _test_filtering(self, filter):
        '''Проверяем, что в выдачу не попадают категории, не имеющие
        офферов/моделей с заданным условием поиска
        '''
        response = self.report.request_json('place=top_categories&numdoc=10&' + filter)
        self.assertFragmentNotIn(response, {'id': 205})

    def test_filtering(self):
        self._test_filtering('vendor_id=101')
        self._test_filtering('licensor=111')
        self._test_filtering('franchise=121')
        self._test_filtering('character=131')

    def _test_numdoc(self, filter):
        '''Проверяем, что выводится категорий не больше, чем задано в параметре numdoc'''
        response = self.report.request_json('place=top_categories&numdoc=1&' + filter)
        self.assertFragmentIn(response, {"search": {"total": 1}})
        response = self.report.request_json('place=top_categories&numdoc=10&' + filter)
        self.assertFragmentIn(response, {"search": {"total": 4}})

    def test_numdoc(self):
        self._test_numdoc('vendor_id=101')
        self._test_numdoc('licensor=111')
        self._test_numdoc('franchise=121')
        self._test_numdoc('character=131')

    def _test_category_data_in_output(self, filter, glfilter=Absent()):
        '''Проверяем состав выводимой информации о категории'''
        response = self.report.request_json('place=top_categories&numdoc=10&' + filter)
        self.assertFragmentIn(
            response,
            {
                'entity': 'navnode',
                'id': 204,
                'name': 'Bags',
                'fullName': 'Bags for cool things',
                'isLeaf': True,
                'link': {
                    'target': 'list',
                    'params': {
                        'hid': '4',
                        'nid': '204',
                        'glfilter': glfilter,
                    },
                },
                'rootNavnode': {
                    'entity': 'navnode',
                    'id': 250,
                    'fullName': 'Root navigation node',
                    'name': 'RootNavNode',
                    'isLeaf': False,
                },
            },
        )

    def test_category_data_in_output(self):
        self._test_category_data_in_output('vendor_id=101', glfilter='301:101')
        self._test_category_data_in_output('licensor=111')
        self._test_category_data_in_output('franchise=121')
        self._test_category_data_in_output('character=131')

    def test_picture_from_model(self):
        response = self.report.request_json('place=top_categories&vendor_id=101&numdoc=10')
        self.assertFragmentIn(
            response,
            {
                'id': 201,
                'icons': [
                    {'entity': 'picture', 'thumbnails': [{'url': '//mdata.yandex.net/i?path=iphone.jpg&size=1'}]}
                ],
            },
        )

    def test_picture_from_offer(self):
        response = self.report.request_json('place=top_categories&vendor_id=101&numdoc=10')
        self.assertFragmentIn(
            response,
            {
                'id': 204,
                'icons': [
                    {
                        'entity': 'picture',
                        'thumbnails': [
                            {
                                'url': 'http://avatars.mdst.yandex.net/get-marketpic/1234/market_AQJnjRkieZz5Eis_EDl18Q/900x1200',
                                'width': 900,
                                'height': 1200,
                            }
                        ],
                    }
                ],
            },
        )

    def test_missing_pp(self):
        self.report.request_json('place=top_categories&vendor_id=101&numdoc=10&ip=127.0.0.1', add_defaults=False)

    @classmethod
    def prepare_test_root_nid_is_hidden(cls):
        """
        Create offers attached to root category
        1) this offers must not be a reason for appearance of the root category in response.
        2) this category must not affect sorting and other category appearance
           despite the fact that it has the biggest number of offers.
        """
        cls.index.offers += [Offer(hid=Const.ROOT_HID, vendor_id=101) for x in range(120)]

    # MARKETOUT-10174
    def test_root_nid_is_hidden(self):
        """
        Make request for maximum amount of categories (numdoc = 120) to ensure that
        there is no root category in response at all
        """
        response = self.report.request_json('place=top_categories&vendor_id=101&numdoc=120')
        self.assertFragmentNotIn(response, {"entity": "navnode", "id": Const.ROOT_NID})

    @classmethod
    def prepare_extra_count(cls):
        """
        Create more than 120 offers
        """
        cls.index.offers += [Offer(hid=hid_id, vendor_id=108) for hid_id in range(10500, 10625)]

    def test_extra_count(self):
        """
        Make request for numdoc =125 to ensure test does not fail
        """
        response = self.report.request_json('place=top_categories&vendor_id=108&numdoc=125')
        self.assertFragmentIn(response, {"entity": "navnode"})

    def test_max_entities(self):
        """
        Make request for numdoc =125 to check that
        response contains 120 entities (max amount)
        """
        response = self.report.request_json('place=top_categories&vendor_id=108&numdoc=125')
        self.assertFragmentIn(response, {"search": {"total": 120}})

    @classmethod
    def prepare_no_pictures_pessimization_data(cls):
        '''Создаем по оферу с картинкой и без в одной категории,
        офер без картинки в другой.
        '''
        cls.index.offers += [
            Offer(
                hid=1, hyperid=401, vendor_id=102, licensor=112, hero_global=122, pers_model=132, no_picture=False, ts=1
            ),
            Offer(
                hid=1, hyperid=401, vendor_id=102, licensor=112, hero_global=122, pers_model=132, no_picture=True, ts=2
            ),
            Offer(hid=2, hyperid=402, vendor_id=103, licensor=113, hero_global=123, pers_model=133, no_picture=True),
        ]

    def _test_no_pictures_pessimization(self, filter1, filter2):
        '''Проверяем в дебаг-выдаче, что:
        - у документов стоят правильные флаги HAS_PICTURES
        - верный порядок полей релевантности (HAS_PICTURES первое)
        - на выдаче с vendor_id=102 офер с картинкой
        - на выдаче с vendor_id=103 - без картинки, т.к. с картинкой нет
        '''
        response = self.report.request_json('place=top_categories&numdoc=10&debug=da&debug-doc-count=2&' + filter1)
        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "TS": "1",
                    "HAS_PICTURE": "1",
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                "properties": {
                    "TS": "2",
                    "HAS_PICTURE": "0",
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                "rank": [
                    {"name": "HAS_PICTURE"},
                    {"name": "DELIVERY_TYPE"},
                    {"name": "IS_MODEL"},
                    {"name": "IS_NOT_MODIFICATION"},
                    {"name": "GURU_POPULARITY"},
                    {"name": "ONSTOCK"},
                    {"name": "RANDX"},
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(response, {"results": [{"icons": NotEmpty()}]})
        response = self.report.request_json('place=top_categories&numdoc=10&' + filter2)
        self.assertFragmentIn(response, {"results": [{"icons": NoKey("icons")}]})

    def test_no_pictures_pessimization(self):
        self._test_no_pictures_pessimization('vendor_id=102', 'vendor_id=103')
        self._test_no_pictures_pessimization('licensor=112', 'licensor=113')
        self._test_no_pictures_pessimization('franchise=122', 'franchise=123')
        self._test_no_pictures_pessimization('character=132', 'character=133')

    @classmethod
    def prepare_vcluster_pictures_data(cls):
        '''Для вендора создаем 2 офера с картинкой и без в разных кластерах,
        связываем с визуальными категориями и навигационными категориями.
        '''
        cls.index.hypertree += [
            HyperCategory(hid=6, visual=True),
            HyperCategory(hid=7, visual=True),
        ]

        cls.index.navtree += [
            NavCategory(hid=6, nid=206),
            NavCategory(hid=7, nid=207),
        ]

        pic = Picture(width=100, height=100, group_id=1234)
        cls.index.vclusters += [
            VCluster(
                hid=6,
                vclusterid=1000000001,
                vendor_id=104,
                licensor=114,
                hero_global=124,
                pers_model=134,
                pictures=[pic],
            ),
            VCluster(
                hid=7,
                vclusterid=1000000002,
                vendor_id=104,
                licensor=114,
                hero_global=124,
                pers_model=134,
                no_pictures=True,
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=6, vclusterid=1000000001, vendor_id=104, licensor=114, hero_global=124, pers_model=134, picture=pic
            ),
            Offer(
                hid=7,
                vclusterid=1000000002,
                vendor_id=104,
                licensor=114,
                hero_global=124,
                pers_model=134,
                no_picture=True,
            ),
        ]

    def _test_vcluster_pictures(self, filter):
        '''Проверяем, что:
        - для категории с кластером с картинкой выводится картинка
        - для категории с кластером без картинки картинка отсутствует
        '''
        response = self.report.request_json('place=top_categories&numdoc=10&' + filter)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"id": 206, "icons": NotEmpty()},
                    {"id": 207, "icons": NoKey("icons")},
                ]
            },
        )

    def test_vcluster_pictures(self):
        self._test_vcluster_pictures('vendor_id=104')
        self._test_vcluster_pictures('licensor=114')
        self._test_vcluster_pictures('franchise=124')
        self._test_vcluster_pictures('character=134')

    @classmethod
    def prepare_use_constant_bid_for_cpm_data(cls):
        '''Для вендора создаем оферы с различными bid, ctr и randx.'''
        cls.index.hypertree += [
            HyperCategory(hid=8),
            HyperCategory(hid=9),
            HyperCategory(hid=10),
        ]

        cls.index.navtree += [
            NavCategory(hid=8, nid=208),
            NavCategory(hid=9, nid=209),
            NavCategory(hid=10, nid=210),
        ]

        cls.index.offers += [
            Offer(hid=8, vendor_id=105, licensor=115, hero_global=125, pers_model=135, bid=100, randx=1000, ts=3),
            Offer(hid=9, vendor_id=105, licensor=115, hero_global=125, pers_model=135, bid=1000, randx=100, ts=4),
            Offer(hid=10, vendor_id=105, licensor=115, hero_global=125, pers_model=135, bid=100, randx=10, ts=5),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.7)

    def _test_use_constant_bid_for_cpm(self, filter):
        '''Проверяем, что у документа проставляется bid=1.
        Проверяем, что категории отранжированы по: (-CTR, -RANDX).
        '''
        response = self.report.request_json('place=top_categories&numdoc=10&debug=da&debug-doc-count=2&' + filter)
        self.assertFragmentIn(response, {'properties': {'BID': '1'}})
        self.assertFragmentIn(
            response,
            [
                {'id': 210},
                {'id': 208},
                {'id': 209},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_use_constant_bid_for_cpm(self):
        self._test_use_constant_bid_for_cpm('vendor_id=105')
        self._test_use_constant_bid_for_cpm('licensor=115')
        self._test_use_constant_bid_for_cpm('franchise=125')
        self._test_use_constant_bid_for_cpm('character=135')

    @classmethod
    def prepare_not_adult(cls):
        cls.index.models += [
            Model(hyperid=404, hid=10, vendor_id=106, licensor=106, hero_global=106, pers_model=106),
        ]

        cls.index.offers += [
            Offer(
                fesh=100,
                hid=10,
                hyperid=404,
                vendor_id=106,
                licensor=106,
                hero_global=106,
                pers_model=106,
                title="normal offer",
            ),
            Offer(
                fesh=100,
                hid=10,
                hyperid=404,
                vendor_id=106,
                licensor=106,
                hero_global=106,
                pers_model=106,
                title="normal offer",
            ),
        ]

    def _test_not_adult(self, filter):
        '''Проверяем, что флаг search.adult не установлен,
        есди у бренда нет никакого контента для взрослых
        '''
        response = self.report.request_json('place=top_categories&numdoc=10&' + filter)
        self.assertFragmentIn(response, {"total": 1, "adult": False})

    def test_not_adult(self):
        self._test_not_adult('vendor_id=106')
        self._test_not_adult('licensor=106')
        self._test_not_adult('franchise=106')
        self._test_not_adult('character=106')

    @classmethod
    def prepare_adult_model(cls):
        cls.index.models += [
            Model(hyperid=405, hid=10, vendor_id=107),
        ]

        cls.index.overall_models += [
            OverallModel(hyperid=405, is_adult=True),
        ]

        cls.index.regional_models += [RegionalModel(hyperid=405, offers=2)]

        cls.index.offers += [
            Offer(fesh=100, hid=10, vendor_id=107, title="normal offer"),
        ]

    def test_adult_model(self):
        '''Проверяем, что флаг search.adult установлен,
        есди у бренда есть модель с флагом adult
        '''
        response = self.report.request_json('place=top_categories&numdoc=10&vendor_id=107')
        self.assertFragmentIn(response, {"search": {"total": 1, "adult": True}})

    @classmethod
    def prepare_adult_category(cls):
        cls.index.hypertree += [
            HyperCategory(hid=ADULT_CATEG_ID),
        ]

        cls.index.models += [
            Model(hyperid=409, hid=ADULT_CATEG_ID, vendor_id=109),
        ]

        cls.index.offers += [
            Offer(fesh=100, hid=ADULT_CATEG_ID, hyperid=409, vendor_id=109, title="adult category offer"),
            Offer(fesh=100, hid=ADULT_CATEG_ID, hyperid=409, vendor_id=109, title="adult category offer"),
        ]

    def test_adult_category(self):
        '''Проверяем, что флаг search.adult установлен,
        если у бренда есть товары категории "Товары для взрослых" или дочерних
        '''

        response = self.report.request_json('place=top_categories&numdoc=10&vendor_id=109&adult=1')
        self.assertFragmentIn(response, {"total": 1, "adult": True})

    @classmethod
    def prepare_adult_offers(cls):
        cls.index.hypertree += [
            HyperCategory(hid=11),
            HyperCategory(hid=12),
            HyperCategory(hid=13),
        ]

        cls.index.navtree += [
            NavCategory(hid=11, nid=211),
            NavCategory(hid=12, nid=212),
            NavCategory(hid=13, nid=213),
        ]

        cls.index.offers += [
            Offer(
                fesh=100, hid=11, vendor_id=110, licensor=110, hero_global=110, pers_model=110, title="normal offer 11"
            )
            for x in range(3)
        ]

        cls.index.offers += [
            Offer(
                fesh=100, hid=12, vendor_id=110, licensor=110, hero_global=110, pers_model=110, title="normal offer 12"
            ),
            Offer(
                fesh=100, hid=12, vendor_id=110, licensor=110, hero_global=110, pers_model=110, title="normal offer 12"
            ),
            Offer(
                fesh=100,
                hid=12,
                vendor_id=110,
                licensor=110,
                hero_global=110,
                pers_model=110,
                title="adult offer 12",
                adult=True,
            ),
            Offer(
                fesh=100,
                hid=12,
                vendor_id=110,
                licensor=110,
                hero_global=110,
                pers_model=110,
                title="adult offer 12",
                adult=True,
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=100,
                hid=13,
                vendor_id=110,
                licensor=110,
                hero_global=110,
                pers_model=110,
                adult=True,
                title="adult offer 13",
            )
            for x in range(5)
        ]

    def _test_adult_offers(self, filter):
        '''Проверяем, что флаг search.adult установлен, есди у бренда есть офферы с флагом adult
        Проверяем, что adult офферы попадают в расчет популярных категорий только при установленном
        параметре запроса adult=1 (т.е. без параметра adult категории, в которых только adult-офферы, не попадут в выдачу,
        порядок категорий, основанный на количестве офферов, в зависимости от наличия параметра adult может меняться)
        '''
        response = self.report.request_json('place=top_categories&numdoc=10&' + filter)
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "adult": True,
                "results": [
                    {'id': 211, "link": {"params": {"hid": "11"}}},
                    {'id': 212, "link": {"params": {"hid": "12"}}},
                ],
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=top_categories&numdoc=10&adult=1&' + filter)
        self.assertFragmentIn(
            response,
            {
                "total": 3,
                "adult": True,
                "results": [
                    {'id': 213, "link": {"params": {"hid": "13"}}},
                    {'id': 212, "link": {"params": {"hid": "12"}}},
                    {'id': 211, "link": {"params": {"hid": "11"}}},
                ],
            },
            preserve_order=True,
        )

    def test_adult_offers(self):
        self._test_adult_offers('vendor_id=110')
        self._test_adult_offers('licensor=110')
        self._test_adult_offers('franchise=110')
        self._test_adult_offers('character=110')

    @classmethod
    def prepare_remove_guru_offers_without_hyperid(cls):
        cls.index.hypertree += [
            HyperCategory(hid=14, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=15, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=16, output_type=HyperCategoryType.GURULIGHT),
            HyperCategory(hid=73, output_type=HyperCategoryType.GURU, show_offers=True),
        ]

        cls.index.navtree += [
            NavCategory(hid=14, nid=214),
            NavCategory(hid=15, nid=215),
            NavCategory(hid=16, nid=216),
            NavCategory(hid=73, nid=273),
        ]

        cls.index.models += [
            Model(
                hyperid=410,
                hid=15,
                title='iphone',
                vendor_id=111,
                picinfo='//mdata.yandex.net/i?path=iphone.jpg',
                add_picinfo='',
            ),
        ]

        cls.index.offers += [
            Offer(hid=14, vendor_id=111),
            Offer(hid=14, vendor_id=111),
            Offer(hid=14, vendor_id=111),
            Offer(hid=14, vendor_id=111),
            Offer(hid=15, vendor_id=111),
            Offer(hid=15, vendor_id=111),
            Offer(hid=15, vendor_id=111, hyperid=410),
            Offer(hid=16, vendor_id=111),
            Offer(hid=16, vendor_id=111),
            Offer(hid=73, vendor_id=111),
        ]

    def test_remove_guru_offers_without_hyperid(self):
        '''Проверяем, что в гуру-категориях офферы без моделей отбрасыватся.
        Гуру-категории, у которых есть только офферы без моделей не попадают в выдачу top_categories
        '''
        response = self.report.request_json('place=top_categories&numdoc=10&vendor_id=111')
        self.assertFragmentIn(
            response,
            [
                {'id': 215, "link": {"params": {"hid": "15"}}},
                {'id': 273, "link": {"params": {"hid": "73"}}},
                {'id': 216, "link": {"params": {"hid": "16"}}},
            ],
            preserve_order=True,
        )
        self.assertFragmentNotIn(
            response,
            [
                {'id': 214, "link": {"params": {"hid": "14"}}},
            ],
        )

    @classmethod
    def prepare_vendor_glfilter(cls):
        cls.index.hypertree += [
            HyperCategory(hid=17, output_type=HyperCategoryType.GURULIGHT),
            HyperCategory(hid=18, output_type=HyperCategoryType.GURULIGHT),
        ]

        cls.index.navtree += [
            NavCategory(hid=17, nid=217),
            NavCategory(hid=18, nid=218),
        ]

        cls.index.gltypes += [
            GLType(param_id=517, hid=17, gltype=GLType.ENUM, vendor=True, values=[111, 112], positionless=True),
            GLType(param_id=518, hid=18, gltype=GLType.ENUM, vendor=True, values=[111, 112]),
        ]

        cls.index.offers += [
            Offer(vendor_id=112, hid=17, glparams=[GLParam(param_id=517, value=112, vendor=True)]),
            Offer(vendor_id=112, hid=18, glparams=[GLParam(param_id=518, value=112, vendor=True)]),
        ]

    def test_vendor_glfilter(self):
        '''Проверяем, что вендорский фильтр выводится только при наличии у него позиции'''
        response = self.report.request_json('place=top_categories&numdoc=10&vendor_id=112')
        self.assertFragmentIn(
            response,
            [
                {'id': 218, "link": {"params": {"hid": "18", "glfilter": "518:112"}}},
                {'id': 217, "link": {"params": {"hid": "17"}}},
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {'id': 217, "link": {"params": {"hid": "17", "glfilter": NotEmpty()}}},
            ],
        )

    @classmethod
    def prepare_search_stats(cls):
        cls.index.shops += [
            Shop(fesh=200, priority_region=213, regions=[2, 213]),
            Shop(fesh=201, priority_region=213, regions=[2, 213]),
            Shop(fesh=202, priority_region=213, regions=[213]),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=19, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=20, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=21, output_type=HyperCategoryType.GURULIGHT),
            HyperCategory(hid=22, output_type=HyperCategoryType.SIMPLE),
            HyperCategory(hid=23, output_type=HyperCategoryType.GURU),  # будет выкинута, т.к. все офферы без моделей
        ]

        cls.index.navtree += [
            NavCategory(hid=19, nid=219),
            NavCategory(hid=20, nid=220),
            NavCategory(hid=21, nid=221),
        ]

        cls.index.models += [
            Model(hyperid=411, hid=19, vendor_id=113),
            Model(hyperid=412, hid=20, vendor_id=113),
            Model(hyperid=413, hid=23, vendor_id=113),
        ]

        cls.index.offers += [
            Offer(hid=19, vendor_id=113, fesh=200),  # оффер гуру-категории без модели - не будет посчитан
            Offer(hid=19, vendor_id=113, fesh=200, hyperid=411),
            Offer(hid=20, vendor_id=113, fesh=200, hyperid=412),
            Offer(hid=21, vendor_id=113, fesh=200),
            Offer(hid=19, vendor_id=113, fesh=201, hyperid=411),
            Offer(hid=20, vendor_id=113, fesh=201, hyperid=412),
            Offer(hid=21, vendor_id=113, fesh=201),
            Offer(hid=19, vendor_id=113, fesh=202, hyperid=411),
            Offer(hid=20, vendor_id=113, fesh=202, hyperid=412),
            Offer(hid=21, vendor_id=113, fesh=202),
            Offer(hid=22, vendor_id=113, fesh=202),
            Offer(hid=23, vendor_id=113, fesh=200),  # оффер гуру-категории без модели - не будет посчитан
            Offer(hid=23, vendor_id=113, fesh=202),  # оффер гуру-категории без модели - не будет посчитан
            # ROOT-категория не будет посчитана в статистике categories
            Offer(hid=Const.ROOT_HID, vendor_id=113, fesh=200),
        ]

    def test_search_stats(self):
        '''Проверяем, что в секциях totalOffers, totalModels, shops и categories будет выведено
        количество офферов, моделей, магазинов и категорий бренда
        Статистика считается после фильтрации, т.е. согласно логике плейса выкидываются офферы гуру-категорий без моделей -
         - они не попадают в totalOffers (т.к. будут выкинуты праймом при схлопывании),
        категория где только такие офферы не попадает в categories (т.к. не имела бы офферов при переходе на нее)

        ROOT-категория так же не выводится плейсом top_categories, поэтому не учитвается и в статистике categories
        Офферы и  модели ROOT-категории, сейчас учитываются в статистике, если этот рассинхрон будет сильно заметен -
        надо будет поправить отдельным тикетом
        '''
        response = self.report.request_json('place=top_categories&numdoc=200&vendor_id=113&rids=213')
        self.assertFragmentIn(response, {'total': 4, 'totalOffers': 11, 'totalModels': 2, 'shops': 3, 'categories': 4})

        response = self.report.request_json('place=top_categories&numdoc=200&vendor_id=113&rids=2')
        self.assertFragmentIn(response, {'total': 3, 'totalOffers': 7, 'totalModels': 2, 'shops': 2, 'categories': 3})

    def test_search_stats_only(self):
        '''Аналогично предыдущему тесту, по задаем numdoc=0 и имеем на выдаче
        только статистику бренда без результатов по его категориям
        '''
        response = self.report.request_json('place=top_categories&numdoc=0&vendor_id=113&rids=213&nosearchresults=1')
        self.assertFragmentIn(
            response,
            {
                'total': 0,
                'totalOffers': 11,
                'totalModels': 2,
                'shops': 3,
                'categories': 4,
                'results': [],
            },
        )

        response = self.report.request_json('place=top_categories&numdoc=0&vendor_id=113&rids=2&nosearchresults=1')
        self.assertFragmentIn(
            response,
            {
                'total': 0,
                'totalOffers': 7,
                'totalModels': 2,
                'shops': 2,
                'categories': 3,
                'results': [],
            },
        )


if __name__ == '__main__':
    main()
