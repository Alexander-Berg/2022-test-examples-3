#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import socket
import time
from datetime import timedelta

from core.types import (
    CpaCategory,
    CpaCategoryType,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    Offer,
    Shop,
    VCluster,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty, Round, NoKey, Contains, LikeUrl


class T(TestCase):
    """
    Test brief debug output for relevance cookie
    """

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=1, uniq_name='Плюшевые игрушки', output_type=HyperCategoryType.GURU),
            HyperCategory(
                hid=2, uniq_name='Игрушки ручной работы', output_type=HyperCategoryType.CLUSTERS, visual=True
            ),
            HyperCategory(hid=3, uniq_name='Игрушки для малышей', output_type=HyperCategoryType.SIMPLE),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=1, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION, fee=500),
            CpaCategory(hid=2, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA, fee=250),
            CpaCategory(hid=3, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[213], name='Новые игрушки', cpa=Shop.CPA_REAL),
        ]

        cls.index.models += [Model(hid=1, ts=1000, title='Олень плюшевый', hyperid=100, vbid=130)]
        cls.index.vclusters += [VCluster(hid=2, ts=2000, vclusterid=2000000002, title='Олень вязаный')]
        cls.index.offers += [
            Offer(
                hid=1,
                hyperid=100,
                ts=1001,
                title='Олень плюшевый 10см',
                fesh=1,
                cpa=Offer.CPA_REAL,
                waremd5='jdhzkvjdNfHcnSz-ZWqDuQ',
                feedid=1,
                offerid=101,
                offer_url_hash='test_offer_url_hash',
                url='http://shop1.ru/olen-plyshevyi-10sm',
            ),
            Offer(
                hid=2,
                vclusterid=2000000002,
                ts=2001,
                title='Олень вязаный ручной работы',
                fesh=1,
                cpa=Offer.CPA_NO,
                feedid=2,
                offerid=202,
            ),
            Offer(
                hid=3,
                ts=3001,
                title='Погремушка Олень',
                fesh=1,
                cpa=Offer.CPA_NO,
                bid=400,
                vbid=201,
                waremd5='N8__PAT5WyK88WbfWGzXqQ',
                feedid=3,
                offerid=303,
            ),
            Offer(hid=1, title='Заяц Пантелеймон', fesh=1),
        ]

        cls.index.creation_time = int(time.time()) // 60 * 60
        cls.index.prices_generation_ts = cls.index.creation_time - int(timedelta(hours=5).total_seconds())

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1001).respond(0.36)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2000).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2001).respond(0.25)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3001).respond(0.39)

        cls.matrixnet.set_defaults = False

    def test_debug_brief_for_model_document(self):
        '''Проверяем запись дебажных данных об модели/кластере
        Блок содержит:
        hid, modelId
        docPriority - данные о head/tail документа
        sale - данные о различных ставках
        properties - свойства документа вычисляемые на базовых (в том числе и элементы релевантности)
        rank - данные о ранжировании документа
        '''
        response = self.report.request_json(
            'place=prime&text=олень&rids=213&debug=da&rearr-factors=market_force_use_vendor_bid=1'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "titles": {"raw": "Олень вязаный"},
                "debug": {
                    "factors": NotEmpty(),
                    "hid": 2,
                    "modelId": 2000000002,
                    "formulaValue": NotEmpty(),
                    "tech": {
                        "docPriority": "DocRangeHead",
                        "originBase": NotEmpty(),
                        "originId": NotEmpty(),
                        "relevance": NotEmpty(),
                        "normalizedRelevance": NotEmpty(),
                    },
                    "sale": {"vBid": 0, "vendorClickPrice": 0},
                    # приматчиваем к документам свойства и информацию о ранжировании с базовых
                    "properties": {
                        "DOCUMENT_AUCTION_TYPE": NotEmpty(),
                        "AUCTION_MULTIPLIER": NotEmpty(),
                        "RELEVANCE": NotEmpty(),
                        "TS": "2000",
                        "DOC_OFFSET": NotEmpty(),
                        "MODEL_ID": "2000000002",
                    },
                    "rank": NotEmpty(),
                    "metaRank": NotEmpty(),
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "titles": {"raw": "Олень плюшевый"},
                "debug": {
                    "factors": NotEmpty(),
                    "hid": 1,
                    "modelId": 100,
                    "formulaValue": NotEmpty(),
                    "tech": {
                        "docPriority": "DocRangeHead",
                        "originBase": NotEmpty(),
                        "originId": NotEmpty(),
                        "relevance": NotEmpty(),
                        "normalizedRelevance": NotEmpty(),
                    },
                    "sale": {"vBid": 130, "vendorClickPrice": 1},  # Автоброкер
                    "properties": {
                        "DOCUMENT_AUCTION_TYPE": NotEmpty(),
                        "AUCTION_MULTIPLIER": NotEmpty(),
                        "RELEVANCE": NotEmpty(),
                        "TS": "1000",
                        "DOC_OFFSET": NotEmpty(),
                        "MODEL_ID": "100",
                    },
                    "rank": NotEmpty(),
                    "metaRank": NotEmpty(),
                },
            },
        )

    def test_debug_brief_for_offer_document(self):
        '''Тестируем debug блок для оффера
        Блок содержит:
        hid, fesh, modelId и различные идентификаторы оффера (feed, wareId и пр.)
        docPriority - данные о head/tail документа
        sale - данные о различных ставках
        properties - свойства документа вычисляемые на базовых (в том числе и элементы релевантности)
        rank - данные о ранжировании оффера
        '''
        response = self.report.request_json('place=prime&text=олень&rids=213&debug=da')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Погремушка Олень"},
                "debug": {
                    "factors": NotEmpty(),
                    "hid": 3,
                    "modelId": NoKey('modelId'),
                    "wareId": "N8__PAT5WyK88WbfWGzXqQ",
                    "tech": {
                        "docPriority": "DocRangeHead",
                        "originBase": NotEmpty(),
                        "originId": NotEmpty(),
                        "relevance": NotEmpty(),
                        "normalizedRelevance": NotEmpty(),
                        "vendorDatasourceId": NotEmpty(),
                    },
                    "fesh": 1,
                    "feed": {
                        "id": "3",
                        "offerId": "303",
                        "idxapiOffer": LikeUrl.of(
                            "http://idxapi.market.yandex.net:29334/v1/smart/offer?feed_id=3&offer_id=303&format=json"
                        ),
                    },
                    "sale": {
                        "bidType": "mbid",
                        "bid": 1,
                        "minBid": 1,
                        "clickPrice": 1,
                        "brokeredClickPrice": 1,
                        "vBid": 0,
                        "vendorClickPrice": 0,
                    },
                    "properties": {
                        "DOCUMENT_AUCTION_TYPE": "CPC",
                        "BID": "1",
                        "AUCTION_MULTIPLIER": Round(1),
                        "RELEVANCE": NotEmpty(),
                        "TS": "3001",
                        "WARE_MD5": "N8__PAT5WyK88WbfWGzXqQ",
                        "DOC_OFFSET": NotEmpty(),
                        "MODEL_ID": NoKey("MODEL_ID"),
                    },
                    "rank": NotEmpty(),
                    "metaRank": NotEmpty(),
                },
            },
        )

    def test_debug_brief_for_offer_document_MARKETOUT_29780(self):
        '''Тестируем debug блок для оффера
        Блок содержит:
        hid, fesh, modelId и различные идентификаторы оффера (feed, wareId и пр.)
        docPriority - данные о head/tail документа
        sale - данные о различных ставках
        properties - свойства документа вычисляемые на базовых (в том числе и элементы релевантности)
        rank - данные о ранжировании оффера
        '''
        response = self.report.request_json(
            'place=prime&text=олень&rids=213&debug=da&rearr-factors=market_new_cpm_iterator=0'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Погремушка Олень"},
                "debug": {
                    "factors": NotEmpty(),
                    "hid": 3,
                    "modelId": NoKey('modelId'),
                    "wareId": "N8__PAT5WyK88WbfWGzXqQ",
                    "tech": {
                        "docPriority": "DocRangeHead",  # первый оффер из fesh=1
                        "originBase": NotEmpty(),
                        "originId": NotEmpty(),
                        "relevance": NotEmpty(),
                        "normalizedRelevance": NotEmpty(),
                        "vendorDatasourceId": NotEmpty(),
                    },
                    "fesh": 1,
                    "feed": {
                        "id": "3",
                        "offerId": "303",
                        "idxapiOffer": LikeUrl.of(
                            "http://idxapi.market.yandex.net:29334/v1/smart/offer?feed_id=3&offer_id=303&format=json"
                        ),
                        "idxapiDelivery": LikeUrl.of(
                            "http://idxapi.market.yandex.net:29334/v1/feeds/3/sessions/published/offers/303/delivery"
                        ),
                    },
                    "sale": {
                        "bidType": "mbid",
                        "bid": 1,
                        "minBid": 1,
                        "clickPrice": 1,
                        "brokeredClickPrice": 1,
                        "vBid": 0,
                        "vendorClickPrice": 0,
                    },
                    "properties": {
                        "DOCUMENT_AUCTION_TYPE": "CPC",
                        "BID": "1",
                        "AUCTION_MULTIPLIER": Round(1),
                        "RELEVANCE": NotEmpty(),
                        "TS": "3001",
                        "WARE_MD5": "N8__PAT5WyK88WbfWGzXqQ",
                        "DOC_OFFSET": NotEmpty(),
                        "MODEL_ID": NoKey("MODEL_ID"),
                    },
                    "rank": NotEmpty(),
                    "metaRank": NotEmpty(),
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Олень плюшевый 10см"},
                "debug": {
                    "factors": NotEmpty(),
                    "hid": 1,
                    "modelId": 100,
                    "wareId": "jdhzkvjdNfHcnSz-ZWqDuQ",
                    "tech": {
                        "docPriority": "DocRangeTail",  # еще оффер из fesh=1 - попадает в tail
                        "originBase": NotEmpty(),
                        "originId": NotEmpty(),
                        "relevance": NotEmpty(),
                        "normalizedRelevance": NotEmpty(),
                        "vendorDatasourceId": NotEmpty(),
                    },
                    "fesh": 1,
                    "feed": {
                        "id": "1",
                        "offerId": "101",
                        "idxapiOffer": LikeUrl.of(
                            "http://idxapi.market.yandex.net:29334/v1/smart/offer?feed_id=1&offer_id=101&format=json"
                        ),
                        "idxapiDelivery": LikeUrl.of(
                            "http://idxapi.market.yandex.net:29334/v1/feeds/1/sessions/published/offers/101/delivery"
                        ),
                    },
                    "sale": {
                        "bidType": "mbid",
                        "bid": 1,
                        "minBid": 1,
                        "clickPrice": 1,
                        "brokeredClickPrice": 1,
                        "vBid": 0,
                        "vendorClickPrice": 0,
                        "shopFee": 500,
                        "brokeredFee": 500,
                        "minFee": 500,
                    },
                    "properties": {
                        "RELEVANCE": NotEmpty(),
                        "TS": "1001",
                        "WARE_MD5": "jdhzkvjdNfHcnSz-ZWqDuQ",
                        "DOC_OFFSET": NotEmpty(),
                        "MODEL_ID": "100",
                    },
                    "rank": NotEmpty(),
                    "metaRank": NotEmpty(),
                },
            },
        )

    def test_collapsed_model(self):
        '''Проверяем как отображается isCollapsed в debug-выдаче
        Если для модели isCollapsed: True - то ее факторы и свойства взяты от
        оффера схлопнутого в данную модель
        '''

        # оффер не имеет поля isCollapsed

        offer = {
            "entity": "offer",
            "titles": {"raw": "Олень плюшевый 10см"},
            "debug": {
                "isCollapsed": NoKey("isCollapsed"),
                "wareId": "jdhzkvjdNfHcnSz-ZWqDuQ",
                "fesh": 1,
                "feed": {
                    "id": "1",
                    "offerId": "101",
                    "idxapiOffer": LikeUrl.of(
                        "http://idxapi.market.yandex.net:29334/v1/smart/offer?feed_id=1&offer_id=101&format=json"
                    ),
                    "idxapiDelivery": LikeUrl.of(
                        "http://idxapi.market.yandex.net:29334/v1/feeds/1/sessions/published/offers/101/delivery"
                    ),
                },
                "offerUrlHash": "test_offer_url_hash",
                "offerUrl": 'http://shop1.ru/olen-plyshevyi-10sm',
                "offerTitle": 'Олень плюшевый 10см',
            },
        }

        # схлопнутая модель
        # isCollapsed = true
        # к модели будут добавлены данные по ранжированию оффера а также факторы посчитанные по офферу
        # а также offerUrl, offerTitle и offerUrlHash, feed, wareId и т.п. от схлопнутого оффера
        collapsed_model = {
            "entity": "product",
            "titles": {"raw": "Олень плюшевый"},
            "debug": {
                "isCollapsed": True,
                # Данные (факторы релевантность и пр.) проброшены от сматченного оффера
                "wareId": "jdhzkvjdNfHcnSz-ZWqDuQ",
                "fesh": 1,
                "feed": {
                    "id": "1",
                    "offerId": "101",
                    "idxapiOffer": LikeUrl.of(
                        "http://idxapi.market.yandex.net:29334/v1/smart/offer?feed_id=1&offer_id=101&format=json"
                    ),
                    "idxapiDelivery": LikeUrl.of(
                        "http://idxapi.market.yandex.net:29334/v1/feeds/1/sessions/published/offers/101/delivery"
                    ),
                },
                "offerUrlHash": "test_offer_url_hash",
                "offerUrl": 'http://shop1.ru/olen-plyshevyi-10sm',
                "offerTitle": 'Олень плюшевый 10см',
                "factors": NotEmpty(),
                "properties": NotEmpty(),
                "rank": NotEmpty(),
                "metaRank": NotEmpty(),
            },
        }

        # не схлопнутая модель - не имеет данных о схлопнутых офферах
        not_collapsed_model = {
            "entity": "product",
            "titles": {"raw": "Олень плюшевый"},
            "debug": {
                "isCollapsed": False,
                # Информация об удаленных офферах приматченных к модели не добавляется
                "wareId": NoKey("wareId"),
                "fesh": NoKey("fesh"),
                # факторы и свойства посчитаны по самой модели
                "factors": NotEmpty(),
                "properties": NotEmpty(),
                "rank": NotEmpty(),
                "metaRank": NotEmpty(),
            },
        }

        # Кейс №1 - есть только оффер и нет модели - при схлапывании модель будет иметь isCollapsed: True
        response = self.report.request_json('place=prime&text=10см&hid=1&allow-collapsing=0&rids=213&debug=da')
        self.assertFragmentIn(response, {"results": [offer]}, allow_different_len=False)

        response = self.report.request_json('place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=da')
        self.assertFragmentIn(response, {"results": [collapsed_model]}, allow_different_len=False)

        # Кейс №2 на выдаче есть и оффер и модель (модель идет выше) - при схлопывании модель будет иметь isCollapsed: False
        response = self.report.request_json('place=prime&text=плюшевый&hid=1&allow-collapsing=0&rids=213&debug=da')
        self.assertFragmentIn(
            response, {"results": [not_collapsed_model, offer]}, allow_different_len=False, preserve_order=True
        )

        response = self.report.request_json('place=prime&text=плюшевый&hid=1&allow-collapsing=1&rids=213&debug=da')
        self.assertFragmentIn(response, {"results": [not_collapsed_model]}, allow_different_len=False)

    def test_debug_matrixnet_properties(self):
        '''Значения кликовой, ассессорской формул и их суммы
        Для формул которые разделены на кликовую и ассессорскую части
        '''
        response = self.report.request_json(
            'place=prime&text=заяц&debug=da&rids=213&rearr-factors=market_search_mn_algo=TESTALGO_combined'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Заяц Пантелеймон"},
                "debug": {
                    "rankedWith": "TESTALGO_combined",
                    "properties": {
                        "MATRIXNET_ASSESSOR_VALUE": NotEmpty(),
                        "MATRIXNET_CLICK_VALUE": NotEmpty(),
                        "MATRIXNET_SUM_VALUE": NotEmpty(),
                        "MATRIXNET_VALUE": NotEmpty(),
                    },
                },
            },
        )

    def test_debug_brief(self):
        '''Тестируем brief блок в общем дебаге'''
        response = self.report.request_json('place=prime&text=олень&rids=213&debug=da')
        self.assertFragmentIn(
            response,
            {
                "search": NotEmpty(),
                "debug": {
                    "brief": {
                        "reqwizardText": Contains("олень::"),
                        "marketIndexerVersion": "2018.12.02.0",
                        "generation": self.index.timestamp2generation(self.index.creation_time),
                        "pricesGeneration": self.index.timestamp2generation(
                            self.index.prices_generation_ts, '%Y%m%d_%H%M%S'
                        ),
                        "rtyFallbackTimestamp": 0,
                        "reportVersion": NotEmpty(),
                        "filters": NotEmpty(),
                        "counters": {
                            "TOTAL_DOCUMENTS_PROCESSED": NotEmpty(),
                            "TOTAL_DOCUMENTS_ACCEPTED": NotEmpty(),
                        },
                    }
                },
            },
        )

    def test_debug_brief_with_rty_fallback(self):
        '''Тестируем brief блок в общем дебаге с включенным RTY-fallback'''
        rty_fallback_interval = 2
        response = self.report.request_json(
            'place=prime&text=олень&rids=213&debug=da&rearr-factors=rty_fallback_interval={}h'.format(
                rty_fallback_interval
            )
        )
        self.assertFragmentIn(
            response,
            {
                "search": NotEmpty(),
                "debug": {
                    "brief": {
                        "reqwizardText": Contains("олень::"),
                        "marketIndexerVersion": "2018.12.02.0",
                        "generation": self.index.timestamp2generation(self.index.creation_time),
                        "pricesGeneration": self.index.timestamp2generation(
                            self.index.prices_generation_ts, '%Y%m%d_%H%M%S'
                        ),
                        "rtyFallbackTimestamp": min(
                            self.index.prices_generation_ts,
                            self.index.creation_time - int(timedelta(hours=rty_fallback_interval).total_seconds()),
                        ),
                        "reportVersion": NotEmpty(),
                        "filters": NotEmpty(),
                        "counters": {
                            "TOTAL_DOCUMENTS_PROCESSED": NotEmpty(),
                            "TOTAL_DOCUMENTS_ACCEPTED": NotEmpty(),
                        },
                    }
                },
            },
        )

    def test_relevance_cookie(self):
        '''Дебаг выводится если debug=da или mrktrlv=... - валидная кука релевантности
        или включен forced-mrktrlv=1 (@see https://st.yandex-team.ru/MARKETOUT-41693)
        Кука имеет формат body_salt_sign
        Кука валидна, если md5(body_salt+secret) == sign
        '''

        expected_debug = {"search": NotEmpty(), "debug": NotEmpty()}

        cookie = '&mrktrlv=kzhagorina,relev:1_odvpubkrouflxbj_5f7b2605ed1de57697f89e7ebddfd4a5'
        invalid_cookie = '&mrktrlv=invalid_salt_0eb31ab6ad88d5e5103bf0e001032f8d'

        response = self.report.request_json('place=prime&text=олень&rids=213' + cookie)
        self.assertFragmentIn(response, expected_debug)

        response = self.report.request_json('place=prime&text=олень&rids=213&forced-mrktrlv=1')
        self.assertFragmentIn(response, expected_debug)

        response = self.report.request_json('place=prime&text=олень&rids=213&debug=0' + cookie)
        self.assertFragmentIn(response, expected_debug)

        response = self.report.request_json('place=prime&text=олень&rids=213&debug=0&forced-mrktrlv=1')
        self.assertFragmentIn(response, expected_debug)

        response = self.report.request_json('place=prime&text=олень&rids=213&debug=da')
        self.assertFragmentIn(response, expected_debug)

        response = self.report.request_json('place=prime&text=олень&rids=213&debug=da' + invalid_cookie)
        self.assertFragmentIn(response, expected_debug)

        response = self.report.request_json(
            'place=prime&text=олень&rids=213&debug=da&forced-mrktrlv=1' + invalid_cookie
        )
        self.assertFragmentIn(response, expected_debug)

        # debug не выводится если нет debug и mrktrlv - отсутствует или невалиден
        expected_no_debug = {"search": NotEmpty(), "debug": NoKey("debug")}

        response = self.report.request_json('place=prime&text=олень&rids=213' + invalid_cookie)
        self.assertFragmentIn(response, expected_no_debug)

        response = self.report.request_json('place=prime&text=олень&rids=213')
        self.assertFragmentIn(response, expected_no_debug)

    def test_lightweight_debug(self):
        '''Если дебаг инициирован только кукой релевантности то он не будет содержать лишнего'''

        brief_debug = {
            "search": NotEmpty(),
            "debug": {
                'brief': NotEmpty(),
                'report': NoKey('report'),
                'basesearch': NoKey('basesearch'),
                'metasearch': NoKey('metasearch'),
            },
        }

        full_debug = {
            "search": NotEmpty(),
            "debug": {'brief': NotEmpty(), 'report': NotEmpty(), 'basesearch': NotEmpty(), 'metasearch': NotEmpty()},
        }

        cookie = '&mrktrlv=kzhagorina,relev:1_odvpubkrouflxbj_5f7b2605ed1de57697f89e7ebddfd4a5'

        response = self.report.request_json('place=prime&text=олень&rids=213' + cookie)
        self.assertFragmentIn(response, brief_debug)

        response = self.report.request_json('place=prime&text=олень&rids=213&forced-mrktrlv=1')
        self.assertFragmentIn(response, brief_debug)

        response = self.report.request_json('place=prime&text=олень&rids=213&debug=da' + cookie)
        self.assertFragmentIn(response, full_debug)

    def test_document_id(self):
        '''
        id докуметов в basesearch должны быть в формате "<id_number> <hostname>:<dir_path>"

        '''
        response = self.report.request_json(
            'place=prime&text=олень&pp=18&rids=213&bsformat=2&debug=da&debug-doc-count=10'
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "basesearch": {
                        "documents": [
                            {
                                "id": Contains(str(socket.gethostname()), self.meta_paths.testroot),
                                "properties": NotEmpty(),
                                "rank": NotEmpty(),
                                "factors": NotEmpty(),
                            }
                        ]
                    }
                }
            },
        )

    def test_meta_rank(self):
        '''
        Проверяется порядок и формат полей metaRank.
        '''
        # offer
        response = self.report.request_json('place=prime&text=заяц+пантелеймон&hid=1&rids=213&debug=da')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Заяц Пантелеймон"},
                        "entity": "offer",
                        "debug": {
                            "metaRank": [
                                {
                                    "name": "NAIL_POSITION",
                                    "value": NotEmpty(),
                                    "inverted": True,
                                },
                                {
                                    "name": "STABLE_RELEVANCE",
                                    "value": NotEmpty(),
                                    "inverted": NoKey("inverted"),
                                },
                                {
                                    "name": "DELIVERY_TYPE",
                                    "value": NotEmpty(),
                                    "inverted": NoKey("inverted"),
                                },
                                {
                                    "name": "IS_IN_DYNSTAT_TOP",
                                    "value": NotEmpty(),
                                    "inverted": NoKey("inverted"),
                                },
                                {
                                    "name": "META_PRIORITY",
                                    "value": NotEmpty(),
                                    "inverted": True,
                                },
                                {
                                    "name": "HAS_META_FACTORS",
                                    "value": NotEmpty(),
                                    "inverted": NoKey("inverted"),
                                },
                            ],
                        },
                    }
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # model
        response = self.report.request_json('place=prime&text=плюшевый&hid=1&allow-collapsing=0&rids=213&debug=da')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Олень плюшевый"},
                        "entity": "product",
                        "debug": {
                            "metaRank": [
                                {
                                    "name": "NAIL_POSITION",
                                    "value": NotEmpty(),
                                    "inverted": True,
                                },
                                {
                                    "name": "STABLE_RELEVANCE",
                                    "value": NotEmpty(),
                                    "inverted": NoKey("inverted"),
                                },
                                {
                                    "name": "DELIVERY_TYPE",
                                    "value": NotEmpty(),
                                    "inverted": NoKey("inverted"),
                                },
                                {
                                    "name": "IS_IN_DYNSTAT_TOP",
                                    "value": NotEmpty(),
                                    "inverted": NoKey("inverted"),
                                },
                                {
                                    "name": "META_PRIORITY",
                                    "value": NotEmpty(),
                                    "inverted": True,
                                },
                                {
                                    "name": "HAS_META_FACTORS",
                                    "value": NotEmpty(),
                                    "inverted": NoKey("inverted"),
                                },
                            ],
                        },
                    },
                    {
                        "titles": {"raw": "Олень плюшевый 10см"},
                        "entity": "offer",
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # collapsed model
        response = self.report.request_json('place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=da')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Олень плюшевый"},
                        "entity": "product",
                        "debug": {
                            "metaRank": [
                                {
                                    "name": "NAIL_POSITION",
                                    "value": NotEmpty(),
                                    "inverted": True,
                                },
                                {
                                    "name": "STABLE_RELEVANCE",
                                    "value": NotEmpty(),
                                    "inverted": NoKey("inverted"),
                                },
                                {
                                    "name": "DELIVERY_TYPE",
                                    "value": NotEmpty(),
                                    "inverted": NoKey("inverted"),
                                },
                                {
                                    "name": "IS_IN_DYNSTAT_TOP",
                                    "value": NotEmpty(),
                                    "inverted": NoKey("inverted"),
                                },
                                {
                                    "name": "META_PRIORITY",
                                    "value": NotEmpty(),
                                    "inverted": True,
                                },
                                {
                                    "name": "HAS_META_FACTORS",
                                    "value": NotEmpty(),
                                    "inverted": NoKey("inverted"),
                                },
                            ],
                        },
                    }
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_debug_trace_truncate(self):
        """&debug-trace-truncate позволяет включать/отключать обрезание длинных строк"""

        # по умолчанию все выводится как и раньше - полными строками и ничего не фильтруется
        response = self.report.request_json('place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=da')
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains(
                        'calc_query_dssm_embs.cpp',
                        'Embedding for query [10см] by model Hard2 equal',
                        '[0.1, 0.1, 0.1,',
                        '0.1, 0.1, 0.1]',
                    )
                ]
            },
        )

        # при debug-trace-truncate=1 длинные строки обрезаются
        response = self.report.request_json(
            'place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=da' '&debug-trace-truncate=1'
        )
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains(
                        'calc_query_dssm_embs.cpp',
                        'Embedding for query [10см] by model Hard2 equal',
                        '[0.1, 0.1, 0.1,',
                        '...',
                    )
                ]
            },
        )

    def test_debug_trace_filter(self):
        """&debug-trace-truncate позволяет по разному фильтровать строки в logicTrace"""

        # по умолчанию они не фильтруются
        response = self.report.request_json('place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=da')
        self.assertFragmentIn(response, {'logicTrace': [Contains('prime_base.cpp')]})
        self.assertFragmentIn(response, {'logicTrace': [Contains('meta_factors_filler.cpp')]})
        self.assertFragmentIn(response, {'logicTrace': [Contains('calc_query_dssm_embs.cpp')]})

        # остаются только строки содержащие подстроку prime_base.cpp
        response = self.report.request_json(
            'place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=da' '&debug-trace-filter=prime_base'
        )
        self.assertFragmentIn(response, {'logicTrace': [Contains('prime_base.cpp')]})
        self.assertFragmentNotIn(response, {'logicTrace': [('meta_factors_filler.cpp')]})
        self.assertFragmentNotIn(response, {'logicTrace': [Contains('calc_query_dssm_embs.cpp ')]})

        # фильтр "-prime_base.cpp" остаются только строки не содержащие prime_base.cpp
        response = self.report.request_json(
            'place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=da' '&debug-trace-filter=-prime_base'
        )
        self.assertFragmentNotIn(response, {'logicTrace': [Contains('prime_base.cpp')]})
        self.assertFragmentIn(response, {'logicTrace': [Contains('meta_factors_filler.cpp')]})
        self.assertFragmentIn(response, {'logicTrace': [Contains('calc_query_dssm_embs.cpp')]})

        # можно указывать несколько фильтров через ,
        response = self.report.request_json(
            'place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=da'
            '&debug-trace-filter=prime_base,meta_factors_filler'
        )
        self.assertFragmentIn(response, {'logicTrace': [Contains('prime_base.cpp')]})
        self.assertFragmentIn(response, {'logicTrace': [Contains('meta_factors_filler.cpp')]})
        self.assertFragmentNotIn(response, {'logicTrace': [Contains('calc_query_dssm_embs.cpp')]})

        response = self.report.request_json(
            'place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=da'
            '&debug-trace-filter=-prime_base,-meta_factors_filler'
        )
        self.assertFragmentNotIn(response, {'logicTrace': [Contains('prime_base.cpp')]})
        self.assertFragmentNotIn(response, {'logicTrace': [Contains('meta_factors_filler.cpp')]})
        self.assertFragmentIn(response, {'logicTrace': [Contains('calc_query_dssm_embs.cpp')]})

    def test_debug_lite(self):
        """По умолчанию debug=lite включает:
        обрезку длинных строк
        фильтрацию некоторых сообщений из logicTrace
        обрезку параметров запросов на базовые"""

        # по умолчанию все выводится как и раньше - полными строками и ничего не фильтруется
        response = self.report.request_json('place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=da')
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains(
                        'calc_query_dssm_embs.cpp',
                        'Embedding for query [10см] by model Hard2 equal',
                        '[0.1, 0.1, 0.1,',
                        '0.1, 0.1, 0.1]',
                    )
                ]
            },
        )

        # при debug=lite длинные строки обрезаются
        response = self.report.request_json('place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=lite')
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains(
                        'calc_query_dssm_embs.cpp',
                        'Embedding for query [10см] by model Hard2 equal',
                        '[0.1, 0.1, 0.1,',
                        '...',
                    )
                ]
            },
        )

        # если строка попадает под положительный фильтр то она не обрезается
        response = self.report.request_json(
            'place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=lite'
            '&debug-trace-filter=embedding for query'
        )
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains(
                        'calc_query_dssm_embs.cpp',
                        'Embedding for query [10см] by model Hard2 equal',
                        '[0.1, 0.1, 0.1,',
                        '0.1, 0.1, 0.1]',
                    )
                ]
            },
        )

        # при debug=lite также обрезаются громоздские запросы на базовые
        response = self.report.request_json('place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=lite')
        self.assertFragmentIn(
            response,
            {
                'report': {
                    'context': {
                        'collections': {'SHOP': {'userhow': [Contains('main:', '...[add &debug-bs-req=1 to see all]')]}}
                    }
                }
            },
        )
        # это можно отменить с помощью &debug-bs-req=1
        response = self.report.request_json(
            'place=prime&text=10см&hid=1&allow-collapsing=1&rids=213&debug=lite' '&debug-bs-req=1'
        )
        self.assertFragmentIn(
            response, {'report': {'context': {'collections': {'SHOP': {'userhow': [Contains('main:')]}}}}}
        )
        self.assertFragmentNotIn(
            response,
            {
                'report': {
                    'context': {'collections': {'SHOP': {'userhow': [Contains('...[add &debug-bs-req=1 to see all]')]}}}
                }
            },
        )


if __name__ == '__main__':
    main()
