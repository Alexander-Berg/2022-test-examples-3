#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.incut import IncutModelsWithBanner
from market.media_adv.incut_search.beam.model import ModelWithBid
from market.media_adv.incut_search.beam.media_element import Logo, ColoredText, Banner
from market.media_adv.incut_search.beam.image import Image
from market.media_adv.incut_search.beam.color import Color


class T(env.MediaAdvIncutSearchSuite):

    base_model_id = 10

    @classmethod
    def prepare_no_logo_warning(cls):
        #  Тест 1. Подготовка врезки для проверки NoLogo
        cls.content.incuts += [
            IncutModelsList(
                id=707,  # врезка для проверки NoLogo
                hid=1200,
                vendor_id=10000,
                datasource_id=10000,
                bid=261,
                logo=None,
                models=[ModelWithBid(model_id=cls.base_model_id * 1 + i) for i in range(1, 4)],
            )
        ]

    def test_no_logo_warning(self):
        """
        Тест 1.
        У врезки нет лого, в ответе предупреждение NoLogo
        """
        response = self.request(
            {
                'incut_ids': '707',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '707': {
                        'Warnings': [
                            'NoLogo',
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_default_logo_title_warning(cls):
        #  Тест 2. Подготовка врезок для проверки DefaultLogoTitle
        cls.content.incuts += [
            IncutModelsList(
                id=101,  # врезка для проверки DefaultLogoTitle - нет текста
                hid=98756,
                vendor_id=10001,
                datasource_id=10001,
                url='test_document_{}',
                bid=50,
                logo=Logo(
                    id=10,
                    text=None,
                    image=Image(
                        url='image_url1',
                        width=800,
                        height=600,
                    ),
                    click_url='click url1',
                    pixel_url='pixel_url1',
                    bid=67,
                ),
                models=[ModelWithBid(model_id=cls.base_model_id * 2 + i) for i in range(1, 4)],
            ),
            IncutModelsList(
                id=102,  # врезка для проверки DefaultLogoTitle - пустой текст
                hid=98756,
                vendor_id=10002,
                datasource_id=30,
                bid=60,
                logo=Logo(
                    id=20,
                    text=ColoredText(
                        text='',
                    ),
                    image=Image(
                        url="image_url2",
                        width=800,
                        height=600,
                    ),
                    click_url='click url2',
                    pixel_url='pixel_url2',
                    bid=68,
                ),
                models=[ModelWithBid(model_id=cls.base_model_id * 3 + i) for i in range(1, 4)],
            ),
            IncutModelsList(
                id=103,  # врезка для проверки DefaultLogoTitle - дефолтный текст
                hid=98756,
                vendor_id=10003,
                datasource_id=40,
                bid=70,
                logo=Logo(
                    id=30,
                    text=ColoredText(
                        text='Идеи для покупок',
                    ),
                    image=Image(
                        url="image_url3",
                        width=800,
                        height=600,
                    ),
                    click_url='click url3',
                    pixel_url='pixel_url3',
                    bid=68,
                ),
                models=[ModelWithBid(model_id=cls.base_model_id * 4 + i) for i in range(1, 4)],
            ),
        ]

    def test_default_logo_title_warning(self):
        """
        Тест 2.
        Запрашиваем 3 врезки - у первой(101) нет текста, у второй(102) текст пустой, у третей(103) дефолтный - "Идеи для покупок"
        для каждой из врезок в ответе есть предупреждение - DefaultLogoTitle
        """
        response = self.request(
            {
                'incut_ids': '101,102,103',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '101': {
                        'Warnings': [
                            'DefaultLogoTitle',
                        ]
                    },
                    '102': {
                        'Warnings': [
                            'DefaultLogoTitle',
                        ]
                    },
                    '103': {
                        'Warnings': [
                            'DefaultLogoTitle',
                        ]
                    },
                }
            },
        )

    @classmethod
    def prepare_no_logo_image_url_warning(cls):
        # Тест 3. Подготовка врезки для проверки NoLogoImageUrl
        cls.content.incuts += [
            IncutModelsList(
                id=201,  # врезка для проверки NoLogoImageUrl - пустой Image
                hid=98756,
                vendor_id=20001,
                datasource_id=20001,
                bid=60,
                url='test_document_{}',
                logo=Logo(
                    id=800,
                    text=ColoredText(
                        text='Great incut',
                    ),
                    image=Image(),
                    click_url='click url1',
                    pixel_url='pixel_url1',
                    bid=67,
                ),
                models=[ModelWithBid(model_id=cls.base_model_id * 5 + i) for i in range(1, 4)],
            ),
        ]

    def test_no_logo_image_url_warning(self):
        """
        Тест 3.
        У запрашиваемой врезки нет url для картинки с лого - в ответе NoLogoImageUrl
        """
        response = self.request(
            {
                'incut_ids': '201',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '201': {
                        'Warnings': [
                            'NoLogoImageUrl',
                        ]
                    },
                }
            },
        )

    @classmethod
    def prepare_no_logo_click_url_warning(cls):
        # Тест 4. Подготовка врезки для проверки NoLogoClickUrl
        cls.content.incuts += [
            IncutModelsList(
                id=202,  # врезка для проверки NoLogoClickUrl - click_url = None
                hid=98756,
                vendor_id=20002,
                datasource_id=20002,
                bid=70,
                url='test_document_{}',
                logo=Logo(
                    id=800,
                    text=ColoredText(
                        text='Another yet great incut',
                    ),
                    image=Image(
                        url='image_url{}',
                        width=800,
                        height=600,
                    ),
                    click_url='',
                    pixel_url='pixel_url1',
                    bid=77,
                ),
                models=[ModelWithBid(model_id=cls.base_model_id * 6 + i) for i in range(1, 4)],
            ),
        ]

    def test_no_logo_click_url_warning(self):
        """
        Тест 4.
        У запрашиваемой врезки нет url в лого для перехода по клику - в ответе предупреждение NoLogoClickUrl
        """
        response = self.request(
            {
                'incut_ids': '202',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '202': {
                        'Warnings': [
                            'NoLogoClickUrl',
                        ]
                    },
                }
            },
        )

    @classmethod
    def prepare_no_banner_warning(cls):
        # Тест 5. Подготовка врезки для проверки NoBanner
        cls.content.incuts += [
            IncutModelsWithBanner(
                id=301,
                hid=98756,
                vendor_id=30001,
                datasource_id=30001,
                models=[ModelWithBid(model_id=cls.base_model_id * 8 + i) for i in range(1, 4)],
                bid=135,
                logo=Logo(
                    id=30001,
                    text=ColoredText(
                        text='More great incuts',
                    ),
                    image=Image(
                        url='image_url',
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                banner=None,
            )
        ]

    def test_no_banner_warning(self):
        """
        Тест 5.
        У запрашиваемой врезки нет баннера - в ответе предупреждение NoBanner
        """
        response = self.request(
            {
                'incut_ids': '301',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '301': {
                        'Warnings': [
                            'NoBanner',
                        ]
                    },
                }
            },
        )

    @classmethod
    def prepare_no_banner_color_warning(cls):
        # Тест 6. Подготовка врезки для проверки NoBannerColor
        cls.content.incuts += [
            IncutModelsWithBanner(
                id=302,
                hid=98756,
                vendor_id=30002,
                datasource_id=30002,
                models=[ModelWithBid(model_id=cls.base_model_id * 9 + i) for i in range(1, 4)],
                bid=450,
                logo=Logo(
                    id=30001,
                    text=ColoredText(
                        text='More great incuts',
                    ),
                    image=Image(
                        url='image_url',
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                banner=Banner(
                    id=300022,
                    image=Image(
                        url='banner_url',
                        width=1,
                        height=2,
                    ),
                    color=None,
                    click_url='banner_click_url',
                    pixel_url='banner_pixel_url',
                    bid=68,
                    text=ColoredText(
                        text='banner text',
                    ),
                    subtitle=ColoredText(
                        text='banner subtitle',
                    ),
                ),
            )
        ]

    def test_no_banner_color_warning(self):
        """
        Тест 6.
        У запрашиваемой врезки у баннера нет цвета - в ответе предупреждение NoBannerColor
        """
        response = self.request(
            {
                'incut_ids': '302',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '302': {
                        'Warnings': [
                            'NoBannerColor',
                        ]
                    },
                }
            },
        )

    @classmethod
    def prepare_no_banner_text_warning(cls):
        # Тест 7. Подготовка врезки для проверки NoBannerText
        cls.content.incuts += [
            IncutModelsWithBanner(
                id=303,
                hid=98756,
                vendor_id=30003,
                datasource_id=30003,
                models=[ModelWithBid(model_id=cls.base_model_id * 10 + i) for i in range(1, 4)],
                bid=120,
                logo=Logo(
                    id=30003,
                    text=ColoredText(
                        text='Just great incuts',
                    ),
                    image=Image(
                        url='image_url',
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                banner=Banner(
                    id=300033,
                    image=Image(
                        url='banner_url',
                        width=1,
                        height=2,
                    ),
                    color=Color(background="#F6E8FA"),
                    click_url='banner_click_url',
                    pixel_url='banner_pixel_url',
                    bid=68,
                    text='',
                    subtitle=ColoredText(
                        text='banner subtitle',
                    ),
                ),
            )
        ]

    def test_no_banner_text_warning(self):
        """
        Тест 7.
        У запрашиваемой врезки у баннера нет текста - в ответе предупреждение NoBannerText
        """
        response = self.request(
            {
                'incut_ids': '303',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '303': {
                        'Warnings': [
                            'NoBannerText',
                        ]
                    },
                }
            },
        )

    @classmethod
    def prepare_no_banner_text_color_warning(cls):
        # Тест 8. Подготовка врезки для проверки NoBannerTextColor
        cls.content.incuts += [
            IncutModelsWithBanner(
                id=304,
                hid=98756,
                vendor_id=30004,
                datasource_id=30004,
                models=[ModelWithBid(model_id=cls.base_model_id * 11 + i) for i in range(1, 4)],
                bid=513,
                logo=Logo(
                    id=30004,
                    text=ColoredText(
                        text='Just great incuts',
                    ),
                    image=Image(
                        url='image_url',
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                banner=Banner(
                    id=300044,
                    image=Image(
                        url='banner_url',
                        width=1,
                        height=2,
                    ),
                    color=Color(background="#F6E8FA"),
                    click_url='banner_click_url',
                    pixel_url='banner_pixel_url',
                    bid=68,
                    text=ColoredText(
                        text='',
                    ),
                    subtitle=ColoredText(
                        text='banner subtitle',
                    ),
                ),
            )
        ]

    def test_no_banner_text_color_warning(self):
        """
        Тест 8.
        У запрашиваемой врезки у баннера у текста нет цвета - в ответе предупреждение NoBannerTextColor
        """
        response = self.request(
            {
                'incut_ids': '304',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '304': {
                        'Warnings': [
                            'NoBannerTextColor',
                        ]
                    },
                }
            },
        )

    @classmethod
    def prepare_no_banner_image_url_warning(cls):
        # Тест 9. Подготовка врезки для проверки NoBannerImageUrl
        cls.content.incuts += [
            IncutModelsWithBanner(
                id=305,
                hid=98756,
                vendor_id=30005,
                datasource_id=30005,
                models=[ModelWithBid(model_id=cls.base_model_id * 12 + i) for i in range(1, 4)],
                bid=783,
                logo=Logo(
                    id=30005,
                    text=ColoredText(
                        text='Just great incuts',
                    ),
                    image=Image(
                        url='image_url',
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                banner=Banner(
                    id=300055,
                    image=Image(
                        url='',
                        width=1,
                        height=2,
                    ),
                    color=Color(background="#F6E8FA"),
                    click_url='banner_click_url',
                    pixel_url='banner_pixel_url',
                    bid=68,
                    text=ColoredText(
                        text='banner text',
                    ),
                    subtitle=ColoredText(
                        text='banner subtitle',
                    ),
                ),
            )
        ]

    def test_no_banner_image_url_warning(self):
        """
        Тест 9.
        У запрашиваемой врезки у баннера нет ссылки на картинку - в ответе предупреждение NoBannerImageUrl
        """
        response = self.request(
            {
                'incut_ids': '305',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '305': {
                        'Warnings': [
                            'NoBannerImageUrl',
                        ]
                    },
                }
            },
        )

    @classmethod
    def prepare_no_banner_click_url_warning(cls):
        # Тест 10. Подготовка врезки для проверки NoBannerClickUrl
        cls.content.incuts += [
            IncutModelsWithBanner(
                id=306,
                hid=98756,
                vendor_id=30006,
                datasource_id=30006,
                models=[ModelWithBid(model_id=cls.base_model_id * 13 + i) for i in range(1, 4)],
                bid=456,
                logo=Logo(
                    id=30005,
                    text=ColoredText(
                        text='Just great incuts',
                    ),
                    image=Image(
                        url='',
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                banner=Banner(
                    id=300055,
                    image=Image(
                        url='banner_url',
                        width=1,
                        height=2,
                    ),
                    color=Color(background="#F6E8FA"),
                    click_url='',
                    pixel_url='banner_pixel_url',
                    bid=68,
                    text=ColoredText(
                        text='banner text',
                    ),
                    subtitle=ColoredText(
                        text='banner subtitle',
                    ),
                ),
            )
        ]

    def test_no_banner_click_url_warning(self):
        """
        Тест 10.
        У запрашиваемой врезки у баннера нет ссылки для клика - в ответе предупреждение NoBannerClickUrl
        """
        response = self.request(
            {
                'incut_ids': '306',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentIn(
            response,
            {
                'IncutStat': {
                    '306': {
                        'Warnings': [
                            'NoBannerClickUrl',
                        ]
                    },
                }
            },
        )

    @classmethod
    def prepare_no_warnings(cls):
        # Тест 11. Подготовка врезки для проверки NoWarnings
        cls.content.incuts += [
            IncutModelsWithBanner(
                id=307,
                hid=98756,
                vendor_id=30007,
                datasource_id=30007,
                models=[ModelWithBid(model_id=cls.base_model_id * 14 + i) for i in range(1, 4)],
                bid=666,
                logo=Logo(
                    id=30007,
                    text=ColoredText(
                        text='Just great incuts',
                    ),
                    image=Image(
                        url='mega_url_for_image',
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                banner=Banner(
                    id=300077,
                    image=Image(
                        url='banner_url',
                        width=1,
                        height=2,
                    ),
                    color=Color(background="#F6E8FA"),
                    click_url='banner_click_url',
                    pixel_url='banner_pixel_url',
                    bid=68,
                    text=ColoredText(
                        text='banner text',
                        color='#F6E8FA',
                    ),
                    subtitle=ColoredText(
                        text='banner subtitle',
                    ),
                ),
            )
        ]

    def test_no_warnings(self):
        """
        Тест 11.
        У запрашиваемой врезки все ок - в ответе NoWarnings
        """
        response = self.request(
            {
                'incut_ids': '307',
                'rids': '1,2,3',
            },
            exp_flags={
                'market_madv_model_regional_stats_enabled': 0,
            },
            handler='incutstat',
        )
        self.assertFragmentNotIn(
            response,
            {
                'IncutStat': {
                    '307': {'Warnings': []},
                }
            },
        )


if __name__ == '__main__':
    env.main()
