# coding: utf-8

from hamcrest import (assert_that,
                      has_length,
                      equal_to,
                      empty)

from pymdb import operations as OPS
from tests_common.pytest_bdd import given, when, then


@given('new initialized user with "{rule_type}" rule for "{folder_type:w}"')
def step_init_user_with_archivation_rule(context, folder_type, rule_type):
    init_user_with_archivation_rule(**locals())


@given('new initialized user with "{rule_type}" rule for "{folder_type:w}" with {keep_days:d} days ttl')
def step_init_user_with_archivation_rule_keep_days(context, folder_type, rule_type, keep_days):
    init_user_with_archivation_rule(**locals())


def init_user_with_archivation_rule(context, folder_type, rule_type, keep_days=30):
    context.execute_steps(u'''
        Given new initialized user
        When we set "{rule_type}" rule for "{folder_type}" with {days} days ttl
        Then "{folder_type}" has archivation rule
    '''.format(rule_type=rule_type, folder_type=folder_type, days=keep_days))


@when('we set "{rule_type:ArchivationType}" rule for "{folder_type:w}"')
def step_set_folder_archivation_rule(context, folder_type, rule_type):
    set_folder_archivation_rule(**locals())


@when('we set "{rule_type:ArchivationType}" rule for "{folder_type:w}" with {keep_days:d} days ttl')
def step_set_folder_archivation_rule_keep_days(context, folder_type, rule_type, keep_days):
    set_folder_archivation_rule(**locals())


@when(
    'we set "{rule_type:ArchivationType}" rule for "{folder_type:w}" '
    'with {keep_days:d} days ttl and {max_size:IntOrNone} max size'
)
def step_set_folder_archivation_rule_keep_days_max_size(context, rule_type, folder_type, keep_days, max_size):
    set_folder_archivation_rule(**locals())


def set_folder_archivation_rule(context, folder_type, rule_type, keep_days=30, max_size=None):
    folder = context.qs.folder_by_type(folder_type)
    context.apply_op(
        OPS.SetFolderArchivationRule,
        fid=folder.fid,
        archive_type=rule_type,
        keep_days=keep_days,
        max_size=max_size,
    )


@when('we remove archivation rule for "{folder_type:w}"')
def step_remove_folder_archivation_rule(context, folder_type):
    folder = context.qs.folder_by_type(folder_type)
    context.apply_op(
        OPS.RemoveFolderArchivationRule,
        fid=folder.fid,
    )


def get_rules_for_folder(context, folder):
    return [r for r in context.qs.folder_archivation_rules() if r.fid == folder.fid]


def get_exactly_one_rule(context, folder):
    rules = get_rules_for_folder(context, folder)
    assert_that(rules, has_length(1))
    context.archivation_rule = rules[0]
    return context.archivation_rule


@then('"{folder_type:w}" has archivation rule')
def step_check_folder_archivation_rule(context, folder_type):
    folder = context.qs.folder_by_type(folder_type)
    rule = get_exactly_one_rule(context, folder)
    if context.table:
        def assert_equal_with_types(real, exp):
            assert_that(real, equal_to(type(real)(exp)))

        expected = context.table[0]
        for key in expected.headings:
            assert_equal_with_types(getattr(rule, key), expected[key])


@then('"{folder_type:w}" has no archivation rules')
def step_check_folder_archivation_rules(context, folder_type):
    folder = context.qs.folder_by_type(folder_type)
    rules = get_rules_for_folder(context, folder)
    assert_that(rules, empty())
