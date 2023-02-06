# -*- coding: utf-8 -*-
import re
import copy
import mock

from lxml import etree
from nose_parameterized import parameterized

from test.helpers.stubs.resources.users_info import update_info_by_uid
from test.parallelly.publication.base import BasePublicationMethods, UserRequest
from test.conftest import collected_data
from test.base_suit import set_up_open_url, tear_down_open_url

import mpfs.engine.process

from mpfs.common.static import codes
from mpfs.core.address import Address, PublicAddress
from mpfs.core import base as core
from mpfs.core.bus import Bus
from mpfs.core.filesystem.quota import Quota
from mpfs.metastorage.mongo.util import decompress_data
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


mpfs_db = CollectionRoutedDatabase()


class FolderBasicPublicationTestCase(BasePublicationMethods):

    def test_publicate_folder(self):
        self.make_dir()
        foldaddr = Address.Make(self.uid, self.pub_folder).id
        request = self.get_request({'uid': self.uid, 'path': foldaddr, 'connection_id': '',
                                    'tld': 'ru'})
        core.set_public(request)

        self.assertTrue(
            collected_data['queue_put']['a'][1]['type'] == 'xiva')
        self.assertTrue(collected_data['queue_put']['a'][1][
                        'data']['xiva_data'][0]['op'] == 'published')

        resource = Bus().resource(self.uid, foldaddr)
        self.assertEqual(resource.is_fully_public(), True)
        self.assertNotEqual(resource.get_public_hash(), None)

        pub_dict = mpfs_db.user_data.find_one({'uid': str(self.uid), 'path': '/disk/pub'})
        self.assertNotEqual(pub_dict, None)
        self.assertEqual(pub_dict['data']['public'], 1)
        spec = {
            'uid': str(self.uid),
            'path': Address.Make(self.uid, self.pub_folder).path,
            }
        zdata = decompress_data(mpfs_db.user_data.find_one(spec)['zdata'])
        self.assertEqual(zdata.get('setprop', {}).get('folder_url'), None)

    def test_visual_folder_public_info(self):
        faddr = Address.Make(self.uid, self.pub_folder).id
        self.make_dir(True)
        resource = Bus().resource(self.uid, faddr)
        hash_ = resource.get_public_hash()

        opts = {'private_hash': hash_, 'meta': 'speed_limited'}
        result = self.json_ok('public_info', opts)
        self.assertEqual(result['resource']['meta']['speed_limited'], 0)
        self.assertEqual(result['user']['login'], self.login)

    @parameterized.expand([
        ('public_info',),
        ('public_list',),
    ])
    def test_getting_proper_preview_links_for_quick_move(self, endpoint):
        faddr = Address.Make(self.uid, self.pub_folder).id
        self.make_dir(True)
        resource = Bus().resource(self.uid, faddr)
        hash_ = resource.get_public_hash()

        self.upload_file(self.uid, self.pub_file)

        opts = {'private_hash': hash_, 'meta': ''}
        with mock.patch('mpfs.core.filesystem.base.is_quick_move_enabled', return_value=True):
            quick_move_result = self.json_ok(endpoint, opts)
        with mock.patch('mpfs.core.filesystem.base.is_quick_move_enabled', return_value=False):
            slow_move_result = self.json_ok(endpoint, opts)

        assert quick_move_result == slow_move_result

    def test_private_folder(self):
        self.make_dir(True)
        self.make_file()

        addr = Address.Make(self.uid, '/disk/pub/' + self.pub_filename).id
        resource = Bus().resource(self.uid, addr)
        hash = resource.get_public_hash()
        req = UserRequest({})
        req.set_args(
            {'hash': hash,
             'bytes_downloaded': self.file_data['size'],
             'count': 1,
             'uid': self.uid})
        core.kladun_download_counter_inc(req)

        file_info = mpfs_db.user_data.find_one(
            {'uid': self.uid, 'path': '/disk/pub/' + self.pub_filename})
        zdata = decompress_data(file_info['zdata'])
        self.assertEqual(file_info['data']['download_counter'], 1)

        foldaddr = Address.Make(self.uid, self.pub_folder).id
        opts = {'uid': self.uid, 'path': foldaddr, 'connection_id': ''}
        self.json_ok('set_private', opts)
        '''
        xiva_notified = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search(
                        'http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    self.assertEqual(uid, self.uid)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'diff' and data.find('op').get('type') == 'unpublished':
                        xiva_notified = True
        self.assertTrue(xiva_notified)
        '''

        resource = Bus().resource(self.uid, foldaddr)
        self.assertEqual(resource.is_fully_public(), False)
        self.assertEqual(resource.get_public_hash(), None)
        file_info = mpfs_db.user_data.find_one(
            {'uid': self.uid, 'path': '/disk/pub/' + self.pub_filename})
        zdata = decompress_data(file_info['zdata'])
        self.assertEqual(file_info['data']['download_counter'], 1)
        self.assertFalse('download_counter' in zdata['pub'])

    def test_get_folder_info_by_hash(self):
        self.make_dir(True)
        self.make_file()
        foldaddr = Address.Make(self.uid, self.pub_folder).id
        resource = Bus().resource(self.uid, foldaddr)
        hash = resource.get_public_hash()

        req = UserRequest({})
        req.set_args({'private_hash': hash, 'connection_id': '', 'uid': None})
        result = core.public_info(req)['resource']['this']
        self.assertEqual(result['meta']['public_hash'], hash)
        self.assertEqual(result['uid'], self.uid)
        self.assertEqual(result['id'] + '/', self.pub_folder)

    def test_get_file_in_folder_url(self):
        self.make_dir(True)
        self.make_file()
        foldaddr = Address.Make(self.uid, self.pub_folder).id
        subfolderaddr = Address.Make(self.uid, self.pub_subfolder).id
        subfileaddr = Address.Make(self.uid, self.pub_subfile).id

        Bus().mkdir(self.uid, subfolderaddr, data=self.file_data)
        Bus().mkfile(self.uid, subfileaddr, data=self.file_data)

        resource = Bus().resource(self.uid, foldaddr)
        hash = resource.get_public_hash()
        public_addr = PublicAddress.Make(hash, self.relative_subfile_path)

        req = UserRequest({})
        req.set_args(
            {'modified': None,  'private_hash': public_addr.id, 'inline': '0', 'uid': None, 'check_blockings': True})
        self.assertTrue(core.public_url(req) is not None)

    # https://jira.yandex-team.ru/browse/CHEMODAN-9515
    def test_get_file_in_folder_info(self):
        self.make_dir(True)
        self.make_file()
        foldaddr = Address.Make(self.uid, self.pub_folder).id
        resource = Bus().resource(self.uid, foldaddr)
        hash = resource.get_public_hash()
        public_addr = PublicAddress.Make(hash, self.relative_subfile_path).id

        subfolderaddr = Address.Make(self.uid, self.pub_subfolder).id
        Bus().mkdir(self.uid, subfolderaddr, data=self.file_data)
        subfileaddr = Address.Make(self.uid, self.pub_subfile).id
        Bus().mkfile(self.uid, subfileaddr, data=self.file_data)

        opts = {'private_hash': public_addr}
        result = self.json_ok('public_info', opts)

        self.assertEqual(result['resource']['type'], 'file')
        self.assertEqual(result['resource']['name'], self.pub_subfilename)

    def test_public_tree(self):
        self.make_dir(True)
        subfolderaddr = Address.Make(self.uid, self.pub_subfolder).id
        Bus().mkdir(self.uid, subfolderaddr, data=self.file_data)
        subfileaddr = Address.Make(self.uid, self.pub_subfile).id
        Bus().mkfile(self.uid, subfileaddr, data=self.file_data)

        foldaddr = Address.Make(self.uid, self.pub_folder).id
        resource = Bus().resource(self.uid, foldaddr)
        hash = resource.get_public_hash()

        req = UserRequest({})
        req.set_args({'private_hash': hash, 'deep_level': 0, 'uid': None})
        result = core.public_fulltree(req)
        self.assertEqual(
            result['list'][0]['this'].id,
            PublicAddress.Make(hash, '/subfolder').id
        )
        return result

    def test_public_content(self):
        self.make_dir(True)
        subfolderaddr = Address.Make(self.uid, self.pub_subfolder).id
        Bus().mkdir(self.uid, subfolderaddr, data=self.file_data)
        foldaddr = Address.Make(self.uid, self.pub_folder).id
        resource = Bus().resource(self.uid, foldaddr)
        hash = resource.get_public_hash()

        req = UserRequest({})
        req.set_args({'private_hash': hash, 'meta': None, 'uid': None})
        result = core.public_content(req)

        self.assertEqual(
            result[0]['id'],
            PublicAddress.Make(hash, '/subfolder').id
        )
        return result

    def test_visual_public_tree(self):
        self.make_dir(True)
        subfolderaddr = Address.Make(self.uid, self.pub_subfolder).id
        Bus().mkdir(self.uid, subfolderaddr, data=self.file_data)
        self.upload_file(self.uid, '/disk/pub/subfolder/file.txt')

        opts = {'uid': self.uid, 'path': '/disk/pub'}
        result = self.json_ok('set_public', opts)
        self.assertTrue(result.get('hash') is not None)
        hash = result.get('hash')

        opts = {'private_hash': hash, 'meta': 'md5,mediatype'}
        web_result = self.json_ok('public_fulltree', opts)

        self.assertTrue(
            web_result['list'][0]['list'][0]['this']['meta']['mediatype'] in
            ['document', 'development', 'text'], web_result[
                'list'][0]['list'][0]['this']['meta']['mediatype']
        )

        opts = {'private_hash': hash, 'meta': 'file_mid'}
        dv_result = self.json_ok('public_fulltree', opts)

        self.assertTrue(
            dv_result['list'][0]['list'][0]['this'][
                'meta']['file_mid'] is not None
        )

    def test_public_fulltree_relative_hash(self):
        """Протестировать что ручка `public_fulltree` возвращает листинг относительного пути
        если передан относительный, за публичным хешом.

        Проблема проявилась при формировании контента папки в Заберуне.
        При относительном хеше получали контент всей папки от корня.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/Public'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/Public/Inner'})
        self.upload_file(uid=self.uid, path='/disk/Public/Inner/test.txt')

        response = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/Public'})
        assert 'hash' in response

        private_hash = response['hash']
        response = self.json_ok('public_fulltree', {'uid': self.uid, 'private_hash': private_hash + ':/Inner'})
        assert response['this']['name'] == 'Inner'
        assert response['this']['path'] == '/'

    def test_visual_public_content(self):
        self.make_dir(True)
        subfolderaddr = Address.Make(self.uid, self.pub_subfolder).id

        file_data = copy.deepcopy(self.file_data)
        Bus().mkdir(self.uid, subfolderaddr, data=file_data)
        foldaddr = Address.Make(self.uid, self.pub_folder).id
        Bus().mkdir(self.uid, foldaddr + 'subfolder/zzz')

        file_data = copy.deepcopy(self.file_data)
        local_file = Address.Make(self.uid, self.pub_folder + 'subfolder/aaa.txt').id
        Bus().mkfile(self.uid, local_file, data=file_data)

        file_data = copy.deepcopy(self.file_data)
        local_file = Address.Make(self.uid, self.pub_folder + 'subfolder/zzz.txt').id
        Bus().mkfile(self.uid, local_file, data=file_data)

        opts = {'uid': self.uid, 'path': self.pub_folder}
        result = self.json_ok('set_public', opts)
        self.assertTrue(result.get('hash') is not None)
        hash = result.get('hash')

        pub_address = PublicAddress.Make(hash, '/subfolder')

        opts = {'private_hash': pub_address.id, 'meta': 'md5,mediatype,drweb'}
        web_result = self.json_ok('public_list', opts)

        self.assertEqual(len(web_result), 4)
        self.assertEqual(web_result[1]['path'],
                         PublicAddress.Make(hash, '/subfolder/zzz').id)
        self.assertEqual(web_result[1]['id'],
                         PublicAddress.Make(hash, '/subfolder/zzz/').id)

        self.assertEqual(web_result[2]['meta']['mediatype'], 'document')
        self.assertEqual(web_result[2]['name'], 'aaa.txt')
        self.assertEqual(web_result[3]['name'], 'zzz.txt')

    # https://jira.yandex-team.ru/browse/CHEMODAN-9619
    def test_visual_public_content_with_offset_amount(self):
        self.make_dir(True)
        subfolderaddr = Address.Make(self.uid, self.pub_subfolder).id
        Bus().mkdir(self.uid, subfolderaddr, data=self.file_data)
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.pub_subfolder + '1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.pub_subfolder + '2'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.pub_subfolder + '3'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.pub_subfolder + '4'})

        opts = {'uid': self.uid, 'path': self.pub_folder}
        result = self.json_ok('set_public', opts)
        pub_address = PublicAddress.Make(result.get('hash'), '/subfolder')

        opts = {
            'private_hash': pub_address.id,
            'meta': 'md5,mediatype,drweb',
            'amount': 2,
        }
        web_result_1 = self.json_ok('public_list', opts)
        self.assertEqual(len(web_result_1), 3)
        self.assertEqual(web_result_1[1]['name'], '1')

        opts = {
            'private_hash': pub_address.id,
            'meta': 'md5,mediatype,drweb',
            'amount': 2,
            'offset': 2,
        }
        web_result_2 = self.json_ok('public_list', opts)
        self.assertEqual(len(web_result_2), 3)
        self.assertEqual(web_result_2[1]['name'], '3')

    # https://jira.yandex-team.ru/browse/CHEMODAN-9477
    def test_visual_tree_for_web(self):
        self.make_dir(True)
        opts = {'uid': self.uid, 'path': '/disk', 'meta': 'public'}
        result = self.json_ok('tree', opts)

        for res in result['resource'][0]['resource']:
            if res['id'] == self.pub_folder:
                if 'public' not in res['meta']:
                    self.fail(res)
                self.assertTrue('public' in res['meta'])
                self.assertEqual(res['meta']['public'], 1)

    def test_download_file_from_public_folder(self):
        self.make_dir(True)
        subfolderaddr = Address.Make(self.uid, self.pub_subfolder).id
        Bus().mkdir(self.uid, subfolderaddr, data=self.file_data)
        self.upload_file(self.uid, '/disk/pub/subfolder/file.txt')
        opts = {'uid': self.uid, 'path': '/disk/pub'}
        result = self.json_ok('set_public', opts)
        self.assertTrue(result.get('hash') is not None)
        hash = result.get('hash')
        pub_address = PublicAddress.Make(hash, '/subfolder/file.txt')
        traffic_before = Quota().download_traffic(self.uid)

        req = UserRequest({})
        req.set_args({'uid': self.uid, 'hash': pub_address.id,
                      'bytes_downloaded': self.file_data['size'], 'count': 1})
        core.kladun_download_counter_inc(req)

        traffic_after = Quota().download_traffic(self.uid)
        self.assertEqual(traffic_before + self.file_data['size'], traffic_after)

        resource = Bus().resource(
            self.uid, Address.Make(self.uid, '/disk/pub/subfolder/file.txt').id)
        self.assertEqual(resource.meta['download_counter'], 1)

        self.assertTrue(
            collected_data['queue_put']['a'][1]['type'] == 'xiva')
        self.assertTrue(collected_data['queue_put']['a'][1]['data'][
                        'xiva_data'][0]['op'] == 'published_download')

    def test_download_public_folder(self):
        self.make_dir(True)
        subfolderaddr = Address.Make(self.uid, self.pub_subfolder).id
        Bus().mkdir(self.uid, subfolderaddr, data=self.file_data)
        opts = {'uid': self.uid, 'path': '/disk/pub'}
        result = self.json_ok('set_public', opts)
        self.assertTrue(result.get('hash') is not None)
        hash = result.get('hash')
        traffic_before = Quota().download_traffic(self.uid)

        req = UserRequest({})
        req.set_args(
            {'uid': self.uid, 'hash': hash,
             'bytes_downloaded': 1000000000000, 'count': 1})
        core.kladun_download_counter_inc(req)

        traffic_after = Quota().download_traffic(self.uid)
        self.assertEqual(traffic_before + 1000000000000, traffic_after)

        resource = Bus().resource(self.uid, Address.Make(self.uid, '/disk/pub').id)
        self.assertEqual(resource.meta['download_counter'], 1)
        self.assertTrue(
            collected_data['queue_put']['a'][1]['type'] == 'xiva')
        self.assertTrue(collected_data['queue_put']['a'][1]['data'][
                        'xiva_data'][0]['op'] == 'published_download')

        opts = {'private_hash': hash, 'meta': 'speed_limited'}
        with mock.patch('mpfs.core.filesystem.quota.FEATURE_TOGGLES_SPEED_LIMIT', True):
            result = self.json_ok('public_info', opts)
        self.assertEqual(result['resource']['meta']['speed_limited'], 1)

    def test_timeline_with_public_elements(self):
        self.make_dir(True)
        self.upload_file(self.uid, '/disk/public_file.txt')
        opts = {
            'uid': self.uid,
            'path': '/disk/public_file.txt',
            }
        self.json_ok('set_public', opts)

        opts = {'uid': self.uid, 'path': '/disk/pub/subpub'}
        self.json_ok('mkdir', opts)
        self.json_ok('set_public', opts)

        opts = {'uid': self.uid, 'path': '/disk/pub/'}
        self.json_ok('share_create_group', opts)

        self.assertNotEqual(
            mpfs_db.link_data.find({'uid': self.uid}).count(), 0)
        opts = {
            'amount': '40',
            'meta': 'public,group',
            'offset': '0',
            'order': '0',
            'path': '/disk',
            'public': '1',
            'sort': 'ctime',
            'uid': self.uid,
            'visible': '1',
            }
        result = self.json_ok('timeline', opts)

        self.assertEqual(len(result), 4)
        for item in result:
            if item['name'] == 'subpub':
                self.assertEqual(item['id'], '/disk/pub/subpub/')
                self.assertTrue('group' in item['meta'])

    def test_public_fulltree_for_blocked_by_passport_account(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/test'})
        public_hash = self.json_ok('info', {'uid': self.uid, 'path': '/disk/test', 'meta': ''})['meta']['public_hash']

        update_info_by_uid(self.uid, is_enabled=False)

        self.json_error('public_fulltree', {'private_hash': public_hash}, code=codes.RESOURCE_NOT_FOUND)
