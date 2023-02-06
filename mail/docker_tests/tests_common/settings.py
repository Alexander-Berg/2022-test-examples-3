# coding: utf-8

from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_package

QUERIES = load_from_package(__package__, __file__)


def create_settings_in_setdb(uid, settings, conn):
    return qexec(
        conn=conn,
        query=QUERIES.create_settings_in_setdb,
        uid=uid,
        settings=settings
    ).fetchone()[0]


def get_settings_in_xdb(uid, conn):
    return qexec(
        conn=conn,
        query=QUERIES.get_settings_in_xdb,
        uid=uid
    ).fetchone()[0]


def delete_settings_in_xdb(uid, conn):
    return qexec(
        conn=conn,
        query=QUERIES.delete_settings_in_xdb,
        uid=uid
    ).fetchone()[0]
