from pymdb.operations import RegenerateImapId
from tests_common.pytest_bdd import when, then


@when(u'we generate new imap_id for message "{mid}"')
def step_generate_new_imap_id(context, mid):
    for mid in context.res.get_mids(mid):
        context.last_imap_id_result = context.make_operation(RegenerateImapId)(mid=mid).commit().result[0]


@then(u'we have last imap_id result equals "{imap_id:d}"')
def step_check_new_imap_id(context, imap_id):
    assert context.last_imap_id_result.imap_id == imap_id
