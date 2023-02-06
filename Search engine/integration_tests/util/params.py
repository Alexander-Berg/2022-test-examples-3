# -*- coding: utf-8 -*-
import os
import json
import pytest

from library.python.vault_client.instances import Production as VaultClient


class Singleton:
    # On @ decoration
    def __init__(self, klass):
        self.klass = klass
        self.instance = None

    # On instance creation
    def __call__(self, *args, **kwargs):
        if self.instance is None:
            self.instance = self.klass(*args, **kwargs)
        return self.instance


@Singleton
class Vault():
    def __init__(self):
        self.head_version = None
        self.secret_id = os.environ.get('SECRET_VERSION', 'sec-01f3dnxszdxw71xyv8fszr22hy')

        if self.secret_id:
            client = VaultClient(decode_files=True, authorization=os.environ.get('OAUTH_TOKEN'))
            self.head_version = client.get_version(self.secret_id)

    def get_secret(self):
        return self.head_version


def get_env_bool(key):
    return os.environ.get(key) == '1'


def soy_mode():
    return get_env_bool('SOY_MODE')


def logging_mode():
    return os.environ.get('LOGGING_MODE', 'INFO')


def exp_confs():
    return os.environ.get('EXP_CONFS')


def test_id():
    return os.environ.get('TEST_ID')


def get_beta_host():
    # do not run tests if no BETA_HOST === BETA_URL specified; protection of ddos by autocheck
    # throw exception; more protection of random launches
    host = os.environ.get('BETA_URL', os.environ.get('BETA_HOST'))
    if host is None:
        pytest.exit('Env BETA_HOST not found.')

    return host


def get_soy_result():
    return os.environ.get('SOY_RESULT')


def get_soy_pool():
    return os.environ.get('SOY_POOL', 'search_integration_tests')


def get_yt_batch_table_folder():
    return os.environ.get('YT_BATCH_TABLE_FOLDER', '//home/search-runtime/search_integration-tests')


def get_soy_map_type():
    return os.environ.get('SOY_MAP_OP_TYPE', 'http')


def _message_for_bad_secret():
    return 'Can not find secret: {}'.format(os.environ.get('SECRET_VERSION'))


def get_soy_token():
    v = Vault().get_secret()
    if v is None:
        pytest.exit(_message_for_bad_secret())

    return v['value']['SOY_TOKEN']


def get_soy_public_key():
    if not Vault().secret_id:
        return None

    v = Vault().get_secret()
    if v is None:
        pytest.exit(_message_for_bad_secret())

    return v['value']['SOY_PUBLIC_KEY']


NOT_SET_SECRET_ERROR = 'Secret is not set to get partners info'


def get_xml_partners():
    if not Vault().secret_id:
        return NOT_SET_SECRET_ERROR

    v = Vault().get_secret()
    if v is None:
        pytest.exit(_message_for_bad_secret())

    return json.loads(v['value']['TEST_XML_PARTNERS'])


def get_xml_banners_old_partners():
    if not Vault().secret_id:
        return NOT_SET_SECRET_ERROR

    v = Vault().get_secret()
    if v is None:
        pytest.exit(_message_for_bad_secret())

    return json.loads(v['value']['TEST_XML_BANNERS_OLD_PARTNERS'])


def get_xml_banners_new_partners():
    if not Vault().secret_id:
        return NOT_SET_SECRET_ERROR

    v = Vault().get_secret()
    if v is None:
        pytest.exit(_message_for_bad_secret())

    return json.loads(v['value']['TEST_XML_BANNERS_NEW_PARTNERS'])


def get_yt_token():
    v = Vault().get_secret()
    if v is None:
        pytest.exit(_message_for_bad_secret())

    return v['value']['YT_TOKEN']


# robot-srch-int-tests
def get_test_user_login():
    v = Vault().get_secret()
    if v is None:
        pytest.exit(_message_for_bad_secret())

    return v['value']['TEST_USER_LOGIN']


def get_test_user_passwd():
    v = Vault().get_secret()
    if v is None:
        pytest.exit(_message_for_bad_secret())

    return v['value']['TEST_USER_PASSWD']


def get_test_user_xml_key():
    v = Vault().get_secret()
    if v is None:
        pytest.exit(_message_for_bad_secret())

    return v['value']['TEST_USER_XML_KEY']


def get_test_user_xml_ip():
    v = Vault().get_secret()
    if v is None:
        pytest.exit(_message_for_bad_secret())

    return v['value']['TEST_USER_XML_IP']
