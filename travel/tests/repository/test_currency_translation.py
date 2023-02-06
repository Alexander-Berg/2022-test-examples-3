from __future__ import absolute_import

from logging import Logger
from mock import Mock
from typing import cast

from travel.avia.library.python.avia_data.models import CurrencyLang
from travel.avia.library.python.tester.factories import (
    create_avia_currency,
    create_avia_currency_translation
)
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.repository.currency_translation import CurrencyTranslationRepository


class CurrencyTranslationRepositoryTest(TestCase):
    def setUp(self):
        self._repository = CurrencyTranslationRepository(
            logger=cast(Logger, Mock())
        )

        self.lang_to_id = {}
        for lang in ['ru', 'uk', 'en', 'tr', 'de']:
            m = CurrencyLang(
                title=lang,
                code=lang,
                enable=True,
            )
            m.save()

            self.lang_to_id[m.code] = m.id

    def test_fallbacks(self):
        rub = create_avia_currency(
            code='RUR',
            iso_code='RUB'
        )

        create_avia_currency_translation(
            currency=rub,
            lang_id=self.lang_to_id['ru'],
            title='ru!'
        )
        create_avia_currency_translation(
            currency=rub,
            lang_id=self.lang_to_id['en'],
            title='en!'
        )

        self._repository.fetch()

        assert self._repository.get_translation(
            currency_id=rub.id,
            lang='ru'
        ).title == 'ru!'
        assert self._repository.get_translation(
            currency_id=rub.id,
            lang='uk'
        ).title == 'ru!'
        assert self._repository.get_translation(
            currency_id=rub.id,
            lang='en'
        ).title == 'en!'
        assert self._repository.get_translation(
            currency_id=rub.id,
            lang='tr'
        ).title == 'en!'
        assert self._repository.get_translation(
            currency_id=rub.id,
            lang='de'
        ).title == 'en!'

    def test_difference_translation_for_different_currency(self):
        rub = create_avia_currency(
            code='RUR',
            iso_code='RUB'
        )
        dollar = create_avia_currency(
            code='Dollar',
            iso_code='Dollar'
        )

        create_avia_currency_translation(
            currency=rub,
            lang_id=self.lang_to_id['ru'],
            title='ru!rub'
        )
        create_avia_currency_translation(
            currency=dollar,
            lang_id=self.lang_to_id['ru'],
            title='ru!dollar'
        )

        self._repository.fetch()

        assert self._repository.get_translation(
            currency_id=rub.id,
            lang='ru'
        ).title == 'ru!rub'
        assert self._repository.get_translation(
            currency_id=dollar.id,
            lang='ru'
        ).title == 'ru!dollar'

    def test_currency_without_translation(self):
        rub = create_avia_currency(
            code='RUR',
            iso_code='RUB'
        )

        self._repository.fetch()

        for lang in ['ru', 'uk', 'en', 'tr', 'de']:
            assert self._repository.get_translation(
                currency_id=rub.id,
                lang=lang
            ).title == u''

    def test_unknown_currency(self):
        rub = create_avia_currency(
            code='RUR',
            iso_code='RUB'
        )

        self._repository.fetch()

        for lang in ['ru', 'uk', 'en', 'tr', 'de']:
            assert self._repository.get_translation(
                currency_id=rub.id + 1,
                lang=lang
            ).title == u''

    def test_unknown_lang(self):
        rub = create_avia_currency(
            code='RUR',
            iso_code='RUB'
        )

        create_avia_currency_translation(
            currency=rub,
            lang_id=self.lang_to_id['ru'],
            title='ru!rub'
        )

        self._repository.fetch()

        assert self._repository.get_translation(
            currency_id=rub.id,
            lang='unknown'
        ).title == u''

    def test_full_translate(self):
        rub = create_avia_currency(
            code='RUR',
            iso_code='RUB'
        )

        create_avia_currency_translation(
            currency=rub,
            lang_id=self.lang_to_id['ru'],
            title='ru!title',
            title_in='ru!title_in',
            template='ru!template',
            template_whole='ru!whole',
            template_cents='ru!cents'
        )

        self._repository.fetch()

        translate = self._repository.get_translation(
            currency_id=rub.id,
            lang='ru'
        )

        assert translate.title == 'ru!title'
        assert translate.title_in == 'ru!title_in'
        assert translate.template == 'ru!template'
        assert translate.template_whole == 'ru!whole'
        assert translate.template_cents == 'ru!cents'
