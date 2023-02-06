import getpass
import logging

import requests

LOGGER = logging.getLogger(__name__)


def get_token_by_password(client_id, client_secret, login=None, password=None):
    if login is None:
        login = getpass.getuser()
    if password is None:
        password = getpass.getpass()

    r = requests.post(
        'https://oauth-test.yandex.ru/token',
        data={
            'grant_type': 'password',
            'client_id': client_id,
            'client_secret': client_secret,
            'username': login,
            'password': password,
        },
    )

    r.raise_for_status()
    return r.json()['access_token']


def get_drive_token(login, password):
    token = get_token_by_password(client_id='c92a6d00cdcc412e8c8cc72cf0b7cd4d',
                                  client_secret='4d4d8e6bf8e447ff92532f7d347df304',
                                  login=login,
                                  password=password
                                  )
    return token
