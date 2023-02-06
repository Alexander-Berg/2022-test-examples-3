# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from sqlalchemy.engine.url import URL

from travel.rasp.bus.settings.base import BaseSettings
from travel.library.python.rasp_vault.api import get_secret


class Settings(BaseSettings):
    SQLALCHEMY_DATABASE_URI = URL(
        'postgresql+psycopg2',
        username='yandex_bus',
        password=get_secret('bus-common-testing.pg-password'),
        database='yandex_busdb_test2',
        query={
            'host': ','.join([
                'man-bnzetetlxbrswgjw.db.yandex.net',
                'sas-k6ru8hqn4lnnpbmk.db.yandex.net',
                'vla-w1e0xhbp8vpuk48e.db.yandex.net'
            ]),
            'port': '6432,6432,6432',
            'sslmode': 'verify-full',
            'sslrootcert': '/usr/share/yandex-internal-root-ca/YandexInternalRootCA.crt',
            'connect_timeout': '3',
            'target_session_attrs': 'read-write'
        }
    )

    class Admin(BaseSettings.Admin):
        TVM_CLIENT_ID = 2017327
        TVM_SECRET = get_secret('bus-common-testing.admin_tvm_secret')

    class Cache(BaseSettings.Cache):
        REDIS_HOST = None
        REDIS_DB = 1
        REDIS_SENTINEL_NAME = 'bus_redis_testing'
        REDIS_SENTINEL_PASSWORD = get_secret('bus-common-testing.redis-password')
        REDIS_SENTINEL_NODES = (
            ('vla-ea6ax7886wr7pcga.db.yandex.net', 26379),
            ('sas-s0kmxjw4jx8pcwo8.db.yandex.net', 26379),
            ('man-w1uoidyqn4y574cc.db.yandex.net', 26379),
        )

    class RaspAdmin(BaseSettings.RaspAdmin):
        URL = 'https://service.admin-test.rasp.yandex-team.ru'

    class PointMatchingSuggest(BaseSettings.PointMatchingSuggest):
        URL = 'https://testing.geo-api.internal.bus.yandex.net/points/matching-suggests/'

    class Meta(BaseSettings.Meta):
        URL = 'https://testing.api.internal.bus.yandex.net'

    class LogBroker(BaseSettings.LogBroker):
        ENABLED = True
        TOKEN = get_secret('bus-common-testing.robot-sputnik-logbroker-token')
        ADMIN_ACCESS_TOPIC = '/sputnik/testing/admin/access.log'

    class IDM(BaseSettings.IDM):
        TVM_CHECK_ENABLED = True
        TVM_CLIENT_ID = 2001600

    class Backend(BaseSettings.Backend):
        API_URL = 'https://testing.backend.internal.bus.yandex.net/api/'

    class S3(BaseSettings.S3):
        PUBLIC_BUCKET = 'travel.buses-public.testing'
