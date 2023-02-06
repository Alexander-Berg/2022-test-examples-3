# -*- coding: utf-8 -*-
from test.parallelly.publication.base import BasePublicationMethods, UserRequest

import urllib2
import copy

import mpfs.engine.process

from test.conftest import collected_data
from mpfs.config import settings
from mpfs.core.address import Address
from mpfs.core import base as core
from mpfs.core.bus import Bus
from mpfs.common import errors
from mpfs.common.static import codes
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.filesystem.quota import Quota

mpfs_db = CollectionRoutedDatabase()


class FileBasicPublicationTestCase(BasePublicationMethods):

    def test_file_set_public(self):
        self.make_file(is_public=False)
        opts = {
            'uid': self.uid,
            'path': '/disk/idle_folder',
        }
        self.json_ok('mkdir', opts)
        current_version = mpfs_db.user_index.find_one({'_id': self.uid})['version']
        opts = {'uid': self.uid, 'path': self.pub_file, 'connection_id': ''}
        self.json_ok('set_public', opts)

        self.assertTrue(collected_data['queue_put']['a'][1]['type'] == 'xiva')
        self.assertTrue(collected_data['queue_put']['a'][1][
                        'data']['xiva_data'][0]['op'] == 'published')
        self.assertEqual(
            int(collected_data['queue_put']['a'][1]['data']['old_version']),
            current_version
        )

        faddr = Address.Make(self.uid, self.pub_file).id
        resource = Bus().resource(self.uid, faddr)
        self.assertEqual(resource.is_fully_public(), True)
        self.assertNotEqual(resource.get_public_hash(), None)

        opts = {
            'uid': self.uid,
            'path': '/disk/idle_folder',
        }
        self.json_ok('rm', opts)

    def test_make_private_file(self):
        self.make_file()
        faddr = Address.Make(self.uid, self.pub_file).id

        request = self.get_request({'uid': self.uid, 'path': faddr, 'connection_id': '', 'return_info': ''})
        core.set_private(request)
        self.assertTrue(
            collected_data['queue_put']['a'][1]['type'] == 'xiva')
        self.assertTrue(collected_data['queue_put']['a'][1][
                        'data']['xiva_data'][0]['op'] == 'unpublished')

        resource = Bus().resource(self.uid, faddr)
        self.assertEqual(resource.is_fully_public(), False)
        self.assertEqual(resource.get_public_hash(), None)

    def test_get_file_info_by_hash(self):
        self.make_file()
        faddr = Address.Make(self.uid, self.pub_file).id
        resource = Bus().resource(self.uid, faddr)
        hash = resource.get_public_hash()

        request = self.get_request({'private_hash': hash, 'connection_id': '',
                                    'tld': settings.user['default_tld'],
                                    'method_name': 'public_info',
                                    'uid': None})
        result = core.public_info(request)['resource']['this']
        self.assertEqual(result['meta']['public_hash'], hash)
        self.assertEqual(result['uid'], self.uid)
        self.assertEqual(result['id'], self.pub_file)

    def test_visual_file_public_info(self):
        self.make_file()
        faddr = Address.Make(self.uid, self.pub_file).id
        resource = Bus().resource(self.uid, faddr)
        hash = resource.get_public_hash()

        opts = {'private_hash': hash, 'meta': 'speed_limited'}
        result = self.json_ok('public_info', opts)
        self.assertEqual(result['resource']['meta']['speed_limited'], 0)
        self.assertTrue('path' not in result['resource'])
        self.assertEqual(result['user']['login'], self.login)
        self.assertEqual(result['user']['locale'], 'ru')
        self.assertEqual(result['user']['paid'], 0)

        # oldfag api test
        result = self.mail_ok('public_info', opts)
        self.assertEqual(result.find('user').find('login').text, self.login)
        self.assertEqual(result.find('file').find('speed_limited').text, '0')

    def test_get_file_link_by_hash(self):
        self.make_file()
        faddr = Address.Make(self.uid, self.pub_file).id

        resource = Bus().resource(self.uid, faddr)
        hash = resource.get_public_hash()

        req = UserRequest({})
        req.set_args(
            {'private_hash': hash, 'connection_id': '', 'inline': '0', 'uid': None, 'check_blockings': True})
        result = core.public_url(req)
        self.assertNotEqual(result['digest'], None)
        self.assertNotEqual(result['file'], None)

        content = urllib2.urlopen(result['file']).read()
        self.assertNotEqual(content, None)

    def test_try_to_get_bad_hash(self):
        req = UserRequest({})
        req.set_args(
            {'private_hash': 'FAKE', 'connection_id': '', 'inline': '0', 'uid': None, 'check_blockings': True})
        self.assertRaises(errors.DecryptionError, core.public_url, req)

    def test_try_to_get_non_public_file(self):
        self.make_file()
        faddr = Address.Make(self.uid, self.pub_file).id

        resource = Bus().resource(self.uid, faddr)
        hash = resource.get_public_hash()

        request = self.get_request({'uid': self.uid, 'path': faddr, 'connection_id': '', 'return_info': ''})
        core.set_private(request)

        request.set_args({'private_hash': hash, 'connection_id': ''})
        self.assertRaises(Exception, core.public_info, request)

    def test_get_file_direct_url(self):
        self.make_file()
        faddr = Address.Make(self.uid, self.pub_file).id
        Bus().rm(self.uid, faddr)
        Bus().mkfile(self.uid, faddr, data=self.file_data)

        request = self.get_request({'uid': self.uid, 'path': faddr, 'connection_id': '',
                                    'tld': settings.user['default_tld']})
        core.set_public(request)

        resource = Bus().resource(self.uid, faddr)
        hash = resource.get_public_hash()

        request.set_args({'modified': None, 'private_hash': hash})
        self.assertTrue(core.public_direct_url(request) is not None)

    def test_get_not_modified_file_direct_url(self):
        self.make_file()
        faddr = Address.Make(self.uid, self.pub_file).id
        Bus().rm(self.uid, faddr)

        Bus().mkfile(self.uid, faddr, data=self.file_data)

        request = self.get_request({'uid': self.uid, 'path': faddr, 'connection_id': '',
                                    'tld': settings.user['default_tld']})
        core.set_public(request)

        resource = Bus().resource(self.uid, faddr)
        hash = resource.get_public_hash()

        request.set_args({'private_hash': hash, 'modified': 'Sat, 29 Oct 3030 19:43:31 GMT'})

        self.assertRaises(
            errors.DocviewerNotModifiedError,
            core.public_direct_url,
            request
        )

    def test_try_to_publicate_viral_file(self):
        faddr = Address.Make(self.uid, self.pub_file).id
        self.make_dir()

        file_data = copy.deepcopy(self.file_data)
        file_data['meta']['drweb'] = 2

        Bus().mkfile(self.uid, faddr, data=file_data)

        request = self.get_request({'uid': self.uid, 'path': faddr, 'connection_id': '',
                                    'tld': settings.user['default_tld']})

        self.assertRaises(
            errors.ViralFilesNotAllowedToPublicate,
            core.set_public,
            request
        )

    def test_download_public_file(self):
        faddr = Address.Make(self.uid, self.pub_file).id
        self.make_file()

        resource = Bus().resource(self.uid, faddr)
        hash = resource.get_public_hash()

        req = UserRequest({})
        req.set_args(
            {'hash': hash,
             'bytes_downloaded': self.file_data['size'],
             'count': 1,
             'uid': self.uid})
        core.kladun_download_counter_inc(req)

        resource = Bus().resource(self.uid, faddr)
        self.assertEqual(resource.meta['download_counter'], 1)

        self.assertTrue(
            collected_data['queue_put']['a'][1]['type'] == 'xiva')
        self.assertTrue(collected_data['queue_put']['a'][1]['data'][
                        'xiva_data'][0]['op'] == 'published_download')

    def test_public_video_url(self):
        self.make_dir(True)
        self.make_file()
        path = '/disk/video_file.mov'
        self.upload_video(self.uid, path)
        opts = {
            'uid': self.uid,
            'path': path,
            'connection_id': '',
            }
        self.json_ok('set_public', opts)
        faddr = Address.Make(self.uid, path).id
        resource = Bus().resource(self.uid, faddr)
        public_hash = resource.get_public_hash()

        opts = {
            'private_hash': public_hash,
            'uid': self.uid,
        }
        result = self.json_ok('public_video_url', opts)
        self.assertTrue('host' in result)
        self.assertTrue('stream' in result)
        self.assertTrue(len(result['stream']) > 10)

    def test_public_video_url_forbidden_for_blocked_user_files(self):
        path = '/disk/video_file.mov'
        self.upload_video(self.uid, path)
        self.json_ok('set_public', {'uid': self.uid, 'path': path})
        public_hash = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})['meta']['public_hash']
        self.support_ok('block_user', {'uid': self.uid, 'moderator': 'moderator', 'comment': 'comment'})
        self.json_error('public_video_url', {'private_hash': public_hash}, code=codes.USER_BLOCKED)

    def test_file_set_public_forbidden_for_overdrawn_user(self):
        Quota().set_limit(1000000, uid=self.uid)
        path = '/disk/test_file.txt'
        self.upload_file(self.uid, path, file_data={'size': 500000})
        Quota().set_limit(500000, uid=self.uid)
        self.json_error('set_public', {'uid': self.uid, 'path': path}, code=codes.USER_OVERDRAWN)
