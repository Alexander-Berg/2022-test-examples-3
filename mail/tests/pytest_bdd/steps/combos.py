from tests_common.pytest_bdd import given, when


@given(u'new initialized user with apple and banana labels')
def step_user_with_labels(context):
    context.execute_steps(u'''
        Given new initialized user
        When we create "user" label "apple"
        And we create "user" label "banana"
        Then global revision is "3"
    ''')


@given(u'new initialized user with "{mids:MidsRange}" in "{folder_type:w}"')
def step_with_messages_in_inbox(context, mids, folder_type):
    context.execute_steps(u'''
        Given new initialized user
        When we store "{mids}" into "{folder_type}"
    '''.format(
        mids=",".join(mids),
        folder_type=folder_type,
    ))


@given(u'new initialized user with "{mids:MidsRange}"'
       ' in "{folder_type:w}" at revision "{revision:d}"')
def step_with_messages_in_inbox_at_revision(context, mids, folder_type, revision):
    context.execute_steps(u'''
        Given new initialized user
        When we store "{mids}" into "{folder_type}"
        Then global revision is "{revision}"
    '''.format(
        mids=",".join(mids),
        folder_type=folder_type,
        revision=revision
    ))


@given(u'new initialized user "{user_name:Name}" with "{folder_type:w}" folder "{folder_name:w}"')
def step_given_make_named_user_with_created_folder(context, folder_type, folder_name, user_name):
    make_user_with_created_folder(**locals())


@when(u'we initialize new user "{user_name:Name}" with "{folder_type:w}" folder "{folder_name:w}"')
def step_when_make_named_user_with_created_folder(context, folder_type, folder_name, user_name):
    make_user_with_created_folder(**locals())


@given(u'new initialized user with "{folder_type:w}" folder "{folder_name:w}"')
def step_given_make_user_with_created_folder(context, folder_type, folder_name):
    make_user_with_created_folder(**locals())


@when(u'we initialize new user with "{folder_type:w}" folder "{folder_name:w}"')
def step_when_make_user_with_created_folder(context, folder_type, folder_name):
    make_user_with_created_folder(**locals())


def make_user_with_created_folder(context, folder_type, folder_name, user_name=u'Anonymous'):
    context.execute_steps(u'''
        When we initialize new user "{user_name}"
        When we create "{folder_type}" folder "{folder_name}"
    '''.format(
        user_name=user_name,
        folder_type=folder_type,
        folder_name=folder_name,
    ))


@given(u'new initialized user with "{first_mids:MidsRange}" in "{first_folder:w}"'
       ' and "{second_mids:MidsRange}" in "{second_folder:w}"')
def step_with_messages_in_2_folders(context, first_mids, first_folder, second_mids, second_folder):
    context.execute_steps(u'''
        Given new initialized user
        When we store "{first_mids}" into "{first_folder}"
        And we store "{second_mids}" into "{second_folder}"
    '''.format(
        first_mids=u','.join(first_mids),
        first_folder=first_folder,
        second_mids=u','.join(second_mids),
        second_folder=second_folder
    ))


@when(u'we set request_info "{request_info}"')
def step_with_request_info(context, request_info):
    context.request_info = request_info


@given(u'new user with popped "{folder_type:w}"')
def step_new_user_with_pop3_folder(context, folder_type):
    context.execute_steps(u'''
        Given new initialized user
        When we enable and initialize pop3 for "{folder_type}"
        Then pop3 is enabled and initialized for "{folder_type}"
    '''.format(folder_type=folder_type))


@given(u'new user with popped "{folder_type:w}" and "{another_folder_type:w}"')
def step_new_user_with_pop3_two_folder(context, folder_type, another_folder_type):
    context.execute_steps(u'''
        Given new initialized user
        When we enable and initialize pop3 for "{folder_type}"
        And we enable and initialize pop3 for "{another_folder_type}"
        Then pop3 is enabled and initialized for "{folder_type}"
        And pop3 is enabled and initialized for "{another_folder_type}"
    '''.format(**locals()))


@given('subscriber with "{mid:Mid}" synced message ready for purge')
def step_setup_subscriber_for_purge_mid(context, mid):
    setup_subscriber_for_purge(**locals())


@given('subscriber with synced message ready for purge')
def step_setup_subscriber_for_purge(context):
    setup_subscriber_for_purge(**locals())


def setup_subscriber_for_purge(context, mid='$1'):
    """
     We should delete and purge original message,cause `code.add_to_storage_delete_queue` check st_id in messages
     before insert it into storage_delete_queue.
     https://github.yandex-team.ru/mail/mdb/blob/f3eb83662131b81dd9c3e0fa20c109b5b12a7139/code/50_add_to_storage_delete_queue.sql
     In real life same situation may happens if `owner` and `subscriber` live on different shards.
    """
    context.execute_steps(u'''
     When we initialize new user "owner" with "inbox" shared folder
     Given new st_id that does not exist in messages and storage delete queue
     When we store "{source_mid}" into "inbox" with our new st_id
     And we initialize new user "subscriber" with "folder" subscribed to "inbox@owner"
     And we sync "{source_mid}" from "inbox@owner" new message "{synced_mid}" appears
     And we switch to "owner"
     And we delete "{source_mid}"
     And we purge deleted message "{source_mid}"
     And we switch to "subscriber"
    '''.format(synced_mid=mid, source_mid=mid + '_source'))


@given(u'new initialized user with "{mid:Mid}" in "{folder_type:w}" with new st_id')
def step_with_messages_in_inbox_with_new_stid(context, mid, folder_type):
    context.execute_steps(u'''
        Given new initialized user
        And new st_id that does not exist in messages and storage delete queue
        When we store "{mid}" into "{folder_type}" with our new st_id
    '''.format(
        mid=mid,
        folder_type=folder_type,
    ))


@given(u'new initialized user with "{mid:Mid}" in "{folder_type:w}" with new st_id and attributes "{attrs}"')
def step_with_messages_in_inbox_with_new_stid_and_attr(context, mid, folder_type, attrs):
    context.execute_steps(u'''
        Given new initialized user
        And new st_id that does not exist in messages and storage delete queue
        When we store "{mid}" into "{folder_type}" with our new st_id and attributes "{attrs}"
    '''.format(
        mid=mid,
        folder_type=folder_type,
        attrs=attrs,
    ))


@given(u'new initialized user with "{mids:MidsRange}" in tab "{tab_type:w}"')
def step_with_messages_in_tab(context, mids, tab_type):
    context.execute_steps(u'''
        Given new initialized user
        When we store "{mids}" into tab "{tab_type}"
    '''.format(
        mids=",".join(mids),
        tab_type=tab_type,
    ))


@when('we create "{label_type:w}" label "{label_name:Name}" with "{message_seen:d}" message_seen')
def step_when_create_label_with_message_seen(context, label_type, label_name, message_seen):
    context.execute_steps(u'''
        When we create "{label_type}" label "{label_name}"
        And we accidentally set message_seen to "{message_seen}" for "{label_type}" label "{label_name}"
    '''.format(
        label_type=label_type,
        label_name=label_name,
        message_seen=message_seen
    ))
