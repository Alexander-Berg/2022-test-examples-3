from .base import *

# External urls
STARTREK_URL = 'https://st-api.test.yandex-team.ru'
STARTREK_TVM2_ID = 177
STARTREK_TVM2_CLIENT_ID = 2023458

class CacheBBProxy(blackbox.XmlBlackbox):
    URL = 'http://pass-stress-s1.sezam.yandex.net/blackbox'


YAUTH_BLACKBOX_INSTANCE = CacheBBProxy()

ALLOWED_HOSTS += [
        '.lunapark.test.yandex-team.ru',
        '.lunapark.test.yandex-team.ru.',
        'lunapark.test.yandex-team.ru',
        'lunapark.test.yandex-team.ru.',
        '.lunapark-test.n.yandex-team.ru',
        '.lunapark-test.n.yandex-team.ru.',
        'lunapark-test.n.yandex-team.ru',
        'lunapark-test.n.yandex-team.ru.',
        'lunapark-testing.in.yandex-team.ru',
        'lunapark-develop.in.yandex-team.ru'
    ]
