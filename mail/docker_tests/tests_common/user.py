# coding: utf-8

import logging
from tests_common.register import (
    register as register_user_in_mdb,
    fill_real_stids,
)
from tests_common.fbbdb import(
    add_user as add_user_to_fbb,
    user_from_id,
)

log = logging.getLogger(__name__)


def make_new_user(context, user_name=None):
    uid = context.get_free_uid()
    try:
        suid = context.get_private_suid()
    except AttributeError:
        suid = uid * 1000
    user = user_from_id(uid, user_name=user_name, suid=suid)
    log.info('ask for new uid=%r, new_user is %r', uid, user)
    return user


def remember_user_in_context(context, user, user_name):
    context.users.add(user, user_name)
    context.user = user


def register_user(context, user, user_name=None, is_mailish=False, empty=False, limit=50):
    fill_real_stids(
        context.config.mulcagate, limit=limit
    )
    register_user_in_mdb(
        uid=user.uid,
        shard_id=context.config.default_shard_id,
        sharddb=context.config.sharddb,
        sharpei=context.config.sharpei,
        maildb_dsn_suffix=context.config.maildb_dsn_suffix,
        is_mailish=is_mailish,
        empty=empty,
        context=context,
    )
    user.shard_id = context.config.default_shard_id
    remember_user_in_context(context, user, user_name)


def register_user_in_mdb_and_fbb(context, user, user_name=None, empty=False):
    register_user(context, user, user_name, empty=empty)
    add_user_to_fbb(context.fbbdb_conn, context.user)
    log.info('Current user is %r', context.user)


def make_user_oneline(context, user_name=None, empty=False):
    user = make_new_user(context, user_name)
    register_user_in_mdb_and_fbb(context, user, user_name, empty=empty)
    return user
