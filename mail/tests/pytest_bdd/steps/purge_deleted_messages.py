from pymdb.operations import PurgeDeletedMessages
from tests_common.pytest_bdd import when, then


@when('we purge deleted message "{mids:MidsRange}"')
def step_purge_deleted_message(context, mids):
    mids = context.res.get_mids(mids)
    PurgeDeletedMessages(context.conn, context.uid)(mids).commit()


@then('messages table is empty')
def step_messages_is_empty(context):
    assert not context.qs.messages_table(), 'messages table is not empty'
