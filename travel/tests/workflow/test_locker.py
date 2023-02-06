# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta

import pytest

from common.test_utils.workflow import create_process_document_locker, DEFAULT_NAMESPACE, LOCK_UPDATE_INTERVAL
from travel.rasp.library.python.common23.date.environment import now_utc
from common.workflow.errors import CantGetLock, CantReleaseLock
from common.workflow.utils import get_by_dotted_path


@pytest.mark.mongouser
class TestDocumentLocker(object):
    def test_build_lock_query(self):
        document_locker = create_process_document_locker()
        assert document_locker.build_lock_query() == {
            '_id': document_locker.document.id,
            DEFAULT_NAMESPACE + '.lock_uid': document_locker.lock_uid,
        }

    def test_is_locked(self):
        document_locker = create_process_document_locker()
        assert not document_locker.is_locked()
        with document_locker():
            assert document_locker.is_locked()
            document_locker._stopped.set()
            document_locker._updater.join()
            assert not document_locker.is_locked()

    def test_get_lock_previously_not_locked(self):
        document_locker = create_process_document_locker()
        with document_locker():
            self.assert_locked(document_locker)

    def test_get_lock_previously_locked_but_freed(self):
        document_locker = create_process_document_locker()
        document_locker.collection.update(
            {'_id': document_locker.document.id},
            {
                '$set': {
                    DEFAULT_NAMESPACE + '.lock_uid': None,
                    DEFAULT_NAMESPACE + '.lock_modified': now_utc()
                }
            }
        )
        with document_locker():
            self.assert_locked(document_locker)

    def test_get_lock_previously_locked_but_lock_outdated(self):
        document_locker = create_process_document_locker()
        outdated_last_modified = now_utc() - timedelta(seconds=document_locker.lock_alive_time + 1)
        document_locker.collection.update(
            {'_id': document_locker.document.id},
            {
                '$set': {
                    DEFAULT_NAMESPACE + '.lock_uid': 'ZagZag!',
                    DEFAULT_NAMESPACE + '.lock_modified': outdated_last_modified
                }
            }
        )
        with document_locker():
            self.assert_locked(document_locker)

    def test_get_lock_currently_locked(self):
        document_locker = create_process_document_locker()
        document_locker.collection.update(
            {'_id': document_locker.document.id},
            {
                '$set': {
                    DEFAULT_NAMESPACE + '.lock_uid': 'ZagZag!',
                    DEFAULT_NAMESPACE + '.lock_modified': now_utc()
                }
            }
        )
        with pytest.raises(CantGetLock):
            with document_locker():
                # Сюда зайти не должен
                assert False

    def test_get_lock_maintaining_lock(self):
        document_locker = create_process_document_locker()
        with document_locker():
            lock_modified = document_locker.lock_modified
            document_locker.update_event.wait(timeout=LOCK_UPDATE_INTERVAL * 10)

            assert document_locker.lock_modified >= lock_modified + timedelta(seconds=LOCK_UPDATE_INTERVAL)
            self.assert_locked(document_locker)

            lock_modified = document_locker.lock_modified
            document_locker.update_event.wait(timeout=LOCK_UPDATE_INTERVAL * 10)

            assert document_locker.lock_modified >= lock_modified + timedelta(seconds=LOCK_UPDATE_INTERVAL)
            self.assert_locked(document_locker)

    def test_get_lock_failed_to_update(self):
        document_locker = create_process_document_locker()
        document_locker.lock_update_interval = 0
        with pytest.raises(CantReleaseLock):
            with document_locker():
                self.assert_locked(document_locker)
                document_locker._stopped.set()
                document_locker._updater.join()
                document_locker._stopped.clear()

                document_locker.collection.update(
                    {'_id': document_locker.document.id},
                    {
                        '$set': {
                            DEFAULT_NAMESPACE + '.lock_uid': 'ZagZag!',
                            DEFAULT_NAMESPACE + '.lock_modified': now_utc()
                        }
                    }
                )
                with pytest.raises(CantGetLock):
                    document_locker._update_lock()

                assert not document_locker.is_locked()

    def test_lock_released(self):
        document_locker = create_process_document_locker()
        with document_locker():
            self.assert_locked(document_locker)
        assert document_locker.is_locked() is False

    @staticmethod
    def assert_locked(document_locker):
        assert document_locker.is_locked()
        document = document_locker.collection.find_one({'_id': document_locker.document.id})
        assert get_by_dotted_path(document, DEFAULT_NAMESPACE + '.lock_uid') == document_locker.lock_uid
