#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid
from market.media_adv.incut_search.beam.media_element import Logo, ColoredText
from market.media_adv.incut_search.beam.constraints import Constraints
from market.media_adv.incut_search.beam.image import Image

from market.pylibrary.lite.matcher import EmptyList, NotEmptyList


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def setUpClass(cls):
        """
        переопределенный метод для дополнительного вызова настроек
        """
        cls.settings.access_using = True
        super(T, cls).setUpClass()

    @classmethod
    def setup_market_access_resources(cls):
        cls.access_resources.cutoff_resource = [14579, 7809, 16835]

    @classmethod
    def prepare_single_models_list(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=1234,
                vendor_id=2345,
                datasource_id=10,
                models=[ModelWithBid(model_id=1000 + i) for i in range(1, 4)],
                bid=90,
            ),
            IncutModelsList(
                hid=1234,
                vendor_id=2346,
                datasource_id=11,
                models=[ModelWithBid(model_id=1010 + i) for i in range(1, 4)],
                bid=45,
            ),
        ]

    # проверим, что врезка отдается без фронтенда
    def test_single_models_list(self):
        response = self.request(
            {
                'hid': 1234,
            },
            exp_flags={
                'exp_flag_one': True,
                'exp_flag_two': 'asd',
            },
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'model': {
                        '1': {
                            'modelId': 1001,
                        },
                        '2': {
                            'modelId': 1002,
                        },
                        '3': {
                            'modelId': 1003,
                        },
                    },
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'models': [
                                {
                                    'entity': 'model',
                                    'id': '1',
                                },
                                {
                                    'entity': 'model',
                                    'id': '2',
                                },
                                {
                                    'entity': 'model',
                                    'id': '3',
                                },
                            ],
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 2345,
                        },
                    },
                },
            },
        )

    # проверим, что врезка не отдается для десктопа (мало моделек)
    def test_single_models_list_desktop(self):
        response = self.request({'hid': 1234, 'frontend': 'desktop'})
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'Empty',
                            'models': EmptyList(),
                        },
                    },
                },
            },
        )

    # проверим, что врезка отдается для десктопа с эксп флагом
    def test_single_models_list_desktop_exp(self):
        response = self.request(
            {'hid': 1234, 'frontend': 'desktop'},
            exp_flags={
                'market_madv_incut_high_snippets_desktop_min_docs': 3,
            },
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'model': {
                        '1': {
                            'modelId': 1001,
                        },
                        '2': {
                            'modelId': 1002,
                        },
                        '3': {
                            'modelId': 1003,
                        },
                    },
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'models': [
                                {
                                    'entity': 'model',
                                    'id': '1',
                                },
                                {
                                    'entity': 'model',
                                    'id': '2',
                                },
                                {
                                    'entity': 'model',
                                    'id': '3',
                                },
                            ],
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 2345,
                        },
                    },
                },
            },
        )

    # проверим, что врезка отдается для тача,
    # также проверим аукцион: сумма ставок для вендора 2346 в 2 раза меньше,
    # значит clickPrice для моделей должен быть равен половине ставки
    def test_single_models_list_touch(self):
        response = self.request({'hid': 1234, 'frontend': 'touch'})
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'model': {
                        '1': {
                            'modelId': 1001,
                        },
                        '2': {
                            'modelId': 1002,
                        },
                        '3': {
                            'modelId': 1003,
                        },
                    },
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 90,
                                'clickPrice': 46,
                            },
                            'models': [
                                {
                                    'entity': 'model',
                                    'id': '1',
                                },
                                {
                                    'entity': 'model',
                                    'id': '2',
                                },
                                {
                                    'entity': 'model',
                                    'id': '3',
                                },
                            ],
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 2345,
                        },
                    },
                },
            },
        )

    # проверим, что врезка отдается для виджета,
    # несмотря на то, что запрос приходит для КМ
    def test_single_models_list_widget(self):
        response = self.request(
            {
                'hid': 1234,
                'frontend': 'widget',
                'incuts': 'ml,model-card',
                'target_page': 'modelcard',
            }
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'model': {
                        '1': {
                            'modelId': 1001,
                        },
                        '2': {
                            'modelId': 1002,
                        },
                        '3': {
                            'modelId': 1003,
                        },
                    },
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 90,
                                'clickPrice': 46,
                            },
                            'models': [
                                {
                                    'entity': 'model',
                                    'id': '1',
                                },
                                {
                                    'entity': 'model',
                                    'id': '2',
                                },
                                {
                                    'entity': 'model',
                                    'id': '3',
                                },
                            ],
                            'constraints': {
                                'minDocs': 3,
                                'maxDocs': 6,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 2345,
                        },
                    },
                },
            },
        )

    # проверим, что врезка не отдается, если запрошен другой тип врезки
    def test_single_models_list_wrong_type(self):
        response = self.request({'hid': 1234, 'incuts': 'mwb'})
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'Empty',
                            'models': EmptyList(),
                        },
                    },
                },
            },
        )

    # проверим, что отдаётся высокая врезка,
    # а в альтернативных типах указана широкая с мин-макс ограничениями на неё
    def test_alternative_incut_types(self):
        response = self.request(
            {
                'hid': 1234,
                'incuts': 'ml,ml3',
                'frontend': 'desktop',
            },
            exp_flags={
                'market_madv_incut_wide_snippets_desktop_min_docs': 2,
                'market_madv_incut_wide_snippets_desktop_max_docs': 8,
                'market_madv_incut_high_snippets_desktop_min_docs': 3,
                'market_madv_incut_high_snippets_desktop_max_docs': 9,
            },
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'alternativeIncutTypes': [
                                {
                                    'incutType': 'ModelsList3Items',
                                    'minDocs': 2,
                                    'maxDocs': 8,
                                },
                            ],
                        },
                    },
                },
            },
        )

    @classmethod
    def prepare_single_models_list_all_fields(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=2223,
                vendor_id=2224,
                datasource_id=11,
                id=5546,  # incut_id
                url="test_document",
                constraints=Constraints(
                    business_id=[4, 8, 15, 16, 23, 42],
                ),
                models=[  # model list fields
                    ModelWithBid(
                        model_id=2000 + i,
                    )
                    for i in range(1, 4)
                ],
                bid=90,
                logo=Logo(
                    id=90,
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
            ),
        ]

    def test_single_models_list_all_fields(self):
        """
        Проверка в выдаче врезочника всех возможных врезок и полей в них
        """
        response = self.request(
            {
                'hid': 2223,
            }
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'model': {
                        '1': {
                            'modelId': 2001,
                        },
                        '2': {
                            'modelId': 2002,
                        },
                        '3': {
                            'modelId': 2003,
                        },
                    },
                    'incut': {
                        '1': {
                            'saasId': 5546,
                            'incutType': 'ModelsList',
                            "vendor": {
                                "entity": "vendor",
                                'id': '1',
                            },
                            'constraints': {
                                'businessId': [4, 8, 15, 16, 23, 42],
                            },
                            'saasRequestHid': 2223,
                            # 'mediaElements': [],
                            'models': [
                                {
                                    'entity': 'model',
                                    'id': '1',
                                },
                                {
                                    'entity': 'model',
                                    'id': '2',
                                },
                                {
                                    'entity': 'model',
                                    'id': '3',
                                },
                            ],
                            'header': {
                                'logos': [
                                    {
                                        'entity': 'mediaElement',  # logo
                                        'id': '1',
                                    },
                                ]
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 2224,
                        },
                    },
                    'mediaElement': {
                        '1': {
                            'id': 90,
                            'bidInfo': {
                                'bid': 67,
                                'clickPrice': T.default_rp,
                            },
                            'text': {
                                'text': 'logo title',
                            },
                            'clickUrl': 'click url',
                            'pixelUrl': 'pixel_url',
                            'type': 'Logo',
                            'image': {
                                'url': 'image_url',
                                'width': 800,
                                'height': 600,
                            },
                        }
                    },
                },
            },
        )

    # проверим, что используется топ1 категория вместо hid
    def test_top_category(self):
        response = self.request(
            {
                'hid': 2223,
                'top_hid': 1234,
            }
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'saasRequestHid': 1234,
                            "vendor": {
                                "entity": "vendor",
                                'id': '1',
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 2345,
                        },
                    },
                },
            },
        )

    @classmethod
    def prepare_region_stats(cls):
        pass

    def test_region_stats(self):
        """
        Учитывание МРС (модельно-региональной статистики)
        """
        pass

    @classmethod
    def prepare_datasource_cutoff(cls):
        # такая же врезка, как в prepare_single_models_list_all_fields,
        # но с другим id, model_id, hid и с datasource_id из списка выключенных
        cls.content.incuts += [
            IncutModelsList(
                hid=2225,
                vendor_id=2226,
                datasource_id=7809,
                id=5547,  # incut_id
                url="test_document",
                constraints=Constraints(
                    business_id=[4, 8, 15, 16, 23, 42],
                ),
                models=[  # model list fields
                    ModelWithBid(
                        model_id=3000 + i,
                    )
                    for i in range(1, 4)
                ],
                bid=90,
                logo=Logo(
                    id=90,
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
            ),
        ]

    def test_datasource_cutoff(self):
        """
        Проверяем, что врезка с выключенным датасорсом не отдаётся
        """
        response = self.request(
            {
                'hid': 2225,
            },
            debug=True,
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'Empty',
                            'models': EmptyList(),
                        },
                    },
                },
                "debug": {
                    "counters": {
                        "incuts": {"Cutoff": 1, "Total": 1},
                        "models": {},
                    },
                },
            },
        )

    @classmethod
    def prepare_incuts_with_equal_bids(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=3001,
                vendor_id=2601,
                datasource_id=2501,
                id=2501,  # incut_id
                url="test_document",
                models=[  # model list fields
                    ModelWithBid(
                        model_id=3000 + i,
                    )
                    for i in range(1, 10)
                ],
                bid=90,
            ),
            IncutModelsList(
                hid=3001,
                vendor_id=2502,
                datasource_id=2502,
                id=2502,  # incut_id
                url="test_document",
                models=[  # model list fields
                    ModelWithBid(
                        model_id=3010 + i,
                    )
                    for i in range(1, 10)
                ],
                bid=90,
            ),
            IncutModelsList(
                hid=3001,
                vendor_id=2503,
                datasource_id=2503,
                id=2503,  # incut_id
                url="test_document",
                models=[  # model list fields
                    ModelWithBid(
                        model_id=3020 + i,
                    )
                    for i in range(1, 10)
                ],
                bid=90,
            ),
        ]

    def test_incuts_with_equal_bids(self):
        response = self.request(
            {
                'hid': 3001,
            },
            exp_flags={
                'market_madv_equal_bids_hold_interval': '4294967295',
            },
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            "vendor": {
                                "entity": "vendor",
                                'id': '1',
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 2502,
                        },
                    },
                },
            },
        )

    @classmethod
    def prepare_incut_with_age(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=3002,
                vendor_id=2702,
                datasource_id=2701,
                id=2701,  # incut_id
                url="test_document",
                models=[  # model list fields
                    ModelWithBid(
                        model_id=3030 + i,
                    )
                    for i in range(1, 10)
                ],
                bid=90,
                age=86400,
            ),
        ]

    def test_incut_with_age_less_than_to_drop(self):
        response = self.request(
            {
                'hid': 3002,
            },
            exp_flags={
                'market_madv_saas_incut_age_to_drop': '86400000',
            },
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                        },
                    },
                },
            },
        )

    def test_incut_with_age_greater_than_to_drop(self):
        response = self.request(
            {
                'hid': 3002,
            },
            exp_flags={
                'market_madv_saas_incut_age_to_drop': '8640',
            },
        )
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'Empty',
                        },
                    },
                },
            },
        )

    def test_empty_incuts_list(self):
        response = self.request({'hid': 3002, 'incuts': ';'})
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ],
                    [
                        {
                            'entity': 'incut',
                            'id': '2',
                        }
                    ],
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'Empty',
                        },
                        '2': {
                            'incutType': 'Empty',
                        },
                    },
                },
            },
        )

    def test_empty_incuts_debug_on(self):
        """
        Проверка корректной работы debug при запросе пустых врезок
        :return:
        """
        response = self.request({'hid': 3002, 'incuts': ';'}, debug=True)
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ],
                    [
                        {
                            'entity': 'incut',
                            'id': '2',
                        }
                    ],
                ],
                'debug': {'logicTrace': NotEmptyList()},
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'Empty',
                        },
                        '2': {
                            'incutType': 'Empty',
                        },
                    },
                },
            },
        )

    def test_empty_incuts_param(self):
        response = self.request({'hid': 3002, 'incuts': ''})
        self.assertFragmentIn(
            response,
            {
                'incutLists': [
                    [
                        {
                            'entity': 'incut',
                            'id': '1',
                        }
                    ]
                ],
                'entities': {
                    'incut': {
                        '1': {
                            'incutType': 'Empty',
                        },
                    },
                },
            },
        )


if __name__ == '__main__':
    env.main()
