from ora2pg.app.transfer_app import TransferApp
from ora2pg.sharpei import get_shard_id
from ora2pg.transfer import Transfer, TransferOptions
from ora2pg.transfer_data import DbEndpoint
from ora2pg.transfer_subscriptions import BadSubscriptionError
from pymdb.queries import Queries
from tests_common.pytest_bdd import when, then
from .common import get_connection_to_shard


def get_from_shard(shards, to_shard):
    return shards.second if to_shard == shards.first else shards.first


@when(u'I transfer "{user_name:w}" to {to_shard:Shard} shard')
def step_transfer(context, user_name, to_shard):
    transfer_options = dict(
        fill_change_log=False,
    )
    from_shard = get_from_shard(context.shards, to_shard)
    Transfer(TransferApp(context.config)).transfer(
        user=context.users[user_name],
        from_db=DbEndpoint.make_pg(from_shard.value),
        to_db=DbEndpoint.make_pg(to_shard.value),
        options=TransferOptions(**transfer_options)
    )


@then(u'transfer "{user_name:w}" to {to_shard:Shard} shard failed with BadSubscriptionError')
def step_transfer_failed(context, user_name, to_shard):
    transfer_options = dict(
        fill_change_log=False,
    )
    from_shard = get_from_shard(context.shards, to_shard)
    try:
        Transfer(TransferApp(context.config)).transfer(
            user=context.users[user_name],
            from_db=DbEndpoint.make_pg(from_shard.value),
            to_db=DbEndpoint.make_pg(to_shard.value),
            options=TransferOptions(**transfer_options)
        )
    except BadSubscriptionError:
        return
    raise AssertionError('Transfer must fail')


@then('"{user_name:w}" leave in {shard:Shard} shard')
def step_check_shard_id_from_sharpei(context, user_name, shard):
    real_shard_id = get_shard_id(
        uid=context.users[user_name].uid,
        dsn=context.config.sharddb
    )
    assert shard.value == real_shard_id, \
        'Expect %r shard_id got %r' % (
            shard.value, real_shard_id)


@then(u'"{user_name:w}" marked as {is_here:IsHere} in {shard:Shard} shard')
def step_check_is_here(context, user_name, is_here, shard):
    with get_connection_to_shard(context, shard.value) as conn:
        maildb_queries = Queries(conn, context.users[user_name].uid)
        maildb_user = maildb_queries.user()
        assert maildb_user.is_here == is_here, \
            'Expect %r, got %r on %r' % (
                is_here, maildb_user.is_here, maildb_user)
