import json

from hamcrest import (
    assert_that,
    has_entries,
    has_item,
    empty,
    is_,
    equal_to,
    not_,
)

from pymdb.operations import (
    CreateSettings,
    DeleteSettings,
    UpdateSettings,
    ClearTaskBulkModifySettings,
    CreateTaskBulkModifySettings,
    BulkModifySettings,
    EraseSettings,
    AddUserForBulkUpdateSettings,
)
from tests_common.pytest_bdd import then, when, given


@given(u'he has enabled hidden_trash')
def step_given_enabled_hidden_trash(context):
    context.operation = context.make_operation(CreateSettings)(
        json.dumps(dict(single_settings={'mail_b2c_can_use_hidden_trash': 'on', 'hidden_trash_enabled': 'on'}))
    )


@given(u'he has enabled admin_search')
def step_given_enabled_admin_search(context):
    context.operation = context.make_operation(CreateSettings)(
        json.dumps(dict(single_settings={'mail_b2b_admin_search_allowed': 'on', 'mail_b2b_admin_search_enabled': 'on'}))
    )


@given(u'new setting "{setting}" with value "{value}"')
def step_given_create_settings(context, setting, value):
    context.operation = context.make_operation(CreateSettings)(
        json.dumps(dict(single_settings={setting: value}, signs=[dict(text='sing')]))
    )


@given(u'empty bulk modify settings task')
def step_given_empty_bulk_modify_settings_task(context):
    context.operation = context.make_operation(ClearTaskBulkModifySettings)()


@when(u'we create new setting "{setting}" with value "{value}"')
def step_when_create_settings(context, setting, value):
    context.operation = context.make_operation(CreateSettings)(
        json.dumps(dict(single_settings={setting: value, 'name': 'user'}, signs=[dict(text='sing')]))
    )


@then(u'user has setting "{setting}" with value "{value}"')
def step_get_settings(context, setting, value):
    user_settings = context.qs.settings(uid=context.uid)
    assert_that(user_settings[0].value, has_entries(
        {
            'signs': has_item(has_entries({'text': 'sing'})),
            'single_settings': has_entries({setting: value})
        }
    ))


@then(u'user has only setting "{setting}" with value "{value}"')
def step_get_only_settings(context, setting, value):
    user_settings = context.qs.settings(uid=context.uid)
    assert_that(user_settings[0].value, equal_to(dict(single_settings={setting: value})))


@when(u'we delete settings')
def step_delete_settings(context):
    context.operation = context.make_operation(DeleteSettings)()


@then(u'"{operation}" operation ended with result "{code:d}"')
def step_checking_operation(context, operation, code):
    context.operation.commit()
    assert_that(context.operation.result,
                has_item(has_entries({'{operation}_settings'.format(operation=operation): code})))


@then(u'user settings has been deleted')
def step_get_deleted_settings(context):
    assert_that(context.qs.settings(uid=context.uid), is_(empty()))


@when(u'we update setting "{setting}" with value "{value}"')
def step_update_settings(context, setting, value):
    context.operation = context.make_operation(UpdateSettings)(
        json.dumps(dict(single_settings={setting: value}))
    )


@then(u'we have bulk task modify settings with name "{name}"')
def step_get_bulk_task_modify_settings(context, name):
    bulk_task_modify_settings = context.qs.bulk_task_modify_settings()
    assert_that(bulk_task_modify_settings[0].name, equal_to(name))


@then(u'creating bulk modification task ended with result "{result}"')
def step_checking_creating_bulk_modify_settings(context, result):
    context.operation.commit()
    assert_that(context.operation.result,
                has_item(has_entries({'create_task_bulk_modify': True if result == "true" else False})))


@then(u'user has status "{status}" for "{operation}"')
def step_check_status_for_operation(context, status, operation):
    if operation == "init":
        user_for_modify_settings = context.qs.user_for_init_settings(uid=context.uid)
    else:
        user_for_modify_settings = context.qs.user_for_update_settings(uid=context.uid)
    assert_that(user_for_modify_settings[0].is_modified, equal_to(True if status == "true" else False))


@when(u'we bulk init setting')
@when(u'we bulk update setting')
def step_bulk_modify_settings(context):
    context.operation = context.make_operation(BulkModifySettings)()


@when(u'we create new bulk modify task with name "{name}" and type "{type_task}" and setting "{setting}" with value "{value}"')
def step_create_bulk_modify_settings_task(context, name, type_task, setting, value):
    context.operation = context.make_operation(CreateTaskBulkModifySettings)(
        name,
        type_task,
        json.dumps(dict(single_settings={setting: value}))
    )


@when(u'we erase settings "{settings}"')
def step_when_erase_settings(context, settings, value):
    settings = settings.split(',')
    context.operation = context.make_operation(EraseSettings)(settings)


@then(u'user has not setting "{setting}" with value "{value}"')
def step_has_not_settings(context, setting, value):
    user_settings = context.qs.settings(uid=context.uid)
    assert_that(user_settings[0].value, has_entries(
        {
            'signs': has_item(has_entries({'text': 'sing'})),
            'single_settings': not_(has_entries({setting: value}))
        }
    ))


@given(u'add setting "{setting}" with value "{value}"')
def step_add_settings(context, setting, value):
    context.operation = context.make_operation(UpdateSettings)(
        json.dumps(dict(single_settings={setting: value}))
    )


@when(u'we add user for update')
def step_when_we_add_user_for_update(context,):
    context.operation = context.make_operation(AddUserForBulkUpdateSettings)()


@then(u'we no have users for "{operation}"')
def step_then_no_have_users_for_operations(context, operation):
    if operation == "init":
        user_for_modify_settings = context.qs.user_for_init_settings(uid=context.uid)
    else:
        user_for_modify_settings = context.qs.user_for_update_settings(uid=context.uid)
    assert_that(len(user_for_modify_settings), equal_to(0))
