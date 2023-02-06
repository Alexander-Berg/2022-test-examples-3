# coding: utf-8
from pymdb.operations import PurgeUser

from tests_common.pytest_bdd import when


def purge_user_step_by_step(executer, purge_steps):
    for purge_proc in purge_steps:
        proc_finished = False
        while not proc_finished:
            proc_finished = executer(purge_proc)


class PurgeProcExecuter(object):
    def __init__(self, conn, uid):
        self.conn = conn
        self.uid = uid

    def __call__(self, proc_name):
        query = 'SELECT {0}(%(uid)s)'.format(proc_name)
        cur = self.conn.cursor()
        cur.execute(query, dict(uid=self.uid))
        # ARCH: remove async blowjobs from tests!
        self.conn.wait()
        return cur.fetchone()[0]


@when(u'we purge him')
@when(u'we purge user')
def step_purge_user(context):
    purge_user_step_by_step(
        PurgeProcExecuter(context.conn, context.uid),
        context.qs.get_purge_steps())


@when(u'we purge him on delete')
@when(u'we purge user on delete')
def step_purge_user_on_delete(context):
    purge_user_step_by_step(
        PurgeProcExecuter(context.conn, context.uid),
        context.qs.get_purge_on_delete_steps())


@when(u'we purge him in one transaction')
def step_purge_user_in_one_transaction(context):
    context.apply_op(PurgeUser)
