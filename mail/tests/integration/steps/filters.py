from contextlib import contextmanager

from mail.pypg.pypg.common import transaction
from pymdb.queries import Queries
from pymdb.operations import CreateRule
from ora2pg.sharpei import get_pg_dsn_from_sharpei

from tests_common.pytest_bdd import given, then
from hamcrest import (assert_that,
                      has_item,
                      has_properties)


@contextmanager
def get_maildb_conn(context, uid):
    dsn = get_pg_dsn_from_sharpei(
        sharpei=context.config.sharpei,
        uid=uid,
        dsn_suffix=context.config.maildb_dsn_suffix)
    with transaction(dsn) as conn:
        yield conn


@given('"{user_name:w}" has {enabled:Enabled} filter "{filter_type:w}" to "{param:w}"')
def step_create_filter(context, user_name, enabled, filter_type, param):
    uid = context.users.get(user_name).uid
    with get_maildb_conn(context, uid) as conn:
        CreateRule(conn, uid)(
            name='test filter for %s' % user_name,
            enabled=enabled,
            stop=False,
            last=False,
            acts=[filter_type, param, 'yes'],
            conds=['header', 'from', 'apple', 'contains', 'or', 'no'],
            old_rule_id=None
        )


@then('"{user_name:w}" has {enabled:Enabled} filter "{filter_type:w}" to "{param:w}"')
def step_user_has_not_filters(context, user_name, enabled, filter_type, param):
    uid = context.users.get(user_name).uid
    with get_maildb_conn(context, uid) as conn:
        filters = Queries(conn, uid).filters()
        assert_that(
            filters,
            has_item(
                has_properties(
                    enabled=enabled,
                    actions=has_item(
                        has_properties(
                            oper=filter_type,
                            param=param,
                        )
                    )
                )
            )
        )
