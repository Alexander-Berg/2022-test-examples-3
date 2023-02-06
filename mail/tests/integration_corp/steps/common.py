from contextlib import contextmanager
from mail.pypg.pypg.common import transaction
from ora2pg.sharpei import get_shard_id, get_connstring_by_id


@contextmanager
def get_connection_to_shard(context, shard_id):
    dsn = get_connstring_by_id(context.config.sharpei, shard_id, context.config.maildb_dsn_suffix)
    with transaction(dsn) as conn:
        yield conn


@contextmanager
def get_connection(context, user):
    shard_id = get_shard_id(user.uid, context.config.sharddb)
    with get_connection_to_shard(context, shard_id) as conn:
        yield conn
