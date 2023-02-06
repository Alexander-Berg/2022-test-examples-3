#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Offer,
    Model,
    MarketSku,
    HyperCategory,
    ResaleCondition,
    ResaleGradations,
    ResaleReason,
    Shop,
    UngroupedModel,
    GLType,
    GLValue,
    GLParam,
)
from core.matcher import Absent, EqualToOneOf
from core.testcase import TestCase, main


MODEL_ID = 100
ONLY_NEW_SKU = 1001
SKU_WITH_PERFECT_RESALE_OFFER = 1002
SKU_WITH_EXCELLENT_RESALE_OFFER = 1003
SKU_WITH_WELL_RESALE_OFFER = 1004
SKU_WITH_TWO_RESALE_OFFERS = 1005
SKU_WITH_MANY_RESALE_OFFERS = 1006
HID_FOR_MANY_OFFERS_WITH_RESALES = 1

ALL_SKU_100_WITH_NOT_RESALE = [
    str(ONLY_NEW_SKU),
    str(SKU_WITH_WELL_RESALE_OFFER),
]
ALL_SKU_100_WITH_PERFECT_RESALE = [
    str(SKU_WITH_PERFECT_RESALE_OFFER),
    str(SKU_WITH_TWO_RESALE_OFFERS),
    str(SKU_WITH_MANY_RESALE_OFFERS),
]
ALL_SKU_100_WITH_EXCELLENT_RESALE = [
    str(SKU_WITH_EXCELLENT_RESALE_OFFER),
    str(SKU_WITH_TWO_RESALE_OFFERS),
    str(SKU_WITH_MANY_RESALE_OFFERS),
]
ALL_SKU_100_WITH_WELL_RESALE = [
    str(SKU_WITH_WELL_RESALE_OFFER),
    str(SKU_WITH_MANY_RESALE_OFFERS),
]
ALL_SKU_100_WITH_RESALE = (
    ALL_SKU_100_WITH_PERFECT_RESALE + ALL_SKU_100_WITH_EXCELLENT_RESALE + ALL_SKU_100_WITH_WELL_RESALE
)

new_offers_count = 2
new_offers_with_uniq_sku_count = 2

perfect_offers_count = 4
perfect_offers_with_uniq_sku_count = 3

excellent_offers_count = 3
excellent_offers_with_uniq_sku_count = 3

well_offers_count = 3
well_offers_with_uniq_sku_count = 2

resale_offers_count = perfect_offers_count + excellent_offers_count + well_offers_count
resale_offers_with_uniq_sku_count = 5

all_offers_count = new_offers_count + resale_offers_count
all_offers_with_uniq_sku_count = 6

MODEL_ID_2 = 200
OLD_USED_GOODS_EXCELLENT_SKU = 2001
HID_FOR_OFFER_WITH_USED_GOODS = 2

HID_FOR_OFFER_WITHOUT_RESALE_PARAMS = 3
MODEL_WITH_RESALE_OFFER_WITHOUT_RESALE_PARAMS = 300
SKU_FOR_OFFER_WITHOUT_RESALE_PARAMS = 3001

MODEL_WITHOUT_RESALE_OFFERS = 400
SKU_FOR_OFFER_WITHOUT_RESALES = 4001
HID_FOR_OFFER_WITHOUT_RESALES = 4

COLOR_PARAM_ID = 54321
COLORS_COUNT = 100

SHOP_FOR_SAME_SHOP_ID = 1
CLONE_SHOP_FOR_SAME_SHOP_ID_1 = 2
CLONE_SHOP_FOR_SAME_SHOP_ID_2 = 3
HID_FOR_SAME_SHOP_ID = 5
MODEL_FOR_SAME_SHOP_ID = 500
SKU_FOR_SAME_SHOP_ID = 5001


def group_id_from_mksu(msku):
    return msku - 1000


def glparams_for_msku(msku):
    return [GLParam(param_id=COLOR_PARAM_ID, value=((msku % COLORS_COUNT) or COLORS_COUNT))]


class T(TestCase):
    @staticmethod
    def generate_resale_offers_for_checking_filters():
        offers = []
        for sku in (
            SKU_WITH_PERFECT_RESALE_OFFER,
            SKU_WITH_TWO_RESALE_OFFERS,
            SKU_WITH_MANY_RESALE_OFFERS,
        ):
            offers += [
                Offer(
                    hyperid=MODEL_ID,
                    sku=sku,
                    resale_condition=ResaleCondition.PERFECT,
                    resale_reason=ResaleReason.USED,
                    resale_description="perfect item",
                    title='perfect resale offer #' + str(sku),
                    price=100,
                    ungrouped_model_blue=group_id_from_mksu(sku),
                    glparams=glparams_for_msku(sku),
                )
            ]
        # у SKU_WITH_MANY_RESALE_OFFERS будет два perfect оффера
        offers += [
            Offer(
                hyperid=MODEL_ID,
                sku=SKU_WITH_MANY_RESALE_OFFERS,
                resale_condition=ResaleCondition.PERFECT,
                resale_reason=ResaleReason.SHOWCASE_SAMPLE,
                resale_description="showcase perfect item",
                title='second perfect resale offer #{}'.format(SKU_WITH_MANY_RESALE_OFFERS),
                price=100,
                ungrouped_model_blue=group_id_from_mksu(sku),
                glparams=glparams_for_msku(sku),
                waremd5='PerfectOfferWithOferId',
            )
        ]
        for sku in (
            SKU_WITH_EXCELLENT_RESALE_OFFER,
            SKU_WITH_TWO_RESALE_OFFERS,
            SKU_WITH_MANY_RESALE_OFFERS,
        ):
            offers += [
                Offer(
                    hyperid=MODEL_ID,
                    sku=sku,
                    resale_condition=ResaleCondition.EXCELLENT,
                    resale_reason=ResaleReason.RESTORED,
                    resale_description="excellent item",
                    title='excellent resale offer #' + str(sku),
                    price=100,
                    ungrouped_model_blue=group_id_from_mksu(sku),
                    glparams=glparams_for_msku(sku),
                )
            ]
        for sku in (
            SKU_WITH_WELL_RESALE_OFFER,
            SKU_WITH_MANY_RESALE_OFFERS,
        ):
            offers += [
                Offer(
                    hyperid=MODEL_ID,
                    sku=sku,
                    resale_condition=ResaleCondition.WELL,
                    resale_reason=ResaleReason.REDUCTION,
                    resale_description="well item",
                    title='well resale offer #' + str(sku),
                    price=100,
                    ungrouped_model_blue=group_id_from_mksu(sku),
                    glparams=glparams_for_msku(sku),
                )
            ]
        # оффер с неизвестным resale_reason
        offers += [
            Offer(
                hyperid=MODEL_ID,
                sku=SKU_WITH_MANY_RESALE_OFFERS,
                resale_condition=ResaleCondition.WELL,
                resale_reason=100500,
                resale_description="unknown reason item",
                title='unknown reason resale offer #' + str(SKU_WITH_MANY_RESALE_OFFERS),
                price=100,
                ungrouped_model_blue=group_id_from_mksu(SKU_WITH_MANY_RESALE_OFFERS),
                glparams=glparams_for_msku(sku),
            )
        ]
        return offers

    @staticmethod
    def generate_resale_params(enable_resale_goods, rearr, resale_goods, resale_goods_condition):
        req = 'debug=1'
        if enable_resale_goods is not None:
            req += '&enable-resale-goods={}'.format(enable_resale_goods)
        if rearr is not None:
            req += '&rearr-factors=market_enable_resale_goods={}'.format(rearr)
        if resale_goods is not None:
            req += '&resale_goods={}'.format(resale_goods)
        if resale_goods_condition is not None:
            req += '&resale_goods_condition={}'.format(resale_goods_condition)
        return req

    @staticmethod
    def generate_request_prime(
        enable_resale_goods='1',
        rearr='1',
        resale_goods=None,
        resale_goods_condition=None,
        hid=HID_FOR_MANY_OFFERS_WITH_RESALES,
    ):
        return (
            'place=prime&allow-collapsing=1&allow-ungrouping=1&'
            + "hid={}&".format(str(hid))
            + T.generate_resale_params(enable_resale_goods, rearr, resale_goods, resale_goods_condition)
        )

    @staticmethod
    def generate_request_productoffers(
        enable_resale_goods='1',
        rearr='1',
        resale_goods=None,
        resale_goods_condition=None,
        hid=HID_FOR_MANY_OFFERS_WITH_RESALES,
        hyperid=MODEL_ID,
    ):
        return (
            'place=productoffers&numdoc=100&'
            + "hid={}&".format(str(hid))
            + "hyperid={}&".format(str(hyperid))
            + T.generate_resale_params(enable_resale_goods, rearr, resale_goods, resale_goods_condition)
        )

    @classmethod
    def prepare(cls):
        cls.index.hypertree += [
            HyperCategory(hid=HID_FOR_MANY_OFFERS_WITH_RESALES),
            HyperCategory(hid=HID_FOR_OFFER_WITH_USED_GOODS),
            HyperCategory(hid=HID_FOR_OFFER_WITHOUT_RESALE_PARAMS),
            HyperCategory(hid=HID_FOR_OFFER_WITHOUT_RESALES),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=COLOR_PARAM_ID,
                hid=HID_FOR_MANY_OFFERS_WITH_RESALES,
                gltype=GLType.ENUM,
                name='Цвет',
                values=[GLValue(value_id=id) for id in range(1, COLORS_COUNT + 1)],
                model_filter_index=1,
                cluster_filter=True,
            ),
        ]

        ungrouped_blue = list()
        for msku in (
            ONLY_NEW_SKU,
            SKU_WITH_PERFECT_RESALE_OFFER,
            SKU_WITH_EXCELLENT_RESALE_OFFER,
            SKU_WITH_WELL_RESALE_OFFER,
            SKU_WITH_TWO_RESALE_OFFERS,
            SKU_WITH_MANY_RESALE_OFFERS,
        ):
            gpoup_id = group_id_from_mksu(msku)
            cls.index.mskus += [
                MarketSku(hyperid=MODEL_ID, sku=msku, ungrouped_model_blue=gpoup_id, glparams=glparams_for_msku(msku))
            ]
            ungrouped_blue += [UngroupedModel(group_id=gpoup_id, key=str(msku))]

        cls.index.models += [
            Model(hyperid=MODEL_ID, hid=HID_FOR_MANY_OFFERS_WITH_RESALES, ungrouped_blue=ungrouped_blue)
        ]
        cls.index.offers = [
            Offer(
                hyperid=MODEL_ID,
                sku=ONLY_NEW_SKU,
                title='not resale offer #{}'.format(ONLY_NEW_SKU),
                price=150,
                ungrouped_model_blue=group_id_from_mksu(ONLY_NEW_SKU),
                waremd5='ItsNewOfferWithOfferId',
                glparams=glparams_for_msku(ONLY_NEW_SKU),
            ),
            Offer(
                hyperid=MODEL_ID,
                sku=SKU_WITH_WELL_RESALE_OFFER,
                title='not resale offer #{}'.format(SKU_WITH_WELL_RESALE_OFFER),
                price=200,
                ungrouped_model_blue=group_id_from_mksu(SKU_WITH_WELL_RESALE_OFFER),
                glparams=glparams_for_msku(SKU_WITH_WELL_RESALE_OFFER),
            ),
        ]
        cls.index.offers += cls.generate_resale_offers_for_checking_filters()

        # во второй категории есть скю со старой б/у реализацией
        cls.index.models += [Model(hyperid=MODEL_ID_2, hid=HID_FOR_OFFER_WITH_USED_GOODS)]
        cls.index.mskus += [
            MarketSku(hyperid=MODEL_ID_2, sku=OLD_USED_GOODS_EXCELLENT_SKU, visual_condition='excellent'),
        ]
        # есть один б/у оффер, приматченный к этой скю, и один обычный оффер, приматченный только к модели
        cls.index.offers += [
            Offer(
                hyperid=MODEL_ID_2,
                sku=OLD_USED_GOODS_EXCELLENT_SKU,
                title='old used goods excellent offer',
                visual_condition='excellent',
            ),
            Offer(hyperid=MODEL_ID_2, title='simple offer in model {}'.format(MODEL_ID_2)),
        ]

        # в третьей категории лежит один ресейл оффер, который не имеет ресейл параметров.
        # используется, чтобы проверить чтение репортом индексаторного офферного флага
        cls.index.models += [
            Model(hyperid=MODEL_WITH_RESALE_OFFER_WITHOUT_RESALE_PARAMS, hid=HID_FOR_OFFER_WITHOUT_RESALE_PARAMS)
        ]
        cls.index.mskus += [
            MarketSku(hyperid=MODEL_WITH_RESALE_OFFER_WITHOUT_RESALE_PARAMS, sku=SKU_FOR_OFFER_WITHOUT_RESALE_PARAMS),
        ]
        cls.index.offers += [
            Offer(
                hyperid=MODEL_WITH_RESALE_OFFER_WITHOUT_RESALE_PARAMS,
                sku=SKU_FOR_OFFER_WITHOUT_RESALE_PARAMS,
                title='resale offer without resale params',
                is_resale=True,
            ),
        ]

        # в четвертой категории лежит один НЕ ресейл оффер.
        cls.index.models += [Model(hyperid=MODEL_WITHOUT_RESALE_OFFERS, hid=HID_FOR_OFFER_WITHOUT_RESALES)]
        cls.index.mskus += [
            MarketSku(hyperid=MODEL_WITHOUT_RESALE_OFFERS, sku=SKU_FOR_OFFER_WITHOUT_RESALES),
        ]
        cls.index.offers += [
            Offer(
                hyperid=MODEL_WITHOUT_RESALE_OFFERS,
                sku=SKU_FOR_OFFER_WITHOUT_RESALES,
                title='not resale offer',
            ),
        ]

        # в пятой категории лежат оффера одного магазина, нужны для теста фильтрации в productoffers
        cls.index.shops += [
            Shop(fesh=SHOP_FOR_SAME_SHOP_ID, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(
                fesh=CLONE_SHOP_FOR_SAME_SHOP_ID_1,
                main_fesh=SHOP_FOR_SAME_SHOP_ID,
                priority_region=213,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=CLONE_SHOP_FOR_SAME_SHOP_ID_2,
                main_fesh=SHOP_FOR_SAME_SHOP_ID,
                priority_region=213,
                cpa=Shop.CPA_REAL,
            ),
        ]
        cls.index.models += [Model(hyperid=MODEL_FOR_SAME_SHOP_ID, hid=HID_FOR_SAME_SHOP_ID)]
        cls.index.mskus += [
            MarketSku(hyperid=MODEL_FOR_SAME_SHOP_ID, sku=SKU_FOR_SAME_SHOP_ID),
        ]
        cls.index.offers += [
            Offer(
                hyperid=MODEL_FOR_SAME_SHOP_ID,
                sku=SKU_FOR_SAME_SHOP_ID,
                title='well resale offer for filter test',
                fesh=SHOP_FOR_SAME_SHOP_ID,
                supplier_id=SHOP_FOR_SAME_SHOP_ID,
                resale_condition=ResaleCondition.WELL,
                resale_reason=ResaleReason.REDUCTION,
                resale_description="well item",
                price=100,
                ts=100231,
                waremd5='wellOfferWAREMD5______',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=MODEL_FOR_SAME_SHOP_ID,
                sku=SKU_FOR_SAME_SHOP_ID,
                title='excellent resale offer1 for filter test',
                fesh=CLONE_SHOP_FOR_SAME_SHOP_ID_1,
                supplier_id=CLONE_SHOP_FOR_SAME_SHOP_ID_1,
                resale_condition=ResaleCondition.EXCELLENT,
                resale_reason=ResaleReason.REDUCTION,
                resale_description="excellent item",
                price=101,
                ts=100232,
                waremd5='excellentWAREMD5______',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=MODEL_FOR_SAME_SHOP_ID,
                sku=SKU_FOR_SAME_SHOP_ID,
                title='excellent resale offer2 for filter test',
                fesh=CLONE_SHOP_FOR_SAME_SHOP_ID_2,
                supplier_id=CLONE_SHOP_FOR_SAME_SHOP_ID_2,
                resale_condition=ResaleCondition.EXCELLENT,
                resale_reason=ResaleReason.REDUCTION,
                resale_description="excellent item",
                price=102,
                ts=100233,
                waremd5='excellent2WAREMD5_____',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=MODEL_FOR_SAME_SHOP_ID,
                sku=SKU_FOR_SAME_SHOP_ID,
                title='not resale offer for filter test',
                price=150,
                fesh=SHOP_FOR_SAME_SHOP_ID,
                supplier_id=SHOP_FOR_SAME_SHOP_ID,
                ts=100234,
                waremd5='not_resaleWAREMD5_____',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=MODEL_FOR_SAME_SHOP_ID,
                sku=SKU_FOR_SAME_SHOP_ID,
                title='not resale offer 2 for filter test',
                price=152,
                fesh=CLONE_SHOP_FOR_SAME_SHOP_ID_2,
                supplier_id=CLONE_SHOP_FOR_SAME_SHOP_ID_2,
                ts=100235,
                waremd5='not_resale2WAREMD5____',
                cpa=Offer.CPA_REAL,
            ),
        ]

    def _check_total_and_debug_filters(self, response, total_offers, resale_goods, resale_goods_condition):
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'totalOffers': total_offers,
                },
            },
        )
        # может быть в каком-то подзапросе
        self.assertFragmentIn(
            response,
            {
                'filters': {
                    'RESALE_GOODS': resale_goods,
                    'RESALE_GOODS_CONDITION': resale_goods_condition,
                },
            },
        )

    def test_filtration(self):
        # проверяем фильтр resale_goods
        # 1) ничего не фильтруем
        response = self.report.request_json(self.generate_request_productoffers())
        self._check_total_and_debug_filters(response, all_offers_count, Absent(), Absent())
        # 2) отсеиваем все б/у оффера
        response = self.report.request_json(self.generate_request_productoffers(resale_goods='resale_new'))
        self._check_total_and_debug_filters(response, new_offers_count, resale_offers_count, Absent())
        # 3) оставляем только б/у оффера
        response = self.report.request_json(self.generate_request_productoffers(resale_goods='resale_resale'))
        self._check_total_and_debug_filters(response, resale_offers_count, new_offers_count, Absent())
        # 4) чекнуты оба значения фильтра
        response = self.report.request_json(
            self.generate_request_productoffers(resale_goods='resale_new,resale_resale')
        )
        self._check_total_and_debug_filters(response, all_offers_count, Absent(), Absent())

        # проверяем фильтр resale_goods_condition
        # 1) оставляем только perfect оффера
        response = self.report.request_json(
            self.generate_request_productoffers(resale_goods_condition='resale_perfect')
        )
        self._check_total_and_debug_filters(
            response, perfect_offers_count, Absent(), all_offers_count - perfect_offers_count
        )
        # 2) оставляем только excellent оффера
        response = self.report.request_json(
            self.generate_request_productoffers(resale_goods_condition='resale_excellent')
        )
        self._check_total_and_debug_filters(
            response, excellent_offers_count, Absent(), all_offers_count - excellent_offers_count
        )
        # 3) оставляем только well оффера
        response = self.report.request_json(self.generate_request_productoffers(resale_goods_condition='resale_well'))
        self._check_total_and_debug_filters(response, well_offers_count, Absent(), all_offers_count - well_offers_count)
        # 4) perfect + excellent
        response = self.report.request_json(
            self.generate_request_productoffers(resale_goods_condition='resale_perfect,resale_excellent')
        )
        self._check_total_and_debug_filters(
            response,
            perfect_offers_count + excellent_offers_count,
            Absent(),
            all_offers_count - perfect_offers_count - excellent_offers_count,
        )
        # 5) выбраны все 3 б/у значения, должен отфильтроваться только новый оффер
        response = self.report.request_json(
            self.generate_request_productoffers(resale_goods_condition='resale_perfect,resale_excellent,resale_well')
        )
        self._check_total_and_debug_filters(response, resale_offers_count, Absent(), new_offers_count)

    def ignore_old_flags(self):
        '''Проверка, что при включенном enable-resale-goods старый show-used-goods=1 игнорится'''

        req = 'place=productoffers&hyperid=200&show-used-goods=1'
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'slug': 'old-used-goods-excellent-offer',
                    },
                    {
                        'slug': 'simple-offer-in-model-200',
                    },
                ]
            },
        )
        self.assertFragmentNotIn(response, {'filters': {'USED_GOODS': 1}})

        # c enable-resale-goods=1 первый оффер не будет показан
        response = self.report.request_json(req + '&enable-resale-goods=1&debug=1')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'slug': 'simple-offer-in-model-200',
                    },
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {'filters': {'USED_GOODS': 1}})

    def test_default_offers_on_prime(self):
        req = self.generate_request_prime() + '&use-default-offers=1'
        response = self.report.request_json(req)
        # Без пессимизации б/у офферов для скю 1004 был бы выбран самый дешевый оффер.
        # Проверяем, что пессимизация применилась, и выбрался новый оффер, пусть и подороже.
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'marketSku': '1004',
                'slug': 'not-resale-offer-1004',
                'prices': {
                    'value': '200',
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'entity': 'offer',
                'marketSku': '1004',
                'prices': {
                    'value': '100',
                },
            },
        )

        # Проверяем, что если у скю нет новых офферов, то б/у спокойно попадает в ДО
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'marketSku': '1003',
                'slug': 'excellent-resale-offer-1003',
            },
        )

        # Если включен фильтр resale_goods=resale_resale, то и у 1004 ДО станет б/у
        response = self.report.request_json(req + '&resale_goods=resale_resale')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'marketSku': '1004',
                'slug': 'well-resale-offer-1004',
                'prices': {
                    'value': '100',
                },
            },
        )

    def test_show_resale_filters_by_cgi_and_rearr(self):
        '''Проверка показа фильтра при отсутствии/наличии cgi и rearr параметров'''
        for enable_resale_goods in (None, '0', '1'):
            for market_enable_resale_goods in (None, '0', '1'):
                response = self.report.request_json(
                    self.generate_request_prime(
                        enable_resale_goods=enable_resale_goods, rearr=market_enable_resale_goods
                    )
                )
                if enable_resale_goods == '1' and market_enable_resale_goods != '0':
                    self.assertFragmentIn(
                        response,
                        {
                            'filters': [
                                {'id': 'resale_goods'},
                                {'id': 'resale_goods_condition'},
                                {'id': str(COLOR_PARAM_ID)},  # на прайме гл фильтры идут после resale_goods_condition
                            ]
                        },
                        preserve_order=True,
                    )
                else:
                    self.assertFragmentNotIn(response, {'filters': [{'id': 'resale_goods'}]})
                    self.assertFragmentNotIn(response, {'filters': [{'id': 'resale_goods_condition'}]})

    @classmethod
    def prepare_prime_outputs_resale_offer_params(cls):
        cls.index.resale_gradations += [ResaleGradations(90401, "перфект", "экселлент", "сойдет")]
        cls.index.resale_reasons += [
            ResaleReason(ResaleReason.USED, "used"),
            ResaleReason(ResaleReason.RESTORED, "restored"),
            ResaleReason(ResaleReason.SHOWCASE_SAMPLE, "showcase"),
            ResaleReason(ResaleReason.REDUCTION, "reduction"),
            ResaleReason(ResaleReason.UNKNOWN, "unknown_resale_reason"),
        ]

    def test_prime_outputs_resale_offer_params(self):
        response = self.report.request_json(self.generate_request_productoffers())

        expected = [
            ("not resale offer #1001", False, None, None, None, None, None),
            ("not resale offer #1004", False, None, None, None, None, None),
            ("perfect resale offer #1006", True, "resale_perfect", "перфект", "1", "used", "perfect item"),
            (
                "second perfect resale offer #1006",
                True,
                "resale_perfect",
                "перфект",
                "3",
                "showcase",
                "showcase perfect item",
            ),
            ("excellent resale offer #1006", True, "resale_excellent", "экселлент", "2", "restored", "excellent item"),
            ("well resale offer #1006", True, "resale_well", "сойдет", "4", "reduction", "well item"),
            (
                "unknown reason resale offer #1006",
                True,
                "resale_well",
                "сойдет",
                "100500",
                "unknown_resale_reason",
                "unknown reason item",
            ),
        ]

        for (title, is_resale, condition, condition_text, reason, reason_text, description) in expected:
            if is_resale:
                self.assertFragmentIn(
                    response,
                    {
                        "titles": {"raw": title},
                        'isResale': is_resale,
                        'resaleSpecs': {
                            "condition": {"value": condition, "text": condition_text},
                            "reason": {"value": reason, "text": reason_text},
                            "description": description,
                        },
                    },
                )
            else:
                self.assertFragmentIn(
                    response, {"titles": {"raw": title}, 'isResale': is_resale, 'resaleSpecs': Absent()}
                )

    def test_report_uses_offer_resale_flag(self):
        '''
        Проверяем, что репорт читает офферный флаг ресейлового товара, а не вычисляет его по ресейл параметрам
        В этом месте resaleSpecs будут присутствовать, но будут пустые, потому что других данных нет
        '''
        request = 'place=productoffers&hyperid=300&enable-resale-goods=1&rearr-factors=market_enable_resale_goods=1&resale_goods=resale_resale&debug=1'
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "titles": {"raw": "resale offer without resale params"},
                        'isResale': True,
                        "resaleSpecs": {
                            "condition": {"text": "", "value": ""},
                            "description": "",
                            "reason": {"text": "", "value": ""},
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_check_resale_goods_filter(self):
        '''Проверка изменений в зависимости от зажатых опций фильтра "Состояние"'''

        for resale_goods_new in (True, False):
            for resale_goods_resale in (True, False):
                resale_goods_new_text = 'resale_new' if resale_goods_new else ''
                resale_goods_resale_text = 'resale_resale' if resale_goods_resale else ''
                response = self.report.request_json(
                    self.generate_request_prime(
                        resale_goods=','.join(filter(None, (resale_goods_new_text, resale_goods_resale_text)))
                    )
                )

                show_only_new = resale_goods_new and not resale_goods_resale
                show_only_resale = not resale_goods_new and resale_goods_resale

                self.assertFragmentIn(
                    response,
                    {
                        'id': 'resale_goods',
                        'name': 'Состояние товара',
                        'type': 'enum',
                        'values': [
                            {
                                'id': 'resale_new',
                                'value': 'Новый',
                                'found': 0 if show_only_resale else new_offers_with_uniq_sku_count,
                                'initialFound': new_offers_with_uniq_sku_count,
                                'checked': True if resale_goods_new else Absent(),
                            },
                            {
                                'id': 'resale_resale',
                                'value': 'Ресейл',
                                'found': 0 if show_only_new else resale_offers_with_uniq_sku_count,
                                'initialFound': resale_offers_with_uniq_sku_count,
                                'checked': True if resale_goods_resale else Absent(),
                            },
                        ],
                    },
                )

                if show_only_new:
                    self.assertFragmentNotIn(response, {'filters': [{'id': 'resale_goods_condition'}]})
                else:
                    self.assertFragmentIn(response, {'filters': [{'id': 'resale_goods_condition'}]})

    @staticmethod
    def _default_offer(sku):
        return dict(
            {
                'entity': 'product',
                'id': 100,
                'offers': {
                    'items': [
                        {
                            'marketSku': str(sku),
                        },
                    ],
                },
            }
        )

    def test_check_resale_goods_condition_filter(self):
        '''Проверка изменений в зависимости от зажатых опций фильтра "Внешний вид ресейл товаров"'''

        for check_resale_goods_perfect in (True, False):
            for check_resale_goods_excellent in (True, False):
                for check_resale_goods_well in (True, False):
                    perfect_text = 'resale_perfect' if check_resale_goods_perfect else ''
                    excellent_text = 'resale_excellent' if check_resale_goods_excellent else ''
                    well_text = 'resale_well' if check_resale_goods_well else ''
                    response = self.report.request_json(
                        self.generate_request_prime(
                            resale_goods='resale_resale',
                            resale_goods_condition=','.join(filter(None, (perfect_text, excellent_text, well_text))),
                        )
                        + '&use-default-offers=1'
                    )

                    if (
                        not check_resale_goods_perfect
                        and not check_resale_goods_excellent
                        and not check_resale_goods_well
                    ):
                        perfect, excellent, well = True, True, True
                    else:
                        perfect, excellent, well = (
                            check_resale_goods_perfect,
                            check_resale_goods_excellent,
                            check_resale_goods_well,
                        )

                    self.assertFragmentIn(
                        response,
                        {
                            'id': 'resale_goods_condition',
                            'name': 'Внешний вид',
                            'type': 'enum',
                            'values': [
                                {
                                    'id': 'resale_perfect',
                                    'value': 'Как новый',
                                    'found': perfect_offers_with_uniq_sku_count if perfect else 0,
                                    'initialFound': perfect_offers_with_uniq_sku_count,
                                    'checked': True if check_resale_goods_perfect else Absent(),
                                },
                                {
                                    'id': 'resale_excellent',
                                    'value': 'Отличный',
                                    'found': excellent_offers_with_uniq_sku_count if excellent else 0,
                                    'initialFound': excellent_offers_with_uniq_sku_count,
                                    'checked': True if check_resale_goods_excellent else Absent(),
                                },
                                {
                                    'id': 'resale_well',
                                    'value': 'Хороший',
                                    'found': well_offers_with_uniq_sku_count if well else 0,
                                    'initialFound': well_offers_with_uniq_sku_count,
                                    'checked': True if check_resale_goods_well else Absent(),
                                },
                            ],
                        },
                    )

                    if perfect:
                        for sku in (
                            SKU_WITH_PERFECT_RESALE_OFFER,
                            SKU_WITH_TWO_RESALE_OFFERS,
                            SKU_WITH_MANY_RESALE_OFFERS,
                        ):
                            self.assertFragmentIn(response, self._default_offer(sku))
                    if excellent:
                        for sku in (
                            SKU_WITH_EXCELLENT_RESALE_OFFER,
                            SKU_WITH_TWO_RESALE_OFFERS,
                            SKU_WITH_MANY_RESALE_OFFERS,
                        ):
                            self.assertFragmentIn(response, self._default_offer(sku))
                    if well:
                        for sku in (SKU_WITH_WELL_RESALE_OFFER, SKU_WITH_MANY_RESALE_OFFERS):
                            self.assertFragmentIn(response, self._default_offer(sku))

    def test_jump_table(self):
        '''Проверка заполнения таблицы переходов б/у у модельки с б/у офферами'''

        response = self.report.request_json(self.generate_request_productoffers())
        self.assertFragmentIn(
            response,
            [
                {
                    'id': 'resale_goods',
                    'values': [
                        {
                            'id': 'resale_new',
                            'marketSku': EqualToOneOf(*ALL_SKU_100_WITH_NOT_RESALE),
                        },
                        {
                            'id': 'resale_resale',
                            'marketSku': EqualToOneOf(*ALL_SKU_100_WITH_RESALE),
                        },
                    ],
                },
                {
                    # в карте переходов resale_goods_condition идет после гл параметров
                    'id': str(COLOR_PARAM_ID),
                },
                {
                    'id': 'resale_goods_condition',
                    'values': [
                        {
                            'id': 'resale_perfect',
                            'marketSku': EqualToOneOf(*ALL_SKU_100_WITH_PERFECT_RESALE),
                        },
                        {
                            'id': 'resale_excellent',
                            'marketSku': EqualToOneOf(*ALL_SKU_100_WITH_EXCELLENT_RESALE),
                        },
                        {
                            'id': 'resale_well',
                            'marketSku': EqualToOneOf(*ALL_SKU_100_WITH_WELL_RESALE),
                        },
                    ],
                },
            ],
            preserve_order=True,
        )

    def test_condition_filter_hiding(self):
        '''При resale_goods=resale_new фильтр resale_goods_condition скрывается'''

        for resale_goods in (None, 'resale_resale', 'resale_new'):
            response_prime = self.report.request_json(self.generate_request_prime(resale_goods=resale_goods))
            response_productoffers = self.report.request_json(
                self.generate_request_productoffers(resale_goods=resale_goods)
            )
            for response in (response_prime, response_productoffers):
                if resale_goods == 'resale_new':
                    self.assertFragmentNotIn(response, {'id': 'resale_goods_condition'})
                else:
                    self.assertFragmentIn(response, {'id': 'resale_goods_condition'})

    def test_missing_resale_jump_table(self):
        '''Проверка отсутствия таблицы переходов б/у у модельки без б/у офферов'''

        response = self.report.request_json(
            self.generate_request_productoffers(hid=HID_FOR_OFFER_WITHOUT_RESALES, hyperid=MODEL_WITHOUT_RESALE_OFFERS)
        )

        self.assertFragmentNotIn(
            response,
            {
                'filters': [
                    {
                        'id': 'resale_goods',
                    },
                ],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'filters': [
                    {
                        'id': 'resale_goods_condition',
                    },
                ],
            },
        )

    def _test_market_sku_in_jump_table(
        self,
        market_sku,
        resale_goods_condition,
        sku_resale_new,
        sku_resale_resale,
        sku_resale_perfect,
        sku_resale_excellent,
        sku_resale_well,
    ):
        def gen_fuzzy(value_sku):
            if market_sku is None or value_sku is None:
                return Absent()
            if isinstance(value_sku, EqualToOneOf):
                return Absent() if str(market_sku) in value_sku.values else True
            return Absent() if str(market_sku) == value_sku else True

        req = self.generate_request_productoffers(resale_goods_condition=resale_goods_condition)
        if market_sku is not None:
            req += '&market-sku={}'.format(market_sku)
        response = self.report.request_json(req)

        if sku_resale_new is not None:
            self.assertFragmentIn(
                response,
                {
                    'id': 'resale_goods',
                    'values': [
                        {
                            'id': 'resale_new',
                            'marketSku': sku_resale_new,
                            'fuzzy': gen_fuzzy(sku_resale_new),
                            'additionalParams': [
                                {
                                    'name': 'resale_goods_condition',
                                    'value': '',
                                },
                            ]
                            if resale_goods_condition is not None
                            else Absent(),
                        },
                    ],
                },
            )
        if sku_resale_resale is not None:
            self.assertFragmentIn(
                response,
                {
                    'id': 'resale_goods',
                    'values': [
                        {
                            'id': 'resale_resale',
                            'marketSku': sku_resale_resale,
                            'fuzzy': gen_fuzzy(sku_resale_resale),
                        },
                    ],
                },
            )
        if sku_resale_perfect is not None:
            self.assertFragmentIn(
                response,
                {
                    'id': 'resale_goods_condition',
                    'values': [
                        {
                            'id': 'resale_perfect',
                            'marketSku': sku_resale_perfect,
                            'fuzzy': gen_fuzzy(sku_resale_perfect),
                        },
                    ],
                },
            )
        if sku_resale_excellent is not None:
            self.assertFragmentIn(
                response,
                {
                    'id': 'resale_goods_condition',
                    'values': [
                        {
                            'id': 'resale_excellent',
                            'marketSku': sku_resale_excellent,
                            'fuzzy': gen_fuzzy(sku_resale_excellent),
                        },
                    ],
                },
            )
        if sku_resale_well is not None:
            self.assertFragmentIn(
                response,
                {
                    'id': 'resale_goods_condition',
                    'values': [
                        {
                            'id': 'resale_well',
                            'marketSku': sku_resale_well,
                            'fuzzy': gen_fuzzy(sku_resale_well),
                        },
                    ],
                },
            )

    def test_jump_table_without_sku(self):
        '''Проверка для продуктофферс без market-sku в запросе.
        Не зависимо от выбранного фильтра, у всех скю будет одинаковая вероятность попадания в marketSku
        '''

        for resale_goods_condition in (None, 'resale_perfect', 'resale_excellent', 'resale_well'):
            self._test_market_sku_in_jump_table(
                market_sku=None,
                resale_goods_condition=resale_goods_condition,
                sku_resale_new=EqualToOneOf(*ALL_SKU_100_WITH_NOT_RESALE),
                sku_resale_resale=EqualToOneOf(*ALL_SKU_100_WITH_RESALE),
                sku_resale_perfect=EqualToOneOf(*ALL_SKU_100_WITH_PERFECT_RESALE),
                sku_resale_excellent=EqualToOneOf(*ALL_SKU_100_WITH_EXCELLENT_RESALE),
                sku_resale_well=EqualToOneOf(*ALL_SKU_100_WITH_WELL_RESALE),
            )

    def test_jump_table_with_sku(self):
        # Проверка для запросов с market-sku. У него приоритет при выборе marketSku

        for resale_goods_condition in (None, 'resale_perfect', 'resale_excellent', 'resale_well'):
            # У SKU_WITH_WELL_RESALE_OFFER есть и новый оффер, и хороший б/у.
            req_sku = str(SKU_WITH_WELL_RESALE_OFFER)
            self._test_market_sku_in_jump_table(
                market_sku=SKU_WITH_WELL_RESALE_OFFER,
                resale_goods_condition=resale_goods_condition,
                sku_resale_new=req_sku,
                sku_resale_resale=req_sku,
                sku_resale_perfect=EqualToOneOf(*ALL_SKU_100_WITH_PERFECT_RESALE),
                sku_resale_excellent=EqualToOneOf(*ALL_SKU_100_WITH_EXCELLENT_RESALE),
                sku_resale_well=req_sku,
            )

            # У SKU_WITH_MANY_RESALE_OFFERS есть б/у оффера всех типов
            req_sku = str(SKU_WITH_MANY_RESALE_OFFERS)
            self._test_market_sku_in_jump_table(
                market_sku=SKU_WITH_MANY_RESALE_OFFERS,
                resale_goods_condition=resale_goods_condition,
                sku_resale_new=EqualToOneOf(*ALL_SKU_100_WITH_NOT_RESALE),
                sku_resale_resale=req_sku,
                sku_resale_perfect=req_sku,
                sku_resale_excellent=req_sku,
                sku_resale_well=req_sku,
            )

    def test_gl_params_for_new(self):
        '''Проверка карты переходов для новых товаров'''

        req = self.generate_request_productoffers(resale_goods='resale_new')
        req += '&market-sku={}'.format(ONLY_NEW_SKU)
        response = self.report.request_json(req)

        # фильтра resale_condition не должно быть
        self.assertFragmentNotIn(response, {'filters': [{'id': 'resale_goods_condition'}]})

        # у этой модели 6 скю разных цветов, новые оффера есть только у двух из них
        for sku in range(ONLY_NEW_SKU, SKU_WITH_MANY_RESALE_OFFERS + 1):
            self.assertFragmentIn(
                response,
                {
                    'id': str(sku % 100),
                    'marketSku': str(sku),
                    'found': 1,  # даже у скю без новых офферов, это признак наличия какого-либо оффера
                    # список отсутствует т.к resale_goods или как в исходном запросе, или вообще нет новых офферов
                    'additionalParams': Absent(),
                    'active': sku in (1001, 1004),
                },
            )

    def test_gl_params_for_resale(self):
        '''Проверка карты переходов для ресейл товаров'''

        req = self.generate_request_productoffers(resale_goods='resale_resale', resale_goods_condition='resale_well')
        req += '&market-sku={}'.format(SKU_WITH_WELL_RESALE_OFFER)
        response = self.report.request_json(req)

        self.assertFragmentIn(
            response,
            [
                {
                    'id': '1',
                    'marketSku': str(ONLY_NEW_SKU),
                    'additionalParams': Absent(),  # у этой скю нет никаких б/у офферов
                    'active': False,
                    'fuzzy': Absent(),
                },
                {
                    'id': '2',
                    'marketSku': str(SKU_WITH_PERFECT_RESALE_OFFER),
                    'additionalParams': [
                        {
                            'name': 'resale_goods_condition',
                            'value': 'resale_perfect',
                        },
                    ],
                    'active': True,  # нет well оффера, но вообще б/у оффера есть
                    'fuzzy': True,
                },
                {
                    'id': '3',
                    'marketSku': str(SKU_WITH_EXCELLENT_RESALE_OFFER),
                    'additionalParams': [
                        {
                            'name': 'resale_goods_condition',
                            'value': 'resale_excellent',
                        },
                    ],
                    'active': True,
                    'fuzzy': True,
                },
                {
                    'id': '4',
                    'marketSku': str(SKU_WITH_WELL_RESALE_OFFER),
                    'additionalParams': Absent(),  # состояние well, как и в запросе
                    'active': True,
                    'fuzzy': Absent(),
                },
                {
                    'id': '5',
                    'marketSku': str(SKU_WITH_TWO_RESALE_OFFERS),
                    # у этой скю есть perfect и excellent оффера, выбираем лучший
                    'additionalParams': [
                        {
                            'name': 'resale_goods_condition',
                            'value': 'resale_perfect',
                        },
                    ],
                    'active': True,
                    'fuzzy': True,
                },
                {
                    'id': '6',
                    'marketSku': str(SKU_WITH_MANY_RESALE_OFFERS),
                    # у этой скю есть б/у оффера всех типов, но выбираем не лучший, а запрашиваемый
                    'additionalParams': Absent(),
                    'active': True,
                    'fuzzy': Absent(),
                },
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(response, {'id': 'resale_goods'})

    def test_gl_params_for_empty_filters_with_sku(self):
        '''Если отсутствуют ресейл фильтры, то не понятно на какой мы вкладке.
        Не должно быть полей с active=false, а в additionalParams всегда должны быть фильтры'''

        req = self.generate_request_productoffers()
        response = self.report.request_json(req)

        resale_new = {'name': 'resale_goods', 'value': 'resale_new'}
        resale_resale = {'name': 'resale_goods', 'value': 'resale_resale'}
        resale_perfect = {'name': 'resale_goods_condition', 'value': 'resale_perfect'}
        resale_excellent = {'name': 'resale_goods_condition', 'value': 'resale_excellent'}
        self.assertFragmentIn(
            response,
            [
                {
                    'id': '1',
                    'marketSku': str(ONLY_NEW_SKU),
                    'additionalParams': [resale_new],
                    'active': True,
                    'fuzzy': Absent(),
                },
                {
                    'id': '2',
                    'marketSku': str(SKU_WITH_PERFECT_RESALE_OFFER),
                    'additionalParams': [resale_resale, resale_perfect],
                    'active': True,
                    'fuzzy': Absent(),
                },
                {
                    'id': '3',
                    'marketSku': str(SKU_WITH_EXCELLENT_RESALE_OFFER),
                    'additionalParams': [resale_resale, resale_excellent],
                    'active': True,
                    'fuzzy': Absent(),
                },
                {
                    'id': '4',
                    'marketSku': str(SKU_WITH_WELL_RESALE_OFFER),
                    'additionalParams': [resale_new],
                    'active': True,
                    'fuzzy': Absent(),
                },
                {
                    'id': '5',
                    'marketSku': str(SKU_WITH_TWO_RESALE_OFFERS),
                    'additionalParams': [resale_resale, resale_perfect],
                    'active': True,
                    'fuzzy': Absent(),
                },
                {
                    'id': '6',
                    'marketSku': str(SKU_WITH_MANY_RESALE_OFFERS),
                    'additionalParams': [resale_resale, resale_perfect],
                    'active': True,
                    'fuzzy': Absent(),
                },
            ],
            allow_different_len=False,
        )

    def test_resale_affix_on_offerinfo(self):
        # Проверка наличия суффикса ", ресейл" в resale-оффере

        # Проверка наличия аффикса ", ресейл" у ресейл-оффера при включенном cgi use-title-affixes
        for use_title_affixes in (None, 0, 1):
            request_resale_offer = (
                'place=offerinfo&offerid=PerfectOfferWithOferId&show-urls=cpa,external&rids=0&regset=2'
            )
            show_affix_resale = False

            if use_title_affixes == 1:
                request_resale_offer += "&use-title-affixes=1"
                show_affix_resale = True

            response = self.report.request_json(request_resale_offer)

            title = "second perfect resale offer #{}".format(SKU_WITH_MANY_RESALE_OFFERS)
            if show_affix_resale:
                title += ", ресейл"

            self.assertFragmentIn(
                response,
                {"titles": {"raw": title, "highlighted": [{"value": title}]}},
            )

        # При соблюдении всех условий в запросе, у new-оффера аффикса ", ресейл" всё равно не должно быть
        request_new_offer = (
            'place=offerinfo&offerid=ItsNewOfferWithOfferId&show-urls=cpa,external&rids=0&regset=2&use-title-affixes=1'
        )
        response = self.report.request_json(request_new_offer)

        self.assertFragmentIn(
            response,
            {
                "titles": {
                    "raw": "not resale offer #{}".format(ONLY_NEW_SKU),
                    "highlighted": [{"value": "not resale offer #{}".format(ONLY_NEW_SKU)}],
                }
            },
        )

    def test_parallel_offers_wizard(self):
        # Проверка отсутствия resale-офферов в place=parallel

        request = 'place=parallel&text=offer&trace_wizard=1&hid={}'.format(HID_FOR_MANY_OFFERS_WITH_RESALES)
        response = self.report.request_bs(request)

        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "offer_count": 2,
                        "showcase": {
                            "items": [
                                {"title": {"text": {"__hl": {"text": "not resale offer #1004", "raw": True}}}},
                                {"title": {"text": {"__hl": {"text": "not resale offer #1001", "raw": True}}}},
                            ]
                        },
                    }
                ]
            },
        )

    def test_productoffers_show_duplicate_resale_offers(self):
        '''
        Проверяем, что включенный show-duplicate-resale-offers заставляет productoffers показывать все ресейл оффера
        '''

        base_request = (
            'place=productoffers&hyperid={0}&enable-resale-goods=1&grhow=supplier&regset=2&market-sku={1}'.format(
                MODEL_FOR_SAME_SHOP_ID, SKU_FOR_SAME_SHOP_ID
            )
        )

        # без выставленного флага все оффера схлопнутся в один
        response = self.report.request_json(base_request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "prices": {
                            "rawValue": EqualToOneOf("100", "101", "102", "150", "152"),
                        }
                    },
                ]
            },
            allow_different_len=False,
        )

        # с выставленным флагом все ресейл оффера будут показаны, новые схлопнутся в один как раньше
        request = base_request + '&show-duplicate-resale-offers=1'
        response = self.report.request_json(request)

        # на выходе ожидаем все ресейл оффера и один из двух новых офферов
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "titles": {
                            "raw": "well resale offer for filter test",
                        },
                        "prices": {
                            "rawValue": "100",
                        },
                    },
                    {
                        "titles": {
                            "raw": "excellent resale offer1 for filter test",
                        },
                        "prices": {
                            "rawValue": "101",
                        },
                    },
                    {
                        "titles": {
                            "raw": "excellent resale offer2 for filter test",
                        },
                        "prices": {
                            "rawValue": "102",
                        },
                    },
                    {
                        "prices": {
                            "rawValue": EqualToOneOf("150", "152"),
                        }
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_modelinfo(self):
        req = 'place=modelinfo&hyperid={}&rids=0'.format(MODEL_ID)
        # без enable-resale-goods б/у оффера не считаются
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'offers': {'count': 2},
            },
        )
        # с enable-resale-goods считаются
        response = self.report.request_json(req + '&enable-resale-goods=1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'offers': {'count': 12},
            },
        )
        # с client=pricelabs используется статистика, а не вызов плейса TModelStatisticsPlace
        response = self.report.request_json(req + '&client=pricelabs')
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'offers': {'count': 12},
            },
        )


if __name__ == '__main__':
    main()
