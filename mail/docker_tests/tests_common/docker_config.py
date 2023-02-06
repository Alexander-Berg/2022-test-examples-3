# coding: utf-8

from collections import namedtuple
import logging

import mysql.connector
import mysql.connector.errors
import mail.pypg.pypg.connect
import psycopg2
import pymongo
import pymongo.errors

from ora2pg.app import config_file
from ora2pg.app.transfer_app import dict2args

HostPort = namedtuple('HostPort', ('host', 'port'))
log = logging.getLogger(__name__)


def serialize_host_port(host_port):
    return host_port.host + ':' + str(host_port.port)

HostPort.__str__ = serialize_host_port


FBBDB_DSN = 'host=fbbdb dbname=fbbdb'

ORACLE_HOST = 'mdb305'

HUSKY_API = HostPort(host='husky-api', port=8081)
YORK = HostPort(host='york', port=80)


def get_docker_config():
    return dict2args(
        config_file.load_config_file(
            config_file.env_to_config_file('docker')))


def fill_docker_context(context):
    context.config = get_docker_config()
    context.husky_api = HUSKY_API

    context.maildb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(context.config.maildb, autocommit=True).connect()
    )
    context.sharddb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(context.config.sharddb, autocommit=True).connect()
    )
    context.huskydb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(context.config.huskydb, autocommit=True).connect()
    )
    context.mlmappingdb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(context.config.mlmappingdb, autocommit=True).connect()
    )
    context.fbbdb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(FBBDB_DSN, autocommit=True).connect()
    )
    context.mongodb_conn = perform_with_retries(
        lambda: pymongo.MongoClient(context.config.mongodb),
        pymongo.errors.AutoReconnect,
    )
    context.setdb_conn = perform_with_retries(
        lambda: mail.pypg.pypg.connect.Connector(context.config.setdb, autocommit=True).connect()
    )

    context.blackbox = 'http://blackbox/blackbox'
    context.hound = 'http://hound:9090'
    context.york = YORK
    context.ora_host = ORACLE_HOST
    context.sharpei = 'http://sharpei:9999'
    context.abook = 'http://abook:5000'


def perform_with_retries(f, error_class=psycopg2.OperationalError):
    last_exception = None
    for _ in range(10):
        try:
            return f()
        except error_class as e:
            last_exception = e
            log.exception(e)
    raise last_exception  # pylint: disable=raising-bad-type
