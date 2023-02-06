# coding=utf-8
from __future__ import absolute_import

import ujson
from logging import Logger

from mock import Mock
from typing import cast

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.rest.currency.currency import CurrencyView
from travel.avia.backend.repository.currency import CurrencyRepository, CurrencyModel
from travel.avia.backend.repository.currency_translation import (
    CurrencyTranslationRepository,
    CurrencyTranslationModel
)


class CurrencyViewTest(TestCase):
    def setUp(self):
        self._currency_repository = Mock()
        self._translation_repository = cast(CurrencyTranslationRepository,
                                            Mock())

        self._translation_repository.get_translation = Mock(
            return_value=CurrencyTranslationModel(
                title=u'заголовок',
                title_in=u'заголовок в',
                template=u'шаблон',
                template_whole=u'шаблон целых',
                template_cents=u'шаблон копеек',
            ))

        self._view = CurrencyView(
            currency_repository=cast(
                CurrencyRepository,
                self._currency_repository
            ),
            logger=cast(Logger, Mock())
        )

    def test_view(self):
        self._currency_repository.get_all = Mock(
            return_value=[
                CurrencyModel(
                    translation_repository=self._translation_repository,
                    pk=123,
                    code=u'SOME_CODE',
                    iso_code=u'SOME_ISO_CODE',
                )
            ]
        )

        result = self._view._unsafe_process({
            'national_version': u'ru',
            'lang': u'ru',
        })

        assert ujson.loads(result.response[0]) == {
            u'status': u'ok',
            u'data': [
                {
                    u'id': 123,
                    u'code': u'SOME_CODE',
                    u'iso_code': u'SOME_ISO_CODE',
                    u'title': u'заголовок',
                    u'title_in': u'заголовок в',
                    u'template': u'шаблон',
                    u'template_whole': u'шаблон целых',
                    u'template_cents': u'шаблон копеек',
                }
            ]
        }
