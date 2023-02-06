import json
import jwt
import psycopg2
import requests
import time

from abc import abstractmethod
from functools import lru_cache
from typing import List

from mail.template_master.load_testing.scripts.lib.util import decode_bytes, get_vault_secret


def _get_jwt(vault_secret_version: str):
    key_file = decode_bytes(get_vault_secret(vault_secret_version)['key'])
    key = json.loads(key_file)
    now = int(time.time())
    payload = {
        'aud': 'https://iam.api.cloud.yandex.net/iam/v1/tokens',
        'iss': key['service_account_id'],
        'iat': now,
        'exp': now + 300
    }
    token = jwt.encode(payload,
                       key['private_key'],
                       algorithm='PS256',
                       headers={'kid': key['id']})
    return decode_bytes(token)


def _get_iam_token(vault_secret_version: str):
    url = 'https://iam.cloud.yandex-team.ru/v1/tokens'
    headers = {'Content-Type': 'application/json'}
    data = json.dumps({'jwt': _get_jwt(vault_secret_version)})
    response = requests.post(url=url, headers=headers, data=data)
    return response.json()['iamToken']


class BaseDBConnectionProvider(object):
    '''
    Be aware that this guy is going to cache your connection string
    '''
    def __init__(self, dbname: str, password: str, sslrootcert_path: str, user: str):
        self.dbname = dbname
        self.password = password
        self.sslrootcert_path = sslrootcert_path
        self.user = user

    @abstractmethod
    def _get_hosts(self):
        pass

    def _get_hosts_as_string(self):
        return ','.join(self._get_hosts())

    @lru_cache(1)
    def get_connection_string(self):
        return f'''
            dbname={self.dbname}
            host={self._get_hosts_as_string()}
            password={self.password}
            port=6432
            sslrootcert={self.sslrootcert_path}
            sslmode=verify-full
            target_session_attrs=read-write
            user={self.user}'''


class DBConnectionProvider(BaseDBConnectionProvider):
    def __init__(self, dbname: str, hosts: List[str], password: str,
                 sslrootcert_path: str, user: str):
        super().__init__(dbname, password, sslrootcert_path, user)
        self.hosts = hosts

    def _get_hosts(self):
        return self.hosts


class YcDBConnectionProvider(BaseDBConnectionProvider):
    def __init__(self, cluster: str, dbname: str, password: str,
                 sslrootcert_path: str, user: str, vault_secret_version: str):
        super().__init__(dbname, password, sslrootcert_path, user)
        self.cluster = cluster
        self.vault_secret_version = vault_secret_version

    def _get_hosts(self):
        url = f'https://gw.db.yandex-team.ru/managed-postgresql/v1/clusters/{self.cluster}/hosts'
        r = requests.get(
            url, headers={'Authorization': f'Bearer {_get_iam_token(self.vault_secret_version)}'})
        return [host['name'] for host in r.json()['hosts']]


class DBAdaptor(object):
    def __init__(self, conninfo_provider: BaseDBConnectionProvider):
        self.conninfo_provider = conninfo_provider

    def get_cursor(self, query: str):
        '''
        Do not forget to call cursor.connection.commit() on the returned cursor manually if you've done any update.
        '''
        connection_string = self.conninfo_provider.get_connection_string()
        connection = psycopg2.connect(connection_string)
        cursor = connection.cursor()
        cursor.execute(query)
        return cursor
