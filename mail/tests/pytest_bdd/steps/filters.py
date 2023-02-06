# coding: utf-8

from hamcrest import assert_that, has_properties, has_length

from pymdb.operations import (
    CreateRule,
    CreateUnsubscribeRule,
    CreateSystemRules,
    RemoveRule,
    RemoveRules,
    VerifyAction,
    EnableRule,
    DisableRule,
    OrderRules,
)
from tests_common.pytest_bdd import then, when
from tools.filters_lang import parse_filter


def check_filters_count(context, expected_count):
    filters = context.qs.filters()
    assert len(filters) == expected_count, \
        'Expect %d filters, got: %d: filters: %r' % (
            expected_count, len(filters), filters)


@then(u'user has no filters')
def step_has_not_filters(context):
    check_filters_count(context, 0)


@then(u'user has {rules_count:d} filters of "{rule_type}" type')
def step_rules_get_rows(context, rules_count, rule_type):
    rules = context.qs.rules_get(uid=context.uid, type=rule_type)
    rules_count = int(rules_count)
    assert len(rules) == rules_count, \
        'Expect %d filters, got: %d: filters: %r' % (
            rules_count, len(rules), rules)


@then(u'unsubscribe filter "{name}" has {actions_count:d} actions and {conditions_count:d} conditions')
def step_unsubscribe_count_actions_and_conditions(context, name, actions_count, conditions_count):
    flt = get_filter_by_name(context, name)
    assert_that(
        flt,
        has_properties({
            'type': 'unsubscribe',
            'actions': has_length(actions_count),
            'conditions': has_length(conditions_count)
        })
    )


@then(u'user has one filter')
def step_has_one_filter(context):
    check_filters_count(context, 1)


@then(u'user has {rules_count:d} filters')
def step_has_n_filter(context, rules_count):
    check_filters_count(context, rules_count)


def make_plane_actions(fd):
    plane = []
    for a in fd.actions:
        plane += [
            a.oper,
            a.param,
            'yes' if a.verified else 'no'
        ]
    return plane


def make_plane_conditions(fd):
    plane = []
    for c in fd.conditions:
        plane += [
            c.field_type,  # field_type
            c.field,  # field,
            c.pattern,  # pattern,
            c.oper,  # oper???
            c.link,
            'yes' if c.negative else 'no'  # negative
        ]
    return plane


def do_create_filter(operation_maker, fd, name, old_rule_id=None, enabled=True, rule_type='user'):
    return operation_maker(CreateRule)(
        name=name,
        enabled=enabled,
        stop=False,
        last=True,
        acts=make_plane_actions(fd),
        conds=make_plane_conditions(fd),
        old_rule_id=old_rule_id,
        rule_type=rule_type,
    )


def do_create_unsubscribe(operation_maker, email, name, message_type, fid):
    return operation_maker(CreateUnsubscribeRule)(
        email=email,
        name=name,
        message_type=message_type,
        fid=fid,
    )


def do_create_system(operation_maker):
    return operation_maker(CreateSystemRules)()


def create_filter(context, name, fd, old_rule_id=None, enabled=True, rule_type='user'):
    do_create_filter(
        operation_maker=context.make_operation,
        fd=fd,
        name=name,
        old_rule_id=old_rule_id,
        enabled=enabled,
        rule_type=rule_type
    ).commit()


def create_unsubscribe(context, email, name, message_type, fid):
    do_create_unsubscribe(
        operation_maker=context.make_operation,
        email=email,
        name=name,
        message_type=message_type,
        fid=fid,
    ).commit()


def create_system(context):
    do_create_system(
        operation_maker=context.make_operation,
    ).commit()


@when(u'we create filter "{name}"')
@when(u'he create filter "{name}"')
def step_create_filter(context, name):
    create_filter(context, name, parse_filter(context.text))


@when(u'we create typed filter "{name}" with type "{type}"')
def step_create_filter_type(context, name, type):
    create_filter(context, name, parse_filter(context.text), None, True, type)


@when(u'we create unsubscribe for "{email}" with name "{name}" with type "{message_type}"')
def step_create_unsubscribe(context, email, name, message_type):
    create_unsubscribe(context, email, name, message_type, "22")


@when(u'we create base system filters')
def step_create_system(context):
    create_system(context)


@when(u'we create disabled filter "{name}"')
def step_create_disabled_filter(context, name):
    create_filter(context, name, parse_filter(context.text), enabled=False)


@when(u'we try create filter "{name}" as "{op_id}"')
def step_try_create_filter(context, name, op_id):
    context.operations[op_id] = do_create_filter(
        operation_maker=context.make_async_operation,
        name=name,
        fd=parse_filter(context.text),
    )


def get_filter_by_name(context, name):
    all_filters = context.qs.filters()
    assert all_filters, 'No filters!'
    flt = [f for f in all_filters if f.name == name]
    assert flt, \
        'No filter with %s found all: %r' % (name, all_filters)
    assert len(flt) == 1, \
        'Too many filters with name: %s found: %r' % (name, flt)
    return flt[0]


@then(u'filter "{name}" is')
def step_check_filter(context, name):
    fd = parse_filter(context.text)
    flt = get_filter_by_name(context, name)
    assert flt.conditions == fd.conditions, \
        'Got different conditions, expected: {0},' \
        ' real: {1}'.format(fd.conditions, flt.conditions)
    assert flt.actions == fd.actions, \
        'Got different actions, expected: {0}, ' \
        ' real: {1}'.format(fd.actions, flt.conditions)


@when(u'we replace filter "{old_name}" with "{new_name}"')
def step_replace_rule(context, old_name, new_name):
    old_flt = get_filter_by_name(context, old_name)
    fd = parse_filter(context.text)
    create_filter(context, new_name, fd, old_rule_id=old_flt.rule_id)


@when(u'we delete filter "{name}"')
def step_delete_rule(context, name):
    flt = get_filter_by_name(context, name)
    context.apply_op(RemoveRule, rule_id=flt.rule_id)


@when(u'we delete two filters "{name1}" and "{name2}"')
def step_delete_two_rules(context, name1, name2):
    flt1 = get_filter_by_name(context, name1)
    flt2 = get_filter_by_name(context, name2)
    ids = [flt1.rule_id, flt2.rule_id]
    context.apply_op(RemoveRules, rule_ids=ids)


@when(u'we verify filter "{filter_name}" action "{action_desc}"')
def step_verify_action(context, filter_name, action_desc):
    flt = get_filter_by_name(context, filter_name)
    assert len(flt.actions) == 1, \
        'Filters with multi actions currently unsupported'
    context.apply_op(
        VerifyAction,
        action_id=flt.actions[0].action_id
    )


@then(u'filter "{filter_name}" action "{action_desc}" is {is_verified:Verified}')
def step_is_verified_action(context, filter_name, action_desc, is_verified):
    flt = get_filter_by_name(context, filter_name)
    assert len(flt.actions) == 1, \
        'Filters with multi actions currently unsupported'
    action = flt.actions[0]
    assert action.verified == is_verified, \
        'Expect action.verified is {is_verified}, ' \
        'got {action.verified}, action: {action}'.format(**locals())


@then(u'filter "{name}" is {is_enabled:Enabled}')
def step_is_enabled(context, name, is_enabled):
    flt = get_filter_by_name(context, name)
    assert flt.enabled == is_enabled, \
        'Expect enabled={is_enabled}, got {flt.enabled} ' \
        ', filter={flt}'.format(**locals())


@when(u'we enable filter "{name}"')
def step_enable_filter(context, name):
    flt = get_filter_by_name(context, name)
    context.apply_op(
        EnableRule,
        rule_id=flt.rule_id
    )


@when(u'we disable filter "{name}"')
def step_disable_filter(context, name):
    flt = get_filter_by_name(context, name)
    context.apply_op(
        DisableRule,
        rule_id=flt.rule_id
    )


def get_names_from_table(context):
    assert context.table, 'This step expect table'
    names = [context.table.headings[0]]
    return names + [r[0] for r in context.table]


@then(u'user filters are in order')
def step_check_filters_order(context):
    filters = context.qs.rules_get(uid=context.uid, type='user')
    names = get_names_from_table(context)
    assert len(filters) == len(names), \
        'Got different names: {names} ' \
        'and filters: {filters} count'.format(**locals())

    for i, (name, flt) in enumerate(zip(names, filters)):
        assert name == flt.name, \
            'Got different order at: {i}, ' \
            'expect "{name}" got "{flt.name}", ' \
            'filter: {flt}'.format(**locals())


@when(u'we reorder user filters')
def step_reorder_filteres(context):
    all_names = get_names_from_table(context)
    filters = context.qs.filters()
    rules_ids = []
    for name in all_names:
        flt = [f for f in filters if f.name == name]
        assert len(flt) == 1, \
            "Can't find filter {name} in filters: {filters}".format(**locals())
        rules_ids.append(flt[0].rule_id)
    context.apply_op(
        OrderRules,
        rule_ids=rules_ids
    )
