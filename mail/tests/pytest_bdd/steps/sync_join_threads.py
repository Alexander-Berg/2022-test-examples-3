from pymdb.operations import SyncJoinThreads
from .sync_message import OwnerInfo
from tests_common.pytest_bdd import when


@when('we sync joined "{join_tids}" into "{tid:d}" '
      'belonging to "{owner_ref:FolderRef}"')
def step_join_threads_synced(context, join_tids, tid, owner_ref):
    owner = OwnerInfo(context, owner_ref)
    join_tids = [int(t.strip()) for t in join_tids.split(',')]
    op = SyncJoinThreads(context.conn, context.uid)(
        owner_uid=owner.uid,
        owner_fid=owner.folder.fid,
        owner_tid=tid,
        owner_join_tids=join_tids,
        owner_revision=owner.folder.revision
    )
    op.commit()


def get_thread_by_mid(context, mid):
    message = context.qs.message(mid=context.res.get_mid(mid))
    return message['tid']


@when(u'we store "{new_mid}" into "{folder_type}" with "{old_mid:Mid}"s thread')
def step_store_with_thread_from_mid(context, new_mid, folder_type, old_mid):
    tid = get_thread_by_mid(context, old_mid)
    context.execute_steps(u'''
        When we store into "{folder}"
         | mid   | tid   |
         | {mid} | {tid} |
    '''.format(folder=folder_type, mid=new_mid, tid=tid))


@when(u'we join "{from_mid:Mid}"s thread into "{to_mid:Mid}"s thread')
def step_join_threads_from_mids(context, from_mid, to_mid):
    from_tid = get_thread_by_mid(context, from_mid)
    to_tid = get_thread_by_mid(context, to_mid)
    context.execute_steps(u'''
        When we join "{from_tid}" into "{to_tid}"
    '''.format(from_tid=from_tid, to_tid=to_tid))
