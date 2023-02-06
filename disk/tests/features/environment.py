# coding: utf-8

import psycopg2
from pypg import logged_connection
import logging
import os

log = logging.getLogger(__name__)


class ConnectionHolder(object):
    def __init__(self, host):
        self.connections = []
        self.host = host

    def __call__(self, dbname):
        conn = psycopg2.connect(
            'host=%s dbname=%s' % (self.host, dbname),
            connection_factory=logged_connection.LoggingConnection
        )
        self.connections.append(conn)
        return conn

PG_HOST = os.environ.get('SHARDDB_TEST_HOST', 'sharddb')
BASE_DIR = os.path.join(
    os.path.dirname(__file__),
    os.path.pardir,
    os.path.pardir,
)
connector = ConnectionHolder(PG_HOST)


def before_all(context):
    context.make_connect = connector

    context.PG_HOST = PG_HOST
    context.BASE_DIR = BASE_DIR

    context.uids = []
    context.ids = []


def before_scenario(context, scenario):
    for c in connector.connections:
        if not c.closed:
            log.warning('Got unclosed connection: %r', c)
            c.cancel()
            c.close()
    connector.connections = []


def after_step(context, step):
    if context.config.userdata.getbool('debug') and step.status == 'failed':
        # -- ENTER DEBUGGER: Zoom in on failure location.
        # NOTE: Use IPython debugger, same for pdb (basic python debugger).
        try:
            import ipdb
        except ImportError:
            import pdb as ipdb
        ipdb.post_mortem(step.exc_traceback)
