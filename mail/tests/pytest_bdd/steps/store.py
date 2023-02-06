# coding: utf-8

import copy
import hashlib
import itertools
import logging
import random
import time
from collections import namedtuple
from datetime import datetime
from dateutil.parser import parse as parse_date

from pytest_bdd import parsers

from pymdb.helpers import labels_from_str, parse_values, attaches_from_str, mime_from_str, \
    recipients_from_str, mailish_from_str
from pymdb.operations import StoreMessage, QuickSaveMessage, \
    JoinThreads, StoreDeletedMessage
from pymdb.types import QuickSaveCoordinates, QuickSaveHeaders
from pymdb.types import StoreCoordinates, StoreDeletedCoordinates, MailishCoordinates, StoreHeaders, \
    StoreAttach, StoreThreading, StoreRecipient
from pymdb.types.adapted import StoreMimePart
from tests_common.pytest_bdd import when, then
from tools.fnv1 import ora_fnv1

log = logging.getLogger(__name__)

DefaultValue = namedtuple('DefaultValue', ('caster', 'make_default'))

STORE_META = {
    "coords": {
        "tid": DefaultValue(int, lambda: None),
        "received_date": DefaultValue(
            lambda v: parse_date(v),
            datetime.now),
        "size": 70650,
        "flags": [],
        "st_id": "21697.62776296.328602695958663276848385379266",
        "pop_uidl": "",
        "attributes": [],
        "tab": None
    },
    "headers": {
        "subject": "",
        "firstline": "",
        "hdr_date": datetime.now(),
        "hdr_message_id": "",
        "extra_data": ""
    },
    "recipients": {},
    "threads_meta": {
        "rule": "hash",
        "references_hashes": [],
        "in_reply_to_hash": None,
        "hash_value": 0,
        "hash_namespace": "subject",
        "hash_uniq_key": 0,
        "sort_options": ""
    },
    "attaches": {},
    "mime": None,
    "mailish": {
        "imap_id": None,
        "imap_time": None
    },
}

STORE_DELETED_META = {
    "coords": {
        "received_date": DefaultValue(
            lambda v: parse_date(v),
            datetime.now),
        "size": 70650,
        "st_id": "21697.62776296.328602695958663276848385379266",
        "attributes": []
    },
    "headers": {
        "subject": "",
        "firstline": "",
        "hdr_date": datetime.now(),
        "hdr_message_id": "",
        "extra_data": ""
    },
    "recipients": {},
    "attaches": {},
    "mime": None,
}


def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()


def references_to_hashes(row):
    refs = (rv.strip() for rv in row.split(","))
    return [ora_fnv1(rv) for rv in refs]


def in_reply_to_hash(row):
    row = row.strip()
    if not row:
        return None
    return ora_fnv1(row)


def attaches_or_null(row):
    row = row.strip()
    if row == 'NULL':
        return None
    return attaches_from_str(row)


def mime_or_null(row):
    row = row.strip()
    if row == 'NULL':
        return None
    return mime_from_str(row)


def recipients_or_null(row):
    row = row.strip()
    if row == 'NULL':
        return None
    return recipients_from_str(row)


def mailish_or_null(row):
    row = row.strip()
    if row == 'NULL':
        return None
    return mailish_from_str(row)


FieldDecorder = namedtuple('FieldDecoder', ('path', 'apply'))

CUSTOM_DECODERS = {
    'labels': FieldDecorder(
        ['labels'],
        labels_from_str,
    ),
    'references': FieldDecorder(
        ['threads_meta', 'references_hashes'],
        references_to_hashes
    ),
    'in-reply-to': FieldDecorder(
        ['threads_meta', 'in_reply_to_hash'],
        in_reply_to_hash
    ),
    'attaches': FieldDecorder(
        ['attaches'],
        attaches_or_null
    ),
    'mime': FieldDecorder(
        ['mime'],
        mime_or_null
    ),
    'recipients': FieldDecorder(
        ['recipients'],
        recipients_or_null
    ),
    'mailish': FieldDecorder(
        ['mailish'],
        mailish_or_null
    )
}


# add aliases
def _add_aliases(d, **kwargs):
    for alias, decoder in kwargs.items():
        d[alias] = d[decoder]


_add_aliases(
    CUSTOM_DECODERS,
    label='labels',
    in_reply_to='in-reply-to'
)


def apply_custom_decoder(meta, decoder, field):
    md = meta
    for cur_path in decoder.path[:-1]:
        md = md[cur_path]
    md[decoder.path[-1]] = decoder.apply(field)


def default_decoder(default_field, field):
    casted = field
    if isinstance(default_field, DefaultValue):
        casted = default_field.caster(field)
    elif isinstance(default_field, int):
        casted = int(field)
    elif isinstance(default_field, list):
        casted = (v.strip() for v in field.split(','))
        casted = [v for v in casted if v]
    return casted


def decode_row_into_meta(row, meta, skip_cols=None):
    log.info('decode_row_into_meta row=[%r] meta=[%r]', row, meta)
    key_location = dict()
    mid = None
    for dn in meta:
        if isinstance(meta[dn], dict):
            key_location.update(
                dict(zip(meta[dn].keys(), itertools.repeat(meta[dn])))
            )
    if row is not None:
        for k in row.headings:
            if skip_cols and k in skip_cols:
                continue
            if k == 'mid':
                mid = row[k]
            elif k in CUSTOM_DECODERS:
                apply_custom_decoder(meta, CUSTOM_DECODERS[k], row[k])
            else:
                assert k in key_location, \
                    'Can\'t find %s handle, probably unsupported key' \
                    ' or different (legacy?) signature,' \
                    ' known keys are: %r and %r' % (
                        k, key_location.keys(), CUSTOM_DECODERS.keys())
                data_dict = key_location[k]
                data_dict[k] = default_decoder(data_dict[k], row[k])
    log.info('result meta=[%r]', meta)
    return meta, mid


def decode_row(row, skip_cols=None, SM=None):
    'convert row:context.table to STORE_META structure'
    SM = SM or STORE_META
    meta = copy.deepcopy(SM)
    return decode_row_into_meta(row, meta, skip_cols)


def take_n_lines(text, n):
    '''get only fist 2 lines'''
    return u'  '.join(text.split(u'\n')[:n])


def fill_meta(context, meta, fld=None, tab=None):
    coords = meta['coords']
    headers = meta['headers']

    speech = context.hamlet.next_speech()

    if fld:
        coords['fid'] = fld.fid
    if tab:
        coords['tab'] = tab

    for ck, cv in coords.items():
        if isinstance(cv, DefaultValue):
            coords[ck] = cv.make_default()

    if not headers['subject']:
        headers['subject'] = take_n_lines(speech.speech, 1)
    if not headers['firstline']:
        headers['firstline'] = speech.speech

    if not meta['recipients']:
        recipients = [
            dict(
                type='to',
                name="Reader",
                email="Reader@Shakespeare"
            )
        ]
        for previsous_actor in set(
                s.actor
                for s in context.hamlet.previous_speeches()
                if s.actor != speech.actor):
            recipients.append(
                dict(
                    type='to',
                    name=previsous_actor,
                    email=previsous_actor.lower() + u'@Shakespeare'
                )
            )
        for header in ['from', 'reply-to']:
            recipients.append(
                dict(
                    type=header,
                    name=speech.actor,
                    email=speech.actor.lower() + u'@Shakespeare'
                )
            )
        meta['recipients'] = recipients

    if not headers['hdr_message_id']:
        headers['hdr_message_id'] = '<%d.%d@pg>' % (
            int(time.time() * 100), random.randint(1, 100500))

    if 'pop_uidl' in coords:
        if not coords['pop_uidl']:
            coords['pop_uidl'] = md5(headers['firstline'])

    if 'threads_meta' in meta:
        meta['threads_meta']['hash_value'] = ora_fnv1(headers['subject'])

    return meta


StoreTypesMap = namedtuple(
    'StoreTypesMap',
    ('coords', 'headers',
     'recipients', 'attaches', 'mime',
     'threads_meta', 'mailish_coords')
)

NEW_TYPES = StoreTypesMap(
    coords=StoreCoordinates,
    headers=StoreHeaders,
    recipients=StoreRecipient,
    attaches=StoreAttach,
    mime=StoreMimePart,
    threads_meta=StoreThreading,
    mailish_coords=MailishCoordinates,
)

QUICK_SAVE_TYPES = StoreTypesMap(
    coords=QuickSaveCoordinates,
    headers=QuickSaveHeaders,
    recipients=StoreRecipient,
    attaches=StoreAttach,
    mime=StoreMimePart,
    threads_meta=None,
    mailish_coords=None,
)

StoreDeletedTypesMap = namedtuple(
    'StoreDeletedTypesMap',
    ('coords', 'headers',
     'recipients', 'attaches', 'mime')
)

NEW_DELETED_TYPES = StoreDeletedTypesMap(
    coords=StoreDeletedCoordinates,
    headers=StoreHeaders,
    recipients=StoreRecipient,
    attaches=StoreAttach,
    mime=StoreMimePart,
)


def make_attaches(meta, T):
    attaches = None
    if meta['attaches'] is not None:
        attaches = [T.attaches(**a) for a in meta['attaches']]
    return attaches


def make_mime(meta, T):
    mime = None
    if meta['mime'] is not None:
        mime = [T.mime(**a) for a in meta['mime']]
    return mime


def make_recipients(meta, T):
    recipients = None
    if meta['recipients'] is not None:
        recipients = [T.recipients(**a) for a in meta['recipients']]
    return recipients


def make_coordinates(meta, T):
    mc = meta['coords']
    coords = dict((k, mc[k]) for k in T.coords.fields() if k in mc)
    COORDS_FLAGS = list(f for f in ['seen', 'recent', 'deleted'] if f in T.coords.fields())
    coords.update(dict.fromkeys(COORDS_FLAGS, False))
    for f in mc['flags']:
        if f in COORDS_FLAGS:
            coords[f] = True
        else:
            raise AssertionError("unknown flag: {0}".format(f))
    return T.coords(**coords)


def make_store_deleted_coordinates(meta, T):
    mc = meta['coords']
    coords = dict((k, mc[k]) for k in T.coords.fields() if k in mc)
    return T.coords(**coords)


def make_mailish_coords(meta, T):
    mc = meta['mailish']
    return T.mailish_coords(**mc)


def make_labels(context, meta):
    labels = set(copy.deepcopy(meta.get('labels', [])))
    return [l.lid for l in context.qs.find_labels(labels)]


def make_headers(meta, T):
    m = meta['headers']
    headers = dict((k, m[k]) for k in T.headers.fields() if k in m)
    return T.headers(**headers)


def make_threads_meta(meta, T):
    return T.threads_meta(**meta['threads_meta'])


def make_meta_from_db_row(context, row):
    meta = copy.deepcopy(STORE_META)
    coords = meta['coords']
    coords['ora_mid'] = 42
    coords['fid'] = ''
    coords['received_date'] = None

    for c in coords.keys():
        if c not in ['flags', 'ora_mid']:
            coords[c] = row[c]

    headers = meta['headers']
    for h in headers.keys():
        if h not in ['in_reply_to']:
            headers[h] = row[h]

    meta['mid'] = row['mid']
    meta['recipients'] = row['recipients']
    meta['attaches'] = row['attaches']
    meta['mime'] = row['mime']
    all_labels = context.qs.labels()
    meta['labels'] = [l for l in all_labels if l.lid in row['lids']]

    return meta


def store_mail(context, operation_maker, mid, uid, meta, T=None, quiet=None):
    def save_mid(result):
        log.info("save mid conv {0} -> {1}".format(mid, result))
        context.res[mid] = result[0]

    T = T or NEW_TYPES

    return operation_maker(StoreMessage)(
        coords=make_coordinates(meta, T),
        headers=make_headers(meta, T),
        recipients=make_recipients(meta, T),
        attaches=make_attaches(meta, T),
        mime=make_mime(meta, T),
        lids=make_labels(context, meta),
        threads_meta=make_threads_meta(meta, T),
        mailish_coords=make_mailish_coords(meta, T),
        quiet=quiet,
    ).add_result_callback(save_mid)


def quick_save_mail(context, operation_maker, mid, uid, meta, T=None):
    return operation_maker(QuickSaveMessage)(
        mid=mid,
        coords=make_coordinates(meta, QUICK_SAVE_TYPES),
        headers=make_headers(meta, QUICK_SAVE_TYPES),
        recipients=make_recipients(meta, QUICK_SAVE_TYPES),
        attaches=make_attaches(meta, QUICK_SAVE_TYPES),
        mime=make_mime(meta, QUICK_SAVE_TYPES)
    )


def get_row_expect_one(table):
    if table is None:
        return None
    row_count = len(table.rows)
    assert row_count < 2, \
        "this step expect only zero or more mails, got: %d" % row_count

    return table.rows[0]


def store_into_folder(context, operation_maker, mid, folder_type, folder_name, quiet=None):
    fld = context.qs.folder_by(folder_type, folder_name)
    meta, _ = decode_row(get_row_expect_one(context.table))
    fill_meta(context, meta, fld)
    return store_mail(
        context=context,
        operation_maker=operation_maker,
        mid=mid,
        uid=context.uid,
        meta=meta,
        quiet=quiet,
    )


def store_deleted(context, operation_maker, mid, meta, T=None, quiet=None):
    def save_mid(result):
        log.info("save mid conv {0} -> {1}".format(mid, result))
        context.res[mid] = result[0]

    T = T or NEW_DELETED_TYPES

    return operation_maker(StoreDeletedMessage)(
        coords=make_store_deleted_coordinates(meta, T),
        headers=make_headers(meta, T),
        recipients=make_recipients(meta, T),
        mime=make_mime(meta, T),
        attaches=make_attaches(meta, T),
    ).add_result_callback(save_mid)


STORE_ONE_RE = r'''
we (?P<quiet>quietly )?store "(?P<mids>[^"]+)" into (?:
"(?P<folder_type>\w+)"|
folder named "(?P<folder_name>\w+(?:\|\w+)*)"
)
'''.strip().replace('\n', '')


@when(STORE_ONE_RE, parse_builder=parsers.re)
def step_store_inline(context, mids):
    quiet = context.args.get('quiet') is not None
    folder_type = context.args.get('folder_type')
    folder_name = context.args.get('folder_name')
    if context.table:
        assert "mid" not in context.table.headings, \
            "duplicate mid specification!"
    for mid in parse_values(mids):
        store_into_folder(context, context.make_operation, mid, folder_type,
                          folder_name, quiet).commit()


STORE_MANY_RE = r'''
we store into (?:
"(?P<folder_type>\w+)"|
folder named "(?P<folder_name>\w+(?:\|\w+)*)"
)
'''.strip().replace('\n', '')


@when(STORE_MANY_RE, parse_builder=parsers.re)
def step_store_many(context):
    folder_type = context.args.get('folder_type')
    folder_name = context.args.get('folder_name')
    fld = context.qs.folder_by(folder_type, folder_name)
    for row in context.table:
        meta, mid = decode_row(row)
        assert mid, 'need mid in row %r, meta: %r' % (row, meta)
        meta = fill_meta(context, meta, fld=fld)
        store_mail(context, context.make_operation, mid, context.uid, meta).commit()


def store_into_tab(context, operation_maker, mid, tab_type):
    fld = context.qs.folder_by_type('inbox')
    meta, _ = decode_row(get_row_expect_one(context.table))
    fill_meta(context, meta, fld, tab_type)
    return store_mail(
        context=context,
        operation_maker=operation_maker,
        mid=mid,
        uid=context.uid,
        meta=meta,
    )


@when('we store "{mids}" into tab "{tab_type:w}"')
def step_store_to_tab(context, mids, tab_type):
    if context.table:
        assert "mid" not in context.table.headings, \
            "duplicate mid specification!"
    for mid in parse_values(mids):
        store_into_tab(context, context.make_operation, mid, tab_type).commit()


@when('we store into tab "{tab_type:w}"')
def step_store_many_to_tab(context, tab_type):
    fld = context.qs.folder_by_type('inbox')
    for row in context.table:
        meta, mid = decode_row(row)
        assert mid, 'need mid in row %r, meta: %r' % (row, meta)
        meta = fill_meta(context, meta, fld=fld, tab=tab_type)
        store_mail(context, context.make_operation, mid, context.uid, meta).commit()


def get_meta_for_quick_save(context, mid):
    mid = context.res.get_mid(mid)
    row = context.qs.message(mid=mid)
    meta = make_meta_from_db_row(context, row)
    row = get_row_expect_one(context.table)
    meta, dummy = decode_row_into_meta(row, meta)
    return mid, meta


@when('we quick save "{mid}" with')
def step_quick_save(context, mid):
    mid, meta = get_meta_for_quick_save(context, mid)
    quick_save_mail(context, context.make_operation, mid, context.uid, meta, QUICK_SAVE_TYPES).commit()


@when('we try quick save "{mid}" as "{op_id}"')
def step_quick_save_no_wait(context, mid, op_id):
    mid, meta = get_meta_for_quick_save(context, mid)
    context.operations[op_id] = quick_save_mail(
        context, context.make_async_operation, mid, context.uid, meta, QUICK_SAVE_TYPES)


@when('we store messages')
def step_store_many_in_diff_folders(context):
    for row in context.table:
        assert "folder" in row.headings, "need folder in row %r" % row
        fld = context.qs.folder_by_type(row['folder'])
        meta, mid = decode_row(row, skip_cols=['folder'])
        assert mid, 'need mid in row %r, meta: %r' % (row, meta)
        meta = fill_meta(
            context,
            meta,
            fld=fld
        )
        store_mail(context, context.make_operation, mid, context.uid, meta).commit()


STORE_NO_WAIT_RE = r'''
we try store "(?P<mid>\$?\w+)" into (?:
"(?P<folder_type>\w+)"|
folder named "(?P<folder_name>\w+(?:\|\w+)*)"
)
 as "(?P<op_id>\$?\w+)"
'''.strip().replace('\n', '')


@when(STORE_NO_WAIT_RE, parse_builder=parsers.re)
def step_store_no_wait(context, mid, op_id):
    folder_type = context.args.get('folder_type')
    folder_name = context.args.get('folder_name')
    context.operations[op_id] = store_into_folder(
        context,
        context.make_async_operation,
        mid,
        folder_type,
        folder_name)


@when('we join "{join_tids}" into "{tid:d}"')
def step_join_threads(context, join_tids, tid):
    join_tids = [int(t.strip()) for t in join_tids.split(',')]
    context.apply_op(
        JoinThreads,
        tid=tid,
        join_tids=join_tids
    )


@when('user "{user}" joins "{join_tids}" into "{tid:d}"')
def step_user_join_threads(context, user, join_tids, tid):
    uid = context.users[user]
    join_tids = [int(t.strip()) for t in join_tids.split(',')]
    JoinThreads(context.conn, uid)(tid, join_tids).commit()


@when('we try join "{join_tids}" into "{tid:d}" as "{op_id}"')
def step_try_join_threads(context, join_tids, tid, op_id):
    join_tids = [int(t.strip()) for t in join_tids.split(',')]
    context.operations[op_id] = context.make_async_operation(
        JoinThreads
    )(
        tid=tid,
        join_tids=join_tids
    )


@then('we found "{mid}" mailish data')
def step_mailish_store(context, mid):
    test_data = get_row_expect_one(context.table)
    db_data = context.qs.mailish_data(uid=context.uid, mid=context.res.get_mid(mid))[0]

    for k, v in test_data.items():
        if str(db_data[k]) != str(v):
            raise AssertionError("wrong value for key {0}: {1}, expected {2}".format(k, db_data[k], v))


@when('we store deleted "{mid}"')
def step_store_deleted(context, mid):
    meta = fill_meta(context, copy.deepcopy(STORE_DELETED_META), context.qs.folder_by_type('inbox'))
    store_deleted(context, context.make_operation, mid, meta).commit()


@when('we store deleted with')
def step_store_deleted_with(context):
    for row in context.table:
        meta, mid = decode_row(row, SM=STORE_DELETED_META)
        meta = fill_meta(context, meta)
        store_deleted(context, context.make_operation, mid, meta).commit()


@when(r'we try store deleted as "(?P<op_id>\$?\w+)"', parse_builder=parsers.re)
def step_store_deleted_no_wait(context, op_id):
    meta, mid = decode_row(get_row_expect_one(context.table), SM=STORE_DELETED_META)
    meta = fill_meta(context, meta)
    context.operations[op_id] = store_deleted(context, context.make_async_operation, mid, meta)
