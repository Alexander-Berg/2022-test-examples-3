from .testing import *  # pylint: disable=wildcard-import,unused-wildcard-import


BLACKBOX['url'] = 'https://blackbox.yandex-team.ru/blackbox'
BLACKBOX['url_external'] = 'https://pass-test.yandex.ru/blackbox'  # testing accounts
BLACKBOX['default_tvm_section'] = None  # do not use tvm accessing blackbox by default

DJANGO_AUTHENTICATION_BACKENDS = [
    'cars.core.authorization.auth_mechanism.Mechanism',
    'django.contrib.auth.backends.ModelBackend',
    # Disable BlackBox auth at all
    # 'django_yauth.authentication_mechanisms.cookie.Mechanism',
    # 'django_yauth.authentication_mechanisms.oauth.Mechanism',
]

YAUTH_MECHANISMS = [
    'cars.core.authorization.auth_mechanism',
    # Disable BlackBox auth at all
    # 'django_yauth.authentication_mechanisms.cookie',
    # 'django_yauth.authentication_mechanisms.oauth',
]
