import os
import json
import requests
import time


VAULT_URL = 'https://vault-api.passport.yandex.net'


def get_root_dir():
    root_dir = os.path.dirname(os.path.abspath(__file__))
    return root_dir


def get_ora_config():
    user = get_secret_value_by_key('sec-01fhwhev29nmjxab7a8y425j8b', 'crm-user')
    connection_string = user + "@" + os.getenv("CRM_ORA_HOST")
    return connection_string


def get_secret_value_by_key(sec_id, key):
    token = os.getenv('VAULT_TOKEN')
    path = '/1/versions/' + sec_id
    headers = {'Authorization': 'OAuth ' + token}
    response = requests.get(VAULT_URL + path, headers=headers, verify=False)
    secret = json.loads(response.text)
    for pair in secret['version']['value']:
        if pair['key'] == key:
            return pair['value']

    raise Exception(f'Key {key} or {sec_id} not found in secret')


# in case of failure 'function' must return None
def wait_for_execution(function, timeout=10, period=1, *args, **kwargs):
    end_time = time.time() + timeout
    while time.time() < end_time:
        result = function(*args, **kwargs)
        if result is not None:
            return result
        time.sleep(period)
    return None
