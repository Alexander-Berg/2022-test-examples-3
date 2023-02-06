# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.common23.settings import *  # noqa
from travel.rasp.library.python.common23.tester.settings import *  # noqa


def add_installed_apps(*apps_to_add):
    global INSTALLED_APPS
    INSTALLED_APPS += list(set(apps_to_add) - set(INSTALLED_APPS))


INSTALLED_APPS = list(globals().setdefault('INSTALLED_APPS', [
    'travel.rasp.library.python.common23.tester'
]))

if 'tester' not in INSTALLED_APPS:  # check for admin
    add_installed_apps('travel.rasp.library.python.common23.tester')

if 'travel.rasp.admin.www' not in INSTALLED_APPS:  # check for admin
    try:
        import travel.rasp.library.python.common23.models.www  # noqa
        add_installed_apps('travel.rasp.library.python.common23.models.www', 'django.contrib.contenttypes')
    except ImportError:
        pass

try:
    import travel.rasp.library.python.common23.models.tariffs  # noqa
    add_installed_apps(
        'travel.rasp.library.python.common23.models.tariffs'
    )
except ImportError:
    pass

if 'currency' not in INSTALLED_APPS:  # check for admin
    try:
        import travel.rasp.library.python.common23.models.currency  # noqa
        add_installed_apps(
            'travel.rasp.library.python.common23.models.currency'
        )
    except ImportError:
        pass

try:
    import travel.rasp.library.python.common23.models.core  # noqa
    add_installed_apps(
        'travel.rasp.library.python.common23.models.texts',
        'travel.rasp.library.python.common23.models.transport',
        'travel.rasp.library.python.common23.models.core',
    )
except ImportError:
    pass

try:
    from travel.rasp.library.python.common23.db.mongo.tester import settings as mongo_settings
    add_installed_apps(*mongo_settings.INSTALLED_APPS)
except ImportError:
    pass
