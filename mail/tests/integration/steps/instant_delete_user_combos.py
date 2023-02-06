from tests_common.pytest_bdd import given, when, then
from collections import Counter
from tests_common.steps.mdb import step_store_messages_impl


@given('user has "{limit:d}" messages')
def given_store_messages_limit(context, limit):
    context.mids = step_store_messages_impl(**locals())


@when('user has "{count:d}" messages')
def when_store_messages_limit(context, count):
    context.mids = step_store_messages_impl(context=context, limit=count)


@given('new user with {to_fill:ToFillAndMore}')
def step_register_user_to_fill(context, to_fill):
    return step_register_user_to_fill_impl(**locals())


@given('new user with "{count:d}" {to_fill:ToFillAndMore}')
def step_register_user_to_fill_counted(context, to_fill, count):
    return step_register_user_to_fill_impl(**locals())


def step_register_user_to_fill_impl(context, to_fill, count=10):
    context.messages_count = Counter(dict.fromkeys(to_fill, count))
    fill_steps = ['Given user has "{count}" {t}'.format(count=count, t=t) for t in to_fill]
    context.execute_steps(u'''
        Given new empty user in first shard
        {}
    '''.format(u'\n'.join(fill_steps)))


def make_check_queue_step(context, is_):
    if hasattr(context, 'messages_count'):
        count = sum(context.messages_count.values())
        if count > 0:
            return u'''
                Then storage delete queue has "{count}" items with deleted date {is_}in past
            '''.format(count=count, is_=is_)
    return u'''
        Then storage delete queue is empty
    '''


@when('we request delete user right now')
def step_delete_user_via_api_right_now(context):
    context.execute_steps(u'''
        When passport requests delete user right now
        Then last delete user request is successful
         And all tasks are successful
         And user is deleted with purge_date in past
    ''')


@when('we request delete user')
def step_delete_user_via_api(context):
    context.execute_steps(u'''
        When passport requests delete user
        Then last delete user request is successful
        And all tasks are successful
        And user is deleted with purge_date not in past
    ''')


@then(u'user data was deleted')
def step_check_user_deleted(context):
    context.execute_steps(u'''
        Then user does not exist in our shard
         And storage delete queue is empty
    ''')
