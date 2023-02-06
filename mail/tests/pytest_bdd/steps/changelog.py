# coding: utf-8

import json
import os.path

import jsonschema
import yaml

from .lists import AttributeCaster, TableCompartor
from tests_common.pytest_bdd import then

_MY_DIR = os.path.dirname(__file__)


class ChangelogAttributeCaster(AttributeCaster):
    def _custom_cast_mids(self, obj, expected_value):
        expected_mids = self.context.res.get_mids(expected_value)
        real_mids = [o['mid'] for o in obj['changed']]
        return set(real_mids), set(expected_mids)


def changelog_compare(context, changelog):
    assert changelog, 'changelog is empty'
    TableCompartor(
        context=context,
        obj_name='change',
        pk='revision',
        caster=ChangelogAttributeCaster(context),
    ).compare(changelog, count=None)


@then('in changelog there are')
@then('in changelog there is')
def step_full_changelog_compare(context):
    changelog_compare(context, context.qs.changelog())


def get_last_changelog_entry(context):
    full_changelog = context.qs.changelog()
    assert full_changelog, \
        'changelog is empty %r' % full_changelog
    max_revision = max(c['revision'] for c in full_changelog)
    entries_at_max_revision = [
        c for c in full_changelog
        if c['revision'] == max_revision]
    assert len(entries_at_max_revision) == 1, \
        'Expect one entry at revision %r, got %d: %r' % (
            max_revision,
            len(entries_at_max_revision),
            entries_at_max_revision)
    return entries_at_max_revision[0]


def get_last_changelog_entry_expect_change_type(context, change_type):
    changelog_entry = get_last_changelog_entry(context)
    assert changelog_entry['type'] == change_type, \
        'Expect %r change_type, got %r, on %r' % (
            change_type, changelog_entry['type'], changelog_entry)
    return changelog_entry


@then(u'"{change_type:DashedWord}" is last changelog entry')
def step_check_last_changelog_entry(context, change_type):
    get_last_changelog_entry_expect_change_type(context, change_type)


@then(u'"{change_type:DashedWord}" is last changelog entry '
      'with "{useful_new_count:d}" as useful_new_count')
def step_last_changlog_entry_with_useful(context, change_type, useful_new_count):
    changelog_entry = get_last_changelog_entry_expect_change_type(context, change_type)
    assert changelog_entry['useful_new_count'] == useful_new_count, \
        'Expect %r useful_new_count got %r, on %r' % (
            useful_new_count, changelog_entry['useful_new_count'],
            changelog_entry
        )


def get_shema_def(filename):
    import library.python.resource as rs
    schema = rs.find('resfs/file/mail/pg/mdb/tests/schemas/%s' % filename)
    if not schema:
        raise RuntimeError(
            "Can't find %s schema file" % filename
        )
    return json.loads(schema)


@then(u'last changelog.{attr:ChangeLogAttribute} matches "{filename:FilePath}" schema')
def step_check_schema_on_last_changelog_entry(context, attr, filename):
    changelog_entry = get_last_changelog_entry(context)
    try:
        jsonschema.validate(changelog_entry[attr], get_shema_def(filename))
    except jsonschema.ValidationError as exc:
        raise AssertionError(exc)


def get_context_text_as_yaml(context):
    try:
        return yaml.safe_load(context.text)
    except (AttributeError, SyntaxError) as exc:
        raise SyntaxError('This step require yaml in .text: %s' % exc)


@then(u'"{change_type:DashedWord}" is last changelog entry with one changed element like')
def step_check_last_changelog_entry_with_changed_like(context, change_type):
    expected_changed = get_context_text_as_yaml(context)
    changelog_entry = get_last_changelog_entry_expect_change_type(context, change_type)

    real_changed = changelog_entry['changed']
    assert len(real_changed) == 1, \
        'Expect one element in real_changed got %d, %r' % (
            len(real_changed), real_changed)
    real_changed = real_changed[0]

    for expected_key, expected_value in expected_changed.items():
        assert expected_key in real_changed, \
            'There is no %s key in changed %r' % (
                expected_key, real_changed)
        assert real_changed[expected_key] == expected_value, \
            'Expect %r on %s, got %r' % (
                expected_value, expected_key, real_changed[expected_key])


REINDEX_CHANGE_TYPE = 'reindex'


@then('in changelog there are reindex changes')
def filtered_changlog(context):
    # workaround for 'reindex' changes
    # util.fill_changelog_for_msearch don't change revision
    # so *classic* changelog comparator can't compare rows
    changelog = context.qs.changelog()
    assert changelog, 'expect not empty changelog got %r' % changelog
    filtered_changelog = [
        c for c in changelog if c['type'] == REINDEX_CHANGE_TYPE
    ]
    assert filtered_changelog, \
        'there are no changes %r in changelog: %r' % (
            REINDEX_CHANGE_TYPE, filtered_changelog)
    changelog_compare(context, filtered_changelog)


@then('it produce "{cid_var:Variable}"')
def step_get_last_operation_cid(context, cid_var):
    try:
        last_op = context.operations.last()
    except IndexError:
        raise AssertionError(
            'There are no options in %r' % context.operations
        )
    last_entry = get_last_changelog_entry_expect_change_type(
        context,
        last_op.change_type.value)
    if not hasattr(context, 'cids'):
        context.cids = {}
    context.cids[cid_var] = last_entry['cid']
