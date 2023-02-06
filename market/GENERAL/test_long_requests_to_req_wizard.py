#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_long_requests_to_req_wizard(cls):
        cls.reqwizard.on_default_request().respond()
        cls.settings.disable_long_requests_to_req_wizard = True
        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

    def test_long_requests_to_req_wizard(self):
        """
        С флагом DisableLongRequestsToReqWizard в конфиге не задаем в визард
        запросы с большим количеством слов.
        """

        trace_error_text = 'Request to reqwizard is forbidden'

        def n_word_request(n, place):
            return 'debug=da&place={}&text={}'.format(place, '+'.join(['word'] * n))

        # Задаём запрос с 11 словами. Проверяем, что в дебажную выдачу попало предупреждение
        # о том, что запрос в реквизард отправлен не будет
        response = self.report.request_json(n_word_request(11, 'prime'))
        self.assertFragmentIn(response, trace_error_text)

        # Проверяем, что для запроса с 10 словами предупреждения нет
        response = self.report.request_json(n_word_request(10, 'prime'))
        self.assertFragmentNotIn(response, trace_error_text)


if __name__ == '__main__':
    main()
