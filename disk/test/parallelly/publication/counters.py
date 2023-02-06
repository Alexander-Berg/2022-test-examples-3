# -*- coding: utf-8 -*-
from hamcrest import assert_that, has_entry

from test.parallelly.publication.base import BasePublicationMethods, UserRequest

import mpfs.engine.process

from mock import patch

from test.conftest import collected_data
from mpfs.core.address import Address, PublicAddress
from mpfs.core import base as core
from mpfs.core.bus import Bus


class CounterPublicationTestCase(BasePublicationMethods):

    def test_update_download_counter(self):
        self.make_dir(True)
        uid = str(self.uid)
        opts = {
            'uid': uid,
            'path': '/disk/pub',
            'meta': ''
            }
        folder_info = self.json_ok('info', opts)
        public_hash = folder_info['meta']['public_hash']
        public_addr = PublicAddress.Make(public_hash, '/subfolder/file.txt')
        opts = {
            'hash': public_addr.id,
            'bytes_downloaded': 100,
            }
        self.service('kladun_download_counter_inc', opts)

    def test_kladun_downloader_counter_inc_with_utf8(self):
        """
        https://jira.yandex-team.ru/browse/CHEMODAN-10144
        """
        ru_name = u'русский+текст.жпг'
        self.make_dir(True)
        faddr = Address.Make(self.uid, self.pub_folder).id
        resource = Bus().resource(self.uid, faddr)
        hash = resource.get_public_hash()
        self.upload_file(self.uid, self.pub_folder + ru_name)

        opts = {
            'hash': '%s:/%s' % (hash, ru_name),
            'bytes': '5927316'
        }

        result = self.service('kladun_download_counter_inc', opts)
        self.assertFalse(result)

    def test_kladun_downloader_counter_with_count(self):
        """
        https://jira.yandex-team.ru/browse/CHEMODAN-10270
        """
        self.make_dir(True)
        self.make_file()
        faddr = Address.Make(self.uid, self.pub_file).id
        resource = Bus().resource(self.uid, faddr)
        hash = resource.get_public_hash()

        req = UserRequest({})
        req.set_args(
            {'uid': self.uid, 'hash': hash,
             'bytes_downloaded': self.file_data['size'], 'count': 10})
        with patch('mpfs.engine.queue2.celery.BaseTask.apply_async') as mocked_put_task_to_queue:
            core.kladun_download_counter_inc(req)

        resource = Bus().resource(self.uid, faddr)
        self.assertEqual(resource.meta['download_counter'], 10)

        self.assertTrue(
            collected_data['queue_put']['a'][1]['type'] == 'xiva')
        assert_that(mocked_put_task_to_queue.call_args.kwargs, has_entry('queue', 'secondary_submit'))
        self.assertTrue(collected_data['queue_put']['a'][1]['data'][
                        'xiva_data'][0]['op'] == 'published_download')

    @patch.dict('mpfs.config.settings.push', {'disable_for': {'published_download': True}})
    def test_disable_push_notification(self):
        self.make_dir(True)
        self.make_file()
        faddr = Address.Make(self.uid, self.pub_file).id
        resource = Bus().resource(self.uid, faddr)
        hash = resource.get_public_hash()

        req = UserRequest({})
        req.set_args(
            {'uid': self.uid, 'hash': hash,
             'bytes_downloaded': self.file_data['size'], 'count': 10})
        core.kladun_download_counter_inc(req)

        # пуш есть (про создание файлика)
        self.assertTrue(
            collected_data['queue_put']['a'][1]['type'] == 'xiva')
        # но не отключенный
        assert collected_data['queue_put']['a'][1]['data']['xiva_data'][0]['op'] != 'published_download'

    def test_increment_attach_counter(self):
        self.upload_file(self.uid, '/attach/file_1.txt')
        opts = {
            'uid': self.uid,
            'path': '/attach',
            }
        attach_listing = self.json_ok('list', opts)
        file_path = attach_listing[1]['id']
        opts = {
            'uid': self.uid,
            'path': file_path,
            }
        self.json_ok('set_public', opts)
        opts = {
            'uid': self.uid,
            'path': file_path,
            'meta': '',
            }
        attach_info = self.json_ok('info', opts)
        self.assertTrue('public_hash' in attach_info['meta'])
        public_hash = attach_info['meta']['public_hash']
        req = UserRequest({})
        req.set_args(
            {'uid': self.uid, 'hash': public_hash,
             'bytes_downloaded': self.file_data['size'], 'count': 1})
        core.kladun_download_counter_inc(req)

        faddr = Address.Make(self.uid, file_path).id
        resource = Bus().resource(self.uid, faddr)
        self.assertEqual(resource.meta['download_counter'], 1)
        core.kladun_download_counter_inc(req)
        resource = Bus().resource(self.uid, faddr)
        self.assertEqual(resource.meta['download_counter'], 2)
