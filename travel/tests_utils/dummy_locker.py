# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import contextlib

from common.workflow.errors import CantGetLock
from common.workflow.locker import DocumentLocker


nothing = object()


class DummyDocumentLocker(DocumentLocker):
    def __init__(self, document, namespace, lock_alive_time=20000, lock_update_interval=0):
        # document attributes
        self.document = document
        self.namespace = namespace

        # lock attributes
        self.lock_uid = 'dummy_lock'
        self.lock_alive_time = lock_alive_time
        self.lock_update_interval = lock_update_interval
        self._lock_modified = self.store and self.store.get('lock_modified')

        self.lock_uid_key = '{}.lock_uid'.format(self.namespace)
        self.lock_modified_key = '{}.lock_modified'.format(self.namespace)
        # позволяет отслеживать, когда происходит обновление блокировки

        self._is_lock_acquired = False

    @property
    def collection(self):
        return self.document.__class__._get_collection()

    @contextlib.contextmanager
    def __call__(self):
        if not self._acquire_lock():
            raise CantGetLock("Can't get lock {} for document {}".format(self.lock_uid, self.document.id))

        try:
            yield
        finally:
            self._release_lock()

    def build_lock_query(self):
        return {
            '_id': self.document.id
        }

    def is_locked(self):
        return True
