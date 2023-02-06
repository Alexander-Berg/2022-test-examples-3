import copy
import decimal

from cars.aggregator.static_data import clients, operators
from .base import *  # pylint: disable=wildcard-import,unused-wildcard-import


IS_TESTS = False


API['location_prefix'] = 'aggregator_testing'
STATIC_URL = '/{}/static/'.format(API['location_prefix'])

BLACKBOX['url'] = 'https://blackbox.yandex.net/blackbox'
BLACKBOX['url_external'] = 'https://blackbox.yandex.net/blackbox'


ADMIN['acl'] = {
    4006777567,  # testcar42
    4008546646,  # rabbitinspace
    4008564906,  # extdata-testuser
    4008631372,  # yndx-carsharing-qweqwe
    4008720366,  # accmanager
    4008830682,  # accmanagertest
    4008832680,  # accman
    4008862662,  # accman2
    4008894404,  # andreevich-test
    4008948870,  # accman5
    4008954170,  # uid-ilg4btd4
    4008955822,  # extdata-testuser2
    4008966436,  # zxqfd-test1
    4008968316,  # zxqfd-test2
    4008994326,  # uid-p5fxxi5n
    4009007910,  # zxqfd-test3
    4009112700,  # uid-hcoviovt
    4009119620,  # uid-7v3vmijh
    4009137056,  # uid-4cwdpxtg
    4009139232,  # accmantesting
    4009159420,  # zxqfd-test4
    4009187314,  # zxqfd55
    4009187322,  # zxqfd100
    4009220704,  # uid-vac25tfc
    4009722002,  # unlying
    4009959856,  # drive-callcenter
    4010022036,  # toplivovbak-testing
    4010110282,  # drive-test
    4010132908,  # rtline-frontend
    4010309528,  # drive-test2
    4010315064,  # drive-android-dev
    4010368402,  # service-app-washer
    4010368530,  # service-app-technic
    4010934936,  # taran-m
    1120000000090013,  # egorkutz
}
ADMIN['internal_callcenter_acl'] = {
    4010610384,  # internal-cc-test
}
ADMIN['transport']['banned_car_ids'] = {
    '3b9c69e0-6370-405f-a41c-9d736e5e6252'
}


BILLING['payment_processor_tick_interval'] = '*/5 * * * *'


CALCULATOR['client']['root_url'] = (
    'https://carsharing-calculator-testing.n.yandex-team.ru/api/calculator/v1/'
)
CALCULATOR['client']['verify_ssl'] = False


CARSHARING['reservation']['constraints'] = [
    {
        'span': datetime.timedelta(minutes=1),
        'count': 1,
    },
    {
        'span': datetime.timedelta(minutes=5),
        'count': 10,
    },
]
CARSHARING['reservation']['constrained_max_duration_seconds'] = 17
CARSHARING['reservation']['full_max_duration_seconds'] = 1 * 60


DB = {
    'default': {
        'host': 'c-e4b922c1-9356-43e4-a0fa-6f3d7b54c962.rw.db.yandex.net',
        'port': '6432',
        'name': 'extmaps-carsharing-testing',
        'user': 'carsharing',
        'password': get_secret('CARS_DB_DEFAULT_PASSWORD', 'CARS_DB_DEFAULT_PASSWORD'),
    },
}

DB_RO_ID = 'ro'

DB[DB_RO_ID] = copy.deepcopy(DB['default'])
DB[DB_RO_ID]['host'] = 'c-e4b922c1-9356-43e4-a0fa-6f3d7b54c962.ro.db.yandex.net'


DEPTRANS['acl'] = {
    4009159420,  # deptrans-rnis-production
}


DRIVE['carsharing']['car_list_location_updated_at_max_delta'] = None
DRIVE['early_access']['enabled'] = False


FEEDS['solomon']['cluster'] = 'carsharing_testing'


GEOCODER['url'] = 'http://geocode.maps.yandex.net/1.x/'


LOGGING['handlers']['sentry'] = {
    'level': 'ERROR',
    'class': 'raven.handlers.logging.SentryHandler',
    'dsn': 'http://69801c593d724bfe84afd31632535aa0:de599d566fe24f3795b9c20d9a3fee5c@sentry.test.tools.yandex.net/582',  # pylint: disable=line-too-long
}
LOGGING['loggers']['']['handlers'].append('sentry')


FEATURES['banned_operators'][clients.AGGREGATOR_BETA] = {
    operators.YOUDRIVE_MSK,
    operators.YOUDRIVE_SPB,
}
FEATURES['banned_operators'][clients.TRANSPORT] = {
    operators.YOUDRIVE_MSK,
    operators.YOUDRIVE_SPB,
}


MDS['verify_ssl'] = False


# OCTOPUS['client_class'] = 'cars.core.octopus.StubOctopusFailing'
OCTOPUS['verify_ssl'] = False


ORDERS['chunked_payment_amount'] = decimal.Decimal('16')
ORDERS['debt_order_termination']['delay'] = 45 * 60
ORDERS['debt_order_termination']['threshold'] = decimal.Decimal('0')
ORDERS['debt_retry_interval'] = datetime.timedelta(minutes=1)
ORDERS['export']['path'] = '//home/carsharing/testing/orders'
ORDERS['preliminary_payment_amount'] = decimal.Decimal('8')


PASSPORT['verify_ssl'] = False


REGISTRATION['taxi_cashback']['phones_table_path'] = \
    '//home/extdata/carsharing/testing/promo/registration-taxi-cashback/registered-users'


REGISTRATION_YANG['verifier']['verify_requests'] = False
REGISTRATION_YANG['verifier']['base_url'] = (
    'https://sandbox.yang.yandex-team.ru/api/v1/assignments/'
)


SENDER['client_class'] = 'cars.core.sender.SenderClientTesting'

SENTRY['feeds'] = ('http://b79528013eb04811ac235f9df2a11d46:b86cbe5cd2f74397bfbe6ad0f84d1053'
                   '@sentry.test.tools.yandex.net/591')


SERVICE_APP['roles'] = {
    'cleaner': {
        4010368402,  # service-app-washer
    },
    'supervisor': {
        4010368724,  # service-app-dev
    },
    'technician': {
        4010368530,  # service-app-technic
    },
}


SOLOMON['cluster'] = 'testing'


TELEMATICS['packet_handlers']['saas_submission']['status_cache']['get_state_url'] = (
    'https://carsharing-testing.n.yandex.ru/api/admin/v1/car-info-by-imei/status/'
)
TELEMATICS['packet_handlers']['saas_submission']['ride_cache']['get_state_url'] = (
    'https://carsharing-testing.n.yandex.ru/api/admin/v1/car-info-by-imei/ride/'
)
TELEMATICS['proxy_class'] = 'cars.carsharing.core.telematics_proxy.TelematicsProxyStub'

TRUST['verify_ssl'] = False
TRUST['back_url'] = 'https://carsharing-testing.n.yandex-team.ru/api/billing/v1/trust/notify/'


USERS['export_table'] = '//home/extdata/carsharing/testing/users/export'
USERS['fast_export_table'] = '//home/extdata/carsharing/testing/users/fast_export'


YT['data']['base_dir'] = '//home/extdata/carsharing/testing'
YT['locks']['base_dir'] = '//home/extmaps/carsharing/testing'


ZOOKEEPER['workdir'] = '/cars/testing/alerts'


EXPORT['tmp_path'] = '//home/carsharing/testing/data/user/_tmp'
EXPORT['temp_path'] = '//home/carsharing/testing/temp'
EXPORT['locks_config']['base_dir'] = '//home/carsharing/testing'
EXPORT['home_directory'] = '//home/carsharing/testing'
EXPORT['users_table_part'] = '//home/carsharing/testing/data/user/_parts/users_table'
EXPORT['orders_table_part'] = '//home/carsharing/testing/data/user/_parts/orders_table'
EXPORT['bonus_account_table_part'] = '//home/carsharing/testing/data/user/_parts/bonus_account_table'
EXPORT['autocode_fine_table_part'] = '//home/carsharing/testing/data/user/_parts/autocode_fine_table'
EXPORT['billing_tasks_table_part'] = '//home/carsharing/testing/data/user/_parts/billing_tasks_table'
EXPORT['datasync_part'] = '//home/carsharing/testing/data/user/_parts/datasync'
EXPORT['security_part'] = '//home/carsharing/testing/data/user/_parts/security'
EXPORT['user_profiles_part'] = '//home/carsharing/testing/data/user/_parts/user_profiles'
EXPORT['users_apps_part'] = '//home/carsharing/testing/data/user/_parts/users_apps'
EXPORT['users_crypta_part'] = '//home/carsharing/testing/data/user/_parts/users_crypta'
EXPORT['users_crypta_profiles_part'] = '//home/carsharing/testing/data/user/_parts/users_crypta_profiles'
EXPORT['current_score_part'] = '//home/carsharing/testing/data/user/_parts/current_score'
EXPORT['plus_subscribers_part'] = '//home/carsharing/testing/data/user/_parts/plus_subscribers'
EXPORT['license_loss_part'] = '//home/carsharing/testing/data/user/_parts/license_loss'
EXPORT['bin_attr_part'] = '//home/carsharing/testing/data/user/_parts/bin_attr'
EXPORT['users_bin_attr_part'] = '//home/carsharing/testing/data/user/_parts/users_bin_attr'
EXPORT['users_table'] = '//home/carsharing/testing/data/user/user_attributes'
EXPORT['orders_table'] = '//home/carsharing/testing/data/orders'
EXPORT['table_refund_issues_history'] = '//home/carsharing/testing/database/refund_issues_history'
