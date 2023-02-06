# coding: utf-8

from pymdb import types as TYP
from pymdb.operations import SyncMessage
from pymdb.queries import Queries
from tests_common.pytest_bdd import when


def cast_as(obj, Type):
    return Type(**obj.as_dict())


def cast_seq_as(obj_seq, Type):
    return [Type(**obj.as_dict()) for obj in obj_seq]


def none_or_cast_seq_as(obj_seq, Type):
    if obj_seq is None:
        return None
    return cast_seq_as(obj_seq, Type)


def sync_types_from_message(message):
    return dict(
        sync_coords=cast_as(message.coords, TYP.SyncCoordinates),
        headers=cast_as(message.headers, TYP.SyncHeaders),
        recipients=cast_seq_as(message.recipients, TYP.SyncRecipient),
        attaches=cast_seq_as(message.attaches or [], TYP.SyncAttach),
        mime=none_or_cast_seq_as(message.mime, TYP.SyncMimePart),
    )


def make_sync_message_op(operation_maker, uid, owner_message, owner_uid, owner_folder, quiet=None):
    owner_coords = TYP.OwnerCoordinates(
        uid=owner_uid,
        fid=owner_folder.fid,
        mid=owner_message.mid,
        tid=owner_message.coords.tid,
        revision=owner_message.coords.revision,
    )
    op = operation_maker(SyncMessage, uid)
    op(
        owner_coords=owner_coords,
        lids=[],
        threads=TYP.SyncThreading(
            references_hashes=[],
            in_reply_to_hash=None,
        ),
        quiet=quiet,
        **sync_types_from_message(owner_message)
    )
    return op


class OwnerInfo(object):
    def __init__(self, context, owner_ref):
        self.uid = context.users[owner_ref.user_name]
        self.qs = Queries(context.conn, self.uid)
        self.folder = self.qs.folder_by(
            folder_name=owner_ref.folder_name,
            folder_type=owner_ref.folder_type,
        )
        self.messages = list(self.qs.mails())

    def get_message_by_mid_key(self, context, mid_key):
        mid = context.res.get_mid(mid_key)

        message = [m for m in self.messages if m.mid == mid]
        assert message, \
            'There are no message with {mid:d}[{mid_key}] in master {folder}'.format(
                mid=mid,
                mid_key=mid_key,
                folder=self.folder)
        return message[0]


@when(u'we sync "{owner_mid_keys:MidsRange}" from "{owner_ref:FolderRef}"'
      ' new message "{subscriber_mid_keys:MidsRange}" appears')
@when(u'we sync "{owner_mid_keys:MidsRange}" from "{owner_ref:FolderRef}"'
      ' new messages "{subscriber_mid_keys:MidsRange}" appear')
def step_sync_mids(context, owner_mid_keys, owner_ref, subscriber_mid_keys):
    sync_mids(**locals())


@when(u'we sync "{owner_mid_keys:MidsRange}" from "{owner_ref:FolderRef}"')
def step_sync_mids_somehow(context, owner_mid_keys, owner_ref):
    sync_mids(**locals())


@when(u'we {quiet} sync "{owner_mid_keys:MidsRange}" from "{owner_ref:FolderRef}"')
def step_quietly_sync_mids_somehow(context, owner_mid_keys, owner_ref, quiet):
    sync_mids(**locals())


def sync_mids(context, owner_mid_keys, owner_ref, subscriber_mid_keys=None, quiet=None):
    if not subscriber_mid_keys:
        subscriber_mid_keys = [mk + '-subscriber' for mk in owner_mid_keys]
    elif not len(owner_mid_keys) == len(subscriber_mid_keys):
        raise RuntimeError('Use keys with same length!')

    owner = OwnerInfo(context, owner_ref)
    for owner_mid_key, subscriber_mid_key in zip(owner_mid_keys, subscriber_mid_keys):
        op = make_sync_message_op(
            context.make_operation,
            context.uid,
            owner_message=owner.get_message_by_mid_key(context, owner_mid_key),
            owner_uid=owner.uid,
            owner_folder=owner.folder,
            quiet=(quiet is not None),
        )
        op.commit()
        context.res[subscriber_mid_key] = op.result[0]


@when(u'we try sync "{owner_mid_key:Mid}" from "{owner_ref:FolderRef}"'
      ' as "{op_id:OpID}"')
def step_try_sync_mid(context, owner_mid_key, owner_ref, op_id):
    owner = OwnerInfo(context, owner_ref)

    context.operations[op_id] = make_sync_message_op(
        context.make_async_operation,
        context.uid,
        owner_message=owner.get_message_by_mid_key(context, owner_mid_key),
        owner_uid=owner.uid,
        owner_folder=owner.folder
    )
