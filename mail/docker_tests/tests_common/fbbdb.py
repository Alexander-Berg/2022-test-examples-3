# coding: utf-8

from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_package

Q = load_from_package(__package__, __file__)


class User(object):
    def __init__(self, uid, suid, login, db, shard_id=None):
        self.uid = uid
        self.suid = suid
        self.login = login
        self.db = db
        self.shard_id = shard_id

    def __repr__(self):
        return \
            'User(' \
            'uid={0.uid}, suid={0.suid}, ' \
            'login={0.login}, db={0.db}, shard_id={0.shard_id})'.format(self)

    @property
    def mdb(self):
        return self.db

    @property
    def name(self):
        return self.login

    @property
    def email(self):
        domain = 'yandex-team.ru'
        return self.login + '@' + domain

    @property
    def imap_email(self):
        domain = 'mail.yandex-team.ru'
        return self.login + '@' + domain


def user_from_id(uid, user_name, suid=None):
    login = (user_name or 'login') + str(uid)
    return User(
        uid=uid,
        suid=suid or uid*1000,
        login=login,
        db='pg'
    )


def add_user(conn, user):
    qexec(
        conn,
        Q.add_user,
        uid=user.uid,
        suid=user.suid,
        login=user.login,
        db=user.db
    )


def is_user_exists(conn, uid):
    cur = qexec(
        conn,
        Q.is_user_exists,
        uid=uid,
    )
    for _ in cur:
        return True
    return False


def remove_user(conn, **kwargs):
    ids = {key: None for key in ['uid', 'suid', 'login']}
    ids.update(kwargs)
    qexec(conn, Q.remove_user, **ids)


def update_user_sids(conn, **kwargs):
    ids = {key: None for key in ['uid', 'suid', 'login']}
    ids['sids'] = []
    ids.update(kwargs)
    qexec(conn, Q.update_user_sids, **ids)


def add_corp_mailing_list(conn, user):
    qexec(
        conn,
        Q.add_corp_mailing_list,
        uid=user.uid,
        suid=user.suid,
        login=user.login,
        db=user.db
    )
