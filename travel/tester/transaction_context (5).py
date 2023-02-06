# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import decorator
from django.db import transaction, connections, connection
from django.test import testcases


_transaction_context_started = False


def is_started():
    return _transaction_context_started


def start():
    # FIXME: удалить это после перехода на django1.8
    global _transaction_context_started

    if _transaction_context_started:
        raise Exception('Transaction Context Double Start')

    # reconnect
    for conn in connections.all():
        conn.close()
    connection.cursor()

    transaction.enter_transaction_management()
    transaction.managed(True)

    testcases.disable_transaction_methods()

    _transaction_context_started = True


def stop():
    global _transaction_context_started

    testcases.restore_transaction_methods()

    transaction.rollback()
    transaction.leave_transaction_management()

    for conn in connections.all():
        conn.close()

    _transaction_context_started = False


def savepoint():
    if not is_started():
        is_final = True
        start()
    else:
        is_final = False

    return {
        'sid': transaction.savepoint(),
        'is_final': is_final
    }


def savepoint_rollback(savepoint_info):
    transaction.savepoint_rollback(savepoint_info['sid'])

    if savepoint_info['is_final']:
        stop()


def enter_atomic():
    atomic = transaction.atomic(using='default')
    atomic.__enter__()
    return atomic


def rollback_atomic(atomic):
    transaction.set_rollback(True, using='default')
    atomic.__exit__(None, None, None)


@decorator.decorator
def transaction_fixture(fixture_func, request, *args, **kwargs):
    """
    Такая фикстура плохо дружит с джанговским TestCase. Особенно если scope это class или module.
    """
    def fin():
        rollback_atomic(atomic)

    atomic = enter_atomic()
    try:
        result = fixture_func(request, *args, **kwargs)
    except Exception:
        fin()
        raise
    else:
        request.addfinalizer(fin)
        return result
