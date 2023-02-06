from tests_common.pytest_bdd import when, then


@when('we make util_freeze_user request')
def step_when_freeze_user(context):
    context.response = context.shiva.client().util().freeze_user(uid=context.get_user().uid)


@when('we make util_archive_user request')
def step_when_archive_user(context):
    context.response = context.shiva.client().util().archive_user(uid=context.get_user().uid)


@when('we make util_purge_transferred_user request')
def step_when_purge_transferred_user(context):
    context.response = context.shiva.client().util().purge_transferred_user(uid=context.get_user().uid)


@when('we make util_clean_archive request')
def step_when_clean_archive(context):
    context.response = context.shiva.client().util().clean_archive(uid=context.get_user().uid)


@when('we make util_purge_archive request')
def step_when_purge_archive(context):
    context.response = context.shiva.client().util().purge_archive(uid=context.get_user().uid)


@then(u'shiva responds server error')
def step_then_shiva_responds_server_error(context):
    assert context.response.status_code == 500, \
        'Expected: status_code 500, but was: "{}"'.format(context.response.status_code)


@then(u'shiva responds bad request')
def step_then_shiva_responds_bad_request(context):
    assert context.response.status_code == 400, \
        'Expected: status_code 400, but was: "{}"'.format(context.response.status_code)
