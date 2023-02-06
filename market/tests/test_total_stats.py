# -*- coding: utf-8 -*-

import json

from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import OfferFlags, DisabledFlags


def read_total_stats(filename):
    stats = {}
    with open(filename) as f:
        for s in f:
            key, val = s.split(': ')
            stats[key] = int(val)
    return stats


class TestTotalStats(StatsCalcBaseTestCase):
    def setUp(self):
        super(TestTotalStats, self).setUp()

        self.gls = [
            {
                # белый офер без картинки
                'shop_id': 1,
                'feed_id': 18762,
                'binary_price': '12 1 0 RUR RUR',
                'flags': 0,
                'category_id': 91491,
                'model_id': 123,
                'url': 'https://market.yandex.ru/1',
            },
            {
                # белый офер с картинкой
                'shop_id': 1,
                'feed_id': 27795,
                'binary_price': '13 1 0 RUR RUR',
                'flags': 0,
                'category_id': 91491,
                'url': 'https://market.yandex.ru/2',
                'picture_url': 'ya.ru/1.jpg',
            },
            {
                # белый пуш оффер
                'shop_id': 1,
                'feed_id': 277997,
                'binary_price': '14 1 0 RUR RUR',
                'flags': OfferFlags.IS_PUSH_PARTNER.value,
            },
            {
                # синий офер
                'shop_id': 1,
                'feed_id': 30404,
                'binary_blue_oldprice': '15 1 0 RUR RUR',
                'binary_white_oldprice': '15 1 0 RUR RUR',
                'flags': OfferFlags.BLUE_OFFER.value,
                'is_blue_offer': True,
                'category_id': 91491,
                'url': 'https://market.yandex.ru/3',
                'delivery_bucket_ids': [121, 122, 123],
                'contex_info': {
                    'experiment_id': 'some_exp',
                    'experimental_msku_id': 2000,
                },
            },
            {
                # синий офер выключенный
                'shop_id': 1,
                'feed_id': 27795,
                'binary_blue_oldprice': '15 1 0 RUR RUR',
                'binary_white_oldprice': '15 1 0 RUR RUR',
                'flags': OfferFlags.BLUE_OFFER.value | OfferFlags.OFFER_HAS_GONE.value,
                'is_blue_offer': True,
                'category_id': 91491,
                'url': 'https://market.yandex.ru/3',
                'has_gone': True,
            },
            {
                # синий офер с ценой больше предельной
                'shop_id': 1,
                'feed_id': 27995,
                'flags': OfferFlags.BLUE_OFFER.value,
                'is_blue_offer': True,
                'category_id': 91491,
                'url': 'https://market.yandex.ru/32',
                'disabled_by_price_limit': True,
            },
            {
                # синий офер выключенный по стокам
                'shop_id': 1,
                'feed_id': 30404,
                'flags': OfferFlags.BLUE_OFFER.value,
                'is_blue_offer': True,
                'category_id': 91491,
                'url': 'https://market.yandex.ru/322',
                'disabled_flags': DisabledFlags.MARKET_STOCK.value,
            },
            {
                # синий офер с флагом OFFER_HAS_GONE, но с has_gone == False
                # OFFER_HAS_GONE - не используется для фильтрации синих офферов
                'shop_id': 1,
                'feed_id': 18762,
                'flags': OfferFlags.BLUE_OFFER.value | OfferFlags.OFFER_HAS_GONE.value,
                'is_blue_offer': True,
                'category_id': 91491,
                'url': 'https://market.yandex.ru/3222'
            },
            {
                # MSKU
                'shop_id': 1,
                'binary_price': '1 1 0 RUR RUR',
                'flags': OfferFlags.MARKET_SKU.value,
                'category_id': 91491,
                'url': 'https://market.yandex.ru/5',
            },
            {
                # синий офер buybox
                'shop_id': 1,
                'binary_blue_oldprice': '17 1 0 RUR RUR',
                'binary_white_oldprice': '17 1 0 RUR RUR',
                'flags': OfferFlags.BLUE_OFFER.value,
                'is_blue_offer': True,
                'is_buyboxes': True,
                'category_id': 91491,
                'url': 'https://market.yandex.ru/9',
            },

            {
                # клон синего офера
                'shop_id': 1,
                'feed_id': 30404,
                'binary_blue_oldprice': '15 1 0 RUR RUR',
                'binary_white_oldprice': '15 1 0 RUR RUR',
                'flags': OfferFlags.BLUE_OFFER.value,
                'is_blue_offer': True,
                'category_id': 91491,
                'url': 'https://market.yandex.ru/3',
                'delivery_bucket_ids': [121, 122, 123],
                'contex_info': {
                    'experiment_id': 'some_exp',
                    'original_msku_id': 1000,
                },
            },
            {
                # экспериментальная MSKU
                'shop_id': 1,
                'binary_price': '1 1 0 RUR RUR',
                'flags': OfferFlags.MARKET_SKU.value,
                'category_id': 91491,
                'url': 'https://market.yandex.ru/5',
                'contex_info': {
                    'experiment_id': 'some_exp',
                    'original_msku_id': 1000,
                },
            },
            {
                # оффер директа
                'shop_id': 1,
                'binary_price': '1 1 0 RUR RUR',
                'flags': OfferFlags.IS_DIRECT.value | OfferFlags.OFFER_HAS_GONE.value,
                'is_direct': True,
                'category_id': 91491,
                'url': 'https://sbermarket/5',
            },
            {
                # оффер Яндекс.Лавки
                'shop_id': 42,
                'flags': OfferFlags.IS_LAVKA | OfferFlags.DELIVERY,
                'category_id': 91491,
                'binary_price': '1 1 0 RUR RUR',
            },
            {
                # оффер Яндекс.Еды
                'shop_id': 43,
                'flags': OfferFlags.IS_EDA | OfferFlags.DELIVERY,
                'category_id': 91491,
                'binary_price': '1 1 0 RUR RUR',
            },
            {
                # DSBS оффер
                'shop_id': 1,
                'category_id': 91491,
                'binary_price': '1 1 0 RUR RUR',
                'is_dsbs': True,
            },
            {
                # оффер категории "Медицина"
                'shop_id': 1,
                'category_id': 15756919,
                'binary_price': '1 1 0 RUR RUR',
                'type': 10,  # Типы оффера https://a.yandex-team.ru/arc_vcs/market/proto/content/ir/Offer.proto?rev=b7fffced853f8f68bb42f86893e861b611b2ed5a#L28
            },
            {
                # оффер категории "Алкоголь"
                'shop_id': 1,
                'category_id': 16155476,
                'binary_price': '1 1 0 RUR RUR',
                'type': 13,  # Типы оффера https://a.yandex-team.ru/arc_vcs/market/proto/content/ir/Offer.proto?rev=b7fffced853f8f68bb42f86893e861b611b2ed5a#L28
            },
        ]

    def test_total_stats(self):
        self.run_stats_calc(
            'TotalStats',
            json.dumps(self.gls)
        )

        stats = read_total_stats(self.tmp_file_path('total-stats.txt'))
        actual = set(stats.iteritems())
        expected = set([
            ('num_offers', 15),
            ('num_offers_with_model', 1),
            ('num_blue_offers', 2),
            ('num_buybox_offers', 1),
            ('num_red_offers', 0),
            ('num_white_offers', 6),
            ('num_disabled_offers', 3),
            ('num_fake_msku_offers', 1),
            ('num_offers_with_picurl', 1),
            ('num_blue_offers_with_delivery', 1),
            ('num_push_offers', 1),
            ('num_white_push_offers', 1),
            # количество клонов офферов под экспериментальными MSKU
            # см. https://wiki.yandex-team.ru/market/report/infra/abtcontent
            ('num_cloned_msku_experimental_offers', 2),  # клон и экспериментальная MSKU
            # ('num_white_feeds', 2),
            # ('num_blue_feeds', 3),
            # TODO раскомментировать после https://st.yandex-team.ru/MARKETINDEXER-33936
            ('num_direct_offers', 1),
            ('num_lavka_offers', 1),
            ('num_eda_offers', 1),
            ('num_dsbs_offers', 1),
            ('num_medicine_offers', 1),
            ('num_alcohol_offers', 1)
        ])
        assert(expected.issubset(actual))
