from __future__ import absolute_import

from logging import Logger

from mock import Mock
from typing import cast

from travel.avia.library.python.avia_data.models import CurrencyLang
from travel.avia.backend.repository.currency import CurrencyRepository
from travel.avia.backend.repository.currency_translation import CurrencyTranslationRepository
from travel.avia.library.python.tester.factories import (
    create_avia_currency,
    create_avia_currency_translation
)
from travel.avia.library.python.tester.testcase import TestCase


class CurrencyRepositoryTest(TestCase):
    def setUp(self):
        self._translation_repository = CurrencyTranslationRepository(
            logger=cast(Logger, Mock())
        )
        self._repository = CurrencyRepository(
            translation_repository=self._translation_repository
        )

    def test_all_methods_for_two_different_currency(self):
        currency = create_avia_currency(
            code='_RC',     # RUR_CODE
            iso_code='IRC'  # ISO_RUR_CODE
        )

        dollar = create_avia_currency(
            code='_DC',     # DOLLAR_CODE
            iso_code='IDC'  # ISO_DOLLAR_CODE
        )

        self._repository.fetch()

        assert self._repository.get_by_id(currency.id).code == '_RC'
        assert self._repository.get_by_code(currency.code).code == '_RC'
        assert self._repository.get_by_iso_code(
            currency.iso_code).code == '_RC'

        assert self._repository.get_by_id(dollar.id).code == '_DC'
        assert self._repository.get_by_code(dollar.code).code == '_DC'
        assert self._repository.get_by_iso_code(
            dollar.iso_code).code == '_DC'

    def test_translation(self):
        currency = create_avia_currency(
            code='_RC',     # RUR_CODE
            iso_code='IRC'  # ISO_RUR_CODE
        )

        lang = CurrencyLang(
            title='ru',
            code='ru',
            enable=True,
        )
        lang.save()

        create_avia_currency_translation(
            currency=currency,
            lang=lang,
            title='ru!rub'
        )

        self._translation_repository.fetch()
        self._repository.fetch()

        assert self._repository.get_by_id(
            currency.id).get_title('ru') == 'ru!rub'
