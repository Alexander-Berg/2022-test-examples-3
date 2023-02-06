#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import LikeUrl
from core.testcase import TestCase, main
from core.types import Model, NoModelCategoryExp, Offer, Picture, VCluster
from core.types.picture import thumbnails_config


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.vclusters += [
            VCluster(
                hid=1,
                vclusterid=1000000101,
                title='Куртка',
                pictures=[
                    Picture(
                        picture_id='5zWKVjRQnIlvC0azzPFkJw',
                        width=100,
                        height=100,
                        thumb_mask=thumbnails_config.get_mask_by_names(['100x100']),
                        group_id=1234,
                    )
                ],
            ),
            VCluster(
                hid=2,
                vclusterid=1000000102,
                title='Джинсы',
                pictures=[
                    Picture(
                        picture_id='5zWKVjRQnIlvC0azzPFkJX',
                        width=100,
                        height=100,
                        thumb_mask=thumbnails_config.get_mask_by_names(['100x100']),
                        group_id=1234,
                    )
                ],
            ),
        ]

        cls.index.offers += [
            Offer(vclusterid=1000000101, title="Кожаная куртка", hid=1, fesh=15),
            Offer(vclusterid=1000000102, title="Синие джинсы", hid=2, fesh=15),
        ]

        cls.index.models += [
            Model(title='Красный айфон', hyperid=123, hid=1),
            Model(title='Золотой айфон', hyperid=124, hid=2),
        ]

        cls.index.offers += [
            Offer(hyperid=123, hid=1, title='Офер красного айфона', fesh=15, price=100),
            Offer(hyperid=124, hid=2, title='Офер золотого айфона', fesh=15, price=100),
        ]

        cls.index.no_models_categories_exp += [NoModelCategoryExp("exp1", 2)]

    def test_prime(self, aux=""):
        """
        Проверим, что если категория попала в эксперимент,
        то в ней будут находится офферы несмотря на то, что она
        визуальная
        """

        # вне эксперимента видим кластера/модели
        for text in ["", "&text=Куртка"]:
            response = self.report.request_json('place=prime&hid=1{}{}'.format(text, aux))
            self.assertFragmentIn(response, "Куртка")

        for text in ["", "&text=Красный"]:
            response = self.report.request_json('place=prime&hid=1{}{}'.format(text, aux))
            self.assertFragmentIn(response, "Красный айфон")

        # в эксперимента видим офферы, но не видим кластера/модели
        for text in ["", "&text=Джинсы"]:
            response = self.report.request_json(
                'place=prime&hid=2&rearr-factors=no_models_categories_exp=exp1{}{}'.format(text, aux)
            )
            self.assertFragmentIn(response, "Синие джинсы")
            self.assertFragmentNotIn(response, "Джинсы")

        for text in ["", "&text=Золотой"]:
            response = self.report.request_json(
                'place=prime&hid=2&rearr-factors=no_models_categories_exp=exp1{}{}'.format(text, aux)
            )
            self.assertFragmentIn(response, "Офер золотого айфона")
            self.assertFragmentNotIn(response, "Золотой айфон")

    def test_prime_collapsing(self):
        return self.test_prime('&allow-collapsing=1')

    def test_prime_nocollapsing(self):
        return self.test_prime('&allow-collapsing=0')

    def test_modelinfo(self):
        """
        Проверим, что place=modelinfo работает независимо от наличия эксперимента
        """
        for exp in ["", "&rearr-factors=no_models_categories_exp=exp1"]:
            for hid in ["", "&hid=1"]:
                response = self.report.request_json("place=modelinfo&hyperid=1000000101&rids=0{}{}".format(exp, hid))
                self.assertFragmentIn(response, "Куртка")

                response = self.report.request_json("place=modelinfo&hyperid=123&rids=0{}{}".format(exp, hid))
                self.assertFragmentIn(response, "Красный айфон")

        for exp in ["", "&rearr-factors=no_models_categories_exp=exp1"]:
            for hid in ["", "&hid=2"]:
                response = self.report.request_json("place=modelinfo&hyperid=1000000102&rids=0{}{}".format(exp, hid))
                self.assertFragmentIn(response, "Джинсы")

                response = self.report.request_json("place=modelinfo&hyperid=124&rids=0{}{}".format(exp, hid))
                self.assertFragmentIn(response, "Золотой айфон")

    def test_parallel(self):
        # вне эксперимента видим кластера/модели
        response = self.report.request_bs('place=parallel&text=Куртка&ignore-mn=1&ignore-all-filters-except-dynamic=1')
        self.assertFragmentIn(response, {"url": LikeUrl.of("//market.yandex.ru/product--kurtka/1000000101")})
        self.assertFragmentIn(response, {"market_offers_wizard": [{"offer_count": 1}]})

        response = self.report.request_bs('place=parallel&text=Джинсы&ignore-mn=1&ignore-all-filters-except-dynamic=1')
        self.assertFragmentIn(response, {"url": LikeUrl.of("//market.yandex.ru/product--dzhinsy/1000000102")})
        self.assertFragmentIn(response, {"market_offers_wizard": [{"offer_count": 1}]})

        response = self.report.request_bs('place=parallel&text=айфон&ignore-mn=1&ignore-all-filters-except-dynamic=1')
        self.assertFragmentIn(response, {"url": LikeUrl.of("//market.yandex.ru/product--krasnyi-aifon/123")})
        self.assertFragmentIn(response, {"url": LikeUrl.of("//market.yandex.ru/product--zolotoi-aifon/124")})
        self.assertFragmentIn(response, {"market_offers_wizard": [{"offer_count": 2}]})

        # в эксперимента видим офферы, но не видим кластера/модели
        response = self.report.request_bs(
            'place=parallel&text=Куртка&ignore-mn=1&ignore-all-filters-except-dynamic=1&rearr-factors=no_models_categories_exp=exp1'
        )
        self.assertFragmentIn(
            response, {"url": LikeUrl.of("//market.yandex.ru/product--kurtka/1000000101")}
        )  # категория hid=1 не участвует в эксперименте
        self.assertFragmentIn(response, {"market_offers_wizard": [{"offer_count": 1}]})

        response = self.report.request_bs(
            'place=parallel&text=Джинсы&ignore-mn=1&ignore-all-filters-except-dynamic=1&rearr-factors=no_models_categories_exp=exp1;market_parallel_use_collapsing=0'
        )
        self.assertFragmentNotIn(response, {"url": LikeUrl.of("//market.yandex.ru/product--dzhinsy/1000000102")})
        self.assertFragmentIn(response, {"market_offers_wizard": [{"offer_count": 1}]})

        response = self.report.request_bs(
            'place=parallel&text=айфон&ignore-mn=1&ignore-all-filters-except-dynamic=1&rearr-factors=no_models_categories_exp=exp1;market_parallel_use_collapsing=0'
        )
        self.assertFragmentIn(
            response, {"url": LikeUrl.of("//market.yandex.ru/product--krasnyi-aifon/123")}
        )  # категория hid=1 не участвует в эксперименте
        self.assertFragmentNotIn(response, {"url": LikeUrl.of("//market.yandex.ru/product--zolotoi-aifon/124")})
        self.assertFragmentIn(response, {"market_offers_wizard": [{"offer_count": 2}]})


if __name__ == '__main__':
    main()
