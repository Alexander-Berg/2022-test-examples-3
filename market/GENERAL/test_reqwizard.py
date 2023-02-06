#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, ReqwExtMarkupMarketShop, ReqwExtMarkupToken, ReqwExtMarkupTokenChar
from core.testcase import TestCase, main
from core.matcher import Contains, Not, Wildcard

from urllib import quote

QTREE = 'cHicdZA7SwNBFIXPvWvMuKRYFCFsJVuY0WqxClZBFFIGG8NWskSMIiqxCVYpYycIFmIhSl6C8dUIYrBUQdhf4G_xJhvNRuIMwxlmztwz9zN9M6FgIalmoNnFpBl0gudw2XAwjwUsJlRMHBAHXGSQRQ55rKN4fvp1w8eEM8IlRR4-EF4IMt4IARVcpGmpToos2BGXA00uZTl3xb1qVJo1B9d33UQxII1V-Moim4OOniqqMkprEV_H4l-fUX199_j6Vlbb46ZoU7Qh2hCti9ZFW6It0Zpore2Py68ghalMh6zI-2Clpk_19rL9xGb-D53Y3o5Mm-Yc0v-jGQYTvhnF5DNkEhr6OCh38oMjY4Y3FiWp3yFVo38WLLA5daANP26xTeEGNqW0If1wyRlRoeIh0jOXVkZ4jjy-ePTofjhoo9CrT2HQhCQaW7ubYZSg42J8P14hSN_OWJeaxuDkGzp-luk,'  # noqa


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.reqwizard.on_default_request().respond()
        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

    @classmethod
    def prepare_no_commonlog_errors_at_prime_requests_with_reqwizard_turned_on(cls):
        cls.reqwizard.on_request('абвг').respond(qtree=QTREE)

    def test_do_not_use_synonyms(self):
        response = self.report.request_json('place=prime&text=kijanka&debug=da&rearr-factors=market_no_synonyms=1')
        self.assertFragmentNotIn(response, {"logicTrace": [Contains('/wizard?rearr=market_use_synonyms%3D1')]})
        self.error_log.ignore(code=3021)

    def test_wizextra(self):
        response = self.report.request_json('place=prime&text=kijanka&debug=da&rearr-factors=market_wizextra=asdf:qwer')
        self.assertFragmentNotIn(response, {"logicTrace": [Contains('/wizard?wizextra=asdf:qwer')]})

    def test_no_unescaping(self):
        """Проверяем, что визарду передается запрос без изменений"""
        text = 'barcode%3ATS31908%25160906%25RU1%25'
        response = self.report.request_json('place=prime&text={}&debug=1'.format(text))
        self.assertFragmentIn(response, {"logicTrace": [Wildcard('*/wizard*text={}*'.format(text))]})
        self.error_log.ignore(code=3021)

    # @see MARKETOUT-10307
    def test_no_commonlog_errors_at_prime_requests_with_reqwizard_turned_on(self):
        '''
        Проверяем, что, если реквизард включен в конфиге и отдаёт корректное
        маркетное дерево запроса, на запросах в prime
        в common-log не попадает ошибка парсинга qtree
        '''
        self.report.request_json('place=prime&text=абвг')

    @classmethod
    def prepare_x_forwarded_for_reqwizard(cls):
        cls.reqwizard.on_request('Абвг', {"X-Forwarded-For": "::1"}).respond(qtree=QTREE)
        cls.reqwizard.on_request('Абвг').return_code(500)
        cls.reqwizard.on_request('АБвг', {"X-Forwarded-For": "131.130.129.128, ::1"}).respond(qtree=QTREE)
        cls.reqwizard.on_request('АБвг').return_code(500)
        cls.reqwizard.on_request('АБВг', {"X-Forwarded-For": "131.130.129.128, 127.126.125.124, ::1"}).respond(
            qtree=QTREE
        )
        cls.reqwizard.on_request('АБВг').return_code(500)
        cls.reqwizard.on_request('АБВГ', {"X-Forwarded-For": "127.126.125.124, ::1"}).respond(qtree=QTREE)
        cls.reqwizard.on_request('АБВГ').return_code(500)
        cls.reqwizard.on_request('аБВГ', {"X-Forwarded-For": "130.109.110.121, 126.126.125.124, ::1"}).respond(
            qtree=QTREE
        )
        cls.reqwizard.on_request('аБВГ').return_code(500)
        cls.reqwizard.on_request('абВГ', {"X-Forwarded-For": "130.109.110.121, 126.126.125.124, ::1"}).respond(
            qtree=QTREE
        )
        cls.reqwizard.on_request('абВГ').return_code(500)
        cls.reqwizard.on_request('абвГ', {"X-Forwarded-For": "::1, 130.109.110.121, 126.126.125.124"}).respond(
            qtree=QTREE
        )
        cls.reqwizard.on_request('абвГ').return_code(500)

    def test_x_forwarded_for_reqwizard(self):
        '''
        Проверяем, что заголовок X-Forwarded-For, передаваемый визарду, содержит по порядку:
            параметр ip=, если там что-то есть
            X-Forwarder-For исходного запроса, если там что-то было
            REMOTE_ADDR (ожидаем строку ::1),
            причем заголовок должен содержать только уникальные аддресса
        '''
        self.report.request_json('place=prime&text=АБВГ&ip=', headers={"X-Forwarded-For": "127.126.125.124"})
        self.report.request_json(
            'place=prime&text=АБВг&ip=131.130.129.128', headers={"X-Forwarded-For": "127.126.125.124"}
        )
        self.report.request_json('place=prime&text=АБвг&ip=131.130.129.128')
        self.report.request_json('place=prime&text=Абвг&ip=')
        '''
        Тестируем что в заголовке не будет повторяющихся адресов
        '''
        self.report.request_json(
            'place=prime&text=аБВГ&ip=130.109.110.121,130.109.110.121',
            headers={"X-Forwarded-For": "126.126.125.124, 130.109.110.121"},
        )
        self.report.request_json(
            'place=prime&text=абВГ&ip=130.109.110.121,130.109.110.121',
            headers={"X-Forwarded-For": "126.126.125.124, ::1, 126.126.125.124"},
        )
        self.report.request_json(
            'place=prime&text=абвГ&ip=::1,130.109.110.121',
            headers={"X-Forwarded-For": "126.126.125.124, ::1, 126.126.125.124"},
        )

    def test_wizard_request_parameters_for_content_api_blue(self):
        """Проверка, что параметр market_no_ext_synonyms=1 не добавляется в запрос к визарду при запросе от content api к синему
        MARKETOUT-18186
        MARKETOUT-35710
        """
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&text=api&rgb=blue&api=content&askreqwizard=1&debug=1'
        )
        self.assertFragmentNotIn(
            response, {'logicTrace': [Wildcard('*Reqwizard request parameters: *rearr=*market_no_ext_synonyms%3D1*')]}
        )

    @classmethod
    def prepare_invalid_tokens_in_response(cls):
        """Реквизард иногда отвечает трешовыми токенами вылезающими за границу строки
        https://st.yandex-team.ru/MARKETOUT-18512
        """

        cls.index.offers += [Offer(fesh=346231, title='оффер из veruspart@yandex.ru')]
        cls.reqwizard.on_request('veruspart@yandex.ru').respond(
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=9, text="veruspart"),
                ReqwExtMarkupTokenChar(begin_char=21, end_char=27, text="yandex"),
                ReqwExtMarkupTokenChar(begin_char=36, end_char=38, text="ru"),
            ],
            found_shop_positions=[
                ReqwExtMarkupToken(
                    begin=1,
                    end=2,
                    data=ReqwExtMarkupMarketShop(shop_id=346231, alias_type='URL', is_good_for_matching=False),
                )
            ],
        )

    def test_invalid_tokens_in_response(self):
        """Проверяем что при невалидных токенах мы пишем в лог
        при этом все равно отдаем редирект по наматчившемуся магазину"""

        response = self.report.request_json('place=prime&cvredirect=1&text=veruspart@yandex.ru&debug=da')
        self.assertFragmentIn(
            response,
            {
                'redirect': {
                    'params': {
                        'fesh': ['346231'],
                    }
                }
            },
        )

        self.error_log.expect(
            code=3810, message=Contains('Token [21, 27] = "yandex" is invalid for request veruspart@yandex.ru')
        ).once()
        self.error_log.expect(
            code=3810, message=Contains('Token [36, 38] = "ru" is invalid for request veruspart@yandex.ru')
        ).once()

    def check_market_req_id_forwarded_value(self, contains_value, not_contains_value, external_service_logs):
        for i, log in enumerate(external_service_logs):
            log.expect(hdr=Contains("X-Market-Req-ID: {}/{}".format(contains_value, i + 1)))
            log.expect(hdr=Not(Contains("X-Market-Req-ID: {}/{}".format(not_contains_value, i + 1))))

    def check_market_req_id_forwarded(self, test_request, header_value, cgi_value, external_service_logs):
        market_req_id_header = {'X-Market-Req-ID': header_value}

        self.report.request_json(test_request, headers=market_req_id_header)
        self.check_market_req_id_forwarded_value(header_value, cgi_value, external_service_logs)

        self.report.request_json(test_request + "&market-req-id={}".format(cgi_value), headers=market_req_id_header)
        self.check_market_req_id_forwarded_value(header_value, cgi_value, external_service_logs)

        self.report.request_json(test_request + "&market-req-id={}".format(cgi_value))
        self.check_market_req_id_forwarded_value(cgi_value, header_value, external_service_logs)

    def test_market_req_id_forwarded(self):
        test_request = "place=prime&text=kijanka&debug=da&rearr-factors=market_no_synonyms=1;market_ugc_saas_enabled=1;&additional_entities=articles"

        '''
        Проверяем, что репорт пробрасывает заголовок X-Market-Req-ID или (в отсутствие заголовка) CGI параметр market-req-id в сервис reqwizard
        Сначала для alphanum значения, потом для numerical
        '''
        self.check_market_req_id_forwarded(test_request, "abc123", "def456", [self.reqwizard_log])
        self.check_market_req_id_forwarded(test_request, 987654321, 12345678, [self.reqwizard_log])

    @classmethod
    def prepare_reqwizard_first_response_failed(cls):
        cls.reqwizard.on_request('abcdef').return_code(500)

    def test_reqwizard_first_response_failed(self):
        """Проверяем отсутствие перезапроса при ошибке, отличной от 429"""
        """500ки не пишем в лог"""
        _ = self.report.request_json('place=prime&text=abcdef')

        self.error_log.not_expect(code=3671)
        self.error_log.not_expect(code=3665)

    @classmethod
    def prepare_reqwizard_got_429(cls):
        cls.reqwizard.on_request('abcdefg', wizclient='market-main-report').return_code(429)

    def test_reqwizard_got_429(self):
        """Проверяем наличие ошибки on quota, если reqwizard ответил 429"""
        _ = self.report.request_json('place=prime&text=abcdefg')
        self.error_log.expect(code=3671).once()

    @classmethod
    def prepare_assert_begemot_flag(cls):
        cls.reqwizard.on_request('noMarketRule').respond(tires_mark="1")
        cls.reqwizard.on_request('noQtree').respond(non_region_query="noqtree")
        cls.reqwizard.on_request('errorQtree').respond(qtree='error_qtree')

    def test_assert_begemot_flag_prime(self):
        """Проверяем, что cgi параметр &assert-begemot-works=1 включает проверку ответа бегемота на прайме
        https://st.yandex-team.ru/MARKETOUT-25991
        """
        request = 'place=prime&assert-begemot-works=1'

        # Ответ бегемота не содержит ошибок
        response = self.report.request_json(request + '&text=абвг', strict=False)
        self.assertEqual(response.code, 200)
        self.assertFragmentNotIn(response, {"error": {"code": "BEGEMOT_ERROR"}})

        # Для бестекстовых запросов ответ бегемота не запрашивается
        response = self.report.request_json(request, strict=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(response, {"error": {"code": "BEGEMOT_ERROR", "message": "Begemot answer parse failed"}})

        # По запросу noMarketRule в ответе бегемота нет правила Market
        response = self.report.request_json(request + '&text=noMarketRule', strict=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response, {"error": {"code": "BEGEMOT_ERROR", "message": "Begemot answer has no Market rule"}}
        )

        # По запросу noQtree в ответе бегемота нет qtree4market
        response = self.report.request_json(request + '&text=noQtree', strict=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response, {"error": {"code": "BEGEMOT_ERROR", "message": "Begemot answer Qtree is empty"}}
        )

        # qtree4market не распарсилось и не сложилось в TRichTreePtr
        response = self.report.request_json(request + '&text=errorQtree', strict=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response, {"error": {"code": "BEGEMOT_ERROR", "message": "Begemot answer Qtree decoding failed"}}
        )

    def test_assert_begemot_flag_parallel(self):
        """Проверяем, что cgi параметр &assert-begemot-works=1 включает проверку ответа бегемота на параллельном
        https://st.yandex-team.ru/MARKETOUT-25991
        """
        request = 'place=parallel&assert-begemot-works=1'
        begemot = '&askreqwizard=1&rearr-factors=market_parallel_wizard=1'

        # Ответ бегемота не содержит ошибок
        response = self.report.request_bs(request + begemot + '&text=абвг', strict=False)
        self.assertEqual(response.code, 200)
        self.assertFragmentNotIn(response, {"error": {"code": "BEGEMOT_ERROR"}})

        # Для бестекстовых запросов ответ бегемота не запрашивается
        response = self.report.request_json(request + '&text=абвг', strict=False)
        self.assertEqual(response.code, 200)
        self.assertFragmentIn(response, {"error": {"code": "BEGEMOT_ERROR", "message": "Begemot answer parse failed"}})

        # По запросу noMarketRule в ответе бегемота нет правила Market
        response = self.report.request_json(request + begemot + '&text=noMarketRule', strict=False)
        self.assertEqual(response.code, 200)
        self.assertFragmentIn(
            response, {"error": {"code": "BEGEMOT_ERROR", "message": "Begemot answer has no Market rule"}}
        )

        # По запросу noQtree в ответе бегемота нет qtree4market
        response = self.report.request_json(request + begemot + '&text=noQtree', strict=False)
        self.assertEqual(response.code, 200)
        self.assertFragmentIn(
            response, {"error": {"code": "BEGEMOT_ERROR", "message": "Begemot answer Qtree is empty"}}
        )

        # qtree4market не распарсилось и не сложилось в TRichTreePtr
        response = self.report.request_json(request + begemot + '&text=errorQtree', strict=False)
        self.assertEqual(response.code, 200)
        self.assertFragmentIn(
            response, {"error": {"code": "BEGEMOT_ERROR", "message": "Begemot answer Qtree decoding failed"}}
        )

    def test_geogzt_limit(self):
        """
        Проверяем, что в бегемот пробрасывается флаг geogztbuilder_items_limit
        в шайни-пайплайне
        """

        def assert_wildcard(response, wildcard):
            return self.assertFragmentIn(response, {"debug": {"report": {"logicTrace": [Wildcard(wildcard)]}}})

        def assert_no_wildcard(response, wildcard):
            return self.assertFragmentNotIn(response, {"debug": {"report": {"logicTrace": [Wildcard(wildcard)]}}})

        wizard_request_flag = quote('geogztbuilder_items_limit=100')

        response = self.report.request_json('place=prime&text=iphone&debug=da')
        assert_wildcard(response, '*/wizard*text=iphone*')
        assert_no_wildcard(response, '*/wizard*action=markup*')
        assert_no_wildcard(response, '*/wizard*{}*'.format(wizard_request_flag))

        response = self.report.request_json('place=brand_products&vendor_id=4&text=iphone&debug=da')
        self.error_log.ignore(code=3663)
        assert_wildcard(response, '*/wizard*text=iphone*')
        assert_wildcard(response, '*/wizard*action=markup*')
        assert_wildcard(response, '*/wizard*{}*'.format(wizard_request_flag))

    @classmethod
    def prepare_reqwizard_got_418(cls):
        cls.reqwizard.on_request('abcdefgh', srcrwr='BEGEMOT_WIZARD:::200ms').respond(qtree=QTREE)
        cls.reqwizard.on_request('abcdefgh').return_code(418)

    def test_reqwizard_got_418(self):
        """
        Проверяем наличие перезапроса при таймауте (если reqwizard ответил 418),
        попадание ответа в кэш и получение из кэша при повторном запросе
        Ошибки быть не должно
        """
        response = self.report.request_json('place=prime&text=abcdefgh&debug=1')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "report": {
                        "logicTrace": [
                            Contains('Request to REQWIZARD timed out'),
                            Wildcard(
                                '*/wizard*srcrwr=BEGEMOT_WIZARD%3A%3A%3A200ms*waitall=da*wizextra=market-lingboost'
                            ),
                        ]
                    }
                }
            },
        )
        response = self.report.request_json('place=prime&text=abcdefgh&debug=1')
        self.assertFragmentIn(response, QTREE)

        self.error_log.not_expect(code=3665)
        self.external_services_log.expect(service='wizard', http_code=418).once()
        self.external_services_log.expect(service='wizard', http_code=200).once()

    @classmethod
    def prepare_reqwizard_stalled(cls):
        cls.settings.memcache_enabled = True
        cls.reqwizard.on_request('abcdefghi').return_code(418)
        cls.reqwizard.on_request('abcdefghij').return_code(429)

    def test_reqwizard_stalled(self):
        """
        Проверяем, что дальнейшие запросы в визард дропаются, если не получилось разок и повторно,
        либо попали на квоту
        """
        _ = self.report.request_json('place=prime&text=abcdefghi&debug=1')
        _ = self.report.request_json('place=prime&text=abcdefghi&debug=1')
        _ = self.report.request_json('place=prime&text=abcdefghij&debug=1')
        _ = self.report.request_json('place=prime&text=abcdefghij&debug=1')

        # плюс перезапрос с другим таймаутом
        self.external_services_log.expect(service='wizard', http_code=418).times(2)
        # запрос на квотер с ошибкой
        self.external_services_log.expect(service='wizard', http_code=429).times(1)
        self.error_log.expect(code=3671).once()

    @classmethod
    def prepare_reqwizard_retries(cls):
        cls.settings.memcache_enabled = True
        cls.reqwizard.on_request('abcdefghijk').return_code(418)
        cls.reqwizard.on_request('abcdefghijkl').return_code(429)

    def test_reqwizard_retries(self):
        """
        Проверяем, работоспособнасть экспфлага для задания повторов клиенту
        """
        _ = self.report.request_json('place=prime&text=abcdefghijk&debug=1&rearr-factors=reqwizard_retry_params=2:100')
        self.external_services_log.expect(service='wizard', http_code=418).times(3 * 2)

        _ = self.report.request_json('place=prime&text=abcdefghijkl&debug=1&rearr-factors=reqwizard_retry_params=2:100')
        self.external_services_log.expect(service='wizard', http_code=429).times(3)
        self.error_log.expect(code=3671).once()


if __name__ == '__main__':
    main()
