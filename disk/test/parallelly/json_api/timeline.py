# -*- coding: utf-8 -*-
import time
import itertools

from test.parallelly.json_api.base import CommonJsonApiTestCase
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase

import mpfs.engine.process

db = CollectionRoutedDatabase()


class AmountOffsetJsonApiTestCase(CommonJsonApiTestCase):
    """
    Тестируем правильную работу amount/offset для разных ручек
    
    Основные проблемы с общими папками
    """

    def setup_method(self, method):
        super(AmountOffsetJsonApiTestCase, self).setup_method(method)
        uid1 = self.uid
        uid2 = self.uid_1
        self.create_user(uid1)
        self.create_user(uid2)
        self.uid1_shared_dir = uid1_shared_dir = u'/disk/005_uid1_shared_dir'
        uid2_own_dir = u'/disk/003_uid2_own_dir'
        uid1_shared_subdir = u'/disk/005_uid1_shared_dir/008_sub_dir'

        # все ресурсы должны быть видны пользователю uid2
        resources = [
            (uid2, 'file', u'/disk/001.avi'),

            (uid2, 'dir', uid2_own_dir),
            (uid1, 'dir', self.uid1_shared_dir),
            (uid1, 'dir', uid1_shared_subdir),

            (uid2, 'file', u'%s/010.avi' % uid2_own_dir),
            (uid1, 'file', u'%s/030.jpg' % uid1_shared_dir),
            (uid2, 'file', u'%s/040.avi' % uid2_own_dir),
            (uid2, 'file', u'%s/100.avi' % uid1_shared_subdir),
        ]

        self.all_resources = [
            (uid2, 'dir', u'/disk'),
            (uid2, 'dir', u'/disk/Музыка'),
        ]

        # сортировка по name должна совпадать с сортировкой по времени создания
        resources.sort(key=lambda x: x[2].rsplit('/')[-1])
        for uid, res_type, path in resources:
            if res_type == 'dir':
                self.json_ok('mkdir', {'uid': uid, 'path': path})
                if path == self.uid1_shared_dir:
                    self.share_dir(uid1, uid2, self.email_1, uid1_shared_dir)
            else:
                self.upload_file(uid, path)
            self.all_resources.append((uid, res_type, path))
            time.sleep(2)

    def args_variator(self, sort_fields=(None,), amount_offset_len=0, orders=(None,)):
        """Формирует все вариации полей: sort, amount, offset, order"""
        args_iter = itertools.product(sort_fields,
                                      range(amount_offset_len) + [None],
                                      range(amount_offset_len) + [None],
                                      orders)
        for sort, offset, amount, order in args_iter:
            args = {}
            args['sort'] = str(sort)
            args['offset'] = str(offset)
            args['amount'] = str(amount)
            args['order'] = str(order)
            if sort is None:
                args.pop('sort')
            if offset is None:
                args.pop('offset')
            if amount is None:
                args.pop('amount')
            if order is None:
                args.pop('order')
            yield sort, amount, offset, order, args

    def get_right_response_pathes(self,
                                  operation,
                                  path=u'/disk',
                                  resources_type='dir',
                                  order=None,
                                  amount=None,
                                  offset=None):
        """
        Отдает правильный ответ, который должен совпасть с MPFS-ым

        Работает только для сортировки по времени создания и имени(дефолтная).
        Для остального можно проверять только кол-во элементов.
        """
        if resources_type == 'file':
            return [path]

        right_response = []
        if operation == 'timeline':
            # первый ресурс целевая папка(есть всегда)
            # остальные ресурсы файлы
            right_response = []
            for _, res_type, res_path in self.all_resources:
                if res_path.startswith(path) and res_type == 'file':
                    right_response.append(res_path)
            end = (offset or 0) + amount if amount is not None else None
            if order == 0:
                right_response.reverse()
            return [path] + right_response[offset:end]
        elif operation == 'public_list':
            # первый ресурс целевая папка(есть всегда)
            # за ним всегда идут папки
            # остальные ресурсы файлы
            right_response = []
            dirs = []
            files = []
            for _, res_type, res_path in self.all_resources:
                if (res_path.startswith(path) and
                                len(path.split('/')) + 1 == len(res_path.split('/'))):
                    if res_type == 'dir':
                        dirs.append(res_path)
                    else:
                        files.append(res_path)
            if order == 0:
                dirs.reverse()
                files.reverse()
            right_response += dirs + files
            end = (offset or 0) + amount if amount is not None else None
            return [path] + right_response[offset:end]
        else:
            raise NotImplementedError()

    @staticmethod
    def response2names(mpfs_response):
        return [i['name'] for i in mpfs_response]

    @staticmethod
    def pathes2names(pathes):
        return [i.rsplit('/')[-1] for i in pathes]

    def test_public_list(self):
        uid1 = self.uid
        uid2 = self.uid_1
        result = self.json_ok('set_public', {'uid': uid1, 'path': self.uid1_shared_dir})
        pub_hash = result['hash']

        for sort, amount, offset, order, args in self.args_variator(amount_offset_len=len(self.all_resources)):
            # print 'Sort: %6s, amount: %5s, offset: %5s, order: %5s, args: %s' % (str(sort), str(amount), str(offset), str(order), str(args))
            args['private_hash'] = pub_hash
            public_list_result = self.json_ok('public_list', args)
            public_list_names = self.response2names(public_list_result)

            my_public_list_pathes = self.get_right_response_pathes(
                'public_list',
                path=self.uid1_shared_dir,
                resources_type='dir',
                order=order,
                amount=amount,
                offset=offset
            )
            my_public_list_names = self.pathes2names(my_public_list_pathes)
            if my_public_list_names != public_list_names:
                print 'Args: ', args
                print 'Result: ', public_list_names
                print 'Excpected: ', my_public_list_names
                print
                assert my_public_list_names == public_list_names

    def test_timeline_root_path(self):
        """
        тестируем offset и amount для timeline с расшаренными папками
        """
        uid1 = self.uid
        uid2 = self.uid_1

        for sort, amount, offset, order, args in self.args_variator(amount_offset_len=len(self.all_resources)):
            args['uid'] = uid2
            tl_result = self.json_ok('timeline', args)
            tl_names = self.response2names(tl_result)

            my_tl_pathes = self.get_right_response_pathes(
                'timeline',
                resources_type='dir',
                order=order,
                amount=amount,
                offset=offset)
            my_tl_names = self.pathes2names(my_tl_pathes)

            if tl_names != my_tl_names:
                print 'Args: ', args
                print 'Result: ', tl_names
                print 'Excpected: ', my_tl_names
                assert my_tl_names == tl_names

    def test_timeline_with_sort_by_mtime_and_offset(self):
        now = int(time.time())
        for i in xrange(10):
            self.upload_file(self.uid, '/disk/file-%d.txt' % i, file_data={'mtime': now + i})

        params = {
            'uid': self.uid,
            'sort': 'mtime',
            'order': '1',
            'mtime_gte': str(now),
            'amount': 10,
            'offset': 2
        }
        result = self.json_ok('timeline', params)
        files = [r for r in result if r['type'] == 'file']

        assert files[0]['name'] == 'file-2.txt'

        previous_mtime = 0
        for i in result:
            assert previous_mtime <= i['mtime']
            previous_mtime = i['mtime']
