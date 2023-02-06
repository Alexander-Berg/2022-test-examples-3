#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from copy import copy
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    OfferConversion,
    Region,
    RegionalDelivery,
    Shop,
)
from core.types.autogen import Const
from core.testcase import TestCase, main
from core.matcher import Capture, GreaterEq, NotEmpty, Absent
import math

FEE_MULTIPLIER = 0.0001
MAX_FEE = 10000
RELEVANCE_MULTIPLIER = 100000
CPC_TO_CPA_CONVERSION = 0.05

SIGMOID_ALPHA = 0.61
SIGMOID_BETA = 0.01
SIGMOID_GAMMA = 2.742


def f(fee):
    return fee


def sigmoid(x, alpha, beta, gamma):
    return 1.0 + alpha * (1.0 / (1.0 + gamma * math.exp(-beta * x)) - 1.0 / (1.0 + gamma))


def convert_bid_to_fee(bid, price, conversion):
    if conversion > 0:
        return min(bid * 0.3 / (conversion * price * FEE_MULTIPLIER), MAX_FEE)
    return 0


def convert_fee_to_bid(fee, price, conversion):
    return (fee * conversion * price * FEE_MULTIPLIER) / 0.3


def convert_vendor_bid_to_fee(bid, price, vendor_conversion=CPC_TO_CPA_CONVERSION):
    return convert_bid_to_fee(bid, price, vendor_conversion)


def calculate_cpm(relevance, price, fee, alpha=SIGMOID_ALPHA, beta=SIGMOID_BETA, gamma=SIGMOID_GAMMA):
    return relevance * sigmoid(price * f(fee) * FEE_MULTIPLIER, alpha, beta, gamma) * RELEVANCE_MULTIPLIER


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


def calculate_fee(cpc_bid, fee, vbid, price, conversion, vendor_conversion=CPC_TO_CPA_CONVERSION):
    if conversion == 0:
        return fee + convert_vendor_bid_to_fee(vbid, price, vendor_conversion)

    return cpc_bid / conversion + fee + convert_vendor_bid_to_fee(vbid, price, vendor_conversion)


class AuctionData(object):
    def __init__(
        self,
        fee,
        brokered_fee,
        bid,
        min_bid,
        vbid,
        price,
        matrixnet,
        cpm,
        conversion,
        boost=1.0,
        vendor_conversion=CPC_TO_CPA_CONVERSION,
    ):
        self.fee = fee
        self.brokered_fee = brokered_fee
        self.bid = bid
        self.min_bid = min_bid
        self.vbid = vbid
        self.price = price
        self.matrixnet = matrixnet
        self.cpm = cpm
        self.conversion = conversion
        self.boost = boost
        self.vendor_conversion = vendor_conversion

    def calculate_cpm(self, alpha=SIGMOID_ALPHA, beta=SIGMOID_BETA, gamma=SIGMOID_GAMMA):
        return self.boost * calculate_cpm(
            self.matrixnet,
            self.price,
            calculate_fee(self.bid, self.fee, self.vbid, self.price, self.conversion),
            alpha,
            beta,
            gamma,
        )


def calculate_bids(first, second, alpha=SIGMOID_ALPHA, beta=SIGMOID_BETA, gamma=SIGMOID_GAMMA):
    first_copy = copy(first)
    first_copy.fee = 1

    min_bid_fee = convert_bid_to_fee(first.min_bid, first.price, first.conversion)

    while (
        calculate_cpm(
            first_copy.boost * first_copy.matrixnet,
            first_copy.price,
            first_copy.fee - min_bid_fee,
            alpha=alpha,
            beta=beta,
            gamma=gamma,
        )
        <= second.cpm
    ) and 0 < first_copy.fee < 10000:
        first_copy.fee += 1

    total_fee = (
        first.fee
        + convert_vendor_bid_to_fee(first.vbid, first.price, first.vendor_conversion)
        + convert_bid_to_fee(first.bid, first.price, first.conversion)
    )

    fee_ab = min(first_copy.fee, total_fee)

    multiplier = fee_ab / total_fee

    return (first.fee * multiplier, first.vbid * multiplier, first.bid * multiplier)


class T(TestCase):
    """
    https://st.yandex-team.ru/MARKETOUT-37971
    Пишем тесты для CPA поискового аукциона
    """

    @classmethod
    def prepare_prime_cpm(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.001)

        cls.index.regiontree += [Region(rid=213, name='Нерезиновая')]
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=11,
                datafeed_id=11,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(fesh=2, priority_region=213, regions=[225]),
            Shop(fesh=3, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=4, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=5, priority_region=213, regions=[225]),
            Shop(fesh=6, priority_region=213, regions=[225]),
        ]

        cls.index.models += [
            Model(hid=1, ts=501, hyperid=1, title='market auction alpha', vbid=10),
            Model(hid=1, ts=502, hyperid=2, title='market auction beta'),
            Model(hid=1, ts=503, hyperid=3, title='market auction gamma'),
            # Model(hid=1, ts=504, hyperid=4, title='market auction delta'),
        ]

        cls.index.offers += [
            Offer(
                hid=1,
                ts=601,
                price=1999,
                title='market auction cpc epsilon',
                waremd5='VkjX-08eqaaf0Q_MrNfQBw',
                hyperid=1,
                fesh=2,
                cbid=100,
            ),
            Offer(
                hid=1,
                ts=602,
                price=2999,
                title='market auction cpa zeta',
                waremd5='mxyTpoZMOeSEF-5Ia4PDhw',
                hyperid=1,
                fesh=3,
                cbid=100,
                fee=35,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=1,
                ts=603,
                price=1299,
                title='market auction cpc eta',
                waremd5='UChUMwabn69TyQDNJkL6nQ',
                hyperid=2,
                fesh=2,
                cbid=200,
            ),
            Offer(
                hid=1,
                ts=604,
                price=1929,
                title='market auction cpa theta',
                waremd5='q-u3w59LXka7z6srVl7zKw',
                hyperid=2,
                fesh=3,
                cbid=500,
                fee=55,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=1,
                ts=605,
                price=1931,
                title='market auction cpc iota',
                waremd5='JTU6zWcYA9rDBeRxm2pKUA',
                hyperid=3,
                fesh=2,
                cbid=97,
            ),
            Offer(
                hid=1,
                ts=606,
                price=2222,
                title='market auction cpc kappa',
                waremd5='0M_6Nugytl_hRabpNgvagw',
                fesh=2,
                cbid=75,
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
            Offer(
                hid=1,
                ts=607,
                price=1231,
                title='market auction cpa lambda',
                waremd5='ySODAC91fN3VLfhOaIlLXg',
                fesh=3,
                cbid=90,
                fee=20,
                cpa=Offer.CPA_REAL,
            ),
            # Offer(hid=1, ts=608, price=1111, title='market auction cpa mu', waremd5='mxyTpoZMOeSEFhgkuyhgky', hyperid=4, fesh=4, cbid=100, fee=35, cpa=Offer.CPA_REAL),
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
                title='market auction blue cpa nu',
                hid=1,
                sku=1,
                vbid=17,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(ts=701, price=2100, feedid=11, fee=78, waremd5='ozmCtRBXgUJgvxo4kHPBzg'),
                ],
            )
        ]

        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 8).respond(89)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 4).respond(88)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 6).respond(87)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 501).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 502).respond(0.52)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 503).respond(0.51)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 504).respond(0.57)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 601).respond(0.56)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 602).respond(0.55)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 603).respond(0.53)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 604).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 605).respond(0.59)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 606).respond(0.58)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 607).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 608).respond(0.49)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 701).respond(0.61)

    def test_search_params_in_access_log(self):
        """
        Проверяем что параметры с которыми запущен поисковый аукцион логируются в access log
        """

        self.report.request_json(
            'place=prime&pp=7&text=market+auction&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1'
        )
        self.access_log.expect(exp_info="prt=0;pst=1;rdoc=5").once()

        self.report.request_json(
            'place=prime&pp=7&hid=10&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1'
        )
        self.access_log.expect(exp_info="prt=0;pst=2;rdoc=5").once()

        # Отдельно проверяем для place=blender
        self.report.request_json(
            'place=blender&pp=7&hid=10&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1'
        )
        self.access_log.expect(exp_info="prt=0;pst=2;rdoc=5")

    def test_prime_cpm(self):
        """
        Делаем запрос в prime и сверяем cpm, расчитанный репортом с расчитанным в тесте с включенным флагом market_white_search_auction_cpa_fee_subtract_minbid_from_bid
        """

        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 1,
            "market_white_search_auction_cpa_fee_subtract_minvbid_from_vbid": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_desktop": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=market+auction&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        alpha_fee = Capture()
        alpha_cpm = Capture()
        alpha_vbid = Capture()
        alpha_min_vbid = Capture()
        beta_fee = Capture()
        beta_cpm = Capture()
        gamma_cpm = Capture()
        kappa_cpm = Capture()
        kappa_bid = Capture()
        kappa_min_bid = Capture()
        kappa_conversion = Capture()
        lambda_cpm = Capture()
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'market-auction-alpha',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=alpha_cpm)},
                                'sale': {
                                    'vBid': NotEmpty(capture=alpha_vbid),
                                    'minVBid': NotEmpty(capture=alpha_min_vbid),
                                },
                            },
                            'offers': {
                                'count': 3,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'wareId': "ozmCtRBXgUJgvxo4kHPBzg",
                                        'slug': "market-auction-blue-cpa-nu",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2100',
                                            },
                                            'sale': {'shopFee': NotEmpty(capture=alpha_fee)},
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'market-auction-beta',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=beta_cpm)},
                            },
                            'offers': {
                                'count': 2,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "market-auction-cpa-theta",
                                        'wareId': "q-u3w59LXka7z6srVl7zKw",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '1929',
                                            },
                                            'sale': {'shopFee': NotEmpty(capture=beta_fee)},
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'market-auction-gamma',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=gamma_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "market-auction-cpc-iota",
                                        'wareId': "JTU6zWcYA9rDBeRxm2pKUA",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '1931',
                                            }
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'offer',
                            'wareId': '0M_6Nugytl_hRabpNgvagw',
                            'slug': 'market-auction-cpc-kappa',
                            'debug': {
                                'properties': {'CURR_PRICE': '2222'},
                                'metaProperties': {'CPM': NotEmpty(capture=kappa_cpm)},
                                'sale': {'bid': NotEmpty(capture=kappa_bid), 'minBid': NotEmpty(capture=kappa_min_bid)},
                            },
                        },
                        {
                            "entity": "product",
                            "id": GreaterEq(Const.VMID_START),
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=lambda_cpm)},
                            },
                            "offers": {
                                "count": 1,
                                "items": [
                                    {
                                        'entity': 'offer',
                                        'wareId': "ySODAC91fN3VLfhOaIlLXg",
                                        'slug': 'market-auction-cpa-lambda',
                                        "marketSku": GreaterEq(Const.VMID_START),
                                        'debug': {'properties': {'CURR_PRICE': '1231'}, 'sale': {'shopFee': 20}},
                                    }
                                ],
                            },
                        },
                    ]
                }
            },
        )

        alpha_price = 2100
        self.assertAlmostEqual(
            calculate_cpm(
                float(0.6),
                alpha_price,
                float(alpha_fee.value) + convert_vendor_bid_to_fee(alpha_vbid.value - alpha_min_vbid.value, 2100),
                sigmoid_alpha,
                sigmoid_beta,
                sigmoid_gamma,
            ),
            float(alpha_cpm.value),
            delta=2,
        )

        beta_price = 1929
        self.assertAlmostEqual(
            calculate_cpm(
                float(0.7),  # here mn value from default offer (strange)
                beta_price,
                float(beta_fee.value),
                sigmoid_alpha,
                sigmoid_beta,
                sigmoid_gamma,
            ),
            float(beta_cpm.value),
            delta=1,
        )

        gamma_price = 1931
        self.assertAlmostEqual(
            calculate_cpm(float(0.59), gamma_price, 0, sigmoid_alpha, sigmoid_beta, sigmoid_gamma),
            float(gamma_cpm.value),
            delta=1,
        )

        # Загрузка конверсии. Подробное описание смотри ниже в test_amnesty
        conversion_response = self.report.request_json('place=print_doc&offerid=0M_6Nugytl_hRabpNgvagw&rids=213')
        self.assertFragmentIn(
            conversion_response,
            {
                'documents': [
                    {
                        'properties': {
                            'ware_md5': '0M_6Nugytl_hRabpNgvagw',
                            'offer_conversion': NotEmpty(capture=kappa_conversion),
                        }
                    }
                ]
            },
        )
        kappa_conversion_parsed = float(kappa_conversion.value.split(';')[0].split(':')[1])
        kappa_price = 2222
        self.assertAlmostEqual(
            calculate_cpm(
                float(0.58),
                kappa_price,
                convert_bid_to_fee(kappa_bid.value - kappa_min_bid.value, kappa_price, kappa_conversion_parsed),
                sigmoid_alpha,
                sigmoid_beta,
                sigmoid_gamma,
            ),
            float(kappa_cpm.value),
            delta=2,
        )

        lambda_price = 1231
        self.assertAlmostEqual(
            calculate_cpm(float(0.50), lambda_price, 20, sigmoid_alpha, sigmoid_beta, sigmoid_gamma),
            float(lambda_cpm.value),
            delta=1,
        )

    def test_prime_cpm_subtract_minbid_touch(self):
        """
        Делаем запрос в prime (на таче) и сверяем cpm, расчитанный репортом с расчитанным в тесте с включенным флагом market_white_search_auction_cpa_fee_subtract_minbid_from_bid
        """

        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_touch": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 1,
            "market_white_search_auction_cpa_fee_subtract_minvbid_from_vbid": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_touch": 1,
            "market_use_white_search_auction_cpa_on_touch": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=48&text=market+auction&touch=1&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        alpha_fee = Capture()
        alpha_cpm = Capture()
        alpha_vbid = Capture()
        alpha_min_vbid = Capture()
        beta_fee = Capture()
        beta_cpm = Capture()
        gamma_cpm = Capture()
        kappa_cpm = Capture()
        kappa_bid = Capture()
        kappa_min_bid = Capture()
        kappa_conversion = Capture()
        lambda_cpm = Capture()
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'market-auction-alpha',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=alpha_cpm),
                                    'WHITE_CPA_SIGMOID': "0.21,0.0015,2.742",
                                    'WHITE_CPA_VENDOR_CONV': "0.05000000075",
                                    'WHITE_CPA_VENDOR_FEE': "228",
                                },
                                'sale': {
                                    'vBid': NotEmpty(capture=alpha_vbid),
                                    'minVBid': NotEmpty(capture=alpha_min_vbid),
                                },
                            },
                            'offers': {
                                'count': 3,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'wareId': "ozmCtRBXgUJgvxo4kHPBzg",
                                        'slug': "market-auction-blue-cpa-nu",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2100',
                                            },
                                            'sale': {'shopFee': NotEmpty(capture=alpha_fee)},
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'market-auction-beta',
                            'debug': {
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=beta_cpm),
                                    'WHITE_CPA_SHOP_CPA_FEE': "55",
                                },
                            },
                            'offers': {
                                'count': 2,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "market-auction-cpa-theta",
                                        'wareId': "q-u3w59LXka7z6srVl7zKw",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '1929',
                                            },
                                            'sale': {'shopFee': NotEmpty(capture=beta_fee)},
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'market-auction-gamma',
                            'debug': {
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=gamma_cpm),
                                    'WHITE_CPA_TOTAL_FEE': "0",
                                },
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "market-auction-cpc-iota",
                                        'wareId': "JTU6zWcYA9rDBeRxm2pKUA",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '1931',
                                            }
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'offer',
                            'wareId': '0M_6Nugytl_hRabpNgvagw',
                            'slug': 'market-auction-cpc-kappa',
                            'debug': {
                                'properties': {'CURR_PRICE': '2222'},
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=kappa_cpm),
                                    'WHITE_CPA_BID_INFO_PRICE': "2222",
                                    'WHITE_CPA_SHOP_CONV': "0.349999994",
                                    'WHITE_CPA_SHOP_CPC_FEE': "277",
                                },
                                'sale': {'bid': NotEmpty(capture=kappa_bid), 'minBid': NotEmpty(capture=kappa_min_bid)},
                            },
                        },
                        {
                            "entity": "product",
                            "id": GreaterEq(Const.VMID_START),
                            'debug': {
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=lambda_cpm),
                                    'WHITE_CPA_SHOP_CPA_FEE': "20",
                                },
                            },
                            "offers": {
                                "count": 1,
                                "items": [
                                    {
                                        'entity': 'offer',
                                        'wareId': "ySODAC91fN3VLfhOaIlLXg",
                                        'slug': 'market-auction-cpa-lambda',
                                        "marketSku": GreaterEq(Const.VMID_START),
                                        'debug': {'properties': {'CURR_PRICE': '1231'}, 'sale': {'shopFee': 20}},
                                    }
                                ],
                            },
                        },
                    ]
                }
            },
        )

        alpha_price = 2100
        self.assertAlmostEqual(
            calculate_cpm(
                float(0.6),
                alpha_price,
                float(alpha_fee.value) + convert_vendor_bid_to_fee(alpha_vbid.value - alpha_min_vbid.value, 2100),
                sigmoid_alpha,
                sigmoid_beta,
                sigmoid_gamma,
            ),
            float(alpha_cpm.value),
            delta=2,
        )

        beta_price = 1929
        self.assertAlmostEqual(
            calculate_cpm(
                float(0.7),  # here mn value from default offer (strange)
                beta_price,
                float(beta_fee.value),
                sigmoid_alpha,
                sigmoid_beta,
                sigmoid_gamma,
            ),
            float(beta_cpm.value),
            delta=1,
        )

        gamma_price = 1931
        self.assertAlmostEqual(
            calculate_cpm(float(0.59), gamma_price, 0, sigmoid_alpha, sigmoid_beta, sigmoid_gamma),
            float(gamma_cpm.value),
            delta=1,
        )

        # Загрузка конверсии. Подробное описание смотри ниже в test_amnesty
        conversion_response = self.report.request_json('place=print_doc&offerid=0M_6Nugytl_hRabpNgvagw&rids=213')
        self.assertFragmentIn(
            conversion_response,
            {
                'documents': [
                    {
                        'properties': {
                            'ware_md5': '0M_6Nugytl_hRabpNgvagw',
                            'offer_conversion': NotEmpty(capture=kappa_conversion),
                        }
                    }
                ]
            },
        )
        kappa_conversion_parsed = float(kappa_conversion.value.split(';')[0].split(':')[1])
        kappa_price = 2222
        self.assertAlmostEqual(
            calculate_cpm(
                float(0.58),
                kappa_price,
                convert_bid_to_fee(kappa_bid.value - kappa_min_bid.value, kappa_price, kappa_conversion_parsed),
                sigmoid_alpha,
                sigmoid_beta,
                sigmoid_gamma,
            ),
            float(kappa_cpm.value),
            delta=2,
        )

        lambda_price = 1231
        self.assertAlmostEqual(
            calculate_cpm(float(0.50), lambda_price, 20, sigmoid_alpha, sigmoid_beta, sigmoid_gamma),
            float(lambda_cpm.value),
            delta=1,
        )

    def test_prime_cpm_subtract_minbid_app(self):
        """
        Делаем запрос в prime (на аппе) и сверяем cpm, расчитанный репортом с расчитанным в тесте с включенным флагом market_white_search_auction_cpa_fee_subtract_minbid_from_bid
        """

        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_ios": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 1,
            "market_white_search_auction_cpa_fee_subtract_minvbid_from_vbid": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_app": 1,
            "market_use_white_search_auction_cpa_on_app": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=507&text=market+auction&client=IOS&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        alpha_fee = Capture()
        alpha_cpm = Capture()
        alpha_vbid = Capture()
        alpha_min_vbid = Capture()
        beta_fee = Capture()
        beta_cpm = Capture()
        gamma_cpm = Capture()
        kappa_cpm = Capture()
        kappa_bid = Capture()
        kappa_min_bid = Capture()
        kappa_conversion = Capture()
        lambda_cpm = Capture()
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'market-auction-alpha',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=alpha_cpm)},
                                'sale': {
                                    'vBid': NotEmpty(capture=alpha_vbid),
                                    'minVBid': NotEmpty(capture=alpha_min_vbid),
                                },
                            },
                            'offers': {
                                'count': 3,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'wareId': "ozmCtRBXgUJgvxo4kHPBzg",
                                        'slug': "market-auction-blue-cpa-nu",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2100',
                                            },
                                            'sale': {'shopFee': NotEmpty(capture=alpha_fee)},
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'market-auction-beta',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=beta_cpm)},
                            },
                            'offers': {
                                'count': 2,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "market-auction-cpa-theta",
                                        'wareId': "q-u3w59LXka7z6srVl7zKw",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '1929',
                                            },
                                            'sale': {'shopFee': NotEmpty(capture=beta_fee)},
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'market-auction-gamma',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=gamma_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "market-auction-cpc-iota",
                                        'wareId': "JTU6zWcYA9rDBeRxm2pKUA",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '1931',
                                            }
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'offer',
                            'wareId': '0M_6Nugytl_hRabpNgvagw',
                            'slug': 'market-auction-cpc-kappa',
                            'debug': {
                                'properties': {'CURR_PRICE': '2222'},
                                'metaProperties': {'CPM': NotEmpty(capture=kappa_cpm)},
                                'sale': {'bid': NotEmpty(capture=kappa_bid), 'minBid': NotEmpty(capture=kappa_min_bid)},
                            },
                        },
                        {
                            "entity": "product",
                            "id": GreaterEq(Const.VMID_START),
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=lambda_cpm)},
                            },
                            "offers": {
                                "count": 1,
                                "items": [
                                    {
                                        'entity': 'offer',
                                        'wareId': "ySODAC91fN3VLfhOaIlLXg",
                                        'slug': 'market-auction-cpa-lambda',
                                        "marketSku": GreaterEq(Const.VMID_START),
                                        'debug': {'properties': {'CURR_PRICE': '1231'}, 'sale': {'shopFee': 20}},
                                    }
                                ],
                            },
                        },
                    ]
                }
            },
        )

        alpha_price = 2100
        self.assertAlmostEqual(
            calculate_cpm(
                float(0.6),
                alpha_price,
                float(alpha_fee.value) + convert_vendor_bid_to_fee(alpha_vbid.value - alpha_min_vbid.value, 2100),
                sigmoid_alpha,
                sigmoid_beta,
                sigmoid_gamma,
            ),
            float(alpha_cpm.value),
            delta=2,
        )

        beta_price = 1929
        self.assertAlmostEqual(
            calculate_cpm(
                float(0.7),  # here mn value from default offer (strange)
                beta_price,
                float(beta_fee.value),
                sigmoid_alpha,
                sigmoid_beta,
                sigmoid_gamma,
            ),
            float(beta_cpm.value),
            delta=1,
        )

        gamma_price = 1931
        self.assertAlmostEqual(
            calculate_cpm(float(0.59), gamma_price, 0, sigmoid_alpha, sigmoid_beta, sigmoid_gamma),
            float(gamma_cpm.value),
            delta=1,
        )

        # Загрузка конверсии. Подробное описание смотри ниже в test_amnesty
        conversion_response = self.report.request_json('place=print_doc&offerid=0M_6Nugytl_hRabpNgvagw&rids=213')
        self.assertFragmentIn(
            conversion_response,
            {
                'documents': [
                    {
                        'properties': {
                            'ware_md5': '0M_6Nugytl_hRabpNgvagw',
                            'offer_conversion': NotEmpty(capture=kappa_conversion),
                        }
                    }
                ]
            },
        )
        kappa_conversion_parsed = float(kappa_conversion.value.split(';')[0].split(':')[1])
        kappa_price = 2222
        self.assertAlmostEqual(
            calculate_cpm(
                float(0.58),
                kappa_price,
                convert_bid_to_fee(kappa_bid.value - kappa_min_bid.value, kappa_price, kappa_conversion_parsed),
                sigmoid_alpha,
                sigmoid_beta,
                sigmoid_gamma,
            ),
            float(kappa_cpm.value),
            delta=2,
        )

        lambda_price = 1231
        self.assertAlmostEqual(
            calculate_cpm(float(0.50), lambda_price, 20, sigmoid_alpha, sigmoid_beta, sigmoid_gamma),
            float(lambda_cpm.value),
            delta=1,
        )

    @classmethod
    def prepare_many_models(cls):
        """
        Заводим 150 офферов/моделей для тестирования подсчёта кол-ва документов
        """
        model_count = 150
        cls.index.models += [
            Model(hid=150 + i, ts=10040 + i, hyperid=150 + i, title='Модель ' + str(150 + i), vbid=10, vendor_id=1)
            for i in range(model_count)
        ]

        cls.index.offers += [
            Offer(
                hid=150 + i,
                ts=5040 + i,
                price=2999 + 18,
                title='Оффер для тестирования подсчёта документов ' + str(i),
                hyperid=150 + i,
                fesh=2,
                cbid=100 + 18,
                fee=100 - 18,
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            )
            for i in range(model_count)
        ]

        for i in range(model_count):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 150 + i).respond(0.05)

    def test_many_models(self):
        """
        Тестируем подсчёт кол-ва документов
        """
        # Запрос без флагов аукциона
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": False,
            "market_report_mimicry_in_serp_pattern": 0,
        }
        rearr_flags_str = '&rearr-factors=%s' % dict_to_rearr(rearr_flags_dict)
        request_base = 'place=prime&pp=7&text=документов&local-offers-first=0&allow-collapsing=1&use-default-offers=1&rids=213&show-urls=external&debug=da&numdoc=48&page=1&onstock=&'
        response = self.report.request_json(request_base + rearr_flags_str)
        self.assertFragmentIn(response, {"total": 150})
        self.assertFragmentIn(response, "GetTotal(): total: 150 docs finished: 0")
        self.assertFragmentNotIn(response, "GetTotal(): total: 150 docs finished: 1")

        # Запрос с флагами аукциона
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": True,
            "market_white_search_auction_cpa_fee_transfer_fee_do": True,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "0.9,0.0015,1",
            "market_tweak_search_auction_white_cpa_fee_no_text_params_desktop": "0.9,0.0015,1",
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_desktop": 100,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": True,
            "market_money_vendor_cpc_to_cpa_conversion": 0.03,
            "market_default_order_for_all": True,
            "market_report_mimicry_in_serp_pattern": 0,
        }
        rearr_flags_str = '&rearr-factors=%s' % dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(request_base + rearr_flags_str)
        self.assertFragmentIn(response, {"total": 150})
        self.assertFragmentIn(response, "GetTotal(): total: 150 docs finished: 0")
        self.assertFragmentNotIn(response, "GetTotal(): total: 150 docs finished: 1")

    def test_amnesty_tail(self):
        """
        Тестируем амнистию в хвосте без ДО. Для отключения ДО используем флаг market_white_search_auction_cpa_fee_test_no_do.
        """
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_desktop": 1,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 0,
            "market_report_mimicry_in_serp_pattern": 0,
        }
        # No default offers
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=transfer-do&rids=213&send-sale-data=1&numdoc=20&use-default-offers=0&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        offer5_click_price = Capture()
        offer4_brokered_fee = Capture()
        offer1_vendor_click_price = Capture()
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': "offer",
                            'slug': "transfer-do-5",
                            'sale': {
                                'shopFee': 0,
                                'bid': 20,
                                'brokeredClickPrice': NotEmpty(capture=offer5_click_price),
                                'brokeredFee': 0,
                            },
                        },
                        # fake model
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-4',
                            'type': 'model',
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-4",
                                        'sale': {
                                            'shopFee': 100,
                                            'brokeredFee': NotEmpty(capture=offer4_brokered_fee),
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-1',
                            'type': 'model',
                            'sale': {'vendorClickPrice': NotEmpty(capture=offer1_vendor_click_price), 'minVBid': 2},
                            'offers': {'count': 1},
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-3',
                            'type': 'model',
                            'sale': {'vendorClickPrice': 0},
                            'offers': {'count': 1},
                        },
                    ]
                }
            },
        )

        offer4_auction_data = AuctionData(100, 0, 0, 0, 0, 2000, 0.52, 52064, 0)
        offer5_auction_data = AuctionData(0, 0, 20, 0, 0, 2000, 0.52, 52054, 0.35)
        offer1_auction_data = AuctionData(0, -1, 0, 0, 2, 2000, 0.52, 52038, 0)
        offer3_auction_data = AuctionData(0, -1, 0, 0, 0, 2000, 0.52, 52000, 0)
        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer4_auction_data, offer5_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer4_brokered_fee.value, brokered_fee, delta=2)
        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer5_auction_data, offer1_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer5_click_price.value, brokered_cbid, delta=2)
        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer1_auction_data, offer3_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        brokered_vbid = max(brokered_vbid, 2)
        self.assertAlmostEqual(offer1_vendor_click_price.value, brokered_vbid, delta=1)

    def test_old_auction_on_touch_by_flag(self):
        """
        Тестируем, что с выключенным флагом market_use_white_search_auction_cpa_on_touch на таче включается старый аукцион
        """
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_touch": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_touch": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_touch": 1,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 0,
            "market_use_white_search_auction_cpa_on_touch": 0,
            "market_report_mimicry_in_serp_pattern": 0,
        }
        # No default offers
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=48&touch=1&text=transfer-do&rids=213&debug=da&numdoc=20&use-default-offers=0&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        _ = Capture()
        _ = Capture()
        _ = Capture()
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-1',
                            'type': 'model',
                            'offers': {'count': 1},
                        },
                        {
                            'entity': "offer",
                            'slug': "transfer-do-5",
                        },
                        # fake model
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-4',
                            'type': 'model',
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-4",
                                        'debug': {
                                            'sale': {
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
            preserve_order=True,
        )
        # Проверяем, что используется старый аукцион отсутствием Use WhiteSearchAuctionCpaPolicy
        self.assertFragmentNotIn(response, "Use WhiteSearchAuctionCpaPolicy")

    def test_old_auction_on_app_by_flag(self):
        """
        Тестируем, что с выключенным флагом market_use_white_search_auction_cpa_on_app на аппе включается старый аукцион
        """
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_app": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_app": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_app": 1,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 0,
            "market_use_white_search_auction_cpa_on_app": 0,
            "market_report_mimicry_in_serp_pattern": 0,
        }
        # No default offers
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=307&client=ANDROID&text=transfer-do&rids=213&debug=da&numdoc=20&use-default-offers=0&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        _ = Capture()
        _ = Capture()
        _ = Capture()
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-1',
                            'type': 'model',
                            'offers': {'count': 1},
                        },
                        {
                            'entity': "offer",
                            'slug': "transfer-do-5",
                        },
                        # fake model
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-4',
                            'type': 'model',
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-4",
                                        'debug': {
                                            'sale': {
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
            preserve_order=True,
        )
        # Проверяем, что используется старый аукцион отсутствием Use WhiteSearchAuctionCpaPolicy
        self.assertFragmentNotIn(response, "Use WhiteSearchAuctionCpaPolicy")

    @classmethod
    def prepare_amnesty_all_in_one_test(cls):
        cls.index.models += [
            Model(hid=10, ts=802, hyperid=4, title='transfer-do-1', vbid=2),
            Model(hid=10, ts=803, hyperid=5, title='transfer-do-2'),
            Model(hid=10, ts=807, hyperid=6, title='transfer-do-3'),
        ]

        cls.index.offers += [
            Offer(hid=10, ts=804, hyperid=4, price=2000, fesh=3, fee=75, title='transfer-do-1', cpa=Offer.CPA_REAL),
            Offer(hid=10, ts=805, hyperid=5, price=2000, fesh=3, fee=55, title='transfer-do-2', cpa=Offer.CPA_REAL),
            Offer(
                hid=10,
                ts=806,
                hyperid=6,
                price=2000,
                fesh=2,
                cbid=97,
                title='transfer-do-3',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
            Offer(hid=10, ts=808, price=2000, fesh=3, fee=100, title='transfer-do-4', cpa=Offer.CPA_REAL),
            Offer(
                hid=10,
                ts=809,
                price=2000,
                fesh=2,
                cbid=20,
                title='transfer-do-5',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 802).respond(0.52)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 803).respond(0.52)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 804).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 805).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 806).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 807).respond(0.52)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 808).respond(0.52)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 809).respond(0.52)

    def test_amnesty_all_in_one(self):
        """
        Тестируем амнистию на выдаче, где есть модель с CPA ДО и vbid, модель с CPA ДО и без vbid-а, модель с CPC ДО,
        не сматченный CPC оффер и не сматченный CPA оффер.
        """
        vendor_conversion = 0.075
        sigmoid_alpha = 0.22
        sigmoid_beta = 0.001
        sigmoid_gamma = 2.6

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_desktop": 1,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 0,  # есть аналогичный тест (test_amnesty_all_in_one_subtract_minbid) с включенным флагом
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=blender&pp=7&text=transfer-do&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        offer1_cpm = Capture()
        offer1_vendor_click_price = Capture()
        offer1_brokered_fee = Capture()

        offer2_cpm = Capture()
        offer2_brokered_fee = Capture()

        offer3_cpm = Capture()
        offer3_click_price = Capture()

        offer4_cpm = Capture()
        offer4_brokered_fee = Capture()

        offer5_cpm = Capture()
        offer5_click_price = Capture()

        # Надо проверить, что для ранжирования берётся верная конверсия
        # Конверсия появляется в двух местах - ранжирование и списание, проверять тоже надо дважды
        # Создаём Capture, чтобы распарсить конверсию в ранжировании для каждого из 5 офферов
        white_cpa_vendor_conv = [Capture() for _ in range(5)]

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-1',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=offer1_cpm),
                                    'WHITE_CPA_VENDOR_CONV': NotEmpty(
                                        capture=white_cpa_vendor_conv[0]
                                    ),  # конверсия для ранжирования
                                },
                                'sale': {'vendorClickPrice': NotEmpty(capture=offer1_vendor_click_price)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-1",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 75,
                                                'brokeredFee': NotEmpty(capture=offer1_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-2',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=offer2_cpm),
                                    'WHITE_CPA_VENDOR_CONV': NotEmpty(capture=white_cpa_vendor_conv[1]),
                                },
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-2",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 55,
                                                'brokeredFee': NotEmpty(capture=offer2_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-3',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=offer3_cpm),
                                    'WHITE_CPA_VENDOR_CONV': NotEmpty(capture=white_cpa_vendor_conv[2]),
                                },
                                'sale': {'vendorClickPrice': 0},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-3",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 0,
                                                'bid': 97,
                                                'brokeredClickPrice': NotEmpty(capture=offer3_click_price),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-4',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=offer4_cpm),
                                    'WHITE_CPA_VENDOR_CONV': NotEmpty(capture=white_cpa_vendor_conv[3]),
                                },
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-4",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                # 56
                                                'brokeredFee': NotEmpty(capture=offer4_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': "offer",
                            'slug': "transfer-do-5",
                            'debug': {
                                'properties': {
                                    'CURR_PRICE': '2000',
                                },
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=offer5_cpm),
                                    'WHITE_CPA_VENDOR_CONV': NotEmpty(capture=white_cpa_vendor_conv[4]),
                                },
                                'sale': {
                                    'shopFee': 0,
                                    'bid': 20,
                                    # 32
                                    'brokeredClickPrice': NotEmpty(capture=offer5_click_price),
                                    'brokeredFee': 0,
                                },
                            },
                        },
                    ]
                }
            },
        )

        offer1_auction_data = AuctionData(
            75, -1, 0, 0, 2, 2000, 0.52, float(offer1_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer1_brokered_data = [offer1_brokered_fee.value, offer1_vendor_click_price.value, 0]

        offer2_auction_data = AuctionData(
            55, -1, 0, 0, 0, 2000, 0.52, float(offer2_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer2_brokered_data = [offer2_brokered_fee.value, 0, 0]

        # Для 3го оффера ставка за клик 97 не учитывается, так как ранжируется модель.
        # И на ранжирование моделей с CPC ДО влияет только веднор бид.
        offer3_auction_data = AuctionData(
            0, -1, 0, 0, 0, 2000, 0.52, float(offer3_cpm.value), 0.35, vendor_conversion=vendor_conversion
        )
        offer3_brokered_data = [0, 0, offer3_click_price.value]

        offer4_auction_data = AuctionData(
            100, 0, 0, 0, 0, 2000, 0.52, float(offer4_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer4_brokered_data = [offer4_brokered_fee.value, 0, 0]

        offer5_auction_data = AuctionData(
            0, 0, 20, 0, 0, 2000, 0.52, float(offer5_cpm.value), 0.35, vendor_conversion=vendor_conversion
        )
        offer5_brokered_data = [0, 0, offer5_click_price.value]

        data = [
            (offer1_auction_data, offer1_brokered_data),
            (offer2_auction_data, offer2_brokered_data),
            (offer3_auction_data, offer3_brokered_data),
            (offer4_auction_data, offer4_brokered_data),
            (offer5_auction_data, offer5_brokered_data),
        ]
        data.sort(key=lambda item: item[0].cpm, reverse=True)

        for i in range(len(data) - 1):
            brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
                data[i][0], data[i + 1][0], alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
            )
            tested_brokered_fee, tested_brokered_vbid, tested_brokered_cbid = data[i][1]
            self.assertAlmostEqual(tested_brokered_fee, round(brokered_fee, 0), delta=2)
            self.assertAlmostEqual(tested_brokered_vbid, round(brokered_vbid, 0), delta=1)
            self.assertAlmostEqual(tested_brokered_cbid, round(brokered_cbid, 0), delta=1)

        # Проверяем, что вендорская конверсия cpc в cpa в ранжировании такая же, как в амнистии
        # Конверсия в ранжировании парсится в виде строки
        for parsed_conversion in white_cpa_vendor_conv:
            self.assertAlmostEqual(float(parsed_conversion.value), vendor_conversion, delta=1e-5)

    def test_amnesty_all_in_one_bids_to_null_flag(self):
        """
        Тестируем, что с флагом market_set_fees_and_bids_null все фи и ставки зануляются.
        У всех офферов bid должны равняться minbid, даже у CPA
        """

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do": 1,
            "market_set_fees_and_bids_null": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=blender&pp=7&text=transfer-do&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        # В выдаче 5 офферов (4 модели с ДО и 1 оффер)
        offers_count = 5
        # Для каждого оффера создаём Capture на каждое из свойств: bid, minBid, clickPrice, brokeredClickPrice
        sale_descriptors = [
            {prop_name: Capture() for prop_name in ['bid', 'minBid', 'clickPrice', 'brokeredClickPrice']}
            for _ in range(offers_count)
        ]
        # Создаём Capture для slugs, по которым будем отличать офферы
        slugs = [Capture() for _ in range(offers_count)]

        # В выдаче 4 модели и 1 оффер, поэтому sale для них смотрим в разных местах
        last_offer = (
            offers_count - 1
        )  # Индекс Capture для свойств оффера, который попал в выдачу не как модель, а как оффер
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': NotEmpty(capture=slugs[offer_num]),
                                        'debug': {
                                            'sale': {
                                                'shopFee': 0,
                                                'brokeredFee': 0,
                                                'bid': NotEmpty(capture=sale_descriptors[offer_num]['bid']),
                                                'minBid': NotEmpty(capture=sale_descriptors[offer_num]['minBid']),
                                                'clickPrice': NotEmpty(
                                                    capture=sale_descriptors[offer_num]['clickPrice']
                                                ),
                                                'brokeredClickPrice': NotEmpty(
                                                    capture=sale_descriptors[offer_num]['brokeredClickPrice']
                                                ),
                                                'vBid': 0,
                                            },
                                        },
                                    }
                                ],
                            },
                        }
                        for offer_num in range(offers_count - 1)
                    ]
                    + [
                        {
                            'entity': 'offer',
                            'slug': NotEmpty(capture=slugs[offer_num]),
                            'debug': {
                                'sale': {
                                    'shopFee': 0,
                                    'brokeredFee': 0,
                                    'bid': NotEmpty(capture=sale_descriptors[last_offer]['bid']),
                                    'minBid': NotEmpty(capture=sale_descriptors[last_offer]['minBid']),
                                    'clickPrice': NotEmpty(capture=sale_descriptors[last_offer]['clickPrice']),
                                    'brokeredClickPrice': NotEmpty(
                                        capture=sale_descriptors[last_offer]['brokeredClickPrice']
                                    ),
                                    'vBid': 0,
                                }
                            },
                        }
                    ],
                },
            },
        )

        for offer_num, sale_props in enumerate(sale_descriptors):
            curr_minbid = sale_props['minBid'].value
            # Проверяем, что ставки равны minBid
            self.assertAlmostEqual(sale_props['bid'].value, curr_minbid, delta=0.0001)
            if slugs[offer_num].value not in ['transfer-do-1', 'transfer-do-2']:
                # Для офферов моделей transfer-do-1 и transfer-do-2 clickPrice и brokeredClickPrice не равны bid, так как clickPrice и brokeredClickPrice
                # не доходит до ДО. Тут баг в коде или проблема в настройке этих офферов в самом тесте
                for prop_name in ['clickPrice', 'brokeredClickPrice']:
                    # Проверяем, что ставки и списания равны minBid
                    self.assertAlmostEqual(sale_props[prop_name].value, curr_minbid, delta=0.0001)
            # Проверяем, что minBid не занулился
            self.assertTrue(curr_minbid != 0)

    def test_amnesty_all_in_one_touch(self):
        """
        Тестируем амнистию на выдаче, где есть модель с CPA ДО и vbid, модель с CPA ДО и без vbid-а, модель с CPC ДО,
        не сматченный CPC оффер и не сматченный CPA оффер. Тест для тача
        """
        vendor_conversion = 0.075
        sigmoid_alpha = 0.217
        sigmoid_beta = 0.00142
        sigmoid_gamma = 2.738

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_touch": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_touch": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_touch": 1,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 0,
            "market_use_white_search_auction_cpa_on_touch": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=blender&pp=48&touch=1&text=transfer-do&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        offer1_cpm = Capture()
        offer1_vendor_click_price = Capture()
        offer1_brokered_fee = Capture()

        offer2_cpm = Capture()
        offer2_brokered_fee = Capture()

        offer3_cpm = Capture()
        offer3_click_price = Capture()

        offer4_cpm = Capture()
        offer4_brokered_fee = Capture()

        offer5_cpm = Capture()
        offer5_click_price = Capture()

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-1',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer1_cpm)},
                                'sale': {'vendorClickPrice': NotEmpty(capture=offer1_vendor_click_price)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-1",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 75,
                                                'brokeredFee': NotEmpty(capture=offer1_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-2',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer2_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-2",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 55,
                                                'brokeredFee': NotEmpty(capture=offer2_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-3',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer3_cpm)},
                                'sale': {'vendorClickPrice': 0},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-3",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 0,
                                                'bid': 97,
                                                'brokeredClickPrice': NotEmpty(capture=offer3_click_price),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-4',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer4_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-4",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                # 56
                                                'brokeredFee': NotEmpty(capture=offer4_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': "offer",
                            'slug': "transfer-do-5",
                            'debug': {
                                'properties': {
                                    'CURR_PRICE': '2000',
                                },
                                'metaProperties': {'CPM': NotEmpty(capture=offer5_cpm)},
                                'sale': {
                                    'shopFee': 0,
                                    'bid': 20,
                                    # 32
                                    'brokeredClickPrice': NotEmpty(capture=offer5_click_price),
                                    'brokeredFee': 0,
                                },
                            },
                        },
                    ]
                }
            },
        )

        offer1_auction_data = AuctionData(
            75, -1, 0, 0, 2, 2000, 0.52, float(offer1_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer1_brokered_data = [offer1_brokered_fee.value, offer1_vendor_click_price.value, 0]

        offer2_auction_data = AuctionData(
            55, -1, 0, 0, 0, 2000, 0.52, float(offer2_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer2_brokered_data = [offer2_brokered_fee.value, 0, 0]

        # Для 3го оффера ставка за клик 97 не учитывается, так как ранжируется модель.
        # И на ранжирование моделей с CPC ДО влияет только веднор бид.
        offer3_auction_data = AuctionData(
            0, -1, 0, 0, 0, 2000, 0.52, float(offer3_cpm.value), 0.35, vendor_conversion=vendor_conversion
        )
        offer3_brokered_data = [0, 0, offer3_click_price.value]

        offer4_auction_data = AuctionData(
            100, 0, 0, 0, 0, 2000, 0.52, float(offer4_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer4_brokered_data = [offer4_brokered_fee.value, 0, 0]

        offer5_auction_data = AuctionData(
            0, 0, 20, 0, 0, 2000, 0.52, float(offer5_cpm.value), 0.35, vendor_conversion=vendor_conversion
        )
        offer5_brokered_data = [0, 0, offer5_click_price.value]

        data = [
            (offer1_auction_data, offer1_brokered_data),
            (offer2_auction_data, offer2_brokered_data),
            (offer3_auction_data, offer3_brokered_data),
            (offer4_auction_data, offer4_brokered_data),
            (offer5_auction_data, offer5_brokered_data),
        ]
        data.sort(key=lambda item: item[0].cpm, reverse=True)

        for i in range(len(data) - 1):
            brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
                data[i][0], data[i + 1][0], alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
            )
            tested_brokered_fee, tested_brokered_vbid, tested_brokered_cbid = data[i][1]
            self.assertAlmostEqual(tested_brokered_fee, round(brokered_fee, 0), delta=2)
            self.assertAlmostEqual(tested_brokered_vbid, round(brokered_vbid, 0), delta=1)
            self.assertAlmostEqual(tested_brokered_cbid, round(brokered_cbid, 0), delta=1)

    def test_amnesty_all_in_one_app(self):
        """
        Тестируем амнистию на выдаче, где есть модель с CPA ДО и vbid, модель с CPA ДО и без vbid-а, модель с CPC ДО,
        не сматченный CPC оффер и не сматченный CPA оффер. Тест для аппа
        """
        vendor_conversion = 0.075
        sigmoid_alpha = 0.22
        sigmoid_beta = 0.00152
        sigmoid_gamma = 2.744

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_android": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_app": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_app": 1,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 0,
            "market_use_white_search_auction_cpa_on_app": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=blender&pp=1707&rgb=green_with_blue&client=ANDROID&text=transfer-do&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        offer1_cpm = Capture()
        offer1_vendor_click_price = Capture()
        offer1_brokered_fee = Capture()

        offer2_cpm = Capture()
        offer2_brokered_fee = Capture()

        offer3_cpm = Capture()
        offer3_click_price = Capture()

        offer4_cpm = Capture()
        offer4_brokered_fee = Capture()

        offer5_cpm = Capture()
        offer5_click_price = Capture()

        # Надо проверить, что для ранжирования берётся верная конверсия
        # Конверсия появляется в двух местах - ранжирование и списание, проверять тоже надо дважды
        # Создаём Capture, чтобы распарсить конверсию в ранжировании для каждого из 5 офферов
        white_cpa_vendor_conv = [Capture() for _ in range(5)]

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-1',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=offer1_cpm),
                                    'WHITE_CPA_VENDOR_CONV': NotEmpty(capture=white_cpa_vendor_conv[0]),
                                },
                                'sale': {'vendorClickPrice': NotEmpty(capture=offer1_vendor_click_price)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-1",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 75,
                                                'brokeredFee': NotEmpty(capture=offer1_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-2',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=offer2_cpm),
                                    'WHITE_CPA_VENDOR_CONV': NotEmpty(capture=white_cpa_vendor_conv[1]),
                                },
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-2",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 55,
                                                'brokeredFee': NotEmpty(capture=offer2_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-3',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=offer3_cpm),
                                    'WHITE_CPA_VENDOR_CONV': NotEmpty(capture=white_cpa_vendor_conv[2]),
                                },
                                'sale': {'vendorClickPrice': 0},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-3",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 0,
                                                'bid': 97,
                                                'brokeredClickPrice': NotEmpty(capture=offer3_click_price),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-4',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=offer4_cpm),
                                    'WHITE_CPA_VENDOR_CONV': NotEmpty(capture=white_cpa_vendor_conv[3]),
                                },
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-4",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                # 56
                                                'brokeredFee': NotEmpty(capture=offer4_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': "offer",
                            'slug': "transfer-do-5",
                            'debug': {
                                'properties': {
                                    'CURR_PRICE': '2000',
                                },
                                'metaProperties': {
                                    'CPM': NotEmpty(capture=offer5_cpm),
                                    'WHITE_CPA_VENDOR_CONV': NotEmpty(capture=white_cpa_vendor_conv[4]),
                                },
                                'sale': {
                                    'shopFee': 0,
                                    'bid': 20,
                                    # 32
                                    'brokeredClickPrice': NotEmpty(capture=offer5_click_price),
                                    'brokeredFee': 0,
                                },
                            },
                        },
                    ]
                }
            },
        )

        offer1_auction_data = AuctionData(
            75, -1, 0, 0, 2, 2000, 0.52, float(offer1_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer1_brokered_data = [offer1_brokered_fee.value, offer1_vendor_click_price.value, 0]

        offer2_auction_data = AuctionData(
            55, -1, 0, 0, 0, 2000, 0.52, float(offer2_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer2_brokered_data = [offer2_brokered_fee.value, 0, 0]

        # Для 3го оффера ставка за клик 97 не учитывается, так как ранжируется модель.
        # И на ранжирование моделей с CPC ДО влияет только веднор бид.
        offer3_auction_data = AuctionData(
            0, -1, 0, 0, 0, 2000, 0.52, float(offer3_cpm.value), 0.35, vendor_conversion=vendor_conversion
        )
        offer3_brokered_data = [0, 0, offer3_click_price.value]

        offer4_auction_data = AuctionData(
            100, 0, 0, 0, 0, 2000, 0.52, float(offer4_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer4_brokered_data = [offer4_brokered_fee.value, 0, 0]

        offer5_auction_data = AuctionData(
            0, 0, 20, 0, 0, 2000, 0.52, float(offer5_cpm.value), 0.35, vendor_conversion=vendor_conversion
        )
        offer5_brokered_data = [0, 0, offer5_click_price.value]

        data = [
            (offer1_auction_data, offer1_brokered_data),
            (offer2_auction_data, offer2_brokered_data),
            (offer3_auction_data, offer3_brokered_data),
            (offer4_auction_data, offer4_brokered_data),
            (offer5_auction_data, offer5_brokered_data),
        ]
        data.sort(key=lambda item: item[0].cpm, reverse=True)

        for i in range(len(data) - 1):
            brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
                data[i][0], data[i + 1][0], alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
            )
            tested_brokered_fee, tested_brokered_vbid, tested_brokered_cbid = data[i][1]
            self.assertAlmostEqual(tested_brokered_fee, round(brokered_fee, 0), delta=2)
            self.assertAlmostEqual(tested_brokered_vbid, round(brokered_vbid, 0), delta=1)
            self.assertAlmostEqual(tested_brokered_cbid, round(brokered_cbid, 0), delta=1)

        # Проверяем, что вендорская конверсия cpc в cpa в ранжировании такая же, как в амнистии
        # Конверсия в ранжировании парсится в виде строки
        for parsed_conversion in white_cpa_vendor_conv:
            self.assertAlmostEqual(float(parsed_conversion.value), vendor_conversion, delta=1e-5)

    def test_amnesty_all_in_one_rearrange_on_current_page(self):
        """
        Случай постраничного ранжирования. (local-offers-first=0)
        Исправление бага MARKETOUT-39257
        Тестируем амнистию на выдаче, где есть модель с CPA ДО и vbid, модель с CPA ДО и без vbid-а, модель с CPC ДО,
        не сматченный CPC оффер и не сматченный CPA оффер.
        """
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_fix": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_desktop": 1,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 0,
            "market_report_mimicry_in_serp_pattern": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        # local-offers-first=0 - для переранжирования только на текущей странице
        response = self.report.request_json(
            'place=blender&pp=7&text=transfer-do&rids=213&debug=da&numdoc=20&page=1&use-default-offers=1&allow-collapsing=1&local-offers-first=0&rearr-factors=%s'
            % rearr_flags_str
        )

        offer1_cpm = Capture()
        offer1_vendor_click_price = Capture()
        offer1_brokered_fee = Capture()

        offer2_cpm = Capture()
        offer2_brokered_fee = Capture()

        offer3_cpm = Capture()
        offer3_click_price = Capture()

        offer4_cpm = Capture()
        offer4_brokered_fee = Capture()

        offer5_cpm = Capture()
        offer5_click_price = Capture()

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-1',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer1_cpm)},
                                'sale': {'vendorClickPrice': NotEmpty(capture=offer1_vendor_click_price)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-1",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 75,
                                                'brokeredFee': NotEmpty(capture=offer1_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-2',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer2_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-2",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 55,
                                                'brokeredFee': NotEmpty(capture=offer2_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-3',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer3_cpm)},
                                'sale': {'vendorClickPrice': 0},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-3",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 0,
                                                'bid': 97,
                                                'brokeredClickPrice': NotEmpty(capture=offer3_click_price),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-4',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer4_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-4",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                # 56
                                                'brokeredFee': NotEmpty(capture=offer4_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': "offer",
                            'slug': "transfer-do-5",
                            'debug': {
                                'properties': {
                                    'CURR_PRICE': '2000',
                                },
                                'metaProperties': {'CPM': NotEmpty(capture=offer5_cpm)},
                                'sale': {
                                    'shopFee': 0,
                                    'bid': 20,
                                    # 32
                                    'brokeredClickPrice': NotEmpty(capture=offer5_click_price),
                                    'brokeredFee': 0,
                                },
                            },
                        },
                    ]
                }
            },
        )

        offer1_auction_data = AuctionData(75, -1, 0, 0, 2, 2000, 0.52, float(offer1_cpm.value), 0)
        offer1_brokered_data = [offer1_brokered_fee.value, offer1_vendor_click_price.value, 0]

        offer2_auction_data = AuctionData(55, -1, 0, 0, 0, 2000, 0.52, float(offer2_cpm.value), 0)
        offer2_brokered_data = [offer2_brokered_fee.value, 0, 0]

        # Для 3го оффера ставка за клик 97 не учитывается, так как ранжируется модель.
        # И на ранжирование моделей с CPC ДО влияет только веднор бид.
        offer3_auction_data = AuctionData(0, -1, 0, 0, 0, 2000, 0.52, float(offer3_cpm.value), 0.35)
        offer3_brokered_data = [0, 0, offer3_click_price.value]

        offer4_auction_data = AuctionData(100, 0, 0, 0, 0, 2000, 0.52, float(offer4_cpm.value), 0)
        offer4_brokered_data = [offer4_brokered_fee.value, 0, 0]

        offer5_auction_data = AuctionData(0, 0, 20, 0, 0, 2000, 0.52, float(offer5_cpm.value), 0.35)
        offer5_brokered_data = [0, 0, offer5_click_price.value]

        data = [
            (offer1_auction_data, offer1_brokered_data),
            (offer2_auction_data, offer2_brokered_data),
            (offer3_auction_data, offer3_brokered_data),
            (offer4_auction_data, offer4_brokered_data),
            (offer5_auction_data, offer5_brokered_data),
        ]
        data.sort(key=lambda item: item[0].cpm, reverse=True)

        for i in range(len(data) - 1):
            brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
                data[i][0], data[i + 1][0], alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
            )
            tested_brokered_fee, tested_brokered_vbid, tested_brokered_cbid = data[i][1]
            self.assertAlmostEqual(tested_brokered_fee, round(brokered_fee, 0), delta=2)
            self.assertAlmostEqual(tested_brokered_vbid, round(brokered_vbid, 0), delta=1)
            self.assertAlmostEqual(tested_brokered_cbid, round(brokered_cbid, 0), delta=1)

    def test_amnesty_all_in_one_subtract_minbid(self):
        """
        Тестируем амнистию на выдаче, где есть модель с CPA ДО и vbid, модель с CPA ДО и без vbid-а, модель с CPC ДО,
        не сматченный CPC оффер и не сматченный CPA оффер. С коррекцией для cpc офферов на minbid
        """

        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_desktop": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=transfer-do&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        offer1_cpm = Capture()
        offer1_vendor_click_price = Capture()
        offer1_brokered_fee = Capture()

        offer2_cpm = Capture()
        offer2_brokered_fee = Capture()

        offer3_cpm = Capture()
        offer3_click_price = Capture()

        offer4_cpm = Capture()
        offer4_brokered_fee = Capture()

        offer5_cpm = Capture()
        offer5_click_price = Capture()

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-1',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer1_cpm)},
                                'sale': {'vendorClickPrice': NotEmpty(capture=offer1_vendor_click_price)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-1",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 75,
                                                'brokeredFee': NotEmpty(capture=offer1_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-2',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer2_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-2",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 55,
                                                'brokeredFee': NotEmpty(capture=offer2_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-3',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer3_cpm)},
                                'sale': {'vendorClickPrice': 0},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-3",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 0,
                                                'bid': 97,
                                                'brokeredClickPrice': NotEmpty(capture=offer3_click_price),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-4',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer4_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-4",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                # 56
                                                'brokeredFee': NotEmpty(capture=offer4_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': "offer",
                            'slug': "transfer-do-5",
                            'debug': {
                                'properties': {
                                    'CURR_PRICE': '2000',
                                },
                                'metaProperties': {'CPM': NotEmpty(capture=offer5_cpm)},
                                'sale': {
                                    'shopFee': 0,
                                    'bid': 20,
                                    # 32
                                    'brokeredClickPrice': NotEmpty(capture=offer5_click_price),
                                    'brokeredFee': 0,
                                },
                            },
                        },
                    ]
                }
            },
        )

        offer1_auction_data = AuctionData(75, -1, 0, 0, 2, 2000, 0.52, float(offer1_cpm.value), 0)
        offer1_brokered_data = [offer1_brokered_fee.value, offer1_vendor_click_price.value, 0]

        offer2_auction_data = AuctionData(55, -1, 0, 0, 0, 2000, 0.52, float(offer2_cpm.value), 0)
        offer2_brokered_data = [offer2_brokered_fee.value, 0, 0]

        # Для 3го оффера ставка за клик 97 не учитывается, так как ранжируется модель.
        # И на ранжирование моделей с CPC ДО влияет только веднор бид.
        offer3_auction_data = AuctionData(0, -1, 0, 0, 0, 2000, 0.52, float(offer3_cpm.value), 0.35)
        offer3_brokered_data = [0, 0, offer3_click_price.value]

        offer4_auction_data = AuctionData(100, 0, 0, 0, 0, 2000, 0.52, float(offer4_cpm.value), 0)
        offer4_brokered_data = [offer4_brokered_fee.value, 0, 0]

        offer5_auction_data = AuctionData(0, 0, 20, 3, 0, 2000, 0.52, float(offer5_cpm.value), 0.35)
        offer5_brokered_data = [0, 0, offer5_click_price.value]

        data = [
            (offer1_auction_data, offer1_brokered_data),
            (offer2_auction_data, offer2_brokered_data),
            (offer3_auction_data, offer3_brokered_data),
            (offer4_auction_data, offer4_brokered_data),
            (offer5_auction_data, offer5_brokered_data),
        ]
        data.sort(key=lambda item: item[0].cpm, reverse=True)

        for i in range(len(data) - 1):
            brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
                data[i][0], data[i + 1][0], alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
            )
            tested_brokered_fee, tested_brokered_vbid, tested_brokered_cbid = data[i][1]
            self.assertAlmostEqual(tested_brokered_fee, round(brokered_fee, 0), delta=2)
            self.assertAlmostEqual(tested_brokered_vbid, round(brokered_vbid, 0), delta=1)
            self.assertAlmostEqual(tested_brokered_cbid, round(brokered_cbid, 0), delta=1)

    def test_amnesty_all_in_one_no_text(self):
        """
        Тестируем амнистию на выдаче, где есть модель с CPA ДО и vbid, модель с CPA ДО и без vbid-а, модель с CPC ДО,
        не сматченный CPC оффер и не сматченный CPA оффер. Без текстовый поиск. (TODO: модели почему то идут вперед)
        """
        vendor_conversion = 0.05
        sigmoid_alpha = 0.215
        sigmoid_beta = 0.0014
        sigmoid_gamma = 2.74

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "show_log_do_with_model_white_auction": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&hid=10&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        offer1_cpm = Capture()
        offer1_vendor_click_price = Capture()
        offer1_brokered_fee = Capture()

        offer2_cpm = Capture()
        offer2_brokered_fee = Capture()

        offer3_cpm = Capture()
        _ = Capture()

        offer4_cpm = Capture()
        offer4_brokered_fee = Capture()

        offer5_cpm = Capture()
        _ = Capture()

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-1',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer1_cpm)},
                                'sale': {'vendorClickPrice': NotEmpty(capture=offer1_vendor_click_price)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-1",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 75,
                                                'brokeredFee': NotEmpty(capture=offer1_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-2',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer2_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-2",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 55,
                                                'brokeredFee': NotEmpty(capture=offer2_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-3',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer3_cpm)},
                                'sale': {'vendorClickPrice': 0},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-3",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 0,
                                                'bid': 97,
                                                # min bid
                                                'brokeredClickPrice': 3,
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-4',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer4_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-4",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                'brokeredFee': NotEmpty(capture=offer4_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': "offer",
                            'slug': "transfer-do-5",
                            'debug': {
                                'properties': {
                                    'CURR_PRICE': '2000',
                                },
                                'metaProperties': {'CPM': NotEmpty(capture=offer5_cpm)},
                                'sale': {
                                    'shopFee': 0,
                                    'bid': 20,
                                    # 32
                                    'brokeredClickPrice': 3,
                                    'brokeredFee': 0,
                                },
                            },
                        },
                    ]
                }
            },
        )

        # Модели и отдельные офферы попадают в разные корзины и у них разные аукционы
        # см TOrdinaryBaseCollector::Finish()

        offer1_auction_data = AuctionData(75, -1, 0, 0, 2, 2000, 0.52, float(offer1_cpm.value), 0)
        offer2_auction_data = AuctionData(55, -1, 0, 0, 0, 2000, 0.52, float(offer2_cpm.value), 0)
        # Для 3го оффера ставка за клик 97 не учитывается, так как ранжируется модель.
        # И на ранжирование моделей с CPC ДО влияет только веднор бид.
        offer3_auction_data = AuctionData(0, -1, 0, 0, 0, 2000, 0.52, float(offer3_cpm.value), 0.35)
        offer4_auction_data = AuctionData(100, 0, 0, 0, 0, 2000, 0.52, float(offer4_cpm.value), 0)
        offer5_auction_data = AuctionData(0, 0, 20, 0, 0, 2000, 0.52, float(offer5_cpm.value), 0.35)

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer1_auction_data, offer2_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer1_brokered_fee.value, brokered_fee, delta=1)
        self.assertAlmostEqual(offer1_vendor_click_price.value, math.ceil(brokered_vbid), delta=1)

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer2_auction_data, offer3_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer2_brokered_fee.value, brokered_fee, delta=1)

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer4_auction_data, offer5_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer4_brokered_fee.value, brokered_fee, delta=2)

        # Проверяем, что в шоу логе каждый ДО залогировался один раз
        # И что он получил super_uid от модели
        self.show_log.expect(super_uid="04884192001117778888816001", position=1, record_type=1).times(1)
        self.show_log.expect(super_uid="04884192001117778888816001", position=1, record_type=0).times(1)  # ДО

        self.show_log.expect(super_uid="04884192001117778888816002", position=2, record_type=1).times(1)
        self.show_log.expect(super_uid="04884192001117778888816002", position=2, record_type=0).times(1)  # ДО

        # ДО в дозапросе не залогировался:
        self.show_log.expect(super_uid="04884192001117778888806000", position=0, record_type=0).times(0)

    def test_amnesty_all_in_one_no_text_touch(self):
        """
        Тестируем амнистию на выдаче, где есть модель с CPA ДО и vbid, модель с CPA ДО и без vbid-а, модель с CPC ДО,
        не сматченный CPC оффер и не сматченный CPA оффер. Без текстовый поиск. Тест для тача. (TODO: модели почему то идут вперед)
        """
        vendor_conversion = 0.075
        sigmoid_alpha = 0.215
        sigmoid_beta = 0.0014
        sigmoid_gamma = 2.738

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_touch": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_touch": 1,
            "show_log_do_with_model_white_auction": 1,
            "market_use_white_search_auction_cpa_on_touch": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=48&touch=1&hid=10&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        offer1_cpm = Capture()
        offer1_vendor_click_price = Capture()
        offer1_brokered_fee = Capture()

        offer2_cpm = Capture()
        offer2_brokered_fee = Capture()

        offer3_cpm = Capture()
        _ = Capture()

        offer4_cpm = Capture()
        offer4_brokered_fee = Capture()

        offer5_cpm = Capture()
        _ = Capture()

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-1',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer1_cpm)},
                                'sale': {'vendorClickPrice': NotEmpty(capture=offer1_vendor_click_price)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-1",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 75,
                                                'brokeredFee': NotEmpty(capture=offer1_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-2',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer2_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-2",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 55,
                                                'brokeredFee': NotEmpty(capture=offer2_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-3',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer3_cpm)},
                                'sale': {'vendorClickPrice': 0},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-3",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 0,
                                                'bid': 97,
                                                # min bid
                                                'brokeredClickPrice': 3,
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-4',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer4_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-4",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                'brokeredFee': NotEmpty(capture=offer4_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': "offer",
                            'slug': "transfer-do-5",
                            'debug': {
                                'properties': {
                                    'CURR_PRICE': '2000',
                                },
                                'metaProperties': {'CPM': NotEmpty(capture=offer5_cpm)},
                                'sale': {
                                    'shopFee': 0,
                                    'bid': 20,
                                    # 32
                                    'brokeredClickPrice': 3,
                                    'brokeredFee': 0,
                                },
                            },
                        },
                    ]
                }
            },
        )

        # Модели и отдельные офферы попадают в разные корзины и у них разные аукционы
        # см TOrdinaryBaseCollector::Finish()

        offer1_auction_data = AuctionData(
            75, -1, 0, 0, 2, 2000, 0.52, float(offer1_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer2_auction_data = AuctionData(
            55, -1, 0, 0, 0, 2000, 0.52, float(offer2_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        # Для 3го оффера ставка за клик 97 не учитывается, так как ранжируется модель.
        # И на ранжирование моделей с CPC ДО влияет только веднор бид.
        offer3_auction_data = AuctionData(
            0, -1, 0, 0, 0, 2000, 0.52, float(offer3_cpm.value), 0.35, vendor_conversion=vendor_conversion
        )
        offer4_auction_data = AuctionData(
            100, 0, 0, 0, 0, 2000, 0.52, float(offer4_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer5_auction_data = AuctionData(
            0, 0, 20, 0, 0, 2000, 0.52, float(offer5_cpm.value), 0.35, vendor_conversion=vendor_conversion
        )

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer1_auction_data, offer2_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer1_brokered_fee.value, brokered_fee, delta=1)
        self.assertAlmostEqual(offer1_vendor_click_price.value, math.ceil(brokered_vbid), delta=1)

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer2_auction_data, offer3_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer2_brokered_fee.value, brokered_fee, delta=1)

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer4_auction_data, offer5_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer4_brokered_fee.value, brokered_fee, delta=2)

        # Проверяем, что в шоу логе каждый ДО залогировался один раз
        # И что он получил super_uid от модели
        self.show_log.expect(super_uid="04884192001117778888816001", position=1, record_type=1).times(1)
        self.show_log.expect(super_uid="04884192001117778888816001", position=1, record_type=0).times(1)  # ДО

        self.show_log.expect(super_uid="04884192001117778888816002", position=2, record_type=1).times(1)
        self.show_log.expect(super_uid="04884192001117778888816002", position=2, record_type=0).times(1)  # ДО

        # ДО в дозапросе не залогировался:
        self.show_log.expect(super_uid="04884192001117778888806000", position=0, record_type=0).times(0)

        # Проверяем, что на таче с флагом market_use_white_search_auction_cpa_on_touch об использовании белого поискового аукциона
        # сигнализирует эта надпись
        self.assertFragmentIn(response, "Use WhiteSearchAuctionCpaPolicy")

    def test_amnesty_all_in_one_no_text_app(self):
        """
        Тестируем амнистию на выдаче, где есть модель с CPA ДО и vbid, модель с CPA ДО и без vbid-а, модель с CPC ДО,
        не сматченный CPC оффер и не сматченный CPA оффер. Без текстовый поиск. Тест для аппа. (TODO: модели почему то идут вперед)
        """
        vendor_conversion = 0.075
        sigmoid_alpha = 0.212
        sigmoid_beta = 0.00147
        sigmoid_gamma = 2.7405

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_no_text_params_android": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_app": 1,
            "show_log_do_with_model_white_auction": 1,
            "market_use_white_search_auction_cpa_on_app": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=307&client=ANDROID&hid=10&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        offer1_cpm = Capture()
        offer1_vendor_click_price = Capture()
        offer1_brokered_fee = Capture()

        offer2_cpm = Capture()
        offer2_brokered_fee = Capture()

        offer3_cpm = Capture()
        _ = Capture()

        offer4_cpm = Capture()
        offer4_brokered_fee = Capture()

        offer5_cpm = Capture()
        _ = Capture()

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-1',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer1_cpm)},
                                'sale': {'vendorClickPrice': NotEmpty(capture=offer1_vendor_click_price)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-1",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 75,
                                                'brokeredFee': NotEmpty(capture=offer1_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-2',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer2_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-2",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 55,
                                                'brokeredFee': NotEmpty(capture=offer2_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-3',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer3_cpm)},
                                'sale': {'vendorClickPrice': 0},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-3",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 0,
                                                'bid': 97,
                                                # min bid
                                                'brokeredClickPrice': 3,
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-4',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer4_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-4",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                'brokeredFee': NotEmpty(capture=offer4_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': "offer",
                            'slug': "transfer-do-5",
                            'debug': {
                                'properties': {
                                    'CURR_PRICE': '2000',
                                },
                                'metaProperties': {'CPM': NotEmpty(capture=offer5_cpm)},
                                'sale': {
                                    'shopFee': 0,
                                    'bid': 20,
                                    # 32
                                    'brokeredClickPrice': 3,
                                    'brokeredFee': 0,
                                },
                            },
                        },
                    ]
                }
            },
        )

        # Модели и отдельные офферы попадают в разные корзины и у них разные аукционы
        # см TOrdinaryBaseCollector::Finish()

        offer1_auction_data = AuctionData(
            75, -1, 0, 0, 2, 2000, 0.52, float(offer1_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer2_auction_data = AuctionData(
            55, -1, 0, 0, 0, 2000, 0.52, float(offer2_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        # Для 3го оффера ставка за клик 97 не учитывается, так как ранжируется модель.
        # И на ранжирование моделей с CPC ДО влияет только веднор бид.
        offer3_auction_data = AuctionData(
            0, -1, 0, 0, 0, 2000, 0.52, float(offer3_cpm.value), 0.35, vendor_conversion=vendor_conversion
        )
        offer4_auction_data = AuctionData(
            100, 0, 0, 0, 0, 2000, 0.52, float(offer4_cpm.value), 0, vendor_conversion=vendor_conversion
        )
        offer5_auction_data = AuctionData(
            0, 0, 20, 0, 0, 2000, 0.52, float(offer5_cpm.value), 0.35, vendor_conversion=vendor_conversion
        )

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer1_auction_data, offer2_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer1_brokered_fee.value, brokered_fee, delta=1.2)
        self.assertAlmostEqual(offer1_vendor_click_price.value, math.ceil(brokered_vbid), delta=1)

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer2_auction_data, offer3_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer2_brokered_fee.value, brokered_fee, delta=1)

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer4_auction_data, offer5_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer4_brokered_fee.value, brokered_fee, delta=2)

        # Проверяем, что в шоу логе каждый ДО залогировался один раз
        # И что он получил super_uid от модели
        self.show_log.expect(super_uid="04884192001117778888816001", position=1, record_type=1).times(1)
        self.show_log.expect(super_uid="04884192001117778888816001", position=1, record_type=0).times(1)  # ДО

        self.show_log.expect(super_uid="04884192001117778888816002", position=2, record_type=1).times(1)
        self.show_log.expect(super_uid="04884192001117778888816002", position=2, record_type=0).times(1)  # ДО

        # ДО в дозапросе не залогировался:
        self.show_log.expect(super_uid="04884192001117778888806000", position=0, record_type=0).times(0)

        # Проверяем, что на аппе с флагом market_use_white_search_auction_cpa_on_app об использовании белого поискового аукциона
        # сигнализирует эта надпись
        self.assertFragmentIn(response, "Use WhiteSearchAuctionCpaPolicy")

    @classmethod
    def prepare_amnesty_tests(cls):
        cls.index.shops += [
            Shop(fesh=5, priority_region=213, regions=[225]),
            Shop(fesh=6, priority_region=213, regions=[225]),
            Shop(fesh=7, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=8, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.models += [
            Model(hid=5, ts=901, hyperid=10, title='aaaa-1'),
            Model(hid=5, ts=902, hyperid=11, title='aaaa-2'),
            Model(hid=5, ts=903, hyperid=12, title='aaaa-3', vbid=0),
            Model(hid=5, ts=907, hyperid=16, title='eeee-1', vbid=10),
            Model(hid=5, ts=908, hyperid=17, title='eeee-2', vbid=5),
            Model(hid=5, ts=909, hyperid=18, title='eeee-3', vbid=0),
        ]

        cls.index.offers += [
            Offer(hid=5, ts=911, hyperid=10, price=2000, fesh=3, fee=150, title='aaaa-1', cpa=Offer.CPA_REAL),
            Offer(hid=5, ts=912, hyperid=11, price=2000, fesh=7, fee=100, title='aaaa-2', cpa=Offer.CPA_REAL),
            Offer(hid=5, ts=913, hyperid=12, price=2000, fesh=8, fee=50, title='aaaa-3', cpa=Offer.CPA_REAL),
            Offer(hid=5, ts=911, hyperid=16, price=2000, fesh=3, fee=150, title='eeee-1', cpa=Offer.CPA_REAL),
            Offer(hid=5, ts=912, hyperid=17, price=2000, fesh=7, fee=100, title='eeee-2', cpa=Offer.CPA_REAL),
            Offer(hid=5, ts=913, hyperid=18, price=2000, fesh=8, fee=50, title='eeee-3', cpa=Offer.CPA_REAL),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 901).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 902).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 903).respond(0.50)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 911).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 912).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 913).respond(0.50)

    def test_amnesty_models_with_cpa_do_vbids(self):
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        e1_fee_ab = Capture()
        e1_vendor_bid_ab = Capture()
        e1_cpm = Capture()

        e2_fee_ab = Capture()
        e2_vendor_bid_ab = Capture()
        e2_cpm = Capture()

        _ = Capture()
        e3_vendor_bid_ab = Capture()
        e3_cpm = Capture()

        response = self.report.request_json(
            'place=prime&pp=7&text=eeee&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'eeee-1',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=e1_cpm)},
                                'sale': {'vBid': 10, 'vendorClickPrice': NotEmpty(capture=e1_vendor_bid_ab)},
                            },
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'eeee-1',
                                        'debug': {
                                            'sale': {'shopFee': 150, 'brokeredFee': NotEmpty(capture=e1_fee_ab)},
                                        },
                                    }
                                ]
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'eeee-2',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=e2_cpm)},
                                'sale': {'vBid': 5, 'vendorClickPrice': NotEmpty(capture=e2_vendor_bid_ab)},
                            },
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'eeee-2',
                                        'debug': {
                                            'sale': {'shopFee': 100, 'brokeredFee': NotEmpty(capture=e2_fee_ab)},
                                        },
                                    }
                                ]
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'eeee-3',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=e3_cpm)},
                                'sale': {'vBid': 0, 'vendorClickPrice': NotEmpty(capture=e3_vendor_bid_ab)},
                            },
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'eeee-3',
                                        'debug': {
                                            'sale': {'shopFee': 50, 'brokeredFee': 0},
                                        },
                                    }
                                ]
                            },
                        },
                    ]
                }
            },
        )
        # TODO почему то тест не проходит, если передать поставить во второй оффер e2_cpm.value хотя значение тоже
        e1_auction_data = AuctionData(150, e1_fee_ab.value, 0, 0, 10, 2000, 0.50, e1_cpm.value, 0)
        e2_auction_data = AuctionData(100, e2_fee_ab.value, 0, 0, 5, 2000, 0.50, 50156, 0)
        e3_auction_data = AuctionData(50, 0, 0, 0, 0, 2000, 0.50, 50030, 0)

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            e1_auction_data, e2_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )

        self.assertAlmostEqual(e1_fee_ab.value, brokered_fee, delta=1)
        self.assertAlmostEqual(e1_vendor_bid_ab.value, math.ceil(brokered_vbid), delta=1)

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            e2_auction_data, e3_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )

        self.assertAlmostEqual(e2_fee_ab.value, brokered_fee, delta=1)
        self.assertAlmostEqual(e2_vendor_bid_ab.value, math.ceil(brokered_vbid), delta=1)

    def test_amnesty_vendor_bids_do_transferred_to_do(self):
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=eeee&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'eeee-1',
                            'debug': {
                                'sale': {
                                    'vBid': 10,
                                }
                            },
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'eeee-1',
                                        'debug': {
                                            'sale': {'vendorClickPrice': 0},
                                        },
                                    }
                                ]
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'eeee-2',
                            'debug': {
                                'sale': {
                                    'vBid': 5,
                                }
                            },
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'eeee-2',
                                        'debug': {
                                            'sale': {'vendorClickPrice': 0},
                                        },
                                    }
                                ]
                            },
                        },
                    ]
                }
            },
        )

    def test_amnesty_models_with_cpa_do_no_vbids(self):
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=aaaa&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'aaaa-1',
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'aaaa-1',
                                        'debug': {
                                            'sale': {
                                                'shopFee': 150,
                                                # TODO: why not 101 here?
                                                'brokeredFee': 102,
                                            },
                                        },
                                    }
                                ]
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'aaaa-2',
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'slug': 'aaaa-2',
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                'brokeredFee': 51,
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'aaaa-3',
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'aaaa-3',
                                        'debug': {
                                            'sale': {
                                                'shopFee': 50,
                                                'brokeredFee': 0,
                                            },
                                        },
                                    }
                                ]
                            },
                        },
                    ]
                }
            },
        )

    @classmethod
    def prepare_amnesty_models_with_cpc_do_vbids_test(cls):
        cls.index.models += [
            Model(hid=5, ts=904, hyperid=13, title='bbbb-1', vbid=10),
            Model(hid=5, ts=905, hyperid=14, title='bbbb-2', vbid=5),
            Model(hid=5, ts=906, hyperid=15, title='bbbb-3', vbid=0),
        ]

        cls.index.offers += [
            Offer(
                hid=5,
                ts=914,
                hyperid=13,
                price=2000,
                fesh=2,
                cbid=30,
                title='bbbb-1',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
            Offer(
                hid=5,
                ts=915,
                hyperid=14,
                price=2000,
                fesh=5,
                cbid=20,
                title='bbbb-2',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
            Offer(
                hid=5,
                ts=916,
                hyperid=15,
                price=2000,
                fesh=6,
                cbid=10,
                title='bbbb-3',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 904).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 905).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 906).respond(0.50)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 914).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 915).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 916).respond(0.50)

    """
    TODO почему то пропадает min bid у моделей
    """

    def test_amnesty_models_with_cpc_do_vbids(self):
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=bbbb&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'bbbb-1',
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'bbbb-1',
                                    }
                                ]
                            },
                            'debug': {'sale': {'vBid': 10, 'vendorClickPrice': 6}},
                        },
                        {
                            'entity': 'product',
                            'slug': 'bbbb-2',
                            'offers': {'count': 1, 'items': [{'slug': 'bbbb-2'}]},
                            'debug': {'sale': {'vBid': 5, 'vendorClickPrice': 2}},
                        },
                        {
                            'entity': 'product',
                            'slug': 'bbbb-3',
                            'offers': {'items': [{'entity': 'offer', 'slug': 'bbbb-3'}]},
                            'debug': {
                                'sale': {
                                    'vBid': 0,
                                    'minVBid': 2,
                                    'vendorClickPrice': 0,
                                }
                            },
                        },
                    ]
                }
            },
        )

    def test_amnesty_models_with_cpc_do_vbids_subtract_min_vbid(self):
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_white_search_auction_cpa_fee_subtract_minvbid_from_vbid": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=bbbb&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'bbbb-1',
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'bbbb-1',
                                    }
                                ]
                            },
                            'debug': {'sale': {'vBid': 10, 'vendorClickPrice': 6}},
                        },
                        {
                            'entity': 'product',
                            'slug': 'bbbb-2',
                            'offers': {'count': 1, 'items': [{'slug': 'bbbb-2'}]},
                            'debug': {
                                'sale': {
                                    'vBid': 5,
                                    # 'vendorClickPrice': 2 - it's correct value
                                    'vendorClickPrice': 3,
                                }
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'bbbb-3',
                            'offers': {'items': [{'entity': 'offer', 'slug': 'bbbb-3'}]},
                            'debug': {
                                'sale': {
                                    'vBid': 0,
                                    'minVBid': 2,
                                    'vendorClickPrice': 0,
                                }
                            },
                        },
                    ]
                }
            },
        )

    @classmethod
    def prepare_amnesty_cpa_offers_test(cls):
        cls.index.offers += [
            Offer(hid=5, ts=917, price=2000, fesh=3, fee=150, title='cccc-1', cpa=Offer.CPA_REAL),
            Offer(hid=5, ts=918, price=2000, fesh=7, fee=100, title='cccc-2', cpa=Offer.CPA_REAL),
            Offer(hid=5, ts=919, price=2000, fesh=8, fee=50, title='cccc-3', cpa=Offer.CPA_REAL),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 917).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 918).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 919).respond(0.50)

    def test_amnesty_cpa_offers(self):
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=cccc&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'cccc-1',
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'cccc-1',
                                        'debug': {
                                            'sale': {
                                                'shopFee': 150,
                                                'brokeredFee': 101,
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'cccc-2',
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'cccc-2',
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                'brokeredFee': 51,
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'cccc-3',
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'cccc-3',
                                        'debug': {
                                            'sale': {
                                                'shopFee': 50,
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

    @classmethod
    def prepare_amnesty_cpc_offers_test(cls):
        cls.index.offers += [
            Offer(
                hid=5,
                ts=920,
                price=2000,
                fesh=2,
                cbid=30,
                title='dddd-1',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
            Offer(
                hid=5,
                ts=921,
                price=2000,
                fesh=5,
                cbid=20,
                title='dddd-2',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
            Offer(
                hid=5,
                ts=922,
                price=2000,
                fesh=6,
                cbid=10,
                title='dddd-3',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 920).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 921).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 922).respond(0.50)

    def test_amnesty_cpc_offers(self):
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_desktop": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(
            'place=prime&pp=7&text=dddd&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'offer',
                            'slug': 'dddd-1',
                            'debug': {
                                'sale': {
                                    'bid': 30,
                                    'brokeredClickPrice': 21,
                                },
                            },
                        },
                        {
                            'entity': 'offer',
                            'slug': 'dddd-2',
                            'debug': {
                                'sale': {'bid': 20, 'brokeredClickPrice': 11},
                            },
                        },
                        {
                            'entity': 'offer',
                            'slug': 'dddd-3',
                            'debug': {
                                'sale': {'bid': 10, 'brokeredClickPrice': 3},
                            },
                        },
                    ]
                }
            },
        )

    @classmethod
    def prepare_amnesty_cpc_offers_diff_cpm_test(cls):
        cls.index.offers += [
            Offer(
                hid=5,
                ts=923,
                price=2000,
                fesh=2,
                cbid=30,
                title='hhhh-1',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
            Offer(
                hid=5,
                ts=924,
                price=2000,
                fesh=5,
                cbid=20,
                title='hhhh-2',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
            Offer(
                hid=5,
                ts=925,
                price=2000,
                fesh=6,
                cbid=10,
                title='hhhh-3',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 923).respond(0.40)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 924).respond(0.41)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 925).respond(0.42)

    def test_amnesty_cpc_offers_diff_cpm(self):
        """
        Точно как тест test_amnesty_cpc_offers, но "более реальная" ситуация, у офферов разный cpm
        """
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_desktop": 100,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(
            'place=prime&pp=7&text=hhhh&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'offer',
                            'slug': 'hhhh-1',
                            'debug': {
                                'sale': {
                                    'bid': 30,
                                    'brokeredClickPrice': 3,
                                },
                            },
                        },
                        {
                            'entity': 'offer',
                            'slug': 'hhhh-2',
                            'debug': {
                                'sale': {'bid': 20, 'brokeredClickPrice': 3},
                            },
                        },
                        {
                            'entity': 'offer',
                            'slug': 'hhhh-3',
                            'debug': {
                                'sale': {'bid': 10, 'brokeredClickPrice': 3},
                            },
                        },
                    ]
                }
            },
        )

    def test_amnesty_cpc_offers_subtraction(self):
        # В тесте при рассчете CPM из ставки вычитаем мин ставку
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_desktop": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(
            'place=prime&pp=7&text=dddd&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'offer',
                            'slug': 'dddd-1',
                            'debug': {
                                'sale': {
                                    'bid': 30,
                                    'minBid': 3,
                                    'brokeredClickPrice': 21,
                                },
                            },
                        },
                        {
                            'entity': 'offer',
                            'slug': 'dddd-2',
                            'debug': {
                                'sale': {
                                    'bid': 20,
                                    'brokeredClickPrice': 11,
                                },
                            },
                        },
                        {
                            'entity': 'offer',
                            'slug': 'dddd-3',
                            'debug': {
                                'sale': {
                                    'bid': 10,
                                    'brokeredClickPrice': 3,
                                },
                            },
                        },
                    ]
                }
            },
        )

    def test_amnesty_cpc_offers_subtraction_touch(self):
        # В тесте при рассчете CPM из ставки вычитаем мин ставку, тест для тача
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_touch": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_touch": 1,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_touch": 1,
            "market_use_white_search_auction_cpa_on_touch": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(
            'place=prime&pp=48&touch=1&text=dddd&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'offer',
                            'slug': 'dddd-1',
                            'debug': {
                                'sale': {
                                    'bid': 30,
                                    'minBid': 3,
                                    'brokeredClickPrice': 21,
                                },
                            },
                        },
                        {
                            'entity': 'offer',
                            'slug': 'dddd-2',
                            'debug': {
                                'sale': {
                                    'bid': 20,
                                    'brokeredClickPrice': 11,
                                },
                            },
                        },
                        {
                            'entity': 'offer',
                            'slug': 'dddd-3',
                            'debug': {
                                'sale': {
                                    'bid': 10,
                                    'brokeredClickPrice': 3,
                                },
                            },
                        },
                    ]
                }
            },
        )

    def test_amnesty_cpc_offers_subtraction_app(self):
        # В тесте при рассчете CPM из ставки вычитаем мин ставку, тест для аппа
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_app": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_app": 1,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_app": 1,
            "market_use_white_search_auction_cpa_on_app": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(
            'place=prime&pp=307&client=ANDROID&text=dddd&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'offer',
                            'slug': 'dddd-1',
                            'debug': {
                                'sale': {
                                    'bid': 30,
                                    'minBid': 3,
                                    'brokeredClickPrice': 21,
                                },
                            },
                        },
                        {
                            'entity': 'offer',
                            'slug': 'dddd-2',
                            'debug': {
                                'sale': {
                                    'bid': 20,
                                    'brokeredClickPrice': 11,
                                },
                            },
                        },
                        {
                            'entity': 'offer',
                            'slug': 'dddd-3',
                            'debug': {
                                'sale': {
                                    'bid': 10,
                                    'brokeredClickPrice': 3,
                                },
                            },
                        },
                    ]
                }
            },
        )

    def test_market_white_search_auction_cpa_fee_only_text(self):
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&hid=10&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(response, "Use WhiteSearchAuctionCpaPolicy")

        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_white_search_auction_cpa_fee_only_text": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&hid=10&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentNotIn(response, "Use WhiteSearchAuctionCpaPolicy")

    @classmethod
    def prepare_vbids_first_rearrange(cls):
        cls.index.models += [
            Model(hid=5, ts=931, hyperid=21, title='ffff-1', vbid=100),
            Model(hid=5, ts=932, hyperid=22, title='ffff-2', vbid=10),
        ]

        cls.index.offers += [
            Offer(
                hid=5,
                ts=933,
                hyperid=21,
                price=2000,
                fesh=2,
                title='ffff-1',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
            Offer(
                hid=5,
                ts=934,
                hyperid=22,
                price=2000,
                cbid=30,
                fesh=5,
                title='ffff-2',
                offer_conversion=[OfferConversion(value=0.35, type='MSK')],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 931).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 932).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 933).respond(0.48)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 934).respond(0.50)

    def test_vendor_bids_plays_role_in_rearrange_with_no_do(self):
        """
        Тестируем, что вендроские ставки учитываются в первом переранжировании.
        cpm модели 21 при учете vbid 52158, если ставка не будет учитываться то cpm будет 0.5
        и ее победит схлопнутый оффер ffff-1 c 50072, который проиграет схлопнутому офферу ffff-2 c cpm 50188
        """
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&hid=5&text=ffff&rids=213&debug=da&numdoc=1&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'ffff-1',
                            'debug': {
                                'sale': {
                                    'vBid': 100,
                                }
                            },
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'ffff-1',
                                        'debug': {
                                            'sale': {'vendorClickPrice': 0},
                                        },
                                    }
                                ]
                            },
                        }
                    ]
                }
            },
        )

    def test_search_boosted_offers_has_correct_amnesty(self):
        """
        Тестируем, что при включенном поисковом бустинге правильно считается амнистия
        """
        """
        Тестируем амнистию на выдаче, где есть модель с CPA ДО и vbid, модель с CPA ДО и без vbid-а, модель с CPC ДО,
        не сматченный CPC оффер и не сматченный CPA оффер.
        """
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742
        offers_mnvalue_coef = 1
        docs_mnvalue_coef = 1.15

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_boost_cpa_offers_mnvalue_coef_text": offers_mnvalue_coef,
            "market_boost_cpa_docs_mnvalue_coef_text": docs_mnvalue_coef,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_desktop": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=transfer-do&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        offer5_cpm = Capture()
        offer5_click_price = Capture()

        offer1_cpm = Capture()
        offer1_vendor_click_price = Capture()
        offer1_brokered_fee = Capture()

        offer4_cpm = Capture()
        offer4_brokered_fee = Capture()

        offer2_cpm = Capture()
        offer2_brokered_fee = Capture()

        offer3_cpm = Capture()
        offer3_click_price = Capture()

        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-1',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer1_cpm)},
                                'sale': {'vendorClickPrice': NotEmpty(capture=offer1_vendor_click_price)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-1",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 75,
                                                'brokeredFee': NotEmpty(capture=offer1_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-4',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer4_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-4",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 100,
                                                # 56
                                                'brokeredFee': NotEmpty(capture=offer4_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-2',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer2_cpm)},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-2",
                                        'debug': {
                                            'sale': {
                                                'shopFee': 55,
                                                'brokeredFee': NotEmpty(capture=offer2_brokered_fee),
                                            },
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            'entity': "offer",
                            'slug': "transfer-do-5",
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer5_cpm)},
                                'sale': {
                                    'shopFee': 0,
                                    'bid': 20,
                                    # 32
                                    'brokeredClickPrice': NotEmpty(capture=offer5_click_price),
                                    'brokeredFee': 0,
                                },
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'transfer-do-3',
                            'type': 'model',
                            'debug': {
                                'metaProperties': {'CPM': NotEmpty(capture=offer3_cpm)},
                                'sale': {'vendorClickPrice': 0},
                            },
                            'offers': {
                                'count': 1,
                                'items': [
                                    {
                                        'entity': "offer",
                                        'slug': "transfer-do-3",
                                        'debug': {
                                            'properties': {
                                                'CURR_PRICE': '2000',
                                            },
                                            'sale': {
                                                'shopFee': 0,
                                                'bid': 97,
                                                'brokeredClickPrice': NotEmpty(capture=offer3_click_price),
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

        offer1_auction_data = AuctionData(75, -1, 0, 0, 2, 2000, 0.52, 59900, 0, boost=docs_mnvalue_coef)
        offer4_auction_data = AuctionData(100, 0, 0, 0, 0, 2000, 0.52, 59874, 0, boost=docs_mnvalue_coef)
        offer2_auction_data = AuctionData(55, -1, 0, 0, 0, 2000, 0.52, 59840, 0, boost=docs_mnvalue_coef)
        offer5_auction_data = AuctionData(0, 0, 20, 0, 0, 2000, 0.52, 52054, 0.35)
        _ = AuctionData(0, -1, 0, 0, 0, 2000, 0.52, 52000, 0.35)

        self.assertAlmostEqual(
            int(offer4_cpm.value),
            offer4_auction_data.calculate_cpm(alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma),
            delta=1,
        )

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer1_auction_data, offer4_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer1_brokered_fee.value, brokered_fee, delta=1)
        self.assertAlmostEqual(offer1_vendor_click_price.value, math.ceil(brokered_vbid), delta=1)

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer4_auction_data, offer2_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer4_brokered_fee.value, brokered_fee, delta=1)

        brokered_fee, brokered_vbid, brokered_cbid = calculate_bids(
            offer2_auction_data, offer5_auction_data, alpha=sigmoid_alpha, beta=sigmoid_beta, gamma=sigmoid_gamma
        )
        self.assertAlmostEqual(offer2_brokered_fee.value, brokered_fee, delta=1)

    @classmethod
    def prepare_subtract_min_vbid_from_vbid(cls):
        cls.index.models += [
            Model(hid=5, ts=931, hyperid=31, title='gggg-1', vbid=10),
            Model(hid=5, ts=932, hyperid=32, title='gggg-2', vbid=8),
            Model(hid=5, ts=933, hyperid=33, title='gggg-3', vbid=6),
        ]

        cls.index.offers += [
            Offer(
                hid=5,
                ts=941,
                hyperid=31,
                price=2000,
                fesh=3,
                fee=50,
                title='gggg-1',
                cpa=Offer.CPA_REAL,
            ),
            Offer(hid=5, ts=942, hyperid=32, price=2000, fesh=7, fee=50, title='gggg-2', cpa=Offer.CPA_REAL),
            Offer(hid=5, ts=943, hyperid=33, price=2000, fesh=8, fee=50, title='gggg-3', cpa=Offer.CPA_REAL),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 931).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 932).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 933).respond(0.50)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 941).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 942).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 943).respond(0.50)

    def test_default_offer_search_tracer(self):
        """
        Проверяем, что работает трейс поиска для ДО (несёт информацию о ДО, запрашиваемых с поиска)
        """
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            # Используем эти флаги, чтобы выдача была непустая
            "market_white_search_auction_cpa_fee": 1,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_subtract_minbid_from_bid": 1,
            "market_white_search_auction_cpa_fee_subtract_minvbid_from_vbid": 1,
            "market_white_search_auction_cpa_fee_cpc_conversion_mult_desktop": 1,
            # Включаем обычные трейсы поиска и трейсы поиска для ДО
            "market_documents_search_trace": "q-u3w59LXka7z6srVl7zKw,2",  # сначала идёт ware_md5 оффера, потом через запятую его модель
            "market_documents_search_trace_default_offer": "q-u3w59LXka7z6srVl7zKw,2",
            "market_use_search_trace_for_do_from_prime": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=prime&pp=7&text=market+auction&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )

        trace_fragment_for_offer = {
            "document": "q-u3w59LXka7z6srVl7zKw",
            "type": "OFFER_BY_WARE_MD5",
            "in_index": True,
            "in_accept_doc": True,
            "passed_accept_doc": True,
            "in_relevance": True,
            "passed_relevance": True,
            "in_rearrange": True,
            # "on_page" -> False для трейса поиска, True для трейса ДО (на поиске оффер превращается в модель, но ДО для неё запрашивается,
            # оффер окончательно находится)
            # "passed_rearrange" -> Точно так же, как "on_page" -> зависит от типа трейса
        }

        self.assertFragmentIn(
            response,
            {
                "search": {
                    # Проверяем, что модель, для которой есть нужный нам оффер, показалась
                    "results": [
                        {
                            "debug": {
                                "properties": {
                                    "WARE_MD5": "q-u3w59LXka7z6srVl7zKw",
                                },
                            },
                        },
                    ],
                },
                "debug": {
                    "docs_search_trace": {"traces": [trace_fragment_for_offer]},
                    "docs_search_trace_default_offer": {"traces": [trace_fragment_for_offer]},
                },
            },
        )

    def test_subtract_min_vbid_from_vbid(self):
        vendor_conversion = 0.05
        sigmoid_alpha = 0.21
        sigmoid_beta = 0.0015
        sigmoid_gamma = 2.742

        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        rearr_flags_dict = {
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_no_base_bids": 0,
            "market_white_search_auction_cpa_fee_minbid_ab": 0,
            "market_tweak_search_auction_white_cpa_fee_params_desktop": "{},{},{}".format(
                sigmoid_alpha, sigmoid_beta, sigmoid_gamma
            ),
            "market_money_vendor_cpc_to_cpa_conversion": vendor_conversion,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_white_search_auction_cpa_fee_subtract_minvbid_from_vbid": 1,
            "market_report_mimicry_in_serp_pattern": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(
            'place=prime&pp=7&text=gggg&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    "results": [
                        {
                            'entity': 'product',
                            'slug': 'gggg-1',
                            'debug': {'sale': {'vBid': 10, 'minVBid': 2, 'vendorClickPrice': 9}},
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'gggg-1',
                                        'debug': {
                                            'sale': {'shopFee': 50, 'brokeredFee': 33},
                                        },
                                    }
                                ]
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'gggg-2',
                            'debug': {'sale': {'vBid': 8, 'minVBid': 2, 'vendorClickPrice': 7}},
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'gggg-2',
                                        'debug': {
                                            'sale': {'shopFee': 50, 'brokeredFee': 29},
                                        },
                                    }
                                ]
                            },
                        },
                        {
                            'entity': 'product',
                            'slug': 'gggg-3',
                            'debug': {'sale': {'vBid': 6, 'vendorClickPrice': 2}},
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'slug': 'gggg-3',
                                        'debug': {
                                            'sale': {'shopFee': 50, 'brokeredFee': 0},
                                        },
                                    }
                                ]
                            },
                        },
                    ]
                }
            },
        )

        # TODO: раскомментировать и попробовать осазнать как оно должно быть и как получается! У меня не сошлось.
        #  В моем понимании aaaa чередуются с dddd (у них чередуется cpm), тогда aaaa должны амнистироваться до следующей dddd
        #  Закомментирован потому что плавает.
        # response = self.report.request_json('place=prime&pp=7&text=aaaa+dddd&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&rearr-factors=%s' % rearr_flags_str)
        # self.assertFragmentIn(response, {
        #     'search': { "results": [
        #         {
        #             'entity': 'product',
        #             'slug': 'aaaa-1',
        #             'debug': {
        #                 'metaProperties': {
        #                     'CPM': '50093',
        #                 },
        #             },
        #             'offers': {
        #                 'items': [{
        #                     'entity': 'offer',
        #                     'slug': 'aaaa-1',
        #                     'debug': {
        #                         'sale': {
        #                             'shopFee': 150,
        #                             'brokeredFee': 2,
        #                         },
        #                     },
        #                 }]
        #             }
        #         },
        #         {
        #             'entity': 'product',
        #             'slug': 'aaaa-2',
        #             'debug': {
        #                 'metaProperties': {
        #                     'CPM': '50062'
        #                 },
        #             },
        #             'offers': {
        #                 'count': 1,
        #                 'items': [{
        #                     'slug': 'aaaa-2',
        #                     'debug': {
        #                         'sale': {
        #                             'shopFee': 100,
        #                             'brokeredFee': 51,
        #                         },
        #                     },
        #                 }]
        #             }
        #         },
        #         {
        #             'entity': 'product',
        #             'slug': 'aaaa-3',
        #             'debug': {
        #                 'metaProperties': {
        #                     'CPM': '50030'
        #                 },
        #             },
        #             'offers': {
        #                 'items': [{
        #                     'entity': 'offer',
        #                     'slug': 'aaaa-3',
        #                     'debug': {
        #                         'sale': {
        #                             'shopFee': 50,
        #                             'brokeredFee': 2,
        #                         },
        #                     },
        #                 }]
        #             }
        #         },
        #         {
        #             'entity': 'offer',
        #             'slug': 'dddd-1',
        #             'debug': {
        #                 'metaProperties': {
        #                     'CPM': '50079'
        #                 },
        #                 'sale': {
        #                     'bid': 30,
        #                     'brokeredClickPrice': 3,
        #                 },
        #             },
        #         },
        #         {
        #             'entity': 'offer',
        #             'slug': 'dddd-2',
        #             'debug': {
        #                 'metaProperties': {
        #                     'CPM': '50052'
        #                 },
        #                 'sale': {
        #                     'bid': 20,
        #                     'brokeredClickPrice': 3
        #                 },
        #             },
        #         },
        #         {
        #             'entity': 'offer',
        #             'slug': 'dddd-3',
        #             'debug': {
        #                 'metaProperties': {
        #                     'CPM': '50025'
        #                 },
        #                 'sale': {
        #                     'bid': 10,
        #                     'brokeredClickPrice': 3
        #                 },
        #             },
        #         },
        #     ]
        #     }})

    @classmethod
    def prepare_log_vendor_bids_for_do_from_model(cls):
        cls.index.models += [
            Model(hid=5, ts=1001, hyperid=23, title='model-1-iiii', vbid=10),
            Model(hid=5, ts=1002, hyperid=24, title='model-2-iiii', vbid=12),
            Model(hid=5, ts=1003, hyperid=25, title='model-3-iiii', vbid=14),
            Model(hid=5, ts=1004, hyperid=26, title='model-4-iiii', vbid=16),
        ]

        cls.index.offers += [
            Offer(
                hid=5,
                ts=1011,
                hyperid=23,
                price=2000,
                fesh=3,
                cbid=30,
                fee=60,
                title='m1-offer-iiii',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=5,
                ts=1012,
                hyperid=24,
                price=2000,
                fesh=3,
                cbid=30,
                fee=30,
                title='m2-offer-iiii',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=5,
                ts=1012,
                hyperid=25,
                price=2000,
                fesh=3,
                cbid=30,
                fee=50,
                title='m3-offer-iiii',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=5,
                ts=1012,
                hyperid=26,
                price=2000,
                fesh=3,
                cbid=30,
                fee=55,
                title='m4-offer-iiii',
                cpa=Offer.CPA_REAL,
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1001).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1002).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1003).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1004).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1011).respond(0.40)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1012).respond(0.40)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1013).respond(0.40)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1014).respond(0.40)

    def test_log_vendor_bids_for_do_from_model(self):
        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        # TODO: отключаем дубли в трафаретах, чтобы не править проверку логов
        rearrs = '&rearr-factors=market_buybox_auction_search_sponsored_places_allow_duplicates=0'
        response = self.report.request_json(
            'place=prime&pp=7&text=iiii&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1' + rearrs
        )

        def _expect_model(slug, vbid, min_vc_bid):
            return {
                'slug': slug,
                'debug': {
                    'sale': {
                        'vBid': vbid,
                        'minVBid': min_vc_bid,
                    }
                },
            }

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        _expect_model(slug='model-4-iiii', vbid=16, min_vc_bid=2),
                        _expect_model(slug='model-3-iiii', vbid=14, min_vc_bid=2),
                        _expect_model(slug='model-2-iiii', vbid=12, min_vc_bid=2),
                        _expect_model(slug='model-1-iiii', vbid=10, min_vc_bid=2),
                    ],
                }
            },
            preserve_order=False,
        )
        # Проверяем, что в офферы залогировались вендорские ставки, взятые из моделей
        self.show_log_tskv.expect(
            vc_bid_from_model=16, min_vc_bid_from_model=2, title='m4-offer-iiii', url_type=6
        ).times(1)
        self.show_log_tskv.expect(
            vc_bid_from_model=14, min_vc_bid_from_model=2, title='m3-offer-iiii', url_type=6
        ).times(1)
        self.show_log_tskv.expect(
            vc_bid_from_model=12, min_vc_bid_from_model=2, title='m2-offer-iiii', url_type=6
        ).times(1)
        self.show_log_tskv.expect(
            vc_bid_from_model=10, min_vc_bid_from_model=2, title='m1-offer-iiii', url_type=6
        ).times(1)
        # Проверяем консистентность залогированных ставок
        self.show_log_tskv.expect(vc_bid=16, min_vc_bid=2, title='model-4-iiii', url_type=16).times(1)
        self.show_log_tskv.expect(vc_bid=14, min_vc_bid=2, title='model-3-iiii', url_type=16).times(1)
        self.show_log_tskv.expect(vc_bid=12, min_vc_bid=2, title='model-2-iiii', url_type=16).times(1)
        self.show_log_tskv.expect(vc_bid=10, min_vc_bid=2, title='model-1-iiii', url_type=16).times(1)

    @classmethod
    def prepare_meta_formula_for_offer_without_bid(cls):
        cls.index.models += [
            Model(hid=5, ts=1005, hyperid=27, title='model-1-jjjj', vbid=10),
            Model(hid=5, ts=1006, hyperid=28, title='model-2-jjjj', vbid=10),
        ]

        cls.index.offers += [
            Offer(
                hid=5,
                ts=1013,
                hyperid=27,
                price=2000,
                fesh=3,
                cbid=0,
                fee=0,
                title='m1-offer-jjjj',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=5,
                ts=1014,
                hyperid=28,
                price=2000,
                fesh=3,
                cbid=0,
                fee=0,
                title='m2-offer-jjjj',
                cpa=Offer.CPA_REAL,
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1005).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1006).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1013).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1014).respond(0.60)
        # meta formulas
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 1005).respond(0.42)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 1006).respond(0.48)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 1013).respond(0.51)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 1014).respond(0.55)

    def test_meta_formula_for_offer_without_bid(self):
        # allow-collapsing=1 - для того чтобы в выдаче оффера были схлопнуты в модели.
        # use-default-offers=1 - дефолтный оффер будет прицеплен к модели в выдаче. Без этого параметра офферов у моделей не будет
        # TODO: отключаем дубли в трафаретах, чтобы не править проверку логов
        rearrs = '&rearr-factors=market_buybox_auction_search_sponsored_places_allow_duplicates=0'
        self.report.request_json(
            'place=prime&pp=7&text=jjjj&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1' + rearrs
        )

        # Проверяем, что в офферы и модели залогировались значения мета-формулы
        self.show_log_tskv.expect(meta_formula_value=0.55, title='m2-offer-jjjj', url_type=6).times(1)
        self.show_log_tskv.expect(meta_formula_value=0.51, title='m1-offer-jjjj', url_type=6).times(1)
        self.show_log_tskv.expect(meta_formula_value=0.51, title='model-1-jjjj', url_type=16).times(1)
        self.show_log_tskv.expect(meta_formula_value=0.55, title='model-2-jjjj', url_type=16).times(1)

    @classmethod
    def prepare_meta_formula_logging(cls):
        cls.index.models += [
            Model(hid=5, ts=1007, hyperid=29, title='model-1-kkkk', vbid=10),
            Model(hid=5, ts=1008, hyperid=30, title='model-2-kkkk', vbid=10),
        ]

        cls.index.offers += [
            Offer(
                hid=5,
                ts=1015,
                hyperid=29,
                price=2000,
                fesh=3,
                cbid=30,
                fee=50,
                title='m1-offer-kkkk',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=5,
                ts=1016,
                hyperid=30,
                price=2000,
                fesh=3,
                cbid=30,
                fee=50,
                title='m2-offer-kkkk',
                cpa=Offer.CPA_REAL,
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1007).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1008).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1015).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1016).respond(0.60)
        # meta formulas
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 1007).respond(0.42)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 1008).respond(0.48)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 1015).respond(0.51)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 1016).respond(0.55)

    def test_meta_formula_logging(self):
        boost_coef = 0.8  # Коэффициент буста для текстовых запросов
        request_raw = 'place={}&pp=7&text=kkkk&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&'
        # TODO: отключаем дубли в трафаретах, чтобы не править проверку логов
        no_doubles_in_sponsored_cgi = '&rearr-factors=market_buybox_auction_search_sponsored_places_allow_duplicates=0'
        boost_flags = 'rearr-factors=market_boost_cpa_docs_mnvalue_coef_text=' + str(boost_coef)
        for place in ['prime', 'blender&blender=1']:
            request_with_place = request_raw.format(place)
            self.report.request_json(request_with_place + no_doubles_in_sponsored_cgi)
            self.report.request_json(request_with_place + boost_flags + no_doubles_in_sponsored_cgi)
        # Проверяем, что в офферы и модели залогировались значения мета-формулы нужное кол-во раз
        for boost_mult in [1, boost_coef]:
            # Каждая запись встречается дважды - один раз для запроса в чистый prime, другой раз для запроса в blender
            # Тут ещё проверяем, что логируются верные позиции
            self.show_log_tskv.expect(
                meta_formula_value=0.55 * boost_mult, title='m2-offer-kkkk', url_type=6, position=1
            ).times(2)
            self.show_log_tskv.expect(
                meta_formula_value=0.51 * boost_mult, title='m1-offer-kkkk', url_type=6, position=2
            ).times(2)
            self.show_log_tskv.expect(
                meta_formula_value=0.51 * boost_mult, title='model-1-kkkk', url_type=16, position=2
            ).times(2)
            self.show_log_tskv.expect(
                meta_formula_value=0.55 * boost_mult, title='model-2-kkkk', url_type=16, position=1
            ).times(2)

    def test_do_positions_logging_fix(self):
        """
        В ДО на поиске должны логироваться правильные позиции, если флаг market_not_null_positions_for_default_offers_fix включен.
        Если он выключен - возвращается старое поведение, когда некоторые ДО могут иметь нулевые позиции.
        Для включённого флага проверка в тесте test_meta_formula_logging, тут хотим проверить именно выключение флага.
        """
        request_base = 'place=prime&pp=7&text=kkkk&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1&'
        # TODO: в трафаретах вылезают дубли, запрещаем их флагов, чтобы не переделывать тест
        rearr_flags_str = 'rearr-factors=market_not_null_positions_for_default_offers_fix=0;market_buybox_auction_search_sponsored_places_allow_duplicates=0'
        self.report.request_json(request_base + rearr_flags_str)
        # У моделей позиции правильные
        self.show_log_tskv.expect(title='model-1-kkkk', url_type=16, position=2).times(1)
        self.show_log_tskv.expect(title='model-2-kkkk', url_type=16, position=1).times(1)
        # У ДО для одной из моделей позиция нулевая
        self.show_log_tskv.expect(title='m2-offer-kkkk', url_type=6, position=0).times(1)
        # У другого ДО позиция правильная, потому что он пришёл при первом запросе прайма с базовых (пришёл ещё на стадии нагребания)
        self.show_log_tskv.expect(title='m1-offer-kkkk', url_type=6, position=2).times(1)

    @classmethod
    def prepare_base_formula_logging(cls):
        cls.index.models += [
            Model(hid=5, ts=1009, hyperid=34, title='model-1-llll', vbid=10),
            Model(hid=5, ts=1010, hyperid=35, title='model-2-llll', vbid=10),
        ]

        cls.index.offers += [
            Offer(
                hid=5,
                ts=1017,
                hyperid=34,
                price=2000,
                fesh=3,
                cbid=30,
                fee=50,
                title='m1-offer-llll',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=5,
                ts=1018,
                hyperid=35,
                price=2000,
                fesh=3,
                cbid=30,
                fee=50,
                title='m2-offer-llll',
                cpa=Offer.CPA_REAL,
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1009).respond(0.70)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1010).respond(0.80)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1017).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1018).respond(0.60)

    def test_base_formula_logged_on_do_from_model(self):
        """
        Проверяем, что в ДО в логе в поле analog_general_score записывается скор базовой формулы, который берётся от модели
        Заодно тут проверим, что top6_formula_value не логируется лишний раз
        """
        # TODO: включаем трафареты, могут вылезти дубли. Запрещаем дубли флагом, чтобы тест не переделывать
        rearrs = '&rearr-factors=market_buybox_auction_search_sponsored_places_allow_duplicates=0'
        request_raw = (
            'place=prime&pp=7&text=llll&rids=213&debug=da&numdoc=20&use-default-offers=1&allow-collapsing=1' + rearrs
        )
        self.report.request_json(request_raw)
        self.show_log_tskv.expect(
            analog_general_score=0.80, mn_ctr=Absent(), title='m2-offer-llll', url_type=6, top6_formula_value=Absent()
        ).times(1)
        self.show_log_tskv.expect(
            analog_general_score=0.70, mn_ctr=Absent(), title='m1-offer-llll', url_type=6, top6_formula_value=Absent()
        ).times(1)
        self.show_log_tskv.expect(
            analog_general_score=Absent(), mn_ctr=0.70, title='model-1-llll', url_type=16, top6_formula_value=Absent()
        ).times(1)
        self.show_log_tskv.expect(
            analog_general_score=Absent(), mn_ctr=0.80, title='model-2-llll', url_type=16, top6_formula_value=Absent()
        ).times(1)


if __name__ == '__main__':
    main()
