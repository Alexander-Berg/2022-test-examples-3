from ora2pg.sharpei import init_in_sharpei, get_connstring_by_id
from mail.pypg.pypg.common import transaction
from tests_common.fbbdb import user_from_id, add_user as add_user_to_fbb
from pymdb.operations import Init


def make_new_user(context, user_name):
    uid = context.get_free_uid()
    user = user_from_id(uid, user_name=user_name)
    context.users.add(user, user_name)
    return user


def init_user_in_sharpei(context, user, shard_id):
    init_in_sharpei(
        user.uid,
        context.config.sharddb,
        allow_inited=False,
        shard_id=shard_id,
    )


def init_user_in_maildb(context, user, shard_id):
    dsn = get_connstring_by_id(context.config.sharpei, shard_id, context.config.maildb_dsn_suffix)
    with transaction(dsn) as conn:
        Init(conn, user.uid)()


def create_new_user(context, user_name, shard):
    user = make_new_user(context, user_name)
    init_user_in_sharpei(context, user, shard.value)
    init_user_in_maildb(context, user, shard.value)
    add_user_to_fbb(context.fbbdb_conn, user)
    return user
