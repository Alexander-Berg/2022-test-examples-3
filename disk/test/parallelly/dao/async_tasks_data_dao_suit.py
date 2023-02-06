# -*- coding: utf-8 -*-
from test.base import DiskTestCase
from mpfs.engine.queue2.async_tasks.models import AsyncTasksData
from mpfs.engine.queue2.async_tasks.controllers import AsyncTasksDataController


class AsyncTasksDataTestCase(DiskTestCase):
    def test_save(self):
        async_task_data = AsyncTasksData(uid=self.uid, data='compressed_data')
        async_task_data.save()

        async_task_data = AsyncTasksDataController().get(uid=async_task_data.uid, id=async_task_data.id)
        assert async_task_data

        async_task_data.delete()
        async_task_data = AsyncTasksDataController().get(uid=async_task_data.uid, id=async_task_data.id)
        assert async_task_data is None
