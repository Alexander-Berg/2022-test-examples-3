# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from django.conf import settings


def test_tests_settings():
    assert settings.TRY_HARD_NEVER_SLEEP
    assert settings.TRAIN_WIZARD_API_DIRECTION_HOST is None
    assert settings.TRAIN_WIZARD_API_INDEXER_HOST is None
    assert settings.TVM_FAKE
    assert not settings.DEBUG_TOOLBAR_PANELS
