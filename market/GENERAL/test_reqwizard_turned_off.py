#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main

'''
Данный набор тестов предназначен для фиксации корректного поведения репорта в случаях,
когда обращение к реквизарду как ко внешнему сервису запрещено в конфигах. Пожалуйста,
не добавляйте в методы prepare* данного класса включение cls.reqwizard.
'''


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    def test_no_commonlog_errors_at_prime_requests_with_reqwizard_turned_off(self):
        '''
        Проверяем, что, если реквизард не включен в конфиге, на запросе в prime
        в common-log не попадает ошибка парсинга qtree
        '''
        self.report.request_json('place=prime&text=абвг')

    def test_no_commonlog_errors_at_parallel_requests_with_reqwizard_answer(self):
        '''
        Проверяем, что, если реквизард не включен в конфиге, а в cgi-параметрах запроса на
        параллельном есть &wizard-rules с корректным деревом запроса внутри
        в common-log не попадает ошибка парсинга qtree
        '''
        wizard_rules = '{"Market":{"qtree4market":"cHicPY2hCsJQGIW__yLuclkYWsaSLA3TxTRMYnFxmEwiYlgUk5h8BJNBTJoFndluvE_gs3g17NTvfOeYuQk1EbHukSlLR7u7e7jaPRNS-gwYhrrlOZ5jGVFQMmNBdT5-buognISLNFotvASft-BkZcllPNESkTSVlEysFKq8qv-QbFLTwEjFP0zOlGXba2TdSm9lp7RUwTrYC_7zC6vELtk,"}}'  # noqa
        self.report.request_bs(
            'place=parallel&text=абвг&rearr-factors=market_parallel_wizard=1&wizard-rules={}'.format(wizard_rules)
        )


if __name__ == '__main__':
    main()
