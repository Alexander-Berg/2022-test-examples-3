import logging
from contextlib import contextmanager
from collections import namedtuple

import mail.pypg.pypg.connect
import psycopg2
from mail.devpack.lib.components.all import All
from mail.devpack.lib.components.base import FakeRootComponent
from mail.devpack.lib.components.fakebb import FakeBlackbox
from mail.devpack.lib.components.fbbdb import FbbDb
from mail.devpack.lib.components.mdb import Mdb
from mail.devpack.lib.components.sharddb import ShardDb
from mail.devpack.lib.components.sharpei import Sharpei
from mail.devpack.lib.components.unimock import Unimock
from mail.devpack.tests.helpers.fixtures import coordinator_context
from ora2pg.storage import MulcaGate

log = logging.getLogger(__name__)


def with_unimock(component):
    class ComponentWithUnimock(FakeRootComponent):
        NAME = component.NAME + '_with_unimock'
        DEPS = [component, Unimock]

    return ComponentWithUnimock


@contextmanager
def make_coordinator(start_component=All):
    top_component = with_unimock(start_component)
    with coordinator_context(top_component) as coord:
        yield coord


def fill_coordinator_context(context, coordinator):
    context.coordinator = coordinator
    components = context.coordinator.components

    context.sharpei = components.get(Sharpei)
    context.blackbox = components.get(FakeBlackbox)
    context.maildb = components.get(Mdb)
    context.sharddb = components.get(ShardDb)
    context.fbbdb = components.get(FbbDb)
    context.mulcagate = components.get(Unimock)

    context.config = dict2args({
        'blackbox': context.blackbox and 'http://localhost:{}/blackbox'.format(context.blackbox.port),
        'sharpei': context.sharpei and 'http://[::1]:{}'.format(context.sharpei.webserver_port()),
        'maildb_dsn_suffix': 'user=root connect_timeout=5',
        'maildb': context.maildb and context.maildb.dsn(),
        'sharddb': context.sharddb and context.sharddb.dsn(),
        'mulcagate': context.mulcagate and MulcaGate('http://localhost', context.mulcagate.port),
        'default_shard_id': '1',
    })

    context.maildb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(context.config.maildb, autocommit=True).connect()
    )
    context.sharddb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(context.config.sharddb, autocommit=True).connect()
    )
    context.fbbdb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(components[FbbDb].dsn(), autocommit=True).connect()
    )


def dict2args(d):
    Args = namedtuple('Args', d.keys())
    return Args(**d)


def perform_with_retries(f, error_class=psycopg2.OperationalError):
    last_exception = None
    for _ in range(10):
        try:
            return f()
        except error_class as e:
            last_exception = e
            log.exception(e)
    raise last_exception
