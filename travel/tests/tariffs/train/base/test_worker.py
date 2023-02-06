# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from travel.rasp.train_api.tariffs.train.base import worker as worker_module


@mock.patch.object(worker_module, 'connection')
def test_worker(m_connection):
    m_connection
    thread_target = mock.Mock()

    worker = worker_module.Worker(target=thread_target)
    worker.start()
    worker.join()

    thread_target.assert_called_once_with()
    m_connection.close.assert_called_once_with()
