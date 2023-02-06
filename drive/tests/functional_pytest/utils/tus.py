import logging
import random
import string

from drive.qatool.tus.api import TusClient
from utils.yav import get_version

TUS_CLIENT = TusClient(token=get_version().get("value")["TUS_TOKEN"])


def create_tus_account():
    name = ''.join((random.choice(string.ascii_lowercase) for x in range(7)))
    account = TUS_CLIENT.create_account(login=f'yndx-drive-{name}',
                                        tus_consumer='drive-autotest-back',
                                        env='test')
    return account


def get_tus_account():
    account = TUS_CLIENT.get_account(tus_consumer='drive-autotest-back',
                                     lock_duration=360
                                     )
    logging.info(f'account {account.login}')
    return account


def unlock_tus_account(uid):
    TUS_CLIENT.unlock_account(uid=uid)
