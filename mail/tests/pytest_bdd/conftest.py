import pytest
from pymdb.replication import Replica
from pymdb.helpers import parse_values
from tools.hamlet import Hamlet
from yatest.common import network

try:
    from collections import OrderedDict
except ImportError:
    from ordereddict import OrderedDict

import pymdb.operations
import mail.pypg.pypg.connect
from pymdb.types import register_types
from tests_common.coordinator_context import dict2args
from tests_common.holders import (
    UsersHolder,
)
import tests_common.pytest_bdd

from mail.devpack.lib.coordinator import Coordinator
from mail.devpack.lib.components.mdb import Mdb
from mail.devpack.tests.helpers.env import TestEnv

from . import step_types_defs  # noqa
from .steps import *  # noqa

import logging

log = logging.getLogger(__name__)


@pytest.fixture(scope="session", autouse=True)
def context():
    return tests_common.pytest_bdd.context


@pytest.fixture(scope="session", autouse=True)
def feature_setup(request, context):
    before_all(context)

    def feature_teardown():
        after_all(context)

    request.addfinalizer(feature_teardown)


@pytest.fixture(scope="function", autouse=True)
def step_setup(request, context):
    context.request = request


def get_param(context, name):
    import os
    return name.upper() in os.environ


REAL_PREFIX = 'r_'


def pretty_broken_violation(violation):
    broken = violation['broken']
    all_real_keys = [k for k in broken.keys() if k.startswith(REAL_PREFIX)]
    diff = []
    for real_key in all_real_keys:
        counter_key = real_key[len(REAL_PREFIX):]
        real_value = broken[real_key]
        counter_value = broken.get(counter_key)
        if counter_value != real_value:
            diff.append(
                '{name}.{counter_name} '
                '<real: {real_value} counter: {counter_value}>'.format(
                    name=violation.get('name'),
                    counter_name=counter_key,
                    real_value=real_value,
                    counter_value=counter_value))
    return '\n'.join(diff)


def fill_coordinator_context(context):
    start_component = Mdb
    port_manager = network.PortManager()
    context.coordinator = Coordinator(TestEnv(port_manager, start_component), start_component)
    context.coordinator.start()
    context.started_components = [start_component]

    components = context.coordinator.components

    context.config = dict2args({
        'maildb_dsn_suffix': 'user=root connect_timeout=5',
        'maildb': components[Mdb].dsn(),
    })
    context.maildb_connector = mail.pypg.pypg.connect.Connector(context.config.maildb)
    context.conn = mail.pypg.pypg.connect.Connector(context.config.maildb).connect()
    context.maildb = components[Mdb]

    if not get_param(context, 'skip-replication'):
        context.replica = Replica(context.maildb.dsn())

    if not get_param(context, 'skip-violations'):
        def find_violations(op):
            if context.scenario_check_violations and \
                    not getattr(op, 'violating', False):
                # with unlogged(op.conn):
                qs = pymdb.queries.Queries(op.conn, op.uid)
                violations = qs.violations()
                formatted = [
                    pretty_broken_violation(v) + ' ' + repr(v)
                    for v in violations
                ]
                assert not violations, \
                    'Find violations: {0}, after operations: {1}'.format(
                        '\n'.join(formatted), op
                    )
        pymdb.operations.UserOperation.add_callback(find_violations)


def is_user_op_class(OpClass):
    return issubclass(OpClass, pymdb.operations.UserOperation)


def is_contacts_user_op_class(OpClass):
    return issubclass(OpClass, pymdb.operations.ContactsUserOperation)


class NewAsyncConnections(object):
    def __init__(self, connector):
        self._conns = []
        self._connector = connector

    def __call__(self):
        conn = self._connector.connect(async_=1)
        self._conns.append(conn)
        return conn

    def __len__(self):
        return len(self._conns)

    @property
    def last(self):
        return self._conns[-1]

    def close_all(self):
        while self._conns:
            self._conns.pop().close()


def install_override(conn):
    import library.python.resource as rs
    log.info('Looking for overrides')
    for path, sql_text in rs.iteritems(prefix='resfs/file/mail/pg/mdb/tests/override'):
        log.info('Applying override from %s', path)
        cur = conn.cursor()
        cur.execute(sql_text)
        conn.wait()
    conn.commit()


def before_all(context):
    def remember_all_operations(op):
        if op not in context.operations.values():
            context.operations.append(op)

    def make_operation_object(OpClass, uid=None, conn=None, user_id=None, user_type=None):
        if is_user_op_class(OpClass):
            return OpClass(conn, uid or context.uid, getattr(context, 'request_info', None))
        elif is_contacts_user_op_class(OpClass):
            return OpClass(
                conn,
                user_id=user_id or context.user_id,
                user_type=user_type,
                request_info=getattr(context, 'request_info', None),
            )
        return OpClass(conn)

    def make_operation(OpClass, uid=None, user_id=None, user_type=None):
        return make_operation_object(OpClass, uid=uid, user_id=user_id, user_type=user_type, conn=context.conn)

    def make_async_operation(OpClass, uid=None, user_id=None, user_type=None):
        log.debug('Make %r', OpClass)
        if len(context.async_connect):
            log.debug(
                'We create next async_operation, wait for previous %r',
                context.async_connect.last
            )
            context.async_connect.last.wait()
            log.debug('Wait ended successfully')
        return make_operation_object(OpClass, uid=uid, conn=context.async_connect(),
                                     user_id=user_id, user_type=user_type)

    def apply_op(OpClass, **op_kwargs):
        op_args = [context.conn]
        if is_user_op_class(OpClass):
            uid = op_kwargs.pop('uid', context.uid)
            op_args += [uid, getattr(context, 'request_info', None)]
        elif is_contacts_user_op_class(OpClass):
            user_id = op_kwargs.pop('user_id', context.user_id)
            user_type = op_kwargs.pop('user_type', context.user_type)
            op_args += [user_id, user_type, getattr(context, 'request_info', None)]
        op = OpClass(*op_args)
        op(**op_kwargs)
        op.commit()
        return op

    fill_coordinator_context(context)

    install_override(context.conn)
    register_types(context.conn)

    pymdb.operations.BaseOperation.add_callback(remember_all_operations)

    context.hamlet = Hamlet()
    context.users = UsersHolder()
    context.make_operation = make_operation
    context.make_async_operation = make_async_operation
    context.async_connect = NewAsyncConnections(context.maildb_connector)
    context.apply_op = apply_op


def after_all(context):
    for c in context.started_components:
        context.coordinator.stop(c)


class FrozenMap(OrderedDict):
    def __getitem__(self, k):
        if k not in self:
            raise RuntimeError(
                "Can't find {k} in {self}".format(**locals())
            )
        return super(FrozenMap, self).__getitem__(k)

    def get(self, k, d=None):
        return self[k]

    def __setitem__(self, key, value, *args, **kwargs):
        if key in self:
            prev_val = self[key]
            raise RuntimeError(
                "{0} already used by {1}, {2}".format(
                    key, prev_val, self
                )
            )
        return super(FrozenMap, self).__setitem__(key, value, *args, **kwargs)


class Operations(FrozenMap):
    def last(self):
        last_key = next(reversed(self))
        return self[last_key]

    def wait_some(self, tm):
        if self:
            next(iter(self)).wait_some(tm)

    def append(self, op):
        # Ugly hack - use int as keys for unnamed operations
        new_key = len(self) + 1
        self[new_key] = op


class ResultsMap(FrozenMap):
    def get_mid(self, k):
        if k.isdigit():
            return int(k)
        return self[k].mid

    def get_mids(self, mids_str):
        mids = mids_str
        if isinstance(mids, str):
            mids = parse_values(mids_str)

        return [self.get_mid(m) for m in mids]


def pytest_bdd_before_scenario(request, feature, scenario):
    log.debug('Started scenario %s', scenario.name)
    context = tests_common.pytest_bdd.context
    context.operations = Operations()
    context.res = ResultsMap()
    context.users = {}
    context.scenario_check_violations = 'no-check-violations' not in scenario.tags
    context.set_example_params(scenario)
    context.feature_name = feature.name


def pytest_bdd_after_scenario(request, feature, scenario):
    log.debug('Finished scenario %s', scenario.name)
    context = tests_common.pytest_bdd.context
    context.conn.rollback()
    context.async_connect.close_all()
    context.operations = Operations()
    context.clear_scenario()
    if 'replica' in context:
        context.replica.stop_replication()


def pytest_bdd_before_step_call(request, feature, scenario, step, step_func, step_func_args):
    log.debug('Matched on step [%s]', step.name)


def pytest_bdd_after_step(request, feature, scenario, step, step_func, step_func_args):
    log.debug('Finished step [%s]', step.name)
    tests_common.pytest_bdd.context.clear_step()
