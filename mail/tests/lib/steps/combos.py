from tests_common.pytest_bdd import given, when


@given('new user "{user_name:w}" with messages in "{folder_type:w}"')
def step_new_user_with_messages_combo(context, user_name, folder_type):
    return step_new_user_with_messages_combo_impl(**locals())


@given('new user "{user_name:w}" with "{count:d}" messages in "{folder_type:w}"')
def step_new_user_with_messages_combo_counted(context, user_name, folder_type, count):
    return step_new_user_with_messages_combo_impl(**locals())


def step_new_user_with_messages_combo_impl(context, user_name, folder_type, count=5):
    context.execute_steps(u'''
        Given new user "{user_name}"
        When she has "{count:d}" messages in "{folder_type}"
    '''.format(
        user_name=user_name,
        count=count,
        folder_type=folder_type,
    ))


@given('she has "{message_name:Var}" message in "{folder_type:w}"')
def step_store_one_message_and_mark_it(context, folder_type, message_name):
    context.execute_steps(u'''
        When she has "1" messages in "{folder_type}"
        And we mark message from "{folder_type}" as "{message_name}"
    '''.format(
        folder_type=folder_type,
        message_name=message_name,
    ))


@given('she has "{stid_type:StidType}" "{message_name:Var}" message in "{folder_type:w}"')
def step_store_one_typed_message_and_mark_it(context, folder_type, message_name, stid_type):
    context.execute_steps(u'''
        When she has "1" "{stid_type}" messages in "{folder_type}"
        And we mark message from "{folder_type}" as "{message_name}"
    '''.format(
        stid_type=stid_type,
        folder_type=folder_type,
        message_name=message_name,
    ))


@when(u'he received the messages')
def step_receive_many_messages(context):
    steps = [
        u'''
            When he received a message with mid "{mid}" in "{folder}" "{days}" days ago
        '''.format(
            mid=row.get('mid'),
            folder=row.get('folder'),
            days=row.get('days_ago'))
        for row in context.table
    ]
    context.execute_steps(u'\n'.join(steps))
