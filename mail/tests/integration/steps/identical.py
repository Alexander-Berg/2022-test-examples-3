# coding: utf-8
from contextlib import contextmanager

from tests_common.pytest_bdd import then

from ora2pg.compare import AreEqual
from ora2pg.pg_get import get_user
from ora2pg.transfer import get_connstring_by_id
from mail.pypg.pypg.common import get_connection


@contextmanager
def get_connection_by_shard_id(config, shard_id):
    with get_connection(
        get_connstring_by_id(
            sharpei=config.sharpei,
            shard_id=shard_id,
            dsn_suffix=config.maildb_dsn_suffix,
        )
    ) as conn:
        yield conn


@then(u'his metadata is identical')
def then_compare_metadata(context):
    return then_compare_metadata_impl(**locals())


def then_compare_metadata_impl(context, volatile_getter=lambda o: set()):
    with \
        get_connection_by_shard_id(context.config, context.first_shard_id) as first_conn, \
        get_connection_by_shard_id(context.config, context.second_shard_id) as second_conn \
    :
        assert AreEqual(
            sorter=sorted,
            volatile_getter=volatile_getter,
        )(
            l=get_user(uid=context.user.uid, conn=first_conn),
            r=get_user(uid=context.user.uid, conn=second_conn),
            name='User'
        )
