# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import celery
import mock

from common.db.switcher import switcher


def test_celery_sync_db():
    with mock.patch.object(switcher, 'sync_with_lazy_reconnect') as m_sync_reconnect:
        @celery.shared_task()
        def some_task():
            m_sync_reconnect.assert_called_once()

        some_task.apply_async()
        m_sync_reconnect.assert_called_once()
