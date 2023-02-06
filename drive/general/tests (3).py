import decimal
import os

from .utils import set_secret_default

set_secret_default('CARSHARING_ANIMALS_TOKEN',            'CARSHARING_ANIMALS_TOKEN',            '')
set_secret_default('CARSHARING_ANIMALS_TOKEN',            'CARSHARING_ANIMALS_TOKEN_LOGIN',      '')
set_secret_default('CARSHARING_FRONTEND_TVM2_SECRET',     'CARSHARING_FRONTEND_TVM2_SECRET',     '')
set_secret_default('CARSHARING_AUDIOTELE_AUTH_TOKEN',     'CARSHARING_AUDIOTELE_AUTH_TOKEN',     '')
set_secret_default('CARSHARING_AUDIOTELE_BASIC_AUTH_PASSWORD', 'CARSHARING_AUDIOTELE_BASIC_AUTH_PASSWORD', '')
set_secret_default('CARSHARING_AUDIOTELE_FTP_PASSWORD',   'CARSHARING_AUDIOTELE_FTP_PASSWORD',   '')
set_secret_default('CARSHARING_AUDIOTELE_TRACKS_AES_KEY', 'CARSHARING_AUDIOTELE_TRACKS_AES_KEY', '0' * 32)
set_secret_default('CARSHARING_AUDIOTELE_TOKEN_SALT',     'CARSHARING_AUDIOTELE_TOKEN_SALT',     '')
set_secret_default('CARSHARING_AUTO_TAGS_TOKEN',          'CARSHARING_AUTO_TAGS_TOKEN',          '')
set_secret_default('CARSHARING_AUTOCODE_SECRET_ID',       'CARSHARING_AUTOCODE_SECRET_ID',       '')
set_secret_default('CARSHARING_AUTOCODE_SECRET_ID',       'CARSHARING_YNDX_FINES_TOKEN',           '')
set_secret_default('CARSHARING_BEEPER_FTP_PASSWORD',      'CARSHARING_BEEPER_FTP_PASSWORD',      '')
set_secret_default('CARSHARING_BEEPER_TRACKS_AES_KEY',    'CARSHARING_BEEPER_TRACKS_AES_KEY', '0' * 32)
set_secret_default('CARSHARING_BLACKBOX_TVM2_SECRET',     'CARSHARING_BLACKBOX_TVM2_SECRET',     '')
set_secret_default('CARSHARING_CALLCENTER_MDS_ACCESS_KEY_ID', 'CARSHARING_CALLCENTER_MDS_ACCESS_KEY_ID', '')
set_secret_default('CARSHARING_CALLCENTER_MDS_SECRET_ACCESS_KEY', 'CARSHARING_CALLCENTER_MDS_SECRET_ACCESS_KEY', '')
set_secret_default('CARSHARING_CALLCENTER_SECRET_KEY',    'CARSHARING_CALLCENTER_SECRET_KEY',    '')
set_secret_default('CARSHARING_CHAT2DESK_API_TOKEN',      'CARSHARING_CHAT2DESK_API_TOKEN',      '')
set_secret_default('CARSHARING_CHAT2DESK_AUTH_TOKEN',     'CARSHARING_CHAT2DESK_AUTH_TOKEN',     '')
set_secret_default('CARSHARING_DEPTRANS_TOKEN',           'CARSHARING_DEPTRANS_TOKEN',           '')
set_secret_default('CARSHARING_DOCS_AES_KEY',             'CARSHARING_DOCS_AES_KEY',       '0' * 64)
set_secret_default('CARSHARING_DOCS_HASHES_KEY',          'CARSHARING_DOCS_HASHES_KEY',    '0' * 32)
set_secret_default('CARSHARING_EKA_PASSWORD',             'CARSHARING_EKA_PASSWORD',             '')
set_secret_default('CARSHARING_MDS_ACCESS_KEY_ID',        'CARSHARING_MDS_ACCESS_KEY_ID',        '')
set_secret_default('CARSHARING_MDS_SECRET_ACCESS_KEY',    'CARSHARING_MDS_SECRET_ACCESS_KEY',    '')
set_secret_default('CARSHARING_OCTOPUS_API_KEY',          'CARSHARING_OCTOPUS_API_KEY',          '')
set_secret_default('CARSHARING_ROBOT_CARSHARING_TOKEN',   'CARSHARING_ROBOT_CARSHARING_TOKEN',   '')
set_secret_default('CARSHARING_ROBOT_DRV_REQ_TOKEN',      'CARSHARING_ROBOT_DRV_REQ_TOKEN',      '')
set_secret_default('CARSHARING_SENDER_TOKEN',             'CARSHARING_SENDER_TOKEN',             '')
set_secret_default('CARSHARING_SOCIAL_TVM2_SECRET',       'CARSHARING_SOCIAL_TVM2_SECRET',       '')
set_secret_default('CARSHARING_STAFF_TOKEN',              'CARSHARING_STAFF_TOKEN',              '')
set_secret_default('CARSHARING_STARTREK_TOKEN',           'CARSHARING_STARTREK_TOKEN',           '')
set_secret_default('CARSHARING_TOLOKA_TOKEN',             'CARSHARING_TOLOKA_TOKEN',             '')
set_secret_default('CARSHARING_TRUST_SERVICE_TOKEN',      'CARSHARING_TRUST_SERVICE_TOKEN',      '')
set_secret_default('CARSHARING_TVM2_SECRET',              'CARSHARING_TVM2_SECRET',              '')
set_secret_default('CARSHARING_XIVA_SEND_TOKEN',          'CARSHARING_XIVA_SEND_TOKEN',          '')
set_secret_default('CARSHARING_XIVA_SUBSCRIPTION_TOKEN',  'CARSHARING_XIVA_SUBSCRIPTION_TOKEN',  '')
set_secret_default('CARSHARING_YANG_ROBOT_OAUTH_TOKEN',   'CARSHARING_YANG_ROBOT_OAUTH_TOKEN',   '')
set_secret_default('CARSHARING_YQL_TOKEN',                'CARSHARING_YQL_TOKEN',                '')
set_secret_default('CARSHARING_YT_TOKEN',                 'CARSHARING_YT_TOKEN',                 '')

set_secret_default('CARS_DB_DEFAULT_PASSWORD',             'CARS_DB_DEFAULT_PASSWORD',              '')
set_secret_default('CARS_DB_QA_PASSWORD',                  'CARS_DB_QA_PASSWORD',                   '')
set_secret_default('CARSHARING_RENAISSANCE_AUTH_KEY',      'CARSHARING_RENAISSANCE_AUTH_KEY',       '')

set_secret_default('CARSHARING_FEATURE_ADMIN_PERMISSIONS', 'CARSHARING_FEATURE_ADMIN_PERMISSIONS', '1')

from .base import *  # pylint: disable=wildcard-import,unused-wildcard-import,wrong-import-position


IS_TESTS = True


USER = os.environ.get('USER', 'anonymous')


ADMIN['acl'] = None
ADMIN['internal_callcenter_acl'] = None


CALLCENTER['call_registration_server_uid'] = 1
CALLCENTER['link_patterns']['user_card'] = '{}'
CALLCENTER['link_patterns']['enter_phone'] = '{}'


DB = {
    'default': {
        'host': os.environ.get('CARSHARING_TESTS_DB_HOST', 'zpd-01.search.yandex.net'),
        'port': 5432,
        'name': 'carsharing_dev_{}'.format(USER),
        'user': USER,
        'password': USER,
    },
}


CALCULATOR['client']['class'] = 'cars.calculator.core.client.StubCalculatorClient'


CARSHARING['registry_manager']['join_threads'] = True
CARSHARING['tags']['token'] = None


DRIVE['intro']['user_setup_class'] = 'cars.drive.tests.stubs.user_setup.StubUserSetup'


_idx = DJANGO_MIDDLEWARE.index('cars.users.middleware.YandexAuthenticationMiddleware')
DJANGO_MIDDLEWARE[_idx] = 'cars.users.middleware.TestYandexAuthenticationMiddleware'


FINES['yandexoid_only'] = False
FINES['drive_only'] = False
FINES['emails_only'] = None
FINES['charge_limit'] = 5000
FINES['charge_time_limit'] = None


MDS['client_class'] = 'cars.core.mds.client.StubMDSClient'
MDS['encrypted_client_class'] = 'cars.core.mds.client.StubMDSClient'


OCTOPUS['client_class'] = 'cars.core.octopus.StubOctopus'


ORDERS['debt_order_termination']['delay'] = decimal.Decimal(0)
ORDERS['debt_order_termination']['threshold'] = decimal.Decimal(0)


PASSPORT['verify_ssl'] = False


REFUEL['telematics_commands_delay'] = 0.0
REFUEL['warming']['delay'] = 0.0


REGISTRATION_YANG['verifier']['class'] = 'cars.registration_yang.core.verifier.YangSecretIdVerifierStub'


SENDER['client_class'] = 'unittest.mock.MagicMock'


SERVICE_APP['roles'] = {
    'cleaner': {
        2,
        4,
    },
    'supervisor': {
        1,
    },
    'technician': {
        3,
        5,
    },
}
SERVICE_APP['temp_cleaning_car_numbers'] = None
SERVICE_APP['min_app_build'] = -1


SOLOMON = {
    'url': '',
    'project': 'carsharing',
    'service': 'api',
    'cluster': 'dev',
    'push_interval': 15,
}


TELEMATICS['proxy_class'] = 'cars.carsharing.core.telematics_proxy.TelematicsProxyStub'


TOLOKA['client_class'] = 'unittest.mock.MagicMock'


TRUST['client_class'] = 'cars.core.trust.StubTrustClient'


XIVA['client_class'] = 'unittest.mock.MagicMock'


YASMS['client_class'] = 'unittest.mock.MagicMock'


YAUTH_TEST_USER = {
    'login': '1',  # Login is forced as username in django_yauth.user.YandexTestUser.
    'default_email': 'test@yandex.ru',
}
