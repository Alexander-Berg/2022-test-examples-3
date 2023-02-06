#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Round
from core.types import BertServiceDoc, HyperCategory, MnPlace, Offer, Region, Shop, MarketSku
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_bert_service(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.bert_service.needs_default = False

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            ),
        ]

        cls.bert_service.on_request(
            query='телефон',
            region=213,
            reqid='123',
            yandexuid='19937',
            icookie='',
            uniquid='1337',
            passportuid='1923',
            querytype=0,  # This is NBg::NProto::TMarketRequest::QT_TEXT.
            hid='',
            nid='',
            query_category='hid - 1',
            # region_chain="москва москва и московская область центральный федеральный округ россия евразия земля",
            qfuf="микроволновая печь микроволновая печь купить микроволновка свч печь купить микроволновку микроволновка "
            "купить микроволновая печь купить недорого в москве микроволновая печь купить спб свч принцип работы микроволновой печи",
            docs=[
                BertServiceDoc(
                    url='http://eldorado.ru/offer/1?sku=1',
                    normalized_url='http://eldorado.ru/offer/1',
                    hyper_id=1,
                    title='телефон apple iphone 128 gb чёрный',
                    price='10000',
                    categ_id='444',
                    model_id=18446744073709551615,
                    ware_md5='09lEaAKkQll1XTaaaaaaaQ',
                    msku_id='1',
                    description='новый чёрный айфон',
                    uniq_name='uniq - hid - 1',
                ),
                BertServiceDoc(
                    url='http://eldorado.ru/offer/1?sku=2',
                    normalized_url='http://eldorado.ru/offer/1',
                    hyper_id=1,
                    title='телефон apple iphone 128 gb зелёный',
                    price='9000',
                    categ_id='444',
                    model_id=18446744073709551615,
                    ware_md5='fDbQKU6BwzM0vDugM73auA',
                    msku_id='2',
                    description='новый зелёный айфон',
                    uniq_name='uniq - hid - 1',
                ),
            ],
        ).respond(
            features_dict={
                'predict_assessment_binary_target': [0.9, 0.76],
                'predict_assessment_target': [0.1, 0.2],
                'predict_click_target': [0.0, 1.0],
                'predict_has_cpa_click_target': [0.3, 0.4],
                'predict_cpa_target': [0.5, 0.6],
            }
        )

        cls.index.hypertree += [
            HyperCategory(hid=1, tovalid=444),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
        ]

        cls.index.mskus += [
            MarketSku(title='телефон Apple IPhone 128 GB чёрный', sku=1, hid=1),
            MarketSku(title='телефон Apple IPhone 128 GB зелёный', sku=2, hid=1),
        ]

        cls.index.offers += [
            Offer(
                title='телефон Apple IPhone 128 GB чёрный',
                ts=501,
                bid=10000,
                price=10000,
                fesh=1,
                hid=1,
                waremd5='09lEaAKkQll1XTaaaaaaaQ',
                sku=1,
                descr='новый чёрный айфон',
                url='http://eldorado.ru/offer/1?sku=1',
            ),
            Offer(
                title='телефон Apple IPhone 128 GB зелёный',
                ts=502,
                bid=9000,
                price=9000,
                fesh=2,
                hid=1,
                waremd5='fDbQKU6BwzM0vDugM73auA',
                sku=2,
                descr='новый зелёный айфон',
                url='http://eldorado.ru/offer/1?sku=2',
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 501).respond(0.50)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 502).respond(0.49)

    def test_bert_service(self):
        """
        Проверяем, что факторы из бертового сервиса применяются к документам на мете
        """

        request = (
            'place=prime&text=телефон&yandexuid=19937&icookie=228&uuid=1337&puid=1923&rids=213&allow-collapsing=1&'
            'debug=da&reqid=123&rearr-factors=non_default_model_name_text=0'
        )
        flag_on = '&rearr-factors=market_use_bert_service=1'
        flag_off = '&rearr-factors=market_use_bert_service=0'
        expected = {
            'results': [
                {
                    'entity': 'offer',
                    'titles': {'raw': 'телефон Apple IPhone 128 GB чёрный'},
                    'debug': {'factors': {'BERT_ASSESSMENT_BINARY': Round(0.90, 2)}},
                },
                {
                    'entity': 'offer',
                    'titles': {'raw': 'телефон Apple IPhone 128 GB зелёный'},
                    'debug': {'factors': {'BERT_ASSESSMENT_BINARY': Round(0.76, 2)}},
                },
            ],
        }
        expected_2 = {
            'results': [
                {
                    'entity': 'offer',
                    'titles': {'raw': 'телефон Apple IPhone 128 GB чёрный'},
                    'debug': {'factors': {'BERT_ASSESSMENT': Round(0.1, 2)}},
                },
                {
                    'entity': 'offer',
                    'titles': {'raw': 'телефон Apple IPhone 128 GB зелёный'},
                    'debug': {'factors': {'BERT_ASSESSMENT': Round(0.2, 2)}},
                },
            ],
        }

        par_flag_tmpl = '&rearr-factors=market_enable_speedups_for_metadoc_search={}'

        for par_flag in ('', par_flag_tmpl.format('0'), par_flag_tmpl.format('1')):
            response = self.report.request_json(request + flag_on + par_flag)
            self.assertFragmentIn(response, expected, allow_different_len=False)
            self.assertFragmentIn(response, expected_2, allow_different_len=False)

            response = self.report.request_json(request + flag_off + par_flag)
            self.assertFragmentNotIn(response, expected)

    def test_text_features_in_productoffers_enabled(self):
        """
        Проверяем, что поисковые текстовые факторы применяются в ДО, если запрос содержит текст и параметр &use-text-search-factors=1
        """

        request = (
            'place=productoffers&text=телефон&yandexuid=19937&icookie=228&uuid=1337&puid=1923&rids=213&allow-collapsing=1&'
            '&debug=da&reqid=123&rearr-factors=non_default_model_name_text=0;market_use_bert_service=1'
            '&pp=18&use-text-search-factors=1&market-sku=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, 'BERT_DSSM_QUERY_LAWNMOVER')
        self.assertFragmentIn(response, 'BERT_DSSM_VACUUM_CLEANER')
        self.assertFragmentIn(response, 'DSSM_BERT')
        self.assertFragmentIn(response, 'DSSM_HARD2')
        self.assertFragmentIn(response, 'OFFER_PRICE')

    def test_text_features_in_productoffers_disabled(self):
        """
        Проверяем, что поисковые текстовые факторы не применяются в ДО, если параметр &use-text-search-factors отсутствует
        """

        request = (
            'place=productoffers&text=телефон&yandexuid=19937&icookie=228&uuid=1337&puid=1923&rids=213&allow-collapsing=1&'
            '&debug=da&reqid=123&rearr-factors=non_default_model_name_text=0;market_use_bert_service=1'
            '&pp=18&market-sku=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, 'BERT_DSSM_QUERY_LAWNMOVER')
        self.assertFragmentNotIn(response, 'BERT_DSSM_VACUUM_CLEANER')
        self.assertFragmentNotIn(response, 'DSSM_BERT')
        self.assertFragmentNotIn(response, 'DSSM_HARD2')
        self.assertFragmentIn(response, 'OFFER_PRICE')

    @classmethod
    def prepare_error(cls):
        cls.bert_service.needs_default = False

        cls.index.shops += [
            Shop(fesh=2, priority_region=213),
        ]
        cls.index.offers += [
            Offer(title='чайник', fesh=2),
        ]

    def test_error(self):
        """
        Проверяем, что, если факторы не нашлись, репорт нормально отработает
        """

        self.error_log.expect(code=3833).once()  # EXTREQUEST_BERT_SERVICE_REQUEST_FAILED error.

        response = self.report.request_json(
            'place=prime&text=чайник&rids=213&allow-collapsing=1&debug=da'
            '&rearr-factors=market_use_bert_service=1&reqid=123'
        )
        self.assertFragmentIn(response, {'results': [{'titles': {'raw': 'чайник'}}]})


if __name__ == '__main__':
    main()
