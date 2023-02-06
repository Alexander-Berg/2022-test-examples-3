# coding: utf-8

from datetime import datetime, timedelta

from dateutil.tz.tz import tzlocal

from tests_common.pytest_bdd import then  # pylint: disable=E0611


@then('message "{mid}" has "{count:d}" references')
def message_has_references(context, mid, count):
    refs = context.qs.references(mid=context.res.get_mid(mid))
    assert len(refs) == count, \
        'Expect {0} references, but found {1}'.format(
            count, len(refs))


@then('message "{mid}" in "{folder_type:w}" with "{imap_id:d}" imap_id')
def step_message_has_imap_id(context, mid, folder_type, imap_id):
    folder = context.qs.folder_by_type(folder_type)
    message = context.qs.message(
        mid=context.res.get_mid(mid)
    )
    assert message['fid'] == folder.fid, \
        'Expect %d fid(folder:%r) got %d' % (
            folder.fid, folder, message['fid'])
    assert message['imap_id'] == imap_id, \
        'Expect %d imap_id got %r, full message %r' % (
            imap_id, message['imap_id'], message)


@then('message "{mid}" has threading info with tid "{tid:d}"')
def message_has_tid(context, mid, tid):
    ti = context.qs.message(mid=context.res.get_mid(mid))
    found_tid = ti['found_tid']
    assert found_tid == tid, \
        "ThreadInfo contains unexpected tid: %r" % found_tid


def get_doom_date(context, mid):
    return context.qs.message(mid=mid)['doom_date']


def now_with_timezone():
    '''
    return datetime.now() with filled tzinfo
    '''
    return datetime.now(tzlocal())


@then(u'message "{mid:Mid}" has recent doom_date')
def step_has_doom_date(context, mid):
    mid = context.res.get_mid(mid)
    doom_date = get_doom_date(context, mid)

    # Don't want to see ValueError
    # if doom_date is NULL
    assert doom_date is not None, \
        'Got null doom_date: %r' % doom_date

    now = now_with_timezone()
    hour = timedelta(hours=1)
    assert now - hour < doom_date < now


@then(u'message "{mid:Mid}" has null doom_date')
def step_has_null_doom_date(context, mid):
    mid = context.res.get_mid(mid)
    doom_date = get_doom_date(context, mid)
    assert doom_date is None
