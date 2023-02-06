# coding: utf-8
from contextlib import contextmanager
from email.mime.text import MIMEText
import logging

from ora2pg.sharpei import (init_in_sharpei,
                            get_connstring_by_id,
                            get_pg_dsn_from_sharpei,)
from mail.pypg.pypg.common import transaction
from mail.pypg.pypg.logged_connection import unlogged
from pymdb.vegetarian import fill_data, make_actions
from pymdb.types import register_types
from pymdb.operations import Init


def make_mime(action, action_target):
    msg = MIMEText('Body for {action} a {action_target}'.format(
        **locals()))
    msg['Subject'] = 'Subject for {action} a {action_target}'.format(
        **locals())
    msg['From'] = '%s@ya.ru' % action_target
    msg['To'] = '%s-lover@ya.ru'
    from six import PY2
    return str(msg) if PY2 else msg.as_bytes()


@contextmanager
def changed_logging_level(logger_name, to_log_level):
    logger = logging.getLogger(logger_name)
    original_level = logger.level
    logger.setLevel(to_log_level)
    try:
        yield
    finally:
        logger.setLevel(original_level)


def make_stids_for_user(mulcagate, user_key, limit=50):
    with changed_logging_level('ora2pg.tools.http', logging.WARNING):
        for action_and_target in make_actions(limit):
            yield mulcagate.put(user_key, make_mime(*action_and_target))


class _RealStids(object):
    stids = []

    @classmethod
    def fill_real_stids(cls, mulcagate, limit=50):
        if cls.stids:
            return
        cls.stids = list(make_stids_for_user(mulcagate, '42', limit))


def fill_real_stids(mulcagate, limit=50):
    _RealStids.fill_real_stids(mulcagate, limit=limit)


def fill_user_data(maildb_dsn, uid, stids, limit_per_folder=5, is_mailish=False, empty=False, context=None):
    with transaction(maildb_dsn) as conn:
        with unlogged(conn):
            if empty:
                Init(conn, uid)().commit()
            else:
                fill_data(conn, uid, limit_per_folder, stids, is_mailish=is_mailish, context=context)


@contextmanager
def make_inited_connection(dsn):
    with transaction(dsn) as conn:
        register_types(conn)
        yield conn


@contextmanager
def make_maildb_conn(uid, sharpei, maildb_dsn_suffix):
    maildb_dsn = get_pg_dsn_from_sharpei(sharpei, uid, maildb_dsn_suffix)
    with make_inited_connection(maildb_dsn) as conn:
        yield conn


@contextmanager
def make_maildb_conn_by_shard_id(sharpei, shard_id, maildb_dsn_suffix):
    maildb_dsn = get_connstring_by_id(sharpei, shard_id, maildb_dsn_suffix)
    with make_inited_connection(maildb_dsn) as conn:
        yield conn


def register(uid, shard_id, sharddb, sharpei, maildb_dsn_suffix, is_mailish=False, empty=False, context=None):
    assert _RealStids.stids, 'Call %r first!' % fill_real_stids
    init_in_sharpei(uid, sharddb, True, shard_id)
    maildb_dsn = get_connstring_by_id(sharpei, shard_id, maildb_dsn_suffix)
    fill_user_data(maildb_dsn, uid, _RealStids.stids, is_mailish=is_mailish, empty=empty, context=context)
