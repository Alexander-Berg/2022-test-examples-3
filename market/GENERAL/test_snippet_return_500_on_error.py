#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer
from core.testcase import TestCase, main
from core.snippet import SAAS_REPLY_WITH_ERROR_SETTING


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.snippet_saas_throw_on_saas_error = True
        cls.index.offers += [
            Offer(title='iphone', title_snippet='snippet-phone'),
        ]

    def test_return_500_on_error(self):
        # Проверяем что репорт вернет 500 если снипетный сервис ответил пятисоткой и в конфиге репорта есть опция SnippetSaasThrowOnSaasError=1
        self.snippets.change_settings(
            {
                SAAS_REPLY_WITH_ERROR_SETTING: True,
            }
        )
        response = self.report.request_plain('place=print_doc&text=iphone&rearr-factors=ext_snippet=1', strict=False)
        self.assertEqual(response.code, 500)
        # На всякий случай проверяем что репорт отвечает нормально если снипетный сервис работает
        self.snippets.change_settings(
            {
                SAAS_REPLY_WITH_ERROR_SETTING: False,
            }
        )
        response = self.report.request_json('place=print_doc&text=iphone&rearr-factors=ext_snippet=1')
        self.assertFragmentIn(response, {'title': 'snippet-phone'})


if __name__ == '__main__':
    main()
