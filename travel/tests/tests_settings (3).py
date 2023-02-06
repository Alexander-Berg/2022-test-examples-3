# -*- coding: utf-8 -*-
from __future__ import absolute_import

from django.utils.translation import ugettext_lazy as _

from local_settings import *  # noqa
from travel.avia.library.python.tester.arcadia_tests_settings import *  # noqa

ENABLE_TVM = False

# disable shared_cache cache for tests
CACHES['shared_cache'] = {  # noqa
    'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
    'LONG_TIMEOUT': 86400,
    'TIMEOUT': 60,
}

DOMAIN_LANGUAGE_MAP = {
    'ru': ('ru', ['ru', 'en']),
    'ua': ('uk', ['uk', 'ru', 'en']),
    'tr': ('tr', ['tr', 'en']),
    'com': ('en', ['en', 'de']),
    'kz': ('ru', ['ru']),
}
AVIA_NATIONAL_VERSIONS = tuple(DOMAIN_LANGUAGE_MAP.keys())

LANGUAGES = [
    ('ru', _(u'Русский')),
    ('uk', _(u'Украинский')),
    ('tr', _(u'Турецкий')),
    ('en', _(u'Английский')),
    ('de', _(u'Немецкий')),
]

DATABASES['writable'] = DATABASES['default']  # noqa
