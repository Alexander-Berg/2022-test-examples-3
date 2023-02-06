from pymdb.operations import SyncDeleteMessages
from .sync_message import OwnerInfo
from tests_common.pytest_bdd import when


@when('we delete synced messages "{owner_mid_keys:MidsRange}" '
      'belonging to "{owner_ref:FolderRef}"')
def step_delete_synced(context, owner_mid_keys, owner_ref):
    owner = OwnerInfo(context, owner_ref)
    owner_mids = [context.res.get_mid(mid_key) for mid_key in owner_mid_keys]
    op = SyncDeleteMessages(context.conn, context.uid)(
        owner_uid=owner.uid,
        owner_fid=owner.folder.fid,
        owner_mids=owner_mids,
        owner_revision=owner.folder.revision
    )
    op.commit()


@when('we try delete synced messages "{owner_mid_keys:MidsRange}" '
      'belonging to "{owner_ref:FolderRef}" as "{op_id:OpID}"')
def step_try_delete_synced(context, owner_mid_keys, owner_ref, op_id):
    owner = OwnerInfo(context, owner_ref)
    owner_mids = [context.res.get_mid(mid_key) for mid_key in owner_mid_keys]
    op = SyncDeleteMessages(context.conn, context.uid)(
        owner_uid=owner.uid,
        owner_fid=owner.folder.fid,
        owner_mids=owner_mids,
        owner_revision=owner.folder.revision
    )
    context.operations[op_id] = op
