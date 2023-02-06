from random import randint
from typing import List


DEFAULT_SENDER = "nw@ya.ru"
DEFAULT_RCPT_0 = "nsls_0@ya.ru"
DEFAULT_RCPT_1 = "nsls_1@ya.ru"

_uids = set()


def gen_uid(is_pdd=False) -> int:
    rng = (3000000000, 4000000000)
    if is_pdd:
        rng = (1120000000000000, 1120000005000000)

    uid = randint(*rng)
    while uid in _uids:
        uid = randint(*rng)
    _uids.add(uid)
    return uid


class User:
    def __init__(self, email=DEFAULT_RCPT_0):
        self.email = email
        self.login, self.domain = self.get_login_domain()
        self.org_id = None
        self.uid = gen_uid()
        self.suid = gen_uid()
        self.stid = None
        self.is_shared_stid = False
        self.is_mailish = False

    def get_login_domain(self) -> List[str]:
        return self.email.split("@", 1)


def make_users(rcpts=[DEFAULT_RCPT_0, DEFAULT_RCPT_1]):
    return {rcpt: User(rcpt) for rcpt in rcpts}
