from tests_common.pytest_bdd import then
from ora2pg import storage


@then('"{messages_name:Var}" message does not exist in storage')
@then('"{messages_name:Var}" messages do not exist in storage')
def step_check_messages_do_not_exist(context, messages_name):
    for message in context.messages[messages_name]:
        st_id = message['st_id']
        try:
            storage.MulcaGate(
                context.config['mulcagate'].host,
                context.config['mulcagate'].port,
                mg_ca_path=context.config['mulcagate'].ssl_cert_path,
            ).get(st_id)
        except storage.DeletedMessage:
            return
        raise AssertionError(
            'Message with st_id: %s exist! (%r)' % (
                st_id, message
            )
        )


@then('"{messages_name:Var}" message exists in storage')
@then('"{messages_name:Var}" messages exist in storage')
def step_check_messages_exist(context, messages_name):
    for message in context.messages[messages_name]:
        st_id = message['st_id']
        try:
            storage.MulcaGate(
                context.config['mulcagate'].host,
                context.config['mulcagate'].port,
                mg_ca_path=context.config['mulcagate'].ssl_cert_path,
            ).get(st_id)
        except storage.DeletedMessage as exc:
            raise AssertionError(
                'Message with st_id %s does not exist: %s (%r)' % (
                    st_id, exc, message
                )
            )
