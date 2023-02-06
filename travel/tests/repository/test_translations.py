# coding=utf-8
from __future__ import absolute_import

import itertools
from mock import patch

from travel.avia.backend.repository.translations import TranslatedTitleRepository
from travel.avia.library.python.tester.factories import create_translated_title
from travel.avia.library.python.tester.testcase import TestCase


class TestTranslatedTitleRepository(TestCase):
    ids = [1, 2]  # Идентификаторы полных переводов
    fallback_ids = [3, 4]  # Идентификаторы переводов, где нужен fallback

    def setUp(self):
        with patch.object(TranslatedTitleRepository, '_load_models') as load_models_mock:
            load_models_mock.side_effect = self._get_models
            self._repository = TranslatedTitleRepository()
            self._repository.fetch(set(self.ids + self.fallback_ids))

    def test_get(self):
        for pk, lang in itertools.product(self.ids, ['ru', 'uk', 'en', 'de', 'tr']):
            assert self._repository.get(pk, lang) == lang + '_nominative_' + str(pk)

        for pk in self.fallback_ids:
            assert self._repository.get(pk, 'ru') == 'ru_nominative_' + str(pk)
            assert self._repository.get(pk, 'uk') == 'ru_nominative_' + str(pk)
            for lang in ['en', 'de', 'tr']:
                assert self._repository.get(pk, lang) is None

    def test_get_genitive(self):
        for pk in self.ids:
            assert self._repository.get_genitive(pk, 'ru') == 'ru_genitive_' + str(pk)

        for pk in self.fallback_ids:
            assert self._repository.get_genitive(pk, 'ru') is None

        for pk, lang in itertools.product(self.ids + self.fallback_ids, ['uk', 'en', 'de', 'tr']):
            assert self._repository.get_genitive(pk, lang) == ''

    def test_get_accusative(self):
        for pk, lang in itertools.product(self.ids, ['ru', 'uk']):
            assert self._repository.get_accusative(pk, lang) == lang + '_accusative_' + str(pk)

        for pk, lang in itertools.product(self.fallback_ids, ['ru', 'uk']):
            assert self._repository.get_accusative(pk, lang) is None

        for pk, lang in itertools.product(self.ids + self.fallback_ids, ['en', 'de', 'tr']):
            assert self._repository.get_accusative(pk, lang) == ''

    def test_get_locative(self):
        for pk in self.ids:
            assert self._repository.get_locative(pk, 'ru') == 'ru_locative_' + str(pk)

        for pk in self.fallback_ids:
            assert self._repository.get_locative(pk, 'ru') is None

        for pk, lang in itertools.product(self.ids + self.fallback_ids, ['uk', 'en', 'de', 'tr']):
            assert self._repository.get_locative(pk, lang) == ''

    def test_all_langs(self):
        translated_title = create_translated_title(
            ru_nominative='ru_some',
            uk_nominative='ua_some',
            en_nominative='en_some',
            tr_nominative='tr_some',
            de_nominative='de_some',
        )

        repository = TranslatedTitleRepository()
        repository.fetch({translated_title.id})

        assert repository.get(translated_title.id, 'ru') == 'ru_some'
        assert repository.get(translated_title.id, 'uk') == 'ua_some'
        assert repository.get(translated_title.id, 'en') == 'en_some'
        assert repository.get(translated_title.id, 'tr') == 'tr_some'
        assert repository.get(translated_title.id, 'de') == 'de_some'

    def test_two_fetch(self):
        translated_title = create_translated_title(
            ru_nominative='ru_some'
        )
        other_translated_title = create_translated_title(
            ru_nominative='ru_other'
        )

        repository = TranslatedTitleRepository()
        repository.fetch({translated_title.id, other_translated_title.id})

        assert repository.get(translated_title.id, 'ru') == 'ru_some'
        assert repository.get(other_translated_title.id, 'ru') == 'ru_other'

    def test_sequence_fetch(self):
        repository = TranslatedTitleRepository()
        translated_title = create_translated_title(ru_nominative='ru_some', de_nominative='de_some')
        repository.fetch({translated_title.id, translated_title.id})
        other_translated_title = create_translated_title(ru_nominative='ru_other', de_nominative='de_other')
        repository.fetch({translated_title.id, other_translated_title.id})

        assert repository.get(translated_title.id, 'ru') == 'ru_some'
        assert repository.get(translated_title.id, 'de') == 'de_some'

        assert repository.get(other_translated_title.id, 'ru') == 'ru_other'
        assert repository.get(other_translated_title.id, 'de') == 'de_other'

    def _get_models(self, ids):
        names = [
            TranslatedTitleRepository.nominative_key(lang) for lang in TranslatedTitleRepository.ALL_LANGS
        ] + TranslatedTitleRepository.CASES

        result = []
        for pk in ids:
            model = {
                'id': pk,
            }
            if pk in self.fallback_ids:
                model.update({
                    name: name + '_' + str(pk) if name == 'ru_nominative' else None for name in names
                })
            else:
                model.update({
                    name: name + '_' + str(pk) for name in names
                })

            result.append(model)

        return result
