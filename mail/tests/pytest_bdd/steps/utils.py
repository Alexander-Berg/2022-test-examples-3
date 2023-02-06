# coding: utf-8

from collections import namedtuple

from pymdb.operations import (
    FillChangelogForMsearch,
    RestoreDeleted,
    DeleteFoldersSubtree,
)
from mail.pypg.pypg.query_handler import ExpectOneItemError
from tests_common.pytest_bdd import given, when


@when('we fill changelog for msearch')
def step_fill_changelog(context):
    o = FillChangelogForMsearch(context.conn, context.uid)
    o()
    o.commit()


@when('DBA restore "{mids:Mid}" into "{folder_type:w}" as "{op_id:OpID}"')
def step_restore_messages_save_mid(context, mids, folder_type, op_id):
    folder = context.qs.folder_by_type(folder_type)
    o = RestoreDeleted(context.conn, context.uid)
    context.operations[op_id] = o
    o(context.res.get_mids(mids), folder.fid)
    o.commit()


NonExistentMid = namedtuple('NonExistentMid', ('mid',))


@given('non existent mid "{mid:Mid}"')
def find_unexisted_mid(context, mid):
    not_existent_mid = 100
    try:
        context.qs.message(mid=not_existent_mid)
    except ExpectOneItemError:
        pass
    else:
        raise NotImplementedError(
            'oh.. mid %r realy exists?! '
            'Probably it is time to write some '
            'logic here ...' % not_existent_mid
        )
    # add class, cause ResultMap
    # expect object with mid attribute
    context.res[mid] = NonExistentMid(not_existent_mid)


@when('we delete folders subtree from folder named "{folder_name:w}"')
def step_delete_folder_tree(context, folder_name):
    context.apply_op(
        DeleteFoldersSubtree,
        root_fid=context.qs.folder_by_name(folder_name).fid
    )
