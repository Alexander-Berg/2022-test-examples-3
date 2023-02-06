# coding: utf-8

import logging

import pytest

from pymdb.helpers import parse_values
from pymdb.operations import CopyMessages
from pymdb.types import CopyResult
from tests_common.pytest_bdd import when  # pylint: disable=E0611

log = logging.getLogger(__name__)


def make_op(context, operation_maker, mids, folder_type, dest_tab, copy_mids):
    def save_mids(result):
        log.info('result is: %r', result)
        revision = result[0].revision
        if copy_mids:
            for mid, copy_mid in zip(
                    sorted(result[0].mids),
                    parse_values(copy_mids)):
                context.res[copy_mid] = CopyResult(revision=revision, mid=mid)

    folder = context.qs.folder_by_type(folder_type)
    mids = context.res.get_mids(mids)
    op = operation_maker(CopyMessages)(mids, folder.fid, dest_tab)
    op.add_result_callback(save_mids)
    return op


@pytest.fixture(scope='function')
def folder_type():
    return 'inbox'


@pytest.fixture(scope='function')
def dest_tab():
    return None


@when('we copy "{mids}" to "{folder_type:w}" new messages "{copy_mids}" appears')
@when('we copy "{mids}" to "{folder_type:w}" new message "{copy_mids}" appears')
@when('we copy "{mids}" to tab "{dest_tab:w}" as "{copy_mids}"')
def step_copy_with_result(context, mids, folder_type, dest_tab, copy_mids):
    copy_with_result(context, mids, folder_type, dest_tab, copy_mids)


@when('we copy "{mids}" to "{folder_type:w}"')
@when('we copy "{mids}" to tab "{dest_tab:w}"')
def step_copy_single_with_result(context, mids, folder_type, dest_tab):
    copy_with_result(context, mids, folder_type, dest_tab)


def copy_with_result(context, mids, folder_type, dest_tab, copy_mids=''):
    make_op(context, context.make_operation, mids, folder_type, dest_tab, copy_mids).commit()


@when('we try copy "{mids}" to "{folder_type:w}" as "{op_id}" new message '
      '"{copy_mids}" can appears')
@when('we try copy "{mids}" to "{folder_type:w}" as "{op_id}" new messages '
      '"{copy_mids}" can appear')
@when('we try copy "{mids}" to tab "{dest_tab:w}" as "{op_id}" new message '
      '"{copy_mids}" can appears')
@when('we try copy "{mids}" to tab "{dest_tab:w}" as "{op_id}" new messages '
      '"{copy_mids}" can appear')
def step_try_copy(context, mids, folder_type, dest_tab, op_id, copy_mids):
    context.operations[op_id] = make_op(
        context, context.make_async_operation, mids, folder_type, dest_tab, copy_mids)
