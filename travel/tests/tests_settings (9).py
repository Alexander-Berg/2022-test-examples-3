# -*- coding: utf-8 -*-
# flake8: noqa
from __future__ import absolute_import

import os
import mock


cwd = os.getcwd()
os.environ.setdefault('AVIA_TICKET_DAEMON_ENVIRONMENT_TYPE', 'development')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRET_KEY', 'Some')
os.environ.setdefault('AVIA_TICKET_DAEMON_LOG_PATH', os.path.join(cwd, 'log'))
os.environ.setdefault('AVIA_TICKET_DAEMON_PARTNER_QUERY_TIMEOUT', '0')
os.environ.setdefault('AVIA_TICKET_DAEMON_REGISTER_REDIRECT', 'true')
os.environ.setdefault('AVIA_TICKET_DAEMON_REVISE_FORCE', 'false')
os.environ.setdefault('AVIA_TICKET_DAEMON_MCR_HOST', 'example.com')
os.environ.setdefault('AVIA_TICKET_DAEMON_REDIS_SENTINEL_SERVICE_NAME', 'mymaster')
os.environ.setdefault('AVIA_TICKET_DAEMON_CURRENCY_RATES_URL', 'example.com')
os.environ.setdefault('AVIA_TICKET_DAEMON_FEATURE_FLAG_HOST', 'example.com')
os.environ.setdefault('AVIA_TICKET_DAEMON_BOOKING_SERVICE_URL', 'example.com')
os.environ.setdefault('AVIA_TICKET_DAEMON_YDB_DATABASE', 'ydb_db')
os.environ.setdefault('AVIA_TICKET_DAEMON_YDB_ENDPOINT', 'ydb_endpoint')
os.environ.setdefault('AVIA_TICKET_DAEMON_WIZARD_YDB_DATABASE', 'ydb_db')
os.environ.setdefault('AVIA_TICKET_DAEMON_WIZARD_YDB_ENDPOINT', 'ydb_endpoint')
os.environ.setdefault('AVIA_TICKET_DAEMON_SENTRY_DSN', 'http://PLEASE:USE@sentry.testing.avia.yandex.net/YOUR_SENTRY')

os.environ.setdefault('AVIA_TICKET_DAEMON_SQS_ACCESS_KEY', 'avia')
os.environ.setdefault('AVIA_TICKET_DAEMON_SQS_SECRET_KEY', 'unused')
os.environ.setdefault('AVIA_TICKET_DAEMON_SQS_PREFIX', 'dev-login')
os.environ.setdefault('AVIA_TICKET_DAEMON_SQS_ENDPOINT', 'example.com')

os.environ.setdefault('AVIA_TICKET_DAEMON_LOGBROKER_TOKEN', 'see readme')

os.environ.setdefault('DEPLOY_NODE_DC', 'sas')

os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AEROTUR_API_KEY', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AGENT3_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AMADEUS_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AVAI_CASSA_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AVAI_CASSA_SHAREDSECRET', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AVIAOPERATOR2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AZIMUTH_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_BILETIK5_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_BILETIK6_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_BILETIX2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_BILETIX2_SHAREDSECRET', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_BILETIX_KZ_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_BILETIX_KZ_SHAREDSECRET', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_BILETONLINE_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_BOOKANDTRIP_RUPASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_BOOKANDTRIP_UAPASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_BRAVOAVIA_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CHABOOKA2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CHARTEOK_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CHARTERBILET2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CHARTERBILET_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CITYTRAVEL_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CITYTRAVEL_KZ_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CITYTRAVEL2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CITYTRAVEL2_KZ_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CITYTRAVEL2_COM_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CSA_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_ETRAVELI_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_EXPRESSAVIA3_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_EXPRESSAVIA4_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_JUSTTRAVEL_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_KUPIBILET_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_KUPIBILET_KZ_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_MAU_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_NABORTU_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_NEMOBOOK_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_ONETWOTRIP32_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_ONETWOTRIP33_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_ONETWOTRIP_KZPASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_ONETWOTRIP_RUPASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_ONETWOTRIP_UAPASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_RUSLINE2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_RUSLINE3_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_RUSLINE4_SIG_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_SINBAD2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_SUPERKASSA2_HASH', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_SUPERKASSA2_HASH_KZ', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_SUPERKASSA2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_SUPERKASSA_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_SUPERKASSA_HASH', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_S_SEVEN2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_S_SEVEN3_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_S_SEVEN4_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_TICKETSUA2_KZPASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_TICKETSUA2_RUPASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_TICKETSUA2_TRPASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_TICKETSUA2_UAPASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_TINKOFF_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_TRIPSA4_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_TRIPSA5_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_UFS_PLANE2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_UTAIRWAYS2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_UTAIR_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AVIAKASSA4_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AVIAKASSA5_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_TEZ_TOUR_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_SUPERSAVER_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_SUPERSAVER2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AEROFLOT_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AEROFLOT2_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AEROFLOT2_YANDEX_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_BILETINET_PASSWORD', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CTRIP_RU_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CTRIP_TR_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CTRIP_KZ_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CTRIP_UA_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_CTRIP_COM_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_MEGOTRAVEL_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_POBEDA_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_POBEDA_CLIENT_ID', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_DOHOP_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_AEGEAN_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_U6_TEST_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_U6_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_ANYWAYANYDAY_PARTNER', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_MYTRIP_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_FLYONE_PASSWORD', '!!!SECRET')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_SMARTAVIA_SALEPOINT', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_SMARTAVIA_TRAFFICSOURCE', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_SMARTAVIA_SEARCH_ENGINE', 'see readme')
os.environ.setdefault('AVIA_TICKET_DAEMON_SECRETS_TUTU_PASSWORD', 'see readme')

os.environ.setdefault('AVIA_TICKET_DAEMON_TRAVEL_HOST_FOR_RU', 'travel-test.yandex.ru')
os.environ.setdefault('AVIA_TICKET_DAEMON_FRONT_HOST_FOR_RU', 'avia-frontend.some-login.avia.dev.yandex.ru')
os.environ.setdefault('AVIA_TICKET_DAEMON_FRONT_HOST_FOR_COM', 'avia-frontend.some-login.avia.dev.yandex.com')
os.environ.setdefault('AVIA_TICKET_DAEMON_FRONT_HOST_FOR_TR', 'avia-frontend.some-login.avia.dev.yandex.com.tr')
os.environ.setdefault('AVIA_TICKET_DAEMON_FRONT_HOST_FOR_UA', 'avia-frontend.some-login.avia.dev.yandex.ua')
os.environ.setdefault('AVIA_TICKET_DAEMON_FRONT_HOST_FOR_KZ', 'avia-frontend.some-login.avia.dev.yandex.kz')

os.environ.setdefault('AVIA_TICKET_DAEMON_AEROFLOT_WHITE_MONDAY_COMPARE_METHOD', 'startswith')
os.environ.setdefault('AVIA_TICKET_DAEMON_AEROFLOT_WHITE_MONDAY_COMPARE_VALUES', 'RNOR,RCOR,RFOR')
os.environ.setdefault('AVIA_TICKET_DAEMON_MYSQL_HOST', os.getenv('AVIA_MYSQL_HOST', '127.0.0.1'))
os.environ.setdefault('AVIA_TICKET_DAEMON_MYSQL_PORT', os.getenv('AVIA_MYSQL_PORT', ''))
os.environ.setdefault('AVIA_TICKET_DAEMON_MYSQL_DATABASE_NAME', os.getenv('AVIA_MYSQL_DATABASE', 'aviatest'))
os.environ.setdefault('AVIA_TICKET_DAEMON_MYSQL_TEST_DATABASE_NAME', os.getenv('AVIA_MYSQL_TEST_DATABASE', 'aviatest2'))
os.environ.setdefault('AVIA_TICKET_DAEMON_MYSQL_PASSWORD', os.getenv('AVIA_MYSQL_PASSWORD', ''))
os.environ.setdefault('AVIA_TICKET_DAEMON_MYSQL_USER', os.getenv('AVIA_MYSQL_USER', 'root'))

os.environ.setdefault('AVIA_TICKET_DAEMON_PRICE_PREDICTION_HOST', 'unknown')

from travel.avia.ticket_daemon.ticket_daemon.settings import *
from travel.avia.library.python.proxy_pool.deploy_proxy_pool import DeployProxyPool


DEBUG = True
MAINTENANCE_DB_NAME = None
assert DATABASES['default']['HOST'] in ('localhost', '127.0.0.1')
assert DATABASES['default']['NAME']

for key in list(DATABASES.keys()):
    if key != 'default':
        del DATABASES[key]

REAL_DB_NAME = DATABASES['default']['NAME']
DATABASES['default']['NAME'] = TEST_DB_NAME

# disable all caches for tests
default_cache_settings = {
    'BACKEND': 'django.core.cache.backends.dummy.DummyCache',
    'LONG_TIMEOUT': 86400,
    'TIMEOUT': 60,
}
CACHES = {
    alias: default_cache_settings.copy()
    for alias in ['default', 'shared_cache', 'memcached']
}

ENABLE_TVM = False
LOGGING['root']['handlers'].remove('sentry')

# чтобы не ходить вовне
CURRENCY_RATES_URL = None
PATHFINDER_URL = None
QUERY_TICKET_DAEMON = False
ALLOW_GEOBASE = True


DeployProxyPool.get_proxy = mock.MagicMock(return_value=None)
