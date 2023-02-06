# coding: utf-8

from contextlib import contextmanager

from pymdb.queries import Queries
from tests_common.register import make_maildb_conn


@contextmanager
def user_connection(context, uid):
    with make_maildb_conn(
        uid=uid,
        sharpei=context.config.sharpei,
        maildb_dsn_suffix='',
    ) as conn:
        yield conn


def current_user_connection(context):
    return user_connection(context, context.user.uid)


@contextmanager
def user_mdb_queries(context, uid):
    with user_connection(context, uid) as conn:
        yield Queries(conn, uid)


def current_user_mdb_queries(context):
    return user_mdb_queries(context, context.user.uid)
