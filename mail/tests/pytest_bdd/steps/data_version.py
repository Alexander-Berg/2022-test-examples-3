# coding: utf-8
import logging

from tests_common.pytest_bdd import given, then
from .accidents import qexec_raw_str

log = logging.getLogger(__name__)


@given('data version is set to minimum')
def step_set_dv_to_minimum(context):
    cur = context.conn.cursor()
    cur.execute(
        'SELECT constants.minimal_data_version()',
    )
    context.conn.wait()
    min_dv = cur.fetchone()[0]
    qexec_raw_str(
        context.conn,
        'UPDATE mail.users SET data_version = %s WHERE uid = %s',
        min_dv, context.uid,
    )


@then('data version is maximum')
def step_check_dv_is_maximum(context):
    cur = context.conn.cursor()
    cur.execute(
        'SELECT constants.newest_data_version()',
    )
    context.conn.wait()
    max_dv = cur.fetchone()[0]
    assert context.qs.user().data_version == max_dv
