# coding: utf-8
import random
from string import ascii_lowercase
from collections import namedtuple
import logging

from behave import given
import requests

from mail.pypg.pypg.common import transaction
from pymdb.operations import Init
from ora2pg.sharpei import init_in_sharpei, get_connstring_by_id

User = namedtuple('User', ('login', 'uid'))
log = logging.getLogger(__name__)


def login_by_user_name(user_name):
    return user_name + '-' + ''.join(random.choice(ascii_lowercase) for _ in range(10))


def init_user_in_blackbox(context, user_name):
    login = login_by_user_name(user_name)
    url = context.config.make_blackbox_url('1/bundle/account/register/intranet/')
    req = requests.get(
        url,
        dict(login=login, is_maillist='no')
    )
    log.info("url: %r, req is %r", url, req)
    uid = req.json()['uid']
    user = User(login=login, uid=uid)
    if 'users' not in context:
        context.users = {}
    context.users[user_name] = user
    return user


def init_user_in_sharpei(context, user, shard_id):
    init_in_sharpei(
        user.uid,
        context.config.sharddb_dsn,
        allow_inited=False,
        shard_id=shard_id,
    )


def init_user_in_maildb(context, user, shard_id):
    with transaction(
        get_connstring_by_id(
            context.config.sharpei,
            shard_id,
            context.config.mdb_dsn_suffix)
    ) as conn:
        Init(conn, user.uid)()


@given('new user "{user_name:UserName}" in {shard:Shard} shard')
def create_new_user(context, user_name, shard):
    user = init_user_in_blackbox(context, user_name)
    init_user_in_sharpei(context, user, shard.value)
    init_user_in_maildb(context, user, shard.value)
