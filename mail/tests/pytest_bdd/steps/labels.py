# coding: utf-8

from pymdb.operations import (
    CreateLabel,
    ResolveLabels,
    DeleteLabel,
    UpdateLabel,
)
from pymdb.types import MailLabelDef
from tests_common.pytest_bdd import given, when, then

LABEL_DEF = '"{label_type:w}" label "{label_name:Name}"'


def get_label(context, label_type, label_name):
    labels = context.qs.labels()
    lbl = [l for l in labels if l.name == label_name and l.type == label_type]
    assert lbl, \
        "Can't find {label_name} with type {label_type} in {labels}".format(
            **locals())
    return lbl[0]


def check_label(context, label_type, label_name, message_count=None,
                revision=None, lid=None, color=None, message_seen=None):
    lbl = get_label(context, label_type, label_name)
    for attr_check in ['message_seen', 'message_count', 'revision', 'lid', 'color']:
        if locals()[attr_check] is not None:
            expected = locals()[attr_check]
            real = getattr(lbl, attr_check)
            assert expected == real, \
                'expected {attr_check}: {expected} on {lbl}'.format(
                    **locals()
                )


@then(LABEL_DEF + ' exists')
def step_check_label(context, label_type, label_name):
    check_label(**locals())


@then(LABEL_DEF + ' has "{message_count:d}" messages')
def step_check_counted_label(context, label_type, label_name, message_count):
    check_label(**locals())


@then(LABEL_DEF + ' has "{message_seen:d}" seen messages')
def step_check_seen_counted_label(context, label_type, label_name, message_seen):
    check_label(**locals())


@then(LABEL_DEF + ' has "{message_count:d}" messages at revision "{revision:d}"')
def step_check_counted_revisioned_label(context, label_type, label_name, message_count, revision):
    check_label(**locals())


@then(LABEL_DEF + ' has color "{color:w}"')
def step_check_colored_label(context, label_type, label_name, color):
    check_label(**locals())


@then(LABEL_DEF + ' has lid "{lid:d}"')
def step_check_lid_label(context, label_type, label_name, lid):
    check_label(**locals())


@then(LABEL_DEF + ' at revision "{revision:d}"')
def step_check_revisioned_label(context, label_type, label_name, revision):
    check_label(**locals())


@then(LABEL_DEF + ' has one message')
def step_check_label_with_one_message(context, label_type, label_name):
    check_label(context, label_type, label_name, message_count=1)


@then(LABEL_DEF + ' has one message at revision "{revision:d}"')
def step_check_label_with_one_rev_message(context, label_type, label_name, revision):
    check_label(context, label_type, label_name, message_count=1, revision=revision)


@then(LABEL_DEF + ' has not messages')
def step_check_label_withot_messages(context, label_type, label_name):
    check_label(context, label_type, label_name, message_count=0)


@then('user has "{labels_count:d}" labels')
def step_has_labels(context, labels_count):
    labels = context.qs.labels()
    assert len(labels) == labels_count, \
        'Expect {0} labels, got: {1}, labels: {2}'.format(
            labels_count, len(labels), labels
        )


@then(LABEL_DEF + ' does not exist')
def step_impl(context, label_name, label_type):
    labels = context.qs.labels()
    lbl = [l for l in labels if l.name == label_name and l.type == label_type]
    assert not lbl, \
        "Find %(label_name)r with %(label_type)r in %(labels)r" % locals()


@then(LABEL_DEF + ' has positive lid')
def system_label_pinned_has_positive_lid(context, label_type, label_name):
    label = get_label(context, label_type, label_name)
    assert label.lid > 0, \
        'Expect positive lid, got %r for label %r' % (
            label.lid, label)


@given(LABEL_DEF)
def step_given_create_label(context, label_type, label_name):
    context.apply_op(
        CreateLabel,
        name=label_name,
        type=label_type,
        color='green'
    )


@when('we create ' + LABEL_DEF)
def step_when_create_label(context, label_type, label_name):
    context.apply_op(
        CreateLabel,
        name=label_name,
        type=label_type,
        color='green'
    )


@when(r'we try create ' + LABEL_DEF + ' as "{op_id:OpID}"')
def step_try_create_label(context, label_type, label_name, op_id):
    context.operations[op_id] = context.make_async_operation(
        CreateLabel
    )(
        label_name, label_type, 'green'
    )


@when('we resolve ' + LABEL_DEF)
def step_resolve_label(context, label_type, label_name):
    context.make_operation(ResolveLabels)(
        [MailLabelDef(name=label_name, type=label_type)]
    ).commit()


@when('we try resolve ' + LABEL_DEF + ' as "{op_id:OpID}"')
def step_try_resolve_label(context, label_type, label_name, op_id):
    context.operations[op_id] = context.make_async_operation(
        ResolveLabels
    )([MailLabelDef(name=label_name, type=label_type)])


@when('we try to delete ' + LABEL_DEF + ' as "{op_id:OpID}"')
def step_try_delete_label(context, label_type, label_name, op_id):
    lbl = get_label(context, label_type, label_name)
    context.operations[op_id] = context.make_async_operation(
        DeleteLabel
    )(
        lbl.lid
    )


@when('we delete ' + LABEL_DEF)
def step_delete_label(context, label_type, label_name):
    lbl = get_label(context, label_type, label_name)
    context.apply_op(DeleteLabel, lid=lbl.lid)


@when('we update ' + LABEL_DEF + ' set color "{label_new_color:w}"')
def step_update_label_color(context, label_type, label_name, label_new_color):
    update_label(**locals())


@when('we update ' + LABEL_DEF + ' set name "{label_new_name:Name}"')
def step_update_label_name(context, label_type, label_name, label_new_name):
    update_label(**locals())


@when('we update ' + LABEL_DEF + ' set color "{label_new_color:w}" and name "{label_new_name:Name}"')
@when('we update ' + LABEL_DEF + ' set name "{label_new_name:Name}" and color "{label_new_color:w}"')
def step_update_label_name_and_color(context, label_type, label_name, label_new_name, label_new_color):
    update_label(**locals())


def update_label(context, label_type, label_name, label_new_name=None, label_new_color=None):
    assert label_new_color or label_new_name, 'Either new color or new name must be provided'
    lbl = get_label(context, label_type, label_name)
    o = context.make_operation(UpdateLabel)
    o(lbl.lid, label_new_name or lbl.name,
      label_new_color or lbl.color).commit()


@then(u'he has "{labels_count:d}" labels')
def step_check_all_labels(context, labels_count):
    user_labels = context.qs.labels()

    assert len(user_labels) == labels_count, \
        'Expect %d labels got %d' % (
            labels_count, len(user_labels))

    expected_labels = set()
    for row in context.table:
        expected_labels.add(
            MailLabelDef(
                name=row['name'],
                type=row['type']
            ))
    assert set(user_labels) == expected_labels, \
        'Got different labels!'
