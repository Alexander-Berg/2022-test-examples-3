#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.bigb import (
    BigBKeyword,
    CategoryLastSeenEvent,
    CategoryViewEvent,
    MarketCategoryLastTimeCounter,
    MarketCategoryViewsCounter,
    MarketModelLastTimeCounter,
    MarketModelViewsCounter,
    ModelLastSeenEvent,
    ModelViewEvent,
    WeightedValue,
)
from core.matcher import Round
from core.testcase import main, TestCase
from core.types import CatMachineCategory, CatMachineFactor, HyperCategory, HyperCategoryType, Model, Offer
from core.report import REQUEST_TIMESTAMP
import math

DEFAULT_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]

OMM_COUNTER_HISTORYMARKETMODELID_VALUE = 'OMM_COUNTER_HISTORYMARKETMODELID_VALUE'
OMM_MARKET_ONLINE_USER_PERIOD_MODEL_UNSEEN = 'OMM_MARKET_ONLINE_USER_PERIOD_MODEL_UNSEEN'
OMM_MARKET_ONLINE_USER_PERIOD_CATEGORY_UNSEEN = 'OMM_MARKET_ONLINE_USER_PERIOD_CATEGORY_UNSEEN'
OMM_MARKET_ONLINE_USER_COUNT_CATEGORY_VIEW = 'OMM_MARKET_ONLINE_USER_COUNT_CATEGORY_VIEW'
OMM_MARKET_ONLINE_USER_RATIO_CATEGORY_VIEW = 'OMM_MARKET_ONLINE_USER_RATIO_CATEGORY_VIEW'

OMM_FACTORS = {
    OMM_COUNTER_HISTORYMARKETMODELID_VALUE: 371,
    OMM_MARKET_ONLINE_USER_PERIOD_MODEL_UNSEEN: 378,
    OMM_MARKET_ONLINE_USER_PERIOD_CATEGORY_UNSEEN: 379,
    OMM_MARKET_ONLINE_USER_COUNT_CATEGORY_VIEW: 385,
    OMM_MARKET_ONLINE_USER_RATIO_CATEGORY_VIEW: 388,
}

MODEL_VIEWS_COUNTER = MarketModelViewsCounter(
    model_view_events=[
        ModelViewEvent(model_id=1721801, view_count=1.5),
        ModelViewEvent(model_id=1721802, view_count=2.6),
    ]
)

MODEL_LAST_SEEN_COUNTER = MarketModelLastTimeCounter(
    model_view_events=[
        ModelLastSeenEvent(model_id=1721801, timestamp=REQUEST_TIMESTAMP - 4.2),
        ModelLastSeenEvent(model_id=1721802, timestamp=REQUEST_TIMESTAMP - 5.3),
    ]
)

CATEGORY_VIEWS_COUNTER = MarketCategoryViewsCounter(
    category_view_events=[
        CategoryViewEvent(category_id=1721800, view_count=3.5),
        CategoryViewEvent(category_id=1721700, view_count=3.5),
        CategoryViewEvent(category_id=1721600, view_count=3.5),
    ]
)

CATEGORY_LAST_SEEN_COUNTER = MarketCategoryLastTimeCounter(
    category_view_events=[
        CategoryLastSeenEvent(category_id=1721800, timestamp=REQUEST_TIMESTAMP - 6.4),
    ]
)


def get_value_for_id(counter, search_key):
    for key, value in zip(counter.keys, counter.values):
        if key == search_key:
            return value


def get_omm_report_factor_values(modelId):
    GLOBAL_CATEGORY_ID = 1721800
    return {
        OMM_COUNTER_HISTORYMARKETMODELID_VALUE: get_value_for_id(MODEL_VIEWS_COUNTER, modelId),
        OMM_MARKET_ONLINE_USER_PERIOD_MODEL_UNSEEN: REQUEST_TIMESTAMP
        - get_value_for_id(MODEL_LAST_SEEN_COUNTER, modelId),
        OMM_MARKET_ONLINE_USER_PERIOD_CATEGORY_UNSEEN: REQUEST_TIMESTAMP
        - get_value_for_id(CATEGORY_LAST_SEEN_COUNTER, GLOBAL_CATEGORY_ID),
        OMM_MARKET_ONLINE_USER_COUNT_CATEGORY_VIEW: math.floor(
            get_value_for_id(CATEGORY_VIEWS_COUNTER, GLOBAL_CATEGORY_ID)
        ),
        OMM_MARKET_ONLINE_USER_RATIO_CATEGORY_VIEW: (
            math.floor(get_value_for_id(CATEGORY_VIEWS_COUNTER, GLOBAL_CATEGORY_ID))
            / sum(map(math.floor, CATEGORY_VIEWS_COUNTER.values))
        ),
    }


BIGB_COUNTERS = [
    CATEGORY_LAST_SEEN_COUNTER,
    CATEGORY_VIEWS_COUNTER,
    MODEL_LAST_SEEN_COUNTER,
    MODEL_VIEWS_COUNTER,
]


class T(TestCase):
    @classmethod
    def prepare_omm(cls):
        cls.settings.omm_requests_enabled_on_prime = True
        cls.bigb.on_request(yandexuid='001', client='merch-machine').respond(
            keywords=DEFAULT_PROFILE, counters=BIGB_COUNTERS
        )

        cls.index.hypertree += [
            HyperCategory(hid=1721800, name='Телевизоры', output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(title='model 1', hyperid=1721801, hid=1721800),
            Model(title='model 2', hyperid=1721802, hid=1721800),
        ]

        cls.index.offers += [
            Offer(title='title N1', hyperid=1721801, discount=50),
            Offer(title='title N2', hyperid=1721802, discount=50),
        ]

    def test_without_omm_factors(self):
        """OMM факторы не должны использоваться под флагом.

        https://st.yandex-team.ru/MARKETRECOM-1943
        """

        def check_factors_value(mul, modelId):
            self.assertFragmentNotIn(
                response,
                {
                    'debug': {
                        'modelId': modelId,
                        'factors': {factor: Round(mul * value) for factor, value in OMM_FACTORS.items()},
                    }
                },
            )

        response = self.report.request_json(
            'text=model_title&place=prime&hid=1721800&reqid=777&yandexuid=001&numdoc=10&debug=da&allow-collapsing=1&rearr-factors=disable_omm_factors=1'
        )
        check_factors_value(1, 1721801)
        check_factors_value(2, 1721802)

    def test_report_omm_factors_replace_old(self):
        """Реализация OMM факторов в репорте.

        https://st.yandex-team.ru/MARKETRECOM-2361
        """

        def check_omm_report_factors_value(modelId):
            omm_factors = get_omm_report_factor_values(modelId)
            self.assertFragmentIn(
                response,
                {
                    'debug': {
                        'modelId': modelId,
                        'factors': {factor: Round(value) for factor, value in omm_factors.items()},
                    }
                },
            )

        response = self.report.request_json(
            'text=model_title&place=prime&hid=1721800&reqid=777&yandexuid=001&numdoc=10&debug=da&allow-collapsing=1'
        )
        for modelId in [1721801, 1721802]:
            check_omm_report_factors_value(modelId)

    @classmethod
    def prepare_cat_machine_factors(cls):
        cls.bigb.on_request(yandexuid='002', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.index.models += [
            Model(
                title='model all factors',
                hyperid=2239601,
                hid=2239600,
                cat_machine_factors=[CatMachineFactor(i, 1 + 0.01 * i) for i in range(CatMachineCategory.COUNT)],
            ),
            Model(
                title='model some factors',
                hyperid=2239602,
                hid=2239600,
                cat_machine_factors=[
                    CatMachineFactor(CatMachineCategory.INSTALLED_MOBILE_APPS, 0.42142),
                    CatMachineFactor(CatMachineCategory.PAGE_ID, 0.3259),
                ],
            ),
            Model(title='model no factors', hyperid=2239603, hid=2239600),
        ]

        cls.index.offers += [
            Offer(title='title N1', hyperid=2239601),
            Offer(title='title N2', hyperid=2239602),
            Offer(title='title N3', hyperid=2239603),
        ]

    def test_cat_machine_factors(self):
        """Без id пользователя не делаем запрос за факторами"""

        response = self.report.request_json(
            'place=prime&hid=2239600&numdoc=10&debug=da&allow-collapsing=1&yandexuid=001'
        )
        self.error_log.ignore(code=3757)
        self.assertFragmentIn(response, "Type: DayOfAWeek\\n    Ids: 1\\n    Tfs: 1\\n")
        self.assertFragmentIn(response, "Type: HourOfDay\\n    Ids: 0\\n    Tfs: 1\\n")

        self.feature_log.expect(
            catm_gender=Round(1.00),
            catm_age=Round(1.01),
            catm_income=Round(1.02),
            catm_visitgoal_metrica=Round(1.03),
            catm_visitgoal_metricaseg=Round(1.04),
            catm_visitgoal_auditorium=Round(1.05),
            catm_visitgoal_metricacounter=Round(1.06),
            catm_bm_categories=Round(1.07),
            catm_krypta_top_domains=Round(1.08),
            catm_krypta_adhoc_v3=Round(1.09),
            catm_installed_mobile_apps=Round(1.10),
            catm_dmp_segment_hashes=Round(1.11),
            catm_region_id=Round(1.12),
            catm_device_type_bt=Round(1.13),
            catm_device_model_hash=Round(1.14),
            catm_cart_offers=Round(1.15),
            catm_purchase_offers=Round(1.16),
            catm_detail_offers=Round(1.17),
            catm_cart_counter_ids=Round(1.18),
            catm_purchase_counter_ids=Round(1.19),
            catm_detail_counter_ids=Round(1.20),
            catm_shown_orders=Round(1.21),
            catm_shown_banners=Round(1.22),
            catm_affinity_sites=Round(1.23),
            catm_day_of_a_week=Round(1.24),
            catm_hour_of_day=Round(1.25),
            catm_new_long_term_interests=Round(1.26),
            catm_inner_segments=Round(1.27),
            catm_public_binary_segments=Round(1.28),
            catm_clicked_categories=Round(1.29),
            catm_page_id=Round(1.30),
            catm_page_url_hash=Round(1.31),
            catm_page_qtail_id=Round(1.32),
            catm_market_vendor_id=Round(1.33),
            catm_market_category_id=Round(1.34),
            catm_market_properties=Round(1.35),
            catm_visited_page_id=Round(1.36),
            catm_market_complementary_models=Round(1.37),
            catm_history_market_model_id=Round(1.38),
            catm_history_market_category_id=Round(1.39),
            catm_history_market_vendor_id=Round(1.4),
            catm_age_segments=Round(1.41),
            catm_income_segments=Round(1.42),
            catm_affinity_sites_weighted=Round(1.43),
            catm_actual_coordinates=Round(1.44),
            catm_regular_coordinates=Round(1.45),
            catm_visited_organizations=Round(1.46),
            catm_visited_domain_ids=Round(1.47),
            catm_short_term_interests=Round(1.48),
            catm_category_profiles=Round(1.49),
            catm_history_beru_view_model_id=Round(1.58),
            catm_history_beru_view_category_id=Round(1.59),
            catm_history_beru_view_vendor_id=Round(1.60),
            catm_history_beru_order_model_id=Round(1.61),
            catm_history_beru_order_category_id=Round(1.62),
            catm_history_beru_order_vendor_id=Round(1.63),
        ).times(1)

        self.feature_log.expect(
            catm_installed_mobile_apps=Round(0.42142, 5),
            catm_page_id=Round(0.3259, 4),
        )


if __name__ == '__main__':
    main()
