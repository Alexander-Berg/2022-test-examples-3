import re

from typing import Tuple, List, Optional

from random import randint

YA_TEAM_REGEX = re.compile(re.escape("@yandex-team.ru"), re.IGNORECASE)
MAIL_YA_TEAM_REGEX = re.compile(re.escape("@mail.yandex-team.ru"), re.IGNORECASE)

_uids = set()


def gen_uid(is_long=False) -> int:
    rng = (3000000000, 4000000000)
    if is_long:
        rng = (1120000000000000, 1120000005000000)

    uid = randint(*rng)
    while uid in _uids:
        uid = randint(*rng)
    _uids.add(uid)
    return uid


class User:
    def __init__(self, email: str, uid: int = None, is_ml=False, is_bad_karma=False,
                 is_temp_bad_karma=False, is_threshold_karma=False, is_empty_1000_suid=False,
                 is_hosted: bool = False, is_email_blocked=False,
                 empty_password=False, is_no_eula=False, country="ru",
                 is_mdbreg=False, is_zero_suid=False, is_phone_confirmed=False, is_registered_in_blackbox=True, token=None):
        self.email = email
        self.is_corp = email.endswith("yandex-team.ru")
        self.login, self.domain = self.get_login_domain()
        self.passwd = "" if empty_password else "password"
        self.token = token
        self.is_ml = is_ml
        self.is_hosted = is_hosted
        self.is_email_blocked = is_email_blocked
        self.subscribers = {} if self.is_ml else None
        self.uid = uid or gen_uid(self.is_corp or self.is_hosted)
        self.suid = 0 if is_zero_suid else gen_uid(self.is_corp or self.is_hosted)
        self.is_eula_accepted = False
        self.is_bad_karma = is_bad_karma
        self.is_temp_bad_karma = is_temp_bad_karma
        self.is_threshold_karma = is_threshold_karma
        self.is_empty_1000_suid = is_empty_1000_suid
        self.is_no_eula = is_no_eula
        self.country = country
        self.is_mdbreg = is_mdbreg
        self.is_phone_confirmed = is_phone_confirmed
        self.is_registered_in_blackbox = is_registered_in_blackbox
        self.has_org_id = False

    def get_login_domain(self) -> List[str]:
        return self.email.split("@", 1)

    def add_subscriber(self, user: "User") -> None:
        assert self.is_ml
        self.subscribers[user.email] = user

    def set_country(self, country):
        self.country = country


class BigMLUser(User):
    def __init__(self, *args, **kwargs):
        super(BigMLUser, self).__init__(*args, is_hosted=True, **kwargs)
        assert not self.is_corp
        self.domid = hash(self.domain)
        self.is_eula_accepted = True

    def get_login_domain(self) -> Tuple[str, str]:
        return self.email, self.email.split("@", 1)[-1]


class Assessor(User):
    def __init__(self, *args, **kwargs):
        super(Assessor, self).__init__(*args, **kwargs)
        assert self.is_corp


class UserWithAppPasswordEnabled(User):
    def __init__(self, *args, **kwargs):
        super(UserWithAppPasswordEnabled, self).__init__(*args, **kwargs)


class HostedUser(BigMLUser):
    def __init__(self, *args, **kwargs):
        super(HostedUser, self).__init__(*args, **kwargs)


class CorpList(User):
    def __init__(self, *args, **kwargs):
        super(CorpList, self).__init__(*args, is_ml=True, **kwargs)
        assert self.is_corp and self.is_empty_1000_suid


def to_mail_yandex_team(email: str) -> str:
    return YA_TEAM_REGEX.sub("@mail.yandex-team.ru", email)


def from_mail_yandex_team(email: str) -> str:
    return MAIL_YA_TEAM_REGEX.sub("@yandex-team.ru", email)


class Users:
    def __init__(self):
        self._users = {}
        self._token_to_user = {}

    def add(self, user: User) -> None:
        self._users[user.email] = user
        if user.token:
            self._token_to_user[user.token] = user

    def get(self, email) -> Optional[User]:
        return self._users.get(email.lower())

    def get_by_login(self, login, is_corp=False):
        return self.get(make_email(login, is_corp))

    def get_by_token(self, token) -> Optional[User]:
        return self._token_to_user.get(token)


def gen_big_ml(total_subscribers: int, users: Users):
    ml = BigMLUser(f"otdel_{total_subscribers}@bigmltest.yaconnect.com", is_ml=True)
    for idx in range(total_subscribers):
        user = BigMLUser(f"user_{idx}@bigmltest.yaconnect.com")
        ml.add_subscriber(user)
        users.add(user)
    users.add(ml)
    return ml


def gen_corp_ml(total_subscribers: int, users: Users):
    ml = make_by_login(f"ml_{total_subscribers}", is_ml=True)
    for idx in range(total_subscribers):
        user = make_by_login(f"user_{idx}")
        ml.add_subscriber(user)
        users.add(user)
    users.add(ml)
    return ml


def make_by_login(login: str, corp: bool = False, **kwargs) -> User:
    return User(make_email(login, corp), **kwargs)


def make_email(login: str, corp: bool = False) -> str:
    domain = "yandex-team.ru" if corp else "yandex.ru"
    return login + "@" + domain


def gen_users(total: int, users: Users, corp: bool = False):
    new_users = []
    for idx in range(total):
        user = make_by_login(f"user_{idx}", corp)
        users.add(user)
        new_users.append(user)
    return new_users
