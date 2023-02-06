# coding: utf-8

from datetime import datetime

from dateutil import parser

from pymdb.operations import DeleteUser
from mail.pypg.pypg.common import qexec
from mail.pypg.pypg.query_conf import load_from_my_file
from tests_common.pytest_bdd import when, given

Q = load_from_my_file(__file__)


@when('we delete user at "{date}"')
def step_delete_user_dated(context, date):
    delete_user(**locals())


@when('we delete user')
def step_delete_user(context):
    delete_user(**locals())


def delete_user(context, date=None):
    if date:
        date = parser.parse(date)
    else:
        date = datetime.now()
    context.apply_op(DeleteUser, deleted_date=date)


@given('he has in storage delete queue')
def step_fill_storage_delete_queue(context):
    for row in context.table:
        date = row.get('deleted_date')
        if date:
            date = parser.parse(date)
        else:
            date = datetime.now()
        qexec(context.conn,
              Q.add_to_storage_delete_queue,
              uid=context.uid,
              st_id=row['st_id'],
              deleted_date=date)
        context.conn.commit()
