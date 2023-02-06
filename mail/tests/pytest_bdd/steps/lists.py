# coding: utf-8

import json
from collections import namedtuple
from datetime import datetime

import six

from pymdb.helpers import (
    split_list, labels_from_str,
    thread_labels_from_str, attaches_from_str, mime_from_str,
    recipients_from_str, cast_bool)
from pymdb.types import ThreadLabel
from mail.pypg.pypg.common import fetch_as_dicts
from tests_common.pytest_bdd import then
from tools import ok_

FLAGS = frozenset(('seen', 'recent', 'deleted'))


def parse_flags(value):
    true_flags = frozenset(flag for flag in split_list(value))
    for flag in true_flags - FLAGS:
        raise NotImplementedError("flag: {0}".format(flag))
    false_flags = FLAGS - true_flags
    return true_flags, false_flags


def is_empty_composite(value):
    if value[0] != '(' or value[-1] != ')':
        return False
    values = value.strip('()').split(',')
    return all(x == '' for x in values)


def to_json(obj):
    return json.dumps(obj, sort_keys=True)


class AttributeCaster(object):
    # pylint: disable=R0201

    def __init__(self, context):
        self.context = context
        self._all_labels = None

    def __call__(self, name, obj, expected_value):
        custom_handler_name = '_custom_cast_%s' % name
        if hasattr(self, custom_handler_name):
            handler = getattr(self, custom_handler_name)
            return handler(obj=obj, expected_value=expected_value)

        cast_handler_name = '_cast_%s' % name
        cast_handler = getattr(self, cast_handler_name, self._default_cast)
        real_value = obj[name]

        return cast_handler(real_value, expected_value)

    def _cast_changed(self, real, expected):
        return real, json.loads(expected)

    def _cast_attaches(self, real, expected):
        return to_json(real), to_json(attaches_from_str(expected))

    def _cast_mime(self, real, expected):
        return to_json(real), to_json(mime_from_str(expected))

    def _cast_recipients(self, real, expected):
        return to_json(real), to_json(recipients_from_str(expected))

    def _cast_attributes(self, real, expected):
        return set(real), set(split_list(expected))

    DATE_FORMAT = '%Y-%m-%d %H:%M:%S %Z'

    def _cast_received_date(self, real, expected):
        return (
            real.utctimetuple(),
            datetime.strptime(expected, self.DATE_FORMAT).utctimetuple()
        )

    _cast_hdr_date = _cast_received_date
    _cast_deleted_date = _cast_received_date

    def _cast_request_info(self, real, expected):
        return (
            '' if is_empty_composite(real) else real,
            expected
        )

    def _cast_mid(self, real, expected):
        if expected in ('', 'NULL'):
            expected = None
        else:
            expected = self.context.res.get_mid(expected)
        return real, expected

    _cast_newest_mid = _cast_mid

    _cast_tid = _cast_mid

    _cast_found_tid = _cast_mid

    def _default_cast(self, real, expected):
        if real is None:
            if expected.strip() in ('', 'NULL'):
                expected = None
        elif isinstance(real, bool):
            expected = cast_bool(expected)
        elif not isinstance(real, str):
            expected = type(real)(expected)
        return real, expected

    @property
    def all_labels(self):
        if self._all_labels is None:
            self._all_labels = self.context.qs.labels()
        return self._all_labels

    def _custom_cast_labels(self, obj, expected_value):
        expected = set()
        # on lids we don't have NOT NULL
        real = set(obj['lids'] or [])

        expected_labels = labels_from_str(expected_value)
        if expected_labels:
            expected = set(
                [l.lid for l in self.all_labels if l in expected_labels]
            )
            assert len(expected) == len(expected_labels), \
                "Can't find lids for labels: {expected_labels}," \
                " found lids: {expected} all labels: {all_labels}".format(
                    expected_labels=expected_labels,
                    expected=expected,
                    all_labels=self.all_labels)
        return real, expected

    def _custom_cast_thread_labels(self, obj, expected_value):
        expected_labels = thread_labels_from_str(expected_value)
        expected = []
        for (el, message_count) in expected_labels.items():
            lid = [l.lid for l in self.all_labels if l == el]
            assert lid, \
                "Can't find lid for label: {el} in {all_labels}".format(
                    el=el,
                    all_labels=self.all_labels)
            expected.append(ThreadLabel(
                lid=lid[0],
                message_count=message_count
            ))
        real = set(obj['thread_labels'] or [])
        expected = set(expected)
        return real, expected


class TableCompartor(object):
    default_aliases = {
        'flag': 'flags',
        'label': 'labels',
        'thread_label': 'thread_labels'
    }

    def __init__(self, context, pk, obj_name, aliases=None, caster=None):
        self.context = context
        self._all_labels = None
        self.pk = pk
        self.obj_name = obj_name
        self.aliases = dict(aliases) if aliases else {}
        self.aliases.update(self.default_aliases)
        self.caster = AttributeCaster(context) if caster is None else caster

    def _check_flags(self, row_h, row, obj):
        true_flags, false_flags = parse_flags(row[row_h])
        for flag in true_flags:
            ok_(
                obj[flag] is True,
                '{0} is not True on {1}'.format(flag, obj)
            )
        for flag in false_flags:
            ok_(
                obj[flag] is False,
                '{0} is not False on {1}'.format(flag, obj)
            )

    def _format_pretty_pk(self, row):
        test_pk_id = row[self.pk]
        if self.pk != 'mid':
            return test_pk_id
        real_id = self.context.res.get_mid(test_pk_id)
        return '%s[%r]' % (test_pk_id, real_id)

    def _compare_attribute(self, row_h, row, obj):
        real_h = self.aliases.get(row_h, row_h)

        check_handler_name = '_check_%s' % real_h
        if hasattr(self, check_handler_name):
            getattr(self, check_handler_name)(row_h, row, obj)
            return

        real, expected = self.caster(
            name=real_h,
            obj=obj,
            expected_value=row[row_h])

        assert expected == real, \
            "expect: {expected}, get: {real} for {obj_name}" \
            " {pretty_key} at {row_h}".format(
                expected=expected,
                real=real,
                obj_name=self.obj_name,
                pretty_key=self._format_pretty_pk(row),
                row_h=row_h,
            )

    def _row_vs_obj(self, row, obj):
        for row_h in row.headings:
            if row_h == self.pk:
                continue
            self._compare_attribute(row_h, row, obj)

    def compare(self, seq, count):
        if count is not None:
            assert len(seq) == count, \
                'Find {real} {obj_name} in [{act}], ' \
                'expect {expected} {obj_name} in [{exp}]'.format(
                    obj_name=self.obj_name,
                    real=len(seq),
                    expected=count,
                    act=seq,
                    exp=self.context.table,
                )
            if self.context.table is None:
                # ugly case -- compare just seq size
                return
        else:
            assert self.context.table is not None, \
                'Nothing to compare, table is empty, count not present!'

        ok_(
            self.pk in self.context.table.headings,
            "Can't find {0} in table: {1}".format(
                self.pk, self.context.table)
        )
        ok_(
            self.pk in seq[0],
            "Can't find {0} in seq: {1}".format(
                self.pk, seq)
        )

        for row in self.context.table:
            key = row[self.pk]
            cast_function_name = '_cast_%s' % self.pk
            if hasattr(self.caster, cast_function_name):
                #  interested only in expected value
                _, key = getattr(self.caster, cast_function_name)(
                    None, key
                )
            elif key.isdigit():
                key = int(key)

            obj = [o for o in seq if o[self.pk] == key]
            ok_(
                obj,
                "Can't find {0} {1}={2} in {3}".format(
                    self.obj_name, self.pk, key, seq)
            )
            ok_(
                len(obj) == 1,
                "Too many {0} found: {1} at key: {2}".format(
                    self.obj_name, obj, key)
            )
            self._row_vs_obj(row, obj[0])


def compare_list(context, obj_name, seq, count, pk, aliases=None):
    # pylint: disable=R0913
    TableCompartor(
        context=context,
        pk=pk,
        obj_name=obj_name,
        aliases=aliases).compare(
        seq=seq,
        count=count)


@then('in "{folder_type:w}" there are "{count:d}" messages')
def step_counted_message_list_by_ftype(context, folder_type, count):
    message_list(context, folder_type=folder_type, count=count)


@then('in "{folder_type:w}" there is one message')
def step_one_message_list_by_ftype(context, folder_type):
    message_list(context, folder_type=folder_type)


@then('in folder named "{folder_name:w}" there are "{count:d}" messages')
def step_counted_message_list_by_fname(context, folder_name, count):
    message_list(context,  folder_name=folder_name, count=count)


@then('in folder named "{folder_name:w}" there is one message')
def step_one_message_list_by_fname(context, folder_name):
    message_list(context, folder_name=folder_name)


def message_list(context, folder_type=None, folder_name=None, count=1):
    fld = context.qs.folder_by(
        folder_type=folder_type,
        folder_name=folder_name)
    messages_in_folder = context.qs.messages(fid=fld.fid)
    compare_list(
        context,
        'message',
        messages_in_folder,
        count,
        'mid',
        {'rule': 'thread_rule'}
    )


@then('message "{property:w}"s in "{folder_type:w}" are sorted in asc order')
def step_message_property_sorted_asc(context, folder_type, property):
    return step_message_property_sorted(context, folder_type, property, asc=True)


@then('message "{property:w}"s in "{folder_type:w}" are sorted in desc order')
def step_message_property_sorted_desc(context, folder_type, property):
    return step_message_property_sorted(context, folder_type, property, asc=False)


def step_message_property_sorted(context, folder_type, property, asc=True):
    # Note: nested properties are not supported
    assert property
    fld = context.qs.folder_by(folder_type=folder_type, folder_name=None)
    messages_in_folder = context.qs.messages(fid=fld.fid)
    assert len(messages_in_folder) > 1
    assert property in messages_in_folder[0]

    p = [msg[property] for msg in messages_in_folder]
    order_str = 'asc'
    if not asc:
        p.reverse()
        order_str = 'desc'

    if not all(p[i] <= p[i+1] for i in range(len(p)-1)):
        raise AssertionError('Not all message\'s \'{}\' are sorted in {} order'
                             .format(property, order_str))


@then('last stored message has correct mid')
def step_last_message_correct_mid(context, folder_type):
    mid = next(reversed(context.res))
    mid = context.res.get_mid(mid)
    last_msg = context.qs.message(mid=mid)

    msg_mid_serial = getattr(context.qs.serials(), 'next_mid_serial') - 1
    assert check_make_mid(last_msg['mid'], msg_mid_serial, last_msg['received_date'])


def check_make_mid(received_mid, mid_serial, received_date):
    # This function checks that stored procedure 'impl.make_mid'
    # still returns valid mid.
    # This is important because we depend on this in
    # the process of archiving users.

    epoch = datetime.fromtimestamp(0, tz=received_date.tzinfo)
    days_from_epoch = (received_date - epoch).days // 30
    if days_from_epoch <= 0:
        return received_mid == mid_serial

    return received_mid == ((days_from_epoch << 48) | mid_serial)


def thread_list(context, folder_type, count=1):
    fld = context.qs.folder_by_type(folder_type)
    threads_in_folder = context.qs.threads(fid=fld.fid)
    compare_list(
        context,
        'thread',
        threads_in_folder,
        count,
        'tid',
        {
            'count': 'message_count',
            'unseen': 'message_unseen',
        }
    )


@then('in "{folder_type:w}" there are "{count:d}" threads')
def step_counted_thread_list(context, folder_type, count):
    thread_list(context, folder_type, count=count)


@then('in "{folder_type:w}" there is one thread')
def step_thread_list(context, folder_type):
    thread_list(context, folder_type, count=1)


@then('in "{folder_type:w}" there are no threads')
def step_thread_list_without_threads(context, folder_type):
    thread_list(context, folder_type, count=0)


@then('in folders "{folders}" there are "{count:d}" threads')
def step_counted_threads_in_different_folders(context, folders, count):
    for folder_type in split_list(folders):
        thread_list(context, folder_type, count=count)


@then('in folders "{folders}" there is one thread')
def step_one_thread_in_different_folders(context, folders):
    for folder_type in split_list(folders):
        thread_list(context, folder_type)


@then('in IMAP "{folder_type:w}" there are "{count:d}" messages')
def step_counted_imap_list(context, folder_type, count):
    imap_list(context, folder_type, count)


@then('in IMAP "{folder_type:w}" there is one message')
def step_counted_imap_list_one(context, folder_type):
    imap_list(context, folder_type)


def imap_list(context, folder_type, count=1):
    fld = context.qs.folder_by_type(folder_type)
    imessages_in_folder = context.qs.imap_messages(fid=fld.fid)
    compare_list(
        context,
        'mail',
        imessages_in_folder,
        count,
        'mid'
    )


@then('chained "{folder_type:w}" is')
def check_chains(context, folder_type):
    CE = namedtuple('CE', ('imap_id', 'chain', 'mid'))

    fld = context.qs.folder_by_type(folder_type)
    imap_messages = context.qs.imap_messages(fid=fld.fid)
    assert imap_messages, \
        'no messages in folder: {0}'.format(fld)
    assert imap_messages[0]['chain'] is not None, \
        'first message must be chained, {0}'.format(
            imap_messages)
    imap_messages = [
        CE(
            imap_id=msg['imap_id'],
            chain=msg['chain'],
            mid=msg['mid']
        )
        for msg in imap_messages
    ]
    by_chained = []
    for msg in imap_messages:
        if msg.chain is not None:
            by_chained.append([msg])
        else:
            by_chained[-1].append(msg)

    for chain_num, real_chain in enumerate(by_chained, 1):
        real = real_chain[0].chain
        expected = len(real_chain)
        assert expected == real, \
            'expect {expected}, got {real} chain size ' \
            'at {chain_num} chain: {real_chain}'.format(
                **locals())

    expected_chains = [
        list(six.moves.map(int, r.split()))
        for r in context.table.headings]
    expected = len(expected_chains)
    real = len(by_chained)
    assert expected == real, \
        'expect {expected}, got {real} chains count,' \
        ' all messages: {imap_messages}'.format(
            **locals())

    for chain_num, (expected_chain, real_chain) in enumerate(
            zip(expected_chains, by_chained), 1):
        expected = len(expected_chain)
        real = len(real_chain)
        assert expected == real, \
            'expect {expected}, got {real} chain size,' \
            ' at {chain_num} chain: {real_chain}'.format(
                **locals())
        assert set(expected_chain) == set(msg.imap_id for msg in real_chain), \
            'chain {real_chain} don\'t contain' \
            ' all expected imap_ids: {expected_chain}'.format(
                **locals())


def chained_log_compare(context, folder_type, revision='HEAD'):
    fld = context.qs.folder_by_type(folder_type)
    revision = context.qs.revision(revision)
    chained_log = context.qs.chained_log(fid=fld.fid, revision=revision)
    assert chained_log, 'chained_log is empty'

    revisions_in_log = set(r['revision'] for r in chained_log)

    assert len(revisions_in_log) == 1, \
        'chained log contains move then one revision: {0}, ' \
        ' it\'s currently unsupported full log: {1}'.format(
            revisions_in_log,
            chained_log)

    compare_list(
        context,
        obj_name='chained_log record',
        seq=chained_log,
        count=len(context.table.rows),
        pk='mid'
    )


@then('"{folder_type:w}" chained log is')
def step_imap_log_is(context, folder_type):
    chained_log_compare(context, folder_type)


_FETCH_TABLE_Q_TEMPLATE = '''
SELECT {columns}
  FROM {schema}.{table}
 WHERE uid=%(uid)s
   AND {key}=ANY(%(keys)s)
'''


def fetch_table(conn, table_name, columns, uid, key_name, keys, schema='mail'):
    query = _FETCH_TABLE_Q_TEMPLATE.format(
        columns=', '.join(columns),
        schema=schema,
        table=table_name,
        key=key_name,
    )
    cur = conn.cursor()
    cur.execute(query, {'uid': uid, 'keys': keys})
    conn.wait()
    return list(fetch_as_dicts(cur))


def cast_mids(context, mids):
    return [context.res.get_mid(m) for m in mids]


def get_key_values_from_table(context, key):
    if key != 'mid':
        raise NotImplementedError(
            'Only mid key current implemented'
        )
    return cast_mids(context, [r['mid'] for r in context.table.rows])


@then('in table "{table_name:w}" by "{key:w}" key there is')
@then('in table "{table_name:w}" by "{key:w}" key there are')
def step_compare_table_by_key(context, table_name, key):
    real_table = fetch_table(
        context.conn,
        table_name,
        columns=context.table.headings,
        uid=context.uid,
        key_name=key,
        keys=get_key_values_from_table(context, key)
    )

    compare_list(
        context,
        obj_name=table_name,
        seq=real_table,
        count=len(context.table.rows),
        pk=key
    )


@then('in table "{table_name:w}" by "{key:w}" key there are no rows at "{keys}"')
def step_table_is_empty_by_key(context, table_name, key, keys):
    real_table = fetch_table(
        context.conn,
        table_name,
        columns=[key],
        uid=context.uid,
        key_name=key,
        keys=context.res.get_mids(keys)
    )

    assert len(real_table) == 0, \
        'Expect no rows in {table} ' \
        'for {key}={keys}, got {real_table}'.format(**locals())


@then('storage delete queue is empty')
def step_check_storage_delete_queue_is_empty(context):
    TableCompartor(
        context=context,
        pk='st_id',
        obj_name='storage_delete_queue row',
    ).compare(
        seq=select_from_storage_delete_queue(context),
        count=0,
    )


@then('in storage delete queue there is')
def step_check_storage_delete_queue(context):
    TableCompartor(
        context=context,
        pk='st_id',
        obj_name='storage_delete_queue row',
    ).compare(
        seq=select_from_storage_delete_queue(context),
        count=len(context.table.rows),
    )


def select_from_storage_delete_queue(context):
    return list(context.qs.storage_delete_queue())


class SyncedMessageCaster(AttributeCaster):
    def _cast_owner_mid(self, real, excepted):
        return self._cast_mid(real, excepted)


@then('there is one synced message')
def step_check_one_synced_message(context):
    there_are_synced_messages(context, count=1)


@then('there are "{count:d}" synced messages')
def step_check_are_synced_messages(context, count):
    there_are_synced_messages(context, count)


def there_are_synced_messages(context, count):
    TableCompartor(
        context=context,
        pk='mid',
        obj_name='synced_message',
        caster=SyncedMessageCaster(context),
    ).compare(
        seq=[o.as_dict() for o in context.qs.synced_messages()],
        count=count)


@then('there are "{count:d}" removed messages')
def step_counted_message_list_from_deleted_box(context, count):
    messages_in_deleted_box = context.qs.deleted_mails_dict()
    compare_list(
        context,
        'message',
        messages_in_deleted_box,
        count,
        'mid',
        {'rule': 'thread_rule'}
    )
