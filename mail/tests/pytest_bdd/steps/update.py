# coding: utf-8

import logging

from pymdb.operations import UpdateMessages
from pymdb.types import MailLabelDef
from tests_common.pytest_bdd import when  # pylint: disable=E0611

log = logging.getLogger(__name__)


def parse_flags(context, flags):
    res = {
        'seen': None,
        'deleted': None,
        'recent': None,
        'lids_add': [],
        'lids_del': []
    }
    assert flags, "flags must be not empty"

    all_labels = None

    for f in flags.split(','):
        f = f.strip()
        assert f.startswith('+') or f.startswith('-'), \
            'invalid flag: {0}, flags: {1}'.format(f, flags)
        add = f.startswith('+')
        f = f[1:]
        if f in ('seen', 'recent', 'deleted'):
            assert res[f] is None, \
                'flag: {0} already defined: {1}'.format(f, res)
            res[f] = add
        else:
            # treat flag as label
            assert ":" in f, \
                "strange flag {0}, not a system and not like label".format(f)
            label_def = f.split(':')
            label_def = MailLabelDef(
                type=label_def[0],
                name=label_def[1]
            )
            if all_labels is None:
                all_labels = context.qs.labels()
            label = [l for l in all_labels if l == label_def]

            assert label, \
                "Can't find {0} in labels: {1}".format(label_def, all_labels)
            label = label[0]
            if add:
                res['lids_add'].append(label.lid)
            else:
                res['lids_del'].append(label.lid)
    return res


def log_operation_result(result):
    log.info('result is: {0}'.format(result))


def make_upd_msgs_op(context, operation_maker, flags, mids):
    mids = context.res.get_mids(mids)
    op = operation_maker(UpdateMessages)(mids, **parse_flags(context, flags))
    op.add_result_callback(log_operation_result)
    return op


@when('we set "{flags}" on "{mids}"')
def step_set_flag(context, flags, mids):
    make_upd_msgs_op(context, context.make_operation, flags, mids).commit()


@when('we try set "{flags}" on "{mids}" as "{op_id}"')
def step_set_no_wait(context, flags, mids, op_id):
    context.operations[op_id] = make_upd_msgs_op(
        context, context.make_async_operation, flags, mids
    )


@when('user "{user}" sets "{flags}" on "{mid_keys:MidsRange}"')
def step_user_set_flags(context, user, flags, mid_keys):
    uid = context.users[user]
    mids = [context.res.get_mid(mid_key) for mid_key in mid_keys]
    UpdateMessages(context.conn, uid)(mids, **parse_flags(context, flags)).commit()
