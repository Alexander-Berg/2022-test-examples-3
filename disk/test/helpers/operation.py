# -*- coding: utf-8 -*-
import mock
from test.helpers.stubs.base import BaseStub
from mpfs.config import settings
from mpfs.core.operations.base import Operation
from mpfs.core.job_handlers.operation import handle_operation


class PendingOperationDisabler(BaseStub):
    """Позволяет отключить выполнение pended операции и выполнять её руками в нужный момент.

    Можно использовать как декоратор и контекст менеджер.

    with PendingOperationDisabler(CopyOnDisk):
        pended_operation = self.json_ok('async_copy', ...)
        # operation in WAITING state
        # do smth
        PendingOperationDisabler.process(uid, oid)
        # operation in DONE state
    """
    def __init__(self, operation_cls):
        self._operation_cls = operation_cls
        if not issubclass(operation_cls, Operation):
            raise TypeError("Operation class expected. Got: %r" % operation_cls)
        if not operation_cls.is_pended():
            raise ValueError('Operation %s isn\'t pended' % operation_cls)
        self._config_patch = None

    def start(self):
        self._config_patch = mock.patch.object(self._operation_cls, 'is_pended', return_value=False)
        self._config_patch.start()

    def stop(self):
        self._config_patch.stop()

    @staticmethod
    def process(uid, oid):
        """Запустить обработку pended операции"""
        handle_operation(uid, oid)

