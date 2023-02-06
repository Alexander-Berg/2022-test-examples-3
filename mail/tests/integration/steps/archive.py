from tests_common.mdb import user_connection
from pymdb.queries import Queries


def change_archive_state(context, uid, state='archivation_in_progress', message_count=0, restored_message_count=0):
    with user_connection(context, uid) as _:
        context.maildb.execute('''
            INSERT INTO
                mail.archives (uid, state, message_count, restored_message_count)
            VALUES
                ({uid}, '{state}', {message_count}, {restored_message_count})
            ON CONFLICT (uid) DO UPDATE SET
                state = excluded.state,
                message_count = excluded.message_count,
                restored_message_count = excluded.restored_message_count
        '''.format(uid=uid, state=state,
                   message_count=message_count, restored_message_count=restored_message_count))


def get_folder_by_type(context, uid, type):
    with user_connection(context, uid) as conn:
        qs = Queries(conn, uid)
        return qs.folder_by_type(type)
