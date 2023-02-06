#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsWithBanner
from market.media_adv.incut_search.beam.media_element import (
    BannerAdaptive,
    BannerFixed,
    ColoredText,
    Logo,
)
from market.media_adv.incut_search.beam.image import Image


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def prepare_adaptive_banner(cls):
        '''
        Тест 1. Подготовка врезки с адаптивным медийным баннером
        '''
        cls.content.incuts += [
            IncutModelsWithBanner(
                hid=607,
                vendor_id=607,
                datasource_id=607,
                id=607,
                url='adaptive banner for main page',
                bid=60,
                logo=Logo(
                    id=607,
                    text=ColoredText(
                        text='banner adaptive logo title',
                    ),
                    image=Image(
                        url='image_url_for_logo',
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                banner=BannerAdaptive(
                    id=1234,
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=60,
                    image=Image(
                        url='img',
                        width=700,
                        height=600,
                    ),
                    text=ColoredText(
                        text='adaptive_banner_for_main_page',
                    ),
                ),
            )
        ]

    def test_adaptive_banner(self):
        '''
        Тест 1. Запрос врезки с типом "адаптивный медийный баннер" (mba)
        В ответе врезка с типом MediaBannerAdaptive
        '''
        response = self.request(
            {
                'hid': 607,
                'incuts': 'mba',
            },
            handler='incuts',
            exp_flags={},
            debug=True,
        )
        self.assertFragmentIn(
            response,
            {
                'entities': {
                    'incut': {
                        '1': {
                            'banner': {
                                'entity': 'mediaElement',
                                'id': '1',
                            },
                            'incutType': 'MediaBannerAdaptive',
                            'saasRequestHid': 607,
                        },
                    },
                    'mediaElement': {
                        '1': {'id': 1234},
                    },
                }
            },
        )

    @classmethod
    def prepare_media_banner_fixed(cls):
        '''
        Тест 2. Подготовка врезки с фиксированным медийным баннером
        '''
        cls.content.incuts += [
            IncutModelsWithBanner(
                hid=707,
                vendor_id=707,
                datasource_id=707,
                id=707,
                url='some_banner_for_main_page',
                bid=90,
                banner=BannerFixed(
                    id=5678,
                    click_url='clk_url',
                    pixel_url='pxl_url',
                    bid=90,
                    image=Image(
                        url='img',
                        width=400,
                        height=400,
                    ),
                    text=ColoredText(
                        text='some_banner_for_main_page',
                    ),
                ),
            )
        ]

    def test_media_banner_fixed(self):
        '''
        Тест 2. Запрос врезки с типом "фиксированный медийный баннер" (mbf)
        В ответе врезка с типом MediaBannerFixed
        '''
        resp = self.request(
            {
                'hid': 707,
                'incuts': 'mbf',
            },
            handler='incuts',
            exp_flags={},
            debug=True,
        )
        self.assertFragmentIn(
            resp,
            {
                'entities': {
                    'incut': {
                        '1': {
                            'banner': {
                                'entity': 'mediaElement',
                                'id': '1',
                            },
                            'incutType': 'MediaBannerFixed',
                            'saasRequestHid': 707,
                        },
                    },
                    'mediaElement': {'1': {'id': 5678}},
                }
            },
        )

    @classmethod
    def prepare_media_banner_type(cls):
        '''
        Тест 3. Подготовка двух врезок с медийным баннером (адаптивный и фиксированный)
        для проверки работы приоритета врезок
        '''
        cls.content.incuts += [
            IncutModelsWithBanner(
                hid=9009,
                vendor_id=790,
                datasource_id=790,
                id=790,
                url='some_banner_for_main_page',
                bid=120,
                banner=BannerFixed(
                    id=790,
                    click_url='clk_url',
                    pixel_url='pxl_url',
                    bid=120,
                    text=ColoredText(
                        text='adaptive_banner_for_main_page',
                    ),
                ),
            ),
            IncutModelsWithBanner(
                hid=9009,
                vendor_id=312,
                datasource_id=312,
                id=312,
                url='some_banner_for_main_page',
                bid=120,
                logo=Logo(
                    id=607,
                    text=ColoredText(
                        text='banner adaptive logo title',
                    ),
                    image=Image(
                        url='image_url_for_logo',
                        width=200,
                        height=200,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                banner=BannerAdaptive(
                    id=312,
                    click_url='clk_url',
                    pixel_url='pxl_url',
                    bid=120,
                    image=Image(
                        url='img',
                        width=400,
                        height=400,
                    ),
                    text=ColoredText(
                        text='fixed_banner_for_main_page',
                    ),
                ),
            ),
        ]

    def test_media_banner_typey_mba(self):
        '''
        Тест 3. При запросе типа "адаптивный медийный баннер",
        из двух врезок в ответе одна(адаптивный баннер) - фиксированный баннер нельзя превратить в адаптивный

        '''
        resp = self.request(
            {
                'hid': 9009,
                'incuts': 'mba',
            },
            handler='incuts',
            exp_flags={},
            debug=True,
        )
        self.assertFragmentIn(
            resp,
            {
                'entities': {
                    'incut': {
                        '1': {
                            'banner': {
                                'entity': 'mediaElement',
                                'id': '1',
                            },
                            'incutType': 'MediaBannerAdaptive',
                            'saasRequestHid': 9009,
                        },
                    },
                    'mediaElement': {'1': {'id': 312}},
                }
            },
        )

    def test_media_banner_priority_mbf(self):
        '''
        Тест 4. Проверка правильной работы приоритетов для медийных баннеров.
        Запрос двух типов сразу.
        В ответе первая врезка - тип с наибольшим приоритетом(mbf, MediaBannerFixed)
        '''
        resp = self.request(
            {
                'hid': 9009,
                'incuts': 'mbf,mba',
            },
            handler='incuts',
            exp_flags={},
            debug=True,
        )
        self.assertFragmentIn(
            resp,
            {
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'MediaBannerFixed',
                            'saasRequestHid': 9009,
                            'saasId': 790,
                            'bidInfo': {
                                'bid': 120,
                                'clickPrice': 115,
                            },
                        },
                        '2': {
                            'incutType': 'MediaBannerAdaptive',
                            'saasRequestHid': 9009,
                            'saasId': 312,
                            'bidInfo': {
                                'bid': 120,
                                'clickPrice': T.default_rp,
                            },
                        },
                    },
                },
                'incutLists': [[{'entity': 'incut', 'id': '1'}, {'entity': 'incut', 'id': '2'}]],
            },
        )


if __name__ == '__main__':
    env.main()
