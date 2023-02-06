#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import BlueOffer, HyperCategory, HyperCategoryType, MarketSku, Model, ReportState
from core.testcase import TestCase, main
from core.matcher import NotEmpty
from core.dj import DjModel
from core.types.autogen import b64url_md5

import six

if six.PY3:
    import urllib.parse as urlparse
else:
    import urlparse


# key: djid, values: paired landing djid, default experiment
djid_default_map_nonthematic = {
    'retargeting_block': ('retargeting_landing', "blue_attractive_models_no_fmcg"),
    'retargeting_block_with_history': ('retargeting_with_history_landing', "blue_attractive_models_with_history"),
    'history_block': ('history_landing', 'recent_findings_rtmr'),
    'history_by_category_block': ('history_by_category_landing', 'recent_findings_rtmr_category'),
    'history_mc_block': ('history_mc_landing', 'recent_findings_rtmr_mc'),
    'discovery_block': ('discovery_landing', 'discovery_block'),
    'discovery_feed': ('discovery_landing', 'discovery_feed'),
    'popular_product_block': ('popular_landing', 'popular_product_block'),
    'spontaneous_product_block': ('spontaneous_landing', 'spontaneous_product_block'),
    'similar_users_product_block': ('similar_users_landing', 'similar_users_product_block'),
    'spontaneous_and_similar_users_product_block': (
        'spontaneous_and_similar_users_landing',
        'spontaneous_and_similar_users_product_block',
    ),
    'cool_product_block': ('cool_landing', 'cool_product_block'),
    'most_relevant_category_block': ('most_relevant_category_landing', 'relevant_category_goods'),
    'fmcg_block': ('fmcg_landing', 'fmcg_main'),
    'fmcg_cart_block': ('fmcg_cart_landing', 'fmcg_cart'),
    'fmcg_block_repeat_purchases': ('fmcg_landing_repeat_purchases', 'fmcg_block_repeat_purchases'),
    'fmcg_cart_block_repeat_purchases': ('fmcg_cart_landing_repeat_purchases', 'fmcg_cart_block_repeat_purchases'),
    'repeat_purchases_block': ('repeat_purchases_landing', 'repeat_purchases_block'),
    'repeat_purchases_cart_block': ('repeat_purchases_cart_landing', 'repeat_purchases_cart_block'),
    'trend_goods_block': ('trend_goods_landing', 'trend_goods_block'),
    'cashback_product_block': ('cashback_product_landing', 'cashback_product_block'),
    'blackfriday_cashback_product_block': (
        'blackfriday_cashback_product_landing',
        'blackfriday_cashback_product_block',
    ),
    'plus_cashback_product_block': ('plus_cashback_product_landing', 'plus_cashback_product_block'),
    'promo_retargeting_block': ('promo_retargeting_landing', 'promo_retargeting_block'),
    'edadeal_block': ('edadeal_landing', 'blue_attractive_models_edadeal'),
    'newcomers_discovery_block': ('newcomers_discovery_landing', 'newcomers_discovery_block'),
    'newcomers_retargeting_block': ('newcomers_retargeting_landing', 'newcomers_blue_attractive_models_no_fmcg'),
    'games_block': ('games_landing', 'games_block'),
    'cart_complementary_block': ('cart_complementary_landing', 'cart_complementary_block'),
    'popular_products_department_like_catalog_retargeting_feed': (
        'popular_products_department_like_catalog_retargeting_feed_landing',
        'popular_products_department_like_catalog_retargeting_feed',
    ),
    'ecom_landing_retargeting_block': ('ecom_landing_retargeting_landing', 'ecom_landing_retargeting_block'),
    'ecom_landing_discovery_block': ('ecom_landing_discovery_landing', 'ecom_landing_discovery_block'),
    'ecom_landing_promo_retargeting_block': (
        'ecom_landing_promo_retargeting_landing',
        'ecom_landing_promo_retargeting_block',
    ),
    'hardcoded_fashion_retargeting_block': (
        'hardcoded_fashion_retargeting_landing',
        'hardcoded_fashion_retargeting_block',
    ),
    'hardcoded_top_popular_retargeting_block': (
        'hardcoded_top_popular_retargeting_landing',
        'hardcoded_top_popular_retargeting_block',
    ),
    'hardcoded_gifts_retargeting_block': ('hardcoded_gifts_retargeting_landing', 'hardcoded_gifts_retargeting_block'),
    'segment_landing_retargeting_block': ('segment_landing_retargeting_landing', 'segment_landing_retargeting_block'),
    'segment_landing_discovery_block': ('segment_landing_discovery_landing', 'segment_landing_discovery_block'),
}

djid_default_map_thematic = {
    'thematics_product_block': ('', "market_thematics_from_context"),
    'thematics_category_block': ('', 'market_thematics_from_context'),
    'morda_promo_thematics_product_block': ('', 'market_morda_thematics_promo'),
    'landing_promo_thematics_product_block': ('', 'market_landing_thematics_promo'),
    'department_page_thematics_product_block': ('', "market_department_page_thematics"),
    'department_page_thematics_category_block': ('', 'market_department_page_thematics'),
    'model_page_thematics_product_block': ('', "market_model_page_thematics"),
    'model_page_thematics_category_block': ('', 'market_model_page_thematics'),
    'department_page_promo_thematics_product_block': ('', 'market_department_page_thematics_promo'),
    'edadeal_promo_thematics_product_block': ('', 'edadeal_promo_thematics_product_block'),
    'model_card_thematics_block': ('', 'market_model_card_thematics'),
    'catalog_thematics_block': ('', 'market_catalog_thematics'),
    'recom_thematic_product_incut': ('', 'thematic_incut'),
    'recom_thematic_category_incut': ('', 'thematic_incut'),
    'ecom_landing_thematics_product_block': ('', "ecom_landing_thematics"),
    'ecom_landing_thematics_category_block': ('', "ecom_landing_thematics"),
    'segment_landing_thematics_product_block': ('', "segment_landing_thematics_product_block"),
    'segment_landing_thematics_category_block': ('', "segment_landing_thematics_category_block"),
}


ignore_search_result_for_id = (
    'cashback_product_block',
    'blackfriday_cashback_product_block',
    'plus_cashback_product_block',
    'promo_retargeting_block',
)


recommended_models = [
    DjModel(id="1241", title='model#1241'),
    DjModel(id="1237", title='model#1237'),
    DjModel(id="1236", title='model#1236'),
]


class T(TestCase):
    @classmethod
    def generate_report_state(cls, models):
        rs = ReportState.create()
        for m in models:
            doc = rs.search_state.nailed_docs.add()
            doc.model_id = str(m)
        rs.search_state.nailed_docs_from_recom_morda = True
        return ReportState.serialize(rs).replace('=', ',')

    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        hids = [54321, 54322, 54323]
        cls.index.hypertree += [
            HyperCategory(hid=1, children=[HyperCategory(hid=HID, output_type=HyperCategoryType.GURU) for HID in hids]),
            HyperCategory(
                hid=2,
            ),
        ]

        blue_offers = [
            BlueOffer(offerid='shop_sku_' + str(id), feedid=id, waremd5=b64url_md5('blue-{}'.format(id)))
            for id in range(1, 9)
        ]

        # models
        cls.index.models += [
            Model(hyperid=1236, hid=hids[0]),
            Model(hyperid=1237, hid=hids[1]),
            Model(hyperid=1241, hid=hids[2]),
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(title='dj_blue_market_sku_3', hyperid=1236, sku=11200003, blue_offers=[blue_offers[3]]),
            MarketSku(title='dj_blue_market_sku_4', hyperid=1237, sku=11200004, blue_offers=[blue_offers[4]]),
            MarketSku(title='dj_blue_market_sku6', hyperid=1241, sku=11200005, blue_offers=[blue_offers[7]]),
        ]

        for djid, (landing_djid, _) in djid_default_map_nonthematic.items():
            cls.dj.on_request(exp=djid, yandexuid='701').respond(
                recommended_models, landing_djid=landing_djid, url='/feedlist/'
            )
        cls.dj.on_request(exp="custom_experiment", yandexuid='701').respond(recommended_models)

        for djid in djid_default_map_thematic:
            cls.dj.on_request(exp=djid, yandexuid='701').respond(
                recommended_models, nid='33333333', title='Ну ты, парень, симпатяга', url='/catalog/33333333/list'
            )

    def check_url(self, actual_url, expected_url):
        actual_url = urlparse.urlparse(actual_url)
        expected_url = urlparse.urlparse(expected_url)

        self.assertEqual(actual_url.path, expected_url.path)

        self.assertEqual(
            urlparse.parse_qs(actual_url.query),
            urlparse.parse_qs(expected_url.query),
        )

    def test_djid_default_experiment_mapping(self):
        def _rs(*models):
            return self.generate_report_state(models=models)

        extra_params_list_for_url_field = [
            'parentPromoId=SP23200',  # fix for SP%23200. It leads to BAD_REQUEST (external_http_service.py - due to _compare_queries).
            'supplier-id=1',
            'supplier-id=2',
            'warehouse_id=1',
            'discount-from=1.5',
            'hyperid=123,456',
        ]
        extra_params_list_for_params_field = [
            (
                'parentPromoId',
                'SP23200',
            ),  # fix for SP%23200. It leads to BAD_REQUEST (external_http_service.py - due to _compare_queries).
            ('supplier-id', '1,2'),
            ('warehouse_id', '1'),
            ('discount-from', '1.5'),
            ('hyperid', '123,456'),
        ]

        def _test_extra_parameters(request_url, params_for_url, params_for_params):
            response = self.report.request_json(request_url + '&' + '&'.join(params_for_url) + '&debug=1')
            self.error_log.ignore(code=9171)  # ignore - nid
            self.error_log.ignore(code=4500)  # ignore - warehouse
            for param in params_for_url:
                self.assertTrue(param in response['search']['link']['url'])
            for param in params_for_params:
                self.assertTrue(param[1] in response['search']['link']['params'][param[0]])

        for place in ['dj', 'dj_links']:
            for param_name in ['dj-place', 'djid']:
                for djid in djid_default_map_nonthematic:
                    request_url = 'place={report_place}&{param}={djid}&yandexuid=701&rearr-factors=market_pin_offers_to_rs=0'.format(
                        report_place=place, djid=djid, param=param_name
                    )
                    response = self.report.request_json(request_url)
                    self.assertFragmentIn(response, {'dj-meta-place': djid})

                    expected_url = '/feedlist/?djid={landing_djid}'.format(
                        landing_djid=djid_default_map_nonthematic[djid][0]
                    )
                    expected_url_rs = '{url}&rs={rs}&tl=1'.format(url=expected_url, rs=_rs(1241, 1237, 1236))

                    if djid not in ignore_search_result_for_id:
                        self.check_url(response['search']['link']['url'], expected_url_rs)

                    _test_extra_parameters(
                        request_url,
                        extra_params_list_for_url_field + ['shop-promo-id=SP211%2CSP212', 'hid=1,2', 'nid=3,4'],
                        extra_params_list_for_params_field
                        + [('shop-promo-id', 'SP211,SP212'), ('hids_str', '1,2'), ('nids_str', '3,4')],
                    )

                for djid in djid_default_map_thematic:
                    request_url = 'place={report_place}&{param}={djid}&yandexuid=701&rearr-factors=market_pin_offers_to_rs=0'.format(
                        report_place=place, djid=djid, param=param_name
                    )
                    response = self.report.request_json(request_url)
                    # TODO: metaplace->djid
                    self.assertFragmentIn(response, {'dj-meta-place': djid, 'title': 'Ну ты, парень, симпатяга'})

                    expected_url = '/catalog/33333333/list?rs={rs}&tl=1'.format(rs=_rs(1241, 1237, 1236))
                    self.check_url(response['search']['link']['url'], expected_url)

                    _test_extra_parameters(
                        request_url, extra_params_list_for_url_field, extra_params_list_for_params_field
                    )

                    # TODO: remove dj_thematic_landings
                    # response = self.report.request_json(
                    #     'place={report_place}&{param}={djid}&yandexuid=701&rearr-factors=market_pin_offers_to_rs=0;dj_thematic_landings=0'.format(
                    #         report_place=place, djid=djid, param=param_name
                    #     )
                    # )
                    # self.assertFragmentIn(response, {'search': {'link': {'url': NoKey('url')}}})

    def test_disable_landing_link(self):
        for place in ['dj', 'dj_links']:
            for param_name in ['dj-place', 'djid']:
                for id in djid_default_map_nonthematic:
                    response = self.report.request_json(
                        'place={report_place}&{param}={djid}&yandexuid=701&rearr-factors=dj_disable_landing_link={djid}'.format(
                            report_place=place, djid=id, param=param_name
                        )
                    )
                    self.assertFragmentNotIn(
                        response,
                        {'search': {'link': NotEmpty()}},
                    )

    @classmethod
    def prepare_djid_cart_complementary_2_products(cls):
        cls.dj.on_request(exp="Cart_Complementary2Product_ProductBlock_DJ", yandexuid='701').respond(
            recommended_models,
            nid='33333333',
            title='Ну ты, парень, симпатяга',
            subtitle="Красивый сабтайтл",
            url='/feedlist/?popup=1',
        )

    def test_djid_cart_complementary_2_products(self):
        for param_name in ['dj-place', 'djid']:
            response = self.report.request_json(
                'place=dj&{param}={djid}&yandexuid=701'.format(
                    djid="Cart_Complementary2Product_ProductBlock_DJ", param=param_name
                )
            )

            self.assertFragmentIn(
                response,
                {'search': {'subtitle': 'Красивый сабтайтл'}},
            )

            self.assertTrue('popup' in response['search']['link']['url'])


if __name__ == '__main__':
    main()
