from tests_common.pytest_bdd import given, then
from mail.devpack.lib.components.mdb import Mdb


@given(u'archive state is "{state}"')
def step_update_archive_state(context, state):
    context.coordinator.components[Mdb].execute('''
            INSERT INTO
                mail.archives (uid, state)
            VALUES
                ({uid}, '{state}')
            ON CONFLICT (uid) DO UPDATE SET
                state = excluded.state
        '''.format(uid=context.params['uid'], state=state))


@then(u'archive state is "{state}"')
def step_check_archive_state(context, state):
    rows = context.coordinator.components[Mdb].query(
        '''
            SELECT state
              FROM mail.archives
             WHERE uid = %(uid)s
        ''',
        uid=context.params['uid'],
    )
    assert rows[0][0] == state
