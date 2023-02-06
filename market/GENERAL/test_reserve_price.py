#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid
from market.media_adv.incut_search.beam.media_element import Logo, ColoredText
from market.pylibrary.lite.matcher import EmptyList


class T(env.MediaAdvIncutSearchSuite):
    @classmethod
    def setup_market_access_resources(cls):
        cls.access_resources.reserve_prices = {
            # hid, rp
            1002: 100,
            1003: 80,
        }

    @classmethod
    def prepare_reserve_price_auction(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=3334,
                vendor_id=3434,
                datasource_id=13,
                models=[ModelWithBid(model_id=3000 + i) for i in range(1, 4)],
                bid=50,
            ),
            IncutModelsList(
                hid=3334,
                vendor_id=3435,
                datasource_id=14,
                models=[ModelWithBid(model_id=3010 + i) for i in range(1, 4)],
                bid=T.default_rp - 2,
            ),
        ]

    # проверим, что ставка амнистируется с применением RP, если ставка конкурента ещё ниже
    def test_reserve_price_auction(self):
        response = self.request(
            {
                'hid': 3334,
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
                            'modelId': 3001,
                        },
                        '2': {
                            'modelId': 3002,
                        },
                        '3': {
                            'modelId': 3003,
                        },
                    },
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 50,
                                'clickPrice': T.default_rp,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 3434,
                        },
                    },
                },
            },
        )

    @classmethod
    def prepare_reserve_price_filtering(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=3335,
                vendor_id=3436,
                datasource_id=15,
                models=[ModelWithBid(model_id=3020 + i) for i in range(1, 4)],
                bid=T.default_rp - 2,
            )
        ]

    # проверим, что врезка не отдаётся, если ставка меньше, чем RP
    def test_reserve_price_filtering(self):
        response = self.request(
            {
                'hid': 3335,
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
                            'incutType': 'Empty',
                            'models': EmptyList(),
                        },
                    },
                },
            },
        )

    @classmethod
    def prepare_dynamic_reserve_price(cls):
        """
        в каждой категории по одному вендору с различными ставками
        :return:
        """
        cls.content.incuts += [
            IncutModelsList(
                hid=1001,
                vendor_id=1001,
                datasource_id=3,
                models=[ModelWithBid(model_id=100 + i) for i in range(0, 6)],
                bid=T.default_rp - 2,  # ставка меньше дефолтного значения
            ),
            IncutModelsList(
                hid=1002,
                vendor_id=1002,
                datasource_id=3,
                models=[ModelWithBid(model_id=200 + i) for i in range(0, 6)],
                bid=50,  # ставка больше минимального, но в аксесе ставка еще больше по этому hid
            ),
            IncutModelsList(
                hid=1003,
                vendor_id=1003,
                datasource_id=3,
                models=[ModelWithBid(model_id=300 + i) for i in range(0, 6)],
                bid=100,  # ставка больше чем в аксесе
            ),
            IncutModelsList(
                hid=1004,
                vendor_id=1004,
                datasource_id=3,
                models=[ModelWithBid(model_id=400 + i) for i in range(0, 6)],
                bid=200,  # в аксесе этой категории нет (должны быть минимальная ставка из rearr)
            ),
        ]

    def test_dynamic_reserve_price(self):
        """
        использование RP, приезжающие из MarketAccess
        """
        # не должно быть выдачи, т.к. ставка меньше минимальной
        response = self.request(
            {
                'hid': 1001,
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
                            'incutType': 'Empty',
                            'models': EmptyList(),
                        },
                    },
                },
            },
        )

        # ставка больше минимальной, но в аксесе rp указано больше - не должна показываться врезка
        response = self.request(
            {
                'hid': 1002,
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
                            'incutType': 'Empty',
                            'models': EmptyList(),
                        },
                    },
                },
            },
        )

        # ставка (100) больше, чем указано в аксесе (80) - должна быть врезка
        response = self.request(
            {
                'hid': 1003,
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
                    'model': {},
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 100,
                                'clickPrice': 80,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 1003,
                        },
                    },
                },
            },
        )

        # без ставки в аксесе - должна быть минимальная
        response = self.request(
            {
                'hid': 1004,
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
                    'model': {},
                    'incut': {
                        '1': {
                            'incutType': 'ModelsList',
                            'bidInfo': {
                                'bid': 200,
                                'clickPrice': T.default_rp,
                            },
                        },
                    },
                    'vendor': {
                        '1': {
                            'vendorId': 1004,
                        },
                    },
                },
            },
        )

    @classmethod
    def prepare_low_logo_bid_incut(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=3001,
                vendor_id=2501,
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
                logo=Logo(
                    id=90,
                    text=ColoredText(
                        text='logo title',
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=T.default_rp - 3,
                ),
            ),
        ]

    def test_low_logo_bid_incut(self):
        response = self.request(
            {
                'hid': 3001,
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
                            'header': {
                                'logos': [
                                    {
                                        'entity': 'mediaElement',
                                        'id': '1',
                                    },
                                ],
                            },
                        },
                    },
                    'mediaElement': {
                        '1': {
                            'bidInfo': {
                                'bid': T.default_rp - 3,
                                'clickPrice': T.default_rp,
                            },
                        },
                    },
                },
            },
        )


if __name__ == '__main__':
    env.main()
