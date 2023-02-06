#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Round, Contains
from core.svn_data import SvnData
from core.types import (
    HyperCategory,
    Model,
    ModelServiceDoc,
    Offer,
    Region,
    Shop,
    SplittedCategoryNamesRecord,
    WebHerfEntry,
    WebHerfFeatures,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_svn_data = True
        cls.settings.market_access_settings.use_svn_data = True

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        report_data = SvnData(access_server=access_server, shade_host_port=shade_host_port, meta_paths=cls.meta_paths)

        # todo: mb generate automatically based on category tree
        report_data.splitted_category_names += [
            SplittedCategoryNamesRecord(categ_id=1, names="list category department".split()),
        ]

        report_data.create_version()

    @classmethod
    def prepare_model_service_user_info(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.model_service.on_request(
            query='телефон',
            region=213,
            reqid='123',
            user_region_id=213,
            user_fedsubj_id=0,
            user_feddistrict_id=0,
            yandexuid="19937",
            icookie="",
            uniquid="1337",
            hid="",
            nid="",
            request_local_number=0,
            req_factors=[],
            device="desktop",
            category_to_names_chain={},
            docs=[],
            is_only_user_request=True,
        ).respond(
            features_dict={
                'model_service_model_service_query_embedding_market_multitarget_rt_dssm': [
                    0.25,
                    0.28,
                    0.14,
                    0.12,
                    0.13,
                    0.13,
                    0.19,
                ],
                'model_service_query_embedding_market_rt_dssm': [0.25, 0.28, 0.14, 0.12, 0.13, 0.13, 0.19],
                'model_service_query_embedding_market_multiclick': [0.25, 0.28, 0.14, 0.12, 0.13, 0.13, 0.19],
                'model_service_query_embedding_market_dwelltime': [0.25, 0.28, 0.14, 0.12, 0.13, 0.13, 0.19],
                'model_service_query_embedding_market_multitarget_small_diff_pretrain_dssm': [
                    0.25,
                    0.28,
                    0.14,
                    0.12,
                    0.13,
                    0.13,
                    0.19,
                ],
            }
        )

    def test_model_service_user_info(self):
        """
        Проверяем, что по флагу на мете происходит запрос
        в сервис моделей только за юзерной частью
        """
        request = (
            'place=prime&text=телефон&yandexuid=19937&icookie=228&uuid=1337&rids=213&allow-collapsing=1&debug=da&reqid=123&device=desktop'
            '&rearr-factors=market_metadoc_search=no;market_use_exp_rapid_clicks=0'
        )
        only_user_info = '&rearr-factors=model_service_only_user_info=1'
        expected = {
            "logicTrace": [
                Contains("[ME]", "IsOnlyUserInfoIntegration", "got response"),
                Contains("[ME]", "IsOnlyUserInfoIntegration", "embedding sizes debug info"),
                Contains("[ME]", "7 7 7 7"),
            ]
        }
        response = self.report.request_json(request + only_user_info)

        self.assertFragmentIn(
            response,
            expected,
            allow_different_len=True,
        )

    @classmethod
    def prepare_model_service(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            ),
        ]

        black_iphone_ms_doc = ModelServiceDoc(
            url="http://eldorado.ru/offer/1?sku=1",
            title="телефон Apple IPhone 128 GB чёрный",
            price="90",
            categ_id="1",
            normalized_url="http://eldorado.ru/offer/1",
            hyper_id=1,
            document_type=1,
            has_picture=1,
            host_hash=134,
            owner_hash=121,
            shop_priority_region=213,
            model_id=18446744073709551615,
            ware_md5="09lEaAKkQll1XTaaaaaaaQ",
            shop_id=1,
            msku_id="1",
            original_ts=1991,
            ts=1991,
            original_shop_priority_region=213,
            original_shop_id=1,
            original_ware_md5="09lEaAKkQll1XTaaaaaaaQ",
            original_msku_id="1",
            doc_factors=[
                1,
                134,
                121,
                1,
                0,
                0,
                1e09,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                3,
                0,
                10000,
                0,
                0,
                0,
                0,
                0,
                10000,
                10000,
                10000,
                10000,
                0,
                3,
                10000,
                0,
                0,
                0,
                100,
                100,
                0.01,
                100,
                0.01,
                100,
                0,
                2,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                213,
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
            ],
        )

        green_iphone_ms_doc = ModelServiceDoc(
            url="http://eldorado.ru/offer/1?sku=2",
            title="телефон Apple IPhone 128 GB зелёный",
            price="80",
            categ_id="1",
            normalized_url="http://eldorado.ru/offer/1",
            hyper_id=1,
            document_type=1,
            has_picture=1,
            host_hash=134,
            owner_hash=121,
            shop_priority_region=213,
            model_id=18446744073709551615,
            ware_md5="fDbQKU6BwzM0vDugM73auA",
            shop_id=2,
            msku_id="2",
            original_ts=1992,
            ts=1992,
            original_shop_priority_region=213,
            original_shop_id=2,
            original_ware_md5="fDbQKU6BwzM0vDugM73auA",
            original_msku_id="2",
            doc_factors=[
                1,
                134,
                121,
                1,
                0,
                0,
                1e09,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                3,
                0,
                10000,
                0,
                0,
                0,
                0,
                0,
                10000,
                10000,
                10000,
                10000,
                0,
                3,
                10000,
                0,
                0,
                0,
                100,
                100,
                0.01,
                100,
                0.01,
                100,
                0,
                2,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                213,
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
            ],
        )

        nokia_ms_doc = ModelServiceDoc(
            url="http://market.yandex.ru/product/1",
            title="телефон Nokia 3210",
            price="",
            categ_id="1",
            normalized_url="http://market.yandex.ru/product/1",
            hyper_id=1,
            document_type=2,
            has_picture=1,
            host_hash=0,
            owner_hash=0,
            shop_priority_region=0,
            model_id=1,
            ware_md5="None",
            msku_id="None",
            shop_id=18446744073709551615,
            original_ts=2000,
            ts=2000,
            original_shop_priority_region=0,
            original_shop_id=18446744073709551615,
            original_ware_md5="None",
            original_msku_id="None",
            doc_factors=[
                1,
                0,
                0,
                1,
                0,
                0,
                1e09,
                10000,
                10000,
                10000,
                0,
                3,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                3,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                10000,
                10000,
                10000,
                0,
                0,
                0,
                10000,
                10000,
                0,
                0,
                0,
                -1,
                -1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
            ],
        )

        nokia_ms_doc_with_origin_fields = ModelServiceDoc(
            url="http://market.yandex.ru/product/1",
            title="телефон Nokia 3210",
            price="",
            categ_id="1",
            normalized_url="http://market.yandex.ru/product/1",
            hyper_id=1,
            document_type=2,
            has_picture=1,
            host_hash=134,
            owner_hash=121,
            shop_priority_region=0,
            model_id=1,
            ware_md5="None",
            shop_id=18446744073709551615,
            msku_id="None",
            original_ts=1993,
            ts=2000,
            original_shop_priority_region=213,
            original_shop_id=1,
            original_ware_md5="1RM2QLmaQNDo5mfdkbuINg",
            original_msku_id="3",
            doc_factors=[
                1,
                134,
                121,
                1,
                0,
                0,
                1e09,
                10000,
                10000,
                10000,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                3,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                10000,
                10000,
                10000,
                10000,
                0,
                3,
                10000,
                10000,
                1.01,
                0,
                100,
                100,
                0.01,
                100,
                0.01,
                100,
                0,
                2,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                213,
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
            ],
        )

        cls.model_service.on_request(
            query='телефон',
            region=213,
            reqid='123',
            user_region_id=213,
            user_fedsubj_id=1,
            user_feddistrict_id=0,
            yandexuid="19937",
            icookie="",
            uniquid="1337",
            hid="",
            nid="",
            request_local_number=0,
            req_factors=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 213, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            device="desktop",
            category_to_names_chain={1: "list category department".split()},
            docs=[
                black_iphone_ms_doc,
                green_iphone_ms_doc,
                nokia_ms_doc,
                nokia_ms_doc_with_origin_fields,
            ],
        ).respond(
            features_dict={
                ('market_meta', 2687): [0.25, 0.28, 0.14, 0.11],
                ('market_meta', 2688): [0.33, 0.35, 0.48, 0.64],
                ('market_meta', 1557): [0.34, 0.35, 0.48, 0.12],
                ('market_base', 2688): [0.2, 0.3, 0.6, 0.8],
            }
        )

        cls.model_service.on_request(
            query='телефон',
            region=213,
            reqid='123',
            user_region_id=213,
            user_fedsubj_id=1,
            user_feddistrict_id=0,
            yandexuid="19937",
            icookie="",
            uniquid="1337",
            hid="",
            nid="",
            request_local_number=1,
            req_factors=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 213, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
            device="desktop",
            category_to_names_chain={1: "list category department".split()},
            docs=[
                black_iphone_ms_doc,
                green_iphone_ms_doc,
                nokia_ms_doc,
            ],
        ).respond(
            features_dict={
                ('market_meta', 2687): [0.25, 0.28, 0.14],
                ('market_meta', 2688): [0.33, 0.35, 0.48],
                ('market_meta', 1557): [0.34, 0.35, 0.48],
                ('market_base', 2688): [0.2, 0.3, 0.6],
            }
        )

        cls.index.hypertree += [
            HyperCategory(
                hid=46050,
                uniq_name="department",
                children=[
                    HyperCategory(hid=1, tovalid=444, uniq_name="list category"),
                ],
            ),
        ]

        cls.index.models += [
            Model(title='телефон Nokia 3210', hyperid=1, hid=1, ts=2000),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
        ]

        cls.index.offers += [
            Offer(
                title='телефон Apple IPhone 128 GB чёрный',
                bid=90,
                price=10000,
                fesh=1,
                hid=1,
                sku=1,
                ts=1991,
                waremd5='09lEaAKkQll1XTaaaaaaaQ',
                url='http://eldorado.ru/offer/1?sku=1',
            ),
            Offer(
                title='телефон Apple IPhone 128 GB зелёный',
                bid=80,
                price=10000,
                fesh=2,
                hid=1,
                sku=2,
                ts=1992,
                waremd5='fDbQKU6BwzM0vDugM73auA',
                url='http://eldorado.ru/offer/1?sku=2',
            ),
            Offer(
                title='телефон Nokia 3210 серебристый',
                hyperid=1,
                bid=80,
                price=10000,
                fesh=1,
                hid=1,
                sku=3,
                ts=1993,
                waremd5='1RM2QLmaQNDo5mfdkbuINg',
                url='http://eldorado.ru/offer/2',
            ),
        ]

        cls.index.web_herf_features += [
            WebHerfEntry(
                host='http://eldorado.ru',
                features=WebHerfFeatures(owner_enough_clicked=1, owner_hash=121, host_hash=134),
            )
        ]

    def test_model_service(self):
        """
        Проверяем, что факторы из сервиса моделей применяются к документам
        на мете
        """
        request = (
            'place=prime&text=телефон&yandexuid=19937&icookie=228&uuid=1337&rids=213&allow-collapsing=1&debug=da&reqid=123&device=desktop'
            '&rearr-factors=market_metadoc_search=no;market_use_exp_rapid_clicks=0'
        )
        rev_flag = '&rearr-factors=market_use_model_service=0'
        expected = {
            'results': [
                {
                    'entity': 'offer',
                    'titles': {'raw': 'телефон Apple IPhone 128 GB чёрный'},
                    'debug': {'factors': {'DSSM_MARKET_WEB_MODEL': Round(0.33, 2)}},
                },
                {
                    'entity': 'offer',
                    'titles': {'raw': 'телефон Apple IPhone 128 GB зелёный'},
                    'debug': {'factors': {'DSSM_MARKET_WEB_MODEL': Round(0.35, 2)}},
                },
                {
                    'entity': 'product',
                    'titles': {'raw': 'телефон Nokia 3210'},
                    'debug': {'factors': {'DSSM_MARKET_WEB_MODEL': Round(0.48, 2)}},
                },
            ],
        }

        response = self.report.request_json(request + rev_flag)
        self.assertFragmentNotIn(response, expected)

        response = self.report.request_json(request)
        self.assertFragmentIn(response, expected, allow_different_len=False)

    @classmethod
    def prepare_error(cls):
        cls.model_service.needs_default = False

        cls.index.shops += [
            Shop(fesh=2, priority_region=213),
        ]
        cls.index.offers += [
            Offer(title='чайник', fesh=2),
        ]

    def test_error(self):
        """
        Проверяем, что, если факторы не нашлись, репорт нормально отработает
        """

        self.error_log.expect(code=3830).once()

        response = self.report.request_json(
            'place=prime&text=чайник&rids=213&allow-collapsing=1&debug=da'
            '&rearr-factors=market_use_model_service=1&reqid=123'
        )
        self.assertFragmentIn(response, {'results': [{'titles': {'raw': 'чайник'}}]})

    @classmethod
    def prepare_pokupki_offer(cls):
        cls.index.offers += [
            Offer(
                title='свч печь',
                bid=90,
                price=10000,
                fesh=1,
                hid=1,
                url='http://pokupki.market.yandex.ru/product/1?offerid=1',
            ),
        ]

    def test_pokupki_offer(self):
        request = (
            'place=prime&text=печь&yandexuid=19937&icookie=228&uuid=1337&rids=213&allow-collapsing=1&debug=da&reqid=123'
        )
        factors_flag = (
            '&rearr-factors=use_meta_dssm_factors_from_model_service=1;beru_instead_pokupki_for_dssm_enabled=1'
        )
        self.error_log.expect(code=3830).once()

        response = self.report.request_json(request + factors_flag)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains("[ME]", "http://pokupki.market.yandex.ru/product/1?offerid=1", "http://beru.ru/product/1")
                ]
            },
            allow_different_len=True,
        )


if __name__ == '__main__':
    main()
