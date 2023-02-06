#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import Model, MarketAccessYtbinfileResource, MarketAccessSingleFbResource, Region

from core.types.ytbinfile_resources import (
    ModelFashionabilityRecord,
    AgeEcomStatsRecord,
    AgeCategEcomStatsRecord,
    AgeCategStreamEcomStatsRecord,
    AgeVendorEcomStatsRecord,
    AgeCategVendorEcomStatsRecord,
    CategEcomStatsRecord,
    CategStreamEcomStatsRecord,
    CategVendorEcomStatsRecord,
    GenderEcomStatsRecord,
    GenderVendorEcomStatsRecord,
    GenderCategEcomStatsRecord,
    GenderCategVendorEcomStatsRecord,
    GenderCategStreamEcomStatsRecord,
    ModelEcomStatsRecord,
    PopEcomStatsRecord,
    PopCategEcomStatsRecord,
    PopCategStreamEcomStatsRecord,
    PopVendorEcomStatsRecord,
    PopCategVendorEcomStatsRecord,
    PrEcomStatsRecord,
    PrCategEcomStatsRecord,
    PrCategStreamEcomStatsRecord,
    PrVendorEcomStatsRecord,
    PrCategVendorEcomStatsRecord,
    VendorEcomStatsRecord,
    YtBinfileRecordsStorage,
)

from core.types.hypercategory import (
    CategoryStreamRecord,
    CategoryStreamsStorage,
)

from core.bigb import BigBKeyword
from core.matcher import Round
from core.testcase import TestCase, main


MAN_5_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.EXACT_SOCDEM,
        pair_values=[
            (BigBKeyword.GENDER, BigBKeyword.GENDER_MALE),
            (BigBKeyword.AGE6, BigBKeyword.AGE6_35_44),
        ],
    ),
    BigBKeyword(id=BigBKeyword.PRISM, uint_values=[42525, 99, 5]),  # weight, cluster, segment
]

WOMAN_2_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.EXACT_SOCDEM,
        pair_values=[
            (BigBKeyword.GENDER, BigBKeyword.GENDER_FEMALE),
            (BigBKeyword.AGE6, BigBKeyword.AGE6_25_34),
        ],
    ),
    BigBKeyword(id=BigBKeyword.PRISM, uint_values=[42525, 50, 2]),  # weight, cluster, segment
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True

        cls.settings.market_access_settings.download_catstreams = True
        cls.settings.market_access_settings.download_model_fashionability = True
        cls.settings.market_access_settings.download_age_ecom_stats = True
        cls.settings.market_access_settings.download_age_categ_ecom_stats = True
        cls.settings.market_access_settings.download_age_categ_stream_ecom_stats = True
        cls.settings.market_access_settings.download_age_categ_vendor_ecom_stats = True
        cls.settings.market_access_settings.download_age_vendor_ecom_stats = True
        cls.settings.market_access_settings.download_categ_ecom_stats = True
        cls.settings.market_access_settings.download_categ_stream_ecom_stats = True
        cls.settings.market_access_settings.download_categ_vendor_ecom_stats = True
        cls.settings.market_access_settings.download_gender_ecom_stats = True
        cls.settings.market_access_settings.download_gender_vendor_ecom_stats = True
        cls.settings.market_access_settings.download_gender_categ_ecom_stats = True
        cls.settings.market_access_settings.download_gender_categ_stream_ecom_stats = True
        cls.settings.market_access_settings.download_gender_categ_vendor_ecom_stats = True
        cls.settings.market_access_settings.download_model_ecom_stats = True
        cls.settings.market_access_settings.download_pr_ecom_stats = True
        cls.settings.market_access_settings.download_pr_categ_ecom_stats = True
        cls.settings.market_access_settings.download_pr_categ_stream_ecom_stats = True
        cls.settings.market_access_settings.download_pr_categ_vendor_ecom_stats = True
        cls.settings.market_access_settings.download_pr_vendor_ecom_stats = True
        cls.settings.market_access_settings.download_pop_ecom_stats = True
        cls.settings.market_access_settings.download_pop_categ_ecom_stats = True
        cls.settings.market_access_settings.download_pop_categ_stream_ecom_stats = True
        cls.settings.market_access_settings.download_pop_categ_vendor_ecom_stats = True
        cls.settings.market_access_settings.download_pop_vendor_ecom_stats = True
        cls.settings.market_access_settings.download_vendor_ecom_stats = True

        cls.settings.market_access_settings.use_model_fashionability = True
        cls.settings.market_access_settings.use_age_ecom_stats = True
        cls.settings.market_access_settings.use_age_categ_ecom_stats = True
        cls.settings.market_access_settings.use_age_categ_stream_ecom_stats = True
        cls.settings.market_access_settings.use_age_categ_vendor_ecom_stats = True
        cls.settings.market_access_settings.use_age_vendor_ecom_stats = True
        cls.settings.market_access_settings.use_categ_ecom_stats = True
        cls.settings.market_access_settings.use_categ_stream_ecom_stats = True
        cls.settings.market_access_settings.use_categ_vendor_ecom_stats = True
        cls.settings.market_access_settings.use_gender_ecom_stats = True
        cls.settings.market_access_settings.use_gender_vendor_ecom_stats = True
        cls.settings.market_access_settings.use_gender_categ_ecom_stats = True
        cls.settings.market_access_settings.use_gender_categ_stream_ecom_stats = True
        cls.settings.market_access_settings.use_gender_categ_vendor_ecom_stats = True
        cls.settings.market_access_settings.use_model_ecom_stats = True
        cls.settings.market_access_settings.use_pr_ecom_stats = True
        cls.settings.market_access_settings.use_pr_categ_ecom_stats = True
        cls.settings.market_access_settings.use_pr_categ_stream_ecom_stats = True
        cls.settings.market_access_settings.use_pr_categ_vendor_ecom_stats = True
        cls.settings.market_access_settings.use_pr_vendor_ecom_stats = True
        cls.settings.market_access_settings.use_pop_ecom_stats = True
        cls.settings.market_access_settings.use_pop_categ_ecom_stats = True
        cls.settings.market_access_settings.use_pop_categ_stream_ecom_stats = True
        cls.settings.market_access_settings.use_pop_categ_vendor_ecom_stats = True
        cls.settings.market_access_settings.use_pop_vendor_ecom_stats = True
        cls.settings.market_access_settings.use_vendor_ecom_stats = True

    @classmethod
    def __setup_ytbinfile_access_resource(
        cls, access_server, shade_host_port, resource_name, schema_path, records, columns, index_name="index"
    ):
        resource = MarketAccessYtbinfileResource(
            access_server=access_server,
            shade_host_port=shade_host_port,
            meta_paths=cls.meta_paths,
            resource_name=resource_name,
            publisher_name="yt2binfile",
        )

        resource.create_version(YtBinfileRecordsStorage(records, columns), schema_path, index_name, columns)

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="model_fashionability",
            schema_path=cls.meta_paths.model_fashionability_schema,
            records=[
                ModelFashionabilityRecord(model_id=473090, score=0.999),
                ModelFashionabilityRecord(model_id=473091, score=0.001),
            ],
            columns=["model_id", "score"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="gender_ecom_stats",
            schema_path=cls.meta_paths.gender_ecom_stats_schema,
            records=[
                GenderEcomStatsRecord(gender=1, cnt=1000),
                GenderEcomStatsRecord(gender=2, cnt=2000),
            ],
            columns=["gender", "cnt"],
            index_name="gender",
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="gender_vendorid_ecom_stats",
            schema_path=cls.meta_paths.gender_vendor_ecom_stats_schema,
            records=[
                GenderVendorEcomStatsRecord(gender=1, vendor_id=464220, cnt=500),
                GenderVendorEcomStatsRecord(gender=2, vendor_id=464220 + 1, cnt=400),
            ],
            columns=["gender", "vendor_id", "cnt"],
            index_name="key",
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="gender_categ_ecom_stats",
            schema_path=cls.meta_paths.gender_categ_ecom_stats_schema,
            records=[
                GenderCategEcomStatsRecord(gender=1, categ_id=464220 + 10, cnt=250),
                GenderCategEcomStatsRecord(gender=2, categ_id=464220 + 20, cnt=750),
            ],
            columns=["gender", "categ_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="gender_categ_vendor_ecom_stats",
            schema_path=cls.meta_paths.gender_categ_vendor_ecom_stats_schema,
            records=[
                GenderCategVendorEcomStatsRecord(gender=1, categ_id=464220 + 10, vendor_id=464220, cnt=100),
                GenderCategVendorEcomStatsRecord(gender=2, categ_id=464220 + 20, vendor_id=464220 + 1, cnt=50),
            ],
            columns=["gender", "categ_id", "vendor_id", "cnt"],
        )

        catstream_resource = MarketAccessSingleFbResource(
            access_server=access_server,
            shade_host_port=shade_host_port,
            meta_paths=cls.meta_paths,
            resource_name="report_catstreams",
            publisher_name="report",
        )
        catstreams_records = [
            CategoryStreamRecord(hid=464220 + 10, category_stream=11),
            CategoryStreamRecord(hid=464220 + 20, category_stream=3),
        ]
        catstream_resource.create_version(CategoryStreamsStorage(catstreams_records, cls.meta_paths))

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="gender_categ_stream_ecom_stats",
            schema_path=cls.meta_paths.gender_categ_stream_ecom_stats_schema,
            records=[
                GenderCategStreamEcomStatsRecord(gender=1, categ_stream=11, cnt=450),
                GenderCategStreamEcomStatsRecord(gender=2, categ_stream=3, cnt=150),
            ],
            columns=["gender", "categ_stream", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="model_ecom_stats",
            schema_path=cls.meta_paths.model_ecom_stats_schema,
            records=[
                ModelEcomStatsRecord(model_id=464220 + 100, cnt=870),
                ModelEcomStatsRecord(model_id=464220 + 200, cnt=1227),
            ],
            columns=["model_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="pr_ecom_stats",
            schema_path=cls.meta_paths.pr_ecom_stats_schema,
            records=[
                PrEcomStatsRecord(prism_segment=5, cnt=2500),
                PrEcomStatsRecord(prism_segment=2, cnt=500),
            ],
            columns=["pr", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="pr_categ_ecom_stats",
            schema_path=cls.meta_paths.pr_categ_ecom_stats_schema,
            records=[
                PrCategEcomStatsRecord(prism_segment=5, categ_id=464220 + 10, cnt=750),
                PrCategEcomStatsRecord(prism_segment=2, categ_id=464220 + 20, cnt=400),
            ],
            columns=["pr", "categ_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="pr_categ_stream_ecom_stats",
            schema_path=cls.meta_paths.pr_categ_stream_ecom_stats_schema,
            records=[
                PrCategStreamEcomStatsRecord(prism_segment=5, categ_stream=11, cnt=175),
                PrCategStreamEcomStatsRecord(prism_segment=2, categ_stream=3, cnt=36),
            ],
            columns=["pr", "categ_stream", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="pr_vendor_ecom_stats",
            schema_path=cls.meta_paths.pr_vendor_ecom_stats_schema,
            records=[
                PrVendorEcomStatsRecord(prism_segment=5, vendor_id=464220, cnt=125),
                PrVendorEcomStatsRecord(prism_segment=2, vendor_id=464220 + 1, cnt=66),
            ],
            columns=["pr", "vendor_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="pr_categ_vendor_ecom_stats",
            schema_path=cls.meta_paths.pr_categ_vendor_ecom_stats_schema,
            records=[
                PrCategVendorEcomStatsRecord(prism_segment=5, categ_id=464220 + 10, vendor_id=464220, cnt=100),
                PrCategVendorEcomStatsRecord(prism_segment=2, categ_id=464220 + 20, vendor_id=464220 + 1, cnt=16),
            ],
            columns=["pr", "categ_id", "vendor_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="pop_ecom_stats",
            schema_path=cls.meta_paths.pop_ecom_stats_schema,
            records=[
                PopEcomStatsRecord(population_segment=5, cnt=2500),
                PopEcomStatsRecord(population_segment=2, cnt=500),
            ],
            columns=["pop", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="pop_categ_ecom_stats",
            schema_path=cls.meta_paths.pop_categ_ecom_stats_schema,
            records=[
                PopCategEcomStatsRecord(population_segment=5, categ_id=464220 + 10, cnt=750),
                PopCategEcomStatsRecord(population_segment=2, categ_id=464220 + 20, cnt=400),
            ],
            columns=["pop", "categ_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="pop_categ_stream_ecom_stats",
            schema_path=cls.meta_paths.pop_categ_stream_ecom_stats_schema,
            records=[
                PopCategStreamEcomStatsRecord(population_segment=5, categ_stream=11, cnt=175),
                PopCategStreamEcomStatsRecord(population_segment=2, categ_stream=3, cnt=36),
            ],
            columns=["pop", "categ_stream", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="pop_vendor_ecom_stats",
            schema_path=cls.meta_paths.pop_vendor_ecom_stats_schema,
            records=[
                PopVendorEcomStatsRecord(population_segment=5, vendor_id=464220, cnt=125),
                PopVendorEcomStatsRecord(population_segment=2, vendor_id=464220 + 1, cnt=66),
            ],
            columns=["pop", "vendor_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="pop_categ_vendor_ecom_stats",
            schema_path=cls.meta_paths.pop_categ_vendor_ecom_stats_schema,
            records=[
                PopCategVendorEcomStatsRecord(population_segment=5, categ_id=464220 + 10, vendor_id=464220, cnt=100),
                PopCategVendorEcomStatsRecord(population_segment=2, categ_id=464220 + 20, vendor_id=464220 + 1, cnt=16),
            ],
            columns=["pop", "categ_id", "vendor_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="age_ecom_stats",
            schema_path=cls.meta_paths.age_ecom_stats_schema,
            records=[
                AgeEcomStatsRecord(age_segment=4, cnt=500),
                AgeEcomStatsRecord(age_segment=3, cnt=1250),
            ],
            columns=["age", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="age_categ_ecom_stats",
            schema_path=cls.meta_paths.age_categ_ecom_stats_schema,
            records=[
                AgeCategEcomStatsRecord(age_segment=4, categ_id=464220 + 10, cnt=150),
                AgeCategEcomStatsRecord(age_segment=3, categ_id=464220 + 20, cnt=600),
            ],
            columns=["age", "categ_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="age_categ_stream_ecom_stats",
            schema_path=cls.meta_paths.age_categ_stream_ecom_stats_schema,
            records=[
                AgeCategStreamEcomStatsRecord(age_segment=4, categ_stream=11, cnt=200),
                AgeCategStreamEcomStatsRecord(age_segment=3, categ_stream=3, cnt=850),
            ],
            columns=["age", "categ_stream", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="age_vendor_ecom_stats",
            schema_path=cls.meta_paths.age_vendor_ecom_stats_schema,
            records=[
                AgeVendorEcomStatsRecord(age_segment=4, vendor_id=464220, cnt=125),
                AgeVendorEcomStatsRecord(age_segment=3, vendor_id=464220 + 1, cnt=430),
            ],
            columns=["age", "vendor_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="age_categ_vendor_ecom_stats",
            schema_path=cls.meta_paths.age_categ_vendor_ecom_stats_schema,
            records=[
                AgeCategVendorEcomStatsRecord(age_segment=4, categ_id=464220 + 10, vendor_id=464220, cnt=100),
                AgeCategVendorEcomStatsRecord(age_segment=3, categ_id=464220 + 20, vendor_id=464220 + 1, cnt=350),
            ],
            columns=["age", "categ_id", "vendor_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="categ_ecom_stats",
            schema_path=cls.meta_paths.categ_ecom_stats_schema,
            records=[
                CategEcomStatsRecord(categ_id=464220 + 10, cnt=330),
                CategEcomStatsRecord(categ_id=464220 + 20, cnt=450),
            ],
            columns=["categ_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="categ_stream_ecom_stats",
            schema_path=cls.meta_paths.categ_stream_ecom_stats_schema,
            records=[
                CategStreamEcomStatsRecord(categ_stream=11, cnt=420),
                CategStreamEcomStatsRecord(categ_stream=3, cnt=1050),
            ],
            columns=["categ_stream", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="categ_vendor_ecom_stats",
            schema_path=cls.meta_paths.categ_vendor_ecom_stats_schema,
            records=[
                CategVendorEcomStatsRecord(categ_id=464220 + 10, vendor_id=464220, cnt=270),
                CategVendorEcomStatsRecord(categ_id=464220 + 20, vendor_id=464220 + 1, cnt=330),
            ],
            columns=["categ_id", "vendor_id", "cnt"],
        )

        cls.__setup_ytbinfile_access_resource(
            access_server,
            shade_host_port,
            resource_name="vendor_ecom_stats",
            schema_path=cls.meta_paths.vendor_ecom_stats_schema,
            records=[
                VendorEcomStatsRecord(vendor_id=464220, cnt=54),
                VendorEcomStatsRecord(vendor_id=464220 + 1, cnt=132),
            ],
            columns=["vendor_id", "cnt"],
        )

    @classmethod
    def prepare_model_fashionability(cls):
        cls.index.models += [
            Model(title="fashionable shirt", hyperid=473090),
            Model(title="not fashionable shirt", hyperid=473091),
        ]

    def test_model_fashionability(self):
        """
        Проверяется загрузка ресурса и поиск по model_id нужного score
        MARKETOUT-47309
        """
        request = 'place=prime&text=shirt&debug=da'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "modelId": 473090,
                "factors": {"MODEL_FASHIONABILITY_SCORE": Round(0.999)},
            },
        )

        self.assertFragmentIn(
            response,
            {
                "modelId": 473091,
                "factors": {"MODEL_FASHIONABILITY_SCORE": Round(0.001)},
            },
        )

    @classmethod
    def prepare_ecom_stats(cls):
        cls.bigb.on_request(yandexuid='46422555', client='merch-machine').respond(keywords=MAN_5_PROFILE)
        cls.bigb.on_request(yandexuid='46422222', client='merch-machine').respond(keywords=WOMAN_2_PROFILE)

        cls.index.models += [
            Model(title="tactical backpack", hyperid=464220 + 100, vendor_id=464220, hid=464220 + 10),
            Model(title="shoulder bag", hyperid=464220 + 200, vendor_id=464220 + 1, hid=464220 + 20),
        ]

        # todo: мб сделать честное дерево
        cls.index.regiontree += [
            Region(
                rid=213,
                name='Москва',
                population=15000000,
            ),
            Region(rid=10758, name='Химки', population=230000),
        ]

    def test_ecom_stats(self):
        """
        Проверяем загрузку ecom_stats ресурсов и вычисление факторов по ним
        MARKETOUT-46422
        """
        # male profile
        request = 'place=prime&text=backpack&debug=da&yandexuid=46422555&rids=213'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "factors": {
                    "ECOMLOG_AGE_CATEG_ORDERS_DIV_TOTAL_AGE_ORDERS": Round(0.3),
                    "ECOMLOG_AGE_CATEG_STREAM_ORDERS_DIV_TOTAL_AGE_ORDERS": Round(0.4),
                    "ECOMLOG_AGE_VENDOR_ORDERS_DIV_TOTAL_AGE_ORDERS": Round(0.25),
                    "ECOMLOG_AGE_CATEG_VENDOR_ORDERS_DIV_TOTAL_AGE_ORDERS": Round(0.2),
                    "ECOMLOG_CATEG_ORDERS_DIV_TOTAL_ORDERS": Round(0.11),
                    "ECOMLOG_CATEG_STREAM_ORDERS_DIV_TOTAL_ORDERS": Round(0.14),
                    "ECOMLOG_CATEG_VENDOR_ORDERS_DIV_TOTAL_ORDERS": Round(0.09),
                    "ECOMLOG_GENDER_VENDOR_ORDERS_DIV_TOTAL_GENDER_ORDERS": Round(0.5),
                    "ECOMLOG_GENDER_CATEG_ORDERS_DIV_TOTAL_GENDER_ORDERS": Round(0.25),
                    "ECOMLOG_GENDER_CATEG_VENDOR_ORDERS_DIV_TOTAL_GENDER_ORDERS": Round(0.1),
                    "ECOMLOG_GENDER_CATEG_STREAM_ORDERS_DIV_TOTAL_GENDER_ORDERS": Round(0.45),
                    "ECOMLOG_MODEL_ORDERS_DIV_TOTAL_ORDERS": Round(0.29),
                    "ECOMLOG_PR_CATEG_ORDERS_DIV_TOTAL_PR_ORDERS": Round(0.3),
                    "ECOMLOG_PR_CATEG_STREAM_ORDERS_DIV_TOTAL_PR_ORDERS": Round(0.07),
                    "ECOMLOG_PR_VENDOR_ORDERS_DIV_TOTAL_PR_ORDERS": Round(0.05),
                    "ECOMLOG_PR_CATEG_VENDOR_ORDERS_DIV_TOTAL_PR_ORDERS": Round(0.04),
                    "ECOMLOG_POP_CATEG_ORDERS_DIV_TOTAL_POP_ORDERS": Round(0.3),
                    "ECOMLOG_POP_CATEG_STREAM_ORDERS_DIV_TOTAL_POP_ORDERS": Round(0.07),
                    "ECOMLOG_POP_VENDOR_ORDERS_DIV_TOTAL_POP_ORDERS": Round(0.05),
                    "ECOMLOG_POP_CATEG_VENDOR_ORDERS_DIV_TOTAL_POP_ORDERS": Round(0.04),
                    "ECOMLOG_VENDOR_ORDERS_DIV_TOTAL_ORDERS": Round(0.018),
                },
            },
        )

        # female profile
        request = 'place=prime&text=bag&debug=da&yandexuid=46422222&rids=10758'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "factors": {
                    "ECOMLOG_AGE_CATEG_ORDERS_DIV_TOTAL_AGE_ORDERS": Round(0.48),
                    "ECOMLOG_AGE_CATEG_STREAM_ORDERS_DIV_TOTAL_AGE_ORDERS": Round(0.68),
                    "ECOMLOG_AGE_VENDOR_ORDERS_DIV_TOTAL_AGE_ORDERS": Round(0.344),
                    "ECOMLOG_AGE_CATEG_VENDOR_ORDERS_DIV_TOTAL_AGE_ORDERS": Round(0.28),
                    "ECOMLOG_CATEG_ORDERS_DIV_TOTAL_ORDERS": Round(0.15),
                    "ECOMLOG_CATEG_STREAM_ORDERS_DIV_TOTAL_ORDERS": Round(0.35),
                    "ECOMLOG_CATEG_VENDOR_ORDERS_DIV_TOTAL_ORDERS": Round(0.11),
                    "ECOMLOG_GENDER_VENDOR_ORDERS_DIV_TOTAL_GENDER_ORDERS": Round(0.2),
                    "ECOMLOG_GENDER_CATEG_ORDERS_DIV_TOTAL_GENDER_ORDERS": Round(0.375),
                    "ECOMLOG_GENDER_CATEG_VENDOR_ORDERS_DIV_TOTAL_GENDER_ORDERS": Round(0.025),
                    "ECOMLOG_GENDER_CATEG_STREAM_ORDERS_DIV_TOTAL_GENDER_ORDERS": Round(0.075),
                    "ECOMLOG_MODEL_ORDERS_DIV_TOTAL_ORDERS": Round(0.409),
                    "ECOMLOG_PR_CATEG_ORDERS_DIV_TOTAL_PR_ORDERS": Round(0.8),
                    "ECOMLOG_PR_CATEG_STREAM_ORDERS_DIV_TOTAL_PR_ORDERS": Round(0.072),
                    "ECOMLOG_PR_VENDOR_ORDERS_DIV_TOTAL_PR_ORDERS": Round(0.132),
                    "ECOMLOG_PR_CATEG_VENDOR_ORDERS_DIV_TOTAL_PR_ORDERS": Round(0.032),
                    "ECOMLOG_POP_CATEG_ORDERS_DIV_TOTAL_POP_ORDERS": Round(0.8),
                    "ECOMLOG_POP_CATEG_STREAM_ORDERS_DIV_TOTAL_POP_ORDERS": Round(0.072),
                    "ECOMLOG_POP_VENDOR_ORDERS_DIV_TOTAL_POP_ORDERS": Round(0.132),
                    "ECOMLOG_POP_CATEG_VENDOR_ORDERS_DIV_TOTAL_POP_ORDERS": Round(0.032),
                    "ECOMLOG_VENDOR_ORDERS_DIV_TOTAL_ORDERS": Round(0.044),
                },
            },
        )


if __name__ == '__main__':
    main()
