# -*- coding: utf-8 -*-
import pytest
from django.utils import translation


class LanguageActivator(object):
    def set_language(self, language):
        self._language = translation.get_language()
        translation.activate(language)

    def rollback_language(self):
        if self._language:
            translation.activate(self._language)


@pytest.yield_fixture
def with_language():
    def set_language(language):
        translation.activate(language)

    original_lang = translation.get_language()
    yield set_language
    translation.activate(original_lang)
