#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Book,
    CategoryOverallStatsRecord,
    HyperCategory,
    Model,
    Offer,
    OverallModel,
    Picture,
    PictureMbo,
    VCluster,
    WebErfEntry,
    WebErfFeatures,
    WebHerfEntry,
    WebHerfFeatures,
)
from core.matcher import Round
from core.types.picture import thumbnails_config
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_market_overall_model_statistics(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        # Заводим несколько моделей с заголовками, по которым их можно будет найти полнотекстовым поиском
        cls.index.models += [
            Model(title="Samsung Galaxy Note 7", hyperid=101),
            Model(title="Iphone 5s", hyperid=102),
        ]

        # Заводим для них общие статистики
        cls.index.overall_models += [
            OverallModel(hyperid=101, price_med=2.5),
            OverallModel(hyperid=102, price_med=0),
        ]

        # Заводим несколько офферов, приматченных к ранее заведённым моделям, которые т.ж. можно будет
        # найти полнотекстовым поиском. Заводим цены, нужные для некоторых факторов
        cls.index.offers += [
            Offer(title="white Samsung Galaxy Note 7", hyperid=101, waremd5='wgrU12_pd1mqJ6DJm_9nEA', price=1.25),
            Offer(title="exploding Samsung Galaxy Note 7", hyperid=101, waremd5='ZRK9Q9nKpuAsmQsKgmUtyg', price=5),
            Offer(title="yellow IPhone 5s", hyperid=102, waremd5='EUhIXt-nprRmCEEWR-cysw'),
        ]

    def test_market_overall_model_statistics(self):
        """
        Проверяем, что в feature-лог попадают значения факторов, представляющих
        собой общие (не региональные) статистики по модели
        """
        # Задаём запрос, по которому найдутся и модель, и офферы
        self.report.request_json('place=prime&text=Samsung+Galaxy+Note+7&show-urls=external')

        # Проверяем, что в логе присутствуют записи с корректными значениями нужных факторов, как для офферов,
        # так и для модели
        self.feature_log.expect(
            overall_median_model_price=2.5, offer_price_div_overall_median_model_price=1, model_id=101
        )

        self.feature_log.expect(
            overall_median_model_price=2.5,
            offer_price_div_overall_median_model_price=0.5,
            ware_md5='wgrU12_pd1mqJ6DJm_9nEA',
        )

        self.feature_log.expect(
            overall_median_model_price=2.5,
            offer_price_div_overall_median_model_price=2,
            ware_md5='ZRK9Q9nKpuAsmQsKgmUtyg',
        )

        # Задаём запрос, по которому найдутся и модель, и оффер
        self.report.request_json('place=prime&text=iphone+5s&show-urls=external')

        # Проверяем, что для нулевой медианы записи в лог ведутся корректно, пишутся ненулевые значения факторов
        # (нулевые писаться не должны)
        self.feature_log.expect(offer_price_div_overall_median_model_price=1, model_id=101)

        self.feature_log.expect(ware_md5='EUhIXt-nprRmCEEWR-cysw')

    @classmethod
    def prepare_market_overall_category_statistics(cls):
        # Заводим несколько категорий
        cls.index.hypertree += [
            HyperCategory(hid=1),
            HyperCategory(hid=2),
        ]

        # Заводим для них общие статистики
        cls.index.overall_category_stats += [
            CategoryOverallStatsRecord(hid=1, price_avg=0.5),
            CategoryOverallStatsRecord(hid=2, price_avg=1000),
        ]

        # Заводим модель, приматченную к одной из категорий...
        cls.index.models += [Model(title="Nike shoes", hyperid=104, hid=1)]

        # ...и оффер, приматченный к другой
        cls.index.offers += [Offer(title="Nike white coat", price=800, hid=2, waremd5='pM8jf4Sv4TytT4wS6ZljHw')]

    def test_market_overall_category_statistics(self):
        """
        Проверяем, что в feature-лог попадают значения факторов, представляющих
        собой общие (не региональные) статистики по категории
        """
        # Задаём запрос, по которому найдутся и модель, и оффер из категории
        _ = self.report.request_json('place=prime&text=Nike&show-urls=external')

        # Проверяем, что в логе присутствуют записи с корректными значениями нужных факторов, как для оффера,
        # так и для модели
        self.feature_log.expect(overall_avg_category_price=0.5, model_id=104)

        self.feature_log.expect(
            offer_price=800,
            overall_avg_category_price=1000,
            offer_price_div_overall_avg_category_price=Round(0.8, 2),
            ware_md5='pM8jf4Sv4TytT4wS6ZljHw',
        )

    @classmethod
    def prepare_web_features(cls):
        # Заводим офферы с урлами, к которым будут приматчены факторы
        cls.index.offers += [
            Offer(title='pylesos 1', url='http://eldorado.ru/p1', waremd5='x18c9f1gD9IWDI01en4Ouw'),
            Offer(title='pylesos 21', url='http://www.eldorado.ru/p2?q=1', waremd5='Alcm7_LNZnNZgLveMk8yaA'),
            Offer(title='pylesos 22', url='http://eldorado.ru/p2?q=2', waremd5='ab5-JEtYmUQWRHaWRkEuqw'),
            Offer(title='pylesos 3', url='https://e96.ru/ololo', waremd5='cxiwaDezlIE_I_hLT_h_vw'),
        ]

        # Заводим подокументные факторы для соответствующих урлов (в т.ч. варьируем http/https,
        # должно разрешиться на уровне indexerf_generator)
        cls.index.web_erf_features += [
            WebErfEntry(url='http://eldorado.ru/p1', features=WebErfFeatures(title_comm=1, f_title_idf_sum=0.5)),
            WebErfEntry(url='http://www.eldorado.ru/p2?q=1', features=WebErfFeatures(segment_aux_spaces_in_text=2)),
            WebErfEntry(url='http://eldorado.ru/p2?q=2', features=WebErfFeatures(title_comm=3, url_has_visits=1)),
            WebErfEntry(url='http://e96.ru/ololo', features=WebErfFeatures(dater_day=15, mtime=1000)),
        ]

        # Заводим похостовые факторы, заводим одни и те же значения для http/https, чтобы убедиться,
        # что, если есть оба, будет выбран более точный (опять же на уровне indexerf_generator)
        cls.index.web_herf_features += [
            WebHerfEntry(host='http://eldorado.ru', features=WebHerfFeatures(owner_enough_clicked=1)),
            WebHerfEntry(host='https://e96.ru', features=WebHerfFeatures(trash_adv=3)),
            WebHerfEntry(host='http://e96.ru', features=WebHerfFeatures(owner_enough_clicked=3)),
        ]

        # Заводим пару моделей
        cls.index.models += [Model(title="holodylnik 1", hyperid=201), Model(title="holodylnik 2", hyperid=202)]

        # Заводим для них соответствующие записи в подокументных и похостовых веб-факторах
        cls.index.web_erf_features += [
            WebErfEntry(
                url='https://market.yandex.ru/product/201',
                features=WebErfFeatures(dater_day=20, fem_and_mas_nouns_portion=4),
            ),
            WebErfEntry(url='market.yandex.ru/product/202', features=WebErfFeatures(title_comm=6)),
        ]

        cls.index.web_herf_features += [
            WebHerfEntry(host='https://market.yandex.ru', features=WebHerfFeatures(is_wikipedia=1, mascot_13=11))
        ]

    def test_web_features(self):
        """
        Проверяем, что в feature-лог попадают значения факторов, приехавших из веба в erf- и herf-индексах
        Смотрим т.ж., что, если нет экспериментальных erf/herf-факторов, независимо от флага
        market_use_exp_static_features всё работает и используются обычные факторы
        """

        for flag in ('', '&rearr-factors=market_use_exp_static_features=1'):
            # Задаём запрос, на который найдутся все офферы
            self.report.request_json('place=prime&text=pylesos&show-urls=external' + flag)

            # Проверяем, что каждому офферу соответствует запись в фича-логе с правильно поматченными
            # подокументными и похостовыми факторами
            self.feature_log.expect(
                title_comm=1, f_title_idf_sum=0.5, owner_enough_clicked=1, ware_md5='x18c9f1gD9IWDI01en4Ouw'
            )

            self.feature_log.expect(segment_aux_spaces_in_text=2, ware_md5='Alcm7_LNZnNZgLveMk8yaA')

            self.feature_log.expect(
                title_comm=3, url_has_visits=1, owner_enough_clicked=1, ware_md5='ab5-JEtYmUQWRHaWRkEuqw'
            )

            self.feature_log.expect(dater_day=15, mtime=1000, trash_adv=3, ware_md5='cxiwaDezlIE_I_hLT_h_vw')

            # То же самое для моделей
            self.report.request_json('place=prime&text=holodylnik&show-urls=external')

            self.feature_log.expect(
                dater_day=20, fem_and_mas_nouns_portion=4, is_wikipedia=1, mascot_13=11, model_id=201
            )

            self.feature_log.expect(title_comm=6, is_wikipedia=1, mascot_13=11, model_id=202)

    @classmethod
    def prepare_colorness(cls):
        cls.index.offers += [
            Offer(
                title='offer_with_colorness',
                picture=Picture(
                    width=100,
                    height=100,
                    thumb_mask=thumbnails_config.get_mask_by_names(['100x100']),
                    group_id=1234,
                    colorness=25,
                    colorness_avg=16,
                ),
                waremd5='Fwkg5VL6viNm8Hf7RcMcgg',
            )
        ]

        cls.index.books += [
            Book(
                title='book_with_colorness',
                picture=Picture(
                    width=100,
                    height=100,
                    thumb_mask=thumbnails_config.get_mask_by_names(['100x100']),
                    group_id=1234,
                    colorness=33,
                    colorness_avg=15,
                ),
                hyperid=303,
            )
        ]

        cls.index.models += [
            Model(
                title='model_with_colorness',
                proto_picture=PictureMbo(
                    '//avatars.mds.yandex.net/get-mpic/19381937/img_id93189317937193/orig',
                    width=200,
                    height=400,
                    colorness=56,
                    colorness_avg=40,
                ),
                hyperid=304,
            )
        ]

        cls.index.models += [
            Model(
                title='model_with_colorness_in_add_pictures',
                proto_add_pictures=[
                    PictureMbo(
                        '//avatars.mds.yandex.net/get-mpic/19381123/img_id931893179377890/orig',
                        width=200,
                        height=400,
                        colorness=42,
                        colorness_avg=30,
                    )
                ],
                hyperid=305,
            )
        ]

        cls.index.vclusters += [
            VCluster(
                title='cluster_with_colorness',
                vclusterid=1000000306,
                pictures=[
                    Picture(
                        width=100,
                        height=100,
                        thumb_mask=thumbnails_config.get_mask_by_names(['100x100']),
                        group_id=1234,
                        colorness=38,
                        colorness_avg=17,
                    )
                ],
            ),
        ]

    def test_colorness(self):
        self.report.request_json('place=prime&text=offer_with_colorness&show-urls=external')
        self.feature_log.expect(colorness=25, colorness_avg=16, ware_md5='Fwkg5VL6viNm8Hf7RcMcgg')

        self.report.request_json('place=prime&text=book_with_colorness&show-urls=external')
        self.feature_log.expect(colorness=33, colorness_avg=15, model_id=303)

        self.report.request_json('place=prime&text=model_with_colorness&show-urls=external')
        self.feature_log.expect(colorness=56, colorness_avg=40, model_id=304)

        self.report.request_json('place=prime&text=model_with_colorness_in_add_pictures&show-urls=external')
        self.feature_log.expect(colorness=42, colorness_avg=30, model_id=305)

        self.report.request_json('place=prime&modelid=1000000306&show-urls=external')
        self.feature_log.expect(colorness=38, colorness_avg=17, vcluster_id=1000000306)


if __name__ == '__main__':
    main()
