from pymdb.operations import SyncUpdateMessages
from .sync_message import OwnerInfo
from tests_common.pytest_bdd import when
from .update import parse_flags


@when('we sync set "{flags}" on "{owner_mid_keys:MidsRange}" '
      'belonging to "{owner_ref:FolderRef}"')
def step_sync_set_flag(context, flags, owner_mid_keys, owner_ref):
    owner = OwnerInfo(context, owner_ref)
    owner_mids = [context.res.get_mid(mid_key) for mid_key in owner_mid_keys]
    op = SyncUpdateMessages(context.conn, context.uid)(
        owner.uid,
        owner.folder.fid,
        owner_mids,
        owner.folder.revision,
        **parse_flags(context, flags)
    )
    op.commit()


@when('we try sync set "{flags}" on "{owner_mid_keys:MidsRange}" '
      'belonging to "{owner_ref:FolderRef}" as "{op_id:OpID}"')
def step_try_sync_set_flag(context, flags, owner_mid_keys, owner_ref, op_id):
    owner = OwnerInfo(context, owner_ref)
    owner_mids = [context.res.get_mid(mid_key) for mid_key in owner_mid_keys]
    op = SyncUpdateMessages(context.conn, context.uid)(
        owner.uid,
        owner.folder.fid,
        owner_mids,
        owner.folder.revision,
        **parse_flags(context, flags)
    )
    context.operations[op_id] = op
