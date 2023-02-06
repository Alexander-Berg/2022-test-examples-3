#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
from market.access.puller.mt.env import AccessPullerSuite, main
from market.pylibrary.lite.response import NullResponse
from market.pylibrary.lite.matcher import NotEmpty
from time import sleep


class T(AccessPullerSuite):
    @classmethod
    def connect(cls):
        return {
            'access_server': cls.access_puller.access,
            's3': cls.access_puller.s3
        }

    def __check(self, resource, versions):
        fragment = {'version': [{
            'number': v,
            'storage': {
                'location': {
                    'access': {
                        'rbtorrent': NotEmpty()
                    }
                }
            }
        } for v in versions]}

        while True:
            response = self.access_server.list_versions(resource_name=resource)
            if isinstance(response, NullResponse):
                self.fail_verbose('Server has crashed', [], response.request)
            if response.contains(fragment, preserve_order=False, allow_different_len=False):
                return
            sleep(1)

    def test_pull_file(self):
        # создаем паблишер
        self.access_server.create_publisher(name='pub1')

        # создаем ресурс
        self.access_server.create_resource(name='res1', publisher_name='pub1')

        # создаем пуллер
        self.access_puller.create_resource_puller(name='puller1', resource_name='res1', mds={
            'host': 'http://localhost:{}'.format(self.s3.port), 'key': 'test_key'
        }, period='1s')

        # пишем конетент в s3
        self.s3.write('test', 'test_key', 'content')

        # проверяем, что версия создалась
        self.__check('res1', ['1.0.0'])

        # проверяем, что при неизменном контенте версии новые версии не создаются
        sleep(5)
        self.__check('res1', ['1.0.0'])

        # обновляем конетент в s3
        self.s3.write('test', 'test_key', 'new content')

        # проверяем, что новая версия создалась
        self.__check('res1', ['1.0.0', '2.0.0'])

    def test_pull_folder(self):
        # создаем паблишер
        self.access_server.create_publisher(name='pub2')

        # создаем ресурс
        self.access_server.create_resource(name='res2', publisher_name='pub2')

        # создаем пуллер
        self.access_puller.create_resource_puller(name='puller2', resource_name='res2', mds={
            'host': 'http://localhost:{}'.format(self.s3.port), 'prefix': 'test_prefix/'
        }, period='1s')

        # пишем конетент в s3
        self.s3.write('test', 'test_prefix/key1', 'content')

        # проверяем, что версия создалась
        self.__check('res2', ['1.0.0'])

        # проверяем, что при неизменном контенте версии новые версии не создаются
        sleep(5)
        self.__check('res2', ['1.0.0'])

        # обновляем конетент в s3
        self.s3.write('test', 'test_prefix/key2', 'new content')

        # проверяем, что новая версия создалась
        self.__check('res2', ['1.0.0', '2.0.0'])


if __name__ == '__main__':
    main()
