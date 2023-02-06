#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    ExperimentalBoostFeeReservePrice,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    RecommendedFee,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import Greater


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"

    return result


class T(TestCase):
    """
    Пишем тесты для 1p поставщика с рекомендованной ставкой
    """

    @classmethod
    def prepare_search(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1, output_type=HyperCategoryType.GURULIGHT),
        ]

        cls.index.experimental_boost_fee_reserve_prices += [ExperimentalBoostFeeReservePrice(1, 500)]

        cls.index.recommended_fee += [
            RecommendedFee(hyper_id=1, recommended_bid=0.05),
        ]

        cls.index.regiontree += [Region(rid=213, name='Нерезиновая')]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                regions=[225],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик Анатлий",
                warehouse_id=145,
            ),
        ]

        cls.index.models += [
            Model(hid=1, ts=501, hyperid=1, title='IPhone X'),
            Model(hid=1, ts=502, hyperid=2, title='Samsung Galaxy S10'),
            Model(hid=1, ts=503, hyperid=3, title='Xiaomi Mi 10'),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145]),
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=2)])],
            )
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                title='mobile phone IPhone X',
                hid=1,
                sku=1,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(ts=701, price=49000, feedid=2, fee=200, waremd5='bjbuwNSDSdBXHSBndmDUCB'),
                ],
            ),
            MarketSku(
                hyperid=2,
                title='mobile phone Samsung Galaxy S10',
                hid=1,
                sku=2,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(ts=702, price=50000, feedid=1, waremd5='ozmCtRBXgUJgvxo4kHPBzg'),
                ],
            ),
            MarketSku(
                hyperid=3,
                title='mobile phone Xiaomi Mi 10',
                hid=1,
                sku=3,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(ts=703, price=50900, feedid=2, waremd5='pfmCtRBXgUJgvxo4kHPBsd'),
                    BlueOffer(ts=704, price=50000, feedid=1, waremd5='bnmCtRBXgUJgvxo4kHPBaa'),
                ],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 501).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 502).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 503).respond(0.5)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 701).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 702).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 703).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 704).respond(0.5)

    def test_search(self):
        """
        Проверяем что 1p магазину назначилась рекомендованная ставка и амнистированная ставка не ноль
        Флаг market_set_1p_fee_recommended по умолчанию включен
        """

        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do": 1,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "1.8,0.0015,1",
            "market_report_mimicry_in_serp_pattern": 0,
            "market_metadoc_search": "no",
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=mobile+phone&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'samsung-galaxy-s10',
                            'type': 'model',
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'wareId': "ozmCtRBXgUJgvxo4kHPBzg",
                                        'slug': "mobile-phone-samsung-galaxy-s10",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 500,
                                                'brokeredFee': Greater(0),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                    ]
                }
            },
        )

        """
        Проверяем что у 1p магазина обнуляется амнистированная ставка с флагом market_zero_1p_brokered_fee = 1
        """

        rearr_flags_dict["market_zero_1p_brokered_fee"] = 1
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=mobile+phone&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'samsung-galaxy-s10',
                            'type': 'model',
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'wareId': "ozmCtRBXgUJgvxo4kHPBzg",
                                        'slug': "mobile-phone-samsung-galaxy-s10",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 500,
                                                'brokeredFee': 0,
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                    ]
                }
            },
        )

        rearr_flags_dict["market_coef_1p_fee_recommended"] = 0.8
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=mobile+phone&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'slug': 'samsung-galaxy-s10',
                            'type': 'model',
                            'offers': {
                                'items': [
                                    {
                                        'entity': "offer",
                                        'wareId': "ozmCtRBXgUJgvxo4kHPBzg",
                                        'slug': "mobile-phone-samsung-galaxy-s10",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 400,
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                    ]
                }
            },
        )

    def test_special_fee_for_top6(self):
        """
        https://st.yandex-team.ru/MARKETOUT-47504
        Увеличить ставки 1p офферам в топ6 и ценах
        """

        rearr_flags_dict = {
            "market_top6_coef_1p_fee_recommended": 2,
            "market_coef_1p_fee_recommended": 1,
            "market_set_1p_fee_recommended": 1,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        pps = [6, 21]
        for pp in pps:
            response = self.report.request_json(
                'place=productoffers&pp=%s&hyperid=3&debug=da&offers-set=default,list&&rearr-factors=%s'
                % (pp, rearr_flags_str)
            )

            self.assertFragmentIn(
                response,
                {
                    'search': {
                        "results": [
                            {
                                # Топ 6, 1P оффер.
                                # Ставка market_top6_coef_1p_fee_recommended * 500
                                'wareId': 'bnmCtRBXgUJgvxo4kHPBaQ',
                                'debug': {
                                    'sale': {
                                        'shopFee': 1000,
                                    }
                                },
                            },
                            {
                                # Топ 6, 3P оффер.
                                # Ставки не должно быть
                                'wareId': 'pfmCtRBXgUJgvxo4kHPBsQ',
                                'debug': {
                                    'sale': {
                                        'shopFee': 0,
                                    }
                                },
                            },
                            {
                                # ДО, 1P оффер.
                                # Ставка market_coef_1p_fee_recommended * 500
                                'wareId': 'bnmCtRBXgUJgvxo4kHPBaQ',
                                'benefit': {
                                    'type': 'default',
                                },
                                'debug': {
                                    'sale': {
                                        'shopFee': 500,
                                    }
                                },
                            },
                        ]
                    }
                },
            )

    @classmethod
    def prepare_special_fee_for_clothes(cls):

        clothes_categ_id = 7877999

        cls.index.hypertree += [
            HyperCategory(hid=2, output_type=HyperCategoryType.GURULIGHT),
        ]

        cls.index.experimental_boost_fee_reserve_prices += [ExperimentalBoostFeeReservePrice(2, 500)]

        cls.index.recommended_fee += [
            RecommendedFee(hyper_id=2, recommended_bid=0.05),
        ]

        cls.index.shops += [
            Shop(
                fesh=13,
                datafeed_id=10,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=14,
                datafeed_id=11,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик 2",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]

        cls.index.models += [
            Model(hid=clothes_categ_id, ts=501, hyperid=10, title='square pants'),
            Model(hid=clothes_categ_id, ts=502, hyperid=11, title='other'),
        ]

        cls.index.offers += [
            Offer(hyperid=10, fesh=13, fee=100, price=1000, hid=clothes_categ_id),
            Offer(hyperid=11, fesh=14, fee=200, price=2000, hid=clothes_categ_id),
        ]

    def test_special_fee_for_clothes(self):
        """
        специальная ставка в департаменте Одежда, обувь и аксессуары
        https://st.yandex-team.ru/MARKETOUT-43003
        """

        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do": 1,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "1.8,0.0015,2",
            "market_metadoc_search": "no",
        }

        request = str('place=prime' '&pp=7' '&text=pants' '&rids=213' '&debug=1' '&numdoc=20' '&use-default-offers=1')

        except_response = {
            'search': {
                "results": [
                    {
                        'offers': {
                            'items': [
                                {
                                    'entity': "offer",
                                    'debug': {
                                        'sale': {
                                            'shopFee': 4000,
                                            # 'brokeredFee': Greater(0),
                                        },
                                    },
                                }
                            ],
                        },
                    },
                ]
            }
        }

        # запрос с функционалом специальной ставки для одежды
        response = self.report.request_json(request + '&rearr-factors=' + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(response, except_response, preserve_order=True)

        # без специальной ставки для департамента одежды
        rearr_flags_dict['market_money_recommended_fee_for_clothes_disable'] = 1
        response = self.report.request_json(request + '&rearr-factors=' + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentNotIn(response, except_response, preserve_order=True)


if __name__ == '__main__':
    main()
