# coding:utf-8
import os
from functools import partial

from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_package

Q = load_from_package(__package__, __file__)


def fetch_max(conn, query, **kwargs):
    row = qexec(conn, query, **kwargs).fetchone()
    return (row[0] or 0) if row else 0


find_max_shard_id = partial(fetch_max, query=Q.sharddb_max_shard_id)


def make_range_from(min_uid):
    min_uid = int(os.environ.get('MIN_UID', min_uid))
    max_uid = int(os.environ.get('MAX_UID', min_uid + 10 * 1000))

    return min_uid, max_uid


class UIDRanges(object):
    transfer = make_range_from(100 * 1000)
    husky = make_range_from(200 * 1000)
    york = make_range_from(400 * 1000)
    system = make_range_from(1000 * 1000)


class UIDHolder(object):
    def __init__(self, uid_range, sharddb_conn=None, fbbdb_conn=None):
        self.min_uid, self.max_uid = uid_range
        self.used_uid_getters = []
        if sharddb_conn:
            self.used_uid_getters.append(lambda: fetch_max(
                sharddb_conn, Q.sharddb_max_uid, min_uid=self.min_uid, max_uid=self.max_uid
            ))
        if fbbdb_conn:
            self.used_uid_getters.append(lambda: fetch_max(
                fbbdb_conn, Q.fbbdb_max_uid, min_uid=self.min_uid, max_uid=self.max_uid
            ))

    def __call__(self):
        max_used_uid = self.min_uid
        for g in self.used_uid_getters:
            max_used_uid = max(max_used_uid, g())
        if max_used_uid >= self.max_uid:
            raise RuntimeError('No uids left in range %d:%d' % (self.min_uid, self.max_uid))
        self.min_uid = max_used_uid + 1
        return self.min_uid


class UsersHolder(object):
    DEFAULT_USER = 'Anonymous'

    def __init__(self):
        self.users = {}

    def __iter__(self):
        return iter(self.users.values())

    def add(self, user, user_name=None):
        user_name = user_name or self.DEFAULT_USER
        self.users[user_name] = user

    def get(self, user_name=None):
        user_name = user_name or self.DEFAULT_USER
        assert user_name in self.users, \
            "Don't know user with name %r" % user_name
        return self.users[user_name]

    def __getitem__(self, item):
        return self.get(item)

    def __setitem__(self, key, value):
        return self.add(user=value, user_name=key)

    @property
    def default(self):
        return self.get()

    def forget(self):
        self.users = {}


class ShardNameHolder(object):
    def __init__(self, next_id):
        self.next_shard_id = next_id

    def __call__(self):
        shard_id = self.next_shard_id
        self.next_shard_id += 1
        return 'xdb%d' % shard_id


class PrivateSuidHolder(object):
    def __init__(self, min_suid=None, max_suid=None, fbbdb_conn=None):
        self.min_suid = min_suid or int(os.environ.get('MIN_PRIVATE_SUID'))
        if self.min_suid is None:
            raise RuntimeError(
                'Env var MIN_PRIVATE_SUID must be set. '
                'Look into private_suid_ranges table in mdb305 for details;'
            )
        self.max_suid = max_suid or int(os.environ.get('MAX_PRIVATE_SUID', self.min_suid + 1000*1000))

        self.used_suid_getters = []
        self.used_suid_getters.append(lambda: fetch_max(
            fbbdb_conn, Q.fbbdb_max_suid, min_suid=self.min_suid, max_suid=self.max_suid
        ))
        self.suid = self.min_suid

    def __call__(self):
        max_used_suid = self.suid
        for g in self.used_suid_getters:
            max_used_suid = max(max_used_suid, g())

        if max_used_suid < self.max_suid:
            self.suid = max_used_suid + 1
            return self.suid
        raise RuntimeError(
            'No suids left in [{}:{}). Hint: increase MAX_PRIVATE_SUID '
            'env var'.format(self.min_suid, self.max_suid)
        )
