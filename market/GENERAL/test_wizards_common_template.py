#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import CardCategory, HyperCategory, HyperCategoryType, MnPlace, Model, Offer, Picture
from core.types.picture import thumbnails_config
from core.testcase import TestCase, main
from core.matcher import LikeUrl, NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):
        """Создаем модели и офферы с картинками для неявного и обычного колдунщиков."""
        cls.index.hypertree += [
            HyperCategory(hid=1, tovalid=1, name='CommonTemplate Category 1', output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(
                hyperid=11,
                ts=121,
                hid=1,
                title='CommonTemplate Model 1',
                picinfo='//avatars.mds.yandex.net/get-marketpic/1/CommonTemplate.1/orig#100#100',
            ),
            Model(
                hyperid=12,
                ts=122,
                hid=1,
                title='CommonTemplate Model 2',
                picinfo='//avatars.mds.yandex.net/get-marketpic/1/CommonTemplate.2/orig#100#100',
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 121).respond(1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 122).respond(0.9)

        cls.index.offers += [
            Offer(
                title='CommonTemplate 1',
                hyperid=11,
                hid=1,
                ts=111,
                picture=Picture(
                    group_id=1,
                    picture_id='CommonTemplateOffer1',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                ),
            ),
            Offer(
                title='CommonTemplate 2',
                hyperid=12,
                hid=1,
                ts=112,
                picture=Picture(
                    group_id=1,
                    picture_id='CommonTemplateOffer2',
                    width=100,
                    height=100,
                    thumb_mask=thumbnails_config.get_mask_by_names(['100x100']),
                ),
            ),
            Offer(title='CommonTemplate 3', ts=113),
            Offer(title='CommonTemplate 4', ts=114),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 111).respond(1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 112).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 113).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 114).respond(0.7)

        cls.index.cards += [
            CardCategory(hid=1, hyperids=[11]),
        ]

    def test_necessary_fields(self):
        """Проверяем новый формат колдунщиков под флагом market_parallel_common_template=1
        Проверяем новый формат, согласно тикету https://st.yandex-team.ru/MARKETOUT-25584
        Новый формат опредляется для офферных, неявных и модельно-офферных колдунщиков
        """
        request = (
            "place=parallel&text=CommonTemplate&rearr-factors="
            "market_enable_model_offers_wizard=1;"
            "market_enable_offers_wiz_center_incut=1;"
            "market_enable_offers_wiz_right_incut=1;"
            "market_enable_offers_adg_wiz=1;"
            "market_enable_implicit_model_wiz_center_incut=1;"
            "market_enable_implicit_model_adg_wiz=1;"
            "market_parallel_common_template=1;"
            "market_offers_incut_threshold=0;"
        )

        main_url_len = len("//market.yandex.ru?clid=...")
        necessary_fields = {
            "type": NotEmpty(),
            "subtype": NotEmpty(),
            "urls": {
                "mainUrl": LikeUrl(url_host="market.yandex.ru", url_params={"clid": NotEmpty()}, url_len=main_url_len),
                "searchUrl": LikeUrl(
                    url_host="market.yandex.ru", url_path="/search", url_params={"text": "commontemplate"}
                ),
            },
            "meta": {"_end": ""},
            "tech": {"_end": ""},
            "snippet": {"text": NotEmpty()},
            "showcase": NotEmpty(),
        }

        response = self.report.request_bs_pb(request)

        # Офферный
        self.assertFragmentIn(response, {"market_offers_wizard": necessary_fields})
        self.assertFragmentIn(response, {"market_offers_wizard_center_incut": necessary_fields})
        self.assertFragmentIn(response, {"market_offers_wizard_right_incut": necessary_fields})
        self.assertFragmentIn(response, {"market_offers_adg_wizard": necessary_fields})

        # Модельно офферный
        self.assertFragmentIn(response, {"market_model_offers_wizard": necessary_fields})

        # Неявный
        self.assertFragmentIn(response, {"market_implicit_model": necessary_fields})
        self.assertFragmentIn(response, {"market_implicit_model_adg_wizard": necessary_fields})
        self.assertFragmentIn(response, {"market_implicit_model_center_incut": necessary_fields})

    def test_sitelinks(self):
        """Проверяем новый формат сайтлинков под флагом market_parallel_common_template=1
        - должно находиться поле url, в котором для тача и деска ведут на разные версии маркета с разными прибитыми clid
        - должен быть hint
        - в обычном офферном её не будет, так как выпиливаем картинки
        """
        request = (
            'place=parallel&text=CommonTemplate&rearr-factors='
            'market_parallel_common_template=1;'
            'market_enable_model_offers_wizard=1;'
            'market_text_cpa_offers_wizard_enrichment=1;'
            'market_enable_offers_wiz_text_cpa=1;'
        )

        # для деска
        response = self.report.request_bs_pb(request + 'device=desktop;')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_text_cpa": {
                    "snippet": {
                        "sitelinks": [
                            {
                                "text": "Акции",
                                "url": LikeUrl(url_host="pokupki.market.yandex.ru", url_params={"clid": 545}),
                            }
                        ]
                    }
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "snippet": {
                        "sitelinks": [
                            {"text": "Отзывы", "url": LikeUrl(url_host="market.yandex.ru", url_params={"clid": 545})}
                        ]
                    }
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "snippet": {
                        "sitelinks": [
                            {"text": "Отзывы", "url": LikeUrl(url_host="market.yandex.ru", url_params={"clid": 698})}
                        ]
                    }
                }
            },
        )

        # для тача
        response = self.report.request_bs_pb(request + 'device=touch&touch=1')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard_text_cpa": {
                    "snippet": {
                        "sitelinks": [
                            {
                                "text": "Акции",
                                "url": LikeUrl(url_host="m.pokupki.market.yandex.ru", url_params={"clid": 708}),
                            }
                        ]
                    }
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "snippet": {
                        "sitelinks": [
                            {"text": "Отзывы", "url": LikeUrl(url_host="m.market.yandex.ru", url_params={"clid": 708})}
                        ]
                    }
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "snippet": {
                        "sitelinks": [
                            {"text": "Отзывы", "url": LikeUrl(url_host="m.market.yandex.ru", url_params={"clid": 721})}
                        ]
                    }
                }
            },
        )

    def test_snippet_images(self):
        """Проверяем новый формат сайтлинков под флагом market_parallel_common_template=1
        - есть поле с картинкой и картинкой в hd формате
        """
        request = 'place=parallel&text=CommonTemplate&rearr-factors=market_parallel_common_template=1;'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "snippet": {
                        "image": "//avatars.mdst.yandex.net/get-marketpic/1/market_CommonTemplateOffer1/100x100",
                        "imageHd": "//avatars.mdst.yandex.net/get-marketpic/1/market_CommonTemplateOffer1/200x200",
                    }
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "snippet": {
                        "image": "//avatars.mds.yandex.net/get-marketpic/1/CommonTemplate.1/100x100",
                        "imageHd": "//avatars.mds.yandex.net/get-marketpic/1/CommonTemplate.1/200x200",
                    }
                }
            },
        )

    def test_showcase_necessary_fields(self):
        """Проверяем новый формат showcase item у колдунщиков под флагом market_parallel_common_showcase_template=1
        Также надо передавать флаг market_parallel_common_template=1, так как новый формат врезки работает только для common шаблона
        Проверяем новый формат showcase item, согласно тикету https://st.yandex-team.ru/MARKETOUT-25584
        Новый формат опредляется для офферных, неявных и модельно-офферных колдунщиков
        """
        request = (
            "place=parallel&text=CommonTemplate&rearr-factors="
            "market_enable_model_offers_wizard=1;"
            "market_enable_offers_wiz_center_incut=1;"
            "market_enable_offers_wiz_right_incut=1;"
            "market_enable_offers_adg_wiz=1;"
            "market_enable_implicit_model_wiz_center_incut=1;"
            "market_enable_implicit_model_adg_wiz=1;"
            "market_offers_incut_threshold=0;"
            "market_parallel_common_template=1;"
            "market_parallel_common_showcase_template=1;"
        )
        necessary_fields = {
            "meta": {
                "currency": NotEmpty(),
            },
            "showcase": {
                "items": [
                    {
                        "meta": NotEmpty(),
                        "tech": NotEmpty(),
                        "urls": NotEmpty(),
                    }
                ]
            },
        }
        response = self.report.request_bs_pb(request)

        # Офферный
        self.assertFragmentIn(response, {"market_offers_wizard": necessary_fields})
        self.assertFragmentIn(response, {"market_offers_wizard_center_incut": necessary_fields})
        self.assertFragmentIn(response, {"market_offers_wizard_right_incut": necessary_fields})
        self.assertFragmentIn(response, {"market_offers_adg_wizard": necessary_fields})

        # Модельно офферный
        self.assertFragmentIn(response, {"market_model_offers_wizard": necessary_fields})

        # # Неявный
        self.assertFragmentIn(response, {"market_implicit_model": necessary_fields})
        self.assertFragmentIn(response, {"market_implicit_model_adg_wizard": necessary_fields})
        self.assertFragmentIn(response, {"market_implicit_model_center_incut": necessary_fields})


if __name__ == '__main__':
    main()
