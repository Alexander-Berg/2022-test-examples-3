#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import os
import tarfile

from core.types import (
    HyperCategory,
    PrismCategoryStatsRecord,
    PrismCategoryVendorStatsRecord,
    PrismDepartmentVendorWeightRecord,
    PrismCategoryVendorWeightRecord,
    Model,
    Offer,
    Picture,
    MnPlace,
    BoosterConfigRecord,
    BoosterConfigFactor,
)
from core.types.hypercategory import (
    PrismCategoryStatsStorage,
    PrismCategoryVendorStatsStorage,
    PrismDepartmentVendorWeightStorage,
    PrismCategoryVendorWeightStorage,
)
from core.bigb import BigBKeyword, WeightedValue
from core.matcher import Round
from core.testcase import TestCase, main
from market.proto.recom.exported_dj_user_profile_pb2 import (
    TEcomVersionedDjUserProfile,
    TVersionedProfileData,
    TBrandsInDepartmentDataV1,
    TBrandsDataV1,
)  # noqa pylint: disable=import-error


Pictures = [Picture() for i in range(4)]


FASHION_BRAND_VALUES = [
    (4450901, 'NIKE'),
    (4450902, 'FiNN FLARE'),
    (4450903, 'LACOSTE'),
    (4450904, 'Tom Tailor'),
    (4450905, 'ТВОЕ'),
]


MAN_5_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=921947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=75515),
        ],
    ),
    BigBKeyword(id=BigBKeyword.PRISM_AGGREGATED, uint_values=[42525, 99, 5]),  # weight, cluster, segment
]

MAN_2_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=921947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=75515),
        ],
    ),
    BigBKeyword(id=BigBKeyword.PRISM_AGGREGATED, uint_values=[42525, 99, 2]),  # weight, cluster, segment
]


class MarketAccessResource:
    def __init__(self, access_server, shade_host_port, meta_paths, publisher_name, resource_name):
        self.__access_server = access_server
        self.__shade_host_port = shade_host_port
        self.__meta_paths = meta_paths
        self.__publisher_name = publisher_name
        self.__resource_name = resource_name

        self.__access_server.create_publisher(name=self.__publisher_name)
        self.__access_server.create_resource(name=self.__resource_name, publisher_name=self.__publisher_name)

    # add multiple resources as tar
    # resources is tuple(record_storage, filename)
    def create_version(self, resources):
        src_path = self.__meta_paths.access_resources_tmp
        dst_path = os.path.join(self.__meta_paths.access_resources, self.__resource_name + '/1.0.0')
        archive = os.path.join(dst_path, self.__resource_name + '.tar.gz')

        if not os.path.exists(dst_path):
            os.makedirs(dst_path)

        with tarfile.open(archive, 'w:gz') as tar:
            for resource in resources:
                storage, file_name = resource
                data_path = os.path.join(src_path, file_name)
                storage.save_fb(data_path)
                tar.add(data_path, file_name)

        mds_url = self._get_mds_url(self.__shade_host_port, archive)
        self.__access_server.create_version(self.__resource_name, http_url=mds_url)

    @staticmethod
    def _get_mds_url(shade_host_port, path):
        path = path if path.startswith('/') else '/' + path
        return '{host_port}/mds{path}'.format(
            host_port=shade_host_port,
            path=path,
        )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_prism_stats = True
        cls.settings.market_access_settings.use_prism_stats = True

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        prism_stats = MarketAccessResource(
            access_server=access_server,
            shade_host_port=shade_host_port,
            meta_paths=cls.meta_paths,
            resource_name="prism_stats",
            publisher_name="report",
        )

        # in csv stats prism_segment is str like "p5", but in fb i's short "5"
        prism_category_stats = [
            PrismCategoryStatsRecord(
                hid=42525 + 0, prism_segment=5, price_ratio=1.25, price_perc1=200, price_perc50=1600, avg_price=2000
            ),
            PrismCategoryStatsRecord(
                hid=42525 + 0, prism_segment=2, price_ratio=1.20, price_perc1=80, price_perc50=800, avg_price=1000
            ),
            PrismCategoryStatsRecord(
                hid=42525 + 1, prism_segment=2, price_ratio=0.8, price_perc1=100, price_perc50=500, avg_price=800
            ),
        ]

        prism_category_vendor_stats = [
            PrismCategoryVendorStatsRecord(
                hid=42525 + 10, vendor_id=1, prism_segment=5, affinity=1.46, freq=0.03, overall_freq=0.02
            ),
            PrismCategoryVendorStatsRecord(
                hid=42525 + 10, vendor_id=2, prism_segment=5, affinity=1.08, freq=0.009, overall_freq=0.008
            ),
        ]

        prism_department_vendor_weight = [
            PrismDepartmentVendorWeightRecord(
                hid=7877999, vendor_id=FASHION_BRAND_VALUES[1][0], prism_segment=5, weight=1.0
            ),
            PrismDepartmentVendorWeightRecord(
                hid=7877999, vendor_id=FASHION_BRAND_VALUES[4][0], prism_segment=5, weight=0.9
            ),
            PrismDepartmentVendorWeightRecord(
                hid=7877999, vendor_id=FASHION_BRAND_VALUES[1][0], prism_segment=2, weight=1.0
            ),
            PrismDepartmentVendorWeightRecord(
                hid=7877999, vendor_id=FASHION_BRAND_VALUES[4][0], prism_segment=2, weight=0.9
            ),
        ]

        prism_category_vendor_weight = [
            PrismCategoryVendorWeightRecord(
                hid=7877999, gender="m", vendor_id=FASHION_BRAND_VALUES[1][0], prism_segment=5, weight=1.0
            ),
            PrismCategoryVendorWeightRecord(
                hid=7877999, gender="m", vendor_id=FASHION_BRAND_VALUES[4][0], prism_segment=5, weight=0.9
            ),
            PrismCategoryVendorWeightRecord(
                hid=7877999, gender="m", vendor_id=FASHION_BRAND_VALUES[1][0], prism_segment=2, weight=1.0
            ),
            PrismCategoryVendorWeightRecord(
                hid=7877999, gender="m", vendor_id=FASHION_BRAND_VALUES[4][0], prism_segment=2, weight=0.8
            ),
            PrismCategoryVendorWeightRecord(
                hid=7811877, gender="m", vendor_id=FASHION_BRAND_VALUES[2][0], prism_segment=5, weight=1.0
            ),
            PrismCategoryVendorWeightRecord(
                hid=7811877, gender="m", vendor_id=FASHION_BRAND_VALUES[3][0], prism_segment=5, weight=0.9
            ),
            PrismCategoryVendorWeightRecord(
                hid=7811877, gender="m", vendor_id=FASHION_BRAND_VALUES[0][0], prism_segment=2, weight=1.0
            ),
            PrismCategoryVendorWeightRecord(
                hid=7811877, gender="m", vendor_id=FASHION_BRAND_VALUES[3][0], prism_segment=2, weight=0.8
            ),
            PrismCategoryVendorWeightRecord(
                hid=7811877, gender="f", vendor_id=FASHION_BRAND_VALUES[0][0], prism_segment=0, weight=1.0
            ),
            PrismCategoryVendorWeightRecord(
                hid=7811877, gender="f", vendor_id=FASHION_BRAND_VALUES[3][0], prism_segment=0, weight=0.8
            ),
        ]

        # todo: use dataclass instead of tuple
        prism_stats.create_version(
            [
                (PrismCategoryStatsStorage(prism_category_stats, cls.meta_paths), "prism_category_stats.fb"),
                (
                    PrismCategoryVendorStatsStorage(prism_category_vendor_stats, cls.meta_paths),
                    "prism_category_vendor_stats.fb",
                ),
                (
                    PrismDepartmentVendorWeightStorage(prism_department_vendor_weight, cls.meta_paths),
                    "prism_department_vendor_weight.fb",
                ),
                (
                    PrismCategoryVendorWeightStorage(prism_category_vendor_weight, cls.meta_paths),
                    "prism_category_vendor_weight.fb",
                ),
            ]
        )

    @classmethod
    def prepare_prism_category_stats(cls):
        # Заводим несколько категорий
        cls.index.hypertree += [
            HyperCategory(hid=42525 + 0),
            HyperCategory(hid=42525 + 1),
        ]

        # Заводим модель, приматченную к одной из категорий...
        cls.index.models += [Model(title="Huawei", hyperid=42525 + 100, hid=42525 + 0)]
        cls.index.offers += [Offer(title="Huawei P20 lite black", price=1000, hyperid=42525 + 100, hid=42525 + 0)]

        # ...и оффер, приматченный к другой
        cls.index.offers += [Offer(title="White socks", hid=42525 + 1, waremd5='xM8jf4Sv4TytT4wS6ZljHw', price=200)]

        P5_UID = 542525
        P2_UID = 242525

        cls.bigb.on_request(yandexuid=P5_UID, client="merch-machine").respond(
            keywords=[
                BigBKeyword(id=BigBKeyword.PRISM_AGGREGATED, uint_values=[42525, 99, 5]),  # weight, cluster, segment
                BigBKeyword(id=BigBKeyword.PRISM, uint_values=[42525, 13, 2]),
            ]
        )

        cls.bigb.on_request(yandexuid=P2_UID, client="merch-machine").respond(
            keywords=[
                BigBKeyword(id=BigBKeyword.PRISM_AGGREGATED, uint_values=[42525, 13, 2]),
            ]
        )

    def test_prism_category_stats(self):
        """
        https://st.yandex-team.ru/MARKETOUT-42525
        Проверяем, что посчитали и записали в feature-лог значения факторов по статистикам призма + категория
        """
        # факторы для пользователя из p5, минимальная цена модели
        request = 'place=prime&text=huawei&yandexuid=542525&debug=da&rearr-factors=market_use_offline_prism=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "PRISM_SEGMENT_RATIO": Round(1.25),
                "PRICE_DIV_AVG_PRICE_IN_CATEGORY_FOR_PRISM": Round(0.5),
                "PRICE_DIV_PERC1_PRICE_IN_CATEGORY_FOR_PRISM": Round(5),
                "PRICE_DIV_PERC50_PRICE_IN_CATEGORY_FOR_PRISM": Round(0.625),
            },
        )

        # факторы для пользователя из p2, цена офера
        request = 'place=prime&text=socks&yandexuid=242525&debug=da&rearr-factors=market_use_offline_prism=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "PRISM_SEGMENT_RATIO": Round(0.8),
                "PRICE_DIV_AVG_PRICE_IN_CATEGORY_FOR_PRISM": Round(0.25),
                "PRICE_DIV_PERC1_PRICE_IN_CATEGORY_FOR_PRISM": Round(2),
                "PRICE_DIV_PERC50_PRICE_IN_CATEGORY_FOR_PRISM": Round(0.4),
            },
        )

    def test_offline_only_prism(self):
        """
        https://st.yandex-team.ru/MARKETOUT-43906
        https://st.yandex-team.ru/MSSUP-233
        Проверяем, что по умолчанию используем сегмент из офлайн призмы
        """
        # факторы для пользователя из p5 под флагом отключения офлайна
        request = 'place=prime&text=huawei&yandexuid=542525&debug=da&rearr-factors=market_use_offline_prism=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "PRISM_SEGMENT_RATIO": Round(1.25),
                "PRICE_DIV_AVG_PRICE_IN_CATEGORY_FOR_PRISM": Round(0.5),
                "PRICE_DIV_PERC1_PRICE_IN_CATEGORY_FOR_PRISM": Round(5),
                "PRICE_DIV_PERC50_PRICE_IN_CATEGORY_FOR_PRISM": Round(0.625),
            },
        )

        # под умолчанию используем оффлайн призму, получим статистики для сегмента p2
        request = 'place=prime&text=huawei&yandexuid=542525&debug=da'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "PRISM_SEGMENT_RATIO": Round(1.2),
                "PRICE_DIV_AVG_PRICE_IN_CATEGORY_FOR_PRISM": Round(1),
                "PRICE_DIV_PERC1_PRICE_IN_CATEGORY_FOR_PRISM": Round(12.5),
                "PRICE_DIV_PERC50_PRICE_IN_CATEGORY_FOR_PRISM": Round(1.25),
            },
        )

    @classmethod
    def prepare_prism_category_vendor_stats(cls):
        cls.index.hypertree += [
            HyperCategory(hid=42525 + 10),
            # HyperCategory(hid=42525 + 11),
        ]

        # Заводим модель, приматченную к одной из категорий...
        cls.index.models += [Model(title="iphone", hyperid=42525 + 200, hid=42525 + 10, vendor_id=1)]
        cls.index.offers += [Offer(title="iphone 13 pro max graphite", hyperid=42525 + 200, hid=42525 + 10)]

        # ...и оффер, приматченный к другой
        cls.index.offers += [
            Offer(title="iphone 13 pro max gold", hid=42525 + 10, waremd5='xM8jf4Sv44NDI1MjUrMTAw', vendor_id=2)
        ]

        P5_UID = 542525

        cls.bigb.on_request(yandexuid=P5_UID, client="merch-machine").respond(
            keywords=[
                BigBKeyword(id=BigBKeyword.PRISM_AGGREGATED, uint_values=[42525, 99, 5]),  # weight, cluster, segment
            ]
        )

    def test_prism_category_vendor_stats(self):
        """
        https://st.yandex-team.ru/MARKETOUT-42525
        Проверяем, что посчитали и записали в feature-лог значения факторов по статистикам призма + категория + вендор
        """
        # факторы для пользователя из p5, первый вендор
        request = 'place=prime&text=iphone graphite&yandexuid=542525&debug=da&rearr-factors=market_use_offline_prism=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "VENDOR_IN_CATEGORY_FOR_PRISM_AFFINITY": Round(1.46),
                "VENDOR_FREQ_IN_CATEGORY_FOR_PRISM": Round(0.03),
                "VENDOR_FREQ_IN_CATEGORY": Round(0.02),
            },
        )

        # тот же пользователь, но другой вендор
        request = 'place=prime&text=iphone gold&yandexuid=542525&debug=da&rearr-factors=market_use_offline_prism=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "VENDOR_IN_CATEGORY_FOR_PRISM_AFFINITY": Round(1.08),
                "VENDOR_FREQ_IN_CATEGORY_FOR_PRISM": Round(0.009),
                "VENDOR_FREQ_IN_CATEGORY": Round(0.008),
            },
        )

    @classmethod
    def prepare_prism_department_weights(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=7877999,
                name='Одежда, обувь и аксессуары',
                children=[
                    HyperCategory(
                        hid=7811877,
                        name='Мужская одежда',
                        children=[
                            HyperCategory(hid=7812157, name='Мужские футболки'),
                        ],
                    ),
                    HyperCategory(
                        hid=7811873,
                        name='Женская одежда',
                        children=[
                            HyperCategory(hid=7811908, name='Женские толстовки'),
                        ],
                    ),
                ],
            ),
        ]

        for seq, (brand_id, brand_name) in enumerate(FASHION_BRAND_VALUES):
            cls.index.offers += [
                Offer(
                    hid=7812157,
                    picture=Pictures[0],
                    title='Футболка мужская ' + brand_name,
                    vendor_id=brand_id,
                    ts=4581000 + seq,
                ),
            ]

        for seq in range(0, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4581000 + seq).respond(60.0 - seq * 0.01)

        profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                BrandsInDepartmentV1=TBrandsInDepartmentDataV1(
                    Departments={7877999: TBrandsDataV1(Brands={4450903: 0.5, 4450905: 0.5})}
                ),
                BrandsV1=TBrandsDataV1(Brands={4450902: 0.5, 4450904: 0.5}),
            )
        )

        cls.bigb.on_request(yandexuid='4581001', client='merch-machine').respond(keywords=MAN_5_PROFILE)
        cls.dj.on_request(yandexuid='4581001', exp='fetch_user_profile_versioned').respond(
            profile_data=profile.SerializeToString(), is_binary_data=True
        )

        cls.bigb.on_request(yandexuid='4581002', client='merch-machine').respond(keywords=MAN_2_PROFILE)
        cls.dj.on_request(yandexuid='4581002', exp='fetch_user_profile_versioned').respond(
            profile_data=profile.SerializeToString(), is_binary_data=True
        )

        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='personal_brand_boost',
                type_name='personal_brand_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TMultiBoostArgs',
                },
                base_coeffs={
                    'text': 1.0,
                    'textless': 1.0,
                },
            ),
            BoosterConfigRecord(
                name='prism_popular_brand_boost',
                type_name='first_factor_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TFactorBoostArgs',
                    'factors': [
                        {
                            'factor_name': 'USER_FAVOURITE_BRAND_PROBABILITY',
                        },
                        {
                            'factor_name': 'PRISM_POPULAR_VENDOR',
                        },
                    ],
                },
                base_coeffs={
                    'text': 1.0,
                    'textless': 1.0,
                },
            ),
        ]

        cls.index.booster_config_factors += [
            BoosterConfigFactor(type_name='prism_popular_brands'),
        ]

    def test_boost_personal_brands(self):
        expected_result = [
            'NIKE',
            'FiNN FLARE',
            'LACOSTE',
            'Tom Tailor',
            'ТВОЕ',
        ]

        expected_factors = [
            {},
            {'PRISM_POPULAR_VENDOR': '1'},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5'},
            {},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5', 'PRISM_POPULAR_VENDOR': '0.8999999762'},
        ]

        expected_result_personal = [
            'LACOSTE',
            'ТВОЕ',
            'NIKE',
            'FiNN FLARE',
            'Tom Tailor',
        ]

        expected_factors_personal = [
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5'},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5', 'PRISM_POPULAR_VENDOR': '0.8999999762'},
            {},
            {'PRISM_POPULAR_VENDOR': '1'},
            {},
        ]

        expected_result_popular = [
            'FiNN FLARE',
            'LACOSTE',
            'ТВОЕ',
            'NIKE',
            'Tom Tailor',
        ]

        expected_factors_popular = [
            {'PRISM_POPULAR_VENDOR': '1'},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5'},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5', 'PRISM_POPULAR_VENDOR': '0.8999999762'},
            {},
            {},
        ]

        expected_factors_no_prism = [
            {},
            {},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5'},
            {},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5'},
        ]

        rearr_factors_default = [
            'fetch_recom_profile_for_prime=1',
            'market_enable_new_booster=1',
            'market_use_offline_prism=0',
        ]
        rearr_factors_personal = [
            'market_modify_boosts_text=personal_brand_boost:1.5',
            'market_modify_boosts_textless=personal_brand_boost:1.5',
            'fetch_recom_profile_for_prime=1',
            'market_enable_new_booster=1',
            'market_use_offline_prism=0',
        ]
        rearr_factors_popular = [
            'market_modify_boosts_text=prism_popular_brand_boost:1.5',
            'market_modify_boosts_textless=prism_popular_brand_boost:1.5',
            'fetch_recom_profile_for_prime=1',
            'market_enable_new_booster=1',
            'market_use_offline_prism=0',
        ]

        request_base = "debug=1&place=prime&yandexuid=4581001"

        for req in [
            '&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors_default)),
            '&text=футболка&rearr-factors={}'.format(';'.join(rearr_factors_default)),
        ]:
            # нет бустов, только факторы
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Футболка мужская ' + expected_result[i]},
                            'debug': {'factors': expected_factors[i]},
                        }
                        for i in range(0, 5)
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

        for req in [
            '&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors_personal)),
            '&text=футболка&rearr-factors={}'.format(';'.join(rearr_factors_personal)),
        ]:
            # буст по факторам перс. брендов
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Футболка мужская ' + expected_result_personal[i]},
                            'debug': {'factors': expected_factors_personal[i]},
                        }
                        for i in range(0, 5)
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

        for req in [
            '&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors_popular)),
            '&text=футболка&rearr-factors={}'.format(';'.join(rearr_factors_popular)),
        ]:
            # буст по факторам перс. брендов + популярных брендов в крипта-сегменте
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Футболка мужская ' + expected_result_popular[i]},
                            'debug': {'factors': expected_factors_popular[i]},
                        }
                        for i in range(0, 5)
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

        request_base = "debug=1&place=prime&yandexuid=4581002"

        for req in [
            '&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors_default)),
            '&text=футболка&rearr-factors={}'.format(';'.join(rearr_factors_default)),
        ]:
            # нет бустов, и нет призма-факторов для призма-сегмента 2
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Футболка мужская ' + expected_result[i]},
                            'debug': {'factors': expected_factors_no_prism[i]},
                        }
                        for i in range(0, 5)
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_prism_category_weights(cls):
        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='prism_gender_popular_brand_boost',
                type_name='first_factor_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TFactorBoostArgs',
                    'factors': [
                        {
                            'factor_name': 'USER_FAVOURITE_BRAND_PROBABILITY',
                        },
                        {
                            'factor_name': 'PRISM_GENDER_POPULAR_VENDOR',
                        },
                    ],
                },
                base_coeffs={
                    'text': 1.0,
                    'textless': 1.0,
                },
            ),
        ]

        cls.index.booster_config_factors += [
            BoosterConfigFactor(type_name='prism_gender_popular_brands'),
        ]

    def test_boost_personal_brands_by_gender(self):
        expected_result = [
            'NIKE',
            'FiNN FLARE',
            'LACOSTE',
            'Tom Tailor',
            'ТВОЕ',
        ]

        expected_factors = [
            {},
            {},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5', 'PRISM_GENDER_POPULAR_VENDOR': '1'},
            {'PRISM_GENDER_POPULAR_VENDOR': '0.8999999762'},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5'},
        ]

        expected_result_popular = [
            'Tom Tailor',
            'LACOSTE',
            'ТВОЕ',
            'NIKE',
            'FiNN FLARE',
        ]

        expected_factors_popular = [
            {'PRISM_GENDER_POPULAR_VENDOR': '0.8999999762'},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5', 'PRISM_GENDER_POPULAR_VENDOR': '1'},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5'},
            {},
            {},
        ]

        expected_result_popular_2 = [
            'NIKE',
            'Tom Tailor',
            'LACOSTE',
            'ТВОЕ',
            'FiNN FLARE',
        ]

        expected_factors_popular_2 = [
            {'PRISM_GENDER_POPULAR_VENDOR': '1'},
            {'PRISM_GENDER_POPULAR_VENDOR': '0.8000000119'},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5'},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5'},
            {},
        ]

        rearr_factors_default = [
            'fetch_recom_profile_for_prime=1',
            'market_enable_new_booster=1',
            'market_use_offline_prism=0',
        ]
        rearr_factors_popular = [
            'market_modify_boosts_text=prism_gender_popular_brand_boost:1.5',
            'market_modify_boosts_textless=prism_gender_popular_brand_boost:1.5',
            'fetch_recom_profile_for_prime=1',
            'market_enable_new_booster=1',
            'market_use_offline_prism=0',
        ]

        request_base = "debug=1&place=prime&yandexuid=4581001"

        for req in [
            '&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors_default)),
            '&text=футболка&rearr-factors={}'.format(';'.join(rearr_factors_default)),
        ]:
            # нет бустов, только факторы
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Футболка мужская ' + expected_result[i]},
                            'debug': {'factors': expected_factors[i]},
                        }
                        for i in range(0, 5)
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

        for req in [
            '&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors_popular)),
            '&text=футболка&rearr-factors={}'.format(';'.join(rearr_factors_popular)),
        ]:
            # буст по факторам перс. брендов + популярных брендов в крипта-сегменте 5, пол мужской
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Футболка мужская ' + expected_result_popular[i]},
                            'debug': {'factors': expected_factors_popular[i]},
                        }
                        for i in range(0, 5)
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

        request_base = "debug=1&place=prime&yandexuid=4581002"

        for req in [
            '&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors_popular)),
            '&text=футболка&rearr-factors={}'.format(';'.join(rearr_factors_popular)),
        ]:
            # буст по факторам перс. брендов + популярных брендов в крипта-сегменте 2, пол мужской
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Футболка мужская ' + expected_result_popular_2[i]},
                            'debug': {'factors': expected_factors_popular_2[i]},
                        }
                        for i in range(0, 5)
                    ]
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_boost_by_factor(cls):
        cls.index.booster_config_records += [
            BoosterConfigRecord(
                name='prism_gender_popular_brand_boost_transform',
                type_name='first_factor_boost',
                args={
                    '@type': 'type.googleapis.com/MarketSearch.Booster.TFactorBoostArgs',
                    'factors': [
                        {
                            'factor_name': 'PRISM_GENDER_POPULAR_VENDOR',
                            'category_args': [
                                {'left_boundary': 0.95, 'right_boundary': 1.0, 'probability': 0.78},
                                {'exact_value': 0.8999999, 'probability': 1.0},
                            ],
                        },
                    ],
                },
                request_hids=[7877999],
                base_coeffs={
                    'text': 1.0,
                    'textless': 1.0,
                },
            ),
        ]

    def test_boost_by_factor(self):
        expected_default_result = [
            'NIKE',
            'FiNN FLARE',
            'LACOSTE',
            'Tom Tailor',
            'ТВОЕ',
        ]

        expected_result = [
            'Tom Tailor',
            'LACOSTE',
            'NIKE',
            'FiNN FLARE',
            'ТВОЕ',
        ]

        expected_factors = [
            {'PRISM_GENDER_POPULAR_VENDOR': '0.8999999762'},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5', 'PRISM_GENDER_POPULAR_VENDOR': '1'},
            {},
            {},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5'},
        ]

        expected_result_2 = [
            'NIKE',
            'FiNN FLARE',
            'LACOSTE',
            'Tom Tailor',
            'ТВОЕ',
        ]

        expected_factors_2 = [
            {'PRISM_GENDER_POPULAR_VENDOR': '1'},
            {},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5'},
            {'PRISM_GENDER_POPULAR_VENDOR': '0.8000000119'},
            {'USER_FAVOURITE_BRAND_PROBABILITY': '0.5'},
        ]

        expected_factors_0 = [
            {'PRISM_GENDER_POPULAR_VENDOR': '1'},
            {},
            {},
            {'PRISM_GENDER_POPULAR_VENDOR': '0.8000000119'},
            {},
        ]

        rearr_factors = [
            'market_modify_boosts_text=prism_gender_popular_brand_boost_transform:1.5',
            'market_modify_boosts_textless=prism_gender_popular_brand_boost_transform:1.5',
            'fetch_recom_profile_for_prime=1',
            'market_enable_new_booster=1',
            'market_use_offline_prism=0',
        ]

        request_base = "debug=1&place=prime&yandexuid=4581001"

        for req in [
            # Если в запросе есть категория не из поддерева разрешенных категорий, то буст не должен применяться
            '&hid=7812157,42525&rearr-factors={}'.format(';'.join(rearr_factors)),
            # Если в запросе отсуствует категория, то буст не должен применяться
            '&text=футболка&rearr-factors={}'.format(';'.join(rearr_factors)),
        ]:
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Футболка мужская ' + expected_default_result[i]},
                        }
                        for i in range(0, 5)
                    ]
                },
                preserve_order=True,
            )

        for req in [
            '&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors)),
            '&text=футболка&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors)),
        ]:
            # буст по факторам популярных брендов в крипта-сегменте 5 с преобразованием значений
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Футболка мужская ' + expected_result[i]},
                            'debug': {'factors': expected_factors[i]},
                        }
                        for i in range(0, 5)
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

        request_base = "debug=1&place=prime&yandexuid=4581002"
        for req in [
            '&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors)),
            '&text=футболка&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors)),
        ]:
            # буст по факторам популярных брендов в крипта-сегменте 2 с преобразованием значений
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Футболка мужская ' + expected_result_2[i]},
                            'debug': {'factors': expected_factors_2[i]},
                        }
                        for i in range(0, 5)
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

        request_base = "debug=1&place=prime"
        for req in [
            '&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors)),
            '&text=футболка&hid=7812157&rearr-factors={}'.format(';'.join(rearr_factors)),
        ]:
            # буст по факторам популярных брендов в неизвестно крипта-сегменте с преобразованием значений
            response = self.report.request_json(request_base + req)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Футболка мужская ' + expected_result_2[i]},
                            'debug': {'factors': expected_factors_0[i]},
                        }
                        for i in range(0, 5)
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
