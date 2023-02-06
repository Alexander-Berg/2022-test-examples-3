# coding: utf-8

import logging

from pytest import fixture

from pymdb.operations import MoveMessages
from tests_common.pytest_bdd import when  # pylint: disable=E0611
from pymdb.queries import FolderDoesNotExist

log = logging.getLogger(__name__)


def log_operation_result(result):
    log.info('result is: {0}'.format(result))


def make_move_op(context, operation_maker, mids, folder_type):
    try:
        folder = context.qs.folder_by_type(folder_type)
    except FolderDoesNotExist:
        folder = context.qs.folder_by_name(folder_type)
    mids = context.res.get_mids(mids)
    new_tab = 'relevant' if folder.type == 'inbox' else None
    op = operation_maker(MoveMessages)(mids, folder.fid, new_tab)
    op.add_result_callback(log_operation_result)
    return op


@when('we move "{mids}" to "{folder_type:w}"')
def step_move(context, mids, folder_type):
    make_move_op(context, context.make_operation, mids, folder_type).commit()


@when('we try move "{mids}" to "{folder_type:w}" as "{op_id}"')
def step_set_no_wait(context, mids, folder_type, op_id):
    context.operations[op_id] = make_move_op(
        context, context.make_async_operation, mids, folder_type
    )


def make_move_to_tab_op(context, operation_maker, mids, dest_tab):
    mids = context.res.get_mids(mids)
    inbox = context.qs.folder_by_type('inbox')
    op = operation_maker(MoveMessages)(mids, inbox.fid, dest_tab)
    op.add_result_callback(log_operation_result)
    return op


@fixture(scope='function')
def dest_tab():
    return None


@when('we move "{mids}" to tab "{dest_tab:w}"')
@when('we move "{mids}" to null tab')
def step_move_to_tab(context, mids, dest_tab):
    if dest_tab == '':
        dest_tab = None
    make_move_to_tab_op(context, context.make_operation, mids, dest_tab).commit()


@when('we try move "{mids}" to tab "{dest_tab:w}" as "{op_id}"')
@when('we try move "{mids}" to null tab as "{op_id}"')
def step_move_to_tab_no_wait(context, mids, dest_tab, op_id):
    if dest_tab == '':
        dest_tab = None
    context.operations[op_id] = make_move_to_tab_op(
        context, context.make_async_operation, mids, dest_tab
    )
