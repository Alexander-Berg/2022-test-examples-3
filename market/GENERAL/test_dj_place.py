#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Const,
    GLParam,
    GLType,
    HyperCategory,
    MarketSku,
    Model,
    ModelDescriptionTemplates,
    NavCategory,
    Offer,
    Opinion,
    Promo,
    PromoType,
    ReportState,
    Vat,
    YamarecPlaceReasonsToBuy,
)
from core.testcase import TestCase, main
from core.matcher import Absent, Contains, ElementCount, NoKey, NotEmpty, EmptyList
from core.dj import DjModel
from itertools import starmap

from core.types.offer_promo import PromoBlueCashback, OffersMatchingRules
from core.types.autogen import b64url_md5
from datetime import datetime

import six

if six.PY3:
    import urllib.parse as urlparse
else:
    import urlparse

FIRST_PAGE_MODEL_IDS = list(range(6000, 6030))
SECOND_PAGE_MODEL_IDS = list(range(6030, 6060))
THIRD_PAGE_MODEL_IDS = list(range(6060, 6070))
ALL_PAGES_MODEL_IDS = FIRST_PAGE_MODEL_IDS + SECOND_PAGE_MODEL_IDS + THIRD_PAGE_MODEL_IDS

PRIMARY_PLACE_MODEL_IDS = list(range(1000, 1020))
SECONDARY_PLACE_MODEL_IDS = list(range(1100, 1120))
TOLOKA_HONEYPOTS = list(range(2000, 2010))
TOLOKA_RECOMMENDATIONS = list(range(2100, 2120))
TOLOKA_FAKE_ITEMS = list(range(100500, 100504))
DJ_LINKS_HONEYPOTS = list(range(3100, 3110))
DJ_LINKS_RECOMMENDATIONS = list(range(3000, 3020))


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        models = list(range(6000, 6070))
        cls.index.models += [
            Model(
                hyperid=m,
                hid=m / 10,
                full_description='full description {}'.format(m),
                title_no_vendor='title not vendor {}'.format(m),
                model_name='model name {}'.format(m),
                opinion=Opinion(total_count=14, rating=4, reviews=5),
                glparams=[GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value='video {}'.format(m))],
            )
            for m in models
        ]
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=600,
                micromodel='micro template',
                friendlymodel=['friendly template'],
                seo='seo template',
            )
        ]
        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy().new_partition().add(hyperid=6000, reasons=[{'factor_name': 'Хорошая модель'}])
        ]
        cls.index.offers += [
            Offer(
                hyperid=m,
                glparams=(
                    [GLParam(param_id=120, value=1), GLParam(param_id=120, value=2)] if m in [6000, 6001] else []
                ),
            )
            for m in models
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=m,
                sku=m * 10,
                blue_offers=[
                    BlueOffer(
                        descr='offer description {}'.format(m),
                        vendor_id=100500,
                        shop_category_path='shop-path',
                        shop_category_path_ids='shop-path-ids',
                        min_quantity=2,
                        step_quantity=3,
                        title='blue offer',
                    )
                ],
            )
            for m in models
        ]

        cls.index.navtree = [
            NavCategory(hid=600, nid=1236, name='nid1236'),
        ]

        cls.index.gltypes += [
            GLType(param_id=120, hid=600, gltype=GLType.ENUM, cluster_filter=True),
            GLType(param_id=Const.MODELS_VIDEO_PARAM_ID, hid=600, xslname="models_video", gltype=GLType.STRING),
        ]

    @classmethod
    def prepare_base_dj_place(cls):
        cls.dj.on_request(exp='base', yandexuid='700').respond(
            [
                DjModel(id=6000),
            ]
        )

    def test_base_dj_place(self):
        response = self.report.request_json('place=dj&dj-place=base&yandexuid=700')
        self.assertFragmentIn(
            response, {'results': [{'entity': 'product', 'id': 6000}]}, preserve_order=True, allow_different_len=False
        )

    def test_base_dj_place_has_filters(self):
        response = self.report.request_json('place=dj&dj-place=base&yandexuid=700&rearr-factors=dj_show_filters=1')
        self.assertFragmentIn(
            response,
            {'results': [{'entity': 'product', 'id': 6000, 'filters': [{"isGuruLight": True}]}]},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_dj_short_output(self):
        def merge_two_dicts(x, y):
            z = x.copy()
            z.update(y)
            return z

        common_category_fields = {
            'entity': 'category',
            'id': 600,
        }
        full_category_fields = {
            'nid': 1600,
            'cpaType': 'cpc_and_cpa',
            'fullName': 'UNIQ-HID-600',
            'name': 'AA',
            'slug': 'aa',
        }
        short_category_fields = {
            'nid': Absent(),
            'cpaType': Absent(),
            'fullName': Absent(),
            'name': Absent(),
            'slug': Absent(),
        }

        common_navnode_fields = {
            'entity': 'navnode',
            'id': 1600,
        }
        full_navnode_fields = {
            'fullName': 'UNIQ-NID-1600',
            'name': 'NID_AA',
            'slug': 'nid-aa',
        }
        short_navnode_fields = {
            'fullName': Absent(),
            'name': Absent(),
            'slug': Absent(),
        }

        _ = {
            'entity': 'offer',
            'modelAwareTitles': {'raw': ''},
            'vendor': {'entity': 'vendor', 'id': 100500},
            'titles': {'raw': 'blue offer'},
            'supplier': {'entity': 'shop', 'id': NotEmpty()},
            'shop': {'entity': 'shop', 'id': NotEmpty(), 'feed': {'id': NotEmpty()}},
        }
        full_offer_fields = {
            'categories': [merge_two_dicts(common_category_fields, full_category_fields)],
            'navnodes': [merge_two_dicts(common_navnode_fields, full_navnode_fields)],
            'cpc': NotEmpty(),
            'description': 'offer description 6000',
            'vendor': {'name': 'VENDOR-100500', 'slug': 'vendor-100500'},
            'shop_category_path': 'shop-path',
            'shop_category_path_ids': 'shop-path-ids',
            'classifierMagicId': NotEmpty(),
            'bundleSettings': {'quantityLimit': {'minimum': 2, 'step': 3}},
            'supplier': {'name': NotEmpty(), 'gradesCount': 0},
            'shop': {'name': NotEmpty(), 'gradesCount': 0},
            'manufacturer': {'entity': 'manufacturer', 'warranty': True},
            'dimensions': ['11', '22', '33'],
            'weight': '1',
            'model': {'id': 6000, 'skuStats': {'totalCount': 1}},
        }
        short_offer_fields = {
            'categories': [merge_two_dicts(common_category_fields, short_category_fields)],
            'navnodes': [merge_two_dicts(common_navnode_fields, short_navnode_fields)],
            'cpc': Absent(),
            'description': Absent(),
            'vendor': {'name': Absent(), 'slug': Absent()},
            'shop_category_path': Absent(),
            'shop_category_path_ids': Absent(),
            'classifierMagicId': Absent(),
            'bundleSettings': Absent(),
            'supplier': {'name': Absent(), 'gradesCount': Absent()},
            'shop': {'name': Absent(), 'gradesCount': Absent()},
            'manufacturer': Absent(),
            'dimensions': Absent(),
            'weight': Absent(),
            'model': {'id': 6000},
        }

        common_model_fields = {
            'entity': 'product',
            'id': 6000,
            'modelCreator': 'market',
            'eligibleForBookingInUserRegion': False,
            'offers': {'count': 2},
            # reasonsToBuy должно остаться
            'reasonsToBuy': [{'factor_name': 'Хорошая модель'}],
            'opinions': 14,
            'reviews': 5,
            'preciseRating': 4,
            'promo': {'whitePromoCount': 0},
            'urls': {'direct': Contains('//market.yandex.ru/product/6000')},
            'titles': {'raw': 'HYPERID-6000'},
        }
        full_model_fields = {
            'navnodes': ElementCount(1),
            'categories': ElementCount(1),
            'vendor': {'entity': 'vendor'},
            'pictures': [{'entity': 'picture'}],
            'slug': 'hyperid-6000',
            'specs': {'internal': ElementCount(0)},
            'description': 'micro template',
            'fullDescription': 'full description 6000',
            'titlesWithoutVendor': {'raw': 'title not vendor 6000'},
            'modelName': {'raw': 'model name 6000'},
            'lingua': {'type': {}},
            'offers': {'items': [full_offer_fields]},
            'cpc': NotEmpty(),
            'showUid': NotEmpty(),
            'video': ['video 6000'],
            'skuPrices': {'min': '100', 'max': '100'},
        }
        short_model_fields = {
            'navnodes': Absent(),
            'categories': Absent(),
            'vendor': Absent(),
            'pictures': Absent(),
            'slug': '',
            'specs': Absent(),
            'description': '',
            'fullDescription': Absent(),
            'titlesWithoutVendor': Absent(),
            'modelName': Absent(),
            'lingua': Absent(),
            'offers': {'items': [short_offer_fields]},
            'cpc': Absent(),
            'showUid': '',
            'video': Absent(),
            'skuPrices': Absent(),
        }

        request_params = {
            'yandexuid': '700',
            'show-urls': 'direct',
            'show-models-specs': ['friendly', 'seo'],
            'add-sku-stats': '1',
            'dj-stats-source-policy': 'dyn_model_stat',
            'rearr-factors': 'prefer_do_with_sku=1;market_metadoc_search=no',
            'show-min-quantity': '1',
            'rgb': 'blue',  # для выдачи productInfo
        }
        request = 'place=dj&dj-place=base'
        for param, values in request_params.items():
            if isinstance(values, list):
                for v in values:
                    request += '&{}={}'.format(param, v)
            else:
                request += '&{}={}'.format(param, values)

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'results': [merge_two_dicts(common_model_fields, full_model_fields)]})

        response = self.report.request_json(request + '&rearr-factors=dj_short_output=1')
        self.assertFragmentIn(response, {'results': [merge_two_dicts(common_model_fields, short_model_fields)]})

    @classmethod
    def prepare_dj_place_map(cls):
        cls.dj.on_request(exp='place_map', yandexuid='700').respond(
            [
                DjModel(id=6000),
            ]
        )

    def test_dj_place_map(self):
        response = self.report.request_json(
            'place=dj&dj-place=null&yandexuid=700' '&rearr-factors=recom_dj_place_map=foo:bar,null:place_map'
        )
        self.assertFragmentIn(
            response, {'results': [{'entity': 'product', 'id': 6000}]}, preserve_order=True, allow_different_len=False
        )

    @classmethod
    def prepare_dj_cluster(cls):
        cls.dj.on_request(exp='cluster', yandexuid='700').respond(
            [
                DjModel(id=6000),
            ]
        )
        cls.fast_dj.on_request(exp='cluster', yandexuid='700').respond(
            [
                DjModel(id=6001),
            ]
        )

    @classmethod
    def prepare_dj_timeout(cls):
        cls.dj.on_request(exp='timeout', yandexuid='700').respond(
            [
                DjModel(id=6000),
            ]
        )

    def test_dj_timeout(self):
        def assert_timeout(expected, timeout_param=None, timeout_map=None):
            request = 'place=dj&dj-place=timeout&yandexuid=700'
            if timeout_param:
                request += '&dj-timeout={}'.format(timeout_param)
            if timeout_map:
                request += '&rearr-factors=recom_dj_timeout_map=' + ','.join(starmap('{}:{}'.format, timeout_map))
            request += '&waitall=0'  # CRAP: why it's not enough to set `wait=False`?
            request += '&debug=1'
            response = self.report.request_json(request, wait=False)
            self.assertFragmentIn(
                response,
                {'debug': {'report': {'logicTrace': [Contains('DJ request timeout: {} ms'.format(expected))]}}},
            )

        assert_timeout(expected=451, timeout_param=451)
        assert_timeout(expected=1984, timeout_param=451, timeout_map=[('timeout', 1984)])
        assert_timeout(expected=1984, timeout_map=[('timeout', 1984)])
        assert_timeout(expected=1984, timeout_map=[('timeout', 1983), ('timeout', 1984)])
        assert_timeout(expected=1984, timeout_map=[('foo', 42), ('timeout', 1984)])

    @classmethod
    def prepare_dj_hid_and_vendor_ids(cls):
        cls.dj.on_request(exp='hid_and_vendor_ids', yandexuid='700', hid='600', vendor_id='500').respond(
            [
                DjModel(id=6000),
            ]
        )

    def test_dj_hid_and_vendor_ids(self):
        response = self.report.request_json('place=dj&dj-place=hid_and_vendor_ids&yandexuid=700&hid=600&vendor_id=500')
        self.assertFragmentIn(
            response, {'results': [{'entity': 'product', 'id': 6000}]}, preserve_order=True, allow_different_len=False
        )

    @classmethod
    def prepare_dj_title(cls):
        cls.dj.on_request(exp='title', yandexuid='700').respond(
            [DjModel(id=6000)],
        )
        cls.dj.on_request(exp='title', yandexuid='701').respond([DjModel(6000)], title='We')

    def test_dj_title(self):
        response = self.report.request_json('place=dj&dj-place=title&yandexuid=700')
        self.assertFragmentNotIn(response, {'title': 'We'})
        response = self.report.request_json('place=dj&dj-place=title&yandexuid=701')
        self.assertFragmentIn(response, {'title': 'We'})

    @classmethod
    def prepare_dj_large_reply(cls):
        cls.dj.on_request(exp='large', yandexuid='700').respond([DjModel(id=m) for m in range(6000, 6050)])

    def test_dj_large_reply(self):
        response = self.report.request_json('place=dj&dj-place=large&yandexuid=700&numdoc=50')
        self.assertFragmentIn(
            response,
            {'results': [{'entity': 'product', 'id': m} for m in range(6000, 6050)]},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_metrics(cls):
        cls.dj.on_request(exp='metrics', yandexuid='700').respond(
            [
                DjModel(id=6000),
            ]
        )
        cls.dj.on_request(exp='metrics', yandexuid='701').respond([])

    def test_dj_metrics(self):
        def check_unistat_data(exp, yandexuid, error_expected):
            before = self.report.request_tass()
            self.report.request_json('place=dj&dj-place={}&yandexuid={}'.format(exp, yandexuid))
            after = self.report.request_tass_or_wait(wait_hole='dj_exp_{}_dj_request_time_hgram'.format(exp))

            self.assertEqual(
                before.get('dj_exp_{}_request_count_dmmm'.format(exp), 0) + 1,
                after.get('dj_exp_{}_request_count_dmmm'.format(exp), 0),
            )
            self.assertEqual(
                before.get('dj_exp_{}_error_count_dmmm'.format(exp), 0) + int(error_expected),
                after.get('dj_exp_{}_error_count_dmmm'.format(exp), 0),
            )
            self.assertIn('dj_exp_{}_request_time_hgram'.format(exp), after.keys())
            self.assertIn('dj_exp_{}_dj_request_time_hgram'.format(exp), after.keys())

        self.error_log.ignore(code=3787)

        check_unistat_data('metrics', 700, error_expected=False)
        check_unistat_data('metrics', 701, error_expected=False)
        check_unistat_data('null', 700, error_expected=True)

    @classmethod
    def prepare_dj_for_dj_paging(cls):
        cls.dj.on_request(
            exp='discovery_feed',
            djid='discovery_feed',
            yandexuid='753',
            page='1',
            view_unique_id='unique_id',
        ).respond(
            models=[DjModel(id=i) for i in FIRST_PAGE_MODEL_IDS],
            is_dj_paging_enabled=True,
        )
        cls.dj.on_request(
            exp='discovery_feed',
            djid='discovery_feed',
            yandexuid='753',
            page='2',
            view_unique_id='unique_id',
        ).respond(
            models=[DjModel(id=i) for i in SECOND_PAGE_MODEL_IDS],
            is_dj_paging_enabled=True,
        )
        cls.dj.on_request(
            exp='discovery_feed',
            djid='discovery_feed',
            yandexuid='753',
            page='3',
            view_unique_id='unique_id',
        ).respond(
            models=[DjModel(id=i) for i in THIRD_PAGE_MODEL_IDS],
            is_dj_paging_enabled=True,
        )
        cls.dj.on_request(
            exp='discovery_feed',
            djid='discovery_feed',
            yandexuid='753',
            page='4',
            view_unique_id='unique_id',
        ).respond(
            models=[],
            is_dj_paging_enabled=True,
        )

    def test_paging_on_dj_for_discovery_feed(self):
        # If dj with cache logic is working, report paging should be disabled, numdoc should be ignored. Use numdoc=10 to check it.
        common_params = 'place=dj&dj-place=this_dj_place_must_not_be_used&djid=discovery_feed&numdoc=10&view-unique-id=unique_id&yandexuid=753'

        response_first = self.report.request_json(common_params + '&page=1')
        response_second = self.report.request_json(common_params + '&page=2')
        response_third = self.report.request_json(common_params + '&page=3')
        response_fourth = self.report.request_json(common_params + '&page=4')

        self.assertFragmentIn(
            response_first,
            {'results': [{'entity': 'product', 'id': m} for m in FIRST_PAGE_MODEL_IDS]},
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response_second,
            {'results': [{'entity': 'product', 'id': m} for m in SECOND_PAGE_MODEL_IDS]},
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response_third,
            {'results': [{'entity': 'product', 'id': m} for m in THIRD_PAGE_MODEL_IDS]},
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response_fourth,
            {'results': []},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_for_dj_paging_over_session_page(cls):
        cls.dj.on_request(
            exp='fashion_feed',
            djid='fashion_feed',
            yandexuid='936',
            page='1',
            session_page_view_unique_id='session_page_view_unique_id_1',
            view_unique_id='view_unique_id_1',
            numdoc='2',
        ).respond(
            models=[DjModel(id=i) for i in FIRST_PAGE_MODEL_IDS],
            is_dj_paging_enabled=True,
        )
        cls.dj.on_request(
            exp='fashion_feed',
            djid='fashion_feed',
            yandexuid='936',
            page='2',
            session_page_view_unique_id='session_page_view_unique_id_1',
            view_unique_id='view_unique_id_2',
            numdoc='4',
        ).respond(
            models=[DjModel(id=i) for i in SECOND_PAGE_MODEL_IDS],
            is_dj_paging_enabled=True,
        )
        cls.dj.on_request(
            exp='fashion_feed',
            djid='fashion_feed',
            yandexuid='936',
            page='3',
            session_page_view_unique_id='session_page_view_unique_id_1',
            view_unique_id='view_unique_id_2',
            numdoc='30',
        ).respond(
            models=[DjModel(id=i) for i in THIRD_PAGE_MODEL_IDS],
            is_dj_paging_enabled=True,
        )
        cls.dj.on_request(
            exp='fashion_feed',
            djid='fashion_feed',
            yandexuid='936',
            page='4',
            session_page_view_unique_id='session_page_view_unique_id_1',
            view_unique_id='view_unique_id_2',
            numdoc='30',
        ).respond(
            models=[],
            is_dj_paging_enabled=True,
        )

    def test_paging_on_dj_for_fashion_feed(self):
        common_params = 'place=dj&dj-place=this_dj_place_must_not_be_used&djid=fashion_feed&session-page-view-unique-id=session_page_view_unique_id_1&yandexuid=936'

        response_first = self.report.request_json(common_params + '&page=1&view-unique-id=view_unique_id_1&numdoc=2')
        response_second = self.report.request_json(common_params + '&page=2&view-unique-id=view_unique_id_2&numdoc=4')
        response_third = self.report.request_json(common_params + '&page=3&view-unique-id=view_unique_id_2&numdoc=30')
        response_fourth = self.report.request_json(common_params + '&page=4&view-unique-id=view_unique_id_2&numdoc=30')

        self.assertFragmentIn(
            response_first,
            {'results': [{'entity': 'product', 'id': m} for m in FIRST_PAGE_MODEL_IDS]},
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response_second,
            {'results': [{'entity': 'product', 'id': m} for m in SECOND_PAGE_MODEL_IDS]},
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response_third,
            {'results': [{'entity': 'product', 'id': m} for m in THIRD_PAGE_MODEL_IDS]},
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response_fourth,
            {'results': []},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_for_no_paging(cls):
        cls.dj.on_request(exp='discovery_feed', djid='discovery_feed', yandexuid='755').respond(
            [DjModel(id=i) for i in ALL_PAGES_MODEL_IDS]
        )

    def test_paging_on_report_for_discovery_feed_disabled_by_default(self):
        # Disabled paging should ignore numdoc
        common_params = (
            'place=dj&dj-place=this_dj_place_must_not_be_used&djid=discovery_feed&view-unique-id=unique_id&yandexuid=755'
            '&rearr-factors=dj_ids_to_enable_paging_on_dj=discovery_feed=0&blue_attractive_models=1'
        )
        # Don't forget to disable paging on dj
        # Add an extra parameter (blue_attractive_models) to test parsing

        response_first = self.report.request_json(common_params + '&page=1')
        response_second = self.report.request_json(common_params + '&page=2')

        self.assertFragmentIn(
            response_first,
            {'results': [{'entity': 'product', 'id': m} for m in ALL_PAGES_MODEL_IDS]},
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response_second,
            {'results': []},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_paging_on_report_disabled_no_page_given(self):
        # Disabled paging should ignore numdoc
        common_params = (
            'place=dj&dj-place=this_dj_place_must_not_be_used&djid=discovery_feed&view-unique-id=unique_id&yandexuid=755'
            '&rearr-factors=dj_ids_to_enable_paging_on_dj=discovery_feed=0'
        )
        # Don't forget to disable paging on dj
        # Add an extra parameter (blue_attractive_models) to test parsing

        response_first = self.report.request_json(common_params)

        self.assertFragmentIn(
            response_first,
            {'results': [{'entity': 'product', 'id': m} for m in ALL_PAGES_MODEL_IDS]},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_for_numdoc_testing(cls):
        cls.dj.on_request(exp='retargeting_block', djid='retargeting_block', yandexuid='757').respond(
            [DjModel(id=i) for i in ALL_PAGES_MODEL_IDS]
        )

    def test_numdoc_works(self):
        response_for_numdoc = self.report.request_json(
            'place=dj&dj-place=this_dj_place_must_not_be_used&djid=retargeting_block&numdoc=30&yandexuid=757'
        )

        self.assertFragmentIn(
            response_for_numdoc,
            {'results': [{'entity': 'product', 'id': m} for m in FIRST_PAGE_MODEL_IDS]},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_with_extra_param(cls):
        cls.dj.on_request(
            exp='extra_params', yandexuid='1253', magic_constant='25', magic_sequence=':=-,&;a=b'
        ).respond([DjModel(id=6000)])

    def test_dj_with_extra_params(self):
        response = self.report.request_json(
            'debug=da&place=dj&dj-place=extra_params&yandexuid=1253&rearr-factors=recom_extra_params%3Dmagic-constant%3D25%26magic-sequence%3D:%253D-,%2526%253Ba%253Db'
        )
        self.assertFragmentIn(
            response, {'results': [{'entity': 'product', 'id': 6000}]}, preserve_order=True, allow_different_len=False
        )

    def _do_check_exploration_results(
        self,
        results,
        expected_primary_in_batch,
        expected_secondary_in_batch,
        result_len=None,
        check_global_ordering=False,
        check_local_ordering=False,
        primary_items=[],
        secondary_items=[],
        get_id=lambda i: int(i["id"]),
    ):
        batch_size = expected_primary_in_batch + expected_secondary_in_batch
        if result_len:
            self.assertEqual(len(results), result_len)
        self.assertEqual(len(results) % batch_size, 0)
        local_ordered = True
        global_ordered = True
        for i in range(0, len(results) // batch_size):
            batch = results[i * batch_size : (i + 1) * batch_size]
            n_primary = 0
            n_secondary = 0
            last_global_id = 0
            last_primary_id = 0
            last_secondary_id = 0
            for item in batch:
                ordered = last_global_id < get_id(item)
                if check_global_ordering:
                    self.assertTrue(ordered)
                last_global_id = get_id(item)
                global_ordered = global_ordered and ordered
                if get_id(item) in primary_items:
                    n_primary += 1
                    ordered = last_primary_id < get_id(item)
                    if check_local_ordering:
                        self.assertTrue(ordered)
                    last_primary_id = get_id(item)
                    local_ordered = local_ordered and ordered
                elif get_id(item) in secondary_items:
                    n_secondary += 1
                    ordered = last_secondary_id < get_id(item)
                    if check_local_ordering:
                        self.assertTrue(ordered)
                    last_secondary_id = get_id(item)
                    local_ordered = local_ordered and ordered
            self.assertEqual(n_primary, expected_primary_in_batch)
            self.assertEqual(n_secondary, expected_secondary_in_batch)
        return (global_ordered, local_ordered)

    @classmethod
    def prepare_dj_with_exploration(cls):
        cls.index.models += [Model(hyperid=i) for i in PRIMARY_PLACE_MODEL_IDS + SECONDARY_PLACE_MODEL_IDS]
        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid,
                sku=hyperid * 10 + 1,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            )
            for hyperid in PRIMARY_PLACE_MODEL_IDS + SECONDARY_PLACE_MODEL_IDS
        ]

        primary_models = [DjModel(id=i, title="primary#{}".format(i)) for i in PRIMARY_PLACE_MODEL_IDS]
        secondary_models = [DjModel(id=i, title="secondary#{}".format(i)) for i in SECONDARY_PLACE_MODEL_IDS]

        cls.dj.on_request(exp="primary_exp", yandexuid="001").respond(primary_models)
        cls.dj.on_request(exp="exploration_exp", yandexuid="001").respond(secondary_models)
        cls.dj.on_request(exp="primary_exp2", yandexuid="002").respond(primary_models[0:8])
        cls.dj.on_request(exp="exploration_exp", yandexuid="002").respond(secondary_models)
        cls.dj.on_request(exp="primary_exp", yandexuid="003").respond(primary_models)
        cls.dj.on_request(exp="exploration_exp", yandexuid="003").respond(secondary_models[0:2] + primary_models)

    def test_dj_with_exploration(self):
        def check_exploration_results(
            results,
            expected_primary_in_batch,
            expected_secondary_in_batch,
            result_len=None,
            check_global_ordering=False,
            check_local_ordering=False,
        ):
            return self._do_check_exploration_results(
                results,
                expected_primary_in_batch,
                expected_secondary_in_batch,
                result_len,
                check_global_ordering,
                check_local_ordering,
                PRIMARY_PLACE_MODEL_IDS,
                SECONDARY_PLACE_MODEL_IDS,
            )

        response = self.report.request_json(
            'place=dj&dj-place=primary_exp&yandexuid=001&debug=1&numdoc=4&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 6)

        response = self.report.request_json(
            'place=dj&dj-place=primary_exp&yandexuid=001&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 12)

        # not enough recommendatons for 4 batches
        response = self.report.request_json(
            'place=dj&dj-place=primary_exp&yandexuid=001&debug=1&numdoc=15&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 18)

        # yuid=002 has only 8 secondary recommendations
        # also yuid=002 has recommendations only in second experiment
        response = self.report.request_json(
            'place=dj&dj-place=primary_exp2&yandexuid=002&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 12)

        response = self.report.request_json(
            'place=dj&dj-place=primary_exp2&yandexuid=002&debug=1&numdoc=12&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 12)

        # yuid=003 has only 2 secondary results
        response = self.report.request_json(
            'place=dj&dj-place=primary_exp&yandexuid=003&debug=1&numdoc=4&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 6)

        response = self.report.request_json(
            'place=dj&dj-place=primary_exp&yandexuid=003&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 6)

        # check different batch size
        response = self.report.request_json(
            'place=dj&dj-place=primary_exp&yandexuid=001&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=2'
            '&rearr-factors=market_dj_exploration_secondary_size=1'
        )
        check_exploration_results(response.root["search"]["results"], 2, 1)

        # check different mixing policies
        response = self.report.request_json(
            'place=dj&dj-place=primary_exp&yandexuid=001&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_mixing_method=simple'
            '&rearr-factors=market_dj_exploration_primary_size=2'
            '&rearr-factors=market_dj_exploration_secondary_size=1'
        )
        (global_ordered, local_ordered) = check_exploration_results(
            response.root["search"]["results"], 2, 1, check_global_ordering=True, check_local_ordering=True
        )
        self.assertTrue(global_ordered)
        self.assertTrue(local_ordered)

        # check different mixing policies (max 5 retries to check that shuffle happened)
        total_global_ordered = True
        total_local_ordered = True
        for i in range(5):
            response = self.report.request_json(
                'place=dj&dj-place=primary_exp&yandexuid=001&debug=1&numdoc=8&page=1'
                '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
                '&rearr-factors=market_dj_exploration_mixing_method=random_position'
                '&rearr-factors=market_dj_exploration_primary_size=2'
                '&rearr-factors=market_dj_exploration_secondary_size=1'
            )
            (global_ordered, local_ordered) = check_exploration_results(
                response.root["search"]["results"], 2, 1, check_global_ordering=False, check_local_ordering=True
            )
            total_global_ordered = total_global_ordered and global_ordered
            total_local_ordered = total_local_ordered and local_ordered
            if (not total_global_ordered) and total_local_ordered:
                break
        self.assertTrue(not total_global_ordered)
        self.assertTrue(total_local_ordered)

        # check different mixing policies (max 5 retries to check that shuffle happened)
        total_global_ordered = True
        total_local_ordered = True
        for i in range(5):
            response = self.report.request_json(
                'place=dj&dj-place=primary_exp&yandexuid=001&debug=1&numdoc=8&page=1'
                '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
                '&rearr-factors=market_dj_exploration_mixing_method=random_shuffle'
                '&rearr-factors=market_dj_exploration_primary_size=2'
                '&rearr-factors=market_dj_exploration_secondary_size=1'
            )
            (global_ordered, local_ordered) = check_exploration_results(
                response.root["search"]["results"], 2, 1, check_global_ordering=False, check_local_ordering=False
            )
            total_global_ordered = total_global_ordered and global_ordered
            total_local_ordered = total_local_ordered and local_ordered
            if (not total_global_ordered) and (not total_local_ordered):
                break
        self.assertTrue(not total_global_ordered)
        self.assertTrue(not total_local_ordered)

    @classmethod
    def prepare_toloka(cls):
        cls.index.models += [Model(hyperid=i + 100) for i in TOLOKA_RECOMMENDATIONS]
        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid + 100,
                sku=(hyperid + 100) * 10 + 1,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            )
            for hyperid in TOLOKA_RECOMMENDATIONS
        ]

        recommendations = [
            DjModel(id=i + 100, title="recommendation#{}".format(i + 100)) for i in TOLOKA_RECOMMENDATIONS
        ]

        cls.dj.on_request(exp="regular_toloka", yandexuid="001").respond(recommendations)

    def test_toloka(self):
        response = self.report.request_json(
            'place=omm_toloka&yandexuid=001&debug=1&numdoc=10&page=1'
            '&rearr-factors=market_dj_exp_for_toloka=regular_toloka'
        )
        expected_results = [{'entity': 'product', 'id': i + 100} for i in TOLOKA_RECOMMENDATIONS[:10]]
        self.assertFragmentIn(response, {'results': expected_results}, preserve_order=True, allow_different_len=False)

    @classmethod
    def prepare_mixing_toloka(cls):
        cls.index.models += [Model(hyperid=i) for i in TOLOKA_RECOMMENDATIONS + TOLOKA_HONEYPOTS]
        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid,
                sku=hyperid * 10 + 1,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            )
            for hyperid in TOLOKA_RECOMMENDATIONS + TOLOKA_HONEYPOTS
        ]

        recommendations = [DjModel(id=i, title="recommendation#{}".format(i)) for i in TOLOKA_RECOMMENDATIONS]
        honeypots = [DjModel(id=i, title="honeypot#{}".format(i)) for i in TOLOKA_HONEYPOTS]

        cls.dj.on_request(exp="toloka_recommendations", yandexuid="001").respond(recommendations)
        cls.dj.on_request(exp="toloka_honeypots", yandexuid="001").respond(honeypots)
        cls.dj.on_request(exp="toloka_recommendations2", yandexuid="002").respond(recommendations[0:8] + honeypots)
        cls.dj.on_request(exp="toloka_honeypots", yandexuid="002").respond(honeypots)
        cls.dj.on_request(exp="toloka_recommendations", yandexuid="003").respond(recommendations)
        cls.dj.on_request(exp="toloka_honeypots", yandexuid="003").respond(honeypots[0:2])

    def test_mixing_toloka(self):
        def check_toloka_results(
            results,
            expected_primary_in_batch,
            expected_honeypots_in_batch,
            result_len=None,
            check_global_ordering=False,
            check_local_ordering=False,
        ):
            return self._do_check_exploration_results(
                results,
                expected_honeypots_in_batch,
                expected_primary_in_batch,
                result_len,
                check_global_ordering,
                check_local_ordering,
                TOLOKA_HONEYPOTS,
                TOLOKA_RECOMMENDATIONS,
            )

        response = self.report.request_json(
            'place=omm_toloka&yandexuid=001&debug=1&numdoc=4&page=1'
            '&rearr-factors=market_dj_exp_for_toloka=toloka_recommendations'
            '&rearr-factors=market_dj_exp_for_toloka_honeypots=toloka_honeypots'
        )
        check_toloka_results(response.root["search"]["results"], 4, 2, 6)

        response = self.report.request_json(
            'place=omm_toloka&yandexuid=001&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_toloka=toloka_recommendations'
            '&rearr-factors=market_dj_exp_for_toloka_honeypots=toloka_honeypots'
        )
        check_toloka_results(response.root["search"]["results"], 4, 2, 12)

        # not enough recommendatons for 4 batches
        response = self.report.request_json(
            'place=omm_toloka&yandexuid=001&debug=1&numdoc=15&page=1'
            '&rearr-factors=market_dj_exp_for_toloka=toloka_recommendations'
            '&rearr-factors=market_dj_exp_for_toloka_honeypots=toloka_honeypots'
        )
        check_toloka_results(response.root["search"]["results"], 4, 2, 18)

        # yuid=002 has only 8 non-honeypot recommendations
        # also yuid=002 has recommendations only in second experiment
        response = self.report.request_json(
            'place=omm_toloka&yandexuid=002&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_toloka=toloka_recommendations2'
            '&rearr-factors=market_dj_exp_for_toloka_honeypots=toloka_honeypots'
        )
        check_toloka_results(response.root["search"]["results"], 4, 2, 12)

        response = self.report.request_json(
            'place=omm_toloka&yandexuid=002&debug=1&numdoc=12&page=1'
            '&rearr-factors=market_dj_exp_for_toloka=toloka_recommendations2'
            '&rearr-factors=market_dj_exp_for_toloka_honeypots=toloka_honeypots'
        )
        check_toloka_results(response.root["search"]["results"], 4, 2, 12)

        # yuid=003 has only 2 honeypots
        response = self.report.request_json(
            'place=omm_toloka&yandexuid=003&debug=1&numdoc=4&page=1'
            '&rearr-factors=market_dj_exp_for_toloka=toloka_recommendations'
            '&rearr-factors=market_dj_exp_for_toloka_honeypots=toloka_honeypots'
        )
        check_toloka_results(response.root["search"]["results"], 4, 2, 6)

        response = self.report.request_json(
            'place=omm_toloka&yandexuid=003&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_toloka=toloka_recommendations'
            '&rearr-factors=market_dj_exp_for_toloka_honeypots=toloka_honeypots'
        )
        check_toloka_results(response.root["search"]["results"], 4, 2, 6)

        # check different batch size
        # note that honeypots are primaryResult for toloka... because of mixing logic
        response = self.report.request_json(
            'place=omm_toloka&yandexuid=001&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_toloka=toloka_recommendations'
            '&rearr-factors=market_dj_exp_for_toloka_honeypots=toloka_honeypots'
            '&rearr-factors=market_dj_exploration_primary_size=1'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_toloka_results(response.root["search"]["results"], 2, 1)

        # check different mixing policies
        response = self.report.request_json(
            'place=omm_toloka&yandexuid=001&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_toloka=toloka_recommendations'
            '&rearr-factors=market_dj_exp_for_toloka_honeypots=toloka_honeypots'
            '&rearr-factors=market_dj_exploration_mixing_method=simple'
            '&rearr-factors=market_dj_exploration_primary_size=1'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        (global_ordered, local_ordered) = check_toloka_results(
            response.root["search"]["results"], 2, 1, check_global_ordering=True, check_local_ordering=True
        )
        self.assertTrue(global_ordered)
        self.assertTrue(local_ordered)

        # check different mixing policies (max 5 retries to check that shuffle happened)
        total_global_ordered = True
        total_local_ordered = True
        for i in range(5):
            response = self.report.request_json(
                'place=omm_toloka&yandexuid=001&debug=1&numdoc=8&page=1'
                '&rearr-factors=market_dj_exp_for_toloka=toloka_recommendations'
                '&rearr-factors=market_dj_exp_for_toloka_honeypots=toloka_honeypots'
                '&rearr-factors=market_dj_exploration_mixing_method=random_position'
                '&rearr-factors=market_dj_exploration_primary_size=1'
                '&rearr-factors=market_dj_exploration_secondary_size=2'
            )
            (global_ordered, local_ordered) = check_toloka_results(
                response.root["search"]["results"], 2, 1, check_global_ordering=False, check_local_ordering=True
            )
            total_global_ordered = total_global_ordered and global_ordered
            total_local_ordered = total_local_ordered and local_ordered
            if (not total_global_ordered) and total_local_ordered:
                break
        self.assertTrue(not total_global_ordered)
        self.assertTrue(total_local_ordered)

        # check different mixing policies (max 5 retries to check that shuffle happened)
        total_global_ordered = True
        total_local_ordered = True
        for i in range(5):
            response = self.report.request_json(
                'place=omm_toloka&yandexuid=001&debug=1&numdoc=8&page=1'
                '&rearr-factors=market_dj_exp_for_toloka=toloka_recommendations'
                '&rearr-factors=market_dj_exp_for_toloka_honeypots=toloka_honeypots'
                '&rearr-factors=market_dj_exploration_mixing_method=random_shuffle'
                '&rearr-factors=market_dj_exploration_primary_size=1'
                '&rearr-factors=market_dj_exploration_secondary_size=2'
            )
            (global_ordered, local_ordered) = check_toloka_results(
                response.root["search"]["results"], 2, 1, check_global_ordering=False, check_local_ordering=False
            )
            total_global_ordered = total_global_ordered and global_ordered
            total_local_ordered = total_local_ordered and local_ordered
            if (not total_global_ordered) and (not total_local_ordered):
                break
        self.assertTrue(not total_global_ordered)
        self.assertTrue(not total_local_ordered)

    @classmethod
    def prepare_fake_items(cls):

        fake_items = [
            DjModel(
                id=i, title="Fake item", attributes={'nid': '{}'.format(i), 'is_fake': '', 'picture': 'picture_url'}
            )
            for i in TOLOKA_FAKE_ITEMS
        ]
        items_not_in_index = [
            DjModel(id=i, title="Fake item", attributes={'nid': '{}'.format(i), 'picture': 'picture_url'})
            for i in TOLOKA_FAKE_ITEMS
        ]

        cls.dj.on_request(exp="toloka_categorical_honeypots", yandexuid="001").respond(fake_items)
        cls.dj.on_request(exp="toloka_categorical_honeypots", yandexuid="002").respond(items_not_in_index)

    def test_fake_items(self):
        not_empty_response = self.report.request_json(
            'place=dj_links&dj-place=toloka_categorical_honeypots' '&yandexuid=001&debug=1&numdoc=4&page=1'
        )
        self.assertTrue(len(not_empty_response.root["search"]["results"]) != 0)

        empty_response = self.report.request_json(
            'place=dj_links&dj-place=toloka_categorical_honeypots' '&yandexuid=002&debug=1&numdoc=4&page=1'
        )
        self.assertTrue(len(empty_response.root["search"]["results"]) == 0)

    @classmethod
    def prepare_dj_morda_url(cls):

        cls.index.hypertree = [
            HyperCategory(
                hid=1000,
                name='A',
                children=[
                    HyperCategory(hid=600, name='AA'),
                    HyperCategory(hid=601, name='AB'),
                ],
            ),
        ]

        cls.index.navtree = [
            NavCategory(
                hid=1000,
                nid=11000,
                is_blue=True,
                name='NID_A',
                children=[
                    NavCategory(hid=600, nid=1600, is_blue=True, name='NID_AA'),
                    NavCategory(hid=601, nid=1601, is_blue=True, name='NID_AB'),
                ],
            ),
        ]

        cls.dj.on_request(exp='dj_morda', yandexuid='1253').respond(
            models=[DjModel(id=6000), DjModel(id=6001)],
            nid=1600,
            url='/catalog/1600/list',
            url_endpoint='catalog',
        )
        cls.dj.on_request(exp='dj_morda2', yandexuid='1253').respond(
            models=[DjModel(id=6000), DjModel(id=6001)],
            hid=600,
            url='/catalog/1600/list',
            url_endpoint='catalog',
        )
        cls.dj.on_request(exp='dj_morda3', yandexuid='1253').respond(
            models=[DjModel(id=6000), DjModel(id=6001)],
            nid=1600,
            glfilter='120:1;120:2',
            url='/catalog/1600/list',
            url_endpoint='catalog',
        )
        cls.dj.on_request(exp='dj_morda4', yandexuid='1253').respond(
            models=[DjModel(id=6000), DjModel(id=6001)],
            nid=1600,
            hid=600,
            glfilter='120:1',
            title='Custom Title 6000-6001',
            url='/catalog/1600/list',
            url_endpoint='catalog',
        )
        cls.dj.on_request(exp='dj_morda5', yandexuid='1253').respond(
            models=[DjModel(id=6010), DjModel(id=6011)],
            hid=601,
            url='/catalog/1601/list',
            url_endpoint='catalog',
        )
        cls.dj.on_request(exp='dj_morda6', yandexuid='1253').respond(
            models=[DjModel(id=6010), DjModel(id=6011)], title='Only Title'
        )
        cls.dj.on_request(exp='dj_morda7', yandexuid='1253').respond(
            models=[DjModel(id=6010), DjModel(id=6011)],
            nid=1601,
            url='/catalog/1601/list',
            thematic_id=1222333,
            range='range1',
            topic='topic1',
        )

    @classmethod
    def generate_report_state(cls, models, thematic_id=None, topic=None, range=None):
        rs = ReportState.create()
        for m in models:
            doc = rs.search_state.nailed_docs.add()
            doc.model_id = str(m)
        rs.search_state.nailed_docs_from_recom_morda = True
        if thematic_id is not None:
            rs.search_state.thematic_id = thematic_id
        if topic is not None:
            rs.search_state.topic = topic
        if range is not None:
            rs.search_state.range = range
        return ReportState.serialize(rs).replace('=', ',')

    def check_url(self, actual_url, expecetd_url):
        actual_url = urlparse.urlparse(actual_url)
        expecetd_url = urlparse.urlparse(expecetd_url)

        self.assertEqual(actual_url.path, expecetd_url.path)

        self.assertEqual(
            urlparse.parse_qs(actual_url.query),
            urlparse.parse_qs(expecetd_url.query),
        )

    def test_dj_morda_url(self):
        def _rs(thematic_id=None, topic=None, range=None, models=[]):
            return self.generate_report_state(models=models, thematic_id=thematic_id, topic=topic, range=range)

        test_cases = [
            {
                'djexp': 'dj_morda',
                'link': {
                    'url': '/catalog/1600/list?rs={rs}&tl=1'.format(rs=_rs(models=[6000, 6001])),
                    'urlEndpoint': 'catalog',
                    'params': {'nid': 1600, 'rs': _rs(models=[6000, 6001]), 'models': "6000,6001"},
                },
                'models': [6000, 6001],
            },
            {
                'djexp': 'dj_morda2',
                'link': {
                    'url': '/catalog/1600/list?rs={rs}&hid=600&tl=1'.format(rs=_rs(models=[6000, 6001])),
                    'urlEndpoint': 'catalog',
                    'params': {'hid': 600, 'rs': _rs(models=[6000, 6001]), 'models': "6000,6001"},
                },
                'models': [6000, 6001],
            },
            {
                'djexp': 'dj_morda3',
                'link': {
                    'url': '/catalog/1600/list?rs={rs}&tl=1&glfilter=120%3A1&glfilter=120%3A2'.format(
                        rs=_rs(models=[6000, 6001])
                    ),
                    'urlEndpoint': 'catalog',
                    'params': {
                        'nid': 1600,
                        'glfilter': '120:1;120:2',
                        'rs': _rs(models=[6000, 6001]),
                        'models': "6000,6001",
                    },
                },
                'models': [6000, 6001],
            },
            {
                'djexp': 'dj_morda4',
                'link': {
                    'url': '/catalog/1600/list?rs={rs}&tl=1&glfilter=120%3A1&hid=600'.format(
                        rs=_rs(models=[6000, 6001])
                    ),
                    'urlEndpoint': 'catalog',
                    'params': {
                        'nid': 1600,
                        'glfilter': '120:1',
                        'hid': 600,
                        'rs': _rs(models=[6000, 6001]),
                        'models': "6000,6001",
                    },
                },
                'title': 'Custom Title 6000-6001',
                'models': [6000, 6001],
            },
            {
                'djexp': 'dj_morda5',
                'link': {
                    'url': '/catalog/1601/list?rs={rs}&hid=601&tl=1'.format(rs=_rs(models=[6010, 6011])),
                    'urlEndpoint': 'catalog',
                    'params': {'hid': 601, 'rs': _rs(models=[6010, 6011]), 'models': "6010,6011"},
                },
                'models': [6010, 6011],
            },
            {
                'djexp': 'dj_morda6',
                'title': 'Only Title',
                'link': {
                    'urlEndpoint': NoKey('urlEndpoint'),
                    'params': {
                        'rs': _rs(models=[6010, 6011]),
                        'models': "6010,6011",
                    },
                },
                'models': [6010, 6011],
            },
            {
                'djexp': 'dj_morda7',
                'link': {
                    'url': '/catalog/1601/list?rs={rs}&tl=1'.format(
                        rs=_rs(models=[6010, 6011], thematic_id=1222333, topic='topic1', range='range1')
                    ),
                    'urlEndpoint': NoKey('urlEndpoint'),
                    'params': {'thematic_id': 1222333, 'topic': 'topic1', 'range': 'range1'},
                },
                'models': [6010, 6011],
            },
        ]

        for tcase in test_cases:
            response = self.report.request_json(
                'place=dj&dj-place={djexp}&yandexuid=1253&rearr-factors=market_pin_offers_to_rs=0'.format(
                    djexp=tcase['djexp']
                )
            )

            expected_link = NoKey('link')
            if 'link' in tcase:
                expected_link = {
                    'url': NotEmpty() if 'url' in tcase['link'] else NoKey('url'),
                    'urlEndpoint': tcase['link']['urlEndpoint'],
                    'params': tcase['link']['params'],
                }

            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': tcase['models'][0]},
                            {'entity': 'product', 'id': tcase['models'][1]},
                        ],
                        'link': expected_link,
                        'title': tcase.get('title', NoKey('title')),
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            if tcase.get('link', {}).get('url') is not None:
                self.check_url(response.root['search']['link']['url'], tcase['link']['url'])

    @classmethod
    def prepare_dj_links_toloka(cls):
        cls.index.models += [Model(hyperid=i, hid=i) for i in DJ_LINKS_RECOMMENDATIONS + DJ_LINKS_HONEYPOTS]
        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid,
                sku=hyperid * 10 + 1,
                blue_offers=[BlueOffer()],
            )
            for hyperid in DJ_LINKS_RECOMMENDATIONS + DJ_LINKS_HONEYPOTS
        ]
        cls.index.offers += [Offer(hyperid=hyperid) for hyperid in DJ_LINKS_RECOMMENDATIONS + DJ_LINKS_HONEYPOTS]
        cls.index.hypertree += [
            HyperCategory(hid=hyperid, name='HID_{}'.format(hyperid))
            for hyperid in DJ_LINKS_RECOMMENDATIONS + DJ_LINKS_HONEYPOTS
        ]
        cls.index.navtree += [
            NavCategory(
                nid=123,
                is_blue=True,
                children=[
                    NavCategory(hid=hyperid, nid=hyperid, is_blue=True, name='NID_{}'.format(hyperid))
                    for hyperid in DJ_LINKS_RECOMMENDATIONS + DJ_LINKS_HONEYPOTS
                ],
            ),
        ]

        primary_models = [
            DjModel(id=i, title="primary#{}".format(i), attributes={'hid': '{}'.format(i)})
            for i in DJ_LINKS_RECOMMENDATIONS
        ]
        secondary_models = [
            DjModel(id=i, title="secondary#{}".format(i), attributes={'hid': '{}'.format(i)})
            for i in DJ_LINKS_HONEYPOTS
        ]

        cls.dj.on_request(exp="primary_exp", yandexuid="1001").respond(primary_models)
        cls.dj.on_request(exp="exploration_exp", yandexuid="1001").respond(secondary_models)
        cls.dj.on_request(exp="primary_exp2", yandexuid="1002").respond(primary_models[0:8])
        cls.dj.on_request(exp="exploration_exp", yandexuid="1002").respond(secondary_models)
        cls.dj.on_request(exp="primary_exp", yandexuid="1003").respond(primary_models)
        cls.dj.on_request(exp="exploration_exp", yandexuid="1003").respond(secondary_models[0:2] + primary_models)

    def test_dj_links_toloka(self):
        def check_exploration_results(
            results,
            expected_primary_in_batch,
            expected_secondary_in_batch,
            result_len=None,
            check_global_ordering=False,
            check_local_ordering=False,
        ):
            return self._do_check_exploration_results(
                results,
                expected_primary_in_batch,
                expected_secondary_in_batch,
                result_len,
                check_global_ordering,
                check_local_ordering,
                DJ_LINKS_RECOMMENDATIONS,
                DJ_LINKS_HONEYPOTS,
                get_id=lambda res: int(res['link']['params']['hid']),
            )

        response = self.report.request_json(
            'place=dj_links&dj-place=primary_exp&yandexuid=1001&debug=1&numdoc=4&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 6)

        response = self.report.request_json(
            'place=dj_links&dj-place=primary_exp&yandexuid=1001&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 12)

        # not enough recommendatons for 4 batches
        response = self.report.request_json(
            'place=dj_links&dj-place=primary_exp&yandexuid=1001&debug=1&numdoc=15&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 18)

        # yuid=002 has only 8 secondary recommendations
        # also yuid=002 has recommendations only in second experiment
        response = self.report.request_json(
            'place=dj_links&dj-place=primary_exp2&yandexuid=1002&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 12)

        response = self.report.request_json(
            'place=dj_links&dj-place=primary_exp2&yandexuid=1002&debug=1&numdoc=12&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 12)

        # yuid=003 has only 2 secondary results
        response = self.report.request_json(
            'place=dj_links&dj-place=primary_exp&yandexuid=1003&debug=1&numdoc=4&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 6)

        response = self.report.request_json(
            'place=dj_links&dj-place=primary_exp&yandexuid=1003&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 6)

        # check different batch size
        response = self.report.request_json(
            'place=dj_links&dj-place=primary_exp&yandexuid=1001&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=2'
            '&rearr-factors=market_dj_exploration_secondary_size=1'
        )
        check_exploration_results(response.root["search"]["results"], 2, 1)

        # check different mixing policies
        response = self.report.request_json(
            'place=dj_links&dj-place=primary_exp&yandexuid=1001&debug=1&numdoc=8&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_mixing_method=simple'
            '&rearr-factors=market_dj_exploration_primary_size=2'
            '&rearr-factors=market_dj_exploration_secondary_size=1'
        )
        (global_ordered, local_ordered) = check_exploration_results(
            response.root["search"]["results"], 2, 1, check_global_ordering=True, check_local_ordering=True
        )
        self.assertTrue(global_ordered)
        self.assertTrue(local_ordered)

        # check different mixing policies (max 5 retries to check that shuffle happened)
        total_global_ordered = True
        total_local_ordered = True
        for i in range(5):
            response = self.report.request_json(
                'place=dj_links&dj-place=primary_exp&yandexuid=1001&debug=1&numdoc=8&page=1'
                '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
                '&rearr-factors=market_dj_exploration_mixing_method=random_position'
                '&rearr-factors=market_dj_exploration_primary_size=2'
                '&rearr-factors=market_dj_exploration_secondary_size=1'
            )
            (global_ordered, local_ordered) = check_exploration_results(
                response.root["search"]["results"], 2, 1, check_global_ordering=False, check_local_ordering=True
            )
            total_global_ordered = total_global_ordered and global_ordered
            total_local_ordered = total_local_ordered and local_ordered
            if (not total_global_ordered) and total_local_ordered:
                break
        self.assertTrue(not total_global_ordered)
        self.assertTrue(total_local_ordered)

        # check different mixing policies (max 5 retries to check that shuffle happened)
        total_global_ordered = True
        total_local_ordered = True
        for i in range(5):
            response = self.report.request_json(
                'place=dj_links&dj-place=primary_exp&yandexuid=1001&debug=1&numdoc=8&page=1'
                '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
                '&rearr-factors=market_dj_exploration_mixing_method=random_shuffle'
                '&rearr-factors=market_dj_exploration_primary_size=2'
                '&rearr-factors=market_dj_exploration_secondary_size=1'
            )
            (global_ordered, local_ordered) = check_exploration_results(
                response.root["search"]["results"], 2, 1, check_global_ordering=False, check_local_ordering=False
            )
            total_global_ordered = total_global_ordered and global_ordered
            total_local_ordered = total_local_ordered and local_ordered
            if (not total_global_ordered) and (not total_local_ordered):
                break
        self.assertTrue(not total_global_ordered)
        self.assertTrue(not total_local_ordered)

    @classmethod
    def prepare_pass_context(cls):
        cls.dj.on_request(exp='get_models', yandexuid='123').respond([DjModel(id=6000)])
        cls.dj.on_request(exp='get_models', yandexuid='123', context='SuperContext').respond([DjModel(id=6001)])

    def test_pass_context(self):
        response1 = self.report.request_json("place=dj&dj-place=get_models&yandexuid=123")
        self.assertFragmentIn(
            response1, {'results': [{'entity': 'product', 'id': 6000}]}, preserve_order=True, allow_different_len=False
        )

        response2 = self.report.request_json("place=dj&dj-place=get_models&yandexuid=123&recom-context=SuperContext")
        self.assertFragmentIn(
            response2, {'results': [{'entity': 'product', 'id': 6001}]}, preserve_order=True, allow_different_len=False
        )

    @classmethod
    def prepare_dj_links_toloka_with_fakes(cls):
        primary_models = [
            DjModel(id=i, title="primary#{}".format(i), attributes={'hid': '{}'.format(i), 'is_fake': ''})
            for i in DJ_LINKS_RECOMMENDATIONS
        ]
        secondary_models = [
            DjModel(id=i, title="secondary#{}".format(i), attributes={'hid': '{}'.format(i), 'is_fake': ''})
            for i in DJ_LINKS_HONEYPOTS
        ]

        cls.dj.on_request(exp="primary_exp", yandexuid="1011").respond(primary_models)
        cls.dj.on_request(exp="exploration_exp", yandexuid="1011").respond(secondary_models)

    def test_dj_links_toloka_with_fakes(self):
        def check_exploration_results(
            results,
            expected_primary_in_batch,
            expected_secondary_in_batch,
            result_len=None,
            check_global_ordering=False,
            check_local_ordering=False,
        ):
            return self._do_check_exploration_results(
                results,
                expected_primary_in_batch,
                expected_secondary_in_batch,
                result_len,
                check_global_ordering,
                check_local_ordering,
                DJ_LINKS_RECOMMENDATIONS,
                DJ_LINKS_HONEYPOTS,
                get_id=lambda res: int(res['link']['params']['hid']),
            )

        response = self.report.request_json(
            'place=dj_links&dj-place=primary_exp&yandexuid=1011&debug=1&numdoc=4&page=1'
            '&rearr-factors=market_dj_exp_for_exploration=exploration_exp'
            '&rearr-factors=market_dj_exploration_primary_size=4'
            '&rearr-factors=market_dj_exploration_secondary_size=2'
        )
        check_exploration_results(response.root["search"]["results"], 4, 2, 6)

    @classmethod
    def prepare_model_without_title_and_picture(cls):
        cls.dj.on_request(exp='no_titles_and_pictures', yandexuid='001').respond(
            models=[
                DjModel(id=2000201),
                DjModel(id=2000202),
                DjModel(id=2000203).clear_title().clear_picture(),
                DjModel(id=2000204).clear_title().clear_picture(),
                DjModel(id=2000205).clear_picture(),
                DjModel(id=2000206).clear_picture(),
                DjModel(id=2000207).clear_title(),
                DjModel(id=2000208).clear_title(),
            ]
        )

        cls.index.models += [
            Model(hyperid=2000201, title="Model 1 Report Title"),
            # no Model 2
            Model(hyperid=2000203, title="Model 3 Report Title"),
            # no Model 4
            Model(hyperid=2000205, title="Model 5 Report Title"),
            # no Model 6
            Model(hyperid=2000207, title="Model 7 Report Title"),
            # no Model 8
        ]
        modelIds = [2000201, 2000203, 2000205, 2000207]
        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid,
                sku=hyperid * 10 + 1,
                blue_offers=[BlueOffer()],
            )
            for hyperid in modelIds
        ]
        cls.index.offers += [Offer(hyperid=hyperid) for hyperid in modelIds]
        cls.index.hypertree += [HyperCategory(hid=hyperid, name='HID_{}'.format(hyperid)) for hyperid in modelIds]
        cls.index.navtree += [
            NavCategory(
                nid=123,
                is_blue=True,
                children=[
                    NavCategory(hid=hyperid, nid=hyperid, is_blue=True, name='NID_{}'.format(hyperid))
                    for hyperid in modelIds
                ],
            ),
        ]

    def test_model_without_title_and_picture(self):
        response = self.report.request_json("place=dj&dj-place=no_titles_and_pictures&yandexuid=001")
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'product', 'id': 2000201, 'titles': {'raw': 'Model 1 Report Title'}},
                    {'entity': 'product', 'id': 2000203, 'titles': {'raw': 'Model 3 Report Title'}},
                    {'entity': 'product', 'id': 2000205, 'titles': {'raw': 'Model 5 Report Title'}},
                    {'entity': 'product', 'id': 2000207, 'titles': {'raw': 'Model 7 Report Title'}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    TEST_DATA_CPB_YANDEXUID = '2000'
    TEST_DATA_CPB_MODELS_IDS_WITH_BIG_CASHBACK = []
    TEST_DATA_CPB_DJ_IDS = [
        'cashback_product_block',
        'blackfriday_cashback_product_block',
        'plus_cashback_product_block',
        'plus_home_cashback_product_block',
    ]
    TEST_DATA_CPB_OFFER_PRICE = 1000
    TEST_DATA_CPB_CASHBACK_PERCENT = 20
    TEST_DATA_CPB_CASHBACK_SHARE = TEST_DATA_CPB_CASHBACK_PERCENT / 100.0
    TEST_DATA_CPB_CASHBACK_AMOUNT = int(TEST_DATA_CPB_OFFER_PRICE * TEST_DATA_CPB_CASHBACK_SHARE)

    @classmethod
    def prepare_cashback_product_block(cls):
        MODELS_WITHOUT_CASHBACK_COUNT = 2
        MODELS_WITH_LOW_CASHBACK_COUNT = 2
        MODELS_WITH_CASHBACK_COUNT = MODELS_WITH_LOW_CASHBACK_COUNT + 3

        MODEL_BASE_ID_WITHOUT_CASHBACK = 3000000
        MODEL_BASE_ID_WITH_CASHBACK = MODEL_BASE_ID_WITHOUT_CASHBACK + MODELS_WITHOUT_CASHBACK_COUNT

        MODEL_IDS_WITHOUT_CASHBACK = list(
            range(MODEL_BASE_ID_WITHOUT_CASHBACK, MODEL_BASE_ID_WITHOUT_CASHBACK + MODELS_WITHOUT_CASHBACK_COUNT)
        )

        MODELS_IDS_WITH_CASHBACK = list(
            range(MODEL_BASE_ID_WITH_CASHBACK, MODEL_BASE_ID_WITH_CASHBACK + MODELS_WITH_CASHBACK_COUNT)
        )

        T.TEST_DATA_CPB_MODELS_IDS_WITH_BIG_CASHBACK = MODELS_IDS_WITH_CASHBACK[MODELS_WITH_LOW_CASHBACK_COUNT:]

        for djid in T.TEST_DATA_CPB_DJ_IDS:
            cls.dj.on_request(djid=djid, yandexuid=T.TEST_DATA_CPB_YANDEXUID).respond(
                models=[DjModel(id=x) for x in MODEL_IDS_WITHOUT_CASHBACK + MODELS_IDS_WITH_CASHBACK],
                blue_cashback_min_amount=100,
                blue_cashback_min_percent=T.TEST_DATA_CPB_CASHBACK_PERCENT,
            )

        cls.index.models += [
            Model(hyperid=x, title="Model {} without cashback".format(x)) for x in MODEL_IDS_WITHOUT_CASHBACK
        ]
        cls.index.models += [
            Model(hyperid=x, title="Model {} with cashback".format(x)) for x in MODELS_IDS_WITH_CASHBACK
        ]

        cls.index.mskus += [
            MarketSku(hyperid=model_id, sku=model_id, blue_offers=[BlueOffer()])
            for model_id in MODEL_IDS_WITHOUT_CASHBACK
        ]

        cls.index.offers += [Offer(hyperid=hyperid) for hyperid in MODEL_IDS_WITHOUT_CASHBACK]

        OFFERS_WITH_CASHBACK = [BlueOffer(price=T.TEST_DATA_CPB_OFFER_PRICE) for _ in MODELS_IDS_WITH_CASHBACK]

        MSKUS_WITH_CASHBACK = [
            MarketSku(
                sku=MODELS_IDS_WITH_CASHBACK[i],
                hyperid=MODELS_IDS_WITH_CASHBACK[i],
                blue_offers=[OFFERS_WITH_CASHBACK[i]],
            )
            for i in range(len(MODELS_IDS_WITH_CASHBACK))
        ]

        cls.index.mskus += MSKUS_WITH_CASHBACK

        # offers with low cashback value(less then 100) - must be rejected
        low_blue_cashback_promo = Promo(
            promo_type=PromoType.BLUE_CASHBACK,
            description='low_blue_cashback_description',
            key=b64url_md5(99),
            shop_promo_id='low_blue_cashback',
            blue_cashback=PromoBlueCashback(
                share=99.0 / T.TEST_DATA_CPB_OFFER_PRICE,  # calc share for 99 cashback value
                version=10,
                priority=3,
            ),
            offers_matching_rules=[
                OffersMatchingRules(mskus=MSKUS_WITH_CASHBACK[:MODELS_WITH_LOW_CASHBACK_COUNT]),
            ],
        )

        blue_cashback_promo = Promo(
            promo_type=PromoType.BLUE_CASHBACK,
            description='blue_cashback_description',
            key=b64url_md5(100),
            shop_promo_id='blue_cashback',
            blue_cashback=PromoBlueCashback(
                share=T.TEST_DATA_CPB_CASHBACK_SHARE,
                version=10,
                priority=3,
            ),
            offers_matching_rules=[
                OffersMatchingRules(mskus=MSKUS_WITH_CASHBACK[MODELS_WITH_LOW_CASHBACK_COUNT:]),
            ],
        )

        for offer in OFFERS_WITH_CASHBACK[:MODELS_WITH_LOW_CASHBACK_COUNT]:
            offer.promo = low_blue_cashback_promo
            offer.blue_promo_key = low_blue_cashback_promo.shop_promo_id

        for offer in OFFERS_WITH_CASHBACK[MODELS_WITH_LOW_CASHBACK_COUNT:]:
            offer.promo = blue_cashback_promo
            offer.blue_promo_key = blue_cashback_promo.shop_promo_id

    def test_cashback_product_block(self):
        for djid in T.TEST_DATA_CPB_DJ_IDS:
            response = self.report.request_json(
                "place=dj&dj-place=base&djid={}&perks=yandex_cashback"
                '&yandexuid={}'.format(djid, T.TEST_DATA_CPB_YANDEXUID)
            )

            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'product',
                            'id': model_id,
                            'offers': {
                                'items': [
                                    {
                                        'promoCollections': [
                                            {'id': 'cashback-value', 'info': {'value': T.TEST_DATA_CPB_CASHBACK_AMOUNT}}
                                        ]
                                    }
                                ]
                            },
                        }
                        for model_id in T.TEST_DATA_CPB_MODELS_IDS_WITH_BIG_CASHBACK
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

    TEST_DATA_PARENT_PROMO_YANDEXUID = '3000'
    TEST_DATA_PARENT_PROMO_MODELS_IDS = []
    TEST_DATA_PARENT_PROMO_ID = 'black_friday_parent_promo'

    @classmethod
    def prepare_product_retargeting_block(cls):
        model_id = 4000000
        model_id_with_parent_promo = model_id + 1

        T.TEST_DATA_PARENT_PROMO_MODELS_IDS += [model_id_with_parent_promo]

        cls.dj.on_request(djid='promo_retargeting_block', yandexuid=T.TEST_DATA_PARENT_PROMO_YANDEXUID).respond(
            models=[DjModel(id=x) for x in [model_id, model_id_with_parent_promo]]
        )

        cls.index.models += [
            Model(hyperid=model_id, title="Model {} without parent promo".format(model_id)),
            Model(
                hyperid=model_id_with_parent_promo,
                title="Model {} with parent promo".format(model_id_with_parent_promo),
            ),
        ]

        cls.index.mskus += [MarketSku(hyperid=model_id, sku=model_id, blue_offers=[BlueOffer(price=2000)])]

        cls.index.offers += [Offer(hyperid=model_id)]

        offer_with_parent_promo = BlueOffer(price=2000)

        msku_with_parent_promo = MarketSku(
            sku=model_id_with_parent_promo,
            hyperid=model_id_with_parent_promo,
            blue_offers=[offer_with_parent_promo],
        )

        cls.index.mskus += [msku_with_parent_promo]

        promo = Promo(
            promo_type=PromoType.PROMO_CODE,
            description='parent promo description',
            key=b64url_md5(300),
            parent_promo_id=T.TEST_DATA_PARENT_PROMO_ID,
            shop_promo_id='black_friday_promo',
            discount_value=10,
            offers_matching_rules=[
                OffersMatchingRules(mskus=[msku_with_parent_promo]),
            ],
        )

        offer_with_parent_promo.promo = [promo]
        offer_with_parent_promo.blue_promo_key = promo.shop_promo_id

    def test_promo_retargeting_block(self):
        response = self.report.request_json(
            'place=dj&dj-place=base&djid=promo_retargeting_block'
            '&parentPromoId={}&yandexuid={}'.format(T.TEST_DATA_PARENT_PROMO_ID, T.TEST_DATA_PARENT_PROMO_YANDEXUID)
        )

        self.assertFragmentIn(
            response,
            {'results': [{'entity': 'product', 'id': model_id} for model_id in T.TEST_DATA_PARENT_PROMO_MODELS_IDS]},
            preserve_order=True,
            allow_different_len=False,
        )

    TEST_DATA_BOOST_PARENT_PROMO_YANDEXUID = '4000'
    TEST_DATA_BOOST_PARENT_PROMO_MODELS_IDS = []
    TEST_DATA_BOOST_PARENT_PROMO_ID = 'black_monday_parent_promo'

    @classmethod
    def prepare_boost_parent_promo_traces(cls):
        model_id = 5000000
        model_id_with_parent_promo = model_id + 1
        model_id_with_parent_promo_finish = model_id + 2

        T.TEST_DATA_BOOST_PARENT_PROMO_MODELS_IDS += [
            model_id,
            model_id_with_parent_promo,
            model_id_with_parent_promo_finish,
        ]

        cls.dj.on_request(
            djid='landing_boost_promo_thematics_product_block', yandexuid=T.TEST_DATA_BOOST_PARENT_PROMO_YANDEXUID
        ).respond(
            models=[
                DjModel(id=x, attributes={'parent_promo_id': T.TEST_DATA_BOOST_PARENT_PROMO_ID})
                for x in [model_id, model_id_with_parent_promo, model_id_with_parent_promo_finish]
            ]
        )

        cls.index.models += [
            Model(hyperid=model_id, title="Model {} without parent promo".format(model_id)),
            Model(
                hyperid=model_id_with_parent_promo,
                title="Model {} with parent promo".format(model_id_with_parent_promo),
            ),
            Model(
                hyperid=model_id_with_parent_promo_finish,
                title="Model {} with finished parent promo".format(model_id_with_parent_promo_finish),
            ),
        ]

        cls.index.mskus += [MarketSku(hyperid=model_id, sku=model_id, blue_offers=[BlueOffer(price=2000)])]

        cls.index.offers += [Offer(hyperid=model_id)]

        offer_with_parent_promo = BlueOffer(price=2000)

        msku_with_parent_promo = MarketSku(
            sku=model_id_with_parent_promo,
            hyperid=model_id_with_parent_promo,
            blue_offers=[offer_with_parent_promo],
        )

        offer_with_parent_promo_finish = BlueOffer(price=2001)

        msku_with_parent_promo_finish = MarketSku(
            sku=model_id_with_parent_promo_finish,
            hyperid=model_id_with_parent_promo_finish,
            blue_offers=[offer_with_parent_promo_finish],
        )

        cls.index.mskus += [msku_with_parent_promo, msku_with_parent_promo_finish]

        promo = Promo(
            promo_type=PromoType.PROMO_CODE,
            description='parent promo description',
            key=b64url_md5(400),
            parent_promo_id=T.TEST_DATA_BOOST_PARENT_PROMO_ID,
            shop_promo_id='black_monday_promo',
            discount_value=10,
            offers_matching_rules=[
                OffersMatchingRules(mskus=[msku_with_parent_promo]),
            ],
        )

        promo_finish = Promo(
            promo_type=PromoType.PROMO_CODE,
            description='parent promo description',
            key=b64url_md5(401),
            parent_promo_id=T.TEST_DATA_BOOST_PARENT_PROMO_ID,
            shop_promo_id='black_monday_promo_finish',
            discount_value=10,
            start_date=datetime(2000, 1, 1),
            end_date=datetime(2001, 12, 31),
            offers_matching_rules=[
                OffersMatchingRules(mskus=[msku_with_parent_promo_finish]),
            ],
        )

        offer_with_parent_promo.promo = [promo]
        offer_with_parent_promo.blue_promo_key = promo.shop_promo_id
        offer_with_parent_promo_finish.promo = [promo_finish]
        offer_with_parent_promo_finish.blue_promo_key = promo_finish.shop_promo_id

    def test_boost_parent_promo_traces(self):
        response = self.report.request_json(
            'place=dj&dj-place=base&djid=landing_boost_promo_thematics_product_block'
            '&boostParentPromoId={}&yandexuid={}'.format(
                T.TEST_DATA_BOOST_PARENT_PROMO_ID, T.TEST_DATA_BOOST_PARENT_PROMO_YANDEXUID
            )
        )
        self.error_log.expect(
            'DJ model has same parent promo id as report \'{}\''.format(T.TEST_DATA_BOOST_PARENT_PROMO_ID)
        ).once()
        self.error_log.expect(
            'DJ model has parent promo id \'{}\' when report doesn\'t'.format(T.TEST_DATA_BOOST_PARENT_PROMO_ID)
        ).times(2)

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'product', 'id': model_id} for model_id in T.TEST_DATA_BOOST_PARENT_PROMO_MODELS_IDS
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_default_offer_filtration(cls):
        cls.index.models += [
            Model(hyperid=7000, title="Model without default offer"),
            Model(hyperid=7001, title="Ordinary model"),
        ]

        cls.index.offers += [Offer(hyperid=7000, price=100), Offer(hyperid=7001, price=100)]
        cls.dj.on_request(exp="custom", yandexuid="999").respond(
            [
                DjModel(id="7000"),
                DjModel(id="7001"),
            ]
        )

    def test_default_offer_filtration(self):
        response = self.report.request_json(
            'place=dj&dj-place=custom&yandexuid=999&rearr-factors=debug_rm_do_for_model=7000'
        )

        self.assertTrue(len(response.root["search"]["results"]) == 1)

    @classmethod
    def prepare_filter_by_base_price(cls):
        """
        Несколько промокодов одного типа не могут применяться. Если для оного оффера есть несколько
        подходящих к нему промокодов одного типа, то выберется тот из них, который имеет меньший приоритет.

        Поэтому мы готовим по одному промокоду каждого типа.
        """
        promo_code_250 = Promo(
            promo_type=PromoType.PROMO_CODE,
            discount_value=250,
            discount_currency='RUR',
            key=b64url_md5(250),
        )
        blue_cashback_25 = Promo(
            promo_type=PromoType.BLUE_CASHBACK,
            key=b64url_md5(25),
            blue_cashback=PromoBlueCashback(share=0.25, version=3, priority=3),
        )

        cls.index.promos += [
            promo_code_250,
            blue_cashback_25,
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=10001, sku=1000, title="Model 10001 NOT OK: ? < 1100", blue_offers=[BlueOffer(price=1100)]
            ),
            MarketSku(
                hyperid=100010, sku=1010, title="Model 10010 NOT OK: 1000 < 1100", blue_offers=[BlueOffer(price=1100)]
            ),
            MarketSku(hyperid=100011, sku=1011, title="Model 10011 OK: 1000 > 999", blue_offers=[BlueOffer(price=999)]),
            MarketSku(
                hyperid=100020,
                sku=1020,
                title="Model 10020 NOT OK: 1000 < 1100 (discount)",
                blue_offers=[BlueOffer(price=1100, price_old=1200)],
            ),
            MarketSku(
                hyperid=100021,
                sku=1021,
                title="Model 10021 OK: 1000 > 999 (discount)",
                blue_offers=[BlueOffer(price=999, price_old=1200)],
            ),
            MarketSku(
                hyperid=100030,
                sku=1030,
                title="Model 10030 NOT OK: 1000 < 1300 - 250 (promo_code)",
                blue_offers=[BlueOffer(price=1300, promo=[promo_code_250], blue_promo_key=[promo_code_250.key])],
            ),
            MarketSku(
                hyperid=100031,
                sku=1031,
                title="Model 10031 OK: 1000 > 1300 - 250 (promo_code)",
                blue_offers=[BlueOffer(price=1200, promo=[promo_code_250], blue_promo_key=[promo_code_250.key])],
            ),
            MarketSku(
                hyperid=100040,
                sku=1040,
                title="Model 10040 NOT OK: 1000 < 1400 - 350 (cashback 25%)",
                blue_offers=[BlueOffer(price=1400, promo=[blue_cashback_25], blue_promo_key=[blue_cashback_25.key])],
            ),
            MarketSku(
                hyperid=100041,
                sku=1041,
                title="Model 10041 OK: 1000 > 1200 - 300 (cashback 25%)",
                blue_offers=[BlueOffer(price=1200, promo=[blue_cashback_25], blue_promo_key=[blue_cashback_25.key])],
            ),
            MarketSku(
                hyperid=100050,
                sku=1050,
                title="Model 10050 NOT OK: 1000 < 1850 (discount) - 250 (promo_code) - 400 (cashback 25%)",
                blue_offers=[
                    BlueOffer(
                        price=1850,
                        price_old=2000,
                        promo=[promo_code_250, blue_cashback_25],
                        blue_promo_key=[promo_code_250.key, blue_cashback_25.key],
                    )
                ],
            ),
            MarketSku(
                hyperid=100051,
                sku=1051,
                title="Model 10051 OK: 1000 > 1450 (discount) - 250 (promo_code) - 300 (cashback 25%)",
                blue_offers=[
                    BlueOffer(
                        price=1450,
                        price_old=2000,
                        promo=[promo_code_250, blue_cashback_25],
                        blue_promo_key=[promo_code_250.key, blue_cashback_25.key],
                    )
                ],
            ),
        ]

        cls.dj.on_request(exp="profit_models_block", yandexuid=1).respond(
            [
                # нет аттрибута benefit_base_price, не выдаём
                DjModel(id="10001"),
                DjModel(id="100010", attributes={"benefit_base_price": "1000"}),
                DjModel(id="100011", attributes={"benefit_base_price": "1000"}),
                DjModel(id="100020", attributes={"benefit_base_price": "1000"}),
                DjModel(id="100021", attributes={"benefit_base_price": "1000"}),
                DjModel(id="100030", attributes={"benefit_base_price": "1000"}),
                DjModel(id="100031", attributes={"benefit_base_price": "1000"}),
                DjModel(id="100040", attributes={"benefit_base_price": "1000"}),
                DjModel(id="100041", attributes={"benefit_base_price": "1000"}),
                DjModel(id="100050", attributes={"benefit_base_price": "1000"}),
                DjModel(id="100051", attributes={"benefit_base_price": "1000"}),
            ],
            base_price_filter_enabled=True,
        )

    def test_filter_by_base_price(self):
        """Проверяем, что в репорте товары фильтруются по base_price из ответа dj с учётом положительности формулы:
        base_price - (offer_price + discount + promo_code + cashback)
        """
        response = self.report.request_json('place=dj&djid=profit_models_block&yandexuid=1&perks=yandex_cashback')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 100011},
                    {"entity": "product", "id": 100021},
                    {"entity": "product", "id": 100031},
                    {"entity": "product", "id": 100041},
                    {"entity": "product", "id": 100051},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_min_num_doc(self):
        """Проверяем, формируется пустой ответ, когда моделей меньше, чем min-num-doc"""
        response = self.report.request_json(
            'place=dj&djid=profit_models_block&yandexuid=1&perks=yandex_cashback&min-num-doc=10'
        )
        self.assertFragmentIn(response, {"results": EmptyList()}, allow_different_len=False)

    @classmethod
    def prepare_title_from_dj(cls):
        cls.dj.on_request(exp='test_custom_title', yandexuid='700').respond(title='custom_title')
        cls.dj.on_request(exp='test_empty_title', yandexuid='700').respond()

    def test_title_from_dj(self):
        for place in ['dj', 'dj_links']:
            response = self.report.request_json('place={}&djid=test_custom_title&yandexuid=700'.format(place))
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'title': 'custom_title',
                    }
                },
            )

            response = self.report.request_json('place={}&djid=test_empty_title&yandexuid=700'.format(place))
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'title': Absent(),
                    }
                },
            )


if __name__ == '__main__':
    main()
