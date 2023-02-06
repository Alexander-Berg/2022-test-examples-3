#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import BlueOffer, HyperCategory, HyperCategoryType, MarketSku, Model
from core.testcase import TestCase, main
from core.dj import DjModel
from core.types.autogen import b64url_md5
from core.matcher import NotEmpty

# key: djid, values: block title, default experiment, name of rearr param for experiment reload, name of specific djid param, dj model attributes
experiment_default_map = {
    'retargeting_block': (
        "На основе просмотров",
        "blue_attractive_models_no_fmcg",
        "dj_exp_retargeting_block",
        "dj_param_retargeting_block",
        {},
    ),
    'retargeting_block_with_history': (
        "На основе просмотров",
        "blue_attractive_models_with_history",
        "dj_exp_retargeting_block_with_history",
        "dj_param_retargeting_block_with_history",
        {},
    ),
    'thematics_product_block': (
        None,
        "market_thematics_from_context",
        "dj_exp_thematics_product_block",
        "dj_param_thematics_product_block",
        {},
    ),
    'thematics_category_block': (
        None,
        "market_thematics_from_context",
        "dj_exp_thematics_category_block",
        "dj_param_thematics_category_block",
        {},
    ),
    'department_page_thematics_product_block': (
        None,
        "market_department_page_thematics",
        "dj_exp_department_page_thematics_product_block",
        "dj_param_department_page_thematics_product_block",
        {},
    ),
    'department_page_thematics_category_block': (
        None,
        "market_department_page_thematics",
        "dj_exp_department_page_thematics_category_block",
        "dj_param_department_page_thematics_category_block",
        {},
    ),
    'model_page_thematics_product_block': (
        None,
        "market_model_page_thematics",
        "dj_exp_model_page_thematics_product_block",
        "dj_param_model_page_thematics_product_block",
        {},
    ),
    'model_page_thematics_category_block': (
        None,
        "market_model_page_thematics",
        "dj_exp_model_page_thematics_category_block",
        "dj_param_model_page_thematics_category_block",
        {},
    ),
    'model_card_thematics_block': (
        None,
        "market_model_card_thematics",
        "dj_exp_model_card_thematics_block",
        "dj_param_model_card_thematics_block",
        {},
    ),
    'catalog_thematics_block': (
        None,
        "market_catalog_thematics",
        "dj_exp_catalog_thematics_block",
        "dj_param_catalog_thematics_block",
        {},
    ),
    'recom_thematic_product_incut': (
        None,
        "thematic_incut",
        "dj_exp_recom_thematic_product_incut",
        "dj_param_recom_thematic_product_incut",
        {},
    ),
    'recom_thematic_category_incut': (
        None,
        "thematic_incut",
        "dj_exp_recom_thematic_category_incut",
        "dj_param_recom_thematic_category_incut",
        {},
    ),
    'history_block': (
        'История просмотров',
        'recent_findings_rtmr',
        "dj_exp_history_block",
        "dj_param_history_block",
        {},
    ),
    'history_by_category_block': (
        'История просмотров',
        'recent_findings_rtmr_category',
        "dj_exp_history_by_category_block",
        "dj_param_history_by_category_block",
        {},
    ),
    'history_mc_block': (
        'История просмотров',
        'recent_findings_rtmr_mc',
        "dj_exp_history_mc_block",
        "dj_param_history_mc_block",
        {},
    ),
    'discovery_block': (
        'Может заинтересовать',
        'discovery_block',
        "dj_exp_discovery_block",
        "dj_param_discovery_block",
        {},
    ),
    'popular_product_block': (
        "Популярное для вас",
        'popular_product_block',
        "dj_exp_popular_product_block",
        "dj_param_popular_product_block",
        {},
    ),
    'spontaneous_product_block': (
        "Выглядит любопытно",
        'spontaneous_product_block',
        "dj_exp_spontaneous_product_block",
        "dj_param_spontaneous_product_block",
        {},
    ),
    'similar_users_product_block': (
        "С похожими интересами смотрят",
        'similar_users_product_block',
        "dj_exp_similar_users_product_block",
        "dj_param_similar_users_product_block",
        {},
    ),
    'spontaneous_and_similar_users_product_block': (
        "Стоит приглядеться",
        'spontaneous_and_similar_users_product_block',
        "dj_exp_spontaneous_and_similar_users_product_block",
        "dj_param_spontaneous_and_similar_users_product_block",
        {},
    ),
    'cool_product_block': (
        "Выглядит любопытно",
        'cool_product_block',
        "dj_exp_cool_product_block",
        "dj_param_cool_product_block",
        {},
    ),
    'discovery_feed': (
        'Может заинтересовать',
        'discovery_feed',
        "dj_exp_discovery_feed",
        "dj_param_discovery_feed",
        {},
    ),
    'discovery_feed_mc': (
        'Может заинтересовать',
        'discovery_feed_mc',
        "dj_exp_discovery_feed_mc",
        "dj_param_discovery_feed_mc",
        {},
    ),
    'fmcg_block': ('Пора пополнить запасы', 'fmcg_main', "dj_exp_fmcg_block", "dj_param_fmcg_block", {}),
    'fmcg_cart_block': ('Пора пополнить запасы', 'fmcg_cart', "dj_exp_fmcg_cart_block", "dj_param_fmcg_cart_block", {}),
    'fmcg_block_special': (
        'Пора пополнить запасы',
        'fmcg_main_block_special',
        "dj_exp_fmcg_block_special",
        "dj_param_fmcg_block_special",
        {},
    ),
    'fmcg_block_repeat_purchases': (
        'Пора пополнить запасы',
        'fmcg_block_repeat_purchases',
        "dj_exp_fmcg_block_repeat_purchases",
        "dj_param_fmcg_block_repeat_purchases",
        {},
    ),
    'fmcg_cart_block_repeat_purchases': (
        'Пора пополнить запасы',
        'fmcg_cart_block_repeat_purchases',
        "dj_exp_fmcg_cart_block_repeat_purchases",
        "dj_param_fmcg_cart_block_repeat_purchases",
        {},
    ),
    'repeat_purchases_block': (
        'Пора пополнить запасы',
        'repeat_purchases_block',
        "dj_exp_repeat_purchases_block",
        "dj_param_repeat_purchases_block",
        {},
    ),
    'repeat_purchases_cart_block': (
        'Пора пополнить запасы',
        'repeat_purchases_cart_block',
        "dj_exp_repeat_purchases_cart_block",
        "dj_param_repeat_purchases_cart_block",
        {},
    ),
    'trend_goods_block': (
        'Сейчас в тренде',
        'trend_goods_block',
        "dj_exp_trend_goods_block",
        "dj_param_trend_goods_block",
        {},
    ),
    'cashback_product_block': (
        'Максимум кешбэка',
        'cashback_product_block',
        'dj_exp_cashback_product_block',
        'dj_param_cashback_product_block',
        {},
    ),
    'blackfriday_cashback_product_block': (
        'Максимум кешбэка',
        'blackfriday_cashback_product_block',
        'dj_exp_blackfriday_cashback_product_block',
        'dj_param_blackfriday_cashback_product_block',
        {},
    ),
    'plus_cashback_product_block': (
        'Максимум кешбэка',
        'plus_cashback_product_block',
        'dj_exp_plus_cashback_product_block',
        'dj_param_plus_cashback_product_block',
        {},
    ),
    'go_express_retargeting_block': (
        'На основе просмотров',
        'express_retargeting',
        'dj_exp_go_express_retargeting_block',
        'dj_param_go_express_retargeting_block',
        {},
    ),
    'most_relevant_category_block': (
        'Популярное в интересной категории',
        'relevant_category_goods',
        'dj_exp_most_relevant_category_block',
        'dj_param_most_relevant_category_block',
        {},
    ),
    'holidays': (None, 'market_thematics_from_context', "dj_exp_holidays", "dj_param_holidays", {}),
    'catalog_retargeting_feed': (
        'Популярные товары',
        'catalog_retargeting_feed',
        'dj_exp_catalog_retargeting_feed',
        'dj_param_catalog_retargeting_feed',
        {},
    ),
    'retargeting_landing': (
        "На основе просмотров",
        "landing_blue_attractive_models_no_fmcg",
        "dj_exp_retargeting_landing",
        "dj_param_retargeting_landing",
        {},
    ),
    'retargeting_with_history_landing': (
        "На основе просмотров",
        "landing_blue_attractive_models_with_history",
        "dj_exp_retargeting_with_history_landing",
        "dj_param_retargeting_with_history_landing",
        {},
    ),
    'history_landing': (
        'История просмотров',
        'landing_recent_findings_rtmr',
        "dj_exp_history_landing",
        "dj_param_history_landing",
        {},
    ),
    'history_by_category_landing': (
        'История просмотров',
        'landing_recent_findings_rtmr_category',
        "dj_exp_history_by_category_landing",
        "dj_param_history_by_category_landing",
        {},
    ),
    'history_mc_landing': (
        'История просмотров',
        'landing_recent_findings_rtmr_mc',
        "dj_exp_history_mc_landing",
        "dj_param_history_mc_landing",
        {},
    ),
    'discovery_landing': (
        'Может заинтересовать',
        'landing_discovery_block',
        "dj_exp_discovery_landing",
        "dj_param_discovery_landing",
        {},
    ),
    'popular_landing': (
        None,
        'landing_popular_product_block',
        "dj_exp_popular_landing",
        "dj_param_popular_landing",
        {},
    ),
    'spontaneous_landing': (
        None,
        'landing_spontaneous_product_block',
        "dj_exp_spontaneous_landing",
        "dj_param_spontaneous_landing",
        {},
    ),
    'similar_users_landing': (
        None,
        'landing_similar_users_product_block',
        "dj_exp_similar_users_landing",
        "dj_param_similar_users_landing",
        {},
    ),
    'spontaneous_and_similar_users_landing': (
        None,
        'landing_spontaneous_and_similar_users_product_block',
        "dj_exp_spontaneous_and_similar_users_landing",
        "dj_param_spontaneous_and_similar_users_landing",
        {},
    ),
    'cool_landing': (None, 'landing_cool_product_block', "dj_exp_cool_landing", "dj_param_cool_landing", {}),
    'fmcg_landing': ('Пора пополнить запасы', 'landing_fmcg_main', "dj_exp_fmcg_landing", "dj_param_fmcg_landing", {}),
    'fmcg_landing_repeat_purchases': (
        'Пора пополнить запасы',
        'fmcg_landing_repeat_purchases',
        "dj_exp_fmcg_landing_repeat_purchases",
        "dj_param_fmcg_landing_repeat_purchases",
        {},
    ),
    'fmcg_cart_landing_repeat_purchases': (
        'Пора пополнить запасы',
        'fmcg_cart_landing_repeat_purchases',
        "dj_exp_fmcg_cart_landing_repeat_purchases",
        "dj_param_fmcg_cart_landing_repeat_purchases",
        {},
    ),
    'repeat_purchases_landing': (
        'Пора пополнить запасы',
        'repeat_purchases_landing',
        "dj_exp_repeat_purchases_landing",
        "dj_param_repeat_purchases_landing",
        {},
    ),
    'repeat_purchases_cart_landing': (
        'Пора пополнить запасы',
        'repeat_purchases_cart_landing',
        "dj_exp_repeat_purchases_cart_landing",
        "dj_param_repeat_purchases_cart_landing",
        {},
    ),
    'fmcg_cart_landing': (
        'Пора пополнить запасы',
        'landing_fmcg_cart',
        "dj_exp_fmcg_cart_landing",
        "dj_param_fmcg_cart_landing",
        {},
    ),
    'trend_goods_landing': (
        'Сейчас в тренде',
        'landing_trend_goods_block',
        "dj_exp_trend_goods_landing",
        "dj_param_trend_goods_landing",
        {},
    ),
    'cashback_product_landing': (
        'Максимум кешбэка',
        'landing_cashback_product_block',
        'dj_exp_cashback_product_landing',
        'dj_param_cashback_product_landing',
        {},
    ),
    'blackfriday_cashback_product_landing': (
        'Максимум кешбэка',
        'landing_blackfriday_cashback_product_block',
        'dj_exp_blackfriday_cashback_product_landing',
        'dj_param_blackfriday_cashback_product_landing',
        {},
    ),
    'plus_cashback_product_landing': (
        'Максимум кешбэка',
        'landing_plus_cashback_product_block',
        'dj_exp_plus_cashback_product_landing',
        'dj_param_plus_cashback_product_landing',
        {},
    ),
    'promo_retargeting_landing': (
        None,
        'landing_promo_retargeting_block',
        'dj_exp_promo_retargeting_landing',
        'dj_param_promo_retargeting_landing',
        {},
    ),
    'most_relevant_category_landing': (
        'Популярное в интересной категории',
        'landing_relevant_category_goods',
        'dj_exp_most_relevant_category_landing',
        'dj_param_most_relevant_category_landing',
        {},
    ),
    'morda_promo_thematics_product_block': (
        None,
        "market_morda_thematics_promo",
        "dj_exp_morda_promo_thematics_product_block",
        "dj_param_morda_promo_thematics_product_block",
        {},
    ),
    'landing_promo_thematics_product_block': (
        None,
        "market_landing_thematics_promo",
        "dj_exp_landing_promo_thematics_product_block",
        "dj_param_landing_promo_thematics_product_block",
        {},
    ),
    'department_page_promo_thematics_product_block': (
        None,
        "market_department_page_thematics_promo",
        "dj_exp_department_page_promo_thematics_product_block",
        "dj_param_department_page_promo_thematics_product_block",
        {},
    ),
    'edadeal_promo_thematics_product_block': (
        None,
        "edadeal_promo_thematics_product_block",
        "dj_exp_edadeal_promo_thematics_product_block",
        "dj_param_edadeal_promo_thematics_product_block",
        {},
    ),
    'landing_promo_product_block': (
        None,
        "landing_promo_product_block",
        "dj_exp_landing_promo_product_block",
        "dj_param_landing_promo_product_block",
        {},
    ),
    'department_page_promo_product_block': (
        None,
        "department_page_promo_product_block",
        "dj_exp_department_page_promo_product_block",
        "dj_param_department_page_promo_product_block",
        {},
    ),
    'edadeal_block': (None, 'blue_attractive_models_edadeal', "dj_exp_edadeal_block", "dj_param_edadeal_block", {}),
    'edadeal_landing': (
        None,
        'blue_attractive_models_edadeal',
        "dj_exp_edadeal_landing",
        "dj_param_edadeal_landing",
        {},
    ),
    'edadeal_promo_retargeting_block': (
        None,
        'edadeal_promo_retargeting_block',
        'dj_exp_edadeal_promo_retargeting_block',
        'dj_param_edadeal_promo_retargeting_block',
        {},
    ),
    'newcomers_discovery_block': (
        "Может заинтересовать",
        'newcomers_discovery_block',
        "dj_exp_newcomers_discovery_block",
        "dj_param_newcomers_discovery_block",
        {},
    ),
    'newcomers_discovery_landing': (
        "Может заинтересовать",
        'landing_newcomers_discovery_block',
        "dj_exp_newcomers_discovery_landing",
        "dj_param_newcomers_discovery_landing",
        {},
    ),
    'newcomers_retargeting_block': (
        "На основе просмотров",
        'newcomers_blue_attractive_models_no_fmcg',
        "dj_exp_newcomers_retargeting_block",
        "dj_param_newcomers_retargeting_block",
        {},
    ),
    'newcomers_retargeting_landing': (
        "На основе просмотров",
        'landing_newcomers_blue_attractive_models_no_fmcg',
        "dj_exp_newcomers_retargeting_landing",
        "dj_param_newcomers_retargeting_landing",
        {},
    ),
    'profit_models_block': (
        'Выгодные товары',
        'category_profit_models_by_benefit',
        'dj_exp_profit_models_block',
        'dj_param_profit_models_block',
        {"benefit_base_price": "1000"},
    ),
    'games_block': ("Игры", 'games_block', "dj_exp_games_block", "dj_param_games_block", {}),
    'games_landing': ("Игры", 'games_block', "dj_exp_games_landing", "dj_param_games_landing", {}),
    'cart_complementary_block': (
        None,
        'cart_complementary_block',
        "dj_exp_cart_complementary_block",
        "dj_param_cart_complementary_block",
        {},
    ),
    'cart_complementary_landing': (
        None,
        'landing_cart_complementary_block',
        "dj_exp_cart_complementary_landing",
        "dj_param_cart_complementary_landing",
        {},
    ),
    'ecom_landing_thematics_product_block': (
        None,
        'ecom_landing_thematics',
        "dj_exp_ecom_landing_thematics_product_block",
        "dj_param_ecom_landing_thematics_product_block",
        {},
    ),
    'ecom_landing_thematics_category_block': (
        None,
        'ecom_landing_thematics',
        "dj_exp_ecom_landing_thematics_category_block",
        "dj_param_ecom_landing_thematics_category_block",
        {},
    ),
    'ecom_landing_discovery_block': (
        "Может заинтересовать",
        'ecom_landing_discovery_block',
        "dj_exp_ecom_landing_discovery_block",
        "dj_param_ecom_landing_discovery_block",
        {},
    ),
    'ecom_landing_discovery_landing': (
        "Может заинтересовать",
        'landing_ecom_landing_discovery_block',
        "dj_exp_ecom_landing_discovery_landing",
        "dj_param_ecom_landing_discovery_landing",
        {},
    ),
    'ecom_landing_retargeting_block': (
        "На основе просмотров",
        'ecom_landing_retargeting_block',
        "dj_exp_ecom_landing_retargeting_block",
        "dj_param_ecom_landing_retargeting_block",
        {},
    ),
    'ecom_landing_retargeting_landing': (
        "На основе просмотров",
        'landing_ecom_landing_retargeting_block',
        "dj_exp_ecom_landing_retargeting_landing",
        "dj_param_ecom_landing_retargeting_landing",
        {},
    ),
    'ecom_landing_promo_retargeting_block': (
        "На основе просмотров",
        'ecom_landing_promo_retargeting_block',
        "dj_exp_ecom_landing_promo_retargeting_block",
        "dj_param_ecom_landing_promo_retargeting_block",
        {},
    ),
    'ecom_landing_promo_retargeting_landing': (
        "На основе просмотров",
        'landing_ecom_landing_promo_retargeting_block',
        "dj_exp_ecom_landing_promo_retargeting_landing",
        "dj_param_ecom_landing_promo_retargeting_landing",
        {},
    ),
    'segment_landing_thematics_product_block': (
        None,
        'segment_landing_thematics_product_block',
        "dj_exp_segment_landing_thematics_product_block",
        "dj_param_segment_landing_thematics_product_block",
        {},
    ),
    'segment_landing_thematics_category_block': (
        None,
        'segment_landing_thematics_category_block',
        "dj_exp_segment_landing_thematics_category_block",
        "dj_param_segment_landing_thematics_category_block",
        {},
    ),
    'segment_landing_discovery_block': (
        "Может заинтересовать",
        'segment_landing_discovery_block',
        "dj_exp_segment_landing_discovery_block",
        "dj_param_segment_landing_discovery_block",
        {},
    ),
    'segment_landing_discovery_landing': (
        "Может заинтересовать",
        'landing_segment_landing_discovery_block',
        "dj_exp_segment_landing_discovery_landing",
        "dj_param_segment_landing_discovery_landing",
        {},
    ),
    'segment_landing_retargeting_block': (
        "На основе просмотров",
        'segment_landing_retargeting_block',
        "dj_exp_segment_landing_retargeting_block",
        "dj_param_segment_landing_retargeting_block",
        {},
    ),
    'segment_landing_retargeting_landing': (
        "На основе просмотров",
        'landing_segment_landing_retargeting_block',
        "dj_exp_segment_landing_retargeting_landing",
        "dj_param_segment_landing_retargeting_landing",
        {},
    ),
    'popular_products_department_like_catalog_retargeting_feed': (
        None,
        'popular_products_department_like_catalog_retargeting_feed',
        "dj_exp_popular_products_department_like_catalog_retargeting_feed",
        "dj_param_popular_products_department_like_catalog_retargeting_feed",
        {},
    ),
    'popular_products_department_like_catalog_retargeting_feed_landing': (
        None,
        'popular_products_department_like_catalog_retargeting_feed_landing',
        "dj_exp_popular_products_department_like_catalog_retargeting_feed_landing",
        "dj_param_popular_products_department_like_catalog_retargeting_feed_landing",
        {},
    ),
    'hardcoded_fashion_retargeting_block': (
        "Собери новогодний образ",
        'hardcoded_fashion_retargeting_block',
        "dj_exp_hardcoded_fashion_retargeting_block",
        "dj_param_hardcoded_fashion_retargeting_block",
        {},
    ),
    'hardcoded_fashion_retargeting_landing': (
        "Собери новогодний образ",
        'landing_hardcoded_fashion_retargeting_block',
        "dj_exp_hardcoded_fashion_retargeting_landing",
        "dj_param_hardcoded_fashion_retargeting_landing",
        {},
    ),
    'hardcoded_top_popular_retargeting_block': (
        "На основе просмотров",
        'hardcoded_top_popular_retargeting_block',
        "dj_exp_hardcoded_top_popular_retargeting_block",
        "dj_param_hardcoded_top_popular_retargeting_block",
        {},
    ),
    'hardcoded_top_popular_retargeting_landing': (
        "На основе просмотров",
        'landing_hardcoded_top_popular_retargeting_block',
        "dj_exp_hardcoded_top_popular_retargeting_landing",
        "dj_param_hardcoded_top_popular_retargeting_landing",
        {},
    ),
    'hardcoded_gifts_retargeting_block': (
        "На основе просмотров",
        'hardcoded_gifts_retargeting_block',
        "dj_exp_hardcoded_gifts_retargeting_block",
        "dj_param_hardcoded_gifts_retargeting_block",
        {},
    ),
    'hardcoded_gifts_retargeting_landing': (
        "На основе просмотров",
        'landing_hardcoded_gifts_retargeting_block',
        "dj_exp_hardcoded_gifts_retargeting_landing",
        "dj_param_hardcoded_gifts_retargeting_landing",
        {},
    ),
    'plus_home_retargeting_block': (
        "Может заинтересовать",
        'plus_home_retargeting_block',
        "dj_exp_plus_home_retargeting_block",
        "dj_param_plus_home_retargeting_block",
        {},
    ),
    'plus_home_cashback_product_block': (
        "Покупки с выгодой",
        'plus_home_cashback_product_block',
        "dj_exp_plus_home_cashback_product_block",
        "dj_param_plus_home_cashback_product_block",
        {},
    ),
}

ignore_search_result_for_id = (
    'cashback_product_block',
    'cashback_product_landing',
    'blackfriday_cashback_product_block',
    'blackfriday_cashback_product_landing',
    'plus_cashback_product_block',
    'plus_cashback_product_landing',
    'plus_home_cashback_product_block',
)


HID = 54321


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False

        cls.index.hypertree += [HyperCategory(hid=HID, output_type=HyperCategoryType.GURU)]

        blue_offers = [
            BlueOffer(offerid='shop_sku_' + str(id), feedid=id, waremd5=b64url_md5('blue-{}'.format(id)), price=100)
            for id in range(1, 9)
        ]

        # models
        cls.index.models += [
            Model(hyperid=1236, hid=HID),
            Model(hyperid=1237, hid=HID),
            Model(hyperid=1241, hid=HID),
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(title='dj_blue_market_sku_3', hyperid=1236, sku=11200003, blue_offers=[blue_offers[3]]),
            MarketSku(title='dj_blue_market_sku_4', hyperid=1237, sku=11200004, blue_offers=[blue_offers[4]]),
            MarketSku(title='dj_blue_market_sku6', hyperid=1241, sku=11200005, blue_offers=[blue_offers[7]]),
        ]

        def create_recommended_models(experiment):
            experiment_attributes = experiment[4]
            return [
                DjModel(id="1241", title='model#1241', attributes=experiment_attributes),
                DjModel(id="1237", title='model#1237', attributes=experiment_attributes),
                DjModel(id="1236", title='model#1236', attributes=experiment_attributes),
            ]

        for djid in experiment_default_map:
            cls.dj.on_request(exp=djid, yandexuid='702').respond(
                create_recommended_models(experiment_default_map[djid]), title='Title_from_dj'
            )
        cls.dj.on_request(exp="custom_experiment", yandexuid='702').respond(
            create_recommended_models(experiment_default_map[djid]), title='Title_from_dj'
        )

        cls.dj.on_request(exp="reloaded_experiment", yandexuid='702').respond(
            models=create_recommended_models(experiment_default_map[djid]), title='Reloaded_Experiment_Title'
        )

    def test_djid_default_experiment_mapping_title_from_dj(self):
        for place in ['dj', 'dj_links']:
            for param_name in ['dj-place', 'djid']:
                for djid in experiment_default_map:
                    response = self.report.request_json(
                        'place={report_place}&{param}={djid}&yandexuid=702'.format(
                            report_place=place, djid=djid, param=param_name
                        )
                    )
                    # TODO: metaplace->djid
                    self.assertFragmentIn(response, {'dj-meta-place': djid, 'dj-place': djid, 'title': 'Title_from_dj'})

            response = self.report.request_json(
                'place={report_place}&dj-place=custom_experiment&yandexuid=702'.format(report_place=place)
            )
            # TODO: metaplace->djid
            self.assertFragmentIn(
                response,
                {'dj-place': 'custom_experiment', 'dj-meta-place': 'custom_experiment', 'title': 'Title_from_dj'},
            )

    def test_djid_experiment_reload(self):
        for place in ['dj', 'dj_links']:
            for param_name in ['dj-place', 'djid']:
                for djid in experiment_default_map:
                    response = self.report.request_json(
                        'place={report_place}&{param}={djid}&yandexuid=702&rearr-factors=dj_exp_{djid}=reloaded_experiment'.format(
                            report_place=place,
                            djid=djid,
                            param=param_name,
                        )
                    )
                    # TODO: metaplace->djid
                    self.assertFragmentIn(
                        response,
                        {
                            'dj-meta-place': djid,
                            'dj-place': 'reloaded_experiment',
                            'title': 'Reloaded_Experiment_Title',
                        },
                    )

    @classmethod
    def prepare_pass_djid_to_dj(cls):
        cls.dj.on_request(exp="discovery_block", yandexuid='1848', djid="discovery_block").respond(
            [
                DjModel(id="1237", title='model#1237'),
                DjModel(id="1241", title='model#1241'),
            ]
        )

        cls.dj.on_request(exp="discovery_block", yandexuid='1848').respond(
            [
                DjModel(id="1241", title='model#1241'),
                DjModel(id="1237", title='model#1237'),
            ]
        )

    def test_pass_djid_to_dj(self):
        response = self.report.request_json("place=dj&djid=discovery_block&yandexuid=1848")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {'entity': 'product', 'id': 1237},
                    {'entity': 'product', 'id': 1241},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json("place=dj&dj-place=discovery_block&yandexuid=1848")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {'entity': 'product', 'id': 1237},
                    {'entity': 'product', 'id': 1241},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_djid_params(cls):
        cls.index.models += [
            Model(hyperid=1242, hid=54321),
        ]

        for djid in experiment_default_map:
            cls.dj.on_request(exp=djid, yandexuid='1848', custom1='1', custom2='2').respond(
                [
                    DjModel(id="1236", title='model#1236', attributes=experiment_default_map[djid][4]),
                    DjModel(id="1241", title='model#1241', attributes=experiment_default_map[djid][4]),
                ]
            )

    def test_djid_params(self):
        for djid in experiment_default_map:
            response = self.report.request_json(
                'place=dj&djid={djid}&yandexuid=1848&debug=1&rearr-factors={rearr}'.format(
                    djid=djid,
                    rearr='dj_prefix_{djid}_custom1=1;dj_prefix_{djid}_custom2=2'.format(djid=djid),
                )
            )
            if djid not in ignore_search_result_for_id:
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {'entity': 'product', 'id': 1236},
                            {'entity': 'product', 'id': 1241},
                        ]
                    },
                    preserve_order=True,
                    allow_different_len=False,
                )
            self.assertIn('&custom1=1&custom2=2', str(response))

    @classmethod
    def prepare_replacement_popular_products(cls):
        cls.dj.on_request(exp="catalog_retargeting_feed", yandexuid='1848').respond(
            [
                DjModel(id="1236", title='model#1236'),
                DjModel(id="1237", title='model#1237'),
            ]
        )

        cls.dj.on_request(exp="popular_products_like_catalog_retargeting_feed", yandexuid='1848').respond(
            [
                DjModel(id="1236", title='model#1236'),
                DjModel(id="1237", title='model#1237'),
            ]
        )

        product_offers_answer = {
            'models': ['1241'],
            'timestamps': ['1'],
        }
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1848').respond(product_offers_answer)

    def test_replacement_popular_products(self):
        """
        Проверяем, что place=popular_products в случае выставленного hid и cpa=real переключается на place=dj с
        djid=catalog_retargeting_feed
        Также, если hid не выставлен (при любом cpa), то происходит переключение на place=dj с
        djid=popular_products_like_catalog_retargeting_feed (выдача должна быть такой же)
        """

        req = 'place=popular_products&yandexuid=1848&debug=1'

        # переключение в catalog_retargeting_feed будет на любых остальных запросах
        response = self.report.request_json(req + '&cpa=real&hid={}'.format(HID))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'product', 'id': 1236},
                    {'entity': 'product', 'id': 1237},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(response, 'InitRecommendations(): dj id to be returned catalog_retargeting_feed')

        # с флагом switch_popular_products_to_dj=0, или без хида или без cpa=real замены не будет
        for suffix in (
            '&rearr-factors=switch_popular_products_to_dj=0&cpa=real&hid={}'.format(HID),
            '&cpa=real',
            '&hid={}'.format(HID),
        ):
            response = self.report.request_json(req + suffix)
            self.assertFragmentIn(response, {'results': NotEmpty()})
            self.assertFragmentNotIn(response, 'djid=catalog_retargeting_feed')

        # переключение в popular_products_like_catalog_retargeting_feed будет запросах
        for request_part in ["", "&cpa=real", "&hid={}".format(HID)]:
            response = self.report.request_json(req + request_part)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {'entity': 'product', 'id': 1236},
                        {'entity': 'product', 'id': 1237},
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )
            self.assertFragmentIn(
                response, 'InitRecommendations(): dj id to be returned popular_products_like_catalog_retargeting_feed'
            )

            # если только это поведение не выключено реарр-флагом
            response = self.report.request_json(
                req + request_part + "&rearr-factors=switch_popular_products_to_dj_no_nid_check=0"
            )
            self.assertFragmentIn(response, {'results': NotEmpty()})
            self.assertFragmentNotIn(response, 'djid=popular_products_like_catalog_retargeting_feed')


if __name__ == '__main__':
    main()
