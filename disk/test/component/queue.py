# -*- coding: utf-8 -*-
from hamcrest import assert_that, has_entry

from test.parallelly.publication.base import BasePublicationMethods, UserRequest

import mpfs.engine.process

from mock import patch

from mpfs.core.address import Address
from mpfs.core import base as core
from mpfs.core.bus import Bus


class Queue(BasePublicationMethods):
    def test_resubmit_push_task_to_secondary_submit(self):
        self.make_dir(True)
        self.make_file()
        faddr = Address.Make(self.uid, self.pub_file).id
        resource = Bus().resource(self.uid, faddr)
        hash = resource.get_public_hash()

        req = UserRequest({})
        req.set_args(
            {'uid': self.uid, 'hash': hash,
             'bytes_downloaded': self.file_data['size'], 'count': 1})
        with patch('mpfs.engine.queue2.celery.get_current_worker_name', return_value='homyak-rabotyaga'), \
             patch('mpfs.core.user.common.CommonUser.check_blocked',
                   side_effect=[KeyError, 7]), \
             patch('mpfs.engine.queue2.celery.BaseTask.retry') as mocked_retry_task_to_queue:
            core.kladun_download_counter_inc(req)

        assert_that(mocked_retry_task_to_queue.call_args.kwargs, has_entry('queue', 'secondary_submit'),
                    u'resubmit при ошибке тоже должен быть в secondary_submit')
