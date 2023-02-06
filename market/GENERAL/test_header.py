#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsList, IncutModelsWithBanner
from market.media_adv.incut_search.beam.model import ModelWithBid
from market.media_adv.incut_search.beam.media_element import (
    Logo,
    Banner,
    ColoredText,
)
from market.media_adv.incut_search.beam.image import Image

from market.pylibrary.lite.matcher import (
    NotEmpty,
)


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def prepare_simple_header(cls):
        cls.content.incuts += [
            IncutModelsWithBanner(
                id=1,
                hid=101,
                vendor_id=11,
                datasource_id=1001,
                models=[ModelWithBid(model_id=1000 + i) for i in range(1, 10)],
                bid=1234,
                logo=Logo(
                    id=11,
                    text=ColoredText(
                        text='logo title',
                    ),
                    image=Image(
                        url="image_url",
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                banner=Banner(
                    id=12,
                    image=Image(
                        url='banner_url',
                        width=1,
                        height=2,
                    ),
                    click_url='banner_click_url',
                    pixel_url='banner_pixel_url',
                    bid=68,
                    text=ColoredText(
                        text='banner text',
                    ),
                    subtitle=ColoredText(
                        text='banner subtitle',
                    ),
                )
            ),
            # incut without logo
            IncutModelsWithBanner(
                id=2,
                hid=102,
                vendor_id=12,
                datasource_id=1002,
                models=[ModelWithBid(model_id=2000 + i) for i in range(1, 10)],
                bid=1234,
                # without logo
                banner=Banner(
                    id=12,
                    image=Image(
                        url='banner_url',
                        width=1,
                        height=2,
                    ),
                    click_url='banner_click_url',
                    pixel_url='banner_pixel_url',
                    bid=68,
                    text=ColoredText(
                        text='banner text',
                    ),
                    subtitle=ColoredText(
                        text='banner subtitle',
                    ),
                )
            ),
            # incut with logo without title
            IncutModelsWithBanner(
                id=3,
                hid=103,
                vendor_id=13,
                datasource_id=1003,
                models=[ModelWithBid(model_id=3000 + i) for i in range(1, 10)],
                bid=1234,
                logo=Logo(
                    id=13,
                    # without title
                    image=Image(
                        url="image_url",
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                banner=Banner(
                    id=12,
                    image=Image(
                        url='banner_url',
                        width=1,
                        height=2,
                    ),
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
            ),
        ]

    def test_simple_header(self):
        """
        проверка валидного заголовка
        """
        expected_result = {
            'incutLists': [[
                {
                    'entity': 'incut',
                    'id': '1',
                }
            ]],
            'entities': {
                'incut': {
                    '1': {
                        'saasId': 1,
                        'header': {
                            'type': 'default',
                            'text': 'logo title',  # custom logo title
                            'logos': [
                                {
                                    'entity': 'mediaElement',
                                    'id': NotEmpty(),
                                },
                            ],
                        },
                    },
                },
            },
        }

        response = self.request(
            {
                'hid': 101,
                'incuts': 'ml',
            },
            exp_flags={},
        )
        self.assertFragmentIn(response, expected_result)

        # тот же результат для врезки на 3 элемента
        response = self.request(
            {
                'hid': 101,
                'incuts': 'ml3',
            },
            exp_flags={},
        )
        self.assertFragmentIn(response, expected_result)

        # тот же результат для врезки с баннером
        response = self.request(
            {
                'hid': 101,
                'incuts': 'mwb',
            },
            exp_flags={},
        )
        self.assertFragmentIn(response, expected_result)

    def test_header_without_logo(self):
        """
        заголовок должен быть, даже без ЛОГО в саасе
        """
        expected_result = {
            'incutLists': [[
                {
                    'entity': 'incut',
                    'id': '1',
                }
            ]],
            'entities': {
                'incut': {
                    '1': {
                        'saasId': 2,
                        'header': {
                            'type': 'default',
                            'text': 'Идеи для покупок',  # заголовок по умолчанию
                        },
                    },
                },
            },
        }

        response = self.request(
            {
                'hid': 102,
                'incuts': 'ml',
            },
            exp_flags={},
        )
        self.assertFragmentIn(response, expected_result)

        response = self.request(
            {
                'hid': 102,
                'incuts': 'ml3',
            },
            exp_flags={},
        )
        self.assertFragmentIn(response, expected_result)

        response = self.request(
            {
                'hid': 102,
                'incuts': 'mwb',
            },
            exp_flags={},
        )
        self.assertFragmentIn(response, expected_result)

    def test_header_with_logo_without_title(self):
        """
        должен быть дефолтный заголовок
        """
        expected_result = {
            'incutLists': [[
                {
                    'entity': 'incut',
                    'id': '1',
                }
            ]],
            'entities': {
                'incut': {
                    '1': {
                        'saasId': 3,
                        'header': {
                            'type': 'default',
                            'text': 'Идеи для покупок',  # default
                            'logos': [
                                {
                                    'entity': 'mediaElement',
                                    'id': NotEmpty(),
                                },
                            ],
                        },
                    },
                },
            },
        }

        response = self.request(
            {
                'hid': 103,
                'incuts': 'ml',
            },
            exp_flags={},
        )
        self.assertFragmentIn(response, expected_result)

        response = self.request(
            {
                'hid': 103,
                'incuts': 'ml3',
            },
            exp_flags={},
        )
        self.assertFragmentIn(response, expected_result)

        response = self.request(
            {
                'hid': 103,
                'incuts': 'mwb',
            },
            exp_flags={},
        )
        self.assertFragmentIn(response, expected_result)

    @classmethod
    def prepare_simple_header_with_many_incuts(cls):
        cls.content.incuts += [
            IncutModelsList(
                id=100+i,
                hid=105,
                vendor_id=20+i,
                datasource_id=2000+i,
                models=[ModelWithBid(model_id=1000 + i) for i in range(1, 10)],
                bid=100*i,
                logo=Logo(
                    id=10+i,
                    text=ColoredText(
                        text='logo title',
                    ),
                    image=Image(
                        url="image_url",
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
            )
            for i in range(4)
        ]

    def test_simple_header_with_many_incuts(self):
        """
        проверка валидного заголовка
        """
        response = self.request(
            {
                'hid': 105,
                'incuts': 'ml,ml3,mwb',
                'top_hid': 105,
            },
            exp_flags={},
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [[
                    {
                        'entity': 'incut',
                        'id': '1',
                    }
                ]],
                'entities': {
                    'incut': {
                        '1': {
                            'header': {
                                'type': 'default',
                                'text': 'logo title',  # custom logo title
                                'logos': [
                                    {
                                        'entity': 'mediaElement',
                                        'id': NotEmpty(),
                                    },
                                ],
                            },
                        },
                    },
                },
            }
        )

    @classmethod
    def prepare_title_replacement(cls):
        cls.content.incuts += [
            IncutModelsList(
                id=200,
                hid=106,
                vendor_id=11,
                datasource_id=1001,
                models=[ModelWithBid(model_id=1000 + i) for i in range(1, 10)],
                bid=1234,
                logo=Logo(
                    id=11,
                    text=ColoredText(
                        text='Идеи для покупок от вендора Одиннадцать',
                    ),
                    image=Image(
                        url="image_url",
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
            ),
            IncutModelsWithBanner(
                id=201,
                hid=107,
                vendor_id=11,
                datasource_id=1001,
                models=[ModelWithBid(model_id=1000 + i) for i in range(1, 10)],
                bid=1234,
                logo=Logo(
                    id=11,
                    text=ColoredText(
                        text='Идеи для покупок от вендора Одиннадцать',
                    ),
                    image=Image(
                        url="image_url",
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
                banner=Banner(
                    id=12,
                    image=Image(
                        url='banner_url',
                        width=1,
                        height=2,
                    ),
                    click_url='banner_click_url',
                    pixel_url='banner_pixel_url',
                    bid=68,
                    text=ColoredText(
                        text='Идеи для покупок от вендора Одиннадцать',
                    ),
                    subtitle=ColoredText(
                        text='banner subtitle',
                    ),
                )
            ),
        ]

    def test_title_replacement(self):
        """
        Замена заголовка у безбаннерных врезок в середине поиска
        """
        response = self.request(
            {
                'hid': 106,
                'incuts': 'ml,ml3,mwb,header;ml,ml3,mwb,search-block',
                'top_hid': 106,
            },
            exp_flags={
                'market_madv_change_search_incut_title': 1,
            },
        )
        # в топе заголовок не подменяется, в середине поиска подменяется
        self.assertFragmentIn(
            response,
            {
                'incutLists': [[
                    {
                        'entity': 'incut',
                        'id': '1',
                    },
                ], [
                    {
                        'entity': 'incut',
                        'id': '2',
                    },
                ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'header': {
                                'text': 'Идеи для покупок от вендора Одиннадцать',  # не заменили
                                'logos': [
                                    {
                                        'entity': 'mediaElement',
                                        'id': "1",
                                    },
                                ],
                            },
                            'text': {
                                'text': 'Идеи для покупок от вендора Одиннадцать',
                            },
                        },
                        '2': {
                            'header': {
                                'text': 'Предложения от вендора Одиннадцать',  # заменили
                                'logos': [
                                    {
                                        'entity': 'mediaElement',
                                        'id': '2',
                                    },
                                ],
                            },
                            'text': {
                                'text': 'Предложения от вендора Одиннадцать',
                            },
                        },
                    },
                    'mediaElement': {
                        '1': {
                            'text': {
                                'text': 'Идеи для покупок от вендора Одиннадцать',  # не заменили
                            },
                        },
                        '2': {
                            'text': {
                                'text': 'Предложения от вендора Одиннадцать',  # заменили
                            },
                        },
                    }
                },
            }
        )

        response = self.request(
            {
                'hid': 106,
                'incuts': 'ml,ml3,mwb,header;ml,ml3,mwb,search-block',
                'top_hid': 106,
            },
            exp_flags={
                'market_madv_change_search_incut_title': 0,
            },
        )

        # с выключенным флагом заголовки не подменяются
        self.assertFragmentIn(
            response,
            {
                'incutLists': [[
                    {
                        'entity': 'incut',
                        'id': '1',
                    }
                ], [
                    {
                        'entity': 'incut',
                        'id': '2',
                    }
                ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'header': {
                                'text': 'Идеи для покупок от вендора Одиннадцать',  # не заменили
                                'logos': [
                                    {
                                        'entity': 'mediaElement',
                                        'id': "1",
                                    },
                                ],
                            },
                            'text': {
                                'text': 'Идеи для покупок от вендора Одиннадцать',
                            },
                        },
                        '2': {
                            'header': {
                                'text': 'Идеи для покупок от вендора Одиннадцать',  # не заменили
                                'logos': [
                                    {
                                        'entity': 'mediaElement',
                                        'id': "2",
                                    },
                                ],
                            },
                            'text': {
                                'text': 'Идеи для покупок от вендора Одиннадцать',
                            },
                        },
                    },
                    'mediaElement': {
                        '1': {
                            'text': {
                                'text': 'Идеи для покупок от вендора Одиннадцать',  # не заменили
                            },
                        },
                        '2': {
                            'text': {
                                'text': 'Идеи для покупок от вендора Одиннадцать',  # не заменили
                            },
                        },
                    }
                },
            }
        )

        response = self.request(
            {
                'hid': 107,
                'incuts': 'ml,ml3,mwb,header;ml,ml3,mwb,search-block',
                'top_hid': 107,
            },
            exp_flags={
                'market_madv_change_search_incut_title': 1,
            },
        )

        # у врезок с баннером заголовки не подменяются
        self.assertFragmentIn(
            response,
            {
                'incutLists': [[
                    {
                        'entity': 'incut',
                        'id': '1',
                    }
                ], [
                    {
                        'entity': 'incut',
                        'id': '2',
                    }

                ]],
                'entities': {
                    'incut': {
                        '1': {
                            'saasId': 201,
                            'header': {
                                'type': 'default',
                                'text': 'Идеи для покупок от вендора Одиннадцать',  # не заменили
                                'logos': [
                                    {
                                        'entity': 'mediaElement',
                                        'id': NotEmpty(),
                                    },
                                ],
                            },
                            'text': {
                                'text': 'Идеи для покупок от вендора Одиннадцать',
                            },
                        },
                        '2': {
                            'saasId': 201,
                            'header': {
                                'type': 'default',
                                'text': 'Идеи для покупок от вендора Одиннадцать',  # не заменили
                                'logos': [
                                    {
                                        'entity': 'mediaElement',
                                        'id': NotEmpty(),
                                    },
                                ],
                            },
                            'text': {
                                'text': 'Идеи для покупок от вендора Одиннадцать',  # не заменили
                            },
                        },
                    },
                },
            }
        )


if __name__ == '__main__':
    env.main()
