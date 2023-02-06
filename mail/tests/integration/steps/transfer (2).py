import logging

import yaml

from ora2pg.app.transfer_app import TransferApp
from ora2pg.sharpei import get_shard_id
from ora2pg.transfer import (
    Transfer,
    TransferOptions,
    get_connstring_by_id,
)
from ora2pg.transfer_data import DbEndpoint
from pymdb.queries import Queries
from mail.pypg.pypg.common import transaction
from tests_common.pytest_bdd import when, then

log = logging.getLogger(__name__)


@when(u'we transfer his metadata to {to_shard_id:ShardID} shard')
@when(u'we transfer his metadata to {to_shard_id:ShardID} shard with')
def when_transfer_metadata(context, to_shard_id):
    transfer_options = dict(
        fill_change_log=False,
    )
    if context.text:
        transfer_options.update(yaml.load(context.text))
    from_shard_id = context.get_from_shard_id(to_shard_id)
    log.info('transfer_options are %r', transfer_options)
    Transfer(TransferApp(context.config)).transfer(
        user=context.user,
        from_db=DbEndpoint.make_pg(from_shard_id),
        to_db=DbEndpoint.make_pg(to_shard_id),
        options=TransferOptions(
            **transfer_options
        )
    )


@then('user leave in {shard_id:ShardID} shard')
def then_leave_in_shard(context, shard_id):
    real_shard_id = get_shard_id(
        uid=context.user.uid,
        dsn=context.config.sharddb
    )
    assert shard_id == real_shard_id, \
        'Expect %r shard_id got %r' % (
            shard_id, real_shard_id)


def check_user_marked_in_shard(context, is_here, shard_id, user_type):
    maildb_dsn = get_connstring_by_id(
        sharpei=context.config.sharpei,
        shard_id=shard_id,
        dsn_suffix=context.config.maildb_dsn_suffix
    )
    with transaction(maildb_dsn) as conn:
        maildb_queries = Queries(conn, context.user.uid)
        maildb_user = maildb_queries.passport_user_contacts_user() if user_type == 'contacts_user' else \
            maildb_queries.user()
        assert maildb_user.is_here == is_here, 'Expect %r, got %r on %r' % (is_here, maildb_user.is_here,
                                                                            maildb_user)


@then(u'user marked as {is_here:IsHere} in {shard_id:ShardID} shard')
def then_user_marked_in_shard(context, is_here, shard_id):
    check_user_marked_in_shard(context, is_here, shard_id, 'user')


@then(u'contacts user marked as {is_here:IsHere} in {shard_id:ShardID} shard')
def then_contacts_user_marked_in_shard(context, is_here, shard_id):
    check_user_marked_in_shard(context, is_here, shard_id, 'contacts_user')
