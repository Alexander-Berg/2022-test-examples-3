# coding: utf-8

import logging

from pytest_bdd import parsers

from pymdb.operations import (
    CreateFolder,
    GetOrCreateFolder,
    ResetFolderUnvisited,
    UpdateFolder,
    DeleteFolder,
    UpdateFolderType,
)
from pymdb.queries import FolderDoesNotExist
from tests_common.pytest_bdd import then, when  # pylint: disable=E0611
from tools import ok_

log = logging.getLogger(__name__)


@when('we reset unvisited flag for "{folder_type}"')
def step_reset_folder_unvisited(context, folder_type):
    folder = context.qs.folder_by_type(folder_type)
    ResetFolderUnvisited(context.conn, context.uid)(folder.fid).commit()


@then('user has just initialized folders')
def step_just_initialized_folder(context):
    check_just_initialized_folder(**locals())


@then('user has folders initialized at revision "{revision:d}"')
def step_initialized_by_revision_folder(context, revision):
    check_just_initialized_folder(**locals())


def check_just_initialized_folder(context, revision=1):
    folders = context.qs.folders()
    for row in context.table:
        fld = [f for f in folders if f.type == row['type']]
        ok_(fld, "Can't find %r in folders: %r" % (row, folders))
        fld = fld[0]
        ok_(
            fld.message_count == 0
            and fld.message_unseen == 0
            and fld.revision == revision,
            "folder {0} not 'just initialized'".format(fld)
        )


@then('user has just initialized "{folder_type}"')
def step_initilized_folder(context, folder_type):
    fld = context.qs.folder_by_type(folder_type)
    ok_(
        fld.message_count == 0
        and fld.message_unseen == 0
        and fld.revision == 1,
        "folder %r not 'just initialized'" % fld
    )


@when(
    r'we try create "(?P<folder_type>\w+)" folder "(?P<folder_name>[\w-]*)" as "(?P<op_id>[\w$-]+)"',
    parse_builder=parsers.re
)
def store_try_create_folder(context, folder_type, folder_name, op_id, **kwargs):
    context.operations[op_id] = context.make_async_operation(
        CreateFolder
    )(
        name=folder_name,
        type=folder_type,
        parent_fid=None
    )


@then('user has "{folders_count:d}" folders')
def step_has_some_folders(context, folders_count):
    folders = context.qs.folders()
    assert len(folders) == folders_count, \
        'expect: {0}, but folders {1} folders: {2}'.format(
            folders_count, len(folders), folders)


@then('user don\'t have any folders')
def step_no_folder(context):
    folders = context.qs.folders()
    ok_(
        len(folders) == 0,
        "user have folders: %r" % folders
    )


@when('user do not have folder named "{name:w}"')
def step_no_folder_with_name_and_symbol(context, name):
    success = False
    try:
        context.qs.folder_by_name(name)
    except FolderDoesNotExist:
        success = True
    except:
        pass
    ok_(success)


def check_folder_attaches(  # pylint: disable=W0613
        context, folder_type, attach_count, attach_size):
    fld = context.qs.folder_by_type(folder_type)
    for attr in ['attach_count', 'attach_size']:
        expected = locals()[attr]
        real = getattr(fld, attr)
        assert expected == real, \
            'Expect %r, got %r %s on folder %r' % (
                expected, real, attr, fld)


@then('"{folder_type:w}" has "{attach_count:d}"'
      ' attaches with "{attach_size:d}" size')
def step_folder_with_attaches(context, folder_type, attach_count, attach_size):
    check_folder_attaches(context, folder_type, attach_count, attach_size)


@then('"{folder_type:w}" has one attach with "{attach_size:d}" size')
def step_folder_with_one_attach(context, folder_type, attach_size):
    check_folder_attaches(context, folder_type, 1, attach_size)


@then('"{folder_type:w}" folder does not exist')
def step_folder_does_not_exists(context, folder_type):
    check_folder_does_not_exists(**locals())


@then('folder named "{folder_name:w}" does not exist')
def step_folder_named_does_not_exists(context, folder_name):
    check_folder_does_not_exists(**locals())


def check_folder_does_not_exists(context, folder_type=None, folder_name=None):
    try:
        folder = context.qs.folder_by(
            folder_type=folder_type,
            folder_name=folder_name,
        )
        raise AssertionError(
            '%r folder exists %r' % folder
        )
    except FolderDoesNotExist:
        pass


def fid_by_var(context, full_var_name):
    var_name = full_var_name[:full_var_name.find('.')]
    var = context.res[var_name]
    return var[0].fid


@then('"{fid_var}" folder path is "{folder_path}"')
def step_folder_path(context, fid_var, folder_path):
    expected_path = folder_path.split('|')
    fid = fid_by_var(context, fid_var)
    all_folders = context.qs.folders()
    fld = [f for f in all_folders if f.fid == fid]
    assert fld, \
        'Can\'t find folder with fid: {fid},' \
        ' by {fid_var} in user folders {all_folders}'.format(
            **locals())
    assert len(fld) == 1, \
        'Too many folders found: %d, all found %r' % (len(fld), fld)
    fld = fld[0]
    real_path = context.qs.folder_path(fid=fld.fid)
    assert real_path == expected_path, \
        'Got different path, expected: {0} real: {1} on folder: {2}'.format(
            expected_path, real_path, fld)


def define_creation_step(operation, context, **kwargs):
    folder_name = kwargs.pop('folder_name')
    folder_type = kwargs.pop('folder_type')
    parent_fid = None
    if kwargs['parent_name']:
        parent_fid = context.qs.folder_by_name(kwargs['parent_name']).fid
    elif kwargs['parent_fid_by_var']:
        full_var_name = kwargs['parent_fid_by_var']
        var_name = full_var_name[:full_var_name.find('.')]
        var = context.res[var_name]
        parent_fid = var[0].fid
    op = context.make_operation(operation)(
        name=folder_name,
        type=folder_type,
        parent_fid=parent_fid
    )
    op.commit()
    if kwargs.get('op_result'):
        context.res[kwargs['op_result']] = op.result


def folder_creation_re(action):
    return r'''
(?:s?he|we) {0} "(?P<folder_type>\w+)"
folder "(?P<folder_name>[\w]+)"
(?:under "((?P<parent_name>[\w|]+)|(?P<parent_fid_by_var>\$[\w.]+))")?
(?:as "(?P<op_result>\$\w+)")?
'''.format(action).strip().replace(u'\n', r'\s*')


@when(folder_creation_re("create"), parse_builder=parsers.re)
def step_create_folder(context):
    kwargs = context.args
    define_creation_step(CreateFolder, context, **kwargs)


@when(folder_creation_re("get or create"), parse_builder=parsers.re)
def step_get_or_create_folder(context):
    kwargs = context.args
    define_creation_step(GetOrCreateFolder, context, **kwargs)


@when(r'we delete folder (named "(?P<folder_name>\w+)"|with fid "(?P<fid>\d+)")', parse_builder=parsers.re)
def delete_folder(context):
    kwargs = context.args
    if kwargs['fid']:
        fid = int(kwargs['fid'])
    else:
        fid = context.qs.folder_by_name(kwargs['folder_name']).fid
    context.make_operation(DeleteFolder)(fid=fid).commit()


@when(
    r'we try to delete folder (named "(?P<folder_name>\w+)"|with fid "(?P<fid>\d+)") as "(?P<op_id>[\w$-]+)"',
    parse_builder=parsers.re
)
def try_delete_folder(context):
    kwargs = context.args
    if kwargs['fid']:
        fid = int(kwargs['fid'])
    else:
        fid = context.qs.folder_by_name(kwargs['folder_name']).fid
    o = context.make_async_operation(DeleteFolder)
    o(fid)
    context.operations[kwargs['op_id']] = o


def parse_counter(counter):
    if counter is None:
        return None
    counter = counter.strip('"')
    if counter.isdigit():
        return int(counter)
    elif counter == 'one':
        return 1
    elif counter in ['not', 'no', 'zero']:
        return 0
    raise AssertionError("unsupported counter value {0}".format(counter))


def parse_flag(value):
    try:
        return {None: None, '': True, 'not': False}[value]
    except KeyError:
        raise AssertionError('unsupported flag value "%s"' % value)


CONFLICT_COUNTERS = [
    set(['message_seen', 'message_unseen']),
    set(['message_count', 'empty_folder', 'not_empty_folder'])
]

FOLDER_RE = r'''
(?:"(?P<type>\w+)"|folder named "(?P<name>[\w|]+)") (?:has|is)
(?:
(,| and)?\s*
(?:
(?:(?P<message_unseen>({0})) unseen)|
(?:(?P<message_seen>({0})) seen)|
(?:(?P<message_count>({0})) messages?)|
(?:(?P<message_recent>({0})) recent)|
(?:(?P<message_size>({0})) size)|
(?:fid "(?P<fid>(\d+))")|
(?:first_unseen at "(?P<first_unseen>\d+)")|
(?:first_unseen_id at "(?P<first_unseen_id>\d+)")|
(?:(?P<empty_folder>empty))|
(?:(?P<not_empty_folder>not empty))|
(?:(?P<unique_type>(:?not)?) unique type)|
(?:(?: at)? revision "(?P<revision>(\d+))"|
(?:(?P<unvisited>(?:not)?) unvisited))
)
)+
'''.format(
    r'not|no|one|"\d+"',
).strip().replace('\n', '')


@then(FOLDER_RE, parse_builder=parsers.re)
def step_check_folder_rich(context):
    kwargs = context.args
    counters = {}
    for k in [
            'message_count',
            'message_unseen', 'message_seen',
            'message_recent',
            'message_size',
            'revision',
            'fid',
            'first_unseen', 'first_unseen_id']:
        val = parse_counter(kwargs[k])
        if val is not None:
            counters[k] = val

    for counters_group in CONFLICT_COUNTERS:
        exists_counters = counters_group.intersection(set(counters.keys()))
        assert len(exists_counters) <= 1, \
            'Got duplicate conditions {exists_counters}' \
            ' from group: {counters_group}' \
            ' counters: {counters}, kwargs: {kwargs}'.format(
                **locals())

    check_is_not_empty = False
    for k in ['empty_folder', 'not_empty_folder']:
        if kwargs[k] is None:
            continue
        if k == 'empty_folder':
            counters['message_count'] = 0
        else:
            check_is_not_empty = True

    for k in ['unvisited', 'unique_type']:
        val = parse_flag(kwargs[k])
        if val is not None:
            counters[k] = val

    assert (counters or check_is_not_empty), (
        'Got itself in trouble,'
        ' counters:{counters}, check_is_not_empty:{check_is_not_empty},'
        ' kwargs: {kwargs}, re:'.format(
            **locals()) + FOLDER_RE)

    assert counters.get('revision') != 100500, \
        'Ask for raise: counters:{counters}' \
        ' kwargs:{kwargs}'.format(**locals())

    folder_by = 'type' if kwargs['type'] else 'name'
    assert folder_by, \
        "Can't find type and name in {0}".format(kwargs)

    fld = context.qs.folder_by_attribute(
        folder_by, kwargs[folder_by]
    )
    for counter, expected in counters.items():
        real = getattr(fld, counter)
        ok_(
            expected == real,
            "{expected}!={real} on {counter} for {fld}".format(**locals())
        )

    if check_is_not_empty:
        ok_(
            fld.message_count != 0,
            "folder is empty {0}".format(fld)
        )


@then(u'folder named "{folder_name:w}" exists')
def step_check_folder_exists(context, folder_name):
    context.qs.folder_by_name(folder_name)


@when(u'we set name on folder named "{old_name}" to "{new_name}"')
def step_modify_any_folder(context, old_name, new_name):
    folder = context.qs.folder_by('user', old_name)
    context.apply_op(UpdateFolder, fid=folder.fid, new_name=new_name, new_parent=None)


@when(u'we try to set name on folder fid "{fid}" to "{new_name}" as "{op_id}"')
def step_try_rename_any_folder(context, fid, new_name, op_id):
    context.operations[op_id] = context.make_async_operation(
        UpdateFolder
    )(
        fid, new_name, None
    )


@when(u'we try to set parent on "{type}" folder named "{name}" to "{new_parent}" as "{op_id}"')
def step_try_reparent_any_folder(context, type, name, new_parent, op_id):
    folder = context.qs.folder_by(type, name)
    context.operations[op_id] = context.make_async_operation(
        UpdateFolder
    )(
        folder.fid, folder.name, new_parent
    )


@when(u'we set type of folder named "{name}" to "{new_type}"')
def step_modify_any_folder_type(context, name, new_type):
    folder = context.qs.folder_by('user', name)
    context.apply_op(UpdateFolderType, fid=folder.fid, new_type=new_type)


@then(u'unique_type of folder named "{name}" is "{unique_type}"')
def step_modify_any_folder_unique_type(context, name, unique_type):
    folder = context.qs.folder_by_name(name)
    ok_("{0}".format(unique_type) == "{0}".format(folder.unique_type),
        "folder.unique_type: {0} != {1}".format(folder.unique_type, unique_type))


@when(u'we try set type of folder named "{name}" to "{new_type}" as "{op_id}"')
def step_modify_any_folder_type_no_wait(context, name, new_type, op_id):
    folder = context.qs.folder_by('user', name)
    context.operations[op_id] = context.make_async_operation(
        UpdateFolderType
    )(
        folder.fid, new_type
    )


@when(u'we try set type of folder fid "{fid}" to "{new_type}" as "{op_id}"')
def step_modify_any_folder_type_by_fid(context, fid, new_type, op_id):
    context.operations[op_id] = context.make_async_operation(
        UpdateFolderType
    )(
        fid, new_type
    )
