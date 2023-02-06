# coding: utf-8

import pymdb.operations as OPS
from datetime import datetime

from tests_common.pytest_bdd import when, then, given  # pylint: disable=E0611


@when('we remove incorrect stickers')
def step_remove_incorrect_stickers(context):
    OPS.RemoveIncorrectStickers(context.conn, context.uid)()


@when('we put a sticker on "{mids:MidsRange}"')
def step_put_sticker(context, mids):
    mids = context.res.get_mids(mids)
    fid = context.qs.folder_by_type('inbox').fid
    for mid in mids:
        OPS.CreateReplyLaterSticker(context.conn, context.uid)(mid=mid, fid=fid, date=datetime.now(), tab=None)


@when('we put a sticker on "{mids:MidsRange}" and catch an error')
def step_put_sticker_and_catch_error(context, mids):
    mids = context.res.get_mids(mids)
    fid = context.qs.folder_by_type('inbox').fid
    for mid in mids:
        try:
            OPS.CreateReplyLaterSticker(context.conn, context.uid)(mid=mid, fid=fid, date=datetime.now(), tab=None)
            assert False
        except:
            pass


def check_sticker(context, mids):
    mids = context.res.get_mids(mids)
    for mid in mids:
        assert context.qs.reply_later_sticker_on_mid(mid=mid)


@then('there are no stickers on "{mids}"')
def step_check_no_stickers(context, mids):
    mids = context.res.get_mids(mids)
    for mid in mids:
        assert not context.qs.reply_later_sticker_on_mid(mid=mid)


@then('there is a sticker on "{mids:MidsRange}"')
def step_then_check_sticker(context, mids):
    check_sticker(context, mids)


@when('there is a sticker on "{mids:MidsRange}"')
def step_when_check_sticker(context, mids):
    check_sticker(context, mids)


@then('we remove sticker from "{mids:MidsRange}"')
def step_remove_stickers(context, mids):
    mids = context.res.get_mids(mids)
    for mid in mids:
        OPS.RemoveReplyLaterSticker(context.conn, context.uid)(mid=mid)


@given('new initialized user with a stickered message in "{folder_type:w}"')
def step_given_with_stickered_message_in_folder(context, folder_type):
    context.execute_steps(u'''
        Given new initialized user
        When we create "reply_later" folder "reply_later"
        And we store "$1" into "{folder_type}"
        And we put a sticker on "$1"
        And we create "system" label "reply_later_finished"
        And we create "system" label "reply_later_started"
    '''.format(
        folder_type=folder_type
    ))
